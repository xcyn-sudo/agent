# agent-qr-knowledge — P2 任务清单

> 知识库模块 P2 扩展：软删除 v2、PDF 流式解析+表格 Markdown+OCR、DOCX 表格 Markdown、Document 实体 domain/sensitivity 字段、DLQ 集成。

---

## 1. Document 实体扩展（domain / sensitivity 字段）

- [ ] **1.1** 在 `org.example.agent_qr.knowledge.entity.Document` 中新增字段
  - `private String domain` — 业务域（HR / FINANCE / RD / SALES）
  - `private Integer sensitivityLevel` — 密级等级（0=公开 1=内部 2=机密 3=绝密）
  - `private String sensitivityLabel` — 密级标签（public / internal / confidential / top_secret）
  - 添加对应的 `@TableField` 注解

---

## 2. DocumentCommandService — 事件驱动的软删除 v2

- [ ] **2.1** 新增 `requestDeleteDocument(Long documentId)` 方法，采用事件驱动软删除策略：
    1. 标记 Document 状态为 DELETING（防止并发操作）
    2. 收集切片列表和 ChromaDB 向量 ID：`chunkMapper.selectByDocumentId(documentId)`
    3. 收集 chromaIds：`chunkMapper.selectChromaIdsByDocumentId(documentId)`
    4. 发布 `DocumentDeleteRequestedEvent(documentId, chunkIds, chromaIds, filePath)`
    5. **立即返回**，不等待物理删除完成，不注入任何 compensation 模块的类
    - 原理：`@TransactionalEventListener(phase = AFTER_COMMIT)` 在 compensation 模块中接收事件并执行物理删除

- [ ] **2.2** 改造 `uploadDocument` 方法签名，新增 `domain` 和 `sensitivityLevel` 参数传递
  - `uploadDocument(MultipartFile file, String title, Long userId, String domain, Integer sensitivityLevel)`
  - 将 domain / sensitivityLevel 写入 Document 实体后保存
  - 保留 P1 原有逻辑（文件存储 → 创建记录 → 发布 DocumentUploadedEvent）

---

## 3. PdfParser P2 增强（流式逐页 + 表格 Markdown + OCR）

- [ ] **3.1** 改造 `org.example.agent_qr.knowledge.parser.PdfParser` 类
  - 新增属性：`ocrEnabled`（默认 false）、`maxMemoryMb`（默认 256MB）
  - 新增方法 `parse(String filePath)`：
    1. 文件大小 > maxMemoryMb → 走流式分支 `parseStreaming(filePath)`
    2. 否则：PDFBox 逐页提取文本 + Tika 表格识别 → Markdown 格式
    3. 若 ocrEnabled 且判定为扫描件（平均每页字符 < 50）→ 调用 Tesseract OCR
  - 私有方法 `parseStreaming(String filePath)`：逐页 `stripper.getText()` 释放内存
  - 私有方法 `isScannedPdf(PDDocument document)`：页数 > 0 且平均每页字符 < 50 → true
  - 私有方法 `performOcr(String filePath)`：Tesseract OCR 识别
  - 私有方法 `extractTablesAsMarkdown(String tikaXml)`：Tika XML 输出 → Markdown 表格格式

---

## 4. DocxParser P2 增强（表格 → Markdown）

- [ ] **4.1** 改造 `org.example.agent_qr.knowledge.parser.DocxParser` 类
  - 在 `parse` 方法中将 `XWPFTable` 转为 Markdown 表格格式
  - 私有方法 `tableToMarkdown(XWPFTable table)`：
    1. 遍历行，每行列内容用 `|` 包裹
    2. 表头行后追加分隔行 `| --- | --- | ... |`
    3. 单元格内容中的换行替换为空格

---

## 5. DocumentParseListener P2 增强（DLQ 集成）

- [ ] **5.1** 改造 `org.example.agent_qr.knowledge.listener.DocumentParseListener` 类
  - 新增属性：`DeadLetterQueue deadLetterQueue`
  - 解析失败 catch 块中，调用 `deadLetterQueue.enqueue("PARSE", event.getDocumentId(), JSON.toJSONString(event), e.getMessage())`
  - 保留 P1 原有流程：更新状态 PARSING → 解析 → 发布 DocumentParsedEvent

---

## 6. ChunkEmbeddingListener P2 增强（DLQ + BatchEmbedding 集成）

- [ ] **6.1** 改造 `org.example.agent_qr.knowledge.listener.ChunkEmbeddingListener` 类
  - 新增属性：`DeadLetterQueue deadLetterQueue`、`BatchEmbeddingService batchEmbeddingService`
  - 处理失败 catch 块中，调用 `deadLetterQueue.enqueue("CHUNK", ...)`
  - 向量化步骤改为调用 `BatchEmbeddingService.submit(chunk)` 攒批处理

---

## 7. DocumentMapper P2 扩展

- [ ] **7.1** 在 `org.example.agent_qr.knowledge.mapper.DocumentMapper` 中新增方法
  - `softDelete(Long documentId)`：`UPDATE kb_document SET deleted = 1 WHERE id = #{documentId}`
  - `updateErrorMsg(Long documentId, String errorMsg)`：更新错误信息字段

---

## 8. ChunkMapper P2 扩展

- [ ] **8.1** 在 `org.example.agent_qr.knowledge.mapper.ChunkMapper` 中新增方法
  - `softDeleteByDocumentId(Long documentId)`：`UPDATE kb_chunk SET deleted = 1 WHERE document_id = #{documentId}`
  - `selectChromaIdsByDocumentId(Long documentId)`：查询已删除文档的所有 chroma_id
  - `selectAllReadyChunks()`：查询 status=READY 且 deleted=0 的全量切片（供 BM25 索引构建）
  - `selectReadyChunksPaged(int offset, int limit)`：分页查询就绪切片（供 BM25 增量索引构建）

---

## 9. DDL — kb_document 表扩展

- [ ] **9.1** 编写 ALTER TABLE SQL（追加到 p2-schema.sql）
  - `ALTER TABLE kb_document ADD COLUMN domain VARCHAR(32) DEFAULT NULL COMMENT '业务域 HR/FINANCE/RD/SALES'`
  - `ALTER TABLE kb_document ADD COLUMN sensitivity_level INT DEFAULT 0 COMMENT '密级 0=公开 1=内部 2=机密 3=绝密'`
  - `ALTER TABLE kb_document ADD COLUMN sensitivity_label VARCHAR(16) DEFAULT 'public' COMMENT '密级标签'`
  - `ALTER TABLE kb_document ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记'`
  - `ALTER TABLE kb_document ADD COLUMN error_msg VARCHAR(1000) DEFAULT NULL COMMENT '处理错误信息'`
  - `ALTER TABLE kb_document ADD INDEX idx_domain (domain)`
  - `ALTER TABLE kb_document ADD INDEX idx_deleted (deleted)`

---

## 10. DDL — kb_chunk 表扩展

- [ ] **10.1** 编写 ALTER TABLE SQL（追加到 p2-schema.sql）
  - `ALTER TABLE kb_chunk ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记'`
  - `ALTER TABLE kb_chunk ADD INDEX idx_deleted (deleted)`

---

## 11. pom.xml 依赖

- [ ] **11.1** 在 `agent-qr-knowledge/pom.xml` 中新增依赖
  - `tika-parsers`（PDF 表格识别）
  - `tesseract`（OCR 可选，由 `parser.ocr.enabled` 开关控制）
