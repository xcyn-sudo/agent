package org.example.agent_qr.rag.provider;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.BusinessException;
import org.example.agent_qr.rag.provider.deepseek.DeepSeekEmbeddingProvider;
import org.example.agent_qr.rag.provider.deepseek.DeepSeekLLMProvider;
import org.example.agent_qr.rag.provider.ollama.OllamaEmbeddingProvider;
import org.example.agent_qr.rag.provider.ollama.OllamaLLMProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 策略工厂，根据配置选择对应的 LLM 和 Embedding 服务提供商。
 * <p>
 * P1 原有：支持 DeepSeek 和 Ollama Embedding。
 * P2 扩展：新增 Ollama LLM 支持、getFallbackLLMProvider 降级链路。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class ProviderFactory {

    @Value("${llm.provider:deepseek}")
    private String llmProviderType;

    @Value("${embedding.provider:deepseek}")
    private String embeddingProviderType;

    @Autowired(required = false)
    private DeepSeekLLMProvider deepSeekLLMProvider;

    @Autowired(required = false)
    private DeepSeekEmbeddingProvider deepSeekEmbeddingProvider;

    @Autowired(required = false)
    private OllamaLLMProvider ollamaLLMProvider;

    @Autowired(required = false)
    private OllamaEmbeddingProvider ollamaEmbeddingProvider;

    /**
     * 根据配置获取 LLM 提供商实例（P2 扩展：支持 ollama）。
     */
    public LLMProvider getLLMProvider() {
        LLMProvider provider = switch (llmProviderType) {
            case "deepseek" -> deepSeekLLMProvider;
            case "ollama" -> ollamaLLMProvider;
            default -> {
                log.warn("未知的 LLM Provider 配置: {}，回退到 DeepSeek", llmProviderType);
                yield deepSeekLLMProvider;
            }
        };
        if (provider == null) {
            log.error("LLM 服务提供商未配置，当前配置: {}", llmProviderType);
            throw new BusinessException("LLM 服务提供商未配置");
        }
        return provider;
    }

    /**
     * 根据配置获取 Embedding 提供商实例（P2 扩展：支持 ollama）。
     */
    public EmbeddingProvider getEmbeddingProvider() {
        EmbeddingProvider provider = switch (embeddingProviderType) {
            case "deepseek" -> deepSeekEmbeddingProvider;
            case "ollama" -> ollamaEmbeddingProvider;
            default -> {
                log.warn("未知的 Embedding Provider 配置: {}，回退到 DeepSeek", embeddingProviderType);
                yield deepSeekEmbeddingProvider;
            }
        };
        if (provider == null) {
            log.error("Embedding 服务提供商未配置，当前配置: {}", embeddingProviderType);
            throw new BusinessException("Embedding 服务提供商未配置");
        }
        return provider;
    }

    /**
     * 获取降级 LLM Provider（P2 新增）。
     * <p>
     * 当主 Provider 触发熔断时，返回 DeepSeek 作为降级。
     * 若 DeepSeek 不可用则抛出异常。
     * </p>
     *
     * @return 降级 LLM Provider
     * @throws BusinessException 当所有 Provider 均不可用时
     */
    public LLMProvider getFallbackLLMProvider() {
        if (deepSeekLLMProvider != null) {
            log.info("LLM 降级：切换到 DeepSeek Provider");
            return deepSeekLLMProvider;
        }
        log.error("降级链路不可用：DeepSeek Provider 未配置");
        throw new BusinessException("所有 LLM Provider 均不可用");
    }
}
