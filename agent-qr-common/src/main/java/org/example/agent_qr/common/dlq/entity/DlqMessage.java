package org.example.agent_qr.common.dlq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 死信队列消息实体，对应数据库表 dlq_message。
 * <p>
 * 记录失败操作的事件类型、关联文档 ID、原始负载、错误信息和重试状态，
 * 支持指数退避重试策略。
 * </p>
 *
 * @author agent-qr
 */
@Data
@TableName("dlq_message")
public class DlqMessage {

    /**
     * 主键 ID，自增。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 事件类型：PARSE / CHUNK / EMBED / DELETE。
     */
    private String eventType;

    /**
     * 关联的文档 ID。
     */
    private Long documentId;

    /**
     * 原始负载数据（JSON 格式）。
     */
    private String payload;

    /**
     * 错误信息。
     */
    private String errorMsg;

    /**
     * 当前重试次数，初始为 0。
     */
    private Integer retryCount;

    /**
     * 下次重试时间。
     */
    private LocalDateTime nextRetryAt;

    /**
     * 状态：PENDING / DEAD。
     */
    private String status;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 死信消息状态常量。
     */
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_DEAD = "DEAD";
}
