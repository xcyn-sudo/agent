package org.example.agent_qr.dataquality.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.agent_qr.dataquality.entity.QualityReport;

/**
 * 质检报告 MyBatis-Plus Mapper。
 * <p>
 * 对应 quality_report 表，failures 字段由 JacksonTypeHandler 自动处理 JSON 序列化。
 * </p>
 *
 * @author agent-qr
 */
@Mapper
public interface QualityReportMapper extends BaseMapper<QualityReport> {

    /**
     * 根据批次 ID 查询质检报告。
     *
     * @param batchId 同步批次 ID
     * @return 质检报告，未找到返回 null
     */
    @Select("SELECT * FROM quality_report WHERE batch_id = #{batchId}")
    QualityReport selectByBatchId(@Param("batchId") String batchId);
}
