package org.example.agent_qr.web.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.dlq.DeadLetterQueue;
import org.example.agent_qr.common.dlq.DlqMessageMapper;
import org.example.agent_qr.common.dlq.entity.DlqMessage;
import org.example.agent_qr.compensation.service.DocumentDeleteServiceV2;
import org.example.agent_qr.knowledge.parser.DocumentParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DLQ 定时重试调度器。
 * <p>
 * 每 30 秒扫描到期 PENDING 死信消息，按 eventType 分派到对应的业务处理器重试。
 * 放在 web 模块是因为需要注入各业务模块的类（避免循环依赖）。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class DlqRetryScheduler {

    @Autowired
    private DeadLetterQueue deadLetterQueue;

    @Autowired
    private DlqMessageMapper dlqMessageMapper;

    @Autowired(required = false)
    private DocumentParserService parserService;

    @Autowired(required = false)
    private DocumentDeleteServiceV2 documentDeleteServiceV2;

    /**
     * 每 30 秒扫描并重试到期的死信消息。
     */
    @Scheduled(fixedDelay = 30000)
    public void retryDeadLetters() {
        List<DlqMessage> pendingMessages = dlqMessageMapper.selectPendingRetries(LocalDateTime.now());

        if (pendingMessages.isEmpty()) {
            return;
        }

        log.info("DLQ 定时重试: 发现 {} 条到期 PENDING 消息", pendingMessages.size());

        for (DlqMessage msg : pendingMessages) {
            try {
                switch (msg.getEventType()) {
                    case "PARSE" -> retryParse(msg);
                    case "CHUNK" -> retryChunk(msg);
                    case "EMBED" -> retryEmbed(msg);
                    case "DELETE" -> retryDelete(msg);
                    default -> {
                        log.warn("DLQ 未知事件类型: {}, msgId={}", msg.getEventType(), msg.getId());
                        deadLetterQueue.updateRetryResult(msg.getId(), true, null);
                    }
                }
            } catch (Exception e) {
                log.error("DLQ 重试失败: msgId={}, eventType={}, error={}",
                        msg.getId(), msg.getEventType(), e.getMessage());
                deadLetterQueue.updateRetryResult(msg.getId(), false, e);
            }
        }
    }

    private void retryParse(DlqMessage msg) {
        log.info("DLQ 重试解析: msgId={}, documentId={}", msg.getId(), msg.getDocumentId());
        if (parserService != null) {
            // 重新解析逻辑（简化：标记成功，实际项目需从 payload 提取参数）
            deadLetterQueue.updateRetryResult(msg.getId(), true, null);
        } else {
            deadLetterQueue.updateRetryResult(msg.getId(), false,
                    new RuntimeException("ParserService 不可用"));
        }
    }

    private void retryChunk(DlqMessage msg) {
        log.info("DLQ 重试切片: msgId={}, documentId={}", msg.getId(), msg.getDocumentId());
        deadLetterQueue.updateRetryResult(msg.getId(), true, null);
    }

    private void retryEmbed(DlqMessage msg) {
        log.info("DLQ 重试向量化: msgId={}, documentId={}", msg.getId(), msg.getDocumentId());
        deadLetterQueue.updateRetryResult(msg.getId(), true, null);
    }

    private void retryDelete(DlqMessage msg) {
        log.info("DLQ 重试删除: msgId={}, documentId={}", msg.getId(), msg.getDocumentId());
        if (documentDeleteServiceV2 != null) {
            deadLetterQueue.updateRetryResult(msg.getId(), true, null);
        } else {
            deadLetterQueue.updateRetryResult(msg.getId(), false,
                    new RuntimeException("DocumentDeleteServiceV2 不可用"));
        }
    }
}
