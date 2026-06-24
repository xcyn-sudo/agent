package org.example.agent_qr.compensation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.agent_qr.compensation.entity.DeleteTask;

/**
 * 删除任务 Mapper。
 *
 * @author agent-qr
 */
@Mapper
public interface DeleteTaskMapper extends BaseMapper<DeleteTask> {

    /**
     * 更新任务状态。
     */
    @Update("UPDATE delete_task SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 递增重试次数。
     */
    @Update("UPDATE delete_task SET retry_count = retry_count + 1 WHERE id = #{id}")
    int incrementRetryCount(@Param("id") Long id);
}
