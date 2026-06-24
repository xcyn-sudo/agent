package org.example.agent_qr.catalog.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 实体节点（三级目录 — 叶子节点）。
 * <p>
 * 对应数据源中的一个实体（数据表/文件/API 端点）。
 * </p>
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityNode {

    /** 实体名称（表名/文件名/端点路径） */
    private String entityName;

    /** 实体类型：TABLE / FILE / API */
    private String entityType;

    /** 记录数 */
    private Integer recordCount;

    /** 实体类型常量 */
    public static final String TYPE_TABLE = "TABLE";
    public static final String TYPE_FILE = "FILE";
    public static final String TYPE_API = "API";
}
