package org.example.agent_qr.auth.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.auth.principal.UserPrincipal;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ABAC 访问拒绝处理器。
 * <p>
 * 捕获 Spring Security {@link AccessDeniedException}，
 * 构建包含审计信息的结构化 JSON 响应并记录 warn 日志。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@RestControllerAdvice
public class AbacAccessDeniedHandler {

    /**
     * 处理访问拒绝异常。
     *
     * @param ex 访问拒绝异常
     * @return 403 结构化错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> auditLog = new LinkedHashMap<>();
        auditLog.put("timestamp", LocalDateTime.now().toString());
        auditLog.put("status", 403);
        auditLog.put("error", "Forbidden");
        auditLog.put("message", "访问被拒绝：" + ex.getMessage());
        auditLog.put("traceId", MDC.get("traceId"));

        // 提取当前用户信息
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            auditLog.put("userId", principal.getUserId());
            auditLog.put("username", principal.getUsername());
            auditLog.put("role", principal.getRole());
            auditLog.put("department", principal.getDepartment());
        }

        log.warn("ABAC 访问拒绝: {}", auditLog);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(auditLog);
    }
}
