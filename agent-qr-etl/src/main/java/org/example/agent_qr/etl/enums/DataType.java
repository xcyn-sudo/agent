package org.example.agent_qr.etl.enums;

/**
 * 数据类型枚举，用于 ETL 管道中的数据分类。
 *
 * @author agent-qr
 */
public enum DataType {

    /** 结构化数据（数据表/CSV/数据库行） */
    STRUCTURED("结构化"),

    /** 半结构化数据（JSON/XML） */
    SEMI_STRUCTURED("半结构化"),

    /** 非结构化数据（PDF/DOCX/TXT 文件） */
    UNSTRUCTURED("非结构化");

    private final String displayName;

    DataType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
