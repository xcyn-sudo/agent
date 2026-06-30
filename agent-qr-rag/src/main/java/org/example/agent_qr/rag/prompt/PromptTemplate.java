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

    /** 系统提示词前缀：角色设定 + 参考资料引导 */
    private static final String SYSTEM_PROMPT_PREFIX = """
            你是一个企业知识库助手，请根据以下参考资料回答用户问题。
            如果参考资料中没有相关信息，请如实告知用户"知识库中暂无相关信息"，不要编造答案。

            参考资料：
            """;

    /** 系统提示词后缀：回答指令 */
    private static final String SYSTEM_PROMPT_SUFFIX = """

            请基于以上参考资料给出准确、简洁的回答：""";

    /** 聚合查询系统提示词前缀：完整记录列表展示 */
    private static final String AGGREGATION_PROMPT_PREFIX = """
            你是一个企业知识库助手。以下是从知识库中检索到的完整记录列表。
            请根据用户问题，完整列出或统计所有匹配的记录。
            如果记录数量较多，请以清晰的结构化格式（如表格或列表）展示。
            不要在回答中遗漏任何记录。""";

    /** 聚合查询系统提示词后缀：统计/列举指令 */
    private static final String AGGREGATION_PROMPT_SUFFIX = """

            请根据以上完整记录列表，回答用户的问题。请确保：
            1. 如果用户要求列出，请列出所有记录，不要遗漏。
            2. 如果用户要求统计，请给出准确的数字。
            3. 如果记录已被截断（上下文中有截断提示），请在回答中注明。""";

    /**
     * 返回不含文档内容的系统提示词基础文本。
     * <p>
     * 供 {@link ContextTokenManager} 进行 token 预算估算使用。
     * </p>
     *
     * @return 系统提示词前缀 + 后缀的拼接
     */
    public String getSystemPromptBase() {
        return SYSTEM_PROMPT_PREFIX + SYSTEM_PROMPT_SUFFIX;
    }

    /**
     * 构建系统消息（角色指令 + 检索上下文）。
     *
     * @param context 从知识库检索到的上下文文本
     * @return 系统 Prompt 文本
     */
    public String buildSystemPrompt(String context) {
        return SYSTEM_PROMPT_PREFIX + context + SYSTEM_PROMPT_SUFFIX;
    }

    /**
     * 构建用户消息（用户原始问题）。
     *
     * @param query 用户问题
     * @return 用户 Prompt 文本
     */
    public String buildUserPrompt(String query) {
        return query;
    }

    /**
     * 返回聚合查询专用的系统提示词基础文本（不含上下文内容）。
     *
     * @return 聚合查询提示词前缀 + 后缀的拼接
     */
    public String getAggregationPromptBase() {
        return AGGREGATION_PROMPT_PREFIX + AGGREGATION_PROMPT_SUFFIX;
    }

    /**
     * 构建聚合查询的 System Prompt（角色指令 + 完整记录上下文）。
     *
     * @param context 聚合查询的上下文文本（紧凑格式）
     * @return 系统 Prompt 文本
     */
    public String buildAggregationSystemPrompt(String context) {
        return AGGREGATION_PROMPT_PREFIX + context + AGGREGATION_PROMPT_SUFFIX;
    }

    /**
     * @deprecated 请使用 {@link #buildSystemPrompt(String)} + {@link #buildUserPrompt(String)}
     *             分别构建 SystemMessage 和 UserMessage，以符合 Chat API 标准对话格式。
     */
    @Deprecated
    public String build(String query, String context) {
        return String.format("""
                你是一个企业知识库助手，请根据以下参考资料回答用户问题。
                如果参考资料中没有相关信息，请如实告知用户"知识库中暂无相关信息"，不要编造答案。

                参考资料：
                %s

                用户问题：%s

                请基于以上参考资料给出准确、简洁的回答：""", context, query);
    }
}
