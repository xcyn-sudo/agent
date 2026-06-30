package org.example.agent_qr.rag.provider.dashscope;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.rag.provider.EmbeddingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DashScope（阿里云千问）Embedding Provider。
 * <p>
 * 通过 DashScope OpenAI 兼容端点 /v1/embeddings 提供文本向量化能力。
 * 与 {@link DashScopeLLMProvider} 共用同一 API Key 和 Base URL。
 * 默认使用 text-embedding-v3 模型（1024 维）。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class DashScopeEmbeddingProvider implements EmbeddingProvider {

    @Value("${dashscope.llm.api-key:${DASHSCOPE_API_KEY:}}")
    private String apiKey;

    @Value("${dashscope.llm.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${dashscope.embedding.model:text-embedding-v3}")
    private String model;

    private final WebClient webClient = WebClient.create();

    @Override
    public float[] embed(String text) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "input", text
            );

            Map response = webClient.post()
                    .uri(baseUrl + "/embeddings")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.get("data") instanceof List<?> dataList
                    && !dataList.isEmpty() && dataList.get(0) instanceof Map dataItem
                    && dataItem.get("embedding") instanceof List<?> embeddingList) {
                float[] vector = new float[embeddingList.size()];
                for (int i = 0; i < embeddingList.size(); i++) {
                    Object val = embeddingList.get(i);
                    vector[i] = val instanceof Number num ? num.floatValue() : 0f;
                }
                log.debug("DashScope Embedding 成功，维度: {}", vector.length);
                return vector;
            }
            throw new RuntimeException("DashScope Embedding 返回格式异常: " + response);
        } catch (Exception e) {
            log.error("DashScope Embedding 调用失败", e);
            throw new RuntimeException("DashScope Embedding 调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }
}
