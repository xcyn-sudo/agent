package org.example.agent_qr.statistics.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日统计数据实体，映射到 stat_daily 表。
 * <p>
 * 记录每天的系统运行指标，包括问答次数、活跃用户数、文档上传数等，
 * 用于仪表盘展示和趋势分析。
 * </p>
 *
 * @author agent-qr
 */
@Data
@TableName("stat_daily")
public class DailyStats {

    /**
     * 主键 ID，自增。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 统计日期。
     */
    private LocalDate statDate;

    /**
     * 当日问答总数（用户提问 + 系统回答），默认为 0。
     */
    private Integer qaCount = 0;

    /**
     * 当日用户提问次数，默认为 0。
     */
    private Integer userQuestionCount = 0;

    /**
     * 当日活跃用户数，默认为 0。
     */
    private Integer activeUserCount = 0;

    /**
     * 当日文档上传数，默认为 0。
     */
    private Integer docUploadCount = 0;

    /**
     * 记录创建时间，插入时自动填充。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ==================== P2 满意度字段 ====================

    /**
     * 点赞数，默认为 0。
     */
    private Integer positiveCount = 0;

    /**
     * 点踩数，默认为 0。
     */
    private Integer negativeCount = 0;

    /**
     * 满意率 = positiveCount / (positiveCount + negativeCount)，0-1 之间的小数。
     * 不映射数据库字段，仅用于前端展示。
     */
    @TableField(exist = false)
    private Double satisfactionRate;
}
