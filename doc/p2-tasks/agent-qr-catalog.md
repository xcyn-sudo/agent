# agent-qr-catalog — P2 任务清单（★ 新模块）

> 知识目录模块：三级目录树（业务域 → 数据源 → 数据实体）、关键词域路由器、检索范围裁剪。

---

## 1. Maven 模块初始化

- [ ] **1.1** 创建 `agent-qr-catalog/` 目录结构
  - `pom.xml`
  - `src/main/java/org/example/agent_qr/catalog/`
  - 子包：`service/`、`router/`、`entity/`、`dto/`

- [ ] **1.2** 配置 `agent-qr-catalog/pom.xml`
  - `groupId`: `org.example`
  - `artifactId`: `agent-qr-catalog`
  - 依赖：`agent-qr-common`、`agent-qr-datasource`、`spring-boot-starter-web`

---

## 2. 目录实体模型

### 2.1 CatalogTree — 目录树根节点

- [ ] **2.1.1** 创建 `org.example.agent_qr.catalog.entity.CatalogTree` 类
  - 注解 `@Data`
  - 属性：`List<DomainNode> domains`
  - 方法 `addDomain(DomainNode node)`

### 2.2 DomainNode — 业务域节点

- [ ] **2.2.1** 创建 `org.example.agent_qr.catalog.entity.DomainNode` 类
  - 注解 `@Data`
  - 属性：`String domainName`、`int sourceCount`、`List<SourceNode> sources`
  - 方法 `addSource(SourceNode node)`

### 2.3 SourceNode — 数据源节点

- [ ] **2.3.1** 创建 `org.example.agent_qr.catalog.entity.SourceNode` 类
  - 注解 `@Data`
  - 属性：`Long sourceId`、`String sourceName`、`String sourceType`、`LocalDateTime lastSyncAt`、`int totalSynced`、`List<EntityNode> entities`

### 2.4 EntityNode — 数据实体节点

- [ ] **2.4.1** 创建 `org.example.agent_qr.catalog.entity.EntityNode` 类
  - 注解 `@Data`
  - 属性：`String entityName`（表名 / 文件名 / API 端点）、`String entityType`（TABLE / FILE / API）、`int recordCount`

---

## 3. KnowledgeCatalogService — 目录管理服务

- [ ] **3.1** 创建 `org.example.agent_qr.catalog.service.KnowledgeCatalogService` 类
  - 注解 `@Service`、`@Slf4j`
  - 属性：`DataSourceMapper dataSourceMapper`、`DocumentMapper documentMapper`、`ChunkMapper chunkMapper`
  - 方法 `getCatalogTree()`：
    1. 查询所有 ACTIVE 数据源
    2. 按 domain 分组 → 构建 DomainNode
    3. 每个数据源填充 SourceNode（sourceId / sourceName / sourceType / lastSyncAt / totalSynced）
    4. 每个数据源下获取数据实体列表（表名 / 文件名）
    5. 返回 `CatalogTree`
  - 方法 `onDataETLed(DataETLedEvent event)`：`@EventListener`、`@Async`
    1. 监听 ETL 完成事件
    2. 更新目录索引（Eager 重建或增量更新）

---

## 4. DomainRouter — 关键词域路由器

- [ ] **4.1** 创建 `org.example.agent_qr.catalog.router.DomainRouter` 类
  - 注解 `@Component`、`@Slf4j`
  - 属性：`KnowledgeCatalogService catalogService`
  - 方法 `route(String query)`：
    1. NLP 实体词提取：`extractKeywords(query)`（使用 IK Analyzer 或 jieba 分词）
    2. 获取 `CatalogTree`
    3. 遍历目录树计算域匹配分数：
       - 域名匹配 query → +0.5
       - 数据源名匹配 → +0.3
       - 实体名匹配（表名 / 文件名）→ +0.4
    4. score > 0 加入 matchedDomains
    5. 若 matchedDomains 为空 → `fallbackToGlobal = true`
    6. 返回 `DomainRoutingResult`
  - 私有方法 `extractKeywords(String query)`：分词 + 停用词过滤
  - 私有方法 `containsAny(String text, String keyword)`：模糊匹配

---

## 5. DomainRoutingResult — 路由结果 DTO

- [ ] **5.1** 创建 `org.example.agent_qr.catalog.dto.DomainRoutingResult` 类
  - 注解 `@Data`
  - 属性：`Map<String, Double> matchedDomains`、`List<String> matchedEntities`、`boolean fallbackToGlobal`
  - 方法 `getPrimaryDomain()`：返回最高分域
  - 静态方法 `fallback()`：返回 fallbackToGlobal=true 的空路由结果

---

## 6. buildRetrievalFilter — 检索过滤条件构建

- [ ] **6.1** 在 `DomainRouter` 中新增方法 `buildRetrievalFilter(DomainRoutingResult routing)`
  - 若 `routing.isFallbackToGlobal()` → 返回空 Map（全库检索）
  - 否则 → 返回 `Map.of("domain", "HR,FINANCE")`（用于 ChromaDB metadata 过滤）
