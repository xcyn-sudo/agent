package org.example.agent_qr.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.agent_qr.rag.entity.ChunkStructured;

import java.util.List;

/**
 * 切片结构化字段 Mapper，提供 kb_chunk_structured 表的 CRUD 操作。
 * <p>
 * 数据同步管线使用：将 ETL 标准化后的结构化元数据（数值、日期、枚举等）
 * 写入此表，支持检索时的 MySQL B+ 树前置过滤。
 * </p>
 *
 * @author agent-qr
 */
@Mapper
public interface ChunkStructuredMapper extends BaseMapper<ChunkStructured> {

    /**
     * 按切片 ID 查询所有结构化字段。
     *
     * @param chunkId 切片 ID
     * @return 结构化字段列表
     */
    @Select("SELECT * FROM kb_chunk_structured WHERE chunk_id = #{chunkId}")
    List<ChunkStructured> selectByChunkId(@Param("chunkId") Long chunkId);

    /**
     * 按切片 ID 删除所有结构化字段。
     *
     * @param chunkId 切片 ID
     * @return 受影响的行数
     */
    @org.apache.ibatis.annotations.Delete("DELETE FROM kb_chunk_structured WHERE chunk_id = #{chunkId}")
    int deleteByChunkId(@Param("chunkId") Long chunkId);
}
