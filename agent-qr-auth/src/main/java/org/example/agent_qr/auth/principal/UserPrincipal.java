package org.example.agent_qr.auth.principal;

import io.jsonwebtoken.Claims;
import lombok.Data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 用户安全主体，封装从 JWT 解析出的 ABAC 属性信息。
 * <p>
 * 替代 P1 中直接使用 SysUser 作为 SecurityContext 主体的方式，
 * 支持部门、密级、业务域、职级等细粒度属性访问控制。
 * </p>
 *
 * @author agent-qr
 */
@Data
public class UserPrincipal {

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 角色：admin 或 user */
    private String role;

    /** 所属部门 */
    private String department;

    /** 数据密级：0=公开/1=内部/2=机密/3=绝密 */
    private Integer clearanceLevel;

    /** 允许访问的业务域列表 */
    private List<String> allowedDomains;

    /** 职级：employee/manager/director */
    private String title;

    /**
     * 从 JWT Claims 构建 UserPrincipal。
     *
     * @param claims JWT 声明
     * @return UserPrincipal 实例
     */
    public static UserPrincipal fromClaims(Claims claims) {
        UserPrincipal principal = new UserPrincipal();
        principal.setUserId(claims.get("userId", Long.class));
        principal.setUsername(claims.getSubject());
        principal.setRole(claims.get("role", String.class));
        principal.setDepartment(claims.get("department", String.class));
        principal.setClearanceLevel(claims.get("clearanceLevel", Integer.class));
        principal.setTitle(claims.get("title", String.class));

        String domainsStr = claims.get("allowedDomains", String.class);
        if (domainsStr != null && !domainsStr.isBlank()) {
            principal.setAllowedDomains(Arrays.asList(domainsStr.split(",")));
        } else {
            principal.setAllowedDomains(Collections.emptyList());
        }

        return principal;
    }

    /**
     * 判断是否为管理员。
     *
     * @return true 表示角色为 admin
     */
    public boolean isAdmin() {
        return "admin".equals(role);
    }

    /**
     * 判断是否有指定业务域的访问权限。
     * <p>
     * 允许条件：allowedDomains 包含该域，或部门等于该域。
     * </p>
     *
     * @param domain 业务域
     * @return true 表示有权限
     */
    public boolean hasDomainAccess(String domain) {
        if (domain == null) {
            return true;
        }
        return allowedDomains.contains(domain) || domain.equals(department);
    }

    /**
     * 判断是否有足够的数据密级访问资源。
     *
     * @param resourceLevel 资源密级
     * @return true 表示用户密级 >= 资源密级
     */
    public boolean hasClearance(int resourceLevel) {
        return clearanceLevel != null && clearanceLevel >= resourceLevel;
    }
}
