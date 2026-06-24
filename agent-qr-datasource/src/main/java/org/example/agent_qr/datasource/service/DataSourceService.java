package org.example.agent_qr.datasource.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.BusinessException;
import org.example.agent_qr.datasource.connector.DataSourceConnector;
import org.example.agent_qr.datasource.dto.ConnectionTestResult;
import org.example.agent_qr.datasource.dto.SyncContext;
import org.example.agent_qr.datasource.dto.SyncResult;
import org.example.agent_qr.datasource.entity.DataSourceConfig;
import org.example.agent_qr.datasource.mapper.DataSourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 数据源管理服务。
 * <p>
 * 提供数据源配置的 CRUD 操作、连通性测试和同步触发。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Service
public class DataSourceService {

    @Autowired
    private DataSourceMapper dataSourceMapper;

    @Autowired
    private Map<String, DataSourceConnector> connectorMap;

    // ==================== CRUD ====================

    /**
     * 创建数据源配置。
     */
    public DataSourceConfig create(DataSourceConfig config) {
        dataSourceMapper.insert(config);
        log.info("数据源配置已创建: id={}, sourceName={}, sourceType={}",
                config.getId(), config.getSourceName(), config.getSourceType());
        return config;
    }

    /**
     * 根据 ID 查询数据源配置。
     */
    public DataSourceConfig getById(Long id) {
        DataSourceConfig config = dataSourceMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("数据源配置不存在: id=" + id);
        }
        return config;
    }

    /**
     * 查询所有数据源配置。
     */
    public List<DataSourceConfig> listAll() {
        return dataSourceMapper.selectList(new LambdaQueryWrapper<>());
    }

    /**
     * 查询所有活跃的数据源配置。
     */
    public List<DataSourceConfig> listActive() {
        return dataSourceMapper.selectAllActive();
    }

    /**
     * 更新数据源配置。
     */
    public DataSourceConfig update(DataSourceConfig config) {
        dataSourceMapper.updateById(config);
        log.info("数据源配置已更新: id={}", config.getId());
        return config;
    }

    /**
     * 删除数据源配置。
     */
    public void delete(Long id) {
        dataSourceMapper.deleteById(id);
        log.info("数据源配置已删除: id={}", id);
    }

    // ==================== 连通性测试 ====================

    /**
     * 测试数据源连通性。
     *
     * @param id 数据源配置 ID
     * @return 连通性测试结果
     */
    @SuppressWarnings("unchecked")
    public ConnectionTestResult testConnection(Long id) {
        DataSourceConfig config = getById(id);
        DataSourceConnector connector = getConnector(config.getSourceType());

        // 解析连接配置 JSON
        Map<String, Object> connConfig;
        try {
            connConfig = parseJson(config.getConnectionConfig());
        } catch (Exception e) {
            return ConnectionTestResult.fail("连接配置 JSON 解析失败: " + e.getMessage());
        }

        return connector.testConnection(connConfig);
    }

    // ==================== 同步触发 ====================

    /**
     * 触发数据源同步。
     *
     * @param id 数据源配置 ID
     * @return 同步结果
     */
    @SuppressWarnings("unchecked")
    public SyncResult triggerSync(Long id) {
        DataSourceConfig config = getById(id);
        DataSourceConnector connector = getConnector(config.getSourceType());

        Map<String, Object> connConfig;
        try {
            connConfig = parseJson(config.getConnectionConfig());
        } catch (Exception e) {
            throw new BusinessException("连接配置 JSON 解析失败: " + e.getMessage());
        }

        SyncContext context = new SyncContext(config.getId(), connConfig);

        SyncResult result;
        if (DataSourceConfig.SYNC_INCREMENTAL.equals(config.getSyncStrategy())
                && config.getLastCursor() != null) {
            result = connector.incrementalSync(context, config.getLastCursor());
        } else {
            result = connector.fullSync(context);
        }

        // 更新同步结果
        if (result.getNextCursor() != null) {
            dataSourceMapper.updateSyncResult(config.getId(),
                    result.getNextCursor(), result.getTotalRows(), java.time.LocalDateTime.now());
        }
        dataSourceMapper.updateStatus(config.getId(), DataSourceConfig.STATUS_ACTIVE);

        log.info("数据源同步完成: id={}, sourceName={}, totalRows={}, nextCursor={}",
                id, config.getSourceName(), result.getTotalRows(), result.getNextCursor());
        return result;
    }

    /**
     * 根据类型获取对应的连接器。
     */
    private DataSourceConnector getConnector(String sourceType) {
        DataSourceConnector connector = connectorMap.get(sourceType);
        if (connector == null) {
            // 尝试通过类型名查找（Spring Bean 命名约定）
            String beanName = sourceType.substring(0, 1).toLowerCase()
                    + sourceType.substring(1) + "Connector";
            connector = connectorMap.values().stream()
                    .filter(c -> c.getType().equalsIgnoreCase(sourceType))
                    .findFirst()
                    .orElse(null);
        }
        if (connector == null) {
            throw new BusinessException("不支持的数据源类型: " + sourceType);
        }
        return connector;
    }

    /**
     * 简单 JSON 解析（将 JSON 字符串转为 Map）。
     * 实际项目中建议使用 Jackson ObjectMapper。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        // 使用 Spring 自带的 Jackson 或简单处理
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new BusinessException("JSON 解析失败: " + e.getMessage());
        }
    }
}
