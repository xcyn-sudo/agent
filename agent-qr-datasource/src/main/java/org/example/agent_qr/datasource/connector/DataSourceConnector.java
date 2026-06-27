package org.example.agent_qr.datasource.connector;

import org.example.agent_qr.datasource.dto.ConnectionTestResult;
import org.example.agent_qr.datasource.dto.SyncContext;
import org.example.agent_qr.datasource.dto.SyncResult;

import java.util.List;
import java.util.Map;

/**
 * 数据源连接器策略接口。
 * <p>
 * 定义统一的数据源接入协议，各类型数据源（JDBC/REST/S3）
 * 实现此接口以提供连接测试、全量同步和增量同步能力。
 * </p>
 *
 * @author agent-qr
 */
public interface DataSourceConnector {

    /**
     * 获取连接器类型标识。
     *
     * @return 类型：JDBC / REST / S3
     */
    String getType();

    /**
     * 测试数据源连通性。
     *
     * @param config 连接配置（JSON 解析后的 Map）
     * @return 连通性测试结果
     */
    ConnectionTestResult testConnection(Map<String, Object> config);

    /**
     * 执行全量数据同步。
     *
     * @param context 同步上下文（含数据源 ID、配置、批次 ID）
     * @return 同步结果（含原始数据和游标）
     */
    SyncResult fullSync(SyncContext context);

    /**
     * 执行增量数据同步（基于游标）。
     *
     * @param context    同步上下文
     * @param lastCursor 上次同步的游标位置
     * @return 同步结果（含增量数据和更新后的游标）
     */
    SyncResult incrementalSync(SyncContext context, String lastCursor);

    /**
     * 检测指定表的字段（列名）列表。
     * 默认实现返回空列表（REST/S3 不支持）。
     *
     * @param config    连接配置（JSON 解析后的 Map）
     * @param tableName 表名
     * @return 字段名列表
     */
    default List<String> detectColumns(Map<String, Object> config, String tableName) {
        return List.of();
    }
}
