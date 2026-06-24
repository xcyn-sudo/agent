# P1 阶段代码生成 Prompt

> 本文档是 P1 阶段代码生成的总控 Prompt，用于驱动 Claude Code Agent 完成全部 P1 模块的代码实现。
>
> 版本：v1.0
>
> 日期：2026-06-17

---

## 一、你的角色

你是一个**主控 Agent（Orchestrator）**，负责：

1. **理解 P1 阶段全部需求**：阅读本文档的全部内容，确保对 P1 阶段的模块划分、依赖关系、开发顺序有完整理解。
2. **跟踪整体进度**：维护一个进度清单，记录每个模块 / 每个类的完成状态。
3. **逐个模块推进**：按照开发顺序，为每个模块创建一个**子 Agent** 来生成该模块的全部代码。
4. **验证完整性**：每个模块完成后，检查子 Agent 的输出是否覆盖了该模块的全部任务项。

---

## 二、项目概览

### 2.1 项目信息

| 项目 | 说明 |
|------|------|
| **项目名称** | 基于 LangChain 的 RAG 企业内部知识库问答 Agent 系统 |
| **基础包名** | `org.example.agent_qr` |
| **Java 版本** | 21 |
| **Spring Boot 版本** | 3.5.15 |
| **构建工具** | Maven（多模块） |
| **ORM** | MyBatis-Plus 3.x |
| **数据库** | MySQL 8.0（端口 3308） |
| **向量数据库** | ChromaDB 0.5.x（端口 8000） |
| **LLM/Embedding** | DeepSeek API（P1 默认） |
| **前端** | 服务端端口 9090 |

### 2.2 P1 阶段目标

实现 **MVP（最小可行产品）**，包含以下核心能力：

1. ✅ 用户认证（JWT 登录/注册）
2. ✅ 用户管理（CRUD）
3. ✅ 文档上传与异步处理（PDF/DOCX/TXT 解析 → 切片 → 向量化）
4. ✅ 基于 RAG 的同步问答（语义检索 + LLM 生成）
5. ✅ Dashboard 统计
6. ✅ 全局异常处理 + 统一响应 + 跨域 + 安全配置

### 2.3 P1 模块列表

| 序号 | 模块 | 包路径 | 说明 |
|------|------|--------|------|
| 1 | agent-qr-web | `org.example.agent_qr` + `org.example.agent_qr.web` | 启动模块（入口 + 全局配置） |
| 2 | agent-qr-common | `org.example.agent_qr.common` | 公共模块（Result、异常、事件、配置） |
| 3 | agent-qr-user | `org.example.agent_qr.user` | 用户管理模块 |
| 4 | agent-qr-auth | `org.example.agent_qr.auth` | 认证授权模块 |
| 5 | agent-qr-rag | `org.example.agent_qr.rag` | RAG 问答模块 |
| 6 | agent-qr-knowledge | `org.example.agent_qr.knowledge` | 知识库模块 |
| 7 | agent-qr-statistics | `org.example.agent_qr.statistics` | 统计模块 |

### 2.4 模块依赖关系图

```
                    ┌─────────────────┐
                    │  agent-qr-web   │  ← 启动类、全局配置、配置文件
                    └───────┬─────────┘
                            │ 依赖所有业务模块
        ┌───────────────────┼───────────────────────────┐
        │                   │                           │
  ┌─────┴─────┐      ┌──────┴──────┐            ┌──────┴──────┐
  │   auth    │      │  knowledge   │            │ statistics  │
  └─────┬─────┘      └──┬─────┬────┘            └──┬─────┬────┘
        │               │     │                    │     │
        │          ┌────┘     └────┐          ┌────┘     │
        │          ▼              ▼          ▼          │
        │    ┌──────────┐  ┌──────────┐  ┌──────────┐  │
        │    │   rag    │  │  common  │  │   user   │  │
        │    └──────────┘  └──────────┘  └──────────┘  │
        │         │              ▲              ▲       │
        └─────────┼──────────────┘              │       │
                  └─────────────────────────────┘       │
                  ┌─────────────────────────────────────┘
                  │
            ┌─────┴─────┐
            │   user    │  ← auth / knowledge / statistics 都依赖
            └───────────┘
```

**关键依赖规则**：
- `common` 被所有模块依赖，不依赖任何业务模块
- `user` 依赖 `common`，被 `auth`、`knowledge`、`statistics` 依赖
- `auth` 依赖 `common` + `user`
- `rag` 依赖 `common`（不依赖 `knowledge`，通过 ChromaDB 解耦）
- `knowledge` 依赖 `common` + `rag`
- `statistics` 依赖 `common` + `knowledge` + `user`
- `web` 依赖所有业务模块

---

## 三、开发顺序（严格执行）

```
第 0 步：基础设施修复
  ├── 0.1 将 gent-qr-web 目录重命名为 agent-qr-web
  ├── 0.2 更新根 pom.xml（新增 user/rag 模块、修正 web 模块名）
  └── 0.3 更新 agent-qr-web/pom.xml（修正 artifactId + 依赖全部业务模块）
          ↓
第 1 步：agent-qr-common（所有模块都依赖它，需最先完成）
          ↓
第 2 步：agent-qr-user（被 auth 模块依赖）
          ↓
第 3 步：agent-qr-auth（依赖 user 模块）
          ↓
第 4 步：agent-qr-rag（Provider 策略接口 + DeepSeek 实现）
          ↓
第 5 步：agent-qr-knowledge（依赖 common + rag）
          ↓
第 6 步：agent-qr-statistics（依赖 common + knowledge + user）
          ↓
第 7 步：agent-qr-web（启动类 + 全局配置 + 配置文件）
```

---

## 四、主控 Agent 工作流程

### 4.1 初始化阶段

1. **读取本文档全部内容**，确认理解所有模块需求。
2. **检查现有代码结构**：
   - 确认 `gent-qr-web` → `agent-qr-web` 重命名是否已完成
   - 确认根 `pom.xml` 中 `<modules>` 是否已包含全部 7 个模块
   - 检查各模块的 `pom.xml` 依赖是否已配置
3. **创建进度跟踪清单**（见下方模板），记录所有类的完成状态。

### 4.2 逐模块推进

对每个模块（按第 3 节的顺序）：

1. **创建子 Agent**：使用 Claude Code 的 Agent 工具，为当前模块创建一个子 Agent。
2. **子 Agent Prompt**：将本文档中对应模块的完整规格传递给子 Agent，包括：
   - 模块包结构
   - 每个类的完整定义（字段、方法、注解、依赖）
   - pom.xml 依赖配置
   - 与其他模块的依赖关系
3. **验证输出**：子 Agent 完成后，检查：
   - 所有 Java 文件是否已创建在正确的包路径下
   - 每个类的方法签名、注解、依赖注入是否正确
   - pom.xml 依赖是否完整
   - 代码是否能通过编译（如可行，执行 `mvn compile -pl <module>`）
4. **更新进度**：将完成状态记录到进度清单。

### 4.3 进度清单模板

在每个模块开始前，输出以下格式的进度表：

```markdown
## P1 进度追踪

| # | 模块 | 状态 | 完成/总计 | 备注 |
|---|------|------|-----------|------|
| 0 | 基础设施修复 | ⬜/🔄/✅ | — | gent-qr-web重命名 + pom更新 |
| 1 | agent-qr-common | ⬜/🔄/✅ | 0/6 | Result/BusinessException/MybatisPlusConfig/SpringContextUtil/5Event |
| 2 | agent-qr-user | ⬜/🔄/✅ | 0/6 | SysUser/SysUserMapper/MyMetaObjectHandler/DTO/AdminController |
| 3 | agent-qr-auth | ⬜/🔄/✅ | 0/7 | AuthController/AuthService/PasswordUtil/JwtUtil/JwtAuthFilter |
| 4 | agent-qr-rag | ⬜/🔄/✅ | 0/12 | LLMProvider/EmbeddingProvider/DeepSeek实现/ChromaRetriever/ChatQueryService/PromptTemplate |
| 5 | agent-qr-knowledge | ⬜/🔄/✅ | 0/11 | KnowledgeController/DocumentCommandService/FileStorage/Parsers/TextSplitter/Listeners |
| 6 | agent-qr-statistics | ⬜/🔄/✅ | 0/7 | DailyStats/StatisticsQueryService/StatisticsController/StatisticsUpdateListener |
| 7 | agent-qr-web | ⬜/🔄/✅ | 0/10 | 启动类/GlobalExceptionHandler/CorsConfig/SecurityConfig/AsyncConfig/application.yml |
```

**状态图例**：⬜ 未开始 → 🔄 进行中 → ✅ 已完成

---

## 五、第 0 步：基础设施修复

> **注意**：当前项目存在以下问题需在生成业务代码前修复。

### 5.1 模块重命名

