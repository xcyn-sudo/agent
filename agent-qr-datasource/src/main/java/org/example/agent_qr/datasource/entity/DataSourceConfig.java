package org.example.agent_qr.datasource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据源配置实体，对应数据库表 data_source_config。
 * <p>
 * 持久化外部数据源的连接信息、同步策略和游标状态。
 * </p>
 *
 * @author agent-qr
 */
@Data
@TableName("data_source_config")
public class DataSourceConfig {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 数据源名称 */
    private String sourceName;

    /** 数据源类型：JDBC / REST / S3 */
    private String sourceType;

    /** 所属业务域 */
    private String domain;

    /** 同步策略：FULL / INCREMENTAL */
    private String syncStrategy;

    /** 增量同步的游标字段名 */
    private String cursorField;

    /** 上次同步的游标值 */
    private String lastCursor;

    /** 连接配置（JSON 格式） */
    private String connectionConfig;

    /** 字段映射配置（JSON 格式） */
    private String fieldMapping;

    /** 状态：ACTIVE / INACTIVE / ERROR */
    private String status;

    /** 累计同步总数 */
    private Integer totalSynced;

    /** 上次同步时间 */
    private LocalDateTime lastSyncAt;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 状态常量 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_ERROR = "ERROR";

    /** 同步策略常量 */
    public static final String SYNC_FULL = "FULL";
    public static final String SYNC_INCREMENTAL = "INCREMENTAL";
}
