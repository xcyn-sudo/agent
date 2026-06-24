package org.example.agent_qr.rag.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话实体类，对应数据库表 chat_conversation。
 * <p>
 * 记录用户与 AI 之间的对话会话，包含会话标题、消息计数和时间信息。
 * </p>
 *
 * @author agent-qr
 */
@Data
@TableName("chat_conversation")
public class Conversation {

    /**
     * 会话主键 ID，自增。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户 ID。
     */
    private Long userId;

    /**
     * 会话标题，通常取首条用户消息的前 30 个字符。
     */
    private String title;

    /**
     * 会话中的消息数量，默认 0。
     */
    private Integer messageCount;

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
}
