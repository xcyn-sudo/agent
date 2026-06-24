package org.example.agent_qr.rag.provider.deepseek;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.BusinessException;
import org.example.agent_qr.rag.provider.EmbeddingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * DeepSeek Embedding 服务提供商实现。
 * <p>
 * 基于 LangChain4j 的 OpenAiEmbeddingModel 调用 DeepSeek Embedding API，
 * 支持单条和批量文本向量化。P1 阶段批量向量化采用循环逐条调用方式。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class DeepSeekEmbeddingProvider implements EmbeddingProvider {

    @Value("${embedding.deepseek.api-key}")
    private String apiKey;

    @Value("${embedding.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${embedding.deepseek.model:deepseek-embedding}")
    private String model;

    private OpenAiEmbeddingModel embeddingModel;

    /**
     * 初始化 OpenAiEmbeddingModel 实例。
     */
    @PostConstruct
    public void init() {
        log.info("初始化 DeepSeek Embedding Provider，baseUrl={}, model={}", baseUrl, model);
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .build();
    }

    /**
     * 将单条文本转换为向量。
     *
     * @param text 待向量化的文本
     * @return 向量数组
     * @throws BusinessException 当向量化服务调用失败时
     */
    @Override
    public float[] embed(String text) {
        try {
            Embedding embedding = embeddingModel.embed(text).content();
            float[] vector = embedding.vector();
            log.debug("文本向量化成功，维度: {}", vector.length);
            return vector;
        } catch (Exception e) {
            log.error("DeepSeek Embedding 调用失败", e);
            throw new BusinessException("向量化服务暂时不可用");
        }
    }

    /**
     * 批量将多条文本转换为向量列表。
     * <p>
     * P1 基础版实现：循环逐条调用 embed() 方法。
     * </p>
     *
     * @param texts 待向量化的文本列表
     * @return 向量数组列表
     */
    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> vectors = new ArrayList<>();
        for (String text : texts) {
            vectors.add(embed(text));
        }
        return vectors;
    }
}
