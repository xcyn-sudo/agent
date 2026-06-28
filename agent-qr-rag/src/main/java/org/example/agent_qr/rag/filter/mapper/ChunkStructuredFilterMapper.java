package org.example.agent_qr.rag.filter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.agent_qr.rag.entity.ChunkStructured;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 切片结构化字段 Mapper，提供基于结构化字段的过滤查询。
 * <p>
 * 所有 SQL 均使用 LIMIT 500 防止结果集过大，
 * 通过 MySQL B+ 树索引实现高效前置过滤。
 * </p>
 *
 * @author agent-qr
 */
@Mapper
public interface ChunkStructuredFilterMapper extends BaseMapper<ChunkStructured> {

    /**
     * 按数值范围过滤切片 ID。
     */
    @Select("SELECT DISTINCT chunk_id FROM kb_chunk_structured " +
            "WHERE field_name = #{fieldName} AND numeric_value >= #{min} AND numeric_value <= #{max} " +
            "ORDER BY chunk_id LIMIT 500")
    List<Long> selectChunkIdsByNumberRange(@Param("fieldName") String fieldName,
                                           @Param("min") BigDecimal min,
                                           @Param("max") BigDecimal max);

    /**
     * 按日期范围过滤切片 ID。
     */
    @Select("SELECT DISTINCT chunk_id FROM kb_chunk_structured " +
            "WHERE field_name = #{fieldName} AND date_value >= #{start} AND date_value <= #{end} " +
            "ORDER BY chunk_id LIMIT 500")
    List<Long> selectChunkIdsByDateRange(@Param("fieldName") String fieldName,
                                         @Param("start") LocalDate start,
                                         @Param("end") LocalDate end);

    /**
     * 按字符串值精确匹配切片 ID。
     */
    @Select("SELECT DISTINCT chunk_id FROM kb_chunk_structured " +
            "WHERE field_name = #{fieldName} AND field_value = #{value} ORDER BY chunk_id LIMIT 500")
    List<Long> selectChunkIdsByStringValue(@Param("fieldName") String fieldName,
                                           @Param("value") String value);

    /**
     * 按业务域查询切片 ID（用于域过滤，数据同步管线）。
     */
    @Select("SELECT DISTINCT chunk_id FROM kb_chunk_structured WHERE domain = #{domain} ORDER BY chunk_id LIMIT 500")
    List<Long> selectChunkIdsByDomain(@Param("domain") String domain);

    /**
     * 按文档 domain 查询切片 ID（用于域过滤，文档上传管线）。
     * JOIN kb_chunk + kb_document，仅返回未删除的切片。
     */
    @Select("SELECT c.id FROM kb_chunk c INNER JOIN kb_document d ON c.document_id = d.id " +
            "WHERE d.domain = #{domain} AND c.deleted = 0 AND d.deleted = 0 ORDER BY c.id LIMIT 500")
    List<Long> selectChunkIdsByDocumentDomain(@Param("domain") String domain);
}
