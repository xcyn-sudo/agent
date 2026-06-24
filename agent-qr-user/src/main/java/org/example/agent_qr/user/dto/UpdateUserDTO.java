package org.example.agent_qr.user.dto;

import lombok.Data;

/**
 * 更新用户请求 DTO。
 * <p>
 * 用于接收管理员更新用户信息时的请求数据，
 * 所有字段均为可选，仅更新非空字段。
 * </p>
 *
 * @author agent-qr
 */
@Data
public class UpdateUserDTO {

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
     * 角色（可选）。
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
