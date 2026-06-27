package org.example.agent_qr.dataquality.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.event.DataSyncCompletedEvent;
import org.example.agent_qr.dataquality.service.DataQualityService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 数据同步完成事件监听器。
 * <p>
 * 监听 {@link DataSyncCompletedEvent}，异步触发数据质量检查。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncListener {

    private final DataQualityService qualityService;

    /**
     * 监听数据同步完成事件，触发质量检查。
     *
     * @param event 同步完成事件
     */
    @Async
    @EventListener
    public void onDataSyncCompleted(DataSyncCompletedEvent event) {
        log.info("收到数据同步完成事件: datasourceId={}, sourceName={}, batchId={}, rows={}",
                event.getDatasourceId(), event.getSourceName(), event.getSyncBatchId(),
                event.getRawData() != null ? event.getRawData().size() : 0);

        try {
            // 执行质量检查并持久化
            qualityService.executeAndSave(
                    event.getSyncBatchId(),
                    event.getDatasourceId(),
                    event.getSourceName(),
                    event.getRawData()
            );
        } catch (Exception e) {
            log.error("同步后质量检查失败: datasourceId={}, batchId={}, error={}",
                    event.getDatasourceId(), event.getSyncBatchId(), e.getMessage(), e);
        }
    }
}
