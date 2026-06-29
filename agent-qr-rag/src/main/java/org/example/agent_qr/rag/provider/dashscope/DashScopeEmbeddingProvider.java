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
 * 阿里云 DashScope (千问) Embedding Provider。
 * <p>
 * 使用 DashScope OpenAI 兼容模式提供文本向量化能力，
 * 替代本地 Ollama 以节省 ECS 内存。
 * </p>
 * <p>
 * API 文档: https://help.aliyun.com/document_detail/2712519.html
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class DashScopeEmbeddingProvider implements EmbeddingProvider {

    @Value("${dashscope.embedding.api-key:${DASHSCOPE_API_KEY:}}")
    private String apiKey;

    @Value("${dashscope.embedding.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
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
                    && !dataList.isEmpty() && dataList.get(0) instanceof Map<?, ?> firstItem) {
                Object embeddingObj = firstItem.get("embedding");
                if (embeddingObj instanceof List<?> embeddingList) {
                    float[] vector = new float[embeddingList.size()];
                    for (int i = 0; i < embeddingList.size(); i++) {
                        Object val = embeddingList.get(i);
                        vector[i] = val instanceof Number num ? num.floatValue() : 0f;
                    }
                    log.debug("DashScope Embedding 成功，维度: {}", vector.length);
                    return vector;
                }
            }
            throw new RuntimeException("DashScope Embedding 返回结果为空或格式异常: response=" + response);
        } catch (Exception e) {
            log.error("DashScope Embedding 调用失败", e);
            throw new RuntimeException("DashScope Embedding 调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "input", texts  // DashScope 支持批量输入
            );

            Map response = webClient.post()
                    .uri(baseUrl + "/embeddings")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.get("data") instanceof List<?> dataList) {
                List<float[]> results = new ArrayList<>();
                for (Object item : dataList) {
                    if (item instanceof Map<?, ?> dataItem
                            && dataItem.get("embedding") instanceof List<?> embeddingList) {
                        float[] vector = new float[embeddingList.size()];
                        for (int i = 0; i < embeddingList.size(); i++) {
                            Object val = embeddingList.get(i);
                            vector[i] = val instanceof Number num ? num.floatValue() : 0f;
                        }
                        results.add(vector);
                    }
                }
                log.debug("DashScope 批量 Embedding 成功，条数: {}/{}", results.size(), texts.size());
                return results;
            }
            throw new RuntimeException("DashScope 批量 Embedding 返回结果为空或格式异常");
        } catch (Exception e) {
            log.error("DashScope 批量 Embedding 调用失败，回退到逐条调用", e);
            // 降级：逐条调用
            List<float[]> results = new ArrayList<>();
            for (String text : texts) {
                results.add(embed(text));
            }
            return results;
        }
    }
}
