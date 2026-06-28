# agent-qr-catalog -- P3 任务清单

> 知识目录模块 P3 扩展：Embedding 语义域路由升级
> 更新日期：2026-06-28
> 阶段目标：从 P2 关键词匹配路由升级为 Embedding 语义相似度路由

---

## 1. 语义域路由 — DomainRouterV2

- [ ] **1.1** 创建 `DomainRouterV2` 类
  - 路径：`org.example.agent_qr.catalog.router.DomainRouterV2`
  - 注解：`@Component`、`@Slf4j`
  - 注入：`@Autowired private EmbeddingProvider embeddingProvider`（通过 `agent-qr-rag` 提供）
  - 注入：`@Autowired private KnowledgeCatalogService catalogService`
  - **设计决策**：catalog 模块的 `DomainRouterV2` 与 rag 模块的版本是**互补关系**：
    - catalog 版负责：基于目录树的域描述生成 Embedding 向量
    - rag 版负责：在问答流程中使用 Embedding 做路由匹配
    - 两者可以合并为一个类（放在 rag 模块），catalog 模块通过依赖注入使用

- [ ] **1.2** 实现基于目录树的域描述生成
  - 方法：`private Map<String, String> buildDomainDescriptions()`
  - 逻辑：
    1. 调用 `catalogService.getCatalogTree()` 获取完整目录树
    2. 遍历每个 `DomainNode`，拼接域描述：
       - 域名
       - 子数据源名称列表
       - 子实体名称列表（Top 10）
    3. 返回 `Map<域ID, 自然语言描述>`
  - 替代硬编码的域描述，实现动态适配

- [ ] **1.3** 实现域 Embedding 缓存管理
  - 属性：`private volatile Map<String, float[]> cachedDomainEmbeddings`
  - 方法：`public Map<String, float[]> getDomainEmbeddings()`
  - 逻辑：若缓存为空或过期（> 5 分钟），重新计算
  - 线程安全：使用 `synchronized` 或 `ReadWriteLock`

- [ ] **1.4** 监听目录变更事件刷新缓存
  - 方法：`@EventListener public void onCatalogChanged(DataSyncCompletedEvent event)`（或其他目录变更事件）
  - 逻辑：清空 `cachedDomainEmbeddings`，下次访问时懒加载重建
  - 新增事件（如需要）：`CatalogUpdatedEvent` 在 `KnowledgeCatalogService` 中发布

---

## 2. 与 P2 DomainRouter 的共存与切换

- [ ] **2.1** 更新 `KnowledgeCatalogService` 支持双路由
  - 路径：`org.example.agent_qr.catalog.service.KnowledgeCatalogService`
  - 新增注入：`@Autowired private DomainRouterV2 domainRouterV2`（可选，`required = false`）
  - 新增方法：`public DomainRoutingResult routeBySemantic(String query)`
  - 使用 `@ConditionalOnProperty` 或配置开关控制启用 P3 语义路由
  - 配置 key：`catalog.routing.mode=semantic|keyword|auto`

- [ ] **2.2** 确保 P2 `DomainRouter` 作为降级方案
  - 保留现有 P2 `DomainRouter` 不变
  - 当 `DomainRouterV2` 不可用（如 Embedding Provider 故障）时自动降级
  - 降级逻辑与 rag 模块的 `ChatQueryService.resolveRouting()` 保持一致

---

## 3. 模块配置更新

- [ ] **3.1** 更新 `agent-qr-catalog/pom.xml`（如需要）
  - 确认已有 `agent-qr-rag` 依赖（提供 `EmbeddingProvider` 接口）
  - P3 无新增外部依赖

- [ ] **3.2** 更新 `CatalogController`（如需新 API）
  - 路径：`org.example.agent_qr.catalog.controller.CatalogController`
  - 新增端点（可选）：`GET /api/catalog/route?q=xxx` — 返回语义路由结果（调试用）
  - 保留 P2 端点 `GET /api/catalog/tree`、`GET /api/catalog/stats` 不变

---

> **依赖关系**：
> - `DomainRouterV2` → `EmbeddingProvider`（rag 模块）
> - `DomainRouterV2` → `KnowledgeCatalogService`（catalog 模块内部）
>
> **说明**：catalog 模块的 `DomainRouterV2` 主要负责域描述的 Embedding 预计算和缓存管理。运行时路由调用由 rag 模块的 `DomainRouterV2` 执行。两个模块的 `DomainRouterV2` 可合并为一个类放在 rag 模块，catalog 模块通过依赖注入使用。
>
> **统计**：共 3 大类，约 7 个子任务
> 预计耗时：1 天
