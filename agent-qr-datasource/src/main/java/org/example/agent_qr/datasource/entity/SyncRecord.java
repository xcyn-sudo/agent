package org.example.agent_qr.datasource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据源同步历史记录实体，对应数据库表 sync_record。
 * <p>
 * 每次触发数据源同步后记录一条历史，用于追踪同步状态和问题排查。
 * </p>
 *
 * @author agent-qr
 */
@Data
@TableName("sync_record")
public class SyncRecord {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 数据源配置 ID */
    private Long datasourceId;

    /** 同步策略：FULL / INCREMENTAL */
    private String syncStrategy;

    /** 本次同步行数 */
    private Integer totalRows;

    /** 增量同步游标（下次同步起点） */
    private String nextCursor;

    /** 同步状态：SUCCESS / FAILED / PARTIAL */
    private String status;

    /** 失败错误信息 */
    private String errorMsg;

    /** 同步完成时间 */
    private LocalDateTime syncTime;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 状态常量 */
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PARTIAL = "PARTIAL";
}
