package org.example.agent_qr.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 域路由结果。
 * <p>
 * 封装关键词域路由的匹配结果，包括匹配到的域及其分数、
 * 匹配到的实体列表以及是否降级到全局检索。
 * </p>
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DomainRoutingResult {

    /** 匹配到的域及分数（域名为 key，分数为 value） */
    private Map<String, Double> matchedDomains = new LinkedHashMap<>();

    /** 匹配到的实体名称列表 */
    private List<String> matchedEntities = List.of();

    /** 是否降级到全局检索 */
    private boolean fallbackToGlobal;

    /**
     * 获取最高分域。
     *
     * @return 最高分域名，无匹配返回 null
     */
    public String getPrimaryDomain() {
        return matchedDomains.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * 创建降级到全局检索的空结果。
     *
     * @return fallbackToGlobal=true 的空路由结果
     */
    public static DomainRoutingResult fallback() {
        DomainRoutingResult result = new DomainRoutingResult();
        result.setFallbackToGlobal(true);
        return result;
    }
}
