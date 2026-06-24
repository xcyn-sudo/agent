package org.example.agent_qr.dataquality.checker;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.dataquality.entity.QualityFailure;
import org.example.agent_qr.dataquality.entity.QualityReport;
import org.example.agent_qr.dataquality.entity.RuleResult;
import org.example.agent_qr.dataquality.rule.QualityRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据质量检查器 — 规则链引擎。
 * <p>
 * 按顺序执行规则链（完整性 → 编码 → 格式），
 * 跨记录 MD5 去重失败项，统计通过率并与阻断阈值比较。
 * 当 passRate < blockThreshold 时标记为阻断。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class DataQualityChecker {

    @Autowired
    private List<QualityRule> rules;

    /** 阻断阈值：通过率低于此值则阻断，默认 0.5 */
    @Value("${agent-qr.data-quality.block-threshold:0.5}")
    private double blockThreshold;

    /**
     * 对同步数据执行质量检查。
     *
     * @param batchId 同步批次 ID
     * @param rawData 原始数据（记录列表）
     * @return 质量检查报告
     */
    public QualityReport check(String batchId, List<Map<String, Object>> rawData) {
        if (rawData == null || rawData.isEmpty()) {
            QualityReport emptyReport = new QualityReport();
            emptyReport.setBatchId(batchId);
            emptyReport.setTotal(0);
            emptyReport.setRate(1.0);
            emptyReport.setBlocked(false);
            return emptyReport;
        }

        List<QualityFailure> allFailures = new ArrayList<>();
        int passCount = 0;
        int failCount = 0;

        for (int i = 0; i < rawData.size(); i++) {
            Map<String, Object> record = rawData.get(i);
            boolean recordPassed = true;

            // 规则链顺序执行
            for (QualityRule rule : rules) {
                RuleResult result = rule.evaluate(record);
                if (!result.isPassed()) {
                    allFailures.add(new QualityFailure(rule.getName(), i, result.getReason()));
                    recordPassed = false;
                }
            }

            if (recordPassed) {
                passCount++;
            } else {
                failCount++;
            }
        }

        // 跨记录 MD5 去重失败项
        List<QualityFailure> dedupedFailures = deduplicateFailures(allFailures);

        int total = rawData.size();
        double passRate = (double) passCount / total;
        boolean blocked = passRate < blockThreshold;

        if (blocked) {
            log.warn("数据质量检查阻断: batchId={}, passRate={}/{}={}, threshold={}",
                    batchId, passCount, total, String.format("%.2f", passRate), blockThreshold);
        } else {
            log.info("数据质量检查通过: batchId={}, passRate={}/{}={}",
                    batchId, passCount, total, String.format("%.2f", passRate));
        }

        return new QualityReport(batchId, total, passCount, dedupedFailures.size(),
                passRate, blocked, dedupedFailures);
    }

    /**
     * 跨记录 MD5 去重失败项。
     */
    private List<QualityFailure> deduplicateFailures(List<QualityFailure> failures) {
        Set<String> seen = new HashSet<>();
        List<QualityFailure> deduped = new ArrayList<>();
        for (QualityFailure failure : failures) {
            String key = md5Hash(failure.getRuleName() + "|" + failure.getReason());
            if (seen.add(key)) {
                deduped.add(failure);
            }
        }
        return deduped;
    }

    /**
     * 计算字符串的 MD5 哈希（用于去重）。
     */
    private String md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
