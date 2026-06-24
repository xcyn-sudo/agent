package org.example.agent_qr.etl.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.agent_qr.etl.enums.DataType;

import java.util.Map;

/**
 * 标准化记录 — ETL 管道的最终输出。
 * <p>
 * 将异构数据源的数据统一转换为标准格式，
 * 包含来源信息、分类、标准化文本内容和元数据。
 * </p>
 *
 * @author agent-qr
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalRecord {

    /** 来源系统/数据源名称 */
    private String sourceSystem;

    /** 所属业务域 */
    private String domain;

    /** 数据类型 */
    private DataType dataType;

    /** 标准化后的自然语言文本 */
    private String canonicalText;

    /** 元数据（字段名 → 字段值） */
    private Map<String, Object> metadata;

    /** 数据源配置 ID */
    private Long datasourceId;

    /** 同步批次 ID */
    private String syncBatchId;
}
