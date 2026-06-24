package org.example.agent_qr.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * 数据同步上下文，封装单次同步任务所需的全部参数。
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncContext {

    /** 数据源配置 ID */
    private Long datasourceId;

    /** 数据源连接配置 */
    private Map<String, Object> config;

    /** 同步批次 ID（UUID） */
    private String syncBatchId;

    /**
     * 创建同步上下文（自动生成批次 ID）。
     */
    public SyncContext(Long datasourceId, Map<String, Object> config) {
        this.datasourceId = datasourceId;
        this.config = config;
        this.syncBatchId = UUID.randomUUID().toString();
    }
}
