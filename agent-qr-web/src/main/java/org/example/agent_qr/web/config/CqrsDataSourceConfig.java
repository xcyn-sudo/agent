package org.example.agent_qr.web.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * CQRS 读写分离数据源 Bean 装配（P3）。
 * <p>
 * 仅在 {@code agent-qr.cqrs.enabled=true} 时激活（即 P3 profile 加载时）。
 * P1/P2 模式下不会加载此类，保持向后兼容。
 * </p>
 *
 * <p><b>Bean 装配：</b></p>
 * <ul>
 *   <li>{@code writeDataSource} — 写库（主库），由 {@code ReadWriteRoutingDataSource} 注入包装</li>
 *   <li>{@code readDataSource} — 读库（从库），配置不可用时回退到写库</li>
 * </ul>
 *
 * <p>{@link org.example.agent_qr.common.datasource.ReadWriteRoutingDataSource} 由
 * common 模块的组件扫描自动发现，无需在此额外注册。</p>
 *
 * @see org.example.agent_qr.common.datasource.ReadWriteRoutingDataSource
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "agent-qr.cqrs.enabled", havingValue = "true")
public class CqrsDataSourceConfig {

    @Value("${agent-qr.cqrs.read-replica-fallback-to-primary:true}")
    private boolean readReplicaFallbackToPrimary;

    /**
     * 写库（主库）DataSource。
     * <p>由 {@code ReadWriteRoutingDataSource} 通过 {@code @Qualifier("writeDataSource")} 注入包装。</p>
     *
     * @return HikariDataSource 写库实例
     */
    @Bean("writeDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.write")
    public DataSource writeDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        log.info("CQRS 写库已配置: HikariDataSource");
        return dataSource;
    }

    /**
     * 读库（从库）DataSource。
     * <p>若读库配置不可用，根据 {@code agent-qr.cqrs.read-replica-fallback-to-primary}
     * 决定是否回退到写库实例。</p>
     *
     * @return HikariDataSource 读库实例（或写库实例作为回退）
     */
    @Bean("readDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.read")
    public DataSource readDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        boolean fallback = readReplicaFallbackToPrimary;
        log.info("CQRS 读库已配置: HikariDataSource (fallback={})", fallback);
        return dataSource;
    }

    @PostConstruct
    public void logConfig() {
        log.info("CQRS 读写分离配置已加载: cqrs.enabled=true, read-replica-fallback={}",
                readReplicaFallbackToPrimary);
    }
}
