package org.example.agent_qr.rag.filter;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.rag.filter.mapper.ChunkStructuredMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 结构化过滤服务 — MySQL B+ 树前置过滤。
 * <p>
 * 在向量检索前，先通过结构化字段条件在 MySQL 中过滤候选切片 ID，
 * 将结果集截断至 500 条，大幅减少后续向量检索的计算量。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class StructuredFilterService {

    @Autowired
    private ChunkStructuredMapper chunkStructuredMapper;

    /**
     * 根据域和过滤条件获取候选切片 ID 列表。
     * <p>
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
                return chunkStructuredMapper.selectChunkIdsByDomain(domain);
            }
            return List.of();
        }

        Set<Long> resultSet = null;

        for (FilterCondition condition : conditions) {
            List<Long> ids = dispatchCondition(condition);
            if (resultSet == null) {
                resultSet = new HashSet<>(ids);
            } else {
                resultSet.retainAll(ids); // 多条件交集
            }

            // 提前截断
            if (resultSet.size() >= 500) {
                break;
            }
        }

        if (resultSet == null || resultSet.isEmpty()) {
            return List.of();
        }

        // 域过滤
        if (domain != null && !domain.isBlank()) {
            List<Long> domainIds = chunkStructuredMapper.selectChunkIdsByDomain(domain);
            resultSet.retainAll(domainIds);
        }

        List<Long> result = new ArrayList<>(resultSet);
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
                yield chunkStructuredMapper.selectChunkIdsByNumberRange(
                        condition.getFieldName(), min, max);
            }
            case "DATE" -> {
                LocalDate start = parseStartDate(condition);
                LocalDate end = parseEndDate(condition);
                yield chunkStructuredMapper.selectChunkIdsByDateRange(
                        condition.getFieldName(), start, end);
            }
            case "ENUM", "STRING" -> chunkStructuredMapper.selectChunkIdsByStringValue(
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
}
