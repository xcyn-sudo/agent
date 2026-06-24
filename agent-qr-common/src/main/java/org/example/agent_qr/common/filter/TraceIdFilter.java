package org.example.agent_qr.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 全链路追踪过滤器，为每个请求生成或提取 TraceId 并写入 MDC 和响应头。
 * <p>
 * 优先从请求头 X-Trace-Id 提取，若无则生成 16 位 UUID 短码。
 * 请求结束后清理 MDC，防止内存泄漏。
 * </p>
 *
 * @author agent-qr
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateShortUuid();
        }

        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    /**
     * 生成 16 位短 UUID（取 UUID 前 16 位十六进制字符）。
     *
     * @return 16 位短标识符
     */
    private String generateShortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