- 将 `D:\Javacode\agent-qr\gent-qr-web` 目录重命名为 `D:\Javacode\agent-qr\agent-qr-web`
- 将 `agent-qr-web/pom.xml` 中的 `<artifactId>gent-qr-web</artifactId>` 改为 `<artifactId>agent-qr-web</artifactId>`

### 5.2 根 pom.xml 修改

在根 `pom.xml` 的 `<modules>` 中：
- 新增 `<module>agent-qr-user</module>`
- 新增 `<module>agent-qr-rag</module>`
- 将 `<module>gent-qr-web</module>` 改为 `<module>agent-qr-web</module>`

修改后的 `<modules>` 应为：

```xml
<modules>
    <module>agent-qr-common</module>
    <module>agent-qr-auth</module>
    <module>agent-qr-user</module>
    <module>agent-qr-rag</module>
    <module>agent-qr-knowledge</module>
    <module>agent-qr-statistics</module>
    <module>agent-qr-web</module>
</modules>
```

### 5.3 agent-qr-web/pom.xml 依赖配置

`agent-qr-web/pom.xml` 需依赖所有业务模块：

```xml
<dependencies>
    <dependency><groupId>org.example</groupId><artifactId>agent-qr-common</artifactId><version>0.0.1-SNAPSHOT</version></dependency>
    <dependency><groupId>org.example</groupId><artifactId>agent-qr-auth</artifactId><version>0.0.1-SNAPSHOT</version></dependency>
    <dependency><groupId>org.example</groupId><artifactId>agent-qr-user</artifactId><version>0.0.1-SNAPSHOT</version></dependency>
    <dependency><groupId>org.example</groupId><artifactId>agent-qr-knowledge</artifactId><version>0.0.1-SNAPSHOT</version></dependency>
    <dependency><groupId>org.example</groupId><artifactId>agent-qr-rag</artifactId><version>0.0.1-SNAPSHOT</version></dependency>
    <dependency><groupId>org.example</groupId><artifactId>agent-qr-statistics</artifactId><version>0.0.1-SNAPSHOT</version></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
</dependencies>
```

---

## 六、第 1 步：agent-qr-common 模块完整规格

### 6.1 模块信息

| 属性 | 值 |
|------|-----|
| **包路径** | `org.example.agent_qr.common` |
| **依赖** | `spring-boot-starter-web`、`mybatis-plus-spring-boot3-starter`、`lombok` |

### 6.2 pom.xml 依赖

```xml
<dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>com.baomidou</groupId><artifactId>mybatis-plus-spring-boot3-starter</artifactId></dependency>
    <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
</dependencies>
```

### 6.3 包结构

```
org.example.agent_qr.common
├── Result.java
├── BusinessException.java
├── config/
│   └── MybatisPlusConfig.java
├── util/
│   └── SpringContextUtil.java
└── event/
    ├── DocumentUploadedEvent.java
    ├── DocumentParsedEvent.java
    ├── ChunksCreatedEvent.java
    ├── EmbeddingCompletedEvent.java
    └── AnswerGeneratedEvent.java
```

### 6.4 类详细规格

---

#### 6.4.1 `Result<T>` — 统一响应类

**路径**：`org.example.agent_qr.common.Result`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    // 静态工厂方法：
    public static <T> Result<T> success()                    // code=200, message="操作成功", data=null
    public static <T> Result<T> success(T data)              // code=200, message="操作成功"
    public static <T> Result<T> success(String message, T data)  // code=200
    public static <T> Result<T> error(Integer code, String message)  // data=null
    // 所有方法均设置 timestamp = System.currentTimeMillis()
}
```

---

#### 6.4.2 `BusinessException` — 业务异常类

**路径**：`org.example.agent_qr.common.BusinessException`

```java
public class BusinessException extends RuntimeException {
    private Integer code;

    public BusinessException(String message)          // 默认 code=400
    public BusinessException(Integer code, String message)
    public Integer getCode()
}
```

---

#### 6.4.3 `MybatisPlusConfig` — MyBatis-Plus 配置

**路径**：`org.example.agent_qr.common.config.MybatisPlusConfig`

```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 创建 MybatisPlusInterceptor，添加 PaginationInnerInterceptor(DbType.MYSQL)
    }
}
```

---

#### 6.4.4 `SpringContextUtil` — Spring 上下文工具

**路径**：`org.example.agent_qr.common.util.SpringContextUtil`

```java
@Component
public class SpringContextUtil implements ApplicationContextAware {
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtil.context = applicationContext;
    }

    public static <T> T getBean(Class<T> clazz)
    public static Object getBean(String name)
}
```

---

#### 6.4.5 事件模型（5 个）

所有事件类继承 `ApplicationEvent`，使用 `@Getter` `@Setter` 注解，放在 `org.example.agent_qr.common.event` 包下。

| 事件类 | 字段 |
|--------|------|
| `DocumentUploadedEvent` | `Long documentId`、`String filePath`、`String fileName`、`String fileType`、`Long userId` |
| `DocumentParsedEvent` | `Long documentId`、`String content` |
| `ChunksCreatedEvent` | `Long documentId`、`List<String> chunks` |
| `EmbeddingCompletedEvent` | `Long documentId`、`Integer chunkCount` |
| `AnswerGeneratedEvent` | `Long userId`、`Long conversationId` |

**注意**：每个事件类的构造器需调用 `super(source)`，source 通常传 `this` 或实际的事件源对象。事件类不需要 `@Component` 注解——它们通过 `new` 创建并发布。

**典型实现示例**（以 DocumentUploadedEvent 为例）：

```java
@Getter
@Setter
public class DocumentUploadedEvent extends ApplicationEvent {
    private Long documentId;
    private String filePath;
    private String fileName;
    private String fileType;
    private Long userId;

    public DocumentUploadedEvent(Object source, Long documentId, String filePath,
                                  String fileName, String fileType, Long userId) {
        super(source);
        this.documentId = documentId;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileType = fileType;
        this.userId = userId;
    }
}
```

---

## 七、第 2 步：agent-qr-user 模块完整规格

### 7.1 模块信息

| 属性 | 值 |
|------|-----|
| **包路径** | `org.example.agent_qr.user` |
| **依赖** | `agent-qr-common`、`mybatis-plus-spring-boot3-starter`、`mysql-connector-j` |

### 7.2 pom.xml 依赖

```xml
<dependencies>
    <dependency><groupId>org.example</groupId><artifactId>agent-qr-common</artifactId><version>0.0.1-SNAPSHOT</version></dependency>
    <dependency><groupId>com.baomidou</groupId><artifactId>mybatis-plus-spring-boot3-starter</artifactId></dependency>
    <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId></dependency>
</dependencies>
```

### 7.3 包结构

```
org.example.agent_qr.user
├── entity/
│   └── SysUser.java
├── mapper/
│   └── SysUserMapper.java
├── handler/
│   └── MyMetaObjectHandler.java
├── dto/
│   ├── CreateUserDTO.java
│   └── UpdateUserDTO.java
└── controller/
    └── AdminController.java
```

### 7.4 类详细规格

---

#### 7.4.1 `SysUser` — 用户实体（P1 基础字段）

**路径**：`org.example.agent_qr.user.entity.SysUser`

```java
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;       // 用户名
    private String password;       // BCrypt 密文
    private String realName;       // 真实姓名
    private String email;          // 邮箱
    private String phone;          // 手机号
    private String role;           // admin / user
    private Integer status;        // 1-启用, 0-禁用
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

**注意**：
- P1 阶段不包含 ABAC 字段（department、clearanceLevel、allowedDomains、title），这些是 P2 的。
- 不要添加 `@TableLogic`（逻辑删除），P1 使用物理删除。

---

#### 7.4.2 `SysUserMapper` — 用户数据访问层

**路径**：`org.example.agent_qr.user.mapper.SysUserMapper`

```java
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser selectByUsername(@Param("username") String username);

    @Select("<script>" +
            "SELECT * FROM sys_user " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (username LIKE CONCAT('%',#{keyword},'%') " +
            "    OR real_name LIKE CONCAT('%',#{keyword},'%'))" +
            "  </if>" +
            "</where>" +
            "ORDER BY create_time DESC" +
            "</script>")
    IPage<SysUser> selectPage(Page<SysUser> page, @Param("keyword") String keyword);

    // 额外方法（statistics 模块需要）：
    @Select("SELECT COUNT(*) FROM sys_user WHERE DATE(create_time) = #{date}")
    Long countByDate(@Param("date") LocalDate date);
}
```

---

#### 7.4.3 `MyMetaObjectHandler` — 自动填充时间

**路径**：`org.example.agent_qr.user.handler.MyMetaObjectHandler`

