package org.example.agent_qr.common.rag;

/**
 * 可索引文本接口 — 扩展 {@link EmbeddableText}，增加 Lucene 索引所需的标识字段。
 * <p>
 * 定义在 agent-qr-common，由 knowledge 模块的实体类（如 {@code Chunk}）实现，
 * rag 模块的检索器（如 {@code BM25Retriever}）面向本接口编程，
 * 避免 rag 反向依赖 knowledge。
 * </p>
 *
 * @author agent-qr
 * @see EmbeddableText
 * @see org.example.agent_qr.rag.retriever.BM25Retriever
 */
public interface IndexableText extends EmbeddableText {

    /**
     * 获取切片唯一标识。
     *
     * @return 切片 ID
     */
    Long getId();

    /**
     * 获取切片在文档中的序号（从 0 开始）。
     *
     * @return 切片序号
     */
    Integer getChunkIndex();
}
