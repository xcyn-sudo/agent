# agent-qr-rag -- P3 任务清单

> RAG 问答模块 P3 扩展：Embedding 语义域路由 + 向量维度管理 + Provider 切换决策
> 更新日期：2026-06-28
> 阶段目标：语义化域路由替代关键词匹配，自动化向量维度管理，智能 Provider 切换

---

## 1. 向量维度管理 — EmbeddingDimensionManager

- [ ] **1.1** 创建 `EmbeddingDimensionManager` 类
  - 路径：`org.example.agent_qr.rag.embedding.EmbeddingDimensionManager`
  - 注解：`@Component`、`@Slf4j`
  - 注入：`@Autowired private ProviderFactory providerFactory`
  - 注入：`@Autowired private ChromaClient chromaClient`（或通过 `ChromaConfig` 获取）

- [ ] **1.2** 实现 Collection 命名方案
  - 方法：`public String getCollectionName()`
  - 返回格式：`kb_{providerType}_{modelName}`
  - 示例：`kb_deepseek_deepseek-embedding`、`kb_ollama_nomic-embed-text`
  - 注意：modelName 中的 `:` 替换为 `-`（避免 ChromaDB 命名限制）

- [ ] **1.3** 实现维度检测
  - 方法：`@EventListener(ApplicationReadyEvent.class) public void checkDimension()`
  - 逻辑：
    1. 调用 `getCollectionName()` 获取当前 Collection 名称
    2. 调用 `chromaClient.collectionExists(collectionName)` 检查是否存在
    3. 若不存在 → `log.warn("检测到新 Embedding 模型: collection={} 不存在，需全量重建向量")`
    4. 若存在 → `log.info("Embedding 维度一致性检查通过: collection={}")`

- [ ] **1.4** 实现 Collection 缓存管理
  - 属性：`private final Map<String, Boolean> collectionCache = new ConcurrentHashMap<>()`
  - 方法：`public boolean ensureCollection(String collectionName)`
  - 逻辑：检查缓存 → 不存在则调用 ChromaDB API 创建 → 更新缓存

---

## 2. Embedding 语义域路由 — DomainRouterV2

- [ ] **2.1** 创建 `DomainRouterV2` 类
  - 路径：`org.example.agent_qr.rag.router.DomainRouterV2`
  - 注解：`@Component`、`@Slf4j`
  - 注入：`@Autowired private EmbeddingProvider embeddingProvider`
  - 注入：`@Autowired(required = false) private KnowledgeCatalogService catalogService`（可选，用于获取域描述）

- [ ] **2.2** 实现域 Embedding 预计算
  - 属性：`private final Map<String, float[]> domainEmbeddings = new ConcurrentHashMap<>()`
  - 方法：`@PostConstruct public void init()`
  - 逻辑：
    1. 定义各业务域及其自然语言描述：
       - `"HR"` → `"人力资源管理，员工信息，薪酬福利，绩效考核，组织架构"`
       - `"FINANCE"` → `"财务管理，会计核算，预算控制，成本管理，财务报表"`
       - `"RD"` → `"研发管理，产品设计，技术标准，代码管理，测试管理"`
       - `"SALES"` → `"销售管理，客户关系，合同管理，销售业绩，市场推广"`
    2. 对每个域描述调用 `embeddingProvider.embed(desc)` 预计算向量
    3. 存入 `domainEmbeddings` Map
  - 注意：如有 `KnowledgeCatalogService`，可动态获取域列表替代硬编码

- [ ] **2.3** 实现语义路由算法
  - 方法：`public DomainRoutingResult route(String query)`
  - 逻辑：
    1. 调用 `embeddingProvider.embed(query)` 获取查询向量
    2. 遍历 `domainEmbeddings`，计算每个域的余弦相似度
    3. 相似度阈值：`> 0.3`（低于此值视为不相关）
    4. 按相似度降序排列，取 Top 2
    5. 若无域达标 → 返回 `DomainRoutingResult.fallback()`（全局检索）
    6. 否则 → 返回 `DomainRoutingResult.routed(top2Map)`

- [ ] **2.4** 实现余弦相似度计算方法
  - 方法：`private double cosineSimilarity(float[] a, float[] b)`
  - 算法：`dotProduct(a, b) / (norm(a) * norm(b))`
  - 注意：处理零向量边界情况（返回 0.0）

