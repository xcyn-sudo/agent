# agent-qr-rag — P2 任务清单

> RAG 问答模块 P2 扩展：SSE 流式输出、混合检索（语义 + BM25 + RRF + Rerank）、Ollama Provider、LLM 熔断器、MySQL 前置结构化过滤、批量向量化攒批、DomainRouter 集成。

---

## 1. ChatQueryService — SSE 流式输出

- [ ] **1.1** 改造 `org.example.agent_qr.rag.service.ChatQueryService` 类
  - 新增属性：`HybridRetriever hybridRetriever`、`LLMCircuitBreaker circuitBreaker`
  - 新增方法 `askStream(String query, Long conversationId, Long userId, SseEmitter responseEmitter)`：
    1. 会话管理（无则新建）
    2. 保存用户提问
    3. 调用 EmbeddingProvider 生成查询向量
    4. 调用 `hybridRetriever.hybridSearch(query, queryEmbedding)` 混合检索
    5. 检索为空 → SSE 推送 error 事件 + complete
    6. 构建 Prompt → 通过熔断器获取 activeProvider → `llmProvider.generateStream(messages)`
    7. 流式订阅：token → SSE push("token", token)，error → 通知熔断器 recordFailure + push error，complete → recordSuccess + 构建 sources + push("done", {conversationId, sources})
    8. 保存完整 AI 回答 + 来源引用
    9. 发布 AnswerGeneratedEvent
  - 私有方法 `sendSseEvent(SseEmitter emitter, String eventName, Object data)`
  - 私有方法 `saveMessage(conversationId, role, content, sources)`
  - 保留 P1 原有同步 `ask()` 方法

---

## 2. Ollama Provider 实现

### 2.1 OllamaLLMProvider

- [ ] **2.1.1** 创建 `org.example.agent_qr.rag.provider.ollama.OllamaLLMProvider` 类
  - 实现 `LLMProvider` 接口
  - 注解 `@Component`
  - 属性：`baseUrl`（默认 `http://localhost:11434`）、`model`（默认 `qwen2.5:7b`）、`temperature`、`maxTokens`
  - 方法 `generate(List<Message> messages)`：调用 Ollama `/api/chat` 同步接口
  - 方法 `generateStream(List<Message> messages)`：调用 Ollama `/api/chat` 流式接口 → 返回 `Flux<String>`

### 2.2 OllamaEmbeddingProvider

- [ ] **2.2.1** 创建 `org.example.agent_qr.rag.provider.ollama.OllamaEmbeddingProvider` 类
  - 实现 `EmbeddingProvider` 接口
  - 注解 `@Component`
  - 属性：`baseUrl`（默认 `http://localhost:11434`）、`model`（默认 `nomic-embed-text`）
  - 方法 `embed(String text)`：调用 Ollama `/api/embeddings` → 返回 `float[]`
  - 方法 `embedBatch(List<String> texts)`：批量调用或循环调用（Ollama 原生不支持批量）

---

## 3. ProviderFactory P2 扩展

- [ ] **3.1** 改造 `org.example.agent_qr.rag.provider.ProviderFactory` 类
  - 新增属性：`OllamaLLMProvider ollamaLLMProvider`（`@Autowired(required = false)`）、`OllamaEmbeddingProvider ollamaEmbeddingProvider`（`@Autowired(required = false)`）
  - 新增方法 `getFallbackLLMProvider()`：面向熔断器，返回 DeepSeek 作为降级 Provider
  - 改造 `getLLMProvider()`：支持 ollama 类型切换
  - 改造 `getEmbeddingProvider()`：支持 ollama 类型切换

---

## 4. 混合检索器 — HybridRetriever

- [ ] **4.1** 创建 `org.example.agent_qr.rag.retriever.HybridRetriever` 类
  - 注解 `@Component`、`@Slf4j`
  - 属性：`ChromaRetriever chromaRetriever`、`BM25Retriever bm25Retriever`、`RerankerService rerankerService`、`StructuredFilterService structuredFilterService`、`DomainRouter domainRouter`（来自 agent-qr-catalog）
  - 配置：`semanticWeight`（0.6）、`keywordWeight`（0.4）、`wideTopK`（20）、`finalTopK`（5）
  - 方法 `hybridSearch(String query, float[] queryEmbedding, DomainRoutingResult routing, List<FilterCondition> filterConditions)`：
    1. Step 0 — 结构化过滤：若 filterConditions 非空 → `structuredFilterService.filterChunkIds()` 产出候选集
    2. Step 1 — 双路宽召回（并行）：语义检索 `chromaRetriever.similaritySearch(embed, wideTopK)` + BM25 `bm25Retriever.keywordSearch(query, wideTopK)`
    3. Step 2 — RRF 加权融合去重：`rrfFusion(semanticResults, bm25Results, w1, w2)`
    4. Step 3 — Rerank 精排：`rerankerService.rerank(query, fusedResults, finalTopK)`
  - 私有方法 `rrfFusion(...)`：score = w1/(k+rank_semantic) + w2/(k+rank_bm25)，k=60
  - 动态切换：若 filterConditions 为空 → 回退全库检索

