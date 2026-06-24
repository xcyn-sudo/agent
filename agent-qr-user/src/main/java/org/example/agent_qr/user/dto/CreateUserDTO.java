package org.example.agent_qr.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建用户请求 DTO。
 * <p>
 * 用于接收管理员创建新用户时的请求数据，
 * 包含必填的用户名和密码以及可选的个人信息字段。
 * </p>
 *
 * @author agent-qr
 */
@Data
public class CreateUserDTO {

    /**
     * 用户名（必填）。
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码（必填，存储时会使用 BCrypt 加密）。
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

    /**
     * 角色（可选），默认值为 "user"。
     */
    private String role;

    // ==================== P2 ABAC 字段 ====================

    /**
     * 所属部门（可选）。
     */
    private String department;

    /**
     * 数据密级（可选）：0=公开 / 1=内部 / 2=机密 / 3=绝密。
     */
    private Integer clearanceLevel;

    /**
     * 允许访问的业务域（可选，逗号分隔）。
     */
    private String allowedDomains;

    /**
     * 职级（可选）：employee / manager / director。
     */
    private String title;
}
