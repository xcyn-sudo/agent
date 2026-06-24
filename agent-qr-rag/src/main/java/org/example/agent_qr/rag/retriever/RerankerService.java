package org.example.agent_qr.rag.retriever;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.rag.entity.RetrievedDocument;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 重排序服务。
 * <p>
 * 对粗排候选结果进行精排。P2 阶段使用基于文本重叠度的
 * 简化重排序算法（bge-reranker-v2-m3 替代方案），
 * 按 query 与文档内容的相关性重新排序并截断至 topK。
 * P3 可升级为调用 bge-reranker-v2-m3 模型 API。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class RerankerService {

    /**
     * 对候选文档进行重排序。
     * <p>
     * P2 简化实现：基于 query 与文档内容的 TF-IDF 风格相关性分数排序。
     * 候选数 <= topK 时直接返回。
     * </p>
     *
     * @param query      用户查询
     * @param candidates 候选文档列表
     * @param topK       返回的最大结果数
     * @return 重排序后的 TopK 结果
     */
    public List<RetrievedDocument> rerank(String query, List<RetrievedDocument> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        if (candidates.size() <= topK) {
            return candidates;
        }

        // 基于文本重叠度的简化相关性计算
        List<RetrievedDocument> reranked = candidates.stream()
                .peek(doc -> {
                    double overlapScore = calculateOverlapScore(query, doc.getContent());
                    // 将原始相似度与重叠度加权合并
                    double combinedScore = 0.4 * (doc.getSimilarity() != null ? doc.getSimilarity() : 0.0)
                            + 0.6 * overlapScore;
                    doc.setSimilarity(combinedScore);
                })
                .sorted(Comparator.comparing(RetrievedDocument::getSimilarity).reversed())
                .limit(topK)
                .toList();

        log.debug("重排序完成: query={}, candidates={} → topK={}", query, candidates.size(), reranked.size());
        return reranked;
    }

    /**
     * 计算查询与文档内容的文本重叠度分数。
     */
    private double calculateOverlapScore(String query, String content) {
        if (query == null || content == null) {
            return 0.0;
        }

        String[] queryTerms = query.toLowerCase().split("\\s+");
        String contentLower = content.toLowerCase();

        int matchedTerms = 0;
        for (String term : queryTerms) {
            if (term.length() >= 2 && contentLower.contains(term)) {
                matchedTerms++;
            }
        }

        return queryTerms.length > 0 ? (double) matchedTerms / queryTerms.length : 0.0;
    }
}
