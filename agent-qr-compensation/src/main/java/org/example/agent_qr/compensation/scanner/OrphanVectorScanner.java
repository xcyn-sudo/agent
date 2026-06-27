package org.example.agent_qr.compensation.scanner;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.knowledge.entity.Chunk;
import org.example.agent_qr.knowledge.entity.Document;
import org.example.agent_qr.knowledge.mapper.ChunkMapper;
import org.example.agent_qr.knowledge.mapper.DocumentMapper;
import org.example.agent_qr.rag.retriever.ChromaRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 孤儿向量扫描器 — 最终兜底。
 * <p>
 * 每 30 分钟扫描 ChromaDB 中的向量记录，清理在 MySQL 中
 * 已不存在或已标记 deleted=1 的文档对应的向量记录。
 * 通过 {@link ChromaRetriever} 封装类操作 ChromaDB。
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

    @Autowired(required = false)
    private ChromaRetriever chromaRetriever;

    /**
     * 每 30 分钟扫描并清理孤儿向量。
     */
    @Scheduled(fixedDelay = 1800000)
    public void scanAndCleanOrphanVectors() {
        log.info("开始孤儿向量扫描...");

        if (chromaRetriever == null) {
            log.warn("ChromaRetriever 未初始化，跳过孤儿向量扫描");
            return;
        }

        try {
            // 1. 获取所有有效文档 ID 集合
            List<Document> allDocs = documentMapper.selectList(null);
            Set<Long> validDocIds = allDocs.stream()
                    .filter(d -> d.getDeleted() == null || d.getDeleted() == 0)
                    .map(Document::getId)
                    .collect(Collectors.toSet());

            // 2. 查询所有就绪切片，按文档 ID 分组找出孤儿文档
            List<Chunk> chunks = chunkMapper.selectAllReadyChunks();
            Set<Long> orphanDocIds = chunks.stream()
                    .map(Chunk::getDocumentId)
                    .filter(docId -> docId != null && !validDocIds.contains(docId))
                    .collect(Collectors.toSet());

            // 3. 按文档批量清理孤儿向量
            int orphansCleaned = 0;
            for (Long docId : orphanDocIds) {
                try {
                    chromaRetriever.deleteByDocumentId(docId);
                    orphansCleaned++;
                } catch (Exception e) {
                    log.warn("清理孤儿向量失败: documentId={}, error={}", docId, e.getMessage());
                }
            }

            log.info("孤儿向量扫描完成: 清理 {} 个文档的向量数据", orphansCleaned);
        } catch (Exception e) {
            log.error("孤儿向量扫描异常", e);
        }
    }
}
