package org.example.agent_qr.rag.filter;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.rag.filter.mapper.ChunkStructuredFilterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 结构化过滤服务 — MySQL B+ 树前置过滤。
 * <p>
 * 在向量检索前，先通过结构化字段条件在 MySQL 中过滤候选切片 ID，
 * 将结果集截断至 500 条，大幅减少后续向量检索的计算量。
 * 域过滤覆盖两条管线：数据同步管线（kb_chunk_structured）和
 * 文档上传管线（kb_chunk JOIN kb_document）。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class StructuredFilterService {

    @Autowired
    private ChunkStructuredFilterMapper chunkStructuredFilterMapper;

    /**
     * 根据域和过滤条件获取候选切片 ID 列表。
     * <p>
     * 域过滤覆盖两条管线：
     * <ul>
     *   <li>数据同步管线：kb_chunk_structured.domain</li>
     *   <li>文档上传管线：kb_chunk JOIN kb_document.domain</li>
     * </ul>
     * 多条件取交集（AND），结果集上限 500 条。
     * </p>
     *
     * @param domain     业务域（可为 null，表示不限域）
     * @param conditions 过滤条件列表
     * @return 候选切片 ID 列表
     */
    public List<Long> filterChunkIds(String domain, List<FilterCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            if (domain != null && !domain.isBlank()) {
                Set<Long> domainIds = new HashSet<>();
                // 数据同步管线
                domainIds.addAll(chunkStructuredFilterMapper.selectChunkIdsByDomain(domain));
                // 文档上传管线
                domainIds.addAll(chunkStructuredFilterMapper.selectChunkIdsByDocumentDomain(domain));
                List<Long> result = new ArrayList<>(domainIds);
                result.sort(Comparator.naturalOrder());
                if (result.size() > 500) {
                    result = result.subList(0, 500);
                }
                log.debug("域过滤(无结构化条件): domain={}, resultSize={}", domain, result.size());
                return result;
            }
            return List.of();
        }

        Set<Long> resultSet = null;

        for (FilterCondition condition : conditions) {
            List<Long> ids = dispatchCondition(condition);
            if (resultSet == null) {
                resultSet = new HashSet<>(ids);
            } else {
                resultSet.retainAll(ids); // 多条件交集（AND）
            }
            // 不再提前 break — 每个条件都必须参与交集计算，确保 AND 语义完整
        }

        if (resultSet == null || resultSet.isEmpty()) {
            return List.of();
        }

        // 域过滤：覆盖两条管线
        if (domain != null && !domain.isBlank()) {
            Set<Long> domainIds = new HashSet<>();
            // 数据同步管线
            domainIds.addAll(chunkStructuredFilterMapper.selectChunkIdsByDomain(domain));
            // 文档上传管线
            domainIds.addAll(chunkStructuredFilterMapper.selectChunkIdsByDocumentDomain(domain));
            resultSet.retainAll(domainIds);
        }

        List<Long> result = new ArrayList<>(resultSet);
        result.sort(Comparator.naturalOrder());
        if (result.size() > 500) {
            result = result.subList(0, 500);
        }

        log.debug("结构化过滤: domain={}, conditions={}, resultSize={}", domain, conditions.size(), result.size());
        return result;
    }

    /**
     * 根据字段类型分派到对应的 Mapper 方法。
     */
    private List<Long> dispatchCondition(FilterCondition condition) {
        return switch (condition.getFieldType()) {
            case "NUMBER" -> {
                BigDecimal min = parseMinNumber(condition);
                BigDecimal max = parseMaxNumber(condition);
                yield chunkStructuredFilterMapper.selectChunkIdsByNumberRange(
                        condition.getFieldName(), min, max);
            }
            case "DATE" -> {
                LocalDate start = parseStartDate(condition);
                LocalDate end = parseEndDate(condition);
                yield chunkStructuredFilterMapper.selectChunkIdsByDateRange(
                        condition.getFieldName(), start, end);
            }
            case "ENUM", "STRING" -> chunkStructuredFilterMapper.selectChunkIdsByStringValue(
                    condition.getFieldName(), condition.getValue());
            default -> List.<Long>of();
        };
    }

    private BigDecimal parseMinNumber(FilterCondition c) {
        try {
            return new BigDecimal(c.getMinValue() != null ? c.getMinValue() : c.getValue());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal parseMaxNumber(FilterCondition c) {
        try {
            return new BigDecimal(c.getMaxValue() != null ? c.getMaxValue() : c.getValue());
        } catch (Exception e) {
            return new BigDecimal("999999999");
        }
    }

    private LocalDate parseStartDate(FilterCondition c) {
        try {
            String date = c.getMinValue() != null ? c.getMinValue() : c.getValue();
            return LocalDate.parse(date);
        } catch (Exception e) {
            return LocalDate.of(2000, 1, 1);
        }
    }

    private LocalDate parseEndDate(FilterCondition c) {
        try {
            String date = c.getMaxValue() != null ? c.getMaxValue() : c.getValue();
            return LocalDate.parse(date);
        } catch (Exception e) {
            return LocalDate.of(2099, 12, 31);
        }
    }

    // ==================== 聚合查询路径专用（无界查询） ====================

    /**
     * 无界结构化过滤（聚合查询路径专用）。
     * <p>
     * 与 {@link #filterChunkIds(String, List)} 的区别：
     * <ul>
     *   <li>去掉 subList(0, 500) 硬截断，SQL 层安全上限 2000</li>
     *   <li>仅当 FilterConditionExtractor 已提取到条件时才调用</li>
     *   <li>避免无条件全表扫描</li>
     * </ul>
     * </p>
     *
     * @param domain     业务域（可为 null）
     * @param conditions 过滤条件列表（不应为空）
     * @return 全部匹配的切片 ID 列表（上限 2000）
     */
    public List<Long> filterChunkIdsUnbounded(String domain, List<FilterCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return filterChunkIds(domain, conditions); // 回退到原方法
        }

        Set<Long> resultSet = null;
        for (FilterCondition condition : conditions) {
            List<Long> ids = dispatchConditionUnbounded(condition);
            if (resultSet == null) {
                resultSet = new HashSet<>(ids);
            } else {
                resultSet.retainAll(ids); // 多条件交集（AND）
            }
        }

        // 域过滤：覆盖两条管线
        if (domain != null && !domain.isBlank()) {
            Set<Long> domainIds = new HashSet<>();
            domainIds.addAll(chunkStructuredFilterMapper.selectChunkIdsByDomainUnbounded(domain));
            domainIds.addAll(chunkStructuredFilterMapper.selectChunkIdsByDocumentDomainUnbounded(domain));
            if (resultSet == null) {
                resultSet = domainIds;
            } else {
                resultSet.retainAll(domainIds);
            }
        }

        if (resultSet == null || resultSet.isEmpty()) {
            return List.of();
        }

        List<Long> result = new ArrayList<>(resultSet);
        result.sort(Comparator.naturalOrder());

        // 安全上限 2000（防止极端情况），但不做 500 的硬截断
        if (result.size() > 2000) {
            log.warn("无界查询结果超过安全上限: size={}, domain={}", result.size(), domain);
            result = result.subList(0, 2000);
        }

        log.info("无界结构化过滤: domain={}, conditions={}, resultSize={}",
                domain, conditions.size(), result.size());
        return result;
    }

    /**
     * 分派到无界 Mapper 方法（聚合查询专用）。
     */
    private List<Long> dispatchConditionUnbounded(FilterCondition condition) {
        return switch (condition.getFieldType()) {
            case "NUMBER" -> {
                BigDecimal min = parseMinNumber(condition);
                BigDecimal max = parseMaxNumber(condition);
                yield chunkStructuredFilterMapper.selectAllChunkIdsByNumberRange(
                        condition.getFieldName(), min, max);
            }
            case "DATE" -> {
                LocalDate start = parseStartDate(condition);
                LocalDate end = parseEndDate(condition);
                yield chunkStructuredFilterMapper.selectAllChunkIdsByDateRange(
                        condition.getFieldName(), start, end);
            }
            case "ENUM", "STRING" -> chunkStructuredFilterMapper.selectAllChunkIdsByStringValue(
                    condition.getFieldName(), condition.getValue());
            default -> List.<Long>of();
        };
    }
}
