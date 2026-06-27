package org.example.agent_qr.datasource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.agent_qr.datasource.entity.SyncRecord;

import java.util.List;

/**
 * 同步历史记录 Mapper，提供同步记录的基础 CRUD 及自定义 SQL 操作。
 *
 * @author agent-qr
 */
@Mapper
public interface SyncRecordMapper extends BaseMapper<SyncRecord> {

    /**
     * 按数据源 ID 分页查询同步历史，按同步时间倒序。
     *
     * @param datasourceId 数据源 ID
     * @param offset       偏移量
     * @param limit        每页条数
     * @return 同步记录列表
     */
    @Select("SELECT * FROM sync_record WHERE datasource_id = #{datasourceId} " +
            "ORDER BY sync_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<SyncRecord> selectByDatasourceIdPaged(@Param("datasourceId") Long datasourceId,
                                                @Param("offset") int offset,
                                                @Param("limit") int limit);

    /**
     * 按数据源 ID 统计同步记录总数。
     *
     * @param datasourceId 数据源 ID
     * @return 记录总数
     */
    @Select("SELECT COUNT(*) FROM sync_record WHERE datasource_id = #{datasourceId}")
    long countByDatasourceId(@Param("datasourceId") Long datasourceId);

    /**
     * 更新同步记录状态。
     *
     * @param id       记录 ID
     * @param status   新状态
     * @param errorMsg 错误信息（可选）
     * @return 影响行数
     */
    @Update("UPDATE sync_record SET status = #{status}, error_msg = #{errorMsg} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("errorMsg") String errorMsg);
}
