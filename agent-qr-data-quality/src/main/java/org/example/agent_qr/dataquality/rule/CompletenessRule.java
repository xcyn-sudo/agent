package org.example.agent_qr.dataquality.rule;

import org.example.agent_qr.dataquality.entity.RuleResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 完整性检查规则。
 * <p>
 * 检查关键字段（content / text）是否为空或仅含空白字符。
 * </p>
 *
 * @author agent-qr
 */
@Component
public class CompletenessRule implements QualityRule {

    @Override
    public String getName() {
        return "完整性";
    }

    @Override
    public RuleResult evaluate(Map<String, Object> record) {
        // 检查 content 字段
        Object content = record.get("content");
        if (content == null || content.toString().isBlank()) {
            // 也尝试检查 text 字段
            Object text = record.get("text");
            if (text == null || text.toString().isBlank()) {
                // 尝试 _content（S3 文件内容字段）
                Object s3Content = record.get("_content");
                if (s3Content == null || s3Content.toString().isBlank()) {
                    return RuleResult.fail("内容字段为空（content/text/_content 均为空）");
                }
            }
        }
        return RuleResult.pass();
    }
}
