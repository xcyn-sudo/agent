package org.example.agent_qr.rag.provider;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.BusinessException;
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
 * P3 扩展：集成 {@link ProviderDecisionEngine}，根据熔断器状态自动切换 Provider。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class ProviderFactory {

    @Value("${llm.provider:deepseek}")
    private String llmProviderType;

    @Value("${embedding.provider:ollama}")
    private String embeddingProviderType;

    /** P3 新增：Provider 自动切换决策引擎（可选） */
    @Autowired(required = false)
    private ProviderDecisionEngine decisionEngine;

    @Autowired(required = false)
    private DeepSeekLLMProvider deepSeekLLMProvider;

    @Autowired(required = false)
    private OllamaLLMProvider ollamaLLMProvider;

    @Autowired(required = false)
    private OllamaEmbeddingProvider ollamaEmbeddingProvider;

    /**
     * 根据配置获取 LLM 提供商实例（P2 扩展：支持 ollama；P3：集成决策引擎）。
     */
    public LLMProvider getLLMProvider() {
        String provider;
        if (decisionEngine != null) {
            provider = decisionEngine.decideLLMProvider();  // P3 决策引擎优先
        } else {
            provider = llmProviderType;  // 降级到配置值
        }
        LLMProvider llmProvider = switch (provider) {
            case "deepseek" -> deepSeekLLMProvider;
            case "ollama" -> ollamaLLMProvider;
            default -> {
                log.warn("未知的 LLM Provider 配置: {}，回退到 DeepSeek", provider);
                yield deepSeekLLMProvider;
            }
        };
        if (llmProvider == null) {
            log.error("LLM 服务提供商未配置，当前配置: {}", provider);
            throw new BusinessException("LLM 服务提供商未配置");
        }
        return llmProvider;
    }

    /**
     * 根据配置获取 Embedding 提供商实例（P2 扩展：支持 ollama；P3：集成决策引擎）。
     */
    public EmbeddingProvider getEmbeddingProvider() {
        String provider;
        if (decisionEngine != null) {
            provider = decisionEngine.decideEmbeddingProvider();  // P3 决策引擎优先
        } else {
            provider = embeddingProviderType;  // 降级到配置值
        }
        EmbeddingProvider embeddingProvider = switch (provider) {
            case "ollama" -> ollamaEmbeddingProvider;
            default -> {
                log.warn("未知的 Embedding Provider 配置: {}，回退到 Ollama", provider);
                yield ollamaEmbeddingProvider;
            }
        };
        if (embeddingProvider == null) {
            log.error("Embedding 服务提供商未配置，当前配置: {}", provider);
            throw new BusinessException("Embedding 服务提供商未配置");
        }
        return embeddingProvider;
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

    /**
     * 获取当前 LLM Provider 类型标识（P3 新增）。
     *
     * @return 当前 LLM Provider 类型（"deepseek" 或 "ollama"）
     */
    public String getLLMProviderType() {
        if (decisionEngine != null) {
            return decisionEngine.decideLLMProvider();
        }
        return llmProviderType;
    }

    /**
     * 获取当前 Embedding Provider 类型标识（P3 新增）。
     *
     * @return 当前 Embedding Provider 类型（"deepseek" 或 "ollama"）
     */
    public String getEmbeddingProviderType() {
        if (decisionEngine != null) {
            return decisionEngine.decideEmbeddingProvider();
        }
        return embeddingProviderType;
    }

    /**
     * 获取降级 LLM Provider 类型标识（P3 新增）。
     *
     * @return 降级 Provider 类型
     */
    public String getFallbackLLMProviderType() {
        return "deepseek".equals(llmProviderType) ? "ollama" : "deepseek";
    }

    /**
     * 获取当前 Embedding 模型名称（P3 新增）。
     * <p>从 P1/P2 配置中读取: {@code embedding.ollama.model} 或 {@code embedding.deepseek.model}。</p>
     *
     * @return 当前 Embedding 模型名称
     */
    public String getEmbeddingModelName() {
        // 由 EmbeddingProvider 具体实现提供模型名
        // 此处返回 Provider 类型作为后备
        return getEmbeddingProviderType();
    }
}
