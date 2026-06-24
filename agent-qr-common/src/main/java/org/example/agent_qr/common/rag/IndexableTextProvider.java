package org.example.agent_qr.common.rag;

import java.util.List;

/**
 * 可索引文本数据提供者接口 — 解耦 BM25Retriever 对 ChunkMapper 的直接依赖。
 * <p>
 * 定义在 agent-qr-common，由 knowledge 模块实现（包装 ChunkMapper），
 * rag 模块的 {@code BM25Retriever} 通过本接口获取待索引数据，
 * 遵循依赖倒置原则（DIP）。
 * </p>
 *
 * @author agent-qr
 * @see IndexableText
 */
@FunctionalInterface
public interface IndexableTextProvider {

    /**
     * 获取所有待索引的文本切片。
     *
     * @return 可索引文本列表
     */
    List<IndexableText> findAllIndexable();
}
