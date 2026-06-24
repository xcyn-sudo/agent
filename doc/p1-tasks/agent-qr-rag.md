# agent-qr-rag — P1 任务清单

> RAG 问答模块：LLM/Embedding Provider 策略接口、DeepSeek 实现、ProviderFactory、ChromaRetriever（向量检索）、ChatQueryService（同步问答）、PromptTemplate、会话与消息实体。

---

## 1. Provider 策略接口

- [ ] **1.1** 创建 `org.example.agent_qr.rag.provider.LLMProvider` 接口
  - 方法：`String generate(List<Message> messages)` — 同步生成
  - 方法：`Flux<String> generateStream(List<Message> messages)` — 流式生成（P1 标记 default 空实现）

- [ ] **1.2** 创建 `org.example.agent_qr.rag.provider.EmbeddingProvider` 接口
  - 方法：`float[] embed(String text)` — 单文本向量化
  - 方法：`List<float[]> embedBatch(List<String> texts)` — 批量向量化

---

## 2. DeepSeek Provider 实现

- [ ] **2.1** 创建 `org.example.agent_qr.rag.provider.deepseek.DeepSeekLLMProvider` 类
  - 注解 `@Component`
  - 实现 `LLMProvider`
  - 从配置读取：`${llm.deepseek.api-key}`、`${llm.deepseek.base-url:https://api.deepseek.com}`、`${llm.deepseek.model:deepseek-chat}`、`${llm.deepseek.temperature:0.7}`、`${llm.deepseek.max-tokens:2048}`
  - `@PostConstruct void init()`：构建 `OpenAiChatModel`
  - `generate()`：调用 `chatModel.chat()` → 返回 `aiMessage().text()`
  - `generateStream()`：调用 `chatModel.chatStream()`（P1 基础实现）

- [ ] **2.2** 创建 `org.example.agent_qr.rag.provider.deepseek.DeepSeekEmbeddingProvider` 类
  - 注解 `@Component`
  - 实现 `EmbeddingProvider`
  - 从配置读取：`${embedding.deepseek.api-key}`、`${embedding.deepseek.base-url:https://api.deepseek.com}`、`${embedding.deepseek.model:deepseek-embedding}`
  - `@PostConstruct void init()`：构建 `OpenAiEmbeddingModel`
  - `embed()`：调用 `embeddingModel.embed()` → 返回 `content().vector()`
  - `embedBatch()`：循环调用 `embed()` 逐条处理（P1 基础版）

---

## 3. ProviderFactory

- [ ] **3.1** 创建 `org.example.agent_qr.rag.provider.ProviderFactory` 类
  - 注解 `@Component`
  - 读取配置：`${llm.provider:deepseek}`、`${embedding.provider:deepseek}`
  - 注入 `DeepSeekLLMProvider`、`DeepSeekEmbeddingProvider`（`@Autowired(required = false)`）
  - `LLMProvider getLLMProvider()`：根据配置返回对应 Provider
  - `EmbeddingProvider getEmbeddingProvider()`：根据配置返回对应 Provider

---

## 4. ChromaRetriever（向量检索器）

- [ ] **4.1** 创建 `org.example.agent_qr.rag.retriever.ChromaRetriever` 类
  - 注解 `@Component`、`@Slf4j`
  - 读取配置 `${langchain4j.chroma.collection-name:enterprise_knowledge}`
  - 注入 `ChromaVectorStore`（LangChain4j Chroma 集成）
  - `List<RetrievedDocument> similaritySearch(float[] queryEmbedding, int topK)` — 向量相似度检索
  - `void deleteByDocumentId(Long documentId)` — 按文档 ID 删除向量

---

## 5. 检索文档模型

- [ ] **5.1** 创建 `org.example.agent_qr.rag.entity.RetrievedDocument` 类
  - 注解 `@Data`
  - 字段：`String documentId`、`String documentTitle`、`String content`、`Double similarity`

---

## 6. PromptTemplate

- [ ] **6.1** 创建 `org.example.agent_qr.rag.prompt.PromptTemplate` 类
  - 注解 `@Component`
  - `String build(String query, String context)`：
    - 模板内容：系统指令 + 参考资料（context）+ 用户问题（query）+ 要求注明来源

