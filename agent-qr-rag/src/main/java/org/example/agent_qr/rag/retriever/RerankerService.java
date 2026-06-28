package org.example.agent_qr.rag.retriever;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.rag.entity.RetrievedDocument;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 重排序服务。
 * <p>
 * 对粗排候选结果进行精排。使用字符级 n-gram（unigram + bigram）
 * 提取中文词项，计算 Jaccard 相似度作为文本相关性分数，
 * 与原始向量相似度加权组合后重新排序并截断至 topK。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class RerankerService {

    /** 原始相似度在组合分中的权重 */
    private static final double ORIGINAL_WEIGHT = 0.4;

    /** 文本相关性分数在组合分中的权重 */
    private static final double TEXT_RELEVANCE_WEIGHT = 0.6;

    /**
     * 对候选文档进行重排序。
     * <p>
     * 候选数 &lt;= topK 时直接返回，不做多余计算。
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

        // 提取查询词项（缓存，避免对每个候选文档重复计算）
        Set<String> queryNgrams = extractNgrams(query);

        List<RetrievedDocument> reranked = candidates.stream()
                .peek(doc -> {
                    double textScore = calculateJaccardSimilarity(queryNgrams, doc.getContent());
                    double combinedScore = ORIGINAL_WEIGHT
                            * (doc.getSimilarity() != null ? doc.getSimilarity() : 0.0)
                            + TEXT_RELEVANCE_WEIGHT * textScore;
                    doc.setSimilarity(combinedScore);
                })
                .sorted(Comparator.comparing(RetrievedDocument::getSimilarity).reversed())
                .limit(topK)
                .toList();

        log.debug("重排序完成: query={}, candidates={} → topK={}, queryNgramCount={}",
                query, candidates.size(), reranked.size(), queryNgrams.size());
        return reranked;
    }

    /**
     * 提取字符级 n-gram（unigram + bigram）。
     * <p>
     * 对中文等无空格语言，字符 n-gram 能有效捕捉词汇边界信息。
     * 对空格分隔的英文词，也保留完整词项。
     * </p>
     * <ul>
     *   <li>unigram: 每个单独字符</li>
     *   <li>bigram: 每两个相邻字符组成的词项</li>
     *   <li>空格分隔词: 保留完整词（如英文单词、数字）</li>
     * </ul>
     *
     * @param text 输入文本
     * @return n-gram 词项集合
     */
    Set<String> extractNgrams(String text) {
        Set<String> ngrams = new HashSet<>();
        if (text == null || text.isEmpty()) {
            return ngrams;
        }

        String lower = text.toLowerCase().trim();

        // 1. 空格分隔的完整词项（处理英文、数字等）
        String[] words = lower.split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                ngrams.add(word);
            }
        }

        // 2. 字符级 unigram + bigram（处理中文等无空格语言）
        // 过滤掉空白字符，保留有意义的字符序列
        String compact = lower.replaceAll("\\s+", "");
        int len = compact.length();

        for (int i = 0; i < len; i++) {
            // unigram
            ngrams.add(String.valueOf(compact.charAt(i)));

            // bigram
            if (i + 1 < len) {
                ngrams.add(compact.substring(i, i + 2));
            }
        }

        return ngrams;
    }

    /**
     * 计算 Jaccard 相似度。
     * <p>
     * Jaccard = |A ∩ B| / |A ∪ B|，衡量两个词项集合的重叠程度。
     * 对缓存的查询词项集合复用，避免重复提取。
     * </p>
     *
     * @param queryNgrams 预提取的查询 n-gram 集合
     * @param content     文档内容
     * @return Jaccard 相似度 [0.0, 1.0]
     */
    private double calculateJaccardSimilarity(Set<String> queryNgrams, String content) {
        if (queryNgrams.isEmpty() || content == null || content.isEmpty()) {
            return 0.0;
        }

        Set<String> contentNgrams = extractNgrams(content);
        if (contentNgrams.isEmpty()) {
            return 0.0;
        }

        // 计算交集大小
        Set<String> intersection = new HashSet<>(queryNgrams);
        intersection.retainAll(contentNgrams);

        // 计算并集大小
        Set<String> union = new HashSet<>(queryNgrams);
        union.addAll(contentNgrams);

        return (double) intersection.size() / union.size();
    }
}
