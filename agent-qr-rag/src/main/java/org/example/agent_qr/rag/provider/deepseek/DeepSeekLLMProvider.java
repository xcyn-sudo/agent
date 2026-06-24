package org.example.agent_qr.rag.provider.deepseek;

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
 * DeepSeek LLM 服务提供商实现。
 * <p>
 * 基于 LangChain4j 的 OpenAiChatModel 调用 DeepSeek API，
 * 支持同步文本生成。配置项通过 {@code llm.deepseek.*} 前缀注入。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class DeepSeekLLMProvider implements LLMProvider {

    @Value("${llm.deepseek.api-key}")
    private String apiKey;

    @Value("${llm.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${llm.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${llm.deepseek.temperature:0.7}")
    private Double temperature;

    @Value("${llm.deepseek.max-tokens:2048}")
    private Integer maxTokens;

    private OpenAiChatModel chatModel;

    /**
     * 初始化 OpenAiChatModel 实例。
     */
    @PostConstruct
    public void init() {
        log.info("初始化 DeepSeek LLM Provider，baseUrl={}, model={}", baseUrl, model);
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }

    /**
     * 调用 DeepSeek API 同步生成回复。
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
            log.debug("DeepSeek 生成回复成功，长度: {}", answer != null ? answer.length() : 0);
            return answer;
        } catch (Exception e) {
            log.error("DeepSeek LLM 调用失败", e);
            throw new BusinessException("AI 服务暂时不可用");
        }
    }
}
