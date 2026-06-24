package org.example.agent_qr.common.rag;

/**
 * 可向量化文本接口 — 解耦 agent-qr-rag 与 agent-qr-knowledge 之间的循环依赖。
 * <p>
 * 定义在 agent-qr-common，由 knowledge 模块的实体类（如 {@code Chunk}）实现，
 * rag 模块的向量化服务（如 {@code BatchEmbeddingService}）面向本接口编程，
 * 避免 rag 反向依赖 knowledge。
 * </p>
 *
 * <p><b>设计模式：</b>依赖倒置原则（DIP）—— 高层模块（rag）不依赖低层模块（knowledge），
 * 二者共同依赖抽象（EmbeddableText）。与 §10.1 中
 * {@code DocumentDeleteRequestedEvent} 解耦 knowledge↔compensation 的模式一致。</p>
 *
 * @author agent-qr
 * @see org.example.agent_qr.rag.embedding.BatchEmbeddingService
 */
@FunctionalInterface
public interface EmbeddableText {

    /**
     * 获取待向量化的文本内容。
     *
     * @return 文本内容，不可为 null
     */
    String getContent();
}