```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

---

#### 7.4.4 DTO 类

**CreateUserDTO**（路径：`org.example.agent_qr.user.dto.CreateUserDTO`）：

```java
@Data
public class CreateUserDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    private String realName;
    private String email;
    private String phone;
    private String role;  // 默认 "user"
}
```

**UpdateUserDTO**（路径：`org.example.agent_qr.user.dto.UpdateUserDTO`）：

```java
@Data
public class UpdateUserDTO {
    private String realName;
    private String email;
    private String phone;
    private String role;
}
```

---

#### 7.4.5 `AdminController` — 用户管理控制器

**路径**：`org.example.agent_qr.user.controller.AdminController`

```java
@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminController {
    @Autowired
    private SysUserMapper sysUserMapper;
    // 注意：P1 直接在 Controller 中使用 Mapper 进行简单 CRUD 即可

    // GET /api/admin/users?page=1&size=10&keyword=
    public Result<IPage<SysUser>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(sysUserMapper.selectPage(new Page<>(page, size), keyword));
    }

    // POST /api/admin/users
    public Result<Void> createUser(@Valid @RequestBody CreateUserDTO dto) {
        // 1. 检查用户名唯一性
        // 2. 使用 PasswordUtil.encode() 加密密码（依赖 auth 模块，或者在此处直接使用 BCryptPasswordEncoder）
        // 3. 创建 SysUser（role 默认 "user"，status 默认 1）
        // 4. 保存
    }

    // PUT /api/admin/users/{id}
    public Result<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserDTO dto) {
        // 1. 查询用户是否存在
        // 2. 更新非空字段
        // 3. sysUserMapper.updateById()
    }

    // PUT /api/admin/users/{id}/status?status=1
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        // 1. 查询用户
        // 2. 设置 status
        // 3. sysUserMapper.updateById()
    }
}
```

**重要说明**：AdminController 中的 `createUser` 需要密码加密。P1 阶段可以有两种处理方式：
- **方式 A（推荐）**：直接在此处使用 `new BCryptPasswordEncoder(12).encode(dto.getPassword())`
- **方式 B**：依赖 `agent-qr-auth` 模块中的 `PasswordUtil`

由于 user 模块先于 auth 模块开发，且 auth 依赖 user，为避免循环依赖问题，**P1 中 AdminController 创建用户时直接使用 BCryptPasswordEncoder 加密密码**。等 auth 模块完成后再统一通过 AuthService 注册用户。

---

## 八、第 3 步：agent-qr-auth 模块完整规格

### 8.1 模块信息

| 属性 | 值 |
|------|-----|
| **包路径** | `org.example.agent_qr.auth` |
| **依赖** | `agent-qr-common`、`agent-qr-user`、`spring-boot-starter-security`、`jjwt` |

### 8.2 pom.xml 依赖

```xml
<dependencies>
    <dependency><groupId>org.example</groupId><artifactId>agent-qr-common</artifactId><version>0.0.1-SNAPSHOT</version></dependency>
    <dependency><groupId>org.example</groupId><artifactId>agent-qr-user</artifactId><version>0.0.1-SNAPSHOT</version></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <!-- JWT -->
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>0.11.5</version></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>0.11.5</version><scope>runtime</scope></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>0.11.5</version><scope>runtime</scope></dependency>
</dependencies>
```

### 8.3 包结构

```
org.example.agent_qr.auth
├── dto/
│   ├── LoginDTO.java
│   ├── RegisterDTO.java
│   └── LoginVO.java
├── util/
│   ├── PasswordUtil.java
│   └── JwtUtil.java
├── filter/
│   └── JwtAuthenticationFilter.java
├── service/
│   ├── AuthService.java
│   └── impl/
│       └── AuthServiceImpl.java
└── controller/
    └── AuthController.java
```

### 8.4 类详细规格

---

#### 8.4.1 DTO / VO

**LoginDTO**（路径：`org.example.agent_qr.auth.dto.LoginDTO`）：

```java
@Data
public class LoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
}
```

**RegisterDTO**（路径：`org.example.agent_qr.auth.dto.RegisterDTO`）：

```java
@Data
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    private String realName;
    private String email;
    private String phone;
}
```

**LoginVO**（路径：`org.example.agent_qr.auth.dto.LoginVO`）：

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginVO {
    private String token;
    private Long userId;
    private String username;
    private String role;
}
```

---

#### 8.4.2 `PasswordUtil` — BCrypt 密码工具

**路径**：`org.example.agent_qr.auth.util.PasswordUtil`

```java
@Component
public class PasswordUtil {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public String encode(String rawPassword)       // BCrypt 加密
    public boolean matches(String rawPassword, String encodedPassword)  // 验证匹配
}
```

---

#### 8.4.3 `JwtUtil` — JWT 令牌工具（P1 基础版）

**路径**：`org.example.agent_qr.auth.util.JwtUtil`

```java
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;  // 默认 24 小时

    // 生成 JWT（sub=username, claims: userId + role）
    public String generateToken(Long userId, String username, String role)

    // 从 Token 解析用户名
    public String getUsernameFromToken(String token)

    // 验证 Token 是否有效（解析成功即为有效，异常返回 false）
    public boolean validateToken(String token)

    // 私有方法：解析 Claims
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }
}
```

**Token 结构**：
- `sub`：username
- `userId`：Long
- `role`：String
- `iat`：签发时间
- `exp`：过期时间 = now + expiration

---

#### 8.4.4 `JwtAuthenticationFilter` — JWT 认证过滤器

**路径**：`org.example.agent_qr.auth.filter.JwtAuthenticationFilter`

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 从 Authorization 头提取 Bearer Token
        String token = extractToken(request);
        if (token == null) { filterChain.doFilter(request, response); return; }

        // 2. 验证 Token
        if (!jwtUtil.validateToken(token)) { filterChain.doFilter(request, response); return; }

        // 3. 解析用户名 → 查 sys_user
        String username = jwtUtil.getUsernameFromToken(token);
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) { filterChain.doFilter(request, response); return; }

        // 4. 构建认证对象注入 SecurityContext
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

**注意**：如果 Token 无效或用户不存在，**不要抛出异常**，直接 `filterChain.doFilter()` 放行，让后续的 Security 配置决定是否拒绝（避免影响 `/api/auth/login` 等公开端点）。

---

#### 8.4.5 `AuthService` 接口 + `AuthServiceImpl`

**AuthService**（路径：`org.example.agent_qr.auth.service.AuthService`）：

```java
public interface AuthService {
    LoginVO login(LoginDTO dto);
    void register(RegisterDTO dto);
    SysUser getCurrentUser();
}
```

**AuthServiceImpl**（路径：`org.example.agent_qr.auth.service.impl.AuthServiceImpl`）：

```java
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private PasswordUtil passwordUtil;
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 查用户 by username
        SysUser user = sysUserMapper.selectByUsername(dto.getUsername());
        // 2. 用户不存在 → throw BusinessException("用户名或密码错误")
        // 3. user.status == 0 → throw BusinessException("账号已被禁用")
        // 4. 验证密码：passwordUtil.matches(dto.getPassword(), user.getPassword())
        // 5. 不匹配 → throw BusinessException("用户名或密码错误")
        // 6. 生成 JWT → 返回 LoginVO(token, userId, username, role)
    }

    @Override
    public void register(RegisterDTO dto) {
        // 1. 检查用户名唯一性（sysUserMapper.selectByUsername）
        //    已存在 → throw BusinessException("用户名已存在")
        // 2. 构建 SysUser 对象：
        //    - password = passwordUtil.encode(dto.getPassword())
        //    - role = "user"（默认）
        //    - status = 1
        // 3. sysUserMapper.insert(user)
    }

    @Override
    public SysUser getCurrentUser() {
        // 从 SecurityContextHolder.getContext().getAuthentication() 获取
        // 返回 (SysUser) authentication.getPrincipal()
    }
}
```

**安全提示**：登录失败时统一返回"用户名或密码错误"，不要区分是"用户不存在"还是"密码错误"，防止用户名枚举攻击。`BusinessException` 的 message 可以包含内部调试信息，但对外暴露的信息应一致。

---

#### 8.4.6 `AuthController` — 认证控制器

**路径**：`org.example.agent_qr.auth.controller.AuthController`

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto)

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto)

    @GetMapping("/info")
    public Result<SysUser> getUserInfo()  // 返回当前登录用户信息
}
```

---

## 九、第 4 步：agent-qr-rag 模块完整规格

### 9.1 模块信息

| 属性 | 值 |
|------|-----|
| **包路径** | `org.example.agent_qr.rag` |
| **依赖** | `agent-qr-common`、`langchain4j`、`langchain4j-open-ai`、`langchain4j-chroma`、`spring-boot-starter-webflux` |

### 9.2 pom.xml 依赖

```xml
<dependencies>
    <dependency><groupId>org.example</groupId><artifactId>agent-qr-common</artifactId><version>0.0.1-SNAPSHOT</version></dependency>
    <dependency><groupId>dev.langchain4j</groupId><artifactId>langchain4j</artifactId><version>1.4.0</version></dependency>
    <dependency><groupId>dev.langchain4j</groupId><artifactId>langchain4j-open-ai</artifactId><version>1.4.0</version></dependency>
    <dependency><groupId>dev.langchain4j</groupId><artifactId>langchain4j-chroma</artifactId><version>1.4.0</version></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-webflux</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
