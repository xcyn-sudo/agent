package org.example.agent_qr.dataquality.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条规则检查结果。
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleResult {

    /** 是否通过检查 */
    private boolean passed;

    /** 失败原因（通过时为空字符串） */
    private String reason;

    /**
     * 创建通过的结果。
     */
    public static RuleResult pass() {
        return new RuleResult(true, "");
    }

    /**
     * 创建失败的结果。
     *
     * @param reason 失败原因
     */
    public static RuleResult fail(String reason) {
        return new RuleResult(false, reason);
    }
}