---

## 7. 实体类

- [ ] **7.1** 创建 `org.example.agent_qr.rag.entity.Conversation` 类
  - 注解 `@Data`、`@TableName("chat_conversation")`
  - 字段：`Long id`、`Long userId`、`String title`、`Integer messageCount`、`LocalDateTime createTime`、`LocalDateTime updateTime`

- [ ] **7.2** 创建 `org.example.agent_qr.rag.entity.Message` 类
  - 注解 `@Data`、`@TableName("chat_message")`
  - 字段：`Long id`、`Long conversationId`、`String role`（user/assistant）、`String content`、`String sources`（JSON 格式引用来源）、`LocalDateTime createTime`

---

## 8. Mapper

- [ ] **8.1** 创建 `org.example.agent_qr.rag.mapper.ConversationMapper` 接口
  - 注解 `@Mapper`，继承 `BaseMapper<Conversation>`
  - `List<Conversation> selectByUserId(@Param("userId") Long userId)` — 按用户查会话列表
  - `int incrementMessageCount(@Param("id") Long id)` — 消息计数 +1

- [ ] **8.2** 创建 `org.example.agent_qr.rag.mapper.MessageMapper` 接口
  - 注解 `@Mapper`，继承 `BaseMapper<Message>`
  - `List<Message> selectByConversationId(@Param("conversationId") Long conversationId)` — 按会话查消息列表

---

## 9. 会话服务 ConversationService

- [ ] **9.1** 创建 `org.example.agent_qr.rag.service.ConversationService` 类
  - 注解 `@Service`
  - 注入 `ConversationMapper`
  - `Long createConversation(Long userId, String title)` — 创建会话（title 取问题前 30 字）
  - `List<Conversation> listConversations(Long userId)` — 用户会话列表
  - `void incrementMessageCount(Long conversationId)` — 消息计数 +1
  - `void deleteConversation(Long id)` — 删除会话

---

## 10. ChatQueryService（同步问答）

- [ ] **10.1** 创建 `org.example.agent_qr.rag.service.ChatQueryService` 类
  - 注解 `@Service`、`@Slf4j`
  - 注入 `ProviderFactory`、`ChromaRetriever`、`PromptTemplate`、`ConversationService`、`MessageMapper`、`ApplicationEventPublisher`
  - `String ask(String query, Long conversationId, Long userId)`（P1 同步版）：
    1. 会话管理 → 无 `conversationId` 则新建
    2. 保存用户消息（role=user）
    3. `EmbeddingProvider.embed(query)` → 查询向量
    4. `ChromaRetriever.similaritySearch()` → Top-5 检索结果
    5. 检索结果为空 → 返回"知识库中暂无相关信息"
    6. `PromptTemplate.build()` 构建 Prompt
    7. `LLMProvider.generate()` 生成回答
    8. 保存助手消息（role=assistant + sources JSON）
    9. 发布 `AnswerGeneratedEvent`
    10. 返回回答文本

---

## 11. 问答控制器 ChatController

- [ ] **11.1** 创建 `org.example.agent_qr.rag.controller.ChatController` 类
  - 注解 `@RestController`、`@RequestMapping("/api/chat")`
  - 注入 `ChatQueryService`、`ConversationService`、`MessageMapper`
  - `POST /api/chat/ask` → `Result<Map<String,Object>>`（含 `answer`、`conversationId`、`sources`）
  - `GET /api/chat/conversations` → `Result<List<Conversation>>`
  - `GET /api/chat/conversations/{id}/messages` → `Result<List<Message>>`
  - `DELETE /api/chat/conversations/{id}` → `Result<Void>`

---

## 12. pom.xml 依赖

- [ ] **12.1** 在 `agent-qr-rag/pom.xml` 中配置依赖
  - `agent-qr-common`（模块依赖）
  - `langchain4j` + `langchain4j-open-ai`（LLM/Embedding 客户端）
  - `langchain4j-chroma`（ChromaDB 集成）
  - `spring-boot-starter-webflux`（提供 `Flux`）
  - `spring-boot-starter-web`