</dependencies>
```

### 9.3 包结构

```
org.example.agent_qr.rag
├── provider/
│   ├── LLMProvider.java
│   ├── EmbeddingProvider.java
│   ├── ProviderFactory.java
│   └── deepseek/
│       ├── DeepSeekLLMProvider.java
│       └── DeepSeekEmbeddingProvider.java
├── retriever/
│   └── ChromaRetriever.java
├── entity/
│   ├── RetrievedDocument.java
│   ├── Conversation.java
│   └── Message.java
├── mapper/
│   ├── ConversationMapper.java
│   └── MessageMapper.java
├── prompt/
│   └── PromptTemplate.java
├── service/
│   ├── ConversationService.java
│   └── ChatQueryService.java
└── controller/
    └── ChatController.java
```

### 9.4 类详细规格

---

#### 9.4.1 Provider 策略接口

**LLMProvider**（路径：`org.example.agent_qr.rag.provider.LLMProvider`）：

```java
public interface LLMProvider {
    /**
     * 同步生成回答
     * @param messages LangChain4j 的 ChatMessage 列表（SystemMessage + UserMessage）
     * @return LLM 生成的回答文本
     */
    String generate(List<dev.langchain4j.data.message.ChatMessage> messages);

    /**
     * 流式生成回答（P1 提供默认空实现——返回 Flux.empty()）
     */
    default Flux<String> generateStream(List<dev.langchain4j.data.message.ChatMessage> messages) {
        return Flux.empty();
    }
}
```

**EmbeddingProvider**（路径：`org.example.agent_qr.rag.provider.EmbeddingProvider`）：

```java
public interface EmbeddingProvider {
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
}
```

---

#### 9.4.2 DeepSeek Provider 实现

**DeepSeekLLMProvider**（路径：`org.example.agent_qr.rag.provider.deepseek.DeepSeekLLMProvider`）：

```java
@Component
@Slf4j
public class DeepSeekLLMProvider implements LLMProvider {
    @Value("${llm.deepseek.api-key}")
    private String apiKey;
    @Value("${llm.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;
    @Value("${llm.deepseek.model:deepseek-chat}")
    private String model;
    @Value("${llm.deepseek.temperature:0.7}")
    private Double temperature;
    @Value("${llm.deepseek.max-tokens:2048}")
    private Integer maxTokens;

    private OpenAiChatModel chatModel;

    @PostConstruct
    public void init() {
        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }

    @Override
    public String generate(List<ChatMessage> messages) {
        // 调用 chatModel.chat(messages) → 返回 aiMessage().text()
        // 异常时抛出 BusinessException("AI 服务暂时不可用")
    }
}
```

**DeepSeekEmbeddingProvider**（路径：`org.example.agent_qr.rag.provider.deepseek.DeepSeekEmbeddingProvider`）：

```java
@Component
@Slf4j
public class DeepSeekEmbeddingProvider implements EmbeddingProvider {
    @Value("${embedding.deepseek.api-key}")
    private String apiKey;
    @Value("${embedding.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;
    @Value("${embedding.deepseek.model:deepseek-embedding}")
    private String model;

    private OpenAiEmbeddingModel embeddingModel;

    @PostConstruct
    public void init() {
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .build();
    }

    @Override
    public float[] embed(String text) {
        // embeddingModel.embed(text).content().vector()
        // 异常时抛出 BusinessException("向量化服务暂时不可用")
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        // P1 基础版：循环调用 embed() 逐条处理
    }
}
```

---

#### 9.4.3 `ProviderFactory` — 策略工厂

**路径**：`org.example.agent_qr.rag.provider.ProviderFactory`

```java
@Component
public class ProviderFactory {
    @Value("${llm.provider:deepseek}")
    private String llmProviderType;
    @Value("${embedding.provider:deepseek}")
    private String embeddingProviderType;

    @Autowired(required = false)
    private DeepSeekLLMProvider deepSeekLLMProvider;
    @Autowired(required = false)
    private DeepSeekEmbeddingProvider deepSeekEmbeddingProvider;

    public LLMProvider getLLMProvider() {
        // switch llmProviderType:
        //   default (deepseek) → deepSeekLLMProvider
        //   null 检查 → throw BusinessException
    }

    public EmbeddingProvider getEmbeddingProvider() {
        // switch embeddingProviderType:
        //   default (deepseek) → deepSeekEmbeddingProvider
        //   null 检查 → throw BusinessException
    }
}
```

---

#### 9.4.4 `ChromaRetriever` — 语义向量检索器

**路径**：`org.example.agent_qr.rag.retriever.ChromaRetriever`

```java
@Component
@Slf4j
public class ChromaRetriever {
    @Value("${langchain4j.chroma.collection-name:enterprise_knowledge}")
    private String collectionName;

    @Autowired
    private ChromaVectorStore chromaVectorStore;  // 需要在 agent-qr-web 中配置 ChromaVectorStore Bean

    /**
     * 向量相似度检索
     * @param queryEmbedding 查询向量
     * @param topK 返回数量（默认 5）
     * @return 检索结果列表
     */
    public List<RetrievedDocument> similaritySearch(float[] queryEmbedding, int topK) {
        // 1. 将 float[] 转为 LangChain4j 的 Embedding 对象
        // 2. 调用 chromaVectorStore.search(embedding, topK, minScore)
        // 3. 将结果映射为 List<RetrievedDocument>
    }

    /**
     * 按文档 ID 删除 ChromaDB 中的向量
     */
    public void deleteByDocumentId(Long documentId) {
        // 调用 chromaVectorStore 的删除方法
    }
}
```

**重要**：`ChromaVectorStore` Bean 需要在 `agent-qr-web` 模块中配置创建。如果 chromaVectorStore 注入失败，设置 `@Autowired(required = false)` 并使用 `@PostConstruct` 检查。

---

#### 9.4.5 `RetrievedDocument` — 检索文档模型

**路径**：`org.example.agent_qr.rag.entity.RetrievedDocument`

```java
@Data
public class RetrievedDocument {
    private String documentId;      // 文档 ID（字符串）
    private String documentTitle;   // 文档标题
    private String content;         // 切片内容
    private Double similarity;      // 相似度分数
}
```

---

#### 9.4.6 `PromptTemplate` — Prompt 模板

**路径**：`org.example.agent_qr.rag.prompt.PromptTemplate`

```java
@Component
public class PromptTemplate {
    public String build(String query, String context) {
        return String.format("""
            你是一个企业知识库助手，请根据以下参考资料回答用户问题。
            如果参考资料中没有直接相关信息，请如实告知用户"知识库中暂无相关记录"。

            参考资料：
            %s

            用户问题：%s

            请用专业、准确的语言回答，并在答案末尾注明引用的来源文档：
            """, context, query);
    }
}
```

---

#### 9.4.7 实体类 + Mapper

**Conversation**（路径：`org.example.agent_qr.rag.entity.Conversation`）：

```java
@Data
@TableName("chat_conversation")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;          // 会话标题（取第一个问题的前 30 字）
    private Integer messageCount;  // 默认 0
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

**Message**（路径：`org.example.agent_qr.rag.entity.Message`）：

```java
@Data
@TableName("chat_message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private String role;           // "user" 或 "assistant"
    private String content;        // 消息文本内容
    private String sources;        // JSON 格式的引用来源列表
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

**ConversationMapper**（路径：`org.example.agent_qr.rag.mapper.ConversationMapper`）：

```java
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
    @Select("SELECT * FROM chat_conversation WHERE user_id = #{userId} ORDER BY update_time DESC")
    List<Conversation> selectByUserId(@Param("userId") Long userId);

    @Update("UPDATE chat_conversation SET message_count = message_count + 1 WHERE id = #{id}")
    int incrementMessageCount(@Param("id") Long id);
}
```

**MessageMapper**（路径：`org.example.agent_qr.rag.mapper.MessageMapper`）：

```java
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    @Select("SELECT * FROM chat_message WHERE conversation_id = #{conversationId} ORDER BY create_time ASC")
    List<Message> selectByConversationId(@Param("conversationId") Long conversationId);
}
```

---

#### 9.4.8 `ConversationService` — 会话服务

**路径**：`org.example.agent_qr.rag.service.ConversationService`

```java
@Service
public class ConversationService {
    @Autowired
    private ConversationMapper conversationMapper;

    // 创建会话：title = query 前 30 字
    public Long createConversation(Long userId, String title)

    // 用户会话列表
    public List<Conversation> listConversations(Long userId)

    // 消息计数 +1
    public void incrementMessageCount(Long conversationId)

