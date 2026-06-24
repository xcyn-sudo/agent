package org.example.agent_qr.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 VO。
 * <p>
 * P1 原有：包含单 Token 和用户基本信息。
 * P2 扩展：支持双 Token 机制（accessToken + refreshToken + expiresIn）。
 * </p>
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /** Access Token（访问令牌，有效期 30 分钟） */
    private String accessToken;

    /** Refresh Token（刷新令牌，有效期 7 天） */
    private String refreshToken;

    /** Access Token 有效期（秒） */
    private Long expiresIn;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 用户角色（admin 或 user） */
    private String role;
}
