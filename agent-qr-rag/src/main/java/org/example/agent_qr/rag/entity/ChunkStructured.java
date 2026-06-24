package org.example.agent_qr.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 切片结构化字段实体，对应数据库表 kb_chunk_structured。
 * <p>
 * 存储知识库切片的结构化元数据（数值、日期、枚举等），
 * 支持 MySQL B+ 树前置过滤，减少向量检索范围。
 * </p>
 *
 * @author agent-qr
 */
@Data
@TableName("kb_chunk_structured")
public class ChunkStructured {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的切片 ID */
    private Long chunkId;

    /** 所属业务域 */
    private String domain;

    /** 字段名 */
    private String fieldName;

    /** 字段值（字符串形式） */
    private String fieldValue;

    /** 数值型值（用于范围查询） */
    private BigDecimal numericValue;

    /** 日期型值（用于范围查询） */
    private LocalDate dateValue;

    /** 字段类型：NUMBER / DATE / ENUM / STRING */
    private String fieldType;

    /** 字段类型常量 */
    public static final String TYPE_NUMBER = "NUMBER";
    public static final String TYPE_DATE = "DATE";
    public static final String TYPE_ENUM = "ENUM";
    public static final String TYPE_STRING = "STRING";
}
