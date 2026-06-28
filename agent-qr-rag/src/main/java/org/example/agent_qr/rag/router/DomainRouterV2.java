package org.example.agent_qr.rag.router;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.catalog.dto.DomainRoutingResult;
import org.example.agent_qr.catalog.entity.CatalogTree;
import org.example.agent_qr.catalog.entity.DomainNode;
import org.example.agent_qr.catalog.service.KnowledgeCatalogService;
import org.example.agent_qr.rag.provider.ProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Embedding 语义域路由 V2（P3）。
 * <p>
 * 使用 Embedding 向量的余弦相似度匹配用户问题到业务域，
 * 替代 P2 的 {@link org.example.agent_qr.catalog.router.DomainRouter} 关键词匹配方案。
 * </p>
 *
 * <p><b>路由流程：</b></p>
 * <ol>
 *   <li>预计算各业务域的 Embedding 向量（启动时 + 定时刷新）</li>
 *   <li>用户查询向量化</li>
 *   <li>计算查询向量与各域向量的余弦相似度</li>
 *   <li>取 Top 2 相似度最高的域（相似度需 &gt; 阈值）</li>
 *   <li>无匹配域时回退全局检索</li>
 * </ol>
 *
 * <p><b>与 P2 DomainRouter 共存：</b>P3 优先使用本类，不可用时降级 P2 关键词路由。</p>
 *
 * @see org.example.agent_qr.catalog.router.DomainRouter
 * @see EmbeddingProvider
 */
@Slf4j
@Component("ragDomainRouterV2")
public class DomainRouterV2 {

    @Autowired
    private ProviderFactory providerFactory;

    @Autowired(required = false)
    private KnowledgeCatalogService catalogService;

    /** 相似度阈值（低于此值视为不相关） */
    @Value("${agent-qr.routing.similarity-threshold:0.3}")
    private double similarityThreshold;

    /** Top K 个匹配域 */
    @Value("${agent-qr.routing.top-k:2}")
    private int topK;

    /** 域 Embedding 缓存：域ID → 向量 */
    private final Map<String, float[]> domainEmbeddings = new ConcurrentHashMap<>();

    /** 上次刷新时间戳 */
    private volatile long lastRefreshTime = 0;

    /** 刷新间隔（毫秒） */
    private static final long REFRESH_INTERVAL_MS = 300_000;

    /**
     * 初始化：加载初始域描述并预计算 Embedding 向量。
     */
    @PostConstruct
    public void init() {
        Map<String, String> domainDescriptions = getDomainDescriptions();
        for (Map.Entry<String, String> entry : domainDescriptions.entrySet()) {
            try {
                float[] emb = providerFactory.getEmbeddingProvider().embed(entry.getValue());
                domainEmbeddings.put(entry.getKey(), emb);
                log.debug("域 Embedding 已计算: domain={}, dims={}", entry.getKey(), emb.length);
            } catch (Exception e) {
                log.error("域 Embedding 计算失败: domain={}", entry.getKey(), e);
            }
        }
        lastRefreshTime = System.currentTimeMillis();
        log.info("DomainRouterV2 初始化完成: 已加载 {} 个域的 Embedding", domainEmbeddings.size());
    }

    /**
     * 获取域描述 Map。
     * <p>优先从 {@link KnowledgeCatalogService} 动态获取，不可用时使用硬编码初始值。</p>
     *
     * @return 域ID → 自然语言描述的 Map
     */
    private Map<String, String> getDomainDescriptions() {
        // 尝试从目录服务动态获取
        if (catalogService != null) {
            try {
                CatalogTree tree = catalogService.getCatalogTree();
                if (tree != null && tree.getDomains() != null && !tree.getDomains().isEmpty()) {
                    return buildDescriptionsFromTree(tree);
                }
            } catch (Exception e) {
                log.warn("从目录服务获取域描述失败，使用硬编码初始值", e);
            }
        }
        // 硬编码初始值
        Map<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put("HR", "人力资源管理，员工信息，薪酬福利，绩效考核，组织架构，招聘培训");
        descriptions.put("FINANCE", "财务管理，会计核算，预算控制，成本管理，财务报表，审计合规");
        descriptions.put("RD", "研发管理，产品设计，技术标准，代码管理，测试管理，项目管理");
        descriptions.put("SALES", "销售管理，客户关系，合同管理，销售业绩，市场推广，渠道管理");
        return descriptions;
    }

