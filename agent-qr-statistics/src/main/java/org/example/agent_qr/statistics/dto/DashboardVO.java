package org.example.agent_qr.statistics.dto;

import lombok.Data;
import org.example.agent_qr.statistics.entity.DailyStats;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘视图对象，封装仪表盘页面所需的全部统计数据。
 * <p>
 * 包含今日实时指标、累计总量、周趋势折线图数据以及文档类型分布。
 * </p>
 *
 * @author agent-qr
 */
@Data
public class DashboardVO {

    /**
     * 今日问答总数（问答总数，含用户提问和系统回答）。
     */
    private Integer todayQA;

    /**
     * 今日新增用户数。
     */
    private Integer todayNewUsers;

    /**
     * 知识库文档总数。
     */
    private Long totalDocuments;

    /**
     * 文档分块总数。
     */
    private Long totalChunks;

    /**
     * 系统注册用户总数。
     */
    private Long totalUsers;

    /**
     * 最近 7 天每日统计数据，用于绘制趋势折线图。
     */
    private List<DailyStats> weeklyTrend;

    /**
     * 文档类型分布，key 为文档类型（如 PDF、DOCX），value 为该类型的文档数量。
     */
    private Map<String, Long> docTypeDistribution;

    // ==================== P2 满意度指标 ====================

    /**
     * 今日点赞数。
     */
    private Integer todayPositive;

    /**
     * 今日点踩数。
     */
    private Integer todayNegative;

    /**
     * 满意度评分：positive / (positive + negative)。
     */
    private Double satisfactionRate;

    /**
     * 累计反馈总数。
     */
    private Integer totalFeedbackCount;
}
