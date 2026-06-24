package org.example.agent_qr.rag.entity;

import lombok.Data;

/**
 * 检索结果模型，用于封装从向量数据库中检索到的文档信息。
 *
 * @author agent-qr
 */
@Data
public class RetrievedDocument {

    /**
     * 文档 ID（对应向量数据库中的 embeddingId）。
     */
    private String documentId;

    /**
     * 文档标题。
     */
    private String documentTitle;

    /**
     * 文档内容片段。
     */
    private String content;

    /**
     * 与查询向量的相似度分数（0.0 ~ 1.0）。
     */
    private Double similarity;
}
