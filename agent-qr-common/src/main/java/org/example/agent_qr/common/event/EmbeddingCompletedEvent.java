package org.example.agent_qr.common.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * 向量化完成事件。
 */
@Getter
@Setter
public class EmbeddingCompletedEvent extends ApplicationEvent {

    private Long documentId;
    private Integer chunkCount;

    public EmbeddingCompletedEvent(Object source, Long documentId, Integer chunkCount) {
        super(source);
        this.documentId = documentId;
        this.chunkCount = chunkCount;
    }
}
