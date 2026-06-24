package org.example.agent_qr.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.agent_qr.auth.principal.UserPrincipal;
import org.example.agent_qr.auth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器，在每次请求时从 Authorization 头中提取并验证 JWT Token。
 * <p>
 * P1 原有：使用 SysUserMapper 查询用户作为主体。
 * P2 改造：使用 JWT Claims 直接构建 UserPrincipal 替代 SysUser 查询，
 * 并将 allowedDomains 编码为 SimpleGrantedAuthority。
 * </p>
 *
 * @author agent-qr
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token == null || !jwtUtil.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // P2: 从 JWT 直接解析 UserPrincipal，不再查询 SysUser 表
        UserPrincipal principal = jwtUtil.parseUserPrincipal(token);

        if (principal == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 构建权限列表：角色权限 + 域权限
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + principal.getRole().toUpperCase()));

        // 将 allowedDomains 编码为 DOMAIN_xxx 格式的权限
        if (principal.getAllowedDomains() != null) {
            for (String domain : principal.getAllowedDomains()) {
                if (domain != null && !domain.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("DOMAIN_" + domain));
                }
            }
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求的 Authorization 头中提取 Bearer Token。
     *
     * @param request HTTP 请求
     * @return Token 字符串，不存在或格式不正确时返回 null
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
