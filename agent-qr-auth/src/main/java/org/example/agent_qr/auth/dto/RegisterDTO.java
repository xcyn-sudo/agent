package org.example.agent_qr.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注册请求 DTO，包含用户名、密码及可选的个人信息字段。
 *
 * @author agent-qr
 */
@Data
public class RegisterDTO {

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

    /**
     * 真实姓名（可选）。
     */
    private String realName;

    /**
     * 电子邮箱（可选）。
     */
    private String email;

    /**
     * 电话号码（可选）。
     */
    private String phone;
}
