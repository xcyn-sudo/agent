# agent-qr-knowledge — P1 任务清单

> 知识库模块：文档上传接口、文件存储、文档解析（PDF/DOCX/TXT）、文本切片（递归字符分割）、状态管理、事件监听器。

---

## 1. 枚举 DocumentStatus

- [ ] **1.1** 创建 `org.example.agent_qr.knowledge.enums.DocumentStatus` 枚举
  - 值：`UPLOADED("已上传")`、`PARSING("解析中")`、`CHUNKING("切片中")`、`EMBEDDING("向量化中")`、`READY("就绪")`、`FAILED("失败")`、`DELETING("删除中")`
  - 每个枚举含 `description` 字段和 getter

---

## 2. 实体类

- [ ] **2.1** 创建 `org.example.agent_qr.knowledge.entity.Document` 类
  - 注解 `@Data`、`@TableName("kb_document")`
  - 字段：`Long id`、`String title`、`String fileName`、`String filePath`、`String fileType`、`Long fileSize`、`String status`、`Long uploadUserId`、`String errorMsg`、`LocalDateTime createTime`、`LocalDateTime updateTime`

- [ ] **2.2** 创建 `org.example.agent_qr.knowledge.entity.Chunk` 类
  - 注解 `@Data`、`@TableName("kb_chunk")`
  - 字段：`Long id`、`Long documentId`、`Integer chunkIndex`、`String content`、`Integer charCount`、`String chromaId`、`LocalDateTime createTime`

---

## 3. Mapper

- [ ] **3.1** 创建 `org.example.agent_qr.knowledge.mapper.DocumentMapper` 接口
  - 注解 `@Mapper`，继承 `BaseMapper<Document>`
  - `int updateStatus(@Param("id") Long id, @Param("status") String status)` — 更新文档状态
  - `int updateErrorMsg(@Param("id") Long id, @Param("errorMsg") String errorMsg)` — 更新错误信息

- [ ] **3.2** 创建 `org.example.agent_qr.knowledge.mapper.ChunkMapper` 接口
  - 注解 `@Mapper`，继承 `BaseMapper<Chunk>`
  - `int deleteByDocumentId(@Param("documentId") Long documentId)` — 按文档 ID 删除所有切片
  - `List<Chunk> selectByDocumentId(@Param("documentId") Long documentId)` — 按文档 ID 查切片列表

---

## 4. 文件存储服务 FileStorageService

- [ ] **4.1** 创建 `org.example.agent_qr.knowledge.service.FileStorageService` 类
  - 注解 `@Service`
  - 读取配置 `${file.upload-dir:./uploads}`
  - `String store(MultipartFile file)`：
    1. 按日期创建子目录 `yyyy/MM`
    2. 生成唯一文件名（时间戳 + 原始文件名）
    3. 写入文件 → 返回相对路径
  - `void delete(String filePath)`：删除指定文件

---

## 5. 文档解析服务

- [ ] **5.1** 创建 `org.example.agent_qr.knowledge.parser.TextParser` 类
  - 注解 `@Component`
  - `String parse(String filePath)`：读取 txt/md 文件内容（UTF-8）

- [ ] **5.2** 创建 `org.example.agent_qr.knowledge.parser.PdfParser` 类（基础版）
  - 注解 `@Component`
  - 使用 Apache PDFBox
  - `String parse(String filePath)`：
    1. `Loader.loadPDF()` 加载文档
    2. `PDFTextStripper` 提取文本（`setSortByPosition(true)`）
    3. 返回纯文本

- [ ] **5.3** 创建 `org.example.agent_qr.knowledge.parser.DocxParser` 类（基础版）
  - 注解 `@Component`
  - 使用 Apache POI
  - `String parse(String filePath)`：
    1. `XWPFDocument` 加载文档
    2. 遍历段落提取文本
    3. 返回纯文本

- [ ] **5.4** 创建 `org.example.agent_qr.knowledge.parser.DocumentParserService` 类
  - 注解 `@Service`
  - 注入 `PdfParser`、`DocxParser`、`TextParser`
  - `String parse(String filePath, String fileType)`：
    - switch fileType → 路由到对应解析器
    - 不支持的类型抛出 `BusinessException`

---

## 6. 文本切片器 TextSplitter

- [ ] **6.1** 创建 `org.example.agent_qr.knowledge.splitter.TextSplitter` 类
  - 注解 `@Service`
  - 读取配置 `${rag.chunk-size:500}`、`${rag.chunk-overlap:50}`
  - `List<String> split(String text)`：
    1. 按 `\n\n` 分段落
    2. 超长段落调用 `splitLongText()` 递归分割
    3. 合并过短片段（< 100 字符）
  - `splitLongText()`：
    - 在 `chunkSize` 末尾附近寻找断点（`。\n！？；， `）
    - 窗口滑动时保留 `chunkOverlap` 重叠
  - `mergeShortChunks()`：合并短片段到前一条

