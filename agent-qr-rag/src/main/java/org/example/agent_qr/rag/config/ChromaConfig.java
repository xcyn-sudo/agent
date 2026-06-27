package org.example.agent_qr.rag.config;

import dev.langchain4j.store.embedding.chroma.ChromaApiVersion;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * ChromaDB 向量存储配置，手动创建 {@link ChromaEmbeddingStore} Bean。
 * <p>
 * langchain4j-chroma 目前没有 Spring Boot 自动配置，
 * 因此需要手动通过 Builder 构造并注册为 Spring Bean。
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
