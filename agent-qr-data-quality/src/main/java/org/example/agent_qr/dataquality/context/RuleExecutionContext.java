package org.example.agent_qr.dataquality.context;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 规则执行上下文 — 基于 ThreadLocal 传递每次质检调用的数据源级配置。
 * <p>
 * 在 {@code DataQualityService} 中设置，在 {@code CompletenessRule} 等规则中读取。
 * 每个 @Async 线程拥有独立副本，天然线程安全。
 * 必须在 finally 块中调用 {@link #remove()} 清理，防止线程池内存泄漏。
 * </p>
 *
 * @author agent-qr
 */
@Component
public class RuleExecutionContext {

    private final ThreadLocal<Map<String, Object>> contextHolder = new ThreadLocal<>();

    /**
     * 向当前线程的上下文中放入键值对。
     *
     * @param key   键
     * @param value 值
     */
    public void put(String key, Object value) {
        Map<String, Object> ctx = contextHolder.get();
        if (ctx == null) {
            ctx = new HashMap<>();
            contextHolder.set(ctx);
        }
        ctx.put(key, value);
    }

    /**
     * 从当前线程的上下文中获取值。
     *
     * @param key 键
     * @return 值，未找到返回 null
     */
    public Object get(String key) {
        Map<String, Object> ctx = contextHolder.get();
        return ctx != null ? ctx.get(key) : null;
    }

    /**
     * 从当前线程的上下文中获取带类型的值。
     *
     * @param key  键
     * @param type 期望的类型
     * @param <T>  泛型类型
     * @return 值，未找到或类型不匹配返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 清理当前线程的上下文，防止内存泄漏。
     */
    public void remove() {
        contextHolder.remove();
    }

    /** 上下文键常量：数据源 ID */
    public static final String KEY_DATASOURCE_ID = "datasourceId";

    /** 上下文键常量：完整性检查字段列表（逗号分隔的字符串） */
    public static final String KEY_CONTENT_FIELDS = "contentFields";
}
