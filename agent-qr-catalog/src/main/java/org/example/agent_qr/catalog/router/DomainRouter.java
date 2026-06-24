package org.example.agent_qr.catalog.router;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.catalog.dto.DomainRoutingResult;
import org.example.agent_qr.catalog.entity.CatalogTree;
import org.example.agent_qr.catalog.entity.DomainNode;
import org.example.agent_qr.catalog.entity.EntityNode;
import org.example.agent_qr.catalog.entity.SourceNode;
import org.example.agent_qr.catalog.service.KnowledgeCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 域路由器（P2 关键词匹配方式）。
 * <p>
 * 根据用户查询中的关键词与知识目录树进行匹配，
 * 计算各域的匹配分数，返回应检索的域和实体列表。
 * 无匹配时降级到全局检索。
 * </p>
 * <p>
 * P3 将升级为 Embedding 语义域路由（DomainRouterV2）。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class DomainRouter {

    @Autowired
    private KnowledgeCatalogService catalogService;

    /**
     * 根据查询文本路由到匹配的域。
     * <p>
     * 算法：提取查询关键词 → 遍历目录树 → 计算匹配分数：
     * <ul>
     *   <li>域名称匹配：+0.5 分</li>
     *   <li>数据源名称匹配：+0.3 分</li>
     *   <li>实体名称匹配：+0.4 分</li>
     * </ul>
     * score > 0 的域加入 matchedDomains，无匹配则降级到全局。
     * </p>
     *
     * @param query 用户查询文本
     * @return 域路由结果
     */
    public DomainRoutingResult route(String query) {
        if (query == null || query.isBlank()) {
            return DomainRoutingResult.fallback();
        }

        List<String> keywords = extractKeywords(query);
        if (keywords.isEmpty()) {
            return DomainRoutingResult.fallback();
        }

        CatalogTree tree = catalogService.getCatalogTree();
        Map<String, Double> domainScores = new LinkedHashMap<>();
        List<String> matchedEntities = new ArrayList<>();

        for (DomainNode domain : tree.getDomains()) {
            double score = 0.0;

            // 域名称匹配
            for (String kw : keywords) {
                if (domain.getDomainName() != null
                        && domain.getDomainName().toLowerCase().contains(kw.toLowerCase())) {
                    score += 0.5;
                }
            }

            // 遍历数据源和实体
            for (SourceNode source : domain.getSources()) {
                for (String kw : keywords) {
                    // 数据源名称匹配
                    if (source.getSourceName() != null
                            && source.getSourceName().toLowerCase().contains(kw.toLowerCase())) {
                        score += 0.3;
                    }
                }

                for (EntityNode entity : source.getEntities()) {
                    for (String kw : keywords) {
                        // 实体名称匹配
                        if (entity.getEntityName() != null
                                && entity.getEntityName().toLowerCase().contains(kw.toLowerCase())) {
                            score += 0.4;
                            if (!matchedEntities.contains(entity.getEntityName())) {
                                matchedEntities.add(entity.getEntityName());
                            }
                        }
                    }
                }
            }

            if (score > 0) {
                domainScores.put(domain.getDomainName(), score);
            }
        }

        if (domainScores.isEmpty()) {
            log.debug("域路由无匹配，降级到全局检索: query={}", query);
            return DomainRoutingResult.fallback();
        }

        DomainRoutingResult result = new DomainRoutingResult();
        result.setMatchedDomains(domainScores);
        result.setMatchedEntities(matchedEntities);
        result.setFallbackToGlobal(false);

        log.debug("域路由结果: query={}, matchedDomains={}, primaryDomain={}",
                query, domainScores.keySet(), result.getPrimaryDomain());
        return result;
    }

    /**
     * 根据路由结果构建检索过滤条件。
     * <p>
     * 降级到全局时返回空 Map（无过滤），
     * 否则返回 domain 过滤条件（逗号分隔的域列表）。
     * </p>
     *
     * @param routing 域路由结果
     * @return 过滤条件 Map（key=domain, value=域列表）
     */
    public Map<String, String> buildRetrievalFilter(DomainRoutingResult routing) {
        if (routing.isFallbackToGlobal()) {
            return Map.of();
        }
        String domainFilter = String.join(",", routing.getMatchedDomains().keySet());
        return Map.of("domain", domainFilter);
    }

    /**
     * 从查询文本中提取关键词。
     * <p>
     * P2 简化实现：按空格和常见分隔符分词，过滤停用词和短词。
     * </p>
     */
    private List<String> extractKeywords(String query) {
        List<String> keywords = new ArrayList<>();
        // 按空格、逗号、句号等分隔
        String[] tokens = query.split("[\\s,，。.、]+");
        for (String token : tokens) {
            String trimmed = token.trim().toLowerCase();
            if (trimmed.length() >= 2 && !isStopWord(trimmed)) {
                keywords.add(trimmed);
            }
        }
        return keywords;
    }

    /**
     * 判断是否为停用词。
     */
    private boolean isStopWord(String word) {
        return switch (word) {
            case "的", "是", "在", "和", "了", "有", "我", "不", "人", "这", "中", "大",
                 "the", "a", "an", "is", "are", "was", "were", "in", "on", "at",
                 "to", "of", "for", "with", "and", "or", "it", "that", "this" -> true;
            default -> false;
        };
    }
}
