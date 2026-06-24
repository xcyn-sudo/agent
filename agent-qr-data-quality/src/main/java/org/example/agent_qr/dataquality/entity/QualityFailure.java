package org.example.agent_qr.dataquality.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 质量检查失败明细。
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QualityFailure {

    /** 触发失败的规则名称 */
    private String ruleName;

    /** 失败的记录索引（从 0 开始） */
    private int recordIndex;

    /** 失败原因 */
    private String reason;
}
