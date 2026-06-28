package org.example.agent_qr.common.datasource;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * CQRS 读写分离数据源路由（P3）。
 * <p>
 * 继承 Spring {@link AbstractRoutingDataSource}，基于 ThreadLocal 实现
 * 读写数据源的动态路由。配合 {@link ReadWriteDataSourceAspect} 切面使用，
 * 从 {@code @Transactional(readOnly)} 注解读取读写标志。
 * </p>
 *
 * <p><b>路由规则：</b></p>
 * <ul>
 *   <li>{@code readOnly=true} → lookup key {@code "read"} → 读库</li>
 *   <li>{@code readOnly=false} → lookup key {@code "write"} → 写库（默认）</li>
 * </ul>
 *
 * <p><b>数据源来源：</b></p>
 * <p>
 * 通过构造函数注入 {@link org.example.agent_qr.web.config.CqrsDataSourceConfig}
 * 创建的 writeDataSource / readDataSource Bean，不再自行构建数据源，
 * 避免重复创建 HikariCP 连接池。
 * </p>
 *
 * <p><b>激活条件：</b></p>
 * <p>
 * 仅在 {@code agent-qr.cqrs.enabled=true}（即 P3 profile）时激活。
 * P1/P2 模式由 Spring Boot 自动配置的 DataSource 接管。
 * </p>
 *
 * @see ReadWriteDataSourceAspect
 * @see AbstractRoutingDataSource
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "agent-qr.cqrs.enabled", havingValue = "true")
public class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {

    /** ThreadLocal 读写标志：{@code true} 表示只读，{@code false} 表示读写 */
    private static final ThreadLocal<Boolean> READ_ONLY_HOLDER = new ThreadLocal<>();

    /** 写库 DataSource（由 CqrsDataSourceConfig 注入） */
    private final DataSource writeDataSource;

    /** 读库 DataSource（由 CqrsDataSourceConfig 注入） */
    private final DataSource readDataSource;

    /**
     * 构造函数注入读写数据源。
     *
     * @param writeDataSource 写库（主库），来自 {@code CqrsDataSourceConfig.writeDataSource()}
     * @param readDataSource  读库（从库），来自 {@code CqrsDataSourceConfig.readDataSource()}
     */
    public ReadWriteRoutingDataSource(
            @Qualifier("writeDataSource") DataSource writeDataSource,
            @Qualifier("readDataSource") DataSource readDataSource) {
        this.writeDataSource = writeDataSource;
        this.readDataSource = readDataSource;
    }

    /**
     * 设置当前线程的读写标志。
     *
     * @param readOnly {@code true} 表示只读路由到读库，{@code false} 表示路由到写库
     */
    public static void setReadOnly(boolean readOnly) {
        READ_ONLY_HOLDER.set(readOnly);
    }

    /**
     * 清除当前线程的读写标志，防止线程池中的 ThreadLocal 泄漏。
     * 必须在 finally 块中调用。
     */
    public static void clear() {
        READ_ONLY_HOLDER.remove();
    }

    /**
     * 根据 ThreadLocal 中的读写标志返回数据源 lookup key。
     *
     * @return {@code "read"} 或 {@code "write"}
     */
    @Override
    protected Object determineCurrentLookupKey() {
        boolean readOnly = Boolean.TRUE.equals(READ_ONLY_HOLDER.get());
        return readOnly ? "read" : "write";
    }

    /**
     * 初始化读写数据源 Map。
     * <p>
     * 在 Spring Bean 初始化后调用，将构造函数注入的读写数据源
     * 组装为 targetDataSources Map。写库为默认数据源。
     * </p>
     */
    @PostConstruct
    public void initDataSources() {
        Map<Object, Object> targetDataSources = new HashMap<>();

        targetDataSources.put("write", writeDataSource);
        targetDataSources.put("read", readDataSource);

        this.setTargetDataSources(targetDataSources);
        this.setDefaultTargetDataSource(writeDataSource);

        // AbstractRoutingDataSource 要求调用 afterPropertiesSet()
        this.afterPropertiesSet();

        log.info("CQRS 读写分离数据源已初始化: write={}, read={}",
                getDataSourceUrl(writeDataSource),
                getDataSourceUrl(readDataSource));
    }

    /**
     * 安全获取 DataSource 的 JDBC URL（用于日志输出）。
     *
     * @param dataSource 数据源实例
     * @return JDBC URL 字符串，无法获取时返回 "unknown"
     */
    private String getDataSourceUrl(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikari) {
            return hikari.getJdbcUrl();
        }
        return "unknown";
    }
}
