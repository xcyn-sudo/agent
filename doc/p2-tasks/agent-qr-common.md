# agent-qr-common — P2 任务清单

> 公共模块 P2 扩展：TraceId 全链路追踪、MDC 异步传递、Caffeine 缓存、死信队列基础设施。

---

## 1. 全链路追踪 TraceIdFilter

- [ ] **1.1** 创建 `org.example.agent_qr.common.filter.TraceIdFilter` 类
  - 继承 `OncePerRequestFilter`
  - 从请求头 `X-Trace-Id` 提取 TraceId，若无则生成 16 位 UUID 短码
  - 写入 `MDC.put("traceId", traceId)`
  - 响应头回写 `X-Trace-Id`
  - finally 块清理 `MDC.clear()`

---

## 2. MDC 异步传递装饰器

- [ ] **2.1** 创建 `org.example.agent_qr.common.executor.MdcTaskDecorator` 类
  - 实现 `TaskDecorator`
  - `decorate(Runnable)` → 复制当前线程 MDC 到工作线程
  - finally 块清理工作线程 MDC

---

## 3. Caffeine 本地缓存配置

- [ ] **3.1** 创建 `org.example.agent_qr.common.config.CaffeineConfig` 类
  - 注解 `@Configuration`
  - Bean `llmResponseCache()`：`Cache<String, String>`
  - 配置：`maximumSize=10000`、`expireAfterWrite=1h`
  - 注解 `@Configuration`

---

## 4. 死信队列（DLQ）基础设施

### 4.1 死信消息实体

- [ ] **4.1.1** 创建 `org.example.agent_qr.common.dlq.entity.DlqMessage` 类
  - 注解 `@Data`、`@TableName("dlq_message")`
  - 属性：`Long id`、`String eventType`、`Long documentId`、`String payload`（TEXT）、`String errorMsg`、`Integer retryCount`、`LocalDateTime nextRetryAt`、`String status`（PENDING/DEAD）
  - 表名 `dlq_message`

### 4.2 DeadLetterQueue 管理器

- [ ] **4.2.1** 创建 `org.example.agent_qr.common.dlq.DeadLetterQueue` 类
  - 注解 `@Component`、`@Slf4j`
  - 属性：`DlqMessageMapper`、`maxRetries`（默认 4）、`backoffBase`（默认 3）
  - 方法 `enqueue(eventType, documentId, payload, error)`：写入死信队列，计算首次重试延迟 = backoffBase^1 秒
  - 方法 `calcBackoffSeconds(retryCount)`：3^(retryCount+1) 秒 → retry 0→3s, 1→9s, 2→27s, 3→81s
  - 方法 `updateRetryResult(msgId, success, error)`：成功则删除，失败则递增 retry_count + 更新 next_retry_at，超过 maxRetries 标记 DEAD

### 4.3 DlqMessageMapper

- [ ] **4.3.1** 创建 `org.example.agent_qr.common.dlq.DlqMessageMapper` 接口
  - 继承 `BaseMapper<DlqMessage>`
  - 方法 `selectPendingRetries(LocalDateTime now)`：查询 next_retry_at ≤ now 且 status='PENDING' 的记录
  - 方法 `updateRetry(msgId, retryCount, nextRetryAt, error)`：更新重试次数和下次重试时间
  - 方法 `updateStatus(msgId, status, error)`：更新状态（标记 DEAD）

### 4.4 DLQ 定时重试调度器

- [ ] **4.4.1** 创建 `org.example.agent_qr.common.dlq.DlqRetryScheduler` 类
  - 注解 `@Component`、`@Slf4j`
  - 属性：`DeadLetterQueue`、`DlqMessageMapper`、`DocumentParserService`、`ChunkEmbeddingListener`
  - 方法 `retryDeadLetters()`：`@Scheduled(fixedDelay = 30000)` 每 30 秒扫描到期记录
  - 根据 eventType 分派：`PARSE` → retryParse / `CHUNK` → retryChunk / `EMBED` → retryEmbed / `DELETE` → retryDelete
  - 私有方法 `retryParse(msg)`、`retryChunk(msg)`、`retryEmbed(msg)`、`retryDelete(msg)`

---

## 5. DDL — dlq_message 表

- [ ] **5.1** 编写 `dlq_message` 建表 SQL（追加到 p2-schema.sql）
  - 字段：`id BIGINT AUTO_INCREMENT PRIMARY KEY`、`event_type VARCHAR(32)`、`document_id BIGINT`、`payload TEXT`、`error_msg VARCHAR(1000)`、`retry_count INT DEFAULT 0`、`next_retry_at DATETIME`、`status VARCHAR(16) DEFAULT 'PENDING'`、`create_time DATETIME`
  - 索引：`idx_status_retry (status, next_retry_at)`、`idx_document_id (document_id)`

---

## 6. 文档删除请求事件 — DocumentDeleteRequestedEvent

- [ ] **6.1** 创建 `org.example.agent_qr.common.event.DocumentDeleteRequestedEvent` 类
  - 继承 `ApplicationEvent`
  - 字段：`Long documentId`、`List<Long> chunkIds`、`List<String> chromaIds`、`String filePath`
  - 构造器：`DocumentDeleteRequestedEvent(Object source, Long documentId, List<Long> chunkIds, List<String> chromaIds, String filePath)`
  - 使用 `@Getter`、`@Setter` 注解
  - **用途**：knowledge 模块发布此事件，compensation 模块监听并执行物理删除

---

## 7. pom.xml 依赖

- [ ] **7.1** 在 `agent-qr-common/pom.xml` 中新增依赖
  - `spring-boot-starter-aop`（为 CaffeineConfig / TraceIdFilter 提供 @Aspect 支持）
  - `caffeine`（Caffeine 本地缓存）
  - 注：第 4 节 DLQ 所需的 `spring-boot-starter` 已由 P1 的 `spring-boot-starter-web` 传递引入