    // 删除会话
    public void deleteConversation(Long id)
}
```

---

#### 9.4.9 `ChatQueryService` — P1 同步问答核心

**路径**：`org.example.agent_qr.rag.service.ChatQueryService`

```java
@Service
@Slf4j
public class ChatQueryService {
    @Autowired
    private ProviderFactory providerFactory;
    @Autowired
    private ChromaRetriever chromaRetriever;
    @Autowired
    private PromptTemplate promptTemplate;
    @Autowired
    private ConversationService conversationService;
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 同步问答（P1 版本）
     * @param query 用户问题
     * @param conversationId 会话ID（null 则新建）
     * @param userId 用户ID
     * @return Map 包含：answer、conversationId、sources
     */
    public Map<String, Object> ask(String query, Long conversationId, Long userId) {
        // ===== 1. 会话管理 =====
        if (conversationId == null) {
            conversationId = conversationService.createConversation(userId, query);
        }

        // ===== 2. 保存用户消息 =====
        Message userMsg = new Message();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(query);
        messageMapper.insert(userMsg);
        conversationService.incrementMessageCount(conversationId);

        // ===== 3. 向量检索 =====
        EmbeddingProvider embedProvider = providerFactory.getEmbeddingProvider();
        float[] queryEmbedding = embedProvider.embed(query);
        List<RetrievedDocument> retrievedDocs = chromaRetriever.similaritySearch(queryEmbedding, 5);

        if (retrievedDocs.isEmpty()) {
            // 返回 "知识库中暂无相关信息，请联系管理员上传相关文档"
        }

        // ===== 4. 构建 Prompt =====
        String contextText = retrievedDocs.stream()
                .map(doc -> "【来源：" + doc.getDocumentTitle() + "】\n" + doc.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));
        String prompt = promptTemplate.build(query, contextText);

        // ===== 5. LLM 生成 =====
        LLMProvider llmProvider = providerFactory.getLLMProvider();
        List<ChatMessage> messages = List.of(
                SystemMessage.from("你是一个企业知识库助手，请根据参考资料准确回答用户问题。"),
                UserMessage.from(prompt)
        );
        String answer = llmProvider.generate(messages);

        // ===== 6. 保存助手消息 =====
        Message assistantMsg = new Message();
        assistantMsg.setConversationId(conversationId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(answer);
        // sources 转为 JSON
        assistantMsg.setSources(JSON.toJSONString(retrievedDocs));
        messageMapper.insert(assistantMsg);
        conversationService.incrementMessageCount(conversationId);

        // ===== 7. 发布统计事件 =====
        AnswerGeneratedEvent event = new AnswerGeneratedEvent(this, userId, conversationId);
        eventPublisher.publishEvent(event);

        // ===== 8. 返回结果 =====
        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("conversationId", conversationId);
        result.put("sources", retrievedDocs);
        return result;
    }
}
```

---

#### 9.4.10 `ChatController` — 问答控制器

**路径**：`org.example.agent_qr.rag.controller.ChatController`

```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Autowired
    private ChatQueryService chatQueryService;
    @Autowired
    private ConversationService conversationService;
    @Autowired
    private MessageMapper messageMapper;

    // POST /api/chat/ask
    // 请求体：{ "query": "...", "conversationId": null }
    // 返回：Result<Map<String, Object>>（含 answer、conversationId、sources）
    @PostMapping("/ask")
    public Result<Map<String, Object>> ask(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        Long conversationId = request.get("conversationId") != null
                ? Long.valueOf(request.get("conversationId").toString()) : null;
        Long userId = getCurrentUserId();  // 从 SecurityContext 获取
        return Result.success(chatQueryService.ask(query, conversationId, userId));
    }

    // GET /api/chat/conversations
    public Result<List<Conversation>> listConversations()

    // GET /api/chat/conversations/{id}/messages
    public Result<List<Message>> listMessages(@PathVariable Long id)

    // DELETE /api/chat/conversations/{id}
    public Result<Void> deleteConversation(@PathVariable Long id)

    // 辅助方法：获取当前用户 ID
    private Long getCurrentUserId() {
        SysUser user = (SysUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return user.getId();
    }
}
```

---

## 十、第 5 步：agent-qr-knowledge 模块完整规格

### 10.1 模块信息

| 属性 | 值 |
|------|-----|
| **包路径** | `org.example.agent_qr.knowledge` |
| **依赖** | `agent-qr-common`、`agent-qr-rag`、`pdfbox`、`poi-ooxml`、`spring-boot-starter-web` |

### 10.2 pom.xml 依赖

```xml
<dependencies>
    <dependency><groupId>org.example</groupId><artifactId>agent-qr-common</artifactId><version>0.0.1-SNAPSHOT</version></dependency>
    <dependency><groupId>org.example</groupId><artifactId>agent-qr-rag</artifactId><version>0.0.1-SNAPSHOT</version></dependency>
    <dependency><groupId>org.apache.pdfbox</groupId><artifactId>pdfbox</artifactId><version>3.0.3</version></dependency>
    <dependency><groupId>org.apache.poi</groupId><artifactId>poi-ooxml</artifactId><version>5.3.0</version></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
</dependencies>
```

### 10.3 包结构

```
org.example.agent_qr.knowledge
├── enums/
│   └── DocumentStatus.java
├── entity/
│   ├── Document.java
│   └── Chunk.java
├── mapper/
│   ├── DocumentMapper.java
│   └── ChunkMapper.java
├── service/
│   ├── FileStorageService.java
│   ├── DocumentQueryService.java
│   └── DocumentCommandService.java
├── parser/
│   ├── TextParser.java
│   ├── PdfParser.java
│   ├── DocxParser.java
│   └── DocumentParserService.java
├── splitter/
│   └── TextSplitter.java
├── listener/
│   ├── DocumentParseListener.java
│   └── ChunkEmbeddingListener.java
└── controller/
    └── KnowledgeController.java
```

### 10.4 类详细规格

---

#### 10.4.1 `DocumentStatus` 枚举

**路径**：`org.example.agent_qr.knowledge.enums.DocumentStatus`

```java
public enum DocumentStatus {
    UPLOADED("已上传"),
    PARSING("解析中"),
    CHUNKING("切片中"),
    EMBEDDING("向量化中"),
    READY("就绪"),
    FAILED("失败"),
    DELETING("删除中");

    private final String description;
    DocumentStatus(String description) { this.description = description; }
    public String getDescription() { return description; }
}
```

---

#### 10.4.2 实体类

**Document**（路径：`org.example.agent_qr.knowledge.entity.Document`）：

```java
@Data
@TableName("kb_document")
public class Document {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String fileName;
    private String filePath;
    private String fileType;       // pdf/docx/txt/md
    private Long fileSize;
    private String status;         // DocumentStatus 枚举值
    private Long uploadUserId;
    private String errorMsg;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

**Chunk**（路径：`org.example.agent_qr.knowledge.entity.Chunk`）：

```java
@Data
@TableName("kb_chunk")
public class Chunk {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private Integer chunkIndex;    // 切片序号
    private String content;        // 切片文本内容
    private Integer charCount;     // 字符数
    private String chromaId;       // ChromaDB 中的向量 ID
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

---

#### 10.4.3 Mapper

**DocumentMapper**（路径：`org.example.agent_qr.knowledge.mapper.DocumentMapper`）：

```java
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
    @Update("UPDATE kb_document SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE kb_document SET error_msg = #{errorMsg} WHERE id = #{id}")
    int updateErrorMsg(@Param("id") Long id, @Param("errorMsg") String errorMsg);

    // statistics 模块需要：
    @Select("SELECT file_type, COUNT(*) as cnt FROM kb_document GROUP BY file_type")
    List<Map<String, Object>> selectTypeDistribution();
}
```

**ChunkMapper**（路径：`org.example.agent_qr.knowledge.mapper.ChunkMapper`）：

```java
@Mapper
public interface ChunkMapper extends BaseMapper<Chunk> {
    @Delete("DELETE FROM kb_chunk WHERE document_id = #{documentId}")
    int deleteByDocumentId(@Param("documentId") Long documentId);

    @Select("SELECT * FROM kb_chunk WHERE document_id = #{documentId} ORDER BY chunk_index")
    List<Chunk> selectByDocumentId(@Param("documentId") Long documentId);
}
```

---

#### 10.4.4 `FileStorageService` — 文件存储服务

**路径**：`org.example.agent_qr.knowledge.service.FileStorageService`

```java
@Service
public class FileStorageService {
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public String store(MultipartFile file) {
        // 1. 按日期创建子目录：uploadDir/yyyy/MM
        // 2. 生成唯一文件名：System.currentTimeMillis() + "_" + 原始文件名
        // 3. file.transferTo() 写入
        // 4. 返回相对路径
    }

