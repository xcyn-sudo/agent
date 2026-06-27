package org.example.agent_qr.dataquality.rule;

import org.example.agent_qr.dataquality.context.RuleExecutionContext;
import org.example.agent_qr.dataquality.entity.RuleResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 完整性检查规则。
 * <p>
 * 检查可配置的关键字段列表是否至少有一个非空。
 * 优先使用数据源级配置 {@code DataSourceConfig.contentFields}，
 * 未配置时回退到全局默认值 {@code agent-qr.data-quality.content-fields}。
 * </p>
 *
 * @author agent-qr
 */
@Component
public class CompletenessRule implements QualityRule {

    /** 全局默认内容字段名列表（逗号分隔），作为回退值 */
    @Value("${agent-qr.data-quality.content-fields:content,text,_content}")
    private String globalContentFieldsConfig;

    @Autowired
    private RuleExecutionContext ruleExecutionContext;

    @Override
    public String getName() {
        return "完整性";
    }

    @Override
    public RuleResult evaluate(Map<String, Object> record) {
        // 1. 优先使用数据源级配置
        String fieldsConfig = ruleExecutionContext.get(
                RuleExecutionContext.KEY_CONTENT_FIELDS, String.class);

        // 2. 数据源未配置时回退到全局默认值
        if (fieldsConfig == null || fieldsConfig.isBlank()) {
            fieldsConfig = globalContentFieldsConfig;
        }

        List<String> contentFields = Arrays.stream(fieldsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        for (String field : contentFields) {
            Object value = record.get(field);
            if (value != null && !value.toString().isBlank()) {
                return RuleResult.pass();
            }
        }

        return RuleResult.fail("内容字段为空（检查字段: " + contentFields + " 均为空）");
    }
}
