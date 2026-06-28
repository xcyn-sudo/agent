package org.example.agent_qr.knowledge.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.event.DataSourceDeletedEvent;
import org.example.agent_qr.knowledge.entity.Chunk;
import org.example.agent_qr.knowledge.mapper.ChunkMapper;
import org.example.agent_qr.rag.mapper.ChunkStructuredMapper;
import org.example.agent_qr.rag.retriever.BM25Retriever;
import org.example.agent_qr.rag.retriever.ChromaRetriever;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据源删除事件监听器。
 * <p>
 * 监听 {@link DataSourceDeletedEvent}，异步执行级联清理：
 * <ol>
 *   <li>软删除关联切片（kb_chunk.deleted = 1）</li>
 *   <li>移除 ChromaDB 向量</li>
 *   <li>移除 BM25 索引</li>
 *   <li>清理 kb_chunk_structured 结构化元数据</li>
 * </ol>
 * 清理失败不影响数据源主流程，由 OrphanVectorScanner 定期兜底。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSourceDeleteListener {

    private final ChunkMapper chunkMapper;
    private final ChromaRetriever chromaRetriever;
    private final BM25Retriever bm25Retriever;
    private final ChunkStructuredMapper chunkStructuredMapper;

    /**
     * 处理数据源删除事件，异步执行级联清理。
     *
     * @param event 数据源删除事件
     */
    @Async("chunkExecutor")
    @EventListener
    public void handleDataSourceDeleted(DataSourceDeletedEvent event) {
        Long datasourceId = event.getDatasourceId();
        log.info("开始数据源删除级联清理: datasourceId={}", datasourceId);

        try {
            // 1. 查询关联切片
            List<Chunk> chunks = chunkMapper.selectByDatasourceId(datasourceId);
            if (chunks.isEmpty()) {
                log.info("数据源无关联切片，跳过清理: datasourceId={}", datasourceId);
                return;
            }
            log.info("找到关联切片: datasourceId={}, chunkCount={}", datasourceId, chunks.size());

            // 2. 软删除切片
            int softDeleted = chunkMapper.softDeleteByDatasourceId(datasourceId);
            log.info("切片软删除完成: datasourceId={}, count={}", datasourceId, softDeleted);

            // 3. 从 BM25 索引移除
            for (Chunk chunk : chunks) {
                try {
                    bm25Retriever.removeFromIndex(chunk.getId());
                } catch (Exception e) {
                    log.warn("BM25 索引移除失败: chunkId={}, datasourceId={}", chunk.getId(), datasourceId, e);
                }
            }

            // 4. 从 ChromaDB 移除向量（优先批量删除，失败时降级逐条删除）
            try {
                chromaRetriever.deleteByMetadata("datasource_id", datasourceId.toString());
                log.info("ChromaDB 向量批量清理完成: datasourceId={}", datasourceId);
            } catch (Exception batchEx) {
                log.warn("ChromaDB 批量清理失败，降级逐条删除: datasourceId={}", datasourceId, batchEx);
                // 降级：逐条按 chunk 删除
                for (Chunk chunk : chunks) {
                    try {
                        if (chunk.getChromaId() != null && !"pending".equals(chunk.getChromaId())) {
                            chromaRetriever.deleteByMetadata("chunk_id", chunk.getId().toString());
                        }
                    } catch (Exception e) {
                        log.warn("ChromaDB 逐条清理失败: chunkId={}, datasourceId={}", chunk.getId(), datasourceId, e);
                    }
                }
            }

            // 5. 清理结构化元数据
            for (Chunk chunk : chunks) {
                try {
                    chunkStructuredMapper.deleteByChunkId(chunk.getId());
                } catch (Exception e) {
                    log.warn("结构化元数据清理失败: chunkId={}, datasourceId={}", chunk.getId(), datasourceId, e);
                }
            }

            log.info("数据源删除级联清理完成: datasourceId={}, chunks={}", datasourceId, chunks.size());
        } catch (Exception e) {
            log.error("数据源删除级联清理异常: datasourceId={}", datasourceId, e);
        }
    }
}
