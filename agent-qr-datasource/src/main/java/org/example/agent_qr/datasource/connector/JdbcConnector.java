package org.example.agent_qr.datasource.connector;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.datasource.dto.ConnectionTestResult;
import org.example.agent_qr.datasource.dto.SyncContext;
import org.example.agent_qr.datasource.dto.SyncResult;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC 数据源连接器。
 * <p>
 * 通过 JDBC 协议连接关系型数据库，支持全量同步和基于游标字段的增量同步。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class JdbcConnector implements DataSourceConnector {

    @Override
    public String getType() {
        return "JDBC";
    }

    @Override
    public ConnectionTestResult testConnection(Map<String, Object> config) {
        String url = (String) config.get("url");
        String username = (String) config.get("username");
        String password = (String) config.get("password");

        long start = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            DatabaseMetaData meta = conn.getMetaData();
            long latency = System.currentTimeMillis() - start;
            return ConnectionTestResult.ok(latency,
                    meta.getDatabaseProductName(),
                    meta.getDatabaseProductVersion());
        } catch (Exception e) {
            log.error("JDBC 连接测试失败: url={}, error={}", url, e.getMessage());
            return ConnectionTestResult.fail(e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public SyncResult fullSync(SyncContext context) {
        Map<String, Object> config = context.getConfig();
        String url = (String) config.get("url");
        String username = (String) config.get("username");
        String password = (String) config.get("password");
        List<String> tableNames = (List<String>) config.get("tableNames");

        if (tableNames == null || tableNames.isEmpty()) {
            log.warn("JDBC 全量同步：未指定表名");
            return SyncResult.empty();
        }

        List<Map<String, Object>> allRows = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            for (String table : tableNames) {
                String sql = "SELECT * FROM " + table;
                try (PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            row.put(meta.getColumnName(i), rs.getObject(i));
                        }
                        allRows.add(row);
                    }
                }
                log.info("JDBC 全量同步: 表 {} 读取 {} 行", table, allRows.size());
            }
        } catch (Exception e) {
            log.error("JDBC 全量同步失败: {}", e.getMessage(), e);
        }

        return new SyncResult(allRows.size(), allRows, null);
    }

    @Override
    public SyncResult incrementalSync(SyncContext context, String lastCursor) {
        Map<String, Object> config = context.getConfig();
        String url = (String) config.get("url");
        String username = (String) config.get("username");
        String password = (String) config.get("password");
        String cursorField = (String) config.get("cursorField");
        String tableName = (String) config.get("tableName");

        if (cursorField == null || tableName == null) {
            log.warn("JDBC 增量同步：缺少游标字段或表名");
            return SyncResult.empty();
        }

        List<Map<String, Object>> allRows = new ArrayList<>();
        String newCursor = lastCursor;

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT * FROM " + tableName
                    + " WHERE " + cursorField + " > ? ORDER BY " + cursorField + " ASC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, lastCursor != null ? lastCursor : "");
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            row.put(meta.getColumnName(i), rs.getObject(i));
                        }
                        Object cursorVal = row.get(cursorField);
                        if (cursorVal != null) {
                            newCursor = cursorVal.toString();
                        }
                        allRows.add(row);
                    }
                }
            }
            log.info("JDBC 增量同步: 表 {} 读取 {} 行, 新游标={}", tableName, allRows.size(), newCursor);
        } catch (Exception e) {
            log.error("JDBC 增量同步失败: {}", e.getMessage(), e);
        }

        return new SyncResult(allRows.size(), allRows, newCursor);
    }
}
