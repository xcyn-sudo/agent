package org.example.agent_qr.rag.provider;

import dev.langchain4j.data.message.ChatMessage;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * LLM 策略接口，定义大语言模型调用的统一抽象。
 * <p>
 * 不同 LLM 服务商（如 DeepSeek、OpenAI 等）通过实现此接口来提供
 * 同步生成和流式生成能力。P1 阶段仅使用同步生成，流式生成为默认空实现。
 * </p>
 *
 * @author agent-qr
 */
public interface LLMProvider {

    /**
     * 根据消息列表同步生成回复。
     *
     * @param messages 聊天消息列表（通常包含 SystemMessage 和 UserMessage）
     * @return LLM 生成的回复文本
     */
    String generate(List<ChatMessage> messages);

    /**
     * 根据消息列表流式生成回复（P1 默认空实现）。
     *
     * @param messages 聊天消息列表
     * @return 空的 Flux 流
     */
    default Flux<String> generateStream(List<ChatMessage> messages) {
        return Flux.empty();
    }
}
