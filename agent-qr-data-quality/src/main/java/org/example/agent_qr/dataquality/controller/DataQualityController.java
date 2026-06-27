package org.example.agent_qr.dataquality.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.Result;
import org.example.agent_qr.dataquality.entity.QualityReport;
import org.example.agent_qr.dataquality.service.DataQualityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据质量报告 REST 控制器。
 * <p>
 * 提供质检报告的分页列表和详情查询接口，
 * 供前端「质量报告」页面调用。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@RestController
@RequestMapping("/api/dataquality")
@RequiredArgsConstructor
public class DataQualityController {

    private final DataQualityService dataQualityService;

    /**
     * 分页查询质检报告列表。
     * <p>
     * 返回 MyBatis-Plus IPage 对象，Jackson 序列化后
     * 包含 records/total/size/current/pages 字段，
     * 与前端的 PageResult&lt;QualityReport&gt; 类型对齐。
     * </p>
     *
     * @param page    页码（默认 1）
     * @param size    每页条数（默认 10）
     * @param blocked 阻断状态筛选（可选）
     * @return 分页报告列表
     */
    @GetMapping("/reports")
    public Result<IPage<QualityReport>> listReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean blocked) {
        log.info("查询质检报告列表: page={}, size={}, blocked={}", page, size, blocked);
        IPage<QualityReport> result = dataQualityService.listReports(page, size, blocked);
        return Result.success(result);
    }

    /**
     * 根据批次 ID 查询质检报告详情（含失败明细）。
     *
     * @param batchId 同步批次 ID
     * @return 质检报告详情（含失败明细列表）
     */
    @GetMapping("/reports/{batchId}")
    public Result<QualityReport> getReport(@PathVariable String batchId) {
        log.info("查询质检报告详情: batchId={}", batchId);
        QualityReport report = dataQualityService.getReportByBatchId(batchId);
        return Result.success(report);
    }
}
