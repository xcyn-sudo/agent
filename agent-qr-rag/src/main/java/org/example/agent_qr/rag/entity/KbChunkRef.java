package org.example.agent_qr.rag.entity;

import lombok.Data;

/**
 * kb_chunk 表的轻量引用，用于聚合查询路径批量获取切片内容。
 * <p>
 * 由于 agent-qr-rag 模块不依赖 agent-qr-knowledge 模块（knowledge 依赖 rag），
 * 不能直接注入 knowledge 模块的 ChunkMapper。本实体 + KbChunkRefMapper 提供
 * kb_chunk 表的只读映射，仅包含聚合查询所需的必要字段。
 * </p>
 *
 * @author agent-qr
 */
@Data
public class KbChunkRef {

    /** 切片主键 ID */
    private Long id;

    /** 切片文本内容 */
    private String content;

    /** 切片标题或来源标识 */
    private String title;
}
