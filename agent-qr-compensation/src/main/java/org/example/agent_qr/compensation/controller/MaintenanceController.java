package org.example.agent_qr.compensation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.Result;
import org.example.agent_qr.compensation.scanner.DuplicateCleanupScanner;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统维护 REST 控制器。
 * <p>
 * 提供手动触发系统维护操作的端点，如去重清理等。
 * 需要 ABAC {@code canManageDatasource} 权限（仅 admin）。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
@PreAuthorize("@abac.canManageDatasource(principal)")
public class MaintenanceController {

    private final DuplicateCleanupScanner duplicateCleanupScanner;

    /**
     * 手动触发去重清理。
     * <p>
     * 同步执行两阶段清理：精确去重（record_hash 匹配）+ 内容近似去重（MD5(content) 匹配）。
     * 清理链路：软删除切片 → BM25 索引移除 → 结构化元数据删除 → ChromaDB 向量删除。
     * 详情见服务端日志。
     * </p>
     *
     * @return 操作结果
     */
    @PostMapping("/cleanup-duplicates")
    public Result<String> cleanupDuplicates() {
        log.info("收到手动去重清理请求");
        duplicateCleanupScanner.cleanDuplicateChunks();
        return Result.success("去重清理已完成，请查看服务端日志了解清理详情");
    }
}