- [ ] **2.5** 实现域 Embedding 定时刷新
  - 方法：`@Scheduled(fixedRate = 300000) public void refreshDomainEmbeddings()`
  - 每 5 分钟重新计算所有域的 Embedding 向量
  - 目的：目录结构可能变化，定期同步保持路由准确性
  - 使用写时复制保证并发安全

---

## 3. Provider 切换自动决策

- [ ] **3.1** 创建 `ProviderDecisionEngine` 类
  - 路径：`org.example.agent_qr.rag.provider.ProviderDecisionEngine`
  - 注解：`@Component`、`@Slf4j`
  - 注入：`@Autowired private ProviderFactory providerFactory`
  - 注入：`@Autowired private LLMCircuitBreaker circuitBreaker`

- [ ] **3.2** 实现 Provider 决策算法
  - 方法：`public LLMProvider decideLLMProvider()`
  - 逻辑：
    1. 读取配置 `llm.provider`（默认值：`deepseek`）
    2. 检查首选 Provider 的熔断器状态
    3. 若熔断器打开（OPEN）→ 自动切换至备用 Provider（如 `ollama`）
    4. 若熔断器半开（HALF_OPEN）→ 尝试首选，失败则切换备用
    5. 若熔断器关闭（CLOSED）→ 使用首选 Provider
    6. 记录决策日志：`log.info("Provider 决策: 选择 {} (原因: {})", provider, reason)`

- [ ] **3.3** 实现 Embedding Provider 决策
  - 方法：`public EmbeddingProvider decideEmbeddingProvider()`
  - 逻辑同 LLM Provider 决策
  - 读取配置 `embedding.provider`（默认值：`deepseek`）

- [ ] **3.4** 更新 `ProviderFactory` 集成决策引擎
  - 路径：`org.example.agent_qr.rag.provider.ProviderFactory`
  - 注入 `ProviderDecisionEngine`
  - 修改 `getLLMProvider()` 方法：优先使用决策引擎推荐，降级使用配置值
  - 修改 `getEmbeddingProvider()` 方法：同上

---

## 4. 集成改造

- [ ] **4.1** 改造 `ChatQueryService` — 接入 DomainRouterV2
  - 路径：`org.example.agent_qr.rag.service.ChatQueryService`
  - 新增注入：`@Autowired(required = false) private DomainRouterV2 domainRouterV2`
  - 修改 `resolveRouting()` 方法：
    1. 优先使用 `domainRouterV2`（P3 语义路由）
    2. 若不可用，降级使用 `domainRouter`（P2 关键词路由）
    3. 两者都不可用 → 全局检索
  - 保留 P2 `domainRouter` 注入以兼容降级

- [ ] **4.2** 改造 `HybridRetriever` — 接入 DomainRouterV2
  - 路径：`org.example.agent_qr.rag.retriever.HybridRetriever`
  - 新增注入：`@Autowired(required = false) private DomainRouterV2 domainRouterV2`
  - 修改 `hybridSearch()` 方法：优先使用语义路由结果
  - 保留 P2 `domainRouter` 注入以兼容降级

- [ ] **4.3** 改造 `BatchEmbeddingService` — 接入 EmbeddingDimensionManager
  - 路径：`org.example.agent_qr.rag.embedding.BatchEmbeddingService`
  - 新增注入：`@Autowired private EmbeddingDimensionManager dimensionManager`
  - 写入 ChromaDB 前调用 `dimensionManager.getCollectionName()` 获取目标 Collection
  - 替代原有硬编码或配置的 Collection 名称

- [ ] **4.4** 更新 `agent-qr-rag/pom.xml`（如需要）
  - P3 无新增外部依赖
  - 确认已有 `agent-qr-catalog` 依赖（DomainRouterV2 可选依赖 `KnowledgeCatalogService`）

---

> **依赖关系**：
> - `DomainRouterV2` → `EmbeddingProvider`（rag 模块内部）
> - `DomainRouterV2` → `KnowledgeCatalogService`（catalog 模块，可选）
> - `EmbeddingDimensionManager` → `ProviderFactory`、`ChromaClient`（rag 模块内部）
> - `ProviderDecisionEngine` → `ProviderFactory`、`LLMCircuitBreaker`（rag 模块内部）
>
> **统计**：共 4 大类，约 15 个子任务
> 预计耗时：2 天