    public void delete(String filePath) {
        // Files.deleteIfExists(Paths.get(filePath))
    }
}
```

---

#### 10.4.5 文档解析器

**TextParser**（路径：`org.example.agent_qr.knowledge.parser.TextParser`）：

```java
@Component
public class TextParser {
    public String parse(String filePath) {
        // Files.readString(Paths.get(filePath), StandardCharsets.UTF_8)
    }
}
```

**PdfParser**（P1 基础版，路径：`org.example.agent_qr.knowledge.parser.PdfParser`）：

```java
@Component
public class PdfParser {
    public String parse(String filePath) {
        // 1. Loader.loadPDF(new File(filePath)) 加载文档
        // 2. PDFTextStripper 提取文本（setSortByPosition(true)）
        // 3. 返回纯文本
        // P1 不含 Tika 表格识别和 OCR
    }
}
```

**DocxParser**（P1 基础版，路径：`org.example.agent_qr.knowledge.parser.DocxParser`）：

```java
@Component
public class DocxParser {
    public String parse(String filePath) {
        // 1. XWPFDocument 加载文档
        // 2. 遍历所有段落，提取文本
        // 3. 返回纯文本
        // P1 基础版不含表格 Markdown 转换
    }
}
```

**DocumentParserService**（路径：`org.example.agent_qr.knowledge.parser.DocumentParserService`）：

```java
@Service
public class DocumentParserService {
    @Autowired
    private PdfParser pdfParser;
    @Autowired
    private DocxParser docxParser;
    @Autowired
    private TextParser textParser;

    public String parse(String filePath, String fileType) {
        return switch (fileType.toLowerCase()) {
            case "pdf" -> pdfParser.parse(filePath);
            case "docx" -> docxParser.parse(filePath);
            case "txt", "md" -> textParser.parse(filePath);
            default -> throw new BusinessException("不支持的文件类型: " + fileType);
        };
    }
}
```

---

#### 10.4.6 `TextSplitter` — 文本切片器

**路径**：`org.example.agent_qr.knowledge.splitter.TextSplitter`

```java
@Service
public class TextSplitter {
    @Value("${rag.chunk-size:500}")
    private int chunkSize;
    @Value("${rag.chunk-overlap:50}")
    private int chunkOverlap;

    public List<String> split(String text) {
        // 1. 按 "\n\n" 分割为段落
        // 2. 超长段落（> chunkSize）→ splitLongText() 递归分割
        // 3. 合并过短片段（< 100 字符）到前一条
    }

    private List<String> splitLongText(String text) {
        // 滑动窗口：
        // - 窗口大小 = chunkSize
        // - 步长 = chunkSize - chunkOverlap
        // - 在窗口末尾附近寻找断点（。\n！？；，）
    }

    private int findBreakPoint(String text, int end) {
        // 在 [end-50, end] 范围内寻找最靠后的断点字符
        // 优先级：。 > \n > ！ > ？ > ; > ； > ， > 空格
    }

    private List<String> mergeShortChunks(List<String> chunks) {
        // 将长度 < 100 的片段合并到前一条
    }
}
```

---

#### 10.4.7 `DocumentQueryService` — 文档查询服务

**路径**：`org.example.agent_qr.knowledge.service.DocumentQueryService`

```java
@Service
public class DocumentQueryService {
    @Autowired
    private DocumentMapper documentMapper;
    @Autowired
    private ChunkMapper chunkMapper;

    public IPage<Document> listDocuments(int page, int size) {
        // documentMapper.selectPage(new Page<>(page, size), null)
    }

    public Document getDocument(Long id) {
        // documentMapper.selectById(id)
        // null → throw BusinessException(404, "文档不存在")
    }

    public DocumentStatus getStatus(Long id) {
        // 查 document → 返回 DocumentStatus.valueOf(doc.getStatus())
    }

    public List<Chunk> getChunks(Long documentId) {
        // chunkMapper.selectByDocumentId(documentId)
    }
}
```

---

#### 10.4.8 `DocumentCommandService` — 文档命令服务

**路径**：`org.example.agent_qr.knowledge.service.DocumentCommandService`

```java
@Service
@Slf4j
public class DocumentCommandService {
    @Autowired
    private DocumentMapper documentMapper;
    @Autowired
    private ChunkMapper chunkMapper;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "docx", "txt", "md");
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;  // 50MB

    @Transactional
    public Document uploadDocument(MultipartFile file, String title, Long userId) {
        // 1. 校验文件类型：getFileExtension()
        // 2. 校验文件大小 ≤ 50MB
        // 3. fileStorageService.store() 保存文件
        // 4. 创建 Document 记录（status = UPLOADED）
        // 5. documentMapper.insert(doc)
        // 6. 发布 DocumentUploadedEvent
        // 7. 返回 Document
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        // 1. 查文档，不存在 → BusinessException(404, "文档不存在")
        // 2. fileStorageService.delete() 删除文件
        // 3. chunkMapper.deleteByDocumentId() 删除切片
        // 4. documentMapper.deleteById() 删除文档记录
        // 注意：P1 不包含 ChromaDB 向量删除（ChromaRetriever 可能还没有 deleteByDocumentId 实现）
    }

    private String getFileExtension(String fileName) {
        // 获取最后一个 "." 之后的字符串并转小写
    }
}
```

---

#### 10.4.9 事件监听器

**DocumentParseListener**（路径：`org.example.agent_qr.knowledge.listener.DocumentParseListener`）：

```java
@Component
@Slf4j
public class DocumentParseListener {
    @Autowired
    private DocumentMapper documentMapper;
    @Autowired
    private DocumentParserService parserService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @EventListener
    @Async("docProcessExecutor")
    public void handleDocumentUploaded(DocumentUploadedEvent event) {
        // 1. 更新状态 → PARSING：documentMapper.updateStatus(id, "PARSING")
        // 2. parserService.parse(event.getFilePath(), event.getFileType())
        // 3. 发布 DocumentParsedEvent
        // 4. 异常：更新状态 → FAILED + 记录 errorMsg
    }
}
```

**ChunkEmbeddingListener**（路径：`org.example.agent_qr.knowledge.listener.ChunkEmbeddingListener`）：

```java
@Component
@Slf4j
public class ChunkEmbeddingListener {
    @Autowired
    private DocumentMapper documentMapper;
    @Autowired
    private ChunkMapper chunkMapper;
    @Autowired
    private TextSplitter textSplitter;
    @Autowired
    private ProviderFactory providerFactory;      // 来自 rag 模块
    @Autowired
    private ChromaRetriever chromaRetriever;      // 来自 rag 模块，用于写入向量
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @EventListener
    @Async("docProcessExecutor")
    public void handleDocumentParsed(DocumentParsedEvent event) {
        try {
            // ===== 1. 切片阶段 =====
            documentMapper.updateStatus(event.getDocumentId(), "CHUNKING");
            List<String> chunks = textSplitter.split(event.getContent());

            // 逐片保存到 kb_chunk
            int idx = 0;
            for (String chunkText : chunks) {
                Chunk chunk = new Chunk();
                chunk.setDocumentId(event.getDocumentId());
                chunk.setChunkIndex(idx++);
                chunk.setContent(chunkText);
                chunk.setCharCount(chunkText.length());
                chunkMapper.insert(chunk);
            }

            // 发布 ChunksCreatedEvent
            ChunksCreatedEvent chunksEvent = new ChunksCreatedEvent(this, event.getDocumentId(), chunks);
            eventPublisher.publishEvent(chunksEvent);

            // ===== 2. 向量化阶段 =====
            documentMapper.updateStatus(event.getDocumentId(), "EMBEDDING");
            EmbeddingProvider embedProvider = providerFactory.getEmbeddingProvider();

            List<Chunk> savedChunks = chunkMapper.selectByDocumentId(event.getDocumentId());
            int successCount = 0;
            for (Chunk chunk : savedChunks) {
                try {
                    float[] vector = embedProvider.embed(chunk.getContent());
                    // 写入 ChromaDB（通过 ChromaRetriever 或直接使用 ChromaVectorStore）
                    // 更新 chunk 的 chroma_id
                    successCount++;
                } catch (Exception e) {
                    log.error("切片向量化失败: chunkId={}", chunk.getId(), e);
                }
            }

            // 更新状态 → READY
            documentMapper.updateStatus(event.getDocumentId(), "READY");

            // 发布 EmbeddingCompletedEvent
            EmbeddingCompletedEvent embEvent = new EmbeddingCompletedEvent(this, event.getDocumentId(), successCount);
            eventPublisher.publishEvent(embEvent);

        } catch (Exception e) {
            log.error("文档处理失败: id={}", event.getDocumentId(), e);
            documentMapper.updateStatus(event.getDocumentId(), "FAILED");
            documentMapper.updateErrorMsg(event.getDocumentId(), "处理失败: " + e.getMessage());
        }
    }
}
```

**注意**：ChunkEmbeddingListener 需要 ChromaDB 写入能力。如果 `ChromaRetriever` 只提供读取接口，则需要额外注入 `ChromaVectorStore` 或创建写入方法。

---

#### 10.4.10 `KnowledgeController` — 知识库控制器

**路径**：`org.example.agent_qr.knowledge.controller.KnowledgeController`

```java
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    @Autowired
    private DocumentCommandService documentCommandService;
    @Autowired
    private DocumentQueryService documentQueryService;

    // POST /api/knowledge/upload
    // 参数：@RequestParam MultipartFile file, @RequestParam(required=false) String title
    // 权限：hasRole("ADMIN")
    @PostMapping("/upload")
    public Result<Document> upload(@RequestParam MultipartFile file,
                                    @RequestParam(required = false) String title) {
        Long userId = getCurrentUserId();
        return Result.success(documentCommandService.uploadDocument(file, title, userId));
    }

    // GET /api/knowledge/documents?page=1&size=10
    @GetMapping("/documents")
    public Result<IPage<Document>> list(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size)

    // GET /api/knowledge/documents/{id}
    @GetMapping("/documents/{id}")
    public Result<Document> get(@PathVariable Long id)

    // DELETE /api/knowledge/documents/{id}
    @DeleteMapping("/documents/{id}")
    public Result<Void> delete(@PathVariable Long id)

    // GET /api/knowledge/documents/{id}/status
    @GetMapping("/documents/{id}/status")
    public Result<String> getStatus(@PathVariable Long id)

    // GET /api/knowledge/documents/{id}/chunks
    @GetMapping("/documents/{id}/chunks")
    public Result<List<Chunk>> getChunks(@PathVariable Long id)
}
```

---

## 十一、第 6 步：agent-qr-statistics 模块完整规格

### 11.1 模块信息

| 属性 | 值 |
|------|-----|
| **包路径** | `org.example.agent_qr.statistics` |
| **依赖** | `agent-qr-common`、`agent-qr-knowledge`、`agent-qr-user`、`mybatis-plus-spring-boot3-starter` |

### 11.2 pom.xml 依赖

```xml
<dependencies>
    <dependency><groupId>org.example</groupId><artifactId>agent-qr-common</artifactId><version>0.0.1-SNAPSHOT</version></dependency>
    <dependency><groupId>org.example</groupId><artifactId>agent-qr-knowledge</artifactId><version>0.0.1-SNAPSHOT</version></dependency>
    <dependency><groupId>org.example</groupId><artifactId>agent-qr-user</artifactId><version>0.0.1-SNAPSHOT</version></dependency>
    <dependency><groupId>com.baomidou</groupId><artifactId>mybatis-plus-spring-boot3-starter</artifactId></dependency>
