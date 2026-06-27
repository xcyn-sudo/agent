package org.example.agent_qr.dataquality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.BusinessException;
import org.example.agent_qr.common.event.DataQualityPassedEvent;
import org.example.agent_qr.dataquality.checker.DataQualityChecker;
import org.example.agent_qr.dataquality.context.RuleExecutionContext;
import org.example.agent_qr.dataquality.entity.QualityReport;
import org.example.agent_qr.dataquality.mapper.QualityReportMapper;
import org.example.agent_qr.datasource.entity.DataSourceConfig;
import org.example.agent_qr.datasource.mapper.DataSourceMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据质量报告服务。
 * <p>
 * 封装质检引擎调用、报告持久化和分页查询，
 * 在数据源同步完成后由 DataSourceService 调用。
 * </p>
 * <p>
 * 方案 B：failures 明细以 JSON 列存储在 quality_report 表中，
 * 由 MyBatis-Plus JacksonTypeHandler 自动序列化/反序列化，无需额外 Mapper。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataQualityService {

    private final DataQualityChecker checker;
    private final QualityReportMapper reportMapper;
    private final DataSourceMapper dataSourceMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RuleExecutionContext ruleExecutionContext;

    /**
     * 对同步批次执行质量检查并持久化报告。
     *
     * @param batchId      同步批次 ID
     * @param datasourceId 数据源配置 ID
     * @param sourceName   数据源名称
     * @param rawData      原始数据
     * @return 持久化后的质量报告
     */
    @Transactional
    public QualityReport executeAndSave(String batchId, Long datasourceId, String sourceName,
                                        List<Map<String, Object>> rawData) {
        // 0. 设置规则执行上下文（数据源级配置，供规则链读取）
        try {
            DataSourceConfig dsConfig = dataSourceMapper.selectById(datasourceId);
            if (dsConfig != null && dsConfig.getContentFields() != null
                    && !dsConfig.getContentFields().isBlank()) {
                ruleExecutionContext.put(RuleExecutionContext.KEY_CONTENT_FIELDS,
                        dsConfig.getContentFields());
            }
            // 注：如果 dsConfig 为 null 或 contentFields 为空，不设置上下文，
            //     CompletenessRule 会自动回退到全局默认值

            // 1. 执行规则链检查
            QualityReport report = checker.check(batchId, datasourceId, sourceName, rawData);

            // 1.5 增量同步可能返回 0 条数据，无需生成质量报告
            if (report.getTotal() == 0) {
                log.info("同步批次无数据，跳过质检报告生成: batchId={}, datasourceId={}",
                        batchId, datasourceId);
                return report;
            }

            // 2. 持久化报告（failures 由 JacksonTypeHandler 自动转为 JSON）
            reportMapper.insert(report);
            log.info("质检报告已持久化: id={}, batchId={}, passRate={}",
                    report.getId(), batchId, String.format("%.2f", report.getRate()));

            // 3. 过滤出通过质量检查的数据
            List<Map<String, Object>> passedData = filterPassedData(rawData, report);

            // 4. 更新数据源的累计质量通过数
            //    即使被阻断也调用（传入 0），以将 total_passed 从 NULL 初始化为 0，
            //    防止 KnowledgeCatalogService 回退到 total_synced 而泄露被阻断数据。
            int passedCount = report.isBlocked() ? 0 : report.getPass();
            dataSourceMapper.updateTotalPassed(datasourceId, passedCount);

            // 5. 发布质检通过事件 → 触发 ETL 管道（仅在不阻断时发布）
            if (!report.isBlocked()) {
                eventPublisher.publishEvent(new DataQualityPassedEvent(
                        report.getRate() >= 1.0 ? "全部通过" : "通过率 " + String.format("%.2f", report.getRate()),
                        passedData,
                        datasourceId,
                        batchId));
                log.info("质检通过事件已发布: datasourceId={}, batchId={}, passRate={}, passedCount={}",
                        datasourceId, batchId, report.getRate(), passedData.size());
            } else {
                log.warn("质检被阻断: datasourceId={}, batchId={}, passRate={}, totalPassed 未累加",
                        datasourceId, batchId, report.getRate());
            }

            return report;
        } finally {
            // 清理 ThreadLocal 上下文，防止线程池中的线程复用导致内存泄漏和上下文污染
            ruleExecutionContext.remove();
        }
    }

    /**
     * 分页查询质检报告列表，支持按阻断状态筛选。
     *
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param blocked 阻断状态筛选（可选，null 表示不筛选）
     * @return MyBatis-Plus IPage 分页结果（含 records/total/size/current/pages）
     */
    public IPage<QualityReport> listReports(int page, int size, Boolean blocked) {
        IPage<QualityReport> ipage = new Page<>(page, size);
        LambdaQueryWrapper<QualityReport> wrapper = new LambdaQueryWrapper<>();
        if (blocked != null) {
            wrapper.eq(QualityReport::isBlocked, blocked);
        }
        wrapper.orderByDesc(QualityReport::getCreateTime);
        return reportMapper.selectPage(ipage, wrapper);
    }

    /**
     * 根据批次 ID 查询质检报告详情（含失败明细）。
     *
     * @param batchId 同步批次 ID
     * @return 质检报告（failures 已由 JacksonTypeHandler 自动反序列化）
     */
    public QualityReport getReportByBatchId(String batchId) {
        QualityReport report = reportMapper.selectByBatchId(batchId);
        if (report == null) {
            throw new BusinessException("质检报告不存在: batchId=" + batchId);
        }
        return report;
    }

    /**
     * 从原始数据中过滤出通过质量检查的记录。
     * <p>
     * 根据质检报告中的失败明细（含 recordIndex），
     * 剔除失败的记录，返回通过检查的数据。
     * </p>
     *
     * @param rawData 原始数据
     * @param report  质检报告
     * @return 通过检查的数据记录
     */
    private List<Map<String, Object>> filterPassedData(List<Map<String, Object>> rawData,
                                                       QualityReport report) {
        if (rawData == null || rawData.isEmpty()) {
            return List.of();
        }
        // 使用 checker 返回的未去重失败索引集合，避免去重后漏过滤
        Set<Integer> failedIndices = report.getFailedIndices();
        if (failedIndices == null) {
            failedIndices = Set.of();
        }
        // 过滤出通过的记录
        List<Map<String, Object>> passedData = new ArrayList<>();
        for (int i = 0; i < rawData.size(); i++) {
            if (!failedIndices.contains(i)) {
                passedData.add(rawData.get(i));
            }
        }
        return passedData;
    }
}
