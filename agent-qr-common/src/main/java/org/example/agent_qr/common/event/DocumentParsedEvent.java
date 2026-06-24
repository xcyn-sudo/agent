package org.example.agent_qr.common.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * 文档解析完成事件。
 */
@Getter
@Setter
public class DocumentParsedEvent extends ApplicationEvent {

    private Long documentId;
    private String content;

    public DocumentParsedEvent(Object source, Long documentId, String content) {
        super(source);
        this.documentId = documentId;
        this.content = content;
    }
}
