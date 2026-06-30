package org.example.agent_qr.rag.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.agent_qr.rag.entity.KbChunkRef;

import java.util.List;

/**
 * kb_chunk 表只读映射器，提供聚合查询路径所需的批量切片内容查询。
 * <p>
 * 由于 agent-qr-rag 不依赖 agent-qr-knowledge（knowledge 依赖 rag），
 * 此处独立映射 kb_chunk 表，避免循环依赖。
 * 仅提供聚合查询路径所需的只读查询方法。
 * </p>
 *
 * @author agent-qr
 */
@Mapper
public interface KbChunkRefMapper {

    /**
     * 按 ID 列表批量查询切片内容（聚合查询专用）。
     * 使用 MyBatis 动态 SQL &lt;foreach&gt; 实现 IN 子句。
     *
     * @param ids 切片 ID 列表
     * @return 切片引用列表（仅含 id、content、title）
     */
    @Select("<script>" +
            "SELECT c.id, c.content, d.title " +
            "FROM kb_chunk c " +
            "LEFT JOIN kb_document d ON c.document_id = d.id " +
            "WHERE c.id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND c.deleted = 0 " +
            "ORDER BY c.id" +
            "</script>")
    List<KbChunkRef> selectByIds(@Param("ids") List<Long> ids);
}
