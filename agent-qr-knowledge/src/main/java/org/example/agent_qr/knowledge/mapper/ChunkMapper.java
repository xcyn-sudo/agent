package org.example.agent_qr.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.agent_qr.knowledge.entity.Chunk;

import java.util.List;

/**
 * 切片 Mapper 接口，提供切片表的基础 CRUD 及自定义 SQL 操作。
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，自动获得通用 CRUD 能力。
 * </p>
 *
 * @author agent-qr
 */
@Mapper
public interface ChunkMapper extends BaseMapper<Chunk> {

    /**
     * 按文档 ID 删除该文档的所有切片。
     *
     * @param documentId 文档 ID
     * @return 受影响的行数
     */
    @Delete("DELETE FROM kb_chunk WHERE document_id = #{documentId}")
    int deleteByDocumentId(@Param("documentId") Long documentId);

    /**
     * 按文档 ID 查询该文档的所有切片，按索引升序排列。
     *
     * @param documentId 文档 ID
     * @return 切片列表
     */
    @Select("SELECT * FROM kb_chunk WHERE document_id = #{documentId} ORDER BY chunk_index")
    List<Chunk> selectByDocumentId(@Param("documentId") Long documentId);

    // ==================== P2 新增方法 ====================

    /**
     * 软删除指定文档的所有切片。
     */
    @Update("UPDATE kb_chunk SET deleted = 1 WHERE document_id = #{documentId}")
    int softDeleteByDocumentId(@Param("documentId") Long documentId);

    /**
     * 查询指定文档所有切片的 ChromaDB ID。
     */
    @Select("SELECT chroma_id FROM kb_chunk WHERE document_id = #{documentId}")
    List<String> selectChromaIdsByDocumentId(@Param("documentId") Long documentId);

    /**
     * 查询所有就绪切片（P2：排除已删除）。
     */
    @Select("SELECT * FROM kb_chunk WHERE status = 'READY' AND deleted = 0")
    List<Chunk> selectAllReadyChunks();

    /**
     * 分页查询就绪切片（P2：用于 BM25 索引构建）。
     */
    @Select("SELECT * FROM kb_chunk WHERE status = 'READY' AND deleted = 0 LIMIT #{limit} OFFSET #{offset}")
    List<Chunk> selectReadyChunksPaged(@Param("offset") int offset, @Param("limit") int limit);
}
