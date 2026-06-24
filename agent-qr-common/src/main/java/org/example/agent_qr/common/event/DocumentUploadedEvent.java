package org.example.agent_qr.common.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * 文档上传完成事件。
 */
@Getter
@Setter
public class DocumentUploadedEvent extends ApplicationEvent {

    private Long documentId;
    private String filePath;
    private String fileName;
    private String fileType;
    private Long userId;

    public DocumentUploadedEvent(Object source, Long documentId, String filePath,
                                  String fileName, String fileType, Long userId) {
        super(source);
        this.documentId = documentId;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileType = fileType;
        this.userId = userId;
    }
}
