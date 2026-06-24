package org.example.agent_qr.etl.engine;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.etl.entity.FieldMapping;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字段映射引擎。
 * <p>
 * 将原始记录中的源字段按映射配置转换为标准字段，
 * 支持字典翻译和格式转换（日期中文化、金额格式化、百分比格式化）。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class FieldMappingEngine {

    private static final DateTimeFormatter SOURCE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 应用字段映射配置，将原始记录转换为标准记录。
     *
     * @param rawRecord     原始数据记录
     * @param fieldMappings 字段映射配置列表
     * @return 标准化的字段 Map（canonicalField → value）
     */
    public Map<String, Object> apply(Map<String, Object> rawRecord, List<FieldMapping> fieldMappings) {
        Map<String, Object> mappedRecord = new LinkedHashMap<>();

        if (fieldMappings == null || fieldMappings.isEmpty()) {
            // 无映射配置时直接透传
            mappedRecord.putAll(rawRecord);
            return mappedRecord;
        }

        for (FieldMapping mapping : fieldMappings) {
            if (FieldMapping.STATUS_INACTIVE.equals(mapping.getStatus())) {
                continue;
            }

            Object rawValue = rawRecord.get(mapping.getSourceField());
            if (rawValue == null) {
                continue;
            }

            // 1. 字典翻译
            Object translatedValue = applyDictionary(rawValue, mapping.getDictMapping());

            // 2. 格式转换
            Object formattedValue = applyFormat(translatedValue, mapping);

            mappedRecord.put(mapping.getCanonicalField(), formattedValue);
        }

        return mappedRecord;
    }

    /**
     * 应用字典翻译。
     *
     * @param rawValue    原始值
     * @param dictMapping 字典映射表
     * @return 翻译后的值
     */
    public Object applyDictionary(Object rawValue, Map<String, String> dictMapping) {
        if (dictMapping == null || dictMapping.isEmpty()) {
            return rawValue;
        }
        String key = rawValue.toString().trim();
        return dictMapping.getOrDefault(key, key);
    }

    /**
     * 应用格式转换规则。
     *
     * @param value   原始值
     * @param mapping 字段映射配置
     * @return 格式化后的值
     */
    public Object applyFormat(Object value, FieldMapping mapping) {
        if (value == null || mapping.getTransformRule() == null) {
            return value;
        }

        String strValue = value.toString().trim();
        return switch (mapping.getTransformRule()) {
            case FieldMapping.RULE_DATE_TO_CHINESE -> formatDateToChinese(strValue);
            case FieldMapping.RULE_MONEY_FORMAT -> formatMoney(strValue);
            case FieldMapping.RULE_PERCENTAGE -> formatPercentage(strValue);
            default -> value;
        };
    }

    /**
     * 将日期字符串转换为中文格式："2024-01-15" → "2024年1月15日"。
     */
    private String formatDateToChinese(String dateStr) {
        try {
            // 处理 ISO 格式
            if (dateStr.contains("T")) {
                dateStr = dateStr.substring(0, 10);
            }
            LocalDate date = LocalDate.parse(dateStr, SOURCE_DATE_FORMAT);
            return String.format("%d年%d月%d日", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        } catch (DateTimeParseException e) {
            log.debug("日期格式化失败，返回原值: {}", dateStr);
            return dateStr;
        }
    }

    /**
     * 格式化金额："15000" → "15,000"。
     */
    private String formatMoney(String value) {
        try {
            double amount = Double.parseDouble(value.replace(",", ""));
            DecimalFormat df = new DecimalFormat("#,###");
            return df.format(amount);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    /**
     * 格式化百分比："0.85" → "85.0%"。
     */
    private String formatPercentage(String value) {
        try {
            double percent = Double.parseDouble(value.replace("%", ""));
            // 如果值 <= 1 则视为小数形式需要乘以 100
            if (percent <= 1) {
                percent *= 100;
            }
            return String.format("%.1f%%", percent);
        } catch (NumberFormatException e) {
            return value;
        }
    }
}
