package org.example.agent_qr.rag.provider.deepseek;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.BusinessException;
import org.example.agent_qr.rag.provider.LLMProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek LLM 服务提供商实现。
 * <p>
 * 同步生成基于 LangChain4j 的 OpenAiChatModel，
 * SSE 流式生成通过 WebClient 直接调用 DeepSeek /v1/chat/completions。
 * 配置项通过 {@code llm.deepseek.*} 前缀注入。
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 初始化 OpenAiChatModel 实例（同步调用复用）。
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

    /**
     * 调用 DeepSeek API 流式生成回复（P2 SSE 支持）。
     * <p>
     * 通过 WebClient + Spring ServerSentEvent 解析器调用 /v1/chat/completions 的 SSE 流式端点，
     * 逐事件提取 delta.content 推送 token，同时捕获 reasoning_content 用于调试。
     * </p>
     *
     * @param messages 聊天消息列表
     * @return 逐 token 的 Flux 流
     */
    @Override
    public Flux<String> generateStream(List<ChatMessage> messages) {
        try {
            List<Map<String, String>> apiMessages = messages.stream()
                    .map(m -> Map.of("role", mapRole(m), "content", extractContent(m)))
                    .toList();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", apiMessages);
            requestBody.put("stream", true);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            return WebClient.create().post()
                    .uri(baseUrl + "/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                    .map(ServerSentEvent::data)
                    .filter(data -> !"[DONE]".equals(data))
                    .mapNotNull(data -> {
                        try {
                            Map<String, Object> obj = OBJECT_MAPPER.readValue(data, Map.class);
                            if (obj.get("choices") instanceof List choices && !choices.isEmpty()) {
                                if (choices.get(0) instanceof Map choice
                                        && choice.get("delta") instanceof Map delta) {
                                    // ★ 优先提取 content
                                    if (delta.get("content") instanceof String content && !content.isEmpty()) {
                                        return content;
                                    }
                                    // ★ 捕获 reasoning_content（记录日志，便于诊断空内容问题）
                                    if (delta.get("reasoning_content") instanceof String reasoning
                                            && !reasoning.isEmpty()) {
                                        log.debug("DeepSeek reasoning_content (长度={}): {}",
                                                reasoning.length(),
                                                reasoning.length() > 200
                                                        ? reasoning.substring(0, 200) + "..."
                                                        : reasoning);
                                    }
                                    // 无 content 也无 reasoning → 记录 key set 用于诊断
                                    if (!delta.containsKey("content")
                                            && !delta.containsKey("reasoning_content")) {
                                        log.debug("DeepSeek SSE delta 无 content/reasoning, keys={}",
                                                delta.keySet());
                                    }
                                }
                            }
                        } catch (JsonProcessingException e) {
                            log.debug("DeepSeek SSE JSON 解析失败: {}",
                                    data.length() > 200 ? data.substring(0, 200) + "..." : data);
                        }
                        return null;
                    })
                    .doOnComplete(() -> log.debug("DeepSeek 流式生成完成"))
                    .doOnError(error -> log.error("DeepSeek 流式生成失败", error));
        } catch (Exception e) {
            log.error("DeepSeek 流式生成调用失败", e);
            return Flux.error(e);
        }
    }

    /**
     * 将 LangChain4j ChatMessage 角色映射为 OpenAI API 角色。
     */
    private String mapRole(ChatMessage message) {
        return switch (message.type()) {
            case SYSTEM -> "system";
            case USER -> "user";
            case AI -> "assistant";
            default -> "user";
        };
    }

    /**
     * 从 ChatMessage 中提取文本内容。
     */
    private String extractContent(ChatMessage message) {
        return switch (message) {
            case SystemMessage sys -> sys.text();
            case UserMessage user -> user.singleText();
            case AiMessage ai -> ai.text();
            default -> "";
        };
    }
}
