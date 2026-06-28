package org.example.agent_qr.rag.util;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.rag.entity.RetrievedDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Token 感知上下文管理器。
 * <p>
 * 负责 token 数量估算和预算约束的文档上下文拼接。
 * 采用保守启发式估算（中文 ~1.5 tokens/字, ASCII ~0.25 tokens/字），
 * 确保不会超出 LLM 上下文窗口。
 * P4 可升级为调用真实 tokenizer（如 langchain4j Tokenizer）。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class ContextTokenManager {

    @Value("${agent-qr.retrieval.max-context-tokens:8000}")
    private int maxContextTokens;

    /** LLM 回复预留 token 数（与 llm.deepseek.max-tokens 对齐） */
    private static final int RESPONSE_RESERVED_TOKENS = 2048;

    /**
     * 估算文本的 token 数量（保守启发式）。
     * <p>
     * 中文字符 ~1.5 tokens, ASCII ~0.25 tokens, 其他 ~1 token。
     * 估算值始终偏高，确保不会超出真实上下文窗口。
     * </p>
     *
     * @param text 待估算文本
     * @return 估算的 token 数
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int chineseChars = 0;
        int asciiChars = 0;
        int otherChars = 0;

        for (char c : text.toCharArray()) {
            if (Character.isIdeographic(c)
                    || Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseChars++;
            } else if (c < 128) {
                asciiChars++;
            } else {
                otherChars++;
            }
        }

        return (int) (chineseChars * 1.5 + asciiChars * 0.25 + otherChars);
    }

    /**
     * 在 token 预算内构建文档上下文文本。
     * <p>
     * 按精排顺序遍历文档，逐个累加 token 估算值，
     * 超出预算时停止后续文档的拼接。
     * </p>
     *
     * @param documents  精排后的文档列表（已按相关性降序排列）
     * @param promptBase 系统提示词模板（不含文档内容，用于 token 估算）
     * @param query      用户原始问题
     * @return 拼接后的上下文文本（已裁剪至预算内）
     */
    public String buildContextWithBudget(List<RetrievedDocument> documents,
                                         String promptBase,
                                         String query) {
        // 固定开销：系统提示词基础文本 + 用户问题 + LLM 回复预留
        int fixedTokens = estimateTokens(promptBase)
                        + estimateTokens(query)
                        + RESPONSE_RESERVED_TOKENS;
        int availableTokens = maxContextTokens - fixedTokens;

        if (availableTokens <= 0) {
            log.warn("固定开销已超过 token 预算 (fixed={}, budget={})，使用紧急回退预算",
                    fixedTokens, maxContextTokens);
            availableTokens = maxContextTokens / 2; // 紧急回退：使用一半预算
        }

        StringBuilder contextBuilder = new StringBuilder();
        int usedTokens = 0;
        int docCount = 0;

        for (RetrievedDocument doc : documents) {
            String docSegment = String.format("【%s】\n%s",
                    doc.getDocumentTitle(), doc.getContent());
            int docTokens = estimateTokens(docSegment);

            // 文档间分隔符 "\n\n" 约 1 token
            if (docCount > 0) {
                docTokens += 1;
            }

            if (usedTokens + docTokens > availableTokens) {
                log.info("Token 预算已满: used={}, nextDoc={}, budget={}, "
                                + "totalCandidates={}, included={}",
                        usedTokens, docTokens, availableTokens,
                        documents.size(), docCount);
                break;
            }

            if (docCount > 0) {
                contextBuilder.append("\n\n");
            }
            contextBuilder.append(docSegment);
            usedTokens += docTokens;
            docCount++;
        }

        log.info("上下文构建完成: {}/{} 篇文档被采用, 估算 tokens: {}/{}",
                docCount, documents.size(), usedTokens, availableTokens);

        return contextBuilder.toString();
    }
}
