package org.example.agent_qr.datasource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.agent_qr.datasource.entity.DataSourceConfig;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据源配置 Mapper，提供数据源配置的持久化操作。
 *
 * @author agent-qr
 */
@Mapper
public interface DataSourceMapper extends BaseMapper<DataSourceConfig> {

    /**
     * 更新同步结果（游标、总数、最后同步时间）。
     *
     * @param id         数据源 ID
     * @param lastCursor 更新后的游标
     * @param totalRows  本次同步行数
     * @param lastSyncAt 同步时间
     * @return 影响行数
     */
    @Update("UPDATE data_source_config SET last_cursor = #{lastCursor}, " +
            "total_synced = IFNULL(total_synced, 0) + #{totalRows}, " +
            "last_sync_at = #{lastSyncAt} WHERE id = #{id}")
    int updateSyncResult(@Param("id") Long id,
                         @Param("lastCursor") String lastCursor,
                         @Param("totalRows") int totalRows,
                         @Param("lastSyncAt") LocalDateTime lastSyncAt);

    /**
     * 更新数据源状态。
     *
     * @param id     数据源 ID
     * @param status 新状态
     * @return 影响行数
     */
    @Update("UPDATE data_source_config SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 累加质量检测通过数（即使 blocked 时也调用，传入 0 以初始化字段，避免 NULL 回退泄露全量数据）。
     *
     * @param id          数据源 ID
     * @param passedCount 本次通过数（blocked 时传 0）
     * @return 影响行数
     */
    @Update("UPDATE data_source_config SET total_passed = IFNULL(total_passed, 0) + #{passedCount} WHERE id = #{id}")
    int updateTotalPassed(@Param("id") Long id, @Param("passedCount") int passedCount);

    /**
     * 查询所有活跃的数据源。
     *
     * @return 活跃数据源列表
     */
    @Select("SELECT * FROM data_source_config WHERE status = 'ACTIVE'")
    List<DataSourceConfig> selectAllActive();
}