    /**
     * 从目录树构建域描述。
     *
     * @param tree 知识目录树
     * @return 域ID → 自然语言描述
     */
    private Map<String, String> buildDescriptionsFromTree(CatalogTree tree) {
        Map<String, String> descriptions = new LinkedHashMap<>();
        for (DomainNode domain : tree.getDomains()) {
            StringBuilder sb = new StringBuilder();
            sb.append(domain.getDomainName()).append("，");
            if (domain.getSources() != null) {
                domain.getSources().stream()
                        .limit(20)
                        .forEach(s -> sb.append(s.getSourceName()).append("，"));
            }
            descriptions.put(domain.getDomainName(), sb.toString());
        }
        return descriptions;
    }

    /**
     * 对用户查询进行语义域路由。
     *
     * @param query 用户查询文本
     * @return 域路由结果（Top K 个匹配域及其相似度，或无匹配时返回 fallback）
     */
    public DomainRoutingResult route(String query) {
        if (domainEmbeddings.isEmpty()) {
            log.warn("域 Embedding 为空，回退全局检索");
            return DomainRoutingResult.fallback();
        }

        try {
            float[] queryEmb = providerFactory.getEmbeddingProvider().embed(query);

            // 计算余弦相似度
            Map<String, Double> scores = new LinkedHashMap<>();
            for (Map.Entry<String, float[]> entry : domainEmbeddings.entrySet()) {
                double sim = cosineSimilarity(queryEmb, entry.getValue());
                if (sim > similarityThreshold) {
                    scores.put(entry.getKey(), sim);
                }
            }

            if (scores.isEmpty()) {
                log.debug("语义路由未匹配到任何域 (threshold={})，回退全局检索", similarityThreshold);
                return DomainRoutingResult.fallback();
            }

            // 按相似度降序排列，取 Top K
            Map<String, Double> topMatches = scores.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(topK)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new));

            log.debug("语义路由结果: query={}, matches={}", query, topMatches);
            DomainRoutingResult result = new DomainRoutingResult();
            result.setMatchedDomains(topMatches);
            result.setMatchedEntities(Collections.emptyList());
            result.setFallbackToGlobal(false);
            return result;

        } catch (Exception e) {
            log.error("语义路由异常，回退全局检索", e);
            return DomainRoutingResult.fallback();
        }
    }

    /**
     * 计算两个向量的余弦相似度。
     *
     * @param a 向量 A
     * @param b 向量 B
     * @return 余弦相似度 [-1.0, 1.0]，零向量返回 0.0
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) {
            return 0.0;
        }
        if (a.length != b.length) {
            log.warn("向量维度不一致: a.length={}, b.length={}", a.length, b.length);
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 定时刷新域 Embedding 缓存（每 5 分钟）。
     * <p>适配目录结构变化和 Embedding 模型热切换。</p>
     */
    @Scheduled(fixedRate = 300_000)
    public void refreshDomainEmbeddings() {
        log.debug("开始刷新域 Embedding 缓存...");
        try {
            Map<String, String> descriptions = getDomainDescriptions();
            for (Map.Entry<String, String> entry : descriptions.entrySet()) {
                try {
                    float[] emb = providerFactory.getEmbeddingProvider().embed(entry.getValue());
                    domainEmbeddings.put(entry.getKey(), emb);
                } catch (Exception e) {
                    log.warn("域 Embedding 刷新失败: domain={}, 保留旧值", entry.getKey(), e);
                }
            }
            // 移除已不存在的域
            domainEmbeddings.keySet().retainAll(descriptions.keySet());
            lastRefreshTime = System.currentTimeMillis();
            log.debug("域 Embedding 缓存刷新完成: {} 个域", domainEmbeddings.size());
        } catch (Exception e) {
            log.error("域 Embedding 缓存刷新异常", e);
        }
    }

    /**
     * 获取已缓存的域 Embedding 数量。
     *
     * @return 缓存数量
     */
    public int getCachedDomainCount() {
        return domainEmbeddings.size();
    }
}
