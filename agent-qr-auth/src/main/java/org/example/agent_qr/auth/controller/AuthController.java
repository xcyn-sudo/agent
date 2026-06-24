package org.example.agent_qr.auth.controller;

import jakarta.validation.Valid;
import org.example.agent_qr.auth.dto.LoginDTO;
import org.example.agent_qr.auth.dto.LoginVO;
import org.example.agent_qr.auth.dto.RefreshDTO;
import org.example.agent_qr.auth.dto.RegisterDTO;
import org.example.agent_qr.auth.dto.TokenPair;
import org.example.agent_qr.auth.service.AuthService;
import org.example.agent_qr.common.Result;
import org.example.agent_qr.user.entity.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器，提供登录、注册、Token 刷新和获取当前用户信息的 REST API。
 * <p>
 * P1 原有：登录/注册/获取用户信息。
 * P2 扩展：登录返回双 Token、新增 Refresh Token 端点。
 * </p>
 *
 * @author agent-qr
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户登录接口（P2 改造：返回双 Token）。
     *
     * @param dto 登录请求
     * @return 包含双 Token 和用户信息的响应
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO loginVO = authService.login(dto);
        return Result.success("登录成功", loginVO);
    }

    /**
     * 用户注册接口。
     *
     * @param dto 注册请求
     * @return 操作结果
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.success("注册成功");
    }

    /**
     * 刷新 Token 接口（P2 新增）。
     * <p>
     * 使用 Refresh Token 换取新的 Access Token 和 Refresh Token（令牌轮换）。
     * </p>
     *
     * @param dto 刷新请求（含 Refresh Token）
     * @return 新的双 Token
     */
    @PostMapping("/refresh")
    public Result<TokenPair> refresh(@Valid @RequestBody RefreshDTO dto) {
        TokenPair tokenPair = authService.refreshToken(dto.getRefreshToken());
        return Result.success("Token 刷新成功", tokenPair);
    }

    /**
     * 获取当前登录用户信息。
     *
     * @return 当前用户实体
     */
    @GetMapping("/info")
    public Result<SysUser> getUserInfo() {
        SysUser user = authService.getCurrentUser();
        return Result.success(user);
    }
}
