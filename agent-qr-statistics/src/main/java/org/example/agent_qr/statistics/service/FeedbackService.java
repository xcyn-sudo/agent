package org.example.agent_qr.statistics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.BusinessException;
import org.example.agent_qr.rag.entity.Message;
import org.example.agent_qr.rag.mapper.MessageMapper;
import org.example.agent_qr.statistics.mapper.DailyStatsMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 满意度反馈服务（P2 新增）。
 * <p>
 * 处理用户对 AI 回答的点赞/点踩反馈，
 * 更新消息记录和每日统计数据。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final MessageMapper messageMapper;
    private final DailyStatsMapper dailyStatsMapper;

    /**
     * 提交满意度反馈。
     *
     * @param messageId 消息 ID
     * @param feedback  反馈类型：positive / negative
     * @param reason    反馈原因（可选）
     * @param userId    用户 ID
     */
    @Transactional
    public void submitFeedback(Long messageId, String feedback, String reason, Long userId) {
        // 1. 校验消息存在且为 AI 回答
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException("消息不存在");
        }
        if (!"assistant".equals(message.getRole())) {
            throw new BusinessException("只能对 AI 回答进行反馈");
        }

        // 2. 更新消息反馈
        messageMapper.updateFeedback(messageId, feedback, reason);

        // 3. 更新每日统计
        LocalDate today = LocalDate.now();
        if ("positive".equals(feedback)) {
            dailyStatsMapper.incrementPositiveCount(today);
        } else if ("negative".equals(feedback)) {
            dailyStatsMapper.incrementNegativeCount(today);
        }

        log.info("满意度反馈已提交: messageId={}, feedback={}, userId={}", messageId, feedback, userId);
    }
}
