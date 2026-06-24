package org.example.agent_qr.rag.provider;

import java.util.List;

/**
 * 向量化策略接口，定义文本向量化的统一抽象。
 * <p>
 * 不同 Embedding 服务商（如 DeepSeek、OpenAI 等）通过实现此接口来提供
 * 单条文本和批量文本的向量化能力。
 * </p>
 *
 * @author agent-qr
 */
public interface EmbeddingProvider {

    /**
     * 将单条文本转换为向量。
     *
     * @param text 待向量化的文本
     * @return 向量数组
     */
    float[] embed(String text);

    /**
     * 批量将多条文本转换为向量列表。
     *
     * @param texts 待向量化的文本列表
     * @return 向量数组列表
     */
    List<float[]> embedBatch(List<String> texts);
}
