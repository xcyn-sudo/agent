package org.example.agent_qr.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ETL 处理完成事件。
 * <p>
 * 由 etl 模块在数据标准化处理完成后发布，
 * catalog 模块监听并更新知识目录索引。
 * </p>
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataETLedEvent {

    /** 业务域 */
    private String domain;

    /** 数据源名称 */
    private String sourceName;

    /** 处理的实体（记录）数量 */
    private Integer entityCount;

    /** 同步批次 ID（UUID） */
    private String syncBatchId;
}
