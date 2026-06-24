package org.example.agent_qr.dataquality.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据质量检查报告。
 * <p>
 * 汇总一次质量检查的结果，包括通过率、阻断状态和失败明细。
 * </p>
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QualityReport {

    /** 同步批次 ID */
    private String batchId;

    /** 总记录数 */
    private int total;

    /** 通过数 */
    private int pass;

    /** 失败数 */
    private int fail;

    /** 通过率（pass / total） */
    private double rate;

    /** 是否被阻断（通过率低于阈值） */
    private boolean blocked;

    /** 失败明细列表 */
    private List<QualityFailure> failures = new ArrayList<>();
}
