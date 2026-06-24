package org.example.agent_qr.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO，包含用户名和密码。
 *
 * @author agent-qr
 */
@Data
public class LoginDTO {

    /**
     * 用户名，不能为空。
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码，不能为空。
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
