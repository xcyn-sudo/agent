package org.example.agent_qr.datasource.connector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Iterator;
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
 *   <li>dataPath - 数据数组所在路径，如 "stories"、"data.items"（选填，不填则自动检测）</li>
 * </ul>
 * </p>
 * <p>
 * 响应解析策略（按优先级）：
 * <ol>
 *   <li>若指定 dataPath，按点号分隔导航 JSON（如 data.items → obj.data.items）</li>
 *   <li>若响应为 JSON 数组，直接解析</li>
 *   <li>若响应为 JSON 对象，自动查找第一个数组字段</li>
 * </ol>
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class RestApiConnector implements DataSourceConnector {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "REST";
    }

    @Override
    public List<String> detectColumns(Map<String, Object> config, String tableName) {
        String baseUrl = (String) config.get("baseUrl");
        String endpoint = (String) config.getOrDefault("endpoint", "/");
        String methodStr = (String) config.getOrDefault("method", "GET");
        String dataPath = (String) config.get("dataPath");
        HttpMethod method = parseHttpMethod(methodStr);
        String url = baseUrl + endpoint;

        try {
            HttpEntity<Void> entity = buildEntity(config);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, method, entity, String.class);
            List<Map<String, Object>> rows = extractList(response.getBody(), dataPath);
            if (!rows.isEmpty()) {
                return new ArrayList<>(rows.get(0).keySet());
            }
            log.warn("REST 字段检测: 响应中无数据行, url={}", url);
        } catch (Exception e) {
            log.error("REST 字段检测失败: url={}, error={}", url, e.getMessage());
        }
        return List.of();
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
    public SyncResult fullSync(SyncContext context) {
        Map<String, Object> config = context.getConfig();
        String baseUrl = (String) config.get("baseUrl");
        String endpoint = (String) config.getOrDefault("endpoint", "/");
        String methodStr = (String) config.getOrDefault("method", "GET");
        String pagination = (String) config.get("pagination");
        String dataPath = (String) config.get("dataPath");
        HttpMethod method = parseHttpMethod(methodStr);

        List<Map<String, Object>> allRows = new ArrayList<>();
        String cursor = null;
        int page = 0;
        int maxPages = 100;

        try {
            while (page < maxPages) {
                String url = buildUrl(baseUrl, endpoint, pagination, page, cursor);

                ResponseEntity<String> response = restTemplate.exchange(
                        url, method, buildEntity(config), String.class);

                List<Map<String, Object>> pageRows = extractList(response.getBody(), dataPath);
                allRows.addAll(pageRows);

                // X-Next-Cursor 游标翻页
                List<String> cursorHeaders = response.getHeaders().get("X-Next-Cursor");
                if (cursorHeaders != null && !cursorHeaders.isEmpty()) {
                    cursor = cursorHeaders.get(0);
                    if (cursor == null || cursor.isEmpty()) break;
                }

                if (pageRows.isEmpty()) {
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
    public SyncResult incrementalSync(SyncContext context, String lastCursor) {
        Map<String, Object> config = context.getConfig();
        String baseUrl = (String) config.get("baseUrl");
        String endpoint = (String) config.getOrDefault("endpoint", "/");
        String methodStr = (String) config.getOrDefault("method", "GET");
        String cursorParam = (String) config.getOrDefault("cursorParam", "since");
        String dataPath = (String) config.get("dataPath");
        HttpMethod method = parseHttpMethod(methodStr);

        List<Map<String, Object>> allRows = new ArrayList<>();
        String newCursor = lastCursor;

        try {
            String url = baseUrl + endpoint + "?" + cursorParam + "="
                    + (lastCursor != null ? lastCursor : "");
            ResponseEntity<String> response = restTemplate.exchange(
                    url, method, buildEntity(config), String.class);
            allRows = extractList(response.getBody(), dataPath);
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

    /**
     * 从 JSON 响应中提取数据列表，同时支持 JSON 数组和 JSON 对象。
     * <p>
     * 解析优先级：
     * <ol>
     *   <li>若指定 dataPath，按点号分隔逐级导航（如 "data.items" → root.data.items）</li>
     *   <li>若响应为 JSON 数组，直接解析</li>
     *   <li>若响应为 JSON 对象，自动查找第一个值为数组的字段</li>
     * </ol>
     *
     * @param json     响应体字符串
     * @param dataPath 数据路径（可选），点号分隔，如 "stories"、"data.items"
     * @return 解析后的数据行列表，解析失败返回空列表
     */
    private List<Map<String, Object>> extractList(String json, String dataPath) {
        if (json == null || json.isBlank()) return List.of();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode target = null;

            // 1. 优先使用 dataPath 导航
            if (dataPath != null && !dataPath.isBlank()) {
                target = root;
                for (String key : dataPath.split("\\.")) {
                    if (target == null) break;
                    target = target.get(key.trim());
                }
                if (target != null && target.isArray()) {
                    log.debug("通过 dataPath={} 定位到数组", dataPath);
                } else {
                    log.warn("dataPath={} 未找到数组，target={}", dataPath, target);
                }
            }

            // 2. 响应本身就是 JSON 数组
            if (target == null && root.isArray()) {
                target = root;
                log.debug("响应为 JSON 数组");
            }

            // 3. 响应是对象：自动查找第一个数组字段
            if (target == null && root.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    if (field.getValue().isArray()) {
                        target = field.getValue();
                        log.info("自动检测到数组字段: {}", field.getKey());
                        break;
                    }
                }
            }

            if (target != null && target.isArray()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> result = objectMapper.convertValue(
                        target, List.class);
                return result;
            }
        } catch (Exception e) {
            log.error("JSON 解析失败: {}", e.getMessage(), e);
        }
        return List.of();
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
