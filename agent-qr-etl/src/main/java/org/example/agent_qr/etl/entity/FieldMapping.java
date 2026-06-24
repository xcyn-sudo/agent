package org.example.agent_qr.etl.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 字段映射配置。
 * <p>
 * 定义源字段到标准字段的映射关系、显示名称、
 * 转换规则和字典映射，用于 ETL 标准化过程。
 * </p>
 *
 * @author agent-qr
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping {

    /** 目标标准字段名 */
    private String canonicalField;

    /** 源字段名 */
    private String sourceField;

    /** 显示名称（中文） */
    private String displayName;

    /** 自然语言模板：如 "{字段中文名}为{值}{单位}" */
    private String template;

    /** 单位（如"元"、"%"、"人"） */
    private String unit;

    /** 转换规则：DATE_TO_CHINESE / MONEY_FORMAT / PERCENTAGE */
    private String transformRule;

    /** 字典映射（源值 → 显示值），如 D01→研发部 */
    private Map<String, String> dictMapping;

    /** 优先级（数字越小优先级越高） */
    private int priority;

    /** 状态：ACTIVE / INACTIVE */
    private String status;

    /** 状态常量 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    /** 转换规则常量 */
    public static final String RULE_DATE_TO_CHINESE = "DATE_TO_CHINESE";
    public static final String RULE_MONEY_FORMAT = "MONEY_FORMAT";
    public static final String RULE_PERCENTAGE = "PERCENTAGE";
}
