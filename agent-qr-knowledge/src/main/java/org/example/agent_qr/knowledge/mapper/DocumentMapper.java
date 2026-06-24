package org.example.agent_qr.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.agent_qr.knowledge.entity.Document;

import java.util.List;
import java.util.Map;

/**
 * 文档 Mapper 接口，提供文档表的基础 CRUD 及自定义 SQL 操作。
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，自动获得通用 CRUD 能力。
 * </p>
 *
 * @author agent-qr
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    /**
     * 更新文档的处理状态。
     *
     * @param id     文档 ID
     * @param status 新的状态值
     * @return 受影响的行数
     */
    @Update("UPDATE kb_document SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 更新文档的错误信息。
     *
     * @param id       文档 ID
     * @param errorMsg 错误信息
     * @return 受影响的行数
     */
    @Update("UPDATE kb_document SET error_msg = #{errorMsg} WHERE id = #{id}")
    int updateErrorMsg(@Param("id") Long id, @Param("errorMsg") String errorMsg);

    /**
     * 统计各文件类型的文档数量分布。
     *
     * @return 文件类型分布列表，每项包含 file_type 和 cnt
     */
    @Select("SELECT file_type, COUNT(*) as cnt FROM kb_document GROUP BY file_type")
    List<Map<String, Object>> selectTypeDistribution();

    /**
     * 软删除文档（P2 新增）。
     */
    @Update("UPDATE kb_document SET deleted = 1 WHERE id = #{documentId}")
    int softDelete(@Param("documentId") Long documentId);
}
