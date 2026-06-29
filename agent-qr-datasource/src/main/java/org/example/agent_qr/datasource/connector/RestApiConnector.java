package org.example.agent_qr.datasource.connector;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.datasource.dto.ConnectionTestResult;
import org.example.agent_qr.datasource.dto.SyncContext;
import org.example.agent_qr.datasource.dto.SyncResult;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API 数据源连接器。
 * <p>
 * 通过 REST API 获取外部系统数据，支持分页循环拉取和
 * 响应头 X-Next-Cursor 驱动的游标翻页。
 * </p>
 * <p>
 * 配置项（通过 connectionConfig JSON 传入）：
 * <ul>
 *   <li>baseUrl - Base URL（必填）</li>
 *   <li>endpoint - 接口路径（选填，默认 /）</li>
 *   <li>method - 请求方式 GET/POST/HEAD（选填，默认 GET）</li>
 *   <li>authHeader - 认证头，格式 "Authorization: Bearer xxx"（选填）</li>
 *   <li>pagination - 分页参数模板，如 "page={page}&size={size}"（选填）</li>
 * </ul>
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class RestApiConnector implements DataSourceConnector {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getType() {
        return "REST";
    }

    @Override
    public ConnectionTestResult testConnection(Map<String, Object> config) {
        String baseUrl = (String) config.get("baseUrl");
        String endpoint = (String) config.getOrDefault("endpoint", "/");
        String methodStr = (String) config.getOrDefault("method", "GET");
        HttpMethod method = parseHttpMethod(methodStr);
        String url = baseUrl + endpoint;

        long start = System.currentTimeMillis();
        try {
            HttpEntity<Void> entity = buildEntity(config);
            ResponseEntity<Void> response = restTemplate.exchange(
                    url, method, entity, Void.class);
            long latency = System.currentTimeMillis() - start;
            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                return ConnectionTestResult.ok(latency, "REST API", null);
            }
            return ConnectionTestResult.fail("HTTP 状态码: " + response.getStatusCodeValue());
        } catch (Exception e) {
            log.error("REST API 连接测试失败: url={}, error={}", url, e.getMessage());
            return ConnectionTestResult.fail(e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public SyncResult fullSync(SyncContext context) {
        Map<String, Object> config = context.getConfig();
        String baseUrl = (String) config.get("baseUrl");
        String endpoint = (String) config.getOrDefault("endpoint", "/");
        String methodStr = (String) config.getOrDefault("method", "GET");
        String pagination = (String) config.get("pagination");
        HttpMethod method = parseHttpMethod(methodStr);

        List<Map<String, Object>> allRows = new ArrayList<>();
        String cursor = null;
        int page = 0;
        int maxPages = 100;

        try {
            while (page < maxPages) {
                String url = buildUrl(baseUrl, endpoint, pagination, page, cursor);

                ResponseEntity<List> response = restTemplate.exchange(
                        url, method, buildEntity(config), List.class);

                if (response.getBody() != null) {
                    for (Object item : response.getBody()) {
                        if (item instanceof Map) {
                            allRows.add(new LinkedHashMap<>((Map<String, Object>) item));
                        }
                    }
                }

                // X-Next-Cursor 游标翻页
                List<String> cursorHeaders = response.getHeaders().get("X-Next-Cursor");
                if (cursorHeaders != null && !cursorHeaders.isEmpty()) {
                    cursor = cursorHeaders.get(0);
                    if (cursor == null || cursor.isEmpty()) break;
                } else if (pagination != null && !pagination.isEmpty()) {
                    // 使用分页参数模板
                } else {
                    // 无分页：一次请求即完成
                }

                if (response.getBody() == null || response.getBody().isEmpty()) {
                    break;
                }
                // 无分页参数且无游标：单次请求
                if ((pagination == null || pagination.isEmpty()) && cursor == null) {
                    break;
                }
                page++;
            }
            log.info("REST API 全量同步: 读取 {} 行, 共 {} 页", allRows.size(), page);
        } catch (Exception e) {
            log.error("REST API 全量同步失败: {}", e.getMessage(), e);
        }

        return new SyncResult(allRows.size(), allRows, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public SyncResult incrementalSync(SyncContext context, String lastCursor) {
        Map<String, Object> config = context.getConfig();
        String baseUrl = (String) config.get("baseUrl");
        String endpoint = (String) config.getOrDefault("endpoint", "/");
        String methodStr = (String) config.getOrDefault("method", "GET");
        String cursorParam = (String) config.getOrDefault("cursorParam", "since");
        HttpMethod method = parseHttpMethod(methodStr);

        List<Map<String, Object>> allRows = new ArrayList<>();
        String newCursor = lastCursor;

        try {
            String url = baseUrl + endpoint + "?" + cursorParam + "="
                    + (lastCursor != null ? lastCursor : "");
            ResponseEntity<List> response = restTemplate.exchange(
                    url, method, buildEntity(config), List.class);
            if (response.getBody() != null) {
                for (Object item : response.getBody()) {
                    if (item instanceof Map) {
                        allRows.add(new LinkedHashMap<>((Map<String, Object>) item));
                    }
                }
            }
            List<String> cursorHeaders = response.getHeaders().get("X-Next-Cursor");
            if (cursorHeaders != null && !cursorHeaders.isEmpty()) {
                newCursor = cursorHeaders.get(0);
            }
            log.info("REST API 增量同步: 读取 {} 行, 新游标={}", allRows.size(), newCursor);
        } catch (Exception e) {
            log.error("REST API 增量同步失败: {}", e.getMessage(), e);
        }

        return new SyncResult(allRows.size(), allRows, newCursor);
    }

    // ── 工具方法 ──

    private HttpMethod parseHttpMethod(String method) {
        if (method == null) return HttpMethod.GET;
        return switch (method.toUpperCase()) {
            case "POST" -> HttpMethod.POST;
            case "HEAD" -> HttpMethod.HEAD;
            case "PUT" -> HttpMethod.PUT;
            case "DELETE" -> HttpMethod.DELETE;
            default -> HttpMethod.GET;
        };
    }

    private HttpEntity<Void> buildEntity(Map<String, Object> config) {
        HttpHeaders headers = new HttpHeaders();
        String authHeader = (String) config.get("authHeader");
        if (authHeader != null && !authHeader.isEmpty()) {
            headers.set("Authorization", authHeader.startsWith("Bearer ")
                    ? authHeader.substring(7) : authHeader);
            if (authHeader.contains(":")) {
                // 格式 "Key: Value" 或 "Authorization: Bearer xxx"
                String[] parts = authHeader.split(":", 2);
                headers.set(parts[0].trim(), parts[1].trim());
            }
        }
        return new HttpEntity<>(headers);
    }

    private String buildUrl(String baseUrl, String endpoint, String pagination,
                            int page, String cursor) {
        String url = baseUrl + endpoint;
        if (cursor != null) {
            url += (url.contains("?") ? "&" : "?") + "cursor=" + cursor;
        } else if (pagination != null && !pagination.isEmpty()) {
            String params = pagination
                    .replace("{page}", String.valueOf(page + 1))
                    .replace("{size}", "50");
            url += (url.contains("?") ? "&" : "?") + params;
        }
        return url;
    }
}
