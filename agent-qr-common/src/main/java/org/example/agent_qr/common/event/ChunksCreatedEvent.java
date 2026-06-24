package org.example.agent_qr.common.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 文本切片创建完成事件。
 */
@Getter
@Setter
public class ChunksCreatedEvent extends ApplicationEvent {

    private Long documentId;
    private List<String> chunks;

    public ChunksCreatedEvent(Object source, Long documentId, List<String> chunks) {
        super(source);
        this.documentId = documentId;
        this.chunks = chunks;
    }
}
