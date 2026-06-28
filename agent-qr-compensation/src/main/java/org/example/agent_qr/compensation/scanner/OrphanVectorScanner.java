package org.example.agent_qr.compensation.scanner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.datasource.entity.DataSourceConfig;
import org.example.agent_qr.datasource.mapper.DataSourceMapper;
import org.example.agent_qr.knowledge.entity.Chunk;
import org.example.agent_qr.knowledge.entity.Document;
import org.example.agent_qr.knowledge.mapper.ChunkMapper;
import org.example.agent_qr.knowledge.mapper.DocumentMapper;
import org.example.agent_qr.rag.mapper.ChunkStructuredMapper;
import org.example.agent_qr.rag.retriever.BM25Retriever;
import org.example.agent_qr.rag.retriever.ChromaRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 孤儿向量扫描器 — 最终兜底。
 * <p>
 * 每 5 分钟扫描并清理两类孤儿数据：
 * <ol>
 *   <li><b>文档级孤儿</b>：MySQL 中已删除/不存在的文档对应的 ChromaDB 向量</li>
 *   <li><b>数据源级孤儿</b>：已删除/非活跃数据源关联的切片、向量、BM25 索引和结构化元数据</li>
 * </ol>
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class OrphanVectorScanner {

    @Autowired
    private ChunkMapper chunkMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DataSourceMapper dataSourceMapper;

    @Autowired(required = false)
    private ChromaRetriever chromaRetriever;

    @Autowired(required = false)
    private BM25Retriever bm25Retriever;

    @Autowired(required = false)
    private ChunkStructuredMapper chunkStructuredMapper;

    /**
     * 每 5 分钟扫描并清理孤儿向量（文档级 + 数据源级）。
     */
    @Scheduled(fixedDelay = 300000)
    public void scanAndCleanOrphanVectors() {
        log.info("开始孤儿向量扫描...");

        if (chromaRetriever == null) {
            log.warn("ChromaRetriever 未初始化，跳过孤儿向量扫描");
            return;
        }

        try {
            // ========== Phase 1: 文档级孤儿清理 ==========
            cleanDocumentOrphans();

            // ========== Phase 2: 数据源级孤儿清理 ==========
            cleanDatasourceOrphans();

            log.info("孤儿向量扫描完成");
        } catch (Exception e) {
            log.error("孤儿向量扫描异常", e);
        }
    }

    /**
     * Phase 1: 清理文档级孤儿（chunks 的 documentId 指向已删除文档）。
     */
    private void cleanDocumentOrphans() {
        try {
            List<Document> allDocs = documentMapper.selectList(null);
            Set<Long> validDocIds = allDocs.stream()
                    .filter(d -> d.getDeleted() == null || d.getDeleted() == 0)
                    .map(Document::getId)
                    .collect(Collectors.toSet());

            List<Chunk> chunks = chunkMapper.selectAllReadyChunks();
            Set<Long> orphanDocIds = chunks.stream()
                    .map(Chunk::getDocumentId)
                    .filter(docId -> docId != null && !validDocIds.contains(docId))
                    .collect(Collectors.toSet());

            int cleaned = 0;
            for (Long docId : orphanDocIds) {
                try {
                    chromaRetriever.deleteByDocumentId(docId);
                    cleaned++;
                } catch (Exception e) {
                    log.warn("清理文档孤儿向量失败: documentId={}, error={}", docId, e.getMessage());
                }
            }

            if (cleaned > 0) {
                log.info("文档级孤儿清理完成: {} 个文档", cleaned);
            }
        } catch (Exception e) {
            log.error("文档级孤儿扫描异常", e);
        }
    }

    /**
     * Phase 2: 清理数据源级孤儿（chunks 的 datasourceId 指向不存在/非活跃数据源）。
     * <p>
     * 覆盖场景：
     * <ul>
     *   <li>数据源已被物理删除，但关联切片/向量/索引/元数据残留</li>
     *   <li>数据源状态为 INACTIVE/ERROR，其切片不应再参与检索</li>
     * </ul>
     * </p>
     */
    private void cleanDatasourceOrphans() {
        try {
            // 1. 获取所有活跃数据源 ID
            List<DataSourceConfig> activeSources = dataSourceMapper.selectAllActive();
            Set<Long> activeDsIds = activeSources.stream()
                    .map(DataSourceConfig::getId)
                    .collect(Collectors.toSet());
            log.debug("活跃数据源 ID 集合: size={}", activeDsIds.size());

            // 2. 查询所有 datasource_id 不为空的未删除切片
            List<Chunk> dsChunks = chunkMapper.selectList(
                    new LambdaQueryWrapper<Chunk>()
                            .isNotNull(Chunk::getDatasourceId)
                            .eq(Chunk::getDeleted, 0));

            if (dsChunks.isEmpty()) {
                return;
            }

            // 3. 找出孤儿：datasourceId 不在活跃数据源集合中的切片
            List<Chunk> orphanChunks = dsChunks.stream()
                    .filter(c -> c.getDatasourceId() != null && !activeDsIds.contains(c.getDatasourceId()))
                    .toList();

            if (orphanChunks.isEmpty()) {
                return;
            }

            log.info("发现数据源孤儿切片: count={}", orphanChunks.size());

            // 收集孤儿数据源 ID（用于 ChromaDB 批量清理）
            Set<Long> orphanDsIds = orphanChunks.stream()
                    .map(Chunk::getDatasourceId)
                    .collect(Collectors.toSet());

            int softDeleted = 0;
            int bm25Cleaned = 0;
            int structuredCleaned = 0;
            int chromaCleaned = 0;

            for (Chunk chunk : orphanChunks) {
                // 4. 软删除切片（MyBatis-Plus @TableLogic → UPDATE SET deleted=1）
                try {
                    chunkMapper.deleteById(chunk.getId());
                    softDeleted++;
                } catch (Exception e) {
                    log.warn("数据源孤儿切片软删除失败: chunkId={}", chunk.getId(), e);
                }

                // 5. 从 BM25 索引移除
                if (bm25Retriever != null) {
                    try {
                        bm25Retriever.removeFromIndex(chunk.getId());
                        bm25Cleaned++;
                    } catch (Exception e) {
                        log.warn("BM25 索引移除失败: chunkId={}", chunk.getId(), e);
                    }
                }

                // 6. 清理结构化元数据
                if (chunkStructuredMapper != null) {
                    try {
                        chunkStructuredMapper.deleteByChunkId(chunk.getId());
                        structuredCleaned++;
                    } catch (Exception e) {
                        log.warn("结构化元数据清理失败: chunkId={}", chunk.getId(), e);
                    }
                }
            }

            // 7. ChromaDB 批量清理：按 orphan datasource_id 元数据删除向量
            for (Long orphanDsId : orphanDsIds) {
                try {
                    chromaRetriever.deleteByMetadata("datasource_id", orphanDsId.toString());
                    chromaCleaned++;
                } catch (Exception e) {
                    log.warn("ChromaDB 按 datasource_id 清理失败: datasourceId={}", orphanDsId, e);
                }
            }

            log.info("数据源孤儿清理完成: 切片软删除={}, BM25={}, 结构化元数据={}, ChromaDB数据源数={}",
                    softDeleted, bm25Cleaned, structuredCleaned, chromaCleaned);
        } catch (Exception e) {
            log.error("数据源孤儿扫描异常", e);
        }
    }
}
