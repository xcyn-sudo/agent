package org.example.agent_qr.rag.filter;

import lombok.Data;

import java.util.List;

/**
 * 字段定义 DTO，用于向 LLM 传递域下可用的结构化字段信息。
 * <p>
 * 包含字段名、字段类型（NUMBER/DATE/ENUM/STRING）以及
 * ENUM 类型字段的所有可选枚举值，供 LLM 构造过滤条件 Prompt 使用。
 * </p>
 *
 * @author agent-qr
 */
@Data
public class FieldDefinition {

    /** 字段名 */
    private String fieldName;

    /** 字段类型：NUMBER / DATE / ENUM / STRING */
    private String fieldType;

    /** 枚举值列表（仅 ENUM 类型字段有值，其他类型为 null） */
    private List<String> enumValues;
}
