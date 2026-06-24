# agent-qr-web — P2 任务清单

> Web 启动模块 P2 扩展：四池隔离 AsyncConfigV2、application-p2.yml、新增 P2 模块依赖。

---

## 1. AsyncConfigV2 — 四池隔离线程池配置

- [ ] **1.1** 创建 `org.example.agent_qr.web.config.AsyncConfigV2` 类
  - 注解 `@Configuration`、`@EnableAsync`
  - Bean `parseExecutor`（CPU 密集型）：
    - `corePoolSize = Runtime.getRuntime().availableProcessors()`
    - `maxPoolSize = core * 2`
    - `queueCapacity = 100`
    - 线程名前缀 `parse-`
    - 拒绝策略 `CallerRunsPolicy`
    - `TaskDecorator` 设为 `MdcTaskDecorator`
  - Bean `chunkExecutor`（CPU 密集型）：
    - 同 parseExecutor 配置
    - 线程名前缀 `chunk-`
  - Bean `embedExecutor`（IO 密集型）：
    - `corePoolSize = 8`
    - `maxPoolSize = 20`
    - `queueCapacity = 200`
    - 线程名前缀 `embed-`
  - Bean `deleteExecutor`（轻量级）：
    - `corePoolSize = 4`
    - `maxPoolSize = 4`
    - `queueCapacity = 50`
    - 线程名前缀 `delete-`
  - Bean `indexBuilderExecutor`（BM25 索引构建专用）：
    - `corePoolSize = 2`
    - `maxPoolSize = 2`
    - 线程名前缀 `index-builder-`

---

## 2. application-p2.yml 配置文件

- [ ] **2.1** 创建 `src/main/resources/application-p2.yml` 文件
  - 包含以下配置节：
    - `spring.profiles: p2`
    - **JWT 双 Token 配置**：`jwt.access-expiration: 1800`、`jwt.refresh-expiration: 604800`
    - **Ollama Provider 配置**：`llm.ollama.base-url`、`llm.ollama.model`、`embedding.ollama.base-url`、`embedding.ollama.model`
    - **Provider 切换开关**：`llm.provider: deepseek`、`embedding.provider: deepseek`
    - **混合检索配置**：`rag.hybrid.semantic-weight: 0.6`、`rag.hybrid.keyword-weight: 0.4`、`rag.retrieval.wide-top-k: 20`、`rag.retrieval.final-top-k: 5`
    - **Reranker 配置**：`rag.reranker.model: bge-reranker-v2-m3`、`rag.reranker.provider: ollama`
    - **熔断器配置**：`circuit-breaker.failure-threshold: 3`、`circuit-breaker.open-duration-ms: 30000`
    - **批量向量化配置**：`embedding.batch.size: 32`、`embedding.batch.timeout-ms: 100`
    - **DLQ 配置**：`dlq.max-retries: 4`、`dlq.backoff-base: 3`
    - **数据质量阻断阈值**：`dataquality.block-threshold: 0.5`
    - **PDF 解析配置**：`parser.pdf.max-memory-mb: 256`、`parser.ocr.enabled: false`
    - **ChromaDB Collection**：`langchain4j.chroma.collection-name: enterprise_knowledge`
    - **Caffeine 缓存**：`cache.llm.max-size: 10000`、`cache.llm.ttl-hours: 1`

---

## 3. 全局异常处理 — GlobalExceptionHandler P2 增强

- [ ] **3.1** 改造 `GlobalExceptionHandler`（agent-qr-web 包下）
  - 确认 `AccessDeniedException` 处理器存在（与 AbacAccessDeniedHandler 互补）
  - 确认所有异常响应包含 TraceId（从 MDC 获取）
  - 新增 `MethodArgumentNotValidException` 处理器（若 P1 未实现则补充）

---

## 4. SecurityConfig P2 更新

- [ ] **4.1** 确认 `SecurityConfig` 从 `agent-qr-auth` 模块引入（P1 设计为 auth 模块管理）
  - 若 `agent-qr-web` 保留了独立的 SecurityConfig，需同步更新路由规则（添加 `/api/auth/refresh`、`/api/catalog/**`）

---

## 5. pom.xml — 新增 P2 模块依赖

- [ ] **5.1** 在 `agent-qr-web/pom.xml` 中新增依赖
  - `agent-qr-compensation`（数据一致性补偿）
  - `agent-qr-datasource`（多源数据接入）
  - `agent-qr-data-quality`（数据质量检查）
  - `agent-qr-etl`（ETL 标准化管道）
  - `agent-qr-catalog`（知识目录）

---

## 6. p2-schema.sql — P2 集中 DDL 迁移脚本

- [ ] **6.1** 创建 `src/main/resources/db/p2-schema.sql` 文件
  - 汇聚所有 P2 新表和变更的 SQL（各模块 DDL 统一由此文件管理）：
    - CREATE TABLE `dlq_message`
    - CREATE TABLE `token_refresh`
    - CREATE TABLE `delete_task`
    - CREATE TABLE `data_source_config`
    - CREATE TABLE `kb_chunk_structured`
    - ALTER TABLE `sys_user`（ABAC 字段）
    - ALTER TABLE `kb_document`（domain / sensitivity / deleted / error_msg）
    - ALTER TABLE `kb_chunk`（deleted）
    - ALTER TABLE `chat_message`（feedback / feedback_reason）
    - ALTER TABLE `stat_daily`（positive_count / negative_count）
