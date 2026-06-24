# agent-qr-compensation — P2 任务清单（★ 新模块）

> 数据一致性补偿模块：孤儿向量扫描清理、文档删除补偿流程（先逻辑删后物理删 + afterCommit 回调）、DLQ 重试集成。

---

## 1. Maven 模块初始化

- [ ] **1.1** 创建 `agent-qr-compensation/` 目录结构
  - `pom.xml`
  - `src/main/java/org/example/agent_qr/compensation/`
  - 子包：`scanner/`、`service/`、`listener/`、`entity/`、`mapper/`

- [ ] **1.2** 配置 `agent-qr-compensation/pom.xml`
  - `groupId`: `org.example`
  - `artifactId`: `agent-qr-compensation`
  - 依赖：`agent-qr-common`、`agent-qr-knowledge`、`langchain4j-chroma`、`spring-boot-starter`

---

## 2. DocumentDeleteListener — 删除事件监听器（★ 事件驱动核心）

- [ ] **2.1** 创建 `org.example.agent_qr.compensation.listener.DocumentDeleteListener` 类
  - 注解 `@Component`、`@Slf4j`
  - 属性：`DocumentMapper documentMapper`、`ChunkMapper chunkMapper`、`DocumentDeleteServiceV2 documentDeleteServiceV2`
  - 方法 `handleDocumentDeleteRequested(DocumentDeleteRequestedEvent event)`：
    - 注解 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` — 确保在 knowledge 模块的事务提交成功后才执行
    - 注解 `@Async("deleteExecutor")` — 异步执行，不阻塞 Controller 响应
    - 逻辑：
      1. 调用 `chunkMapper.softDeleteByDocumentId(event.getDocumentId())` — MySQL 逻辑删除切片
      2. 调用 `documentMapper.softDelete(event.getDocumentId())` — MySQL 逻辑删除文档
      3. 调用 `documentDeleteServiceV2.asyncPhysicalDelete(event.getDocumentId(), event.getChromaIds())` — 异步物理删除 ChromaDB 向量
      4. 异常处理：`deadLetterQueue.enqueue("DELETE", event.getDocumentId(), ...)` → 进入 DLQ 重试
  - **关键设计**：knowledge 模块只发布事件，不注入 compensation 的任何类；compensation 单向依赖 knowledge，无循环依赖

---

## 3. 孤儿向量扫描器 — OrphanVectorScanner

- [ ] **2.1** 创建 `org.example.agent_qr.compensation.scanner.OrphanVectorScanner` 类
  - 注解 `@Component`、`@Slf4j`
  - 属性：`ChunkMapper chunkMapper`、`ChromaVectorStore chromaVectorStore`、`DocumentMapper documentMapper`
  - 方法 `scanAndCleanOrphanVectors()`：`@Scheduled(fixedDelay = 1800000)` 每 30 分钟执行
    1. 获取 ChromaDB 中所有向量的 document_id 元数据
    2. 在 MySQL 中查询哪些文档已不存在（`documentMapper.selectById(id) == null`）
    3. 批量删除孤儿向量：`chromaVectorStore.deleteByMetadata("document_id", docId)`
    4. 记录清理数量日志

---

## 4. 删除任务实体 & Mapper

### 4.1 DeleteTask 实体

- [ ] **3.1.1** 创建 `org.example.agent_qr.compensation.entity.DeleteTask` 类
  - 注解 `@Data`、`@TableName("delete_task")`
  - 属性：`Long id`、`Long documentId`、`String chromaIds`（TEXT，JSON 数组）、`String status`（PENDING / DONE / FAILED）、`Integer retryCount`（默认 0）、`LocalDateTime createTime`

### 4.2 DeleteTaskMapper

- [ ] **3.2.1** 创建 `org.example.agent_qr.compensation.mapper.DeleteTaskMapper` 接口
  - 继承 `BaseMapper<DeleteTask>`
  - 方法 `updateStatus(Long id, String status)`：更新删除任务状态
  - 方法 `incrementRetryCount(Long id)`：递增重试计数

---

## 5. DocumentDeleteServiceV2 — 物理删除 + 补偿重试

- [ ] **5.1** 创建 `org.example.agent_qr.compensation.service.DocumentDeleteServiceV2` 类
  - 注解 `@Service`、`@Slf4j`
  - 属性：`ChromaVectorStore chromaVectorStore`、`DeleteTaskMapper deleteTaskMapper`、`DeadLetterQueue deadLetterQueue`
  - 方法 `asyncPhysicalDelete(Long documentId, List<String> chromaIds)`：
    1. 创建 `DeleteTask` 记录（documentId + chromaIds JSON + status=PENDING）
    2. `deleteTaskMapper.insert(task)`
    3. 调用 `chromaVectorStore.deleteByIds(chromaIds)` 物理删除
    4. 成功 → `deleteTaskMapper.updateStatus(taskId, "DONE")`
    5. 失败 → `deleteTaskMapper.incrementRetryCount(taskId)` + `deadLetterQueue.enqueue("DELETE", documentId, chromaIds, error)`
  - **注**：逻辑删除（MySQL 端 deleted=1）由 `DocumentDeleteListener` 在事务提交后完成，此类只负责 ChromaDB 物理删除

---

## 6. OrphanVectorScanner 定时扫描（原 §8.5.1）

- [ ] **6.1** 已包含在第 3 节中，本节为补充说明：`@Scheduled(fixedDelay = 1800000)` 每 30 分钟执行，作为最终兜底清理

---

## 7. DDL — delete_task 表

- [ ] **6.1** 编写 `delete_task` 建表 SQL
  - 字段：`id BIGINT AUTO_INCREMENT PRIMARY KEY`、`document_id BIGINT NOT NULL`、`chroma_ids TEXT`、`status VARCHAR(16) DEFAULT 'PENDING'`、`retry_count INT DEFAULT 0`、`create_time DATETIME`
  - 索引：`idx_status (status)`、`idx_document_id (document_id)`
