package org.example.agent_qr.common.dlq;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.agent_qr.common.dlq.entity.DlqMessage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 死信队列消息 Mapper，提供 DLQ 消息的数据库操作。
 *
 * @author agent-qr
 */
@Mapper
public interface DlqMessageMapper extends BaseMapper<DlqMessage> {

    /**
     * 查询所有到期且状态为 PENDING 的重试记录。
     *
     * @param now 当前时间
     * @return 到期 PENDING 记录列表
     */
    @Select("SELECT * FROM dlq_message WHERE status = 'PENDING' AND next_retry_at <= #{now} ORDER BY next_retry_at ASC")
    List<DlqMessage> selectPendingRetries(@Param("now") LocalDateTime now);

    /**
     * 更新重试结果（递增重试次数并更新下次重试时间和错误信息）。
     *
     * @param id          消息 ID
     * @param retryCount  新的重试次数
     * @param nextRetryAt 下次重试时间
     * @param errorMsg    错误信息
     * @return 影响行数
     */
    @Update("UPDATE dlq_message SET retry_count = #{retryCount}, next_retry_at = #{nextRetryAt}, error_msg = #{errorMsg} WHERE id = #{id}")
    int updateRetry(@Param("id") Long id,
                    @Param("retryCount") Integer retryCount,
                    @Param("nextRetryAt") LocalDateTime nextRetryAt,
                    @Param("errorMsg") String errorMsg);

    /**
     * 更新消息状态。
     *
     * @param id     消息 ID
     * @param status 新状态（PENDING / DEAD）
     * @param errorMsg 错误信息（可选）
     * @return 影响行数
     */
    @Update("UPDATE dlq_message SET status = #{status}, error_msg = #{errorMsg} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("errorMsg") String errorMsg);
}
