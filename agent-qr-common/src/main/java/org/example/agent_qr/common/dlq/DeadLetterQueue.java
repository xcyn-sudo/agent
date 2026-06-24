package org.example.agent_qr.common.dlq;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.dlq.entity.DlqMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 死信队列管理器，提供消息入队、重试结果更新和指数退避计算。
 * <p>
 * 重试策略：指数退避 3^1=3s → 3^2=9s → 3^3=27s → 3^4=81s，
 * 最多重试 4 次，超限后标记为 DEAD 状态。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class DeadLetterQueue {

    @Autowired
    private DlqMessageMapper dlqMessageMapper;

    /**
     * 最大重试次数，默认 4 次。
     */
    @Value("${agent-qr.dlq.max-retries:4}")
    private int maxRetries;

    /**
     * 退避基数，默认 3 秒。
     */
    @Value("${agent-qr.dlq.backoff-base:3}")
    private int backoffBase;

    /**
     * 将失败操作入队到死信队列。
     * <p>
     * 首次重试延迟 = backoffBase 秒。
     * </p>
     *
     * @param eventType  事件类型（PARSE / CHUNK / EMBED / DELETE）
     * @param documentId 关联的文档 ID
     * @param payload    原始负载（JSON 格式）
     * @param error      异常信息
     */
    public void enqueue(String eventType, Long documentId, String payload, Throwable error) {
        DlqMessage msg = new DlqMessage();
        msg.setEventType(eventType);
        msg.setDocumentId(documentId);
        msg.setPayload(payload);
        msg.setErrorMsg(error != null ? error.getMessage() : "未知错误");
        msg.setRetryCount(0);
        msg.setNextRetryAt(LocalDateTime.now().plusSeconds(backoffBase));
        msg.setStatus(DlqMessage.STATUS_PENDING);
        msg.setCreateTime(LocalDateTime.now());

        dlqMessageMapper.insert(msg);
        log.warn("死信队列入队: eventType={}, documentId={}, msgId={}, nextRetryAt={}",
                eventType, documentId, msg.getId(), msg.getNextRetryAt());
    }

    /**
     * 更新重试结果。
     * <p>
     * 成功则删除记录，失败则递增重试次数并计算下次重试时间，
     * 超过最大重试次数标记为 DEAD。
     * </p>
     *
     * @param msgId  消息 ID
     * @param success 是否成功
     * @param error   失败时的异常（成功时为 null）
     */
    public void updateRetryResult(Long msgId, boolean success, Throwable error) {
        if (success) {
            dlqMessageMapper.deleteById(msgId);
            log.info("DLQ 重试成功，消息已删除: msgId={}", msgId);
            return;
        }

        DlqMessage msg = dlqMessageMapper.selectById(msgId);
        if (msg == null) {
            log.warn("DLQ 消息不存在: msgId={}", msgId);
            return;
        }

        int newRetryCount = msg.getRetryCount() + 1;
        if (newRetryCount >= maxRetries) {
            dlqMessageMapper.updateStatus(msgId, DlqMessage.STATUS_DEAD,
                    error != null ? error.getMessage() : "超过最大重试次数");
            log.error("DLQ 重试耗尽，标记为 DEAD: msgId={}, eventType={}, documentId={}, retryCount={}",
                    msgId, msg.getEventType(), msg.getDocumentId(), newRetryCount);
        } else {
            long backoffSeconds = calcBackoffSeconds(newRetryCount);
            LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(backoffSeconds);
            dlqMessageMapper.updateRetry(msgId, newRetryCount, nextRetryAt,
                    error != null ? error.getMessage() : "重试失败");
            log.warn("DLQ 重试失败，将在 {} 秒后重试: msgId={}, retryCount={}, nextRetryAt={}",
                    backoffSeconds, msgId, newRetryCount, nextRetryAt);
        }
    }

    /**
     * 计算指数退避延迟秒数。
     * <p>
     * 公式：backoffBase ^ (retryCount + 1) 秒，
     * 即 3s → 9s → 27s → 81s。
     * </p>
     *
     * @param retryCount 当前重试次数（从 0 开始）
     * @return 退避延迟秒数
     */
    public long calcBackoffSeconds(int retryCount) {
        return (long) Math.pow(backoffBase, retryCount + 1);
    }
}
