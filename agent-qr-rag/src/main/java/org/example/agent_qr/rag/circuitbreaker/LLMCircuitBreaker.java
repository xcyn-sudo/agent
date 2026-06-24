package org.example.agent_qr.rag.circuitbreaker;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.rag.provider.LLMProvider;
import org.example.agent_qr.rag.provider.ProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * LLM 熔断器 — 状态机保护。
 * <p>
 * 状态转换：
 * <pre>
 *   CLOSED ──(连续失败达阈值)──→ OPEN
 *   OPEN ──(等待 openDurationMs)──→ HALF_OPEN
 *   HALF_OPEN ──(成功)──→ CLOSED (重置)
 *   HALF_OPEN ──(失败)──→ OPEN (重新计时)
 * </pre>
 * OPEN 状态期间自动降级到备用 Provider。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class LLMCircuitBreaker {

    /** 熔断器状态枚举 */
    public enum State { CLOSED, OPEN, HALF_OPEN }

    @Autowired
    private ProviderFactory providerFactory;

    /** 失败阈值，默认 3 次 */
    @Value("${agent-qr.circuit-breaker.failure-threshold:3}")
    private int failureThreshold;

    /** 熔断打开持续时间（毫秒），默认 30 秒 */
    @Value("${agent-qr.circuit-breaker.open-duration-ms:30000}")
    private long openDurationMs;

    /** 当前状态 */
    private volatile State state = State.CLOSED;

    /** 连续失败计数 */
    private final AtomicInteger failureCount = new AtomicInteger(0);

    /** 熔断打开的时间戳 */
    private volatile long openTimestamp = 0;

    /**
     * 获取当前可用的 LLM Provider。
     * <p>
     * CLOSED → 返回默认 Provider<br>
     * OPEN → 检查是否已过 openDurationMs，到期则转为 HALF_OPEN 并探测；未到期则降级<br>
     * HALF_OPEN → 使用默认 Provider 探测
     * </p>
     *
     * @return 可用的 LLM Provider
     */
    public LLMProvider getActiveProvider() {
        switch (state) {
            case CLOSED:
                return providerFactory.getLLMProvider();

            case OPEN:
                if (System.currentTimeMillis() - openTimestamp >= openDurationMs) {
                    state = State.HALF_OPEN;
                    log.info("熔断器: OPEN → HALF_OPEN（探测恢复）");
                    return providerFactory.getLLMProvider();
                }
                // 降级
                log.warn("熔断器 OPEN，降级到备用 Provider");
                return providerFactory.getFallbackLLMProvider();

            case HALF_OPEN:
                return providerFactory.getLLMProvider();

            default:
                return providerFactory.getLLMProvider();
        }
    }

    /**
     * 记录成功调用。
     * <p>
     * HALF_OPEN 状态下成功 → 转 CLOSED 并重置计数。
     * </p>
     */
    public void recordSuccess() {
        if (state == State.HALF_OPEN) {
            state = State.CLOSED;
            failureCount.set(0);
            log.info("熔断器: HALF_OPEN → CLOSED（恢复成功）");
        }
        // CLOSED 状态下成功，重置失败计数
        if (state == State.CLOSED) {
            failureCount.set(0);
        }
    }

    /**
     * 记录调用失败。
     * <p>
     * CLOSED 状态下失败计数达阈值 → 转 OPEN<br>
     * HALF_OPEN 状态下失败 → 立即转 OPEN
     * </p>
     */
    public void recordFailure() {
        switch (state) {
            case CLOSED:
                int count = failureCount.incrementAndGet();
                if (count >= failureThreshold) {
                    state = State.OPEN;
                    openTimestamp = System.currentTimeMillis();
                    log.warn("熔断器: CLOSED → OPEN（连续失败 {} 次，达到阈值 {}）",
                            count, failureThreshold);
                }
                break;

            case HALF_OPEN:
                state = State.OPEN;
                openTimestamp = System.currentTimeMillis();
                log.warn("熔断器: HALF_OPEN → OPEN（探测失败）");
                break;

            case OPEN:
                // 已经是 OPEN，重置计时器
                openTimestamp = System.currentTimeMillis();
                break;
        }
    }

    /**
     * 获取当前熔断器状态（供监控使用）。
     */
    public State getState() {
        return state;
    }

    /**
     * 获取当前失败计数。
     */
    public int getFailureCount() {
        return failureCount.get();
    }
}
