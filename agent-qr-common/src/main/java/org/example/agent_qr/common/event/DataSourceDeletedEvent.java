package org.example.agent_qr.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据源删除事件。
 * <p>
 * 由 datasource 模块在物理删除数据源后发布，
 * knowledge 模块监听此事件执行级联清理：
 * 切片软删除、ChromaDB 向量移除、BM25 索引移除、结构化元数据清理。
 * 清理失败时由 OrphanVectorScanner 兜底。
 * </p>
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceDeletedEvent {

    /** 被删除的数据源 ID */
    private Long datasourceId;
}
