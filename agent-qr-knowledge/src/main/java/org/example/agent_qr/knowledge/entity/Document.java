package org.example.agent_qr.knowledge.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.example.agent_qr.knowledge.enums.DocumentStatus;

import java.time.LocalDateTime;

/**
 * 知识库文档实体类，对应数据库表 kb_document。
 * <p>
 * 记录上传到知识库的文档元信息，包括文件名、类型、大小、
 * 处理状态等。时间字段由 MyBatis-Plus 自动填充。
 * status 字段使用 DocumentStatus 枚举，由 MyBatis-Plus {@code @EnumValue} 自动转换。
 * </p>
 *
 * @author agent-qr
 */
@Data
@TableName("kb_document")
public class Document {

    /**
     * 主键 ID，自增。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文档标题。
     */
    private String title;

    /**
     * 原始文件名。
     */
    private String fileName;

    /**
     * 文件存储路径。
     */
    private String filePath;

    /**
     * 文件类型（扩展名，如 pdf、docx、txt）。
     */
    private String fileType;

    /**
     * 文件大小（字节）。
     */
    private Long fileSize;

    /**
     * 处理状态，使用 DocumentStatus 枚举，由 MyBatis-Plus {@code @EnumValue} 自动映射。
     */
    private DocumentStatus status;

    /**
     * 上传用户 ID。
     */
    private Long uploadUserId;

    /**
     * 错误信息，处理失败时记录。
     */
    private String errorMsg;

    /**
     * 创建时间，插入时自动填充。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间，插入和更新时自动填充。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== P2 新增字段 ====================

    /**
     * 所属业务域（HR / FINANCE / RD / SALES / COMMON）。
     */
    private String domain;

    /**
     * 数据敏感级别：0=公开 / 1=内部 / 2=机密 / 3=绝密。
     */
    private Integer sensitivityLevel;

    /**
     * 敏感级别标签。
     */
    private String sensitivityLabel;

    /**
     * 软删除标记：0=未删除 / 1=已删除。
     */
    private Integer deleted;
}