</dependencies>
```

### 11.3 包结构

```
org.example.agent_qr.statistics
├── entity/
│   └── DailyStats.java
├── mapper/
│   └── DailyStatsMapper.java
├── dto/
│   └── DashboardVO.java
├── service/
│   └── StatisticsQueryService.java
├── listener/
│   └── StatisticsUpdateListener.java
└── controller/
    └── StatisticsController.java
```

### 11.4 类详细规格

---

#### 11.4.1 `DailyStats` — 日统计实体

**路径**：`org.example.agent_qr.statistics.entity.DailyStats`

```java
@Data
@TableName("stat_daily")
public class DailyStats {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;        // 统计日期
    private Integer qaCount = 0;       // 问答数
    private Integer userQuestionCount = 0;  // 提问用户数
    private Integer activeUserCount = 0;   // 活跃用户数
    private Integer docUploadCount = 0;    // 文档上传统计
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

---

#### 11.4.2 `DailyStatsMapper`

**路径**：`org.example.agent_qr.statistics.mapper.DailyStatsMapper`

```java
@Mapper
public interface DailyStatsMapper extends BaseMapper<DailyStats> {
    @Select("SELECT * FROM stat_daily WHERE stat_date = #{date}")
    DailyStats selectByDate(@Param("date") LocalDate date);

    @Update("UPDATE stat_daily SET doc_upload_count = doc_upload_count + 1 WHERE stat_date = #{date}")
    int incrementDocUploadCount(@Param("date") LocalDate date);

    @Update("<script>" +
            "UPDATE stat_daily SET qa_count = qa_count + 1" +
            "<if test='userId != null'>, active_user_count = active_user_count + 1</if>" +
            " WHERE stat_date = #{date}" +
            "</script>")
    int incrementQaCount(@Param("date") LocalDate date, @Param("userId") Long userId);

    @Select("SELECT * FROM stat_daily WHERE stat_date >= DATE_SUB(#{endDate}, INTERVAL 6 DAY) AND stat_date <= #{endDate} ORDER BY stat_date ASC")
    List<DailyStats> selectWeeklyTrend(@Param("endDate") LocalDate endDate);
}
```

---

#### 11.4.3 `DashboardVO`

**路径**：`org.example.agent_qr.statistics.dto.DashboardVO`

```java
@Data
public class DashboardVO {
    private Integer todayQA;                 // 今日问答数
    private Integer todayNewUsers;           // 今日新增用户
    private Long totalDocuments;             // 文档总数
    private Long totalChunks;                // 切片总数
    private Long totalUsers;                 // 用户总数
    private List<DailyStats> weeklyTrend;    // 近 7 天趋势
    private Map<String, Long> docTypeDistribution;  // 文档类型分布
}
```

---

#### 11.4.4 `StatisticsQueryService`

**路径**：`org.example.agent_qr.statistics.service.StatisticsQueryService`

```java
@Service
public class StatisticsQueryService {
    @Autowired
    private DailyStatsMapper dailyStatsMapper;
    @Autowired
    private DocumentMapper documentMapper;   // 来自 knowledge 模块
    @Autowired
    private ChunkMapper chunkMapper;         // 来自 knowledge 模块
    @Autowired
    private SysUserMapper sysUserMapper;     // 来自 user 模块

    public DashboardVO getDashboard() {
        LocalDate today = LocalDate.now();
        DashboardVO vo = new DashboardVO();

        // 1. 今日统计
        DailyStats todayStats = dailyStatsMapper.selectByDate(today);
        if (todayStats != null) {
            vo.setTodayQA(todayStats.getQaCount());
            vo.setTodayNewUsers(sysUserMapper.countByDate(today).intValue());
        } else {
            vo.setTodayQA(0);
            vo.setTodayNewUsers(0);
        }

        // 2. 知识库概览
        vo.setTotalDocuments(documentMapper.selectCount(null));
        vo.setTotalChunks(chunkMapper.selectCount(null));
        vo.setTotalUsers(sysUserMapper.selectCount(null));

        // 3. 近 7 天趋势
        vo.setWeeklyTrend(dailyStatsMapper.selectWeeklyTrend(today));

        // 4. 文档类型分布
        vo.setDocTypeDistribution(
            documentMapper.selectTypeDistribution().stream()
                .collect(Collectors.toMap(
                    m -> (String) m.get("file_type"),
                    m -> ((Number) m.get("cnt")).longValue()
                ))
        );

        return vo;
    }
}
```

---

#### 11.4.5 `StatisticsUpdateListener`

**路径**：`org.example.agent_qr.statistics.listener.StatisticsUpdateListener`

```java
@Component
@Slf4j
public class StatisticsUpdateListener {
    @Autowired
    private DailyStatsMapper dailyStatsMapper;

    // 监听 EmbeddingCompletedEvent → 文档上传数 +1
    @EventListener
    @Async("statExecutor")
    public void handleEmbeddingCompleted(EmbeddingCompletedEvent event) {
        LocalDate today = LocalDate.now();
        DailyStats stats = dailyStatsMapper.selectByDate(today);
        if (stats == null) {
            stats = new DailyStats();
            stats.setStatDate(today);
            stats.setDocUploadCount(1);
            dailyStatsMapper.insert(stats);
        } else {
            dailyStatsMapper.incrementDocUploadCount(today);
        }
    }

    // 监听 AnswerGeneratedEvent → 问答数 +1
    @EventListener
    @Async("statExecutor")
    public void handleAnswerGenerated(AnswerGeneratedEvent event) {
        LocalDate today = LocalDate.now();
        DailyStats stats = dailyStatsMapper.selectByDate(today);
        if (stats == null) {
            stats = new DailyStats();
            stats.setStatDate(today);
            stats.setQaCount(1);
            stats.setUserQuestionCount(1);
            stats.setActiveUserCount(1);
            dailyStatsMapper.insert(stats);
        } else {
            dailyStatsMapper.incrementQaCount(today, event.getUserId());
        }
    }
}
```

---

#### 11.4.6 `StatisticsController`

**路径**：`org.example.agent_qr.statistics.controller.StatisticsController`

```java
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
    @Autowired
    private StatisticsQueryService statisticsQueryService;

    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard() {
        return Result.success(statisticsQueryService.getDashboard());
    }
}
```

---

## 十二、第 7 步：agent-qr-web 模块完整规格

### 12.1 模块信息

| 属性 | 值 |
|------|-----|
| **包路径** | `org.example.agent_qr`（启动类）+ `org.example.agent_qr.web.config`（配置类） |
| **依赖** | 所有 6 个业务模块 + `spring-boot-starter-web/security` + `mysql-connector-j` + `spring-boot-starter-validation` |

### 12.2 pom.xml 依赖

（已在第 5.3 节列出，确认与 5.3 一致）

### 12.3 包结构

```
org.example.agent_qr
├── AgentQrApplication.java          // 启动类
└── web/
    └── config/
        ├── GlobalExceptionHandler.java
        ├── CorsConfig.java
        ├── SecurityConfig.java
        └── AsyncConfig.java

src/main/resources/
├── application.yml
└── application-p1.yml
```

### 12.4 类详细规格

---

#### 12.4.1 `AgentQrApplication` — 启动类

**路径**：`org.example.agent_qr.AgentQrApplication`

```java
@SpringBootApplication
@ComponentScan(basePackages = "org.example.agent_qr")  // 确保扫描所有模块
public class AgentQrApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentQrApplication.class, args);
    }
}
```

---

#### 12.4.2 `GlobalExceptionHandler` — 全局异常处理

**路径**：`org.example.agent_qr.web.config.GlobalExceptionHandler`

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.error(400, msg);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        return Result.error(403, "权限不足，无法访问该资源");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, "服务器内部错误，请稍后重试");
    }
}
```

---

#### 12.4.3 `CorsConfig` — 跨域配置

**路径**：`org.example.agent_qr.web.config.CorsConfig`

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")          // 开发阶段允许所有来源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

---

#### 12.4.4 `SecurityConfig` — Spring Security 配置

**路径**：`org.example.agent_qr.web.config.SecurityConfig`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Autowired
    private JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/knowledge/**").hasRole("ADMIN")
                .requestMatchers("/api/chat/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

**重要**：`JwtAuthenticationFilter` 来自 `agent-qr-auth` 模块。需要确保 auth 模块已正确添加到依赖中。

---

#### 12.4.5 `AsyncConfig` — 异步线程池配置

**路径**：`org.example.agent_qr.web.config.AsyncConfig`

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("docProcessExecutor")
    public Executor docProcessExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("doc-process-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("statExecutor")
    public Executor statExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("stat-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

---

#### 12.4.6 配置文件

**application.yml**（路径：`src/main/resources/application.yml`）：

```yaml
spring:
  application:
    name: agent-qr
  datasource:
    url: jdbc:mysql://localhost:3308/agent_qr?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto

