package org.example.agent_qr.rag.provider.dashscope;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.BusinessException;
import org.example.agent_qr.rag.provider.LLMProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 阿里云 DashScope（千问）LLM Provider，用于结构化过滤条件提取。
 * <p>
 * 使用 DashScope OpenAI 兼容端点，通过 LangChain4j 的 OpenAiChatModel 调用。
 * 配置项通过 {@code dashscope.llm.*} 前缀注入。
 * 仅实现同步 generate()——提取任务不需要流式输出。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class DashScopeLLMProvider implements LLMProvider {

    @Value("${dashscope.llm.api-key:${DASHSCOPE_API_KEY:}}")
    private String apiKey;

    @Value("${dashscope.llm.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${dashscope.llm.model:qwen-turbo}")
    private String model;

    @Value("${dashscope.llm.temperature:0.1}")
    private Double temperature;

    @Value("${dashscope.llm.max-tokens:1024}")
    private Integer maxTokens;

    private OpenAiChatModel chatModel;

    /**
     * 初始化 OpenAiChatModel 实例，指向 DashScope 兼容端点。
     */
    @PostConstruct
    public void init() {
        log.info("初始化 DashScope LLM Provider，baseUrl={}, model={}", baseUrl, model);
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }

    /**
     * 调用 DashScope API 同步生成回复。
     *
     * @param messages 聊天消息列表
     * @return AI 生成的回复文本
     * @throws BusinessException 当 AI 服务调用失败时
     */
    @Override
    public String generate(List<ChatMessage> messages) {
        try {
            AiMessage aiMessage = chatModel.chat(messages).aiMessage();
            String answer = aiMessage.text();
            log.debug("DashScope 生成回复成功，长度: {}", answer != null ? answer.length() : 0);
            return answer;
        } catch (Exception e) {
            log.error("DashScope LLM 调用失败", e);
            throw new BusinessException("DashScope AI 服务暂时不可用");
        }
    }
}
