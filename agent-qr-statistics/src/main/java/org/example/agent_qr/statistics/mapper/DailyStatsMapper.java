package org.example.agent_qr.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.agent_qr.statistics.entity.DailyStats;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日统计数据访问层，提供统计数据的 CRUD 及聚合查询。
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，自动拥有基础 CRUD 方法，
 * 同时扩展了按日期查询、增量更新、周趋势查询等自定义方法。
 * </p>
 *
 * @author agent-qr
 */
@Mapper
public interface DailyStatsMapper extends BaseMapper<DailyStats> {

    /**
     * 根据统计日期查询当天的统计数据。
     *
     * @param date 统计日期
     * @return 匹配的统计实体，不存在则返回 null
     */
    @Select("SELECT * FROM stat_daily WHERE stat_date = #{date}")
    DailyStats selectByDate(@Param("date") LocalDate date);

    /**
     * 将指定日期的文档上传计数加 1。
     *
     * @param date 统计日期
     * @return 影响的行数
     */
    @Update("UPDATE stat_daily SET doc_upload_count = doc_upload_count + 1 WHERE stat_date = #{date}")
    int incrementDocUploadCount(@Param("date") LocalDate date);

    /**
     * 将指定日期的问答计数加 1，若提供了 userId 则同时增加活跃用户计数。
     *
     * @param date   统计日期
     * @param userId 用户 ID，用于判断是否为活跃用户统计
     * @return 影响的行数
     */
    @Update("<script>" +
            "UPDATE stat_daily SET qa_count = qa_count + 1, user_question_count = user_question_count + 1" +
            "<if test='userId != null'>, active_user_count = active_user_count + 1</if>" +
            " WHERE stat_date = #{date}" +
            "</script>")
    int incrementQaCount(@Param("date") LocalDate date, @Param("userId") Long userId);

    /**
     * 查询指定结束日期往前 7 天（含当天）的统计数据，按日期升序排列。
     *
     * @param endDate 结束日期
     * @return 最近 7 天的统计记录列表
     */
    @Select("SELECT * FROM stat_daily WHERE stat_date >= DATE_SUB(#{endDate}, INTERVAL 6 DAY) AND stat_date <= #{endDate} ORDER BY stat_date ASC")
    List<DailyStats> selectWeeklyTrend(@Param("endDate") LocalDate endDate);

    // ==================== P2 满意度统计方法 ====================

    /**
     * 点赞数 +1。
     */
    @Update("UPDATE stat_daily SET positive_count = positive_count + 1 WHERE stat_date = #{date}")
    int incrementPositiveCount(@Param("date") LocalDate date);

    /**
     * 点踩数 +1。
     */
    @Update("UPDATE stat_daily SET negative_count = negative_count + 1 WHERE stat_date = #{date}")
    int incrementNegativeCount(@Param("date") LocalDate date);
}