---

## 5. BM25Retriever — Lucene 关键词检索

- [ ] **5.1** 创建 `org.example.agent_qr.rag.retriever.BM25Retriever` 类
  - 注解 `@Component`、`@Slf4j`
  - 属性：`IndexSearcherManager indexManager`（volatile）、`ChunkMapper chunkMapper`
  - 方法 `buildIndex()`：`@PostConstruct` → 从 MySQL 加载全量 READY 切片 → Lucene 内存索引
  - 方法 `buildIndexAsync()`：`@Async("indexBuilderExecutor")`、`@EventListener(ApplicationReadyEvent.class)` → 分页加载切片 → 异步构建磁盘索引，不阻塞启动
  - 方法 `onEmbeddingCompleted(EmbeddingCompletedEvent event)`：`@EventListener`、`@Async` → 增量添加新切片到索引
  - 方法 `keywordSearch(String query, int topK)`：IK Analyzer 中文分词 → QueryParser → `searcher.search()` → 返回 `List<RetrievedDocument>`
  - 方法 `addToIndex(Chunk chunk)`：增量添加单条切片
  - 方法 `removeFromIndex(Long chunkId)`：从索引中移除切片

---

## 6. RerankerService — bge-reranker-v2-m3 精排

- [ ] **6.1** 创建 `org.example.agent_qr.rag.retriever.RerankerService` 类
  - 注解 `@Component`、`@Slf4j`
  - 属性：`modelName`（`bge-reranker-v2-m3`）、`provider`（ollama）
  - 方法 `rerank(String query, List<RetrievedDocument> candidates, int topK)`：
    1. 候选 ≤ topK → 直接返回
    2. 构建 [query, doc] 对 → 调用 Reranker 模型打分
    3. 按相关性分数降序 → 取 Top-K
  - 私有方法 `computeRelevanceScore(String query, String document)`：调用 Ollama `/api/rerank` or 自定义评分

---

## 7. LLMCircuitBreaker — LLM 熔断器

- [ ] **7.1** 创建 `org.example.agent_qr.rag.circuitbreaker.LLMCircuitBreaker` 类
  - 注解 `@Component`、`@Slf4j`
  - 内部枚举 `State { CLOSED, OPEN, HALF_OPEN }`，初始 CLOSED
  - 属性：`ProviderFactory providerFactory`、`failureCount`（AtomicInteger）、`openTimestamp`（volatile long）
  - 配置：`failureThreshold`（3）、`openDurationMs`（30000）
  - 方法 `getActiveProvider()`：
    - CLOSED → 返回默认 Provider
    - OPEN → 检查熔断时长是否到期 → 到期则 HALF_OPEN + 返回主 Provider，否则返回 `getFallbackLLMProvider()`
    - HALF_OPEN → 允许探测请求通过（返回主 Provider）
  - 方法 `recordSuccess()`：HALF_OPEN → CLOSED + 重置计数器；CLOSED → 重置计数器
  - 方法 `recordFailure()`：CLOSED 且达到阈值 → OPEN；HALF_OPEN → OPEN

---

## 8. MySQL 前置结构化过滤

### 8.1 StructuredFilterService

- [ ] **8.1.1** 创建 `org.example.agent_qr.rag.filter.StructuredFilterService` 类
  - 注解 `@Service`、`@Slf4j`
  - 属性：`ChunkStructuredMapper structuredMapper`
  - 方法 `filterChunkIds(String domain, List<FilterCondition> conditions)`：
    1. 遍历 conditions → 按 fieldType 分派 Mapper 方法（NUMBER / DATE / ENUM / STRING）
    2. 多条件取交集（AND 语义）
    3. 截断到 500 条
    4. 返回 `List<Long>` chunk_id 候选集

### 8.2 ChunkStructuredMapper

