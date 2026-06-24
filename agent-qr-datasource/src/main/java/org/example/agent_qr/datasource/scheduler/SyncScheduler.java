package org.example.agent_qr.datasource.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.event.DataSyncCompletedEvent;
import org.example.agent_qr.datasource.connector.DataSourceConnector;
import org.example.agent_qr.datasource.dto.ConnectionTestResult;
import org.example.agent_qr.datasource.dto.SyncContext;
import org.example.agent_qr.datasource.dto.SyncResult;
import org.example.agent_qr.datasource.entity.DataSourceConfig;
import org.example.agent_qr.datasource.mapper.DataSourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 数据同步调度器。
 * <p>
 * 负责执行单个数据源的同步任务：
 * 连通性检查 → 按策略全量/增量同步 → 更新结果 → 发布事件。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class SyncScheduler {

    @Autowired
    private DataSourceMapper dataSourceMapper;

    @Autowired
    private Map<String, DataSourceConnector> connectorMap;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 调度指定数据源的同步任务。
     * <p>
     * 流程：查配置 → 获取 Connector → 测连通性 →
     * 按策略全量/增量同步 → 更新同步结果 → 发布 DataSyncCompletedEvent。
     * </p>
     *
     * @param datasourceId 数据源配置 ID
     */
    @SuppressWarnings("unchecked")
    public void scheduleSync(Long datasourceId) {
        DataSourceConfig config = dataSourceMapper.selectById(datasourceId);
        if (config == null) {
            log.warn("同步调度失败：数据源不存在, id={}", datasourceId);
            return;
        }

        DataSourceConnector connector = findConnector(config.getSourceType());
        if (connector == null) {
            log.error("同步调度失败：不支持的数据源类型, id={}, type={}",
                    datasourceId, config.getSourceType());
            return;
        }

        // 解析连接配置
        Map<String, Object> connConfig;
        try {
            connConfig = parseJson(config.getConnectionConfig());
        } catch (Exception e) {
            log.error("同步调度失败：连接配置 JSON 解析错误, id={}", datasourceId, e);
            dataSourceMapper.updateStatus(datasourceId, DataSourceConfig.STATUS_ERROR);
            return;
        }

        // 连通性测试
        ConnectionTestResult testResult = connector.testConnection(connConfig);
        if (!testResult.isSuccess()) {
            log.error("同步调度失败：连通性测试不通过, id={}, error={}",
                    datasourceId, testResult.getErrorMsg());
            dataSourceMapper.updateStatus(datasourceId, DataSourceConfig.STATUS_ERROR);
            return;
        }

        // 执行同步
        SyncContext context = new SyncContext(config.getId(), connConfig);
        SyncResult result;
        try {
            if (DataSourceConfig.SYNC_INCREMENTAL.equals(config.getSyncStrategy())) {
                result = connector.incrementalSync(context, config.getLastCursor());
            } else {
                result = connector.fullSync(context);
            }
        } catch (Exception e) {
            log.error("同步调度失败：同步执行异常, id={}", datasourceId, e);
            dataSourceMapper.updateStatus(datasourceId, DataSourceConfig.STATUS_ERROR);
            return;
        }

        // 更新同步结果
        dataSourceMapper.updateSyncResult(config.getId(),
                result.getNextCursor(), result.getTotalRows(), java.time.LocalDateTime.now());
        dataSourceMapper.updateStatus(datasourceId, DataSourceConfig.STATUS_ACTIVE);

        // 发布数据同步完成事件
        DataSyncCompletedEvent event = new DataSyncCompletedEvent(
                datasourceId, result.getRawData(), context.getSyncBatchId());
        eventPublisher.publishEvent(event);

        log.info("同步调度完成: id={}, sourceName={}, totalRows={}, batchId={}",
                datasourceId, config.getSourceName(), result.getTotalRows(), context.getSyncBatchId());
    }

    /**
     * 根据类型名查找连接器。
     */
    private DataSourceConnector findConnector(String sourceType) {
        return connectorMap.values().stream()
                .filter(c -> c.getType().equalsIgnoreCase(sourceType))
                .findFirst()
                .orElse(null);
    }

    /**
     * JSON 解析工具方法。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("JSON 解析失败: " + e.getMessage(), e);
        }
    }
}
