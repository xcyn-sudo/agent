package org.example.agent_qr.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 双 Token 响应 DTO。
 * <p>
 * 包含 Access Token（短期）和 Refresh Token（长期），
 * 以及 Access Token 的有效期秒数。
 * </p>
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenPair {

    /** 访问令牌（有效期 30 分钟） */
    private String accessToken;

    /** 刷新令牌（有效期 7 天） */
    private String refreshToken;

    /** Access Token 有效期（秒），默认 1800 */
    private Long expiresIn;
}
