package org.example.agent_qr.etl.converter;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.etl.entity.FieldMapping;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 结构化数据转换器。
 * <p>
 * 将字段映射后的结构化数据转换为自然语言段落，
 * 按字段优先级排序并应用模板生成可读文本。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class StructuredDataConverter {

    /**
     * 将映射后的记录转换为自然语言段落。
     * <p>
     * 输出格式示例：
     * "【HR数据库】员工张三，所属部门为研发部，月薪15,000元，入职日期为2024年1月15日。"
     * </p>
     *
     * @param mappedRecord  字段映射后的标准记录（canonicalField → value）
     * @param fieldMappings 字段映射配置列表（含模板、优先级等信息）
     * @param sourceName    数据源名称（用于段落标题）
     * @return 自然语言段落
     */
    public String convert(Map<String, Object> mappedRecord,
                          List<FieldMapping> fieldMappings,
                          String sourceName) {
        if (mappedRecord == null || mappedRecord.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // 段落标题
        sb.append("【").append(sourceName != null ? sourceName : "数据源").append("】");

        // 按优先级排序字段映射
        List<FieldMapping> sortedMappings = fieldMappings.stream()
                .filter(m -> FieldMapping.STATUS_ACTIVE.equals(m.getStatus()))
                .sorted(Comparator.comparingInt(FieldMapping::getPriority))
                .toList();

        int fieldCount = 0;
        for (FieldMapping mapping : sortedMappings) {
            Object value = mappedRecord.get(mapping.getCanonicalField());
            if (value == null || value.toString().isBlank()) {
                continue;
            }

            // 应用模板生成自然语言
            String text = applyTemplate(mapping, value);
            sb.append(text);

            fieldCount++;
            if (fieldCount < sortedMappings.size()) {
                sb.append("，");
            }
        }

        // 兜底：无 FieldMapping 配置时，遍历所有字段生成文本
        if (fieldCount == 0) {
            int i = 0;
            int totalFields = mappedRecord.size();
            for (Map.Entry<String, Object> entry : mappedRecord.entrySet()) {
                Object value = entry.getValue();
                if (value == null || value.toString().isBlank()) {
                    continue;
                }
                sb.append(entry.getKey()).append("为").append(value);
                i++;
                if (i < totalFields) {
                    sb.append("，");
                }
            }
        }

        sb.append("。");
        return sb.toString();
    }

    /**
     * 应用字段模板生成自然语言片段。
     * <p>
     * 模板格式："{字段中文名}为{值}{单位}"。
     * 若未配置模板，使用默认格式。
     * </p>
     */
    private String applyTemplate(FieldMapping mapping, Object value) {
        String displayName = mapping.getDisplayName() != null
                ? mapping.getDisplayName()
                : mapping.getCanonicalField();
        String unit = mapping.getUnit() != null ? mapping.getUnit() : "";

        if (mapping.getTemplate() != null && !mapping.getTemplate().isBlank()) {
            return mapping.getTemplate()
                    .replace("{字段中文名}", displayName)
                    .replace("{值}", value.toString())
                    .replace("{单位}", unit);
        }

        // 默认模板
        return displayName + "为" + value + unit;
    }
}
