package org.example.agent_qr.catalog.router;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.catalog.entity.CatalogTree;
import org.example.agent_qr.catalog.entity.DomainNode;
import org.example.agent_qr.catalog.service.KnowledgeCatalogService;
import org.example.agent_qr.common.event.DataSyncCompletedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 目录语义域路由 V2 — Catalog 版（P3）。
 * <p>
 * 职责：基于目录树动态生成域的自然语言描述并缓存。
 * 不直接依赖 agent-qr-rag 模块（避免循环依赖），
 * rag 模块的 {@code DomainRouterV2} 通过 {@link KnowledgeCatalogService}
 * 获取目录树后自行构建域描述和 Embedding。
 * </p>
 *
 * <p><b>与 rag 版 DomainRouterV2 的关系：</b></p>
 * <ul>
 *   <li>Catalog 版：负责域描述文本的生成与缓存</li>
 *   <li>Rag 版：负责使用 Embedding 做语义路由匹配</li>
 *   <li>两者通过 {@link KnowledgeCatalogService#getCatalogTree()} 解耦</li>
 * </ul>
 *
 * <p><b>刷新策略：</b></p>
 * <ul>
 *   <li>定时刷新：每 5 分钟重新构建域描述</li>
 *   <li>事件驱动：监听 {@link DataSyncCompletedEvent}，标记缓存过期（下次访问时懒加载）</li>
 * </ul>
 *
 * @see KnowledgeCatalogService
 */
@Slf4j
@Component("catalogDomainRouterV2")
public class DomainRouterV2 {

    @Autowired
    private KnowledgeCatalogService catalogService;

    /** 缓存的域描述文本：域ID → 自然语言描述 */
    private volatile Map<String, String> cachedDomainDescriptions = new ConcurrentHashMap<>();

    /** 上次刷新时间戳 */
    private volatile long lastRefreshTime = 0;

    /** 缓存过期时间（毫秒），默认 5 分钟 */
    private static final long STALE_THRESHOLD_MS = 300_000;

    /**
     * 初始化：首次加载域描述。
     */
    @PostConstruct
    public void init() {
        refresh();
    }

    /**
     * 获取缓存的域描述文本 Map。
     * <p>若缓存为空或已过期（超过 5 分钟），自动触发刷新。</p>
     *
     * @return 域ID → 自然语言描述的不可修改 Map
     */
    public Map<String, String> getDomainDescriptions() {
        if (cachedDomainDescriptions.isEmpty() || isStale()) {
            refresh();
        }
        return Collections.unmodifiableMap(cachedDomainDescriptions);
    }

    /**
     * 判断缓存是否已过期（超过 5 分钟未刷新）。
     *
     * @return {@code true} 表示缓存已过期
     */
    private boolean isStale() {
        return System.currentTimeMillis() - lastRefreshTime > STALE_THRESHOLD_MS;
    }

    /**
     * 定时刷新域描述缓存（每 5 分钟）。
     */
    @Scheduled(fixedRate = 300_000)
    public void refresh() {
        try {
            CatalogTree tree = catalogService.getCatalogTree();
            if (tree == null || tree.getDomains() == null || tree.getDomains().isEmpty()) {
                log.debug("目录树为空，跳过域描述刷新");
                return;
            }

            Map<String, String> descriptions = buildDomainDescriptions(tree);
            this.cachedDomainDescriptions = new ConcurrentHashMap<>(descriptions);
            this.lastRefreshTime = System.currentTimeMillis();
            log.info("目录域描述刷新完成: {} 个域", descriptions.size());

        } catch (Exception e) {
            log.error("目录域描述刷新异常", e);
        }
    }

    /**
     * 基于目录树构建各域的自然语言描述。
     * <p>每个域的描述包含：域名 + 前 20 个数据源名称 + 前 50 个实体名称。</p>
     *
     * @param tree 知识目录树
     * @return 域ID → 自然语言描述的 Map
     */
    private Map<String, String> buildDomainDescriptions(CatalogTree tree) {
        Map<String, String> descriptions = new LinkedHashMap<>();
        for (DomainNode domain : tree.getDomains()) {
            StringBuilder sb = new StringBuilder();
            sb.append(domain.getDomainName()).append("，");
            if (domain.getSources() != null) {
                domain.getSources().stream()
                        .limit(20)
                        .forEach(s -> sb.append(s.getSourceName()).append("，"));
                // 追加实体名称（Top 50）
                domain.getSources().stream()
                        .flatMap(s -> s.getEntities() != null
                                ? s.getEntities().stream()
                                : java.util.stream.Stream.empty())
                        .limit(50)
                        .forEach(e -> sb.append(e.getEntityName()).append("，"));
            }
            descriptions.put(domain.getDomainName(), sb.toString());
        }
        return descriptions;
    }

    /**
     * 监听数据同步完成事件，标记缓存过期。
     * <p>不立即刷新（懒加载），下次调用 {@link #getDomainDescriptions()} 时自动重建。</p>
     *
     * @param event 数据同步完成事件
     */
    @EventListener
    public void onCatalogChanged(DataSyncCompletedEvent event) {
        log.info("检测到目录变更事件 (datasourceId={}, sourceName={})，标记域描述缓存过期",
                event.getDatasourceId(), event.getSourceName());
        // 标记为过期，下次访问时懒加载
        this.lastRefreshTime = 0;
    }
}
