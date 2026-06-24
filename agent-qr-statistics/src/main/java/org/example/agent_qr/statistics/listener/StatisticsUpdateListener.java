package org.example.agent_qr.statistics.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.event.AnswerGeneratedEvent;
import org.example.agent_qr.common.event.EmbeddingCompletedEvent;
import org.example.agent_qr.statistics.entity.DailyStats;
import org.example.agent_qr.statistics.mapper.DailyStatsMapper;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 统计数据更新监听器，异步响应领域事件以更新每日统计。
 * <p>
 * 监听 {@link EmbeddingCompletedEvent} 和 {@link AnswerGeneratedEvent}，
 * 通过 "statExecutor" 线程池异步执行，避免阻塞主业务流程。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsUpdateListener {

    private final DailyStatsMapper dailyStatsMapper;

    /**
     * 处理向量化完成事件，更新当日文档上传计数。
     * <p>
     * 如果当天统计记录不存在则创建新记录（docUploadCount=1），
     * 否则将 doc_upload_count 加 1。
     * </p>
     *
     * @param event 向量化完成事件，包含 documentId 和 chunkCount
     */
    @EventListener
    @Async("statExecutor")
    public void handleEmbeddingCompleted(EmbeddingCompletedEvent event) {
        try {
            LocalDate today = LocalDate.now();
            DailyStats stats = dailyStatsMapper.selectByDate(today);

            if (stats == null) {
                DailyStats newStats = new DailyStats();
                newStats.setStatDate(today);
                newStats.setDocUploadCount(1);
                dailyStatsMapper.insert(newStats);
                log.info("创建今日统计记录并设置文档上传数为 1: date={}", today);
            } else {
                dailyStatsMapper.incrementDocUploadCount(today);
                log.info("今日文档上传计数 +1: date={}, documentId={}", today, event.getDocumentId());
            }
        } catch (Exception e) {
            log.error("处理向量化完成事件失败: documentId={}", event.getDocumentId(), e);
        }
    }

    /**
     * 处理问答回答生成完成事件，更新当日问答计数。
     * <p>
     * 如果当天统计记录不存在则创建新记录（qaCount=1, userQuestionCount=1, activeUserCount=1），
     * 否则将 qa_count 加 1，并根据 userId 决定是否增加 active_user_count。
     * </p>
     *
     * @param event 问答回答生成事件，包含 userId 和 conversationId
     */
    @EventListener
    @Async("statExecutor")
    public void handleAnswerGenerated(AnswerGeneratedEvent event) {
        try {
            LocalDate today = LocalDate.now();
            DailyStats stats = dailyStatsMapper.selectByDate(today);

            if (stats == null) {
                DailyStats newStats = new DailyStats();
                newStats.setStatDate(today);
                newStats.setQaCount(1);
                newStats.setUserQuestionCount(1);
                newStats.setActiveUserCount(1);
                dailyStatsMapper.insert(newStats);
                log.info("创建今日统计记录并设置问答数为 1: date={}, userId={}", today, event.getUserId());
            } else {
                dailyStatsMapper.incrementQaCount(today, event.getUserId());
                log.info("今日问答计数 +1: date={}, userId={}", today, event.getUserId());
            }
        } catch (Exception e) {
            log.error("处理问答回答生成事件失败: userId={}, conversationId={}",
                    event.getUserId(), event.getConversationId(), e);
        }
    }
}
