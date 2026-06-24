package org.example.agent_qr.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Refresh Token 实体，对应数据库表 token_refresh。
 * <p>
 * 用于双 Token 机制中的 Refresh Token 持久化，
 * 支持签发后撤销和过期管理。
 * </p>
 *
 * @author agent-qr
 */
@Data
@TableName("token_refresh")
public class TokenRefresh {

    /**
     * 主键 ID，自增。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的用户 ID。
     */
    private Long userId;

    /**
     * Refresh Token 字符串。
     */
    private String token;

    /**
     * 是否已撤销：false=有效，true=已撤销。
     */
    private Boolean revoked;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 过期时间。
     */
    private LocalDateTime expireTime;
}
