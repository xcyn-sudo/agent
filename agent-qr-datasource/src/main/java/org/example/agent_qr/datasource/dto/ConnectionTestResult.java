package org.example.agent_qr.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据源连通性测试结果。
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionTestResult {

    /** 是否连接成功 */
    private boolean success;

    /** 连接延迟（毫秒） */
    private long latencyMs;

    /** 数据库产品名称（JDBC 数据源适用） */
    private String dbProduct;

    /** 数据库版本（JDBC 数据源适用） */
    private String dbVersion;

    /** 错误信息（连接失败时） */
    private String errorMsg;

    /**
     * 创建成功的测试结果。
     */
    public static ConnectionTestResult ok(long latencyMs, String dbProduct, String dbVersion) {
        return new ConnectionTestResult(true, latencyMs, dbProduct, dbVersion, null);
    }

    /**
     * 创建失败的测试结果。
     */
    public static ConnectionTestResult fail(String errorMsg) {
        return new ConnectionTestResult(false, 0, null, null, errorMsg);
    }
}
