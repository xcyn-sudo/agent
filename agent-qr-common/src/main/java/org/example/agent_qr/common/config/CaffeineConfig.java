package org.example.agent_qr.common.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置。
 * <p>
 * 提供应用级本地缓存 Bean，用于 LLM 响应缓存等场景，
 * 最大 10000 条，写入后 1 小时过期。
 * </p>
 *
 * @author agent-qr
 */
@Configuration
public class CaffeineConfig {

    /**
     * LLM 响应缓存 Bean。
     * <p>
     * Key 为查询文本的 hash，Value 为 LLM 生成的回答文本。
     * </p>
     *
     * @return Caffeine Cache 实例
     */
    @Bean
    public Cache<String, String> llmResponseCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }
}
