package org.example.agent_qr.dataquality.rule;

import org.example.agent_qr.dataquality.entity.RuleResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * 格式检查规则。
 * <p>
 * 检查记录中日期字段（yyyy-MM-dd 格式）和数字字段的格式合法性。
 * 百分比字段值应在 [0, 100] 范围内。
 * </p>
 *
 * @author agent-qr
 */
@Component
public class FormatRule implements QualityRule {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String getName() {
        return "格式";
    }

    @Override
    public RuleResult evaluate(Map<String, Object> record) {
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            String key = entry.getKey().toLowerCase();
            Object value = entry.getValue();

            if (value == null) {
                continue;
            }

            String strValue = value.toString().trim();
            if (strValue.isEmpty()) {
                continue;
            }

            // 日期字段检查
            if (key.contains("date") || key.contains("time") || key.contains("_at")) {
                try {
                    // 尝试解析 ISO 日期时间格式
                    if (strValue.contains("T")) {
                        LocalDate.parse(strValue.substring(0, 10), DATE_FORMATTER);
                    } else {
                        LocalDate.parse(strValue, DATE_FORMATTER);
                    }
                } catch (DateTimeParseException e) {
                    return RuleResult.fail(
                            String.format("字段 '%s' 的值 '%s' 不是合法的日期格式(yyyy-MM-dd)",
                                    entry.getKey(), strValue));
                }
            }

            // 数字字段检查
            if (key.contains("amount") || key.contains("price") || key.contains("salary")
                    || key.contains("number") || key.contains("count") || key.contains("size")) {
                try {
                    new BigDecimal(strValue.replace(",", ""));
                } catch (NumberFormatException e) {
                    return RuleResult.fail(
                            String.format("字段 '%s' 的值 '%s' 不是合法的数字格式",
                                    entry.getKey(), strValue));
                }
            }

            // 百分比字段检查
            if (key.contains("percent") || key.contains("rate") || key.contains("ratio")) {
                try {
                    double percent = Double.parseDouble(strValue.replace("%", ""));
                    if (percent < 0 || percent > 100) {
                        return RuleResult.fail(
                                String.format("字段 '%s' 的值 '%s' 不在 [0, 100] 范围内",
                                        entry.getKey(), strValue));
                    }
                } catch (NumberFormatException e) {
                    return RuleResult.fail(
                            String.format("字段 '%s' 的值 '%s' 不是合法的百分比格式",
                                    entry.getKey(), strValue));
                }
            }
        }

        return RuleResult.pass();
    }
}