---

## 7. 查询服务 DocumentQueryService

- [ ] **7.1** 创建 `org.example.agent_qr.knowledge.service.DocumentQueryService` 类
  - 注解 `@Service`
  - 注入 `DocumentMapper`、`ChunkMapper`
  - `IPage<Document> listDocuments(int page, int size)` — 分页文档列表
  - `Document getDocument(Long id)` — 查单个文档
  - `DocumentStatus getStatus(Long id)` — 查文档状态
  - `List<Chunk> getChunks(Long documentId)` — 查文档切片

---

## 8. 命令服务 DocumentCommandService

- [ ] **8.1** 创建 `org.example.agent_qr.knowledge.service.DocumentCommandService` 类
  - 注解 `@Service`、`@Slf4j`
  - 注入 `DocumentMapper`、`ChunkMapper`、`FileStorageService`、`ApplicationEventPublisher`
  - 常量：`ALLOWED_TYPES = {"pdf", "docx", "txt", "md"}`、`MAX_FILE_SIZE = 50MB`
  - `Document uploadDocument(MultipartFile file, String title, Long userId)`：
    1. 校验文件类型白名单
    2. 校验文件大小 ≤ 50MB
    3. `fileStorageService.store()` 保存文件
    4. 创建 Document 记录（status=UPLOADED）
    5. 发布 `DocumentUploadedEvent`
    6. 返回 Document
  - `void deleteDocument(Long documentId)`：
    1. 校验文档存在
    2. 删除服务器文件
    3. 删除切片记录
    4. 删除文档记录

---

## 9. 事件监听器

- [ ] **9.1** 创建 `org.example.agent_qr.knowledge.listener.DocumentParseListener` 类
  - 注解 `@Component`、`@Slf4j`
  - 注入 `DocumentMapper`、`DocumentParserService`、`ApplicationEventPublisher`
  - `@EventListener` + `@Async("docProcessExecutor")` 监听 `DocumentUploadedEvent`
  - 处理流程：
    1. 更新状态 → PARSING
    2. `parserService.parse()` 解析文档
    3. 发布 `DocumentParsedEvent`
    4. 失败 → 更新状态 FAILED + 记 errorMsg

- [ ] **9.2** 创建 `org.example.agent_qr.knowledge.listener.ChunkEmbeddingListener` 类
  - 注解 `@Component`、`@Slf4j`
  - 注入 `DocumentMapper`、`ChunkMapper`、`TextSplitter`、`ProviderFactory`（来自 rag 模块）、`ChromaVectorStore`（来自 rag 模块）、`ApplicationEventPublisher`
  - `@EventListener` + `@Async("docProcessExecutor")` 监听 `DocumentParsedEvent`
  - 处理流程：
    1. 更新状态 → CHUNKING
    2. `textSplitter.split()` 切片
    3. 逐片保存到 `kb_chunk` 表
    4. 发布 `ChunksCreatedEvent`
    5. 更新状态 → EMBEDDING
    6. 调用 `EmbeddingProvider.embedBatch()` 向量化
    7. 写入 ChromaDB → 更新 `chroma_id`
    8. 更新状态 → READY
    9. 发布 `EmbeddingCompletedEvent`
    10. 失败 → 更新状态 FAILED + 记 errorMsg

---

## 10. 知识库控制器 KnowledgeController

- [ ] **10.1** 创建 `org.example.agent_qr.knowledge.controller.KnowledgeController` 类
  - 注解 `@RestController`、`@RequestMapping("/api/knowledge")`
  - 注入 `DocumentCommandService`、`DocumentQueryService`
  - `POST /api/knowledge/upload` → `Result<Document>`（`@RequestParam MultipartFile file`、`@RequestParam(required=false) String title`）
  - `GET /api/knowledge/documents` → `Result<IPage<Document>>`（`page`、`size`）
  - `GET /api/knowledge/documents/{id}` → `Result<Document>`
  - `DELETE /api/knowledge/documents/{id}` → `Result<Void>`
  - `GET /api/knowledge/documents/{id}/status` → `Result<String>`
  - `GET /api/knowledge/documents/{id}/chunks` → `Result<List<Chunk>>`

---

## 11. pom.xml 依赖

- [ ] **11.1** 在 `agent-qr-knowledge/pom.xml` 中配置依赖
  - `agent-qr-common`（模块依赖）
  - `agent-qr-rag`（模块依赖 — ProviderFactory、ChromaVectorStore）
  - `pdfbox`（Apache PDFBox）
  - `poi-ooxml`（Apache POI）
  - `spring-boot-starter-web`
