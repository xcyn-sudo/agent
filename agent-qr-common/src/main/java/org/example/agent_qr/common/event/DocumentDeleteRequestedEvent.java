package org.example.agent_qr.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文档删除请求事件。
 * <p>
 * 由 knowledge 模块发布，compensation 模块通过
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} 监听处理。
 * 实现软删除 + ChromaDB 物理删除的解耦。
 * </p>
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDeleteRequestedEvent {

    /** 文档 ID */
    private Long documentId;

    /** 关联的切片 ID 列表 */
    private List<Long> chunkIds;

    /** ChromaDB 中的向量 ID 列表 */
    private List<String> chromaIds;

    /** 文件存储路径 */
    private String filePath;
}
