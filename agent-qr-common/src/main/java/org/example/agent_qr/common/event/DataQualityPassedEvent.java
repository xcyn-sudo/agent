package org.example.agent_qr.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 数据质量检查通过事件。
 * <p>
 * 由 data-quality 模块在质量检查通过后发布，
 * etl 模块监听并触发 ETL 标准化管道处理。
 * </p>
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityPassedEvent {

    /** 质量报告（JSON 格式的描述信息） */
    private String report;

    /** 通过质量检查的数据 */
    private List<Map<String, Object>> passedData;

    /** 数据源 ID */
    private Long datasourceId;

    /** 同步批次 ID（UUID） */
    private String syncBatchId;

    /**
     * 轻量构造器 — 通知数据源 ID 和批次 ID，下游模块自行查询报告。
     *
     * @param datasourceId 数据源 ID
     * @param syncBatchId  同步批次 ID
     */
    public DataQualityPassedEvent(Long datasourceId, String syncBatchId) {
        this.datasourceId = datasourceId;
        this.syncBatchId = syncBatchId;
    }
}