- [ ] **8.2.1** 创建 `org.example.agent_qr.rag.filter.mapper.ChunkStructuredMapper` 接口
  - 继承 `BaseMapper<ChunkStructured>`
  - 方法 `selectChunkIdsByNumberRange(domain, fieldName, minValue, maxValue)`：B+树索引 `idx_domain_field_number`
  - 方法 `selectChunkIdsByDateRange(domain, fieldName, minDate, maxDate)`：B+树索引 `idx_domain_field_date`
  - 方法 `selectChunkIdsByStringValue(domain, fieldName, value)`：枚举/字符串精确匹配
  - 每条 SQL `LIMIT 500`

---

## 9. 批量向量化聚合 — BatchEmbeddingService

- [ ] **9.1** 创建 `org.example.agent_qr.rag.embedding.BatchEmbeddingService` 类
  - 注解 `@Component`、`@Slf4j`
  - 属性：`ProviderFactory providerFactory`、`BlockingQueue<ChunkEmbedTask> batchQueue`（容量 2000）
  - 配置：`batchSize`（32）、`batchTimeoutMs`（100）
  - 方法 `startConsumers()`：`@PostConstruct` → 启动 N 个消费者线程（N = CPU 核心数）
  - 方法 `submit(Chunk chunk)`：将切片提交到攒批队列，返回 `CompletableFuture<float[]>`
  - 私有方法 `consumeBatch()`：消费者循环 → poll 任务 → 攒满 OR 超时 → `executeBatch(batch)`
  - 私有方法 `executeBatch(List<ChunkEmbedTask> batch)`：一次 API 调用处理整批；批量失败降级逐条重试
  - 内部 record `ChunkEmbedTask(Chunk chunk, CompletableFuture<float[]> future)`

---

## 10. ChatController P2 扩展

- [ ] **10.1** 改造 `org.example.agent_qr.rag.controller.ChatController` 类
  - 新增端点 `POST /api/chat/ask` 返回 `SseEmitter`（MediaType `text/event-stream`）
  - 方法签名：`askStream(@RequestBody ChatRequest request)` → 创建 SseEmitter（超时 5 分钟）→ 委托 `chatQueryService.askStream()`
  - 新增端点 `POST /api/chat/feedback/{messageId}`：提交点赞/点踩评价 → 委托 `FeedbackService.submitFeedback()`

---

## 11. LLMProvider 接口扩展

- [ ] **11.1** 在 `org.example.agent_qr.rag.provider.LLMProvider` 接口中新增方法
  - `Flux<String> generateStream(List<Message> messages)`：流式生成方法
  - （P1 的 `generate` 方法保留）

---

## 12. EmbeddingProvider 接口扩展

- [ ] **12.1** 在 `org.example.agent_qr.rag.provider.EmbeddingProvider` 接口中新增方法
  - `List<float[]> embedBatch(List<String> texts)`：批量向量化方法
  - （P1 的 `embed` 方法保留）

---

## 13. DomainRouter 集成（引用 catalog 模块）

- [ ] **13.1** 在 `HybridRetriever` 中注入 `DomainRouter`（来自 `agent-qr-catalog`）
  - 调用 `domainRouter.route(query)` 获取 `DomainRoutingResult`
  - 调用 `domainRouter.buildRetrievalFilter(routing)` 构建 ChromaDB metadata 过滤条件
  - 若 routing.isFallbackToGlobal() → 全库检索

---

## 14. DDL — kb_chunk_structured 表

- [ ] **14.1** 编写 `kb_chunk_structured` 建表 SQL（追加到 p2-schema.sql）
  - 字段：`id BIGINT AUTO_INCREMENT PRIMARY KEY`、`chunk_id BIGINT NOT NULL`、`domain VARCHAR(32)`、`field_name VARCHAR(64)`、`field_value VARCHAR(255)`、`numeric_value DECIMAL(18,4)`、`date_value DATE`、`field_type VARCHAR(16) COMMENT 'NUMBER/DATE/ENUM/STRING'`
  - 索引：`idx_domain_field_number (domain, field_name, numeric_value)`、`idx_domain_field_date (domain, field_name, date_value)`、`idx_chunk_id (chunk_id)`

---

## 15. pom.xml 依赖

- [ ] **15.1** 在 `agent-qr-rag/pom.xml` 中新增依赖
  - `lucene-core`、`lucene-queryparser`、`lucene-analyzers-smartcn`（BM25 检索）
  - `spring-boot-starter-webflux`（SSE 流式响应 / WebClient）
  - `agent-qr-catalog`（引用 DomainRouter 进行域路由裁剪）
