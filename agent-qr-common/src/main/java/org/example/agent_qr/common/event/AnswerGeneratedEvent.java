package org.example.agent_qr.common.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * 问答回答生成完成事件。
 */
@Getter
@Setter
public class AnswerGeneratedEvent extends ApplicationEvent {

    private Long userId;
    private Long conversationId;

    public AnswerGeneratedEvent(Object source, Long userId, Long conversationId) {
        super(source);
        this.userId = userId;
        this.conversationId = conversationId;
    }
}
