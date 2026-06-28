package org.example.agent_qr.catalog.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.catalog.dto.DomainRoutingResult;
import org.example.agent_qr.catalog.entity.CatalogTree;
import org.example.agent_qr.catalog.router.DomainRouterV2;
import org.example.agent_qr.catalog.service.KnowledgeCatalogService;
import org.example.agent_qr.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 知识目录 REST 控制器。
 * <p>
 * 提供知识目录树查询和统计概览两个只读端点。
 * 目录树由 {@link KnowledgeCatalogService} 基于数据源配置实时构建，
 * 无需持久化存储；监听 ETL 完成事件以保证索引时效性。
 * </p>
 * <p>
 * P3 新增：{@code GET /api/catalog/route?q=xxx} 调试端点，
 * 用于观察语义路由匹配结果。
 * </p>
 * <p>
 * 权限：P2 阶段所有已认证用户均可浏览知识目录，
 * 不在此层做 ABAC 细粒度域过滤（目录树展示的是全貌，检索阶段再由 DomainRouter 做域裁剪）。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final KnowledgeCatalogService catalogService;

    /** P3 新增：catalog 模块的语义域描述缓存（可选，用于调试端点） */
    @Autowired(required = false)
    private DomainRouterV2 domainRouterV2;

    /**
     * 获取三级知识目录树。
     * <p>
     * 返回结构：
     * <ul>
     *   <li>一级：业务域节点（DomainNode），含域名、数据源数量、实体总数</li>
     *   <li>二级：数据源节点（SourceNode），含数据源类型、最后同步时间、同步总量</li>
     *   <li>三级：实体节点（EntityNode），含实体名称、类型、记录数、最后更新时间</li>
     * </ul>
     * </p>
     *
     * @return 目录树
     */
    @GetMapping("/tree")
    public Result<CatalogTree> getCatalogTree() {
        CatalogTree tree = catalogService.getCatalogTree();
        log.debug("目录树查询完成: {} 个域", tree.getDomains().size());
        return Result.success(tree);
    }

    /**
     * 获取知识目录统计概览。
     * <p>
     * 聚合统计域数量、数据源总数和实体总数，
     * 供前端 Dashboard 或目录页概览卡片使用。
     * </p>
     *
     * @return 统计数据（totalDomains / totalSources / totalEntities）
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = catalogService.getStats();
        return Result.success(stats);
    }

    /**
     * 域描述调试端点（P3 新增，可选）。
     * <p>
     * 返回当前目录树中所有域的自然语言描述，便于观察语义路由的域描述内容。
     * 若 DomainRouterV2 不可用，返回空的域描述 Map。
     * </p>
     *
     * @return 域ID → 自然语言描述的 Map
     */
    @GetMapping("/route")
    public Result<Map<String, String>> route() {
        if (domainRouterV2 != null) {
            try {
                Map<String, String> descriptions = domainRouterV2.getDomainDescriptions();
                log.debug("域描述查询: {} 个域", descriptions.size());
                return Result.success(descriptions);
            } catch (Exception e) {
                log.warn("域描述查询失败", e);
            }
        }
        return Result.success(Map.of());
    }
}
