package org.example.agent_qr.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 数据同步完成事件。
 * <p>
 * 由 datasource 模块在数据源同步完成后发布，
 * data-quality 模块监听并触发数据质量检查。
 * </p>
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataSyncCompletedEvent {

    /** 数据源 ID */
    private Long datasourceId;

    /** 同步获取的原始数据 */
    private List<Map<String, Object>> rawData;

    /** 同步批次 ID（UUID） */
    private String syncBatchId;
}
