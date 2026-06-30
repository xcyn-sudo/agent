package org.example.agent_qr.datasource.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.BusinessException;
import org.example.agent_qr.common.Result;
import org.example.agent_qr.datasource.dto.ConnectionTestResult;
import org.example.agent_qr.datasource.dto.SyncResult;
import org.example.agent_qr.datasource.entity.DataSourceConfig;
import org.example.agent_qr.datasource.entity.SyncRecord;
import org.example.agent_qr.datasource.service.DataSourceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据源管理 REST 控制器。
 * <p>
 * 提供数据源配置的 CRUD、连通性测试、手动同步触发和同步历史查询。
 * 所有接口需要 ABAC {@code canManageDatasource} 权限（仅 admin）。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@RestController
@RequestMapping("/api/datasource")
@RequiredArgsConstructor
@PreAuthorize("@abac.canManageDatasource(principal)")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    /**
     * 分页查询数据源列表，支持按 domain 筛选。
     *
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @param domain 业务域筛选（可选）
     * @return 分页结果
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> listByPage(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) String domain) {
        Map<String, Object> result = dataSourceService.listByPage(page, size, domain);
        return Result.success(result);
    }

    /**
     * 根据 ID 获取数据源详情。
     *
     * @param id 数据源 ID
     * @return 数据源配置
     */
    @GetMapping("/{id}")
    public Result<DataSourceConfig> getById(@PathVariable Long id) {
        DataSourceConfig config = dataSourceService.getById(id);
        return Result.success(config);
    }

    /**
     * 创建数据源配置。
     *
     * @param config 数据源配置
     * @return 创建后的配置
     */
    @PostMapping
    public Result<DataSourceConfig> create(@RequestBody DataSourceConfig config) {
        if (config.getSourceName() == null || config.getSourceName().isBlank()) {
            throw new BusinessException("数据源名称不能为空");
        }
        if (config.getSourceType() == null || config.getSourceType().isBlank()) {
            throw new BusinessException("数据源类型不能为空");
        }
        DataSourceConfig created = dataSourceService.create(config);
        return Result.success("数据源创建成功", created);
    }

    /**
     * 更新数据源配置。
     *
     * @param id     数据源 ID
     * @param config 更新的配置
     * @return 更新后的配置
     */
    @PutMapping("/{id}")
    public Result<DataSourceConfig> update(@PathVariable Long id, @RequestBody DataSourceConfig config) {
        config.setId(id);
        DataSourceConfig updated = dataSourceService.update(config);
        return Result.success("数据源更新成功", updated);
    }

    /**
     * 删除数据源配置。
     *
     * @param id 数据源 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dataSourceService.delete(id);
        return Result.success("数据源删除成功");
    }

    /**
     * 测试数据源连通性。
     *
     * @param id 数据源 ID
     * @return 连通性测试结果
     */
    @PostMapping("/{id}/test")
    public Result<ConnectionTestResult> testConnection(@PathVariable Long id) {
        ConnectionTestResult result = dataSourceService.testConnection(id);
        if (result.isSuccess()) {
            return Result.success("连接测试成功", result);
        }
        return new Result<>(400, result.getErrorMsg(), result, System.currentTimeMillis());
    }

    /**
     * 检测指定表的字段列表。
     * 直接接收连接配置 JSON 和表名，不需要已保存的数据源 ID。
     *
     * @param body 包含 connectionConfig 和 tableName
     * @return 字段名列表
     */
    @PostMapping("/detect-columns")
    public Result<List<String>> detectColumns(@RequestBody Map<String, String> body) {
        String connectionConfig = body.get("connectionConfig");
        String tableName = body.get("tableName");
        String sourceType = body.get("sourceType");
        if (connectionConfig == null || connectionConfig.isBlank()) {
            throw new BusinessException("connectionConfig 不能为空");
        }
        List<String> columns = dataSourceService.detectColumns(connectionConfig, tableName, sourceType);
        return Result.success("字段检测成功", columns);
    }

    /**
     * 手动触发数据源同步。
     *
     * @param id 数据源 ID
     * @return 同步结果
     */
    @PostMapping("/{id}/sync")
    public Result<SyncResult> triggerSync(@PathVariable Long id) {
        SyncResult result = dataSourceService.triggerSync(id);
        log.info("手动触发同步完成: datasourceId={}, totalRows={}", id, result.getTotalRows());
        return Result.success("同步完成", result);
    }

    /**
     * 查询数据源同步历史，按同步时间倒序。
     *
     * @param id   数据源 ID
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @return 同步历史分页结果
     */
    @GetMapping("/{id}/sync-history")
    public Result<Map<String, Object>> getSyncHistory(@PathVariable Long id,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> result = dataSourceService.getSyncHistory(id, page, size);
        return Result.success(result);
    }
}
