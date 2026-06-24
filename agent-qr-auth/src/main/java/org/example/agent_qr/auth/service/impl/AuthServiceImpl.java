package org.example.agent_qr.auth.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.auth.dto.LoginDTO;
import org.example.agent_qr.auth.dto.LoginVO;
import org.example.agent_qr.auth.dto.RegisterDTO;
import org.example.agent_qr.auth.dto.TokenPair;
import org.example.agent_qr.auth.principal.UserPrincipal;
import org.example.agent_qr.auth.service.AuthService;
import org.example.agent_qr.auth.service.RefreshTokenService;
import org.example.agent_qr.auth.util.JwtUtil;
import org.example.agent_qr.auth.util.PasswordUtil;
import org.example.agent_qr.common.BusinessException;
import org.example.agent_qr.user.entity.SysUser;
import org.example.agent_qr.user.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现类，处理登录、注册和获取当前用户的核心业务逻辑。
 * <p>
 * P1 原有：单 Token 登录/注册。
 * P2 扩展：双 Token 机制（Access + Refresh）。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Override
    public LoginVO login(LoginDTO dto) {
        SysUser user = sysUserMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        if (!passwordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // P2: 签发双 Token
        TokenPair tokenPair = refreshTokenService.issueTokens(user);
        log.info("用户 {} 登录成功（双Token）", user.getUsername());

        return new LoginVO(
                tokenPair.getAccessToken(),
                tokenPair.getRefreshToken(),
                tokenPair.getExpiresIn(),
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }

    @Override
    public void register(RegisterDTO dto) {
        SysUser existingUser = sysUserMapper.selectByUsername(dto.getUsername());
        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordUtil.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRole("user");
        user.setStatus(1);

        sysUserMapper.insert(user);
        log.info("新用户 {} 注册成功", user.getUsername());
    }

    @Override
    public SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        // P2: 支持 UserPrincipal 类型
        Object principal = authentication.getPrincipal();
        if (principal instanceof SysUser sysUser) {
            return sysUser;
        }
        if (principal instanceof UserPrincipal userPrincipal) {
            SysUser user = new SysUser();
            user.setId(userPrincipal.getUserId());
            user.setUsername(userPrincipal.getUsername());
            user.setRole(userPrincipal.getRole());
            return user;
        }
        return null;
    }

    @Override
    public TokenPair issueTokens(SysUser user) {
        return refreshTokenService.issueTokens(user);
    }

    @Override
    public TokenPair refreshToken(String refreshToken) {
        return refreshTokenService.refresh(refreshToken);
    }
}
