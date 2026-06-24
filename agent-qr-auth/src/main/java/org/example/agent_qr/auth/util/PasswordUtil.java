package org.example.agent_qr.auth.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码工具类，基于 BCrypt 实现密码加密和验证。
 * <p>
 * 使用强度为 12 的 BCryptPasswordEncoder，在安全性和性能之间取得平衡。
 * </p>
 *
 * @author agent-qr
 */
@Component
public class PasswordUtil {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    /**
     * 对原始密码进行 BCrypt 加密。
     *
     * @param rawPassword 原始密码
     * @return 加密后的密文
     */
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 验证原始密码是否与加密密文匹配。
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密文
     * @return 匹配返回 true，否则返回 false
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
