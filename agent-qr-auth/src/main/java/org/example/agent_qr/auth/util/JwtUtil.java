package org.example.agent_qr.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.example.agent_qr.auth.principal.UserPrincipal;
import org.example.agent_qr.user.entity.SysUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 工具类，负责 Token 的生成、解析和验证。
 * <p>
 * P1 原有：单 Token 生成/解析/验证。
 * P2 扩展：双 Token 机制（Access 30min + Refresh 7day）和 UserPrincipal 解析。
 * </p>
 *
 * @author agent-qr
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    // ==================== P1 原有属性 ====================

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    // ==================== P2 新增属性 ====================

    /** Access Token 有效期（秒），默认 1800 = 30 分钟 */
    @Value("${jwt.access-expiration:1800}")
    private Long accessExpiration;

    /** Refresh Token 有效期（秒），默认 604800 = 7 天 */
    @Value("${jwt.refresh-expiration:604800}")
    private Long refreshExpiration;

    // ==================== P1 原有方法 ====================

    /**
     * 根据用户信息生成 JWT Token（P1 兼容）。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     用户角色
     * @return JWT Token 字符串
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                .compact();
    }

    /**
     * 从 Token 中提取用户名（subject）。
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * 验证 Token 是否有效。
     *
     * @param token JWT Token
     * @return 有效返回 true，否则返回 false
     */
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== P2 新增方法 ====================

    /**
     * 生成 Access Token（短期，含完整 ABAC 属性）。
     * <p>
     * Payload 包含：userId, role, department, clearanceLevel, allowedDomains, title。
     * </p>
     *
     * @param user 用户实体
     * @return Access Token 字符串
     */
    public String generateAccessToken(SysUser user) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + accessExpiration * 1000);

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .claim("department", user.getDepartment())
                .claim("clearanceLevel", user.getClearanceLevel())
                .claim("allowedDomains", user.getAllowedDomains())
                .claim("title", user.getTitle())
                .claim("tokenType", "access")
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                .compact();
    }

    /**
     * 生成 Refresh Token（长期，仅含 userId 和 tokenType）。
     * <p>
     * 有效期 7 天，用于获取新的 Access Token。
     * </p>
     *
     * @param user 用户实体
     * @return Refresh Token 字符串
     */
    public String generateRefreshToken(SysUser user) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + refreshExpiration * 1000);

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("tokenType", "refresh")
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                .compact();
    }

    /**
     * 从 Access Token 解析 UserPrincipal。
     *
     * @param token JWT Token
     * @return UserPrincipal 实例
     */
    public UserPrincipal parseUserPrincipal(String token) {
        Claims claims = getClaimsFromToken(token);
        return UserPrincipal.fromClaims(claims);
    }

    /**
     * 获取 Access Token 有效期（秒）。
     *
     * @return 有效期秒数
     */
    public Long getAccessExpiration() {
        return accessExpiration;
    }

    /**
     * 获取 Refresh Token 有效期（秒）。
     *
     * @return 有效期秒数
     */
    public Long getRefreshExpiration() {
        return refreshExpiration;
    }

    // ==================== 私有方法 ====================

    /**
     * 从 Token 中解析 Claims。
     *
     * @param token JWT Token
     * @return Token 中的 Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret.getBytes())
                .parseClaimsJws(token)
                .getBody();
    }
}
