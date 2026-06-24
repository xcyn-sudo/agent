package org.example.agent_qr.auth.service;

import org.example.agent_qr.auth.dto.LoginDTO;
import org.example.agent_qr.auth.dto.LoginVO;
import org.example.agent_qr.auth.dto.RegisterDTO;
import org.example.agent_qr.auth.dto.TokenPair;
import org.example.agent_qr.user.entity.SysUser;

/**
 * 认证服务接口，提供登录、注册和获取当前用户功能。
 * <p>
 * P2 扩展：登录返回双 Token（TokenPair）。
 * </p>
 *
 * @author agent-qr
 */
public interface AuthService {

    /**
     * 用户登录，验证用户名和密码后返回双 Token 及用户信息。
     *
     * @param dto 登录请求
     * @return 登录响应（包含双 Token 和用户信息）
     */
    LoginVO login(LoginDTO dto);

    /**
     * 用户注册，创建新用户并设置默认角色和状态。
     *
     * @param dto 注册请求
     */
    void register(RegisterDTO dto);

    /**
     * 获取当前登录用户信息。
     *
     * @return 当前用户实体
     */
    SysUser getCurrentUser();

    /**
     * 签发双 Token（P2 新增）。
     *
     * @param user 用户实体
     * @return TokenPair 双 Token
     */
    TokenPair issueTokens(SysUser user);

    /**
     * 刷新 Token（P2 新增）。
     *
     * @param refreshToken Refresh Token
     * @return 新的 TokenPair
     */
    TokenPair refreshToken(String refreshToken);
}
