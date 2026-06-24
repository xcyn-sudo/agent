package org.example.agent_qr.catalog.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据源节点（二级目录）。
 * <p>
 * 对应一个数据源配置，包含该数据源下的实体列表。
 * </p>
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceNode {

    /** 数据源 ID */
    private Long sourceId;

    /** 数据源名称 */
    private String sourceName;

    /** 数据源类型：JDBC / REST / S3 */
    private String sourceType;

    /** 最后同步时间 */
    private LocalDateTime lastSyncAt;

    /** 累计同步总数 */
    private Integer totalSynced;

    /** 该数据源下的实体列表（三级目录） */
    private List<EntityNode> entities = new ArrayList<>();
}
