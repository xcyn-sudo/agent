package org.example.agent_qr.rag.prompt;

import org.springframework.stereotype.Component;

/**
 * Prompt 模板，用于构建发送给 LLM 的提示词。
 * <p>
 * 将用户问题和检索到的知识库上下文拼接成结构化的 Prompt，
 * 引导 LLM 基于参考资料进行回答。
 * </p>
 *
 * @author agent-qr
 */
@Component
public class PromptTemplate {

    private static final String TEMPLATE = """
            你是一个企业知识库助手，请根据以下参考资料回答用户问题。
            如果参考资料中没有相关信息，请如实告知用户"知识库中暂无相关信息"，不要编造答案。

            参考资料：
            %s

            用户问题：%s

            请基于以上参考资料给出准确、简洁的回答：""";

    /**
     * 构建完整的 Prompt 字符串。
     *
     * @param query   用户问题
     * @param context 从知识库检索到的上下文文本
     * @return 格式化后的 Prompt
     */
    public String build(String query, String context) {
        return String.format(TEMPLATE, context, query);
    }
}