server:
  port: 9090
```

**application-p1.yml**（路径：`src/main/resources/application-p1.yml`）：

```yaml
jwt:
  secret: ${JWT_SECRET:your-secret-key-change-in-production}
  expiration: 86400000  # 24 hours

llm:
  provider: deepseek
  deepseek:
    api-key: ${DEEPSEEK_API_KEY:your-api-key}
    base-url: https://api.deepseek.com
    model: deepseek-chat
    temperature: 0.7
    max-tokens: 2048

embedding:
  provider: deepseek
  deepseek:
    api-key: ${DEEPSEEK_API_KEY:your-api-key}
    base-url: https://api.deepseek.com
    model: deepseek-embedding

langchain4j:
  chroma:
    base-url: http://localhost:8000
    collection-name: enterprise_knowledge

file:
  upload-dir: ./uploads

rag:
  chunk-size: 500
  chunk-overlap: 50
```

---

## 十三、数据库初始化 SQL

在主控 Agent 完成所有模块代码生成后，需要创建数据库初始化脚本。

**路径**：`agent-qr-web/src/main/resources/db/p1-schema.sql`

```sql
-- P1 阶段建表语句

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) DEFAULT 'user',
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kb_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(10) NOT NULL,
    file_size BIGINT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'UPLOADED',
    upload_user_id BIGINT,
    error_msg TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_upload_user (upload_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kb_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    chunk_index INT DEFAULT 0,
    content TEXT,
    char_count INT DEFAULT 0,
    chroma_id VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_document_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    message_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    sources TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS stat_daily (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_date DATE NOT NULL UNIQUE,
    qa_count INT DEFAULT 0,
    user_question_count INT DEFAULT 0,
    active_user_count INT DEFAULT 0,
    doc_upload_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 十四、关键技术决策说明

### 14.1 P1 阶段的简化处理

以下功能在 P1 阶段暂不实现，留到 P2/P3：

| 功能 | P1 处理 | P2/P3 实现 |
|------|---------|-----------|
| ABAC 属性权限 | 仅角色权限（admin/user） | AbacEvaluator + UserPrincipal |
| 流式 SSE 输出 | `generateStream()` 返回空 | 完整 SSE 流式推送 |
| 混合检索（BM25+Rerank） | 仅 ChromaDB 语义检索 | BM25Retriever + RerankerService |
| 文档软删除 | 物理删除 | 软删除 + 补偿机制 |
| DLQ 死信队列 | 异常直接标记 FAILED | DeadLetterQueue + 指数退避 |
| 批量向量化 | 逐条 embed() | BatchEmbeddingService 攒批 |
| RefreshToken | 无 | 双 Token 机制 |
| 多源数据接入 | 无 | JDBC/REST/S3 Connector |
| ETL 标准化 | 无 | DataNormalizer + StructuredDataConverter |
| 满意度反馈 | 无 | FeedbackService |
| 知识目录 | 无 | KnowledgeCatalogService + DomainRouter |
| CQRS 读写分离 | 单数据源 | ReadWriteRoutingDataSource |
| 全链路追踪 | 无 | TraceIdFilter + MdcTaskDecorator |

### 14.2 循环依赖规避

- `user` 不依赖 `auth`：AdminController 创建用户时直接使用 `BCryptPasswordEncoder`，不通过 `PasswordUtil`
- `rag` 不依赖 `knowledge`：两者通过 ChromaDB 解耦（knowledge 写入向量，rag 检索向量）
- `statistics` 通过事件监听（`@EventListener`）与 `knowledge` 和 `rag` 通信，无直接方法调用依赖

### 14.3 事件驱动链（P1）

```
DocumentCommandService.uploadDocument()
  → 发布 DocumentUploadedEvent
    → DocumentParseListener（异步解析文档）
      → 发布 DocumentParsedEvent
        → ChunkEmbeddingListener（异步切片 + 向量化）
          → 发布 EmbeddingCompletedEvent
            → StatisticsUpdateListener（更新日统计）

ChatQueryService.ask()
  → 发布 AnswerGeneratedEvent
    → StatisticsUpdateListener（更新问答统计）
```

---

## 十五、子 Agent 创建指南

当你（主控 Agent）为每个模块创建子 Agent 时，使用以下模板：

### 子 Agent Prompt 模板

```
你是 agent-qr 项目的代码生成 Agent。你的任务是完成 [模块名] 模块的全部代码实现。

## 上下文
- 项目：基于 LangChain4j 的 RAG 企业内部知识库问答系统
- Java 21 + Spring Boot 3.5.15 + MyBatis-Plus
- 基础包路径：org.example.agent_qr
- P1 阶段（MVP）

## 已有模块（可直接 import 使用）
[列出已完成模块的包路径和关键类]

## 你的任务
[从本文档对应模块章节中复制完整的类规格]

## 要求
1. 为每个类创建完整的 Java 文件，放在正确的包路径下
2. 添加所有必要的 import 语句
3. 添加完整的 Javadoc 注释
4. 正确使用 Lombok 注解（@Data, @Slf4j 等）
5. 正确使用 Spring 注解（@Service, @Component, @Autowired 等）
6. 异常处理：业务异常使用 BusinessException，系统异常记录 log.error
7. P1 阶段不实现标记为 P2/P3 的功能
8. 完成后列出你创建的所有文件路径
```

---

## 十六、验证检查清单

每个模块完成后，主控 Agent 应验证：

### 代码层面
- [ ] 所有 Java 文件存在于正确的包路径下
- [ ] 所有 import 语句正确，无未解析的引用
- [ ] 构造器、getter/setter 由 Lombok 生成
- [ ] 依赖注入使用 `@Autowired` 字段注入（与现有设计文档风格一致）
- [ ] 事务方法使用 `@Transactional`

### 模块层面
- [ ] pom.xml 依赖与本文档规定的依赖一致
- [ ] 不依赖尚未完成的模块（遵循开发顺序）
- [ ] 跨模块引用均指向已存在的类

### 全局层面
- [ ] 根 pom.xml `<modules>` 包含全部 7 个模块
- [ ] 模块目录名与 pom.xml artifactId 一致（注意 gent-qr-web → agent-qr-web）
- [ ] application.yml 配置完整
- [ ] 可执行 `mvn compile`（如环境允许）
- [ ] 数据库建表 SQL 与实体类字段一致

---

> **文档版本**：v1.0
>
> **生成日期**：2026-06-17
>
> **输入文档**：系统详细设计说明书 v1.0、p1-tasks 文件夹全部内容
