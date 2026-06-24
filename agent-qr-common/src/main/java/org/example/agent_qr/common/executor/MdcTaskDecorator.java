package org.example.agent_qr.common.executor;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MDC 任务装饰器，将主线程的 MDC 上下文传递到异步工作线程。
 * <p>
 * 在 {@link org.springframework.scheduling.annotation.Async} 标注的方法执行前，
 * 将父线程的 MDC 复制到子线程，执行完毕后清理子线程 MDC，确保 TraceId 全链路可追踪。
 * </p>
 *
 * @author agent-qr
 */
@Component
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 复制当前线程的 MDC 上下文
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
