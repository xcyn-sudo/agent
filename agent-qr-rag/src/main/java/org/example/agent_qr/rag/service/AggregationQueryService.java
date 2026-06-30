package org.example.agent_qr.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.catalog.dto.DomainRoutingResult;
import org.example.agent_qr.rag.entity.KbChunkRef;
import org.example.agent_qr.rag.entity.RetrievedDocument;
import org.example.agent_qr.rag.filter.FilterCondition;
import org.example.agent_qr.rag.filter.FilterConditionExtractor;
import org.example.agent_qr.rag.filter.StructuredFilterService;
import org.example.agent_qr.rag.mapper.KbChunkRefMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聚合查询编排服务，用于列举/统计类查询（需要完整数据集）。
 * <p>
 * 与语义查询路径（走 HybridRetriever + Reranker）不同，
 * 聚合查询路径跳过向量检索和精排，直接通过 SQL 全量查询匹配的切片。
 * </p>
 *
 * <h3>编排流程</h3>
 * <ol>
 *   <li>FilterConditionExtractor.extract(query, domain) → 提取结构化条件</li>
 *   <li>条件为空 → 返回空列表（ChatQueryService 降级到语义路径）</li>
 *   <li>StructuredFilterService.filterChunkIdsUnbounded(domain, conditions) → 无界查询</li>
 *   <li>KbChunkRefMapper.selectByIds(allChunkIds) → 批量获取切片内容</li>
 *   <li>转为 List&lt;RetrievedDocument&gt; → 返回</li>
 * </ol>
 *
 * <h3>降级策略</h3>
 * <p>
 * 任何异常均返回空列表，不影响问答主流程——ChatQueryService 自动降级到语义检索路径。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Service
public class AggregationQueryService {

    @Autowired(required = false)
    private FilterConditionExtractor filterConditionExtractor;

    @Autowired
    private StructuredFilterService structuredFilterService;

    @Autowired
    private KbChunkRefMapper kbChunkRefMapper;

    /**
     * 执行聚合查询（列举/统计类问题专用）。
     * <p>
     * 从用户查询中提取结构化过滤条件，通过无界 SQL 查询全部匹配切片，
     * 跳过向量检索和 Reranker 截断，确保返回完整的记录列表。
     * </p>
     *
     * @param query   用户自然语言问题
     * @param routing 域路由结果
     * @return 全部匹配的文档列表（提取失败或无结果时返回空列表）
     */
    public List<RetrievedDocument> aggregate(String query, DomainRoutingResult routing) {
        String domain = routing != null ? routing.getPrimaryDomain() : null;

        // 1. 提取结构化过滤条件（复用 FilterConditionExtractor）
        List<FilterCondition> conditions;
        try {
            if (filterConditionExtractor != null) {
                conditions = filterConditionExtractor.extract(query, domain);
            } else {
                log.debug("FilterConditionExtractor 未注入，聚合查询降级");
                return List.of();
            }
        } catch (Exception e) {
            log.warn("聚合查询：过滤条件提取失败，降级语义检索", e);
            return List.of();
        }

        if (conditions.isEmpty()) {
            log.info("聚合查询：未提取到过滤条件，降级语义检索: query=\"{}\"", query);
            return List.of();
        }

        // 2. 无界查询全部匹配 chunkId
        List<Long> allChunkIds;
        try {
            allChunkIds = structuredFilterService.filterChunkIdsUnbounded(domain, conditions);
        } catch (Exception e) {
            log.warn("聚合查询：无界过滤查询失败，降级语义检索", e);
            return List.of();
        }

        log.info("聚合查询：匹配 chunk 数={}, domain={}, conditions={}",
                allChunkIds.size(), domain, conditions.size());

        if (allChunkIds.isEmpty()) {
            return List.of();
        }

        // 3. 批量获取 chunk 内容
        List<KbChunkRef> chunkRefs;
        try {
            chunkRefs = kbChunkRefMapper.selectByIds(allChunkIds);
        } catch (Exception e) {
            log.warn("聚合查询：批量获取切片内容失败", e);
            return List.of();
        }

        // 4. 转为 RetrievedDocument 列表
        List<RetrievedDocument> documents = new ArrayList<>();
        for (KbChunkRef ref : chunkRefs) {
            RetrievedDocument doc = new RetrievedDocument();
            doc.setDocumentId(String.valueOf(ref.getId()));
            doc.setChunkId(ref.getId());
            doc.setContent(ref.getContent());
            doc.setDocumentTitle(ref.getTitle() != null ? ref.getTitle() : "chunk-" + ref.getId());
            doc.setSimilarity(1.0); // 聚合查询无相关性排序
            documents.add(doc);
        }

        log.info("聚合查询完成：返回 {} 条文档", documents.size());
        return documents;
    }
}
