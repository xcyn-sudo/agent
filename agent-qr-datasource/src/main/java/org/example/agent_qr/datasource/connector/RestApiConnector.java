package org.example.agent_qr.datasource.connector;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.datasource.dto.ConnectionTestResult;
import org.example.agent_qr.datasource.dto.SyncContext;
import org.example.agent_qr.datasource.dto.SyncResult;
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
        long start = System.currentTimeMillis();
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    baseUrl, HttpMethod.HEAD, null, Void.class);
            long latency = System.currentTimeMillis() - start;
            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                return ConnectionTestResult.ok(latency, "REST API", null);
            }
            return ConnectionTestResult.fail("HTTP 状态码: " + response.getStatusCodeValue());
        } catch (Exception e) {
            log.error("REST API 连接测试失败: url={}, error={}", baseUrl, e.getMessage());
            return ConnectionTestResult.fail(e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public SyncResult fullSync(SyncContext context) {
        Map<String, Object> config = context.getConfig();
        String baseUrl = (String) config.get("baseUrl");
        String endpoint = (String) config.getOrDefault("endpoint", "/data");

        List<Map<String, Object>> allRows = new ArrayList<>();
        String cursor = null;
        int page = 0;
        int maxPages = 100; // 安全上限

        try {
            while (page < maxPages) {
                String url = baseUrl + endpoint;
                if (cursor != null) {
                    url += "?cursor=" + cursor;
                } else {
                    url += "?page=" + page;
                }

                ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
                if (response.getBody() != null) {
                    for (Object item : response.getBody()) {
                        if (item instanceof Map) {
                            allRows.add(new LinkedHashMap<>((Map<String, Object>) item));
                        }
                    }
                }

                // 从响应头获取下一页游标
                List<String> cursorHeaders = response.getHeaders().get("X-Next-Cursor");
                if (cursorHeaders != null && !cursorHeaders.isEmpty()) {
                    cursor = cursorHeaders.get(0);
                    if (cursor == null || cursor.isEmpty()) {
                        break;
                    }
                } else {
                    // 无游标头，检查返回数据量是否为空
                    if (response.getBody() == null || response.getBody().isEmpty()) {
                        break;
                    }
                }
                page++;
            }
            log.info("REST API 全量同步: 读取 {} 行, 共 {} 页", allRows.size(), page);
        } catch (Exception e) {
            log.error("REST API 全量同步失败: {}", e.getMessage(), e);
        }

        return new SyncResult(allRows.size(), allRows, cursor);
    }

    @Override
    public SyncResult incrementalSync(SyncContext context, String lastCursor) {
        // REST API 增量同步与全量同步类似，使用 lastCursor 作为起始游标
        Map<String, Object> config = context.getConfig();
        String baseUrl = (String) config.get("baseUrl");
        String endpoint = (String) config.getOrDefault("endpoint", "/data");
        String cursorParam = (String) config.getOrDefault("cursorParam", "since");

        List<Map<String, Object>> allRows = new ArrayList<>();
        String newCursor = lastCursor;

        try {
            String url = baseUrl + endpoint + "?" + cursorParam + "="
                    + (lastCursor != null ? lastCursor : "");
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            if (response.getBody() != null) {
                for (Object item : response.getBody()) {
                    if (item instanceof Map) {
                        allRows.add(new LinkedHashMap<>((Map<String, Object>) item));
                    }
                }
            }
            // 获取下一游标
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
}
