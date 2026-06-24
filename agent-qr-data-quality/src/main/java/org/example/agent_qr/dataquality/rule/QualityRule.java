package org.example.agent_qr.dataquality.rule;

import org.example.agent_qr.dataquality.entity.RuleResult;

import java.util.Map;

/**
 * 数据质量检查规则接口。
 * <p>
 * 所有质量检查规则实现此接口，通过 evaluate 方法对单条记录执行检查。
 * 规则链按顺序执行：完整性 → 编码 → 格式。
 * </p>
 *
 * @author agent-qr
 */
public interface QualityRule {

    /**
     * 获取规则名称。
     *
     * @return 规则名称（如"完整性"、"编码"、"格式"）
     */
    String getName();

    /**
     * 对单条记录执行质量检查。
     *
     * @param record 待检查的数据记录（字段名 → 字段值）
     * @return 检查结果（通过或失败 + 原因）
     */
    RuleResult evaluate(Map<String, Object> record);
}
