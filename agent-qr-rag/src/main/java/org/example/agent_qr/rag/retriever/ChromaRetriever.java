package org.example.agent_qr.rag.retriever;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.rag.entity.RetrievedDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ChromaDB 向量检索器，封装 ChromaDB 的相似度搜索与删除操作。
 * <p>
 * 通过 LangChain4j 的 ChromaEmbeddingStore 与 ChromaDB 交互，
 * 支持基于向量相似度的 top-K 检索和按文档 ID 删除向量。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class ChromaRetriever {

    @Value("${langchain4j.chroma.collection-name:enterprise_knowledge}")
    private String collectionName;

    @Autowired(required = false)
    private ChromaEmbeddingStore chromaEmbeddingStore;

    /**
     * 相似度搜索，返回与查询向量最相似的 topK 个文档。
     *
     * @param queryEmbedding 查询文本的向量表示
     * @param topK           返回的最大结果数
     * @return 检索结果列表
     */
    public List<RetrievedDocument> similaritySearch(float[] queryEmbedding, int topK) {
        if (chromaEmbeddingStore == null) {
            log.warn("ChromaEmbeddingStore 未初始化，返回空检索结果");
            return new ArrayList<>();
        }

        try {
            Embedding embedding = new Embedding(queryEmbedding);
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(embedding)
                    .maxResults(topK)
                    .minScore(0.0)
                    .build();
            EmbeddingSearchResult<TextSegment> result = chromaEmbeddingStore.search(request);
            List<EmbeddingMatch<TextSegment>> matches = result.matches();

            List<RetrievedDocument> documents = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : matches) {
                RetrievedDocument doc = new RetrievedDocument();
                doc.setDocumentId(match.embeddingId());
                doc.setContent(match.embedded().text());
                doc.setSimilarity(match.score());

                // 从元数据中提取文档标题
                if (match.embedded().metadata() != null) {
                    String title = match.embedded().metadata().getString("document_title");
                    doc.setDocumentTitle(title != null ? title : "未命名文档");
                } else {
                    doc.setDocumentTitle("未命名文档");
                }

                documents.add(doc);
            }

            log.debug("相似度搜索完成，查询 TopK={}, 返回结果数={}", topK, documents.size());
            return documents;
        } catch (Exception e) {
            log.error("ChromaDB 相似度搜索失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据文档 ID 删除对应的向量记录。
     * <p>
     * P1 基础实现：如果 chromaEmbeddingStore 为 null 则仅记录警告日志。
     * </p>
     *
     * @param documentId 文档 ID
     */
    public void deleteByDocumentId(Long documentId) {
        if (documentId == null) {
            log.warn("documentId 为 null，跳过删除向量记录");
            return;
        }
        if (chromaEmbeddingStore == null) {
            log.warn("ChromaEmbeddingStore 未初始化，无法删除文档 ID={} 的向量", documentId);
            return;
        }

        try {
            Filter filter = MetadataFilterBuilder.metadataKey("document_id").isEqualTo(documentId.toString());
            chromaEmbeddingStore.removeAll(filter);
            log.info("已删除文档 ID={} 的向量记录", documentId);
        } catch (Exception e) {
            log.error("删除文档 ID={} 的向量记录失败", documentId, e);
        }
    }

    // ==================== P2 新增方法 ====================

    /**
     * ★ P2: 按 ChromaDB 向量 ID 批量物理删除。
     * <p>
     * 由 compensation 模块的 {@code DocumentDeleteServiceV2} 调用，
     * 实现 ChromaDB 端向量的物理删除。
     * </p>
     *
     * @param ids ChromaDB 向量 ID 列表
     */
    public void deleteByIds(List<String> ids) {
        if (chromaEmbeddingStore == null) {
            log.warn("ChromaEmbeddingStore 未初始化，跳过向量批量删除");
            return;
        }
        if (ids == null || ids.isEmpty()) {
            return;
        }

        try {
            chromaEmbeddingStore.removeAll(ids);
            log.info("ChromaDB 批量删除向量完成: count={}", ids.size());
        } catch (Exception e) {
            log.error("ChromaDB 批量删除向量失败: count={}", ids.size(), e);
            throw new RuntimeException("ChromaDB 批量删除失败", e);
        }
    }

    /**
     * ★ P2: 按元数据键值对删除向量记录。
     * <p>
     * 由 compensation 模块的 {@code OrphanVectorScanner} 调用，
     * 用于清理孤儿向量（MySQL 中已删除但 ChromaDB 中残留的记录）。
     * </p>
     *
     * @param metadataKey   元数据键名（如 "document_id"）
     * @param metadataValue 元数据值
     */
    public void deleteByMetadata(String metadataKey, String metadataValue) {
        if (chromaEmbeddingStore == null) {
            log.warn("ChromaEmbeddingStore 未初始化，无法按元数据删除向量: {}={}", metadataKey, metadataValue);
            return;
        }

        try {
            Filter filter = MetadataFilterBuilder.metadataKey(metadataKey).isEqualTo(metadataValue);
            chromaEmbeddingStore.removeAll(filter);
            log.info("已删除元数据 {}={} 的向量记录", metadataKey, metadataValue);
        } catch (Exception e) {
            log.error("按元数据删除向量失败: {}={}", metadataKey, metadataValue, e);
        }
    }
}
