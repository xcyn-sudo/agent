package org.example.agent_qr.rag.provider.ollama;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.rag.provider.LLMProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Ollama LLM 提供商实现。
 * <p>
 * 通过 Ollama 本地部署的 /api/chat 接口提供同步和流式生成能力。
 * 默认使用 qwen2.5:7b 模型。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class OllamaLLMProvider implements LLMProvider {

    @Value("${ollama.llm.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.llm.model:qwen2.5:7b}")
    private String model;

    private final WebClient webClient = WebClient.create();

    @Override
    public String generate(List<ChatMessage> messages) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", messages.stream()
                            .map(m -> Map.of("role", mapRole(m), "content", extractContent(m)))
                            .toList(),
                    "stream", false
            );

            Map response = webClient.post()
                    .uri(baseUrl + "/api/chat")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.get("message") instanceof Map msg) {
                Object content = msg.get("content");
                return content != null ? content.toString() : "";
            }
            return "";
        } catch (Exception e) {
            log.error("Ollama LLM 同步调用失败", e);
            return "Ollama 调用失败: " + e.getMessage();
        }
    }

    @Override
    public Flux<String> generateStream(List<ChatMessage> messages) {
        return Flux.create(sink -> {
            try {
                Map<String, Object> requestBody = Map.of(
                        "model", model,
                        "messages", messages.stream()
                                .map(m -> Map.of("role", mapRole(m), "content", extractContent(m)))
                                .toList(),
                        "stream", true
                );

                webClient.post()
                        .uri(baseUrl + "/api/chat")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToFlux(Map.class)
                        .doOnNext(chunk -> {
                            if (chunk.get("message") instanceof Map msg) {
                                Object content = msg.get("content");
                                if (content != null) {
                                    sink.next(content.toString());
                                }
                            }
                            if (Boolean.TRUE.equals(chunk.get("done"))) {
                                sink.complete();
                            }
                        })
                        .doOnError(sink::error)
                        .doOnComplete(sink::complete)
                        .subscribe();
            } catch (Exception e) {
                log.error("Ollama LLM 流式调用失败", e);
                sink.error(e);
            }
        });
    }

    /**
     * 将 LangChain4j ChatMessage 角色映射为 Ollama API 角色。
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
     * <p>
     * langchain4j 1.4.0 中 ChatMessage 基接口没有 text() 方法，
     * 需根据子类型分别提取：AiMessage/SystemMessage → text()，UserMessage → singleText()。
     * </p>
     *
     * @param message 聊天消息
     * @return 消息文本内容
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
