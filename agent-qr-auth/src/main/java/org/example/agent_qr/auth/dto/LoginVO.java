package org.example.agent_qr.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 VO。
 * <p>
 * P1 原有：包含单 Token 和用户基本信息。
 * P2 扩展：支持双 Token 机制（accessToken + refreshToken + expiresIn）
 *           同步返回 ABAC 属性（department/clearanceLevel/allowedDomains/title）。
 * </p>
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /** Access Token（访问令牌，含 ABAC 属性，有效期 30 分钟） */
    private String accessToken;

    /** Refresh Token（刷新令牌，仅用于刷新，有效期 7 天） */
    private String refreshToken;

    /** Access Token 有效期（秒） */
    private Long expiresIn;

    // ─────────────────── 用户基本信息 ───────────────────

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 用户角色（admin 或 user） */
    private String role;

    // ─────────────────── P2 ABAC 扩展属性 ───────────────────

    /** ★ 所属部门（HR / FINANCE / RD / SALES / COMMON） */
    private String department;

    /** ★ 数据密级（0=公开 / 1=内部 / 2=机密 / 3=绝密） */
    private Integer clearanceLevel;

    /** ★ 允许访问的业务域（逗号分隔字符串，前端解析为数组） */
    private String allowedDomains;

    /** ★ 职级（employee / manager / director） */
    private String title;
}
