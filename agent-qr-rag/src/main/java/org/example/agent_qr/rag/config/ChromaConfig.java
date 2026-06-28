package org.example.agent_qr.rag.config;

import dev.langchain4j.store.embedding.chroma.ChromaApiVersion;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * ChromaDB 向量存储配置，手动创建 {@link ChromaEmbeddingStore} Bean。
 * <p>
 * langchain4j-chroma 目前没有 Spring Boot 自动配置，
 * 因此需要手动通过 Builder 构造并注册为 Spring Bean。
 * </p>
 * <p>
 * 在 Bean 初始化之前，通过 ChromaDB REST API 确保 collection
 * 使用余弦相似度（cosine）而非默认的 L2 距离度量。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Configuration
public class ChromaConfig {

    @Value("${langchain4j.chroma.base-url:http://localhost:8000}")
    private String baseUrl;

    @Value("${langchain4j.chroma.collection-name:enterprise_knowledge}")
    private String collectionName;

    @Value("${langchain4j.chroma.timeout-seconds:30}")
    private long timeoutSeconds;

    /**
     * 在 ChromaEmbeddingStore Bean 创建之前，确保 collection
     * 使用 cosine 距离度量。如果 collection 已存在，不做修改
     * （ChromaDB 的 distance metric 在创建后不可更改）。
     */
    @PostConstruct
    public void ensureCosineDistance() {
        WebClient client = WebClient.create(baseUrl);
        try {
            // 检查 collection 是否已存在
            String collectionId = client.get()
                    .uri("/api/v2/collections/{name}", collectionName)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, resp -> {
                        log.info("ChromaDB collection '{}' 不存在，将创建为 cosine 距离度量", collectionName);
                        return Mono.empty();
                    })
                    .bodyToMono(Map.class)
                    .map(body -> Objects.toString(body.get("id"), null))
                    .onErrorReturn("")
                    .block(Duration.ofSeconds(10));

            if (collectionId == null || collectionId.isEmpty()) {
                // Collection 不存在 → 创建时指定 cosine
                Map<String, Object> requestBody = Map.of(
                        "name", collectionName,
                        "metadata", Map.of("hnsw:space", "cosine")
                );
                Map<String, Object> response = client.post()
                        .uri("/api/v2/collections")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block(Duration.ofSeconds(10));

                log.info("ChromaDB collection '{}' 已创建，distance metric=cosine, response={}",
                        collectionName, response);
            } else {
                log.info("ChromaDB collection '{}' 已存在 (id={})，跳过创建。"
                                + "注意：如果现有 collection 使用 L2 距离，"
                                + "需手动删除后重建以获得更好的语义检索效果。",
                        collectionName, collectionId);
            }
        } catch (Exception e) {
            log.warn("ChromaDB collection 初始化失败 (baseUrl={}, collection={}): {}。"
                            + "将回退到 langchain4j 默认行为。",
                    baseUrl, collectionName, e.getMessage());
        }
    }

    @Bean
    public ChromaEmbeddingStore chromaEmbeddingStore() {
        log.info("初始化 ChromaEmbeddingStore: baseUrl={}, collectionName={}, timeout={}s",
                baseUrl, collectionName, timeoutSeconds);
        return ChromaEmbeddingStore.builder()
                .apiVersion(ChromaApiVersion.V2)
                .baseUrl(baseUrl)
                .collectionName(collectionName)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
