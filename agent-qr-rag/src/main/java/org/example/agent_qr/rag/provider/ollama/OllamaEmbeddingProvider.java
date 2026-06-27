package org.example.agent_qr.rag.provider.ollama;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.rag.provider.EmbeddingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ollama Embedding 提供商实现（P2 优化版）。
 * <p>
 * 通过 Ollama 本地部署的 /api/embeddings 接口提供文本向量化能力。
 * 默认使用 nomic-embed-text 模型。Ollama 原生不支持批量 embedding，
 * 因此 embedBatch 通过循环调用 embed 实现。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    @Value("${ollama.embedding.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.embedding.model:nomic-embed-text}")
    private String model;

    private final WebClient webClient = WebClient.create();

    @Override
    public float[] embed(String text) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "prompt", text
            );

            Map response = webClient.post()
                    .uri(baseUrl + "/api/embeddings")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.get("embedding") instanceof List<?> embeddingList) {
                float[] vector = new float[embeddingList.size()];
                for (int i = 0; i < embeddingList.size(); i++) {
                    Object val = embeddingList.get(i);
                    vector[i] = val instanceof Number num ? num.floatValue() : 0f;
                }
                log.debug("Ollama Embedding 成功，维度: {}", vector.length);
                return vector;
            }
            throw new RuntimeException("Ollama Embedding 返回结果为空或格式异常: response=" + response);
        } catch (Exception e) {
            log.error("Ollama Embedding 调用失败", e);
            throw new RuntimeException("Ollama Embedding 调用失败: " + e.getMessage(), e);
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
