package org.example.agent_qr.rag.filter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.agent_qr.rag.entity.ChunkStructured;
import org.example.agent_qr.rag.filter.FieldDefinition;

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
     * 查询指定域下的所有可用字段定义（去重）。
     * 用于 FilterConditionExtractor 构建 LLM Prompt 中的字段列表。
     */
    @Select("SELECT DISTINCT field_name AS fieldName, field_type AS fieldType " +
            "FROM kb_chunk_structured WHERE domain = #{domain} " +
            "ORDER BY field_name")
    List<FieldDefinition> selectDistinctFieldsByDomain(@Param("domain") String domain);

    /**
     * 查询指定域下某枚举字段的所有去重值。
     * 用于 FilterConditionExtractor 校验 LLM 提取的枚举值是否合法。
     */
    @Select("SELECT DISTINCT field_value FROM kb_chunk_structured " +
            "WHERE field_name = #{fieldName} AND domain = #{domain} " +
            "ORDER BY field_value LIMIT 50")
    List<String> selectEnumValues(@Param("fieldName") String fieldName,
                                  @Param("domain") String domain);

    /**
     * 按文档 domain 查询切片 ID（用于域过滤，文档上传管线）。
     * JOIN kb_chunk + kb_document，仅返回未删除的切片。
     */
    @Select("SELECT c.id FROM kb_chunk c INNER JOIN kb_document d ON c.document_id = d.id " +
            "WHERE d.domain = #{domain} AND c.deleted = 0 AND d.deleted = 0 ORDER BY c.id LIMIT 500")
    List<Long> selectChunkIdsByDocumentDomain(@Param("domain") String domain);

    // ========== FilterConditionExtractor 所需（字段定义查询） ==========

    /**
     * 查询域下所有可用的结构化字段定义（去重字段名和类型）。
     * 供 LLM 构造过滤条件提取 Prompt 使用。
     *
     * @param domain 业务域
     * @return 字段定义列表
     */
    @Select("SELECT DISTINCT field_name AS fieldName, field_type AS fieldType " +
            "FROM kb_chunk_structured WHERE domain = #{domain} ORDER BY field_name")
    List<FieldDefinition> selectDistinctFieldsByDomain(@Param("domain") String domain);

    /**
     * 查询枚举字段的所有可选值（用于 LLM Prompt 和校验）。
     *
     * @param fieldName 字段名
     * @param domain    业务域
     * @return 枚举值列表（最多 100 条）
     */
    @Select("SELECT DISTINCT field_value FROM kb_chunk_structured " +
            "WHERE field_name = #{fieldName} AND domain = #{domain} " +
            "AND field_type = 'ENUM' ORDER BY field_value LIMIT 100")
    List<String> selectEnumValues(@Param("fieldName") String fieldName,
                                  @Param("domain") String domain);

    // ========== 聚合查询路径专用（无界查询，安全上限 2000） ==========

    /**
     * 按字符串值精确匹配全部切片 ID（聚合查询专用，无 LIMIT 500 硬截断）。
     */
    @Select("SELECT DISTINCT chunk_id FROM kb_chunk_structured " +
            "WHERE field_name = #{fieldName} AND field_value = #{value} ORDER BY chunk_id LIMIT 2000")
    List<Long> selectAllChunkIdsByStringValue(@Param("fieldName") String fieldName,
                                             @Param("value") String value);

    /**
     * 按数值范围过滤全部切片 ID（聚合查询专用）。
     */
    @Select("SELECT DISTINCT chunk_id FROM kb_chunk_structured " +
            "WHERE field_name = #{fieldName} AND numeric_value >= #{min} AND numeric_value <= #{max} " +
            "ORDER BY chunk_id LIMIT 2000")
    List<Long> selectAllChunkIdsByNumberRange(@Param("fieldName") String fieldName,
                                             @Param("min") BigDecimal min,
                                             @Param("max") BigDecimal max);

    /**
     * 按日期范围过滤全部切片 ID（聚合查询专用）。
     */
    @Select("SELECT DISTINCT chunk_id FROM kb_chunk_structured " +
            "WHERE field_name = #{fieldName} AND date_value >= #{start} AND date_value <= #{end} " +
            "ORDER BY chunk_id LIMIT 2000")
    List<Long> selectAllChunkIdsByDateRange(@Param("fieldName") String fieldName,
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end);

    /**
     * 按业务域查询全部切片 ID（聚合查询专用，数据同步管线）。
     */
    @Select("SELECT DISTINCT chunk_id FROM kb_chunk_structured " +
            "WHERE domain = #{domain} ORDER BY chunk_id LIMIT 2000")
    List<Long> selectChunkIdsByDomainUnbounded(@Param("domain") String domain);

    /**
     * 按文档 domain 查询全部切片 ID（聚合查询专用，文档上传管线）。
     */
    @Select("SELECT c.id FROM kb_chunk c INNER JOIN kb_document d ON c.document_id = d.id " +
            "WHERE d.domain = #{domain} AND c.deleted = 0 AND d.deleted = 0 ORDER BY c.id LIMIT 2000")
    List<Long> selectChunkIdsByDocumentDomainUnbounded(@Param("domain") String domain);
}
