package org.example.agent_qr.rag.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息实体类，对应数据库表 chat_message。
 * <p>
 * 记录会话中的每条消息，包括用户消息和 AI 助手消息。
 * AI 消息的来源引用以 JSON 格式存储在 sources 字段中。
 * </p>
 *
 * @author agent-qr
 */
@Data
@TableName("chat_message")
public class Message {

    /**
     * 消息主键 ID，自增。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属会话 ID。
     */
    private Long conversationId;

    /**
     * 消息角色：user（用户）或 assistant（助手）。
     */
    private String role;

    /**
     * 消息内容。
     */
    private String content;

    /**
     * 引用来源，JSON 格式存储检索到的文档信息。
     */
    private String sources;

    /**
     * 创建时间，插入时自动填充。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ==================== P2 满意度反馈字段 ====================

    /**
     * 满意度反馈：positive（点赞）/ negative（点踩）。
     */
    private String feedback;

    /**
     * 反馈原因（可选）。
     */
    private String feedbackReason;
}
