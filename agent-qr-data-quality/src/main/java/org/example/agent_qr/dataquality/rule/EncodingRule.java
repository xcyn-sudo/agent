package org.example.agent_qr.dataquality.rule;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.dataquality.entity.RuleResult;
import org.example.agent_qr.dataquality.util.CharsetDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * 编码检查规则。
 * <p>
 * 对记录中所有 String 字段检测字符编码，标记非 UTF-8 编码的记录。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class EncodingRule implements QualityRule {

    @Autowired
    private CharsetDetector charsetDetector;

    @Override
    public String getName() {
        return "编码";
    }

    @Override
    public RuleResult evaluate(Map<String, Object> record) {
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            if (entry.getValue() instanceof String text && !text.isBlank()) {
                String detectedCharset = charsetDetector.detect(text);
                if (detectedCharset != null && !"UTF-8".equalsIgnoreCase(detectedCharset)) {
                    return RuleResult.fail(
                            String.format("字段 '%s' 编码为 %s，非 UTF-8", entry.getKey(), detectedCharset));
                }
            }
        }
        return RuleResult.pass();
    }
}
