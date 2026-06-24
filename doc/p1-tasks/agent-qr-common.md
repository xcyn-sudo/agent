# agent-qr-common — P1 任务清单

> 公共模块：提供 Result 统一响应、BusinessException 业务异常、MyBatis-Plus 配置、Spring 上下文工具、5 个事件模型。

---

## 1. 统一响应类 Result

- [ ] **1.1** 创建 `org.example.agent_qr.common.Result<T>` 类
  - 属性：`Integer code`、`String message`、`T data`、`Long timestamp`
  - 注解：`@Data`、`@NoArgsConstructor`、`@AllArgsConstructor`
  - 静态方法：`success()`（无数据）、`success(T data)`（带数据）、`success(String message, T data)`（自定义消息）、`error(Integer code, String message)`

---

## 2. 业务异常 BusinessException

- [ ] **2.1** 创建 `org.example.agent_qr.common.BusinessException` 类
  - 继承 `RuntimeException`
  - 属性：`Integer code`
  - 构造器：`BusinessException(String message)`（默认 code=400）、`BusinessException(Integer code, String message)`
  - Getter：`getCode()`

---

## 3. MyBatis-Plus 配置

- [ ] **3.1** 创建 `org.example.agent_qr.common.config.MybatisPlusConfig` 类
  - 注解 `@Configuration`
  - 配置分页插件 `MybatisPlusInterceptor`（添加 `PaginationInnerInterceptor`）

---

## 4. Spring 上下文工具

- [ ] **4.1** 创建 `org.example.agent_qr.common.util.SpringContextUtil` 类
  - 实现 `ApplicationContextAware`
  - 静态方法：`getBean(Class<T> clazz)`、`getBean(String name)`

---

## 5. 事件模型（5 个）

- [ ] **5.1** 创建 `org.example.agent_qr.common.event.DocumentUploadedEvent` 类
  - 继承 `ApplicationEvent`
  - 字段：`Long documentId`、`String filePath`、`String fileName`、`String fileType`、`Long userId`

- [ ] **5.2** 创建 `org.example.agent_qr.common.event.DocumentParsedEvent` 类
  - 继承 `ApplicationEvent`
  - 字段：`Long documentId`、`String content`

- [ ] **5.3** 创建 `org.example.agent_qr.common.event.ChunksCreatedEvent` 类
  - 继承 `ApplicationEvent`
  - 字段：`Long documentId`、`List<String> chunks`

- [ ] **5.4** 创建 `org.example.agent_qr.common.event.EmbeddingCompletedEvent` 类
  - 继承 `ApplicationEvent`
  - 字段：`Long documentId`、`Integer chunkCount`

- [ ] **5.5** 创建 `org.example.agent_qr.common.event.AnswerGeneratedEvent` 类
  - 继承 `ApplicationEvent`
  - 字段：`Long userId`、`Long conversationId`

---

## 6. pom.xml 依赖

- [ ] **6.1** 在 `agent-qr-common/pom.xml` 中配置依赖
  - `spring-boot-starter-web`（提供 REST 基础）
  - `mybatis-plus-spring-boot3-starter`（ORM）
  - `lombok`（编译期注解）
