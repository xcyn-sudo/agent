package org.example.agent_qr.rag.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结构化过滤条件。
 * <p>
 * 封装单个字段的过滤条件，支持数值范围、日期范围和精确匹配。
 * </p>
 *
 * @author agent-qr
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterCondition {

    /** 字段名 */
    private String fieldName;

    /** 字段类型：NUMBER / DATE / ENUM / STRING */
    private String fieldType;

    /** 操作符：EQ / GT / GTE / LT / LTE / BETWEEN */
    private String operator;

    /** 单值（EQ/GT/GTE/LT/LTE 时使用） */
    private String value;

    /** 范围最小值（BETWEEN 时使用） */
    private String minValue;

    /** 范围最大值（BETWEEN 时使用） */
    private String maxValue;

    /** 操作符常量 */
    public static final String OP_EQ = "EQ";
    public static final String OP_GT = "GT";
    public static final String OP_GTE = "GTE";
    public static final String OP_LT = "LT";
    public static final String OP_LTE = "LTE";
    public static final String OP_BETWEEN = "BETWEEN";
}
