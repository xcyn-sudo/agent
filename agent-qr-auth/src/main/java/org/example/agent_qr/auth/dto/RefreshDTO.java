package org.example.agent_qr.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Refresh Token 刷新请求 DTO。
 *
 * @author agent-qr
 */
@Data
public class RefreshDTO {

    /** Refresh Token 字符串（必填） */
    @NotBlank(message = "Refresh Token 不能为空")
    private String refreshToken;
}
