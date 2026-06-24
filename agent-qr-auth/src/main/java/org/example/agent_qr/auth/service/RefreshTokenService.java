package org.example.agent_qr.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.auth.dto.TokenPair;
import org.example.agent_qr.auth.entity.TokenRefresh;
import org.example.agent_qr.auth.mapper.TokenRefreshMapper;
import org.example.agent_qr.auth.util.JwtUtil;
import org.example.agent_qr.common.BusinessException;
import org.example.agent_qr.user.entity.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Refresh Token 服务，负责双 Token 的签发、刷新和撤销。
 * <p>
 * 签发双 Token 时 Refresh Token 写入数据库；
 * 刷新时执行令牌轮换（旧 Token 撤销 + 新 Token 签发）。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Service
public class RefreshTokenService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenRefreshMapper tokenRefreshMapper;

    /**
     * 签发双 Token（Access + Refresh）。
     *
     * @param user 用户实体
     * @return TokenPair 包含双 Token
     */
    @Transactional
    public TokenPair issueTokens(SysUser user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // Refresh Token 持久化
        TokenRefresh tokenRefresh = new TokenRefresh();
        tokenRefresh.setUserId(user.getId());
        tokenRefresh.setToken(refreshToken);
        tokenRefresh.setRevoked(false);
        tokenRefresh.setCreateTime(LocalDateTime.now());
        tokenRefresh.setExpireTime(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpiration()));
        tokenRefreshMapper.insert(tokenRefresh);

        log.info("双 Token 已签发: userId={}, username={}", user.getId(), user.getUsername());
        return new TokenPair(accessToken, refreshToken, jwtUtil.getAccessExpiration());
    }

    /**
     * 刷新 Token（令牌轮换）。
     * <p>
     * 验证 Refresh Token 有效性 → 查 DB 未撤销 →
     * 删除旧 Refresh Token（轮换）→ 签发新令牌对。
     * </p>
     *
     * @param refreshToken 当前的 Refresh Token
     * @return 新的 TokenPair
     */
    @Transactional
    public TokenPair refresh(String refreshToken) {
        // 1. 验证 JWT 有效性
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(401, "Refresh Token 无效或已过期");
        }

        // 2. 查 DB 确认未撤销
        TokenRefresh stored = tokenRefreshMapper.selectByToken(refreshToken);
        if (stored == null) {
            throw new BusinessException(401, "Refresh Token 已被撤销或不存在");
        }

        // 3. 令牌轮换：撤销旧 Token
        stored.setRevoked(true);
        tokenRefreshMapper.updateById(stored);

        // 4. 签发新令牌对（需要用户信息）
        SysUser user = new SysUser();
        user.setId(stored.getUserId());
        // 从旧 JWT 解析用户名
        user.setUsername(jwtUtil.getUsernameFromToken(refreshToken));
        user.setRole(jwtUtil.getUsernameFromToken(refreshToken) != null ? "user" : "user");

        log.info("Refresh Token 轮换成功: userId={}", stored.getUserId());
        return issueTokens(user);
    }

    /**
     * 撤销用户的所有 Refresh Token。
     *
     * @param userId 用户 ID
     */
    @Transactional
    public void revoke(Long userId) {
        int count = tokenRefreshMapper.revokeByUserId(userId);
        log.info("已撤销用户 {} 的所有 Refresh Token，共 {} 条", userId, count);
    }
}
