package org.example.agent_qr.knowledge.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.example.agent_qr.common.rag.IndexableText;

import java.time.LocalDateTime;

/**
 * 文档切片实体类，对应数据库表 kb_chunk。
 * <p>
 * 存储文档经文本切割后的片段及其向量化结果引用。
 * 每个切片是知识库检索的最小单元。
 * </p>
 *
 * @author agent-qr
 */
@Data
@TableName("kb_chunk")
public class Chunk implements IndexableText {

    /**
     * 主键 ID，自增。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属文档 ID。
     */
    private Long documentId;

    /**
     * 切片在文档中的序号，从 0 开始。
     */
    private Integer chunkIndex;

    /**
     * 切片文本内容。
     */
    private String content;

    /**
     * 切片字符数。
     */
    private Integer charCount;

    /**
     * ChromaDB 中的向量引用 ID。
     * P1 阶段暂不写入，设为 "pending"。
     */
    private String chromaId;

    /**
     * 创建时间，插入时自动填充。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ==================== P2 新增字段 ====================

    /**
     * 软删除标记：0=未删除 / 1=已删除。
     */
    private Integer deleted;
}
