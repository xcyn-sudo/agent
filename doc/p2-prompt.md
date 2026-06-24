# P2 阶段代码生成 Prompt

> 本文档是 P2 阶段代码生成的总控 Prompt，用于驱动 Claude Code Agent 完成全部 P2 模块的代码实现。
>
> 版本：v1.0
>
> 日期：2026-06-24
>
> 前置条件：P1 阶段全部代码已实现并可编译通过

---

## 一、你的角色

你是一个**主控 Agent（Orchestrator）**，负责：

1. **理解 P2 阶段全部需求**：阅读本文档的全部内容，确保对 P2 阶段的模块划分、依赖关系、开发顺序有完整理解。
2. **跟踪整体进度**：维护一个进度清单，记录每个模块 / 每个类的完成状态。
3. **逐个模块推进**：按照开发顺序，为每个模块创建一个**子 Agent** 来生成该模块的全部 P2 代码。
4. **验证完整性**：每个模块完成后，检查子 Agent 的输出是否覆盖了该模块的全部任务项。

**重要：P2 阶段是在 P1 已完成代码的基础上进行扩展。** 对于已有模块（common/auth/user/knowledge/rag/statistics/web），你需要：
- **扩展已有类**（新增字段/方法/注解），而非重新创建
- **新建 P2 类**到正确包路径下
- **修改 pom.xml** 添加新依赖

对于 P2 新增模块（compensation/datasource/data-quality/etl/catalog），需要**从零创建**完整的 Maven 模块结构。

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
| **LLM/Embedding** | DeepSeek API（默认）+ Ollama（本地部署选项） |
| **前端** | 服务端端口 9090 |

### 2.2 P2 阶段目标

在 P1 MVP 基础上实现以下增强：

1. ABAC 属性访问控制（部门/密级/域/职级细粒度权限）
2. 双 Token 机制（Access 30min + Refresh 7day，令牌轮换）
3. SSE 流式输出（逐 token 推送）
4. 混合检索（语义+BM25→RRF融合→Rerank精排）
5. 多源数据接入（JDBC/REST/S3 统一连接器策略）
6. ETL 标准化管道（异构数据→字段映射→自然语言）
7. 数据质量检查（规则链引擎，合格率阻断判定）
8. 知识目录（三级目录树，域路由裁剪检索）
9. 数据一致性补偿（软删除+事件驱动物理删除+DLQ+孤儿扫描）
10. LLM 熔断器（状态机保护，自动降级）
11. 批量向量化攒批（BlockingQueue 生产者-消费者，100倍提升）
12. 全链路追踪（TraceId 过滤 + MDC 异步传递）
13. 满意度反馈（答案点赞/点踩 + Dashboard 统计）

### 2.3 P2 模块列表

| 序号 | 模块 | 包路径 | 类型 | 说明 |
|------|------|--------|------|------|
| 1 | agent-qr-common | `org.example.agent_qr.common` | P1 扩展 | TraceId/MDC/DLQ/Caffeine/新事件 |
| 2 | agent-qr-user | `org.example.agent_qr.user` | P1 扩展 | SysUser ABAC 字段扩展 |
| 3 | agent-qr-auth | `org.example.agent_qr.auth` | P1 扩展 | ABAC 权限体系、双Token机制 |
| 4 | agent-qr-datasource | `org.example.agent_qr.datasource` | ★ P2 新模块 | 多源数据接入（JDBC/REST/S3） |
| 5 | agent-qr-catalog | `org.example.agent_qr.catalog` | ★ P2 新模块 | 知识目录 + DomainRouter |
| 6 | agent-qr-data-quality | `org.example.agent_qr.dataquality` | ★ P2 新模块 | 数据质量规则链引擎 |
| 7 | agent-qr-etl | `org.example.agent_qr.etl` | ★ P2 新模块 | ETL 标准化管道 |
| 8 | agent-qr-rag | `org.example.agent_qr.rag` | P1 扩展 | 混合检索/SSE流式/熔断/批量向量化 |
| 9 | agent-qr-knowledge | `org.example.agent_qr.knowledge` | P1 扩展 | 软删除v2/PDF增强/DOCX表格/DLQ集成 |
| 10 | agent-qr-compensation | `org.example.agent_qr.compensation` | ★ P2 新模块 | 数据一致性补偿 |
| 11 | agent-qr-statistics | `org.example.agent_qr.statistics` | P1 扩展 | 满意度统计/FeedbackService |
| 12 | agent-qr-web | `org.example.agent_qr` | P1 扩展 | 汇聚配置/AsyncConfigV2/p2-schema.sql |

★ = P2 阶段新增 Maven 模块

### 2.4 P2 模块依赖关系图

```
                          ┌───────────────────────────────────┐
                          │         agent-qr-web              │
                          │  (依赖全部模块 + application-p2.yml) │
                          └──────────────┬────────────────────┘
                                         │
     ┌───────────────┬───────────┬───────┼───────────┬───────────────┐
     │               │           │       │           │               │
     ▼               ▼           ▼       ▼           ▼               ▼
┌──────────┐  ┌────────────┐ ┌──────────┐ ┌──────────────┐ ┌──────────────┐
│  auth   │  │ knowledge  │ │   rag    │ │ statistics   │ │ compensation │
│  (P2)   │  │   (P2)     │ │  (P2)    │ │    (P2)      │ │    (★新增)    │
└────┬─────┘  └──┬───┬─────┘ └──┬───┬───┘ └──┬───┬───────┘ └──┬───────────┘
     │           │   │          │   │        │   │            │
     │      ┌────┘   └────┐     │   │   ┌────┘   │       ┌────┘
     │      │             │     │   │   │        │       │
     ▼      ▼             ▼     ▼   ▼   ▼        ▼       ▼
┌──────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────────────┐
│  user   │ │  common (P2) │ │   catalog ★  │ │  knowledge (P1 base) │
│  (P2)   │ │ DLQ/TraceId  │ │  DomainRouter│ │                      │
└──────────┘ └──────────────┘ └──────┬───────┘ └──────────────────────┘
                                     │
                              ┌──────┴───────┐
                              │ datasource ★ │
                              │ (统一接入网关) │
                              └──────┬───────┘
                                     │
                              ┌──────┴───────┐
                              │  etl ★      │
                              │ (标准化管道)  │
                              └──────┬───────┘
                                     │
                              ┌──────┴───────────┐
                              │ data-quality ★  │
                              │ (质检引擎)       │
                              └─────────────────┘
```

### 2.5 关键依赖规则

| 依赖方 | 被依赖方 | 说明 |
|--------|---------|------|
| `agent-qr-common` | 无 | 被所有模块依赖，禁止反向依赖任何业务模块 |
| `agent-qr-user` | `agent-qr-common` | SysUser ABAC 字段扩展 |
| `agent-qr-auth` | `agent-qr-common` + `agent-qr-user` | ABAC 属性读取 SysUser |
| `agent-qr-datasource` | `agent-qr-common` | 独立新模块 |
| `agent-qr-catalog` | `agent-qr-common` + `agent-qr-datasource` | 目录构建读取数据源列表 |
| `agent-qr-etl` | `agent-qr-common` + `agent-qr-datasource` | 读取 DataSourceConfig |
| `agent-qr-data-quality` | `agent-qr-common` | 独立新模块 |
| `agent-qr-rag` | `agent-qr-common` + `agent-qr-catalog` | HybridRetriever 调用 DomainRouter |
| `agent-qr-knowledge` | `agent-qr-common` + `agent-qr-rag` | ★ 不依赖 compensation |
| `agent-qr-compensation` | `agent-qr-common` + `agent-qr-knowledge` | ★ 单向依赖 knowledge |
| `agent-qr-statistics` | `agent-qr-common` + `agent-qr-knowledge` + `agent-qr-user` | 满意度统计 |
| `agent-qr-web` | 所有 11 个模块 | 启动类 + 全局配置 + p2-schema.sql |

> ★ **循环依赖解耦设计**：`agent-qr-knowledge` 与 `agent-qr-compensation` 之间通过**事件驱动**实现解耦：
> - `knowledge` 只发布 `DocumentDeleteRequestedEvent` 事件（定义在 `agent-qr-common`），**不注入 compensation 的任何类**
> - `compensation` 单向依赖 `knowledge`（访问 ChunkMapper / DocumentMapper），并通过 `@TransactionalEventListener(phase = AFTER_COMMIT)` 监听删除事件
> - 依赖链：`compensation → knowledge → common ← compensation`（通过 common 事件类解耦，无循环）

---

## 三、开发顺序（严格执行）

P2 阶段依赖 P1 全部产出，开发顺序按内部依赖关系编排：

```
第 0 步：根 pom.xml 注册 5 个新模块（★ 前置步骤，在所有子模块代码生成前完成）
          ↓
第 1 步：agent-qr-common P2（DLQ + TraceId + Caffeine + 新事件类）
          ↓
第 2 步：agent-qr-user P2（SysUser ABAC 字段扩展）
          ↓
第 3 步：agent-qr-auth P2（ABAC 权限体系、双 Token）← 依赖 user
          ↓
第 4 步：agent-qr-datasource ★（数据源接入层）← 独立新模块
          ↓
第 5 步：agent-qr-catalog ★（知识目录 + DomainRouter）← 依赖 datasource + knowledge
          ↓
第 6 步：agent-qr-data-quality ★（数据质量检查）← 独立新模块
          ↓
第 7 步：agent-qr-etl ★（ETL 标准化管道）← 依赖 datasource
          ↓
第 8 步：agent-qr-rag P2（混合检索 + 流式 + 熔断）← 依赖 catalog + common
          ↓
第 9 步：agent-qr-knowledge P2（软删除v2 + 解析增强 + DLQ 集成）← 依赖 rag + common
          ↓
第 10 步：agent-qr-compensation ★（数据一致性补偿）← 依赖 knowledge + common
          ↓
第 11 步：agent-qr-statistics P2（满意度统计）← 依赖 knowledge + user
          ↓
第 12 步：agent-qr-web P2（汇聚配置 + agent-qr-web/pom.xml 新模块依赖 + p2-schema.sql）
```

> ★ 第 0 步是**前置步骤**：必须先更新根 `pom.xml` 注册 5 个新 Maven 模块（compensation/datasource/data-quality/etl/catalog），
> 后续各新模块的子 Agent 才能正常创建 `pom.xml` 并让 Maven 正确解析项目结构。

---

## 四、主控 Agent 工作流程

### 4.1 初始化阶段

1. **阅读本文档全部内容**，确认理解所有模块需求。
2. **检查 P1 代码现状**：
   - 确认各 P1 模块的 `pom.xml` 存在且依赖正确
   - 确认各 P1 类的包路径和文件位置
   - 确认根 `pom.xml` 中 `<modules>` 已包含 7 个 P1 模块
3. **创建进度跟踪清单**（见下方模板），记录所有类的完成状态。

### 4.2 逐模块推进

对每个模块（按第 3 节的顺序）：

1. **创建子 Agent**：使用 Agent 工具，为当前模块创建一个子 Agent。
2. **子 Agent Prompt**：将本文档中对应模块的完整规格传递给子 Agent，包括：
   - 模块基本信息（包路径、依赖关系）
   - P1 已有代码情况（哪些类存在，哪些需要扩展）
   - P2 需要新增的类（完整字段、方法、注解）
   - P2 需要修改的类（具体新增哪些字段/方法）
   - pom.xml 新增依赖
   - DDL 变更（如果有）
3. **验证输出**：子 Agent 完成后，检查：
   - 所有 Java 文件是否已创建/修改在正确的包路径下
   - 扩展已有类时，是否保留了 P1 原有代码
   - P2 新增类的方法签名、注解、依赖注入是否正确
   - pom.xml 依赖是否完整
4. **更新进度**：将完成状态记录到进度清单。

### 4.3 进度清单模板

```markdown
## P2 进度追踪

| # | 模块 | 状态 | 新增类 | 扩展类 | 备注 |
|---|------|------|--------|--------|------|
| 0 | 根 pom.xml 注册新模块 | ⬜ | 0 | 1 | ★ 前置：注册5个新Maven模块 |
| 1 | agent-qr-common P2 | ⬜ | 10 | 0 | TraceId/MDC/DLQ/Caffeine/4事件 |
| 2 | agent-qr-user P2 | ⬜ | 0 | 3 | SysUser+2 DTO |
| 3 | agent-qr-auth P2 | ⬜ | 7 | 4 | UserPrincipal/Abac/RefreshToken |
| 4 | agent-qr-datasource ★ | ⬜ | 12 | 0 | Connector/Service/Scheduler |
| 5 | agent-qr-catalog ★ | ⬜ | 9 | 0 | 目录树/DomainRouter |
| 6 | agent-qr-data-quality ★ | ⬜ | 9 | 0 | 规则链/报告 |
| 7 | agent-qr-etl ★ | ⬜ | 7 | 0 | Normalizer/Converter |
| 8 | agent-qr-rag P2 | ⬜ | 13 | 5 | 混合检索/SSE/熔断/批量 |
| 9 | agent-qr-knowledge P2 | ⬜ | 0 | 10 | 软删除/PDF增强/DLQ集成 |
| 10 | agent-qr-compensation ★ | ⬜ | 5 | 0 | 孤儿扫描/物理删除 |
| 11 | agent-qr-statistics P2 | ⬜ | 2 | 5 | 满意度/Feedback |
| 12 | agent-qr-web P2 | ⬜ | 3 | 3 | AsyncV2/DlqRetry/Security/p2-schema |
```

**状态图例**：⬜ 未开始 → 🔄 进行中 → ✅ 已完成

---

## 五、第 0 步（前置）：根 pom.xml 注册 5 个新模块

> ★ 此步骤必须在所有子模块代码生成**之前**完成。

### 修改根 `pom.xml`

在 `<modules>` 中新增 5 个 P2 模块：

```xml
<modules>
    <module>agent-qr-common</module>
    <module>agent-qr-auth</module>
    <module>agent-qr-user</module>
    <module>agent-qr-rag</module>
    <module>agent-qr-knowledge</module>
    <module>agent-qr-statistics</module>
    <module>agent-qr-web</module>
    <!-- ★ P2 新增 -->
    <module>agent-qr-compensation</module>
    <module>agent-qr-datasource</module>
    <module>agent-qr-data-quality</module>
    <module>agent-qr-etl</module>
    <module>agent-qr-catalog</module>
</modules>
```

### 子 Agent 任务清单

- [ ] 在根 `pom.xml` 的 `<modules>` 中注册 5 个新模块

---

## 六、第 1 步：agent-qr-common P2 扩展

### 5.1 模块信息

| 属性 | 值 |
|------|-----|
| **包路径** | `org.example.agent_qr.common` |
| **P1 已有类** | Result, BusinessException, MybatisPlusConfig, SpringContextUtil, 5个Event类 |
| **P2 新增类 (10个)** | TraceIdFilter, MdcTaskDecorator, CaffeineConfig, DeadLetterQueue, DlqMessage, DlqMessageMapper, DocumentDeleteRequestedEvent, DataSyncCompletedEvent, DataQualityPassedEvent, DataETLedEvent |
| **P2 新增依赖** | `spring-boot-starter-aop`, `caffeine` |

### 5.2 pom.xml 新增依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### 5.3 类详细规格

#### TraceIdFilter [P2 NEW]
**路径**: `org.example.agent_qr.common.filter.TraceIdFilter`
- 继承 `OncePerRequestFilter`
- 从请求头 `X-Trace-Id` 提取 TraceId，若无则生成 16 位 UUID 短码
- `MDC.put("traceId", traceId)` + 响应头回写 `X-Trace-Id`
- finally 块清理 `MDC.clear()`

#### MdcTaskDecorator [P2 NEW]
**路径**: `org.example.agent_qr.common.executor.MdcTaskDecorator`
- 实现 `TaskDecorator`
- `decorate(Runnable)`: 复制当前线程 MDC → 工作线程，finally 清理

#### CaffeineConfig [P2 NEW]
**路径**: `org.example.agent_qr.common.config.CaffeineConfig`
- `@Configuration`，Bean `llmResponseCache()`：`Caffeine.newBuilder().maximumSize(10000).expireAfterWrite(1, TimeUnit.HOURS).build()`

#### DlqMessage 实体 [P2 NEW]
**路径**: `org.example.agent_qr.common.dlq.entity.DlqMessage`
- `@Data`, `@TableName("dlq_message")`
- 字段：id, eventType, documentId, payload(TEXT), errorMsg, retryCount(0), nextRetryAt, status(PENDING/DEAD), createTime

#### DeadLetterQueue [P2 NEW]
**路径**: `org.example.agent_qr.common.dlq.DeadLetterQueue`
- `@Component`, 注入 `DlqMessageMapper`
- 配置：`maxRetries`(4), `backoffBase`(3)
- `enqueue(eventType, docId, payload, error)`: 写入死信队列，首次重试延迟=backoffBase秒
- `calcBackoffSeconds(retryCount)`: 3^(retryCount+1) 秒 → 3s→9s→27s→81s
- `updateRetryResult(msgId, success, error)`: 成功删除，失败递增retryCount+更新nextRetryAt，超限标记DEAD

#### DlqMessageMapper [P2 NEW]
**路径**: `org.example.agent_qr.common.dlq.DlqMessageMapper`
- 继承 `BaseMapper<DlqMessage>`
- `selectPendingRetries(LocalDateTime now)`: 查询到期PENDING记录
- `updateRetry(id, retryCount, nextRetryAt, error)`, `updateStatus(id, status, error)`

#### 新增事件类 [P2 NEW]
**路径**: `org.example.agent_qr.common.event`
- `DocumentDeleteRequestedEvent`: documentId, chunkIds, chromaIds, filePath — knowledge发布→compensation监听
- `DataSyncCompletedEvent`: datasourceId, rawData, syncBatchId — datasource发布→data-quality监听
- `DataQualityPassedEvent`: report, passedData, syncBatchId — data-quality发布→etl监听
- `DataETLedEvent`: domain, sourceName, entityCount, syncBatchId — etl发布→catalog监听
- 使用 `@Data`，建议设计为普通POJO（不继承ApplicationEvent），Spring支持发布任意对象

### 5.4 子 Agent 任务清单

- [ ] 创建 TraceIdFilter / MdcTaskDecorator / CaffeineConfig
- [ ] 创建 DlqMessage / DeadLetterQueue / DlqMessageMapper
- [ ] 创建 4 个新事件类
- [ ] 更新 pom.xml (caffeine + spring-boot-starter-aop)

---

## 六、第 2 步：agent-qr-user P2 扩展

### 6.1 模块信息

| 属性 | 值 |
|------|-----|
| **包路径** | `org.example.agent_qr.user` |
| **P1 已有类** | SysUser, SysUserMapper, MyMetaObjectHandler, CreateUserDTO, UpdateUserDTO, AdminController |
| **P2 扩展类 (3个)** | SysUser(新增ABAC字段), CreateUserDTO(新增ABAC字段), UpdateUserDTO(新增ABAC字段) |
| **P2 新增依赖** | 无 |

### 6.2 变更内容

#### SysUser [P2 MODIFY] — 新增 4个 ABAC 字段
```java
private String department;          // 所属部门(HR/FINANCE/RD/SALES/COMMON)
private Integer clearanceLevel;     // 数据密级(0=公开/1=内部/2=机密/3=绝密)
private String allowedDomains;      // 允许访问的业务域(逗号分隔)
private String title;               // 职级(employee/manager/director)
```
每个字段添加 `@TableField` 注解。**保留所有 P1 字段不变。**

#### CreateUserDTO / UpdateUserDTO [P2 MODIFY]
新增相同 4 个字段。

### 6.3 子 Agent 任务清单

- [ ] SysUser 新增 4 个 ABAC 字段
- [ ] CreateUserDTO / UpdateUserDTO 各新增 4 个字段
- [ ] 确认 pom.xml 无需变更

---

## 七、第 3 步：agent-qr-auth P2 扩展

### 7.1 模块信息

| 属性 | 值 |
|------|-----|
| **包路径** | `org.example.agent_qr.auth` |
| **P1 已有类** | AuthController, AuthService/AuthServiceImpl, PasswordUtil, JwtUtil, JwtAuthenticationFilter, LoginDTO/RegisterDTO/LoginVO |
| **P2 新增类 (7个)** | UserPrincipal, AbacEvaluator, AbacAccessDeniedHandler, RefreshTokenService, TokenRefresh, TokenRefreshMapper, TokenPair |
| **P2 扩展类 (4个)** | JwtUtil(新增3方法+2属性), JwtAuthenticationFilter(注入UserPrincipal), SecurityConfig(@EnableMethodSecurity), AuthController(登录返回双Token+refresh端点) |
| **P2 新增依赖** | 无（ABAC纯内存，双Token基于已有jjwt） |

### 7.2 类详细规格

#### UserPrincipal [P2 NEW]
**路径**: `org.example.agent_qr.auth.principal.UserPrincipal`
- `@Data`，字段：userId, username, role, department, clearanceLevel, `List<String> allowedDomains`, title
- `fromClaims(Claims claims)`: 从JWT解析，allowedDomains从逗号分隔字符串→List
- `isAdmin()`: role=="admin"
- `hasDomainAccess(String domain)`: allowedDomains包含domain 或 domain==department
- `hasClearance(int resourceLevel)`: clearanceLevel >= resourceLevel

#### AbacEvaluator [P2 NEW]
**路径**: `org.example.agent_qr.auth.evaluator.AbacEvaluator`
- `@Component("abac")`, `@Slf4j`，注入 `DocumentMapper`
- `canQueryDomain(user, domain)`: admin→允许，allowedDomains含domain→允许，department==domain→允许
- `canAccessDocument(user, documentId)`: admin→允许，密级检查+域检查（查Document表获取sensitivityLevel和domain）
- `canUploadToDomain(user, domain)`: canQueryDomain + 职级要求 manager/director
- `canDeleteDocument(user, documentId)`: canAccessDocument + 职级要求 director
- `canModifyUser(user, targetUserId)`: admin→所有，本人→允许改自己
- `canManageDatasource(user)`: 仅admin
- 所有拒绝分支记录 `log.warn` 结构化日志

#### JwtUtil 扩展 [P2 MODIFY]
在P1基础上新增：
- 属性：`accessExpiration`(1800s), `refreshExpiration`(604800s)
- `generateAccessToken(SysUser user)`: Payload含userId/role/department/clearanceLevel/allowedDomains/title
- `generateRefreshToken(SysUser user)`: Payload仅含userId/tokenType="refresh"，7天过期
- `parseUserPrincipal(String token)`: 解析JWT→返回UserPrincipal
- 保留P1的generateToken/getUsernameFromToken/validateToken

#### JwtAuthenticationFilter 改造 [P2 MODIFY]
- Token验证通过后，调用 `jwtUtil.parseUserPrincipal(token)` 替代原SysUser查询
- principal设为UserPrincipal实例
- allowedDomains编码为 `SimpleGrantedAuthority("DOMAIN_"+d)`
- 不再注入SysUserMapper

#### AbacAccessDeniedHandler [P2 NEW]
**路径**: `org.example.agent_qr.auth.handler.AbacAccessDeniedHandler`
- `@RestControllerAdvice`
- `@ExceptionHandler(AccessDeniedException.class)`: 提取UserPrincipal→构建审计日志JSON(MDC traceId)→`log.warn`→返回403+结构化JSON

#### TokenRefresh 实体 [P2 NEW]
**路径**: `org.example.agent_qr.auth.entity.TokenRefresh`
- `@Data`, `@TableName("token_refresh")`
- 字段：id, userId, token(TEXT), revoked(false), createTime, expireTime

#### TokenRefreshMapper [P2 NEW]
**路径**: `org.example.agent_qr.auth.mapper.TokenRefreshMapper`
- 继承 `BaseMapper<TokenRefresh>`
- `selectByToken(String token)`: 查询未撤销Token
- `revokeByUserId(Long userId)`: 撤销用户所有Token

#### TokenPair [P2 NEW]
**路径**: `org.example.agent_qr.auth.dto.TokenPair`
- `@Data @AllArgsConstructor`，字段：accessToken, refreshToken, expiresIn(1800)

#### RefreshTokenService [P2 NEW]
**路径**: `org.example.agent_qr.auth.service.RefreshTokenService`
- `issueTokens(SysUser user)`: 签发双Token→RefreshToken写入DB→返回TokenPair
- `refresh(String refreshToken)`: 验证有效性→查DB未撤销→删除旧Refresh(轮换)→签发新令牌对
- `revoke(Long userId)`: 撤销用户所有RefreshToken

#### AuthController 改造 [P2 MODIFY]
- `POST /api/auth/login` 改造：返回双Token(TokenPair)替代原单Token
- 新增 `POST /api/auth/refresh`: 接收RefreshDTO→refreshTokenService.refresh()→返回新TokenPair

#### ABAC 注解集成说明
以下注解在对应Controller位于的模块步骤中实际标注：
- KnowledgeController(第9步): `@PreAuthorize("@abac.canAccessDocument(principal, #id)")` / `canUploadToDomain` / `canDeleteDocument`
- ChatController(第8步): `@PreAuthorize("@abac.canQueryDomain(principal, #request.domain)")`
- AdminController(第2步): `@PreAuthorize("@abac.canModifyUser(principal, #id)")` / `canManageDatasource`

### 7.3 子 Agent 任务清单

- [ ] 创建 UserPrincipal / AbacEvaluator / AbacAccessDeniedHandler
- [ ] 创建 TokenRefresh / TokenRefreshMapper / TokenPair / RefreshTokenService
- [ ] 改造 JwtUtil(新增generateAccessToken/generateRefreshToken/parseUserPrincipal)
- [ ] 改造 JwtAuthenticationFilter(注入UserPrincipal替代SysUser)
- [ ] 改造 AuthController(双Token+refresh端点)
- [ ] 确认pom.xml无需变更

---

## 八、第 4 步：agent-qr-datasource ★ 新模块

### 8.1 模块信息

| 属性 | 值 |
|------|-----|
| **Maven GAV** | `org.example:agent-qr-datasource:0.0.1-SNAPSHOT` |
| **包路径** | `org.example.agent_qr.datasource` |
| **依赖** | `agent-qr-common`, `spring-boot-starter-web`, `aws-java-sdk-s3`, `mysql-connector-j` |
| **类型** | P2 全新模块，需从零创建目录+pom.xml+全部类 |

### 8.2 包结构

```
agent-qr-datasource/src/main/java/org/example/agent_qr/datasource/
├── connector/
│   ├── DataSourceConnector.java      ← 策略接口(getType/testConnection/fullSync/incrementalSync)
│   ├── JdbcConnector.java
│   ├── RestApiConnector.java
│   └── S3Connector.java
├── service/
│   └── DataSourceService.java        ← CRUD+连通性测试+触发同步
├── scheduler/
│   └── SyncScheduler.java            ← 同步调度器
├── entity/
│   └── DataSourceConfig.java         ← 数据源配置实体
├── mapper/
│   └── DataSourceMapper.java
└── dto/
    ├── ConnectionTestResult.java
    ├── SyncResult.java
    └── SyncContext.java
```

### 8.3 类详细规格

#### DataSourceConnector 策略接口
```java
public interface DataSourceConnector {
    String getType();  // JDBC / REST / S3
    ConnectionTestResult testConnection(Map<String, Object> config);
    SyncResult fullSync(SyncContext context);
    SyncResult incrementalSync(SyncContext context, String lastCursor);
}
```

#### JdbcConnector
- getType()→"JDBC"
- testConnection: DriverManager.getConnection→获取DatabaseMetaData→返回延迟/产品名/版本
- fullSync: 获取tableNames→遍历SELECT * FROM {table}→逐行转LinkedHashMap
- incrementalSync: SELECT * WHERE cursorField > ? ORDER BY cursorField ASC→更新游标

#### RestApiConnector
- getType()→"REST"，使用RestTemplate
- testConnection: HTTP HEAD→检查2xx
- fullSync: 循环分页GET→从响应头X-Next-Cursor获取游标→合并结果

#### S3Connector
- getType()→"S3"，使用AmazonS3客户端
- testConnection: listObjectsV2请求1个对象验证
- fullSync: 分页列出对象→过滤支持格式(pdf/docx/txt/md/csv/json)→下载内容
- incrementalSync: lastCursor=ISO8601→过滤lastModified>cursorTime→newCursor=最新时间戳

#### DataSourceConfig 实体
- `@Data @TableName("data_source_config")`
- 字段：id, sourceName, sourceType, domain, syncStrategy(FULL/INCREMENTAL), cursorField, lastCursor, connectionConfig(TEXT/JSON), fieldMapping(TEXT/JSON), status(ACTIVE/INACTIVE/ERROR), totalSynced, lastSyncAt, createTime, updateTime

#### DataSourceMapper
- 继承BaseMapper + updateSyncResult(id, lastCursor, totalRows, lastSyncAt) + updateStatus(id, status) + selectAllActive()

#### DTO类
- ConnectionTestResult: success, latencyMs, dbProduct, dbVersion, errorMsg
- SyncResult: totalRows, rawData(`List<Map<String,Object>>`), nextCursor
- SyncContext: datasourceId, config, syncBatchId(UUID)

#### DataSourceService
- `@Service`，注入DataSourceMapper+Map<String, DataSourceConnector>
- CRUD方法 + testConnection(Long id) + triggerSync(Long id)

#### SyncScheduler
- `@Component`, 注入DataSourceMapper+connectorMap+ApplicationEventPublisher
- scheduleSync(Long datasourceId): 查配置→获取Connector→testConnection→按策略full/incremental→updateSyncResult→发布DataSyncCompletedEvent

### 8.4 子 Agent 任务清单

- [ ] 创建agent-qr-datasource/目录+pom.xml
- [ ] 创建 4个Connector(接口+3实现) + 3个DTO + DataSourceConfig + DataSourceMapper + DataSourceService + SyncScheduler

---

## 九、第 5 步：agent-qr-catalog ★ 新模块

### 9.1 模块信息

| 属性 | 值 |
|------|-----|
| **Maven GAV** | `org.example:agent-qr-catalog:0.0.1-SNAPSHOT` |
| **包路径** | `org.example.agent_qr.catalog` |
| **依赖** | `agent-qr-common`, `agent-qr-datasource`, `agent-qr-knowledge`, `spring-boot-starter-web` |
| **类型** | P2 全新模块 |

> **依赖说明**：KnowledgeCatalogService.getCatalogTree() 需要注入 `DocumentMapper` 和 `ChunkMapper`（来自 `agent-qr-knowledge`），因此必须依赖 knowledge 模块。

### 9.2 包结构

```
agent-qr-catalog/src/main/java/org/example/agent_qr/catalog/
├── service/
│   └── KnowledgeCatalogService.java   ← 三级目录树管理
├── router/
│   └── DomainRouter.java              ← 关键词域路由器(P2)
├── entity/
│   ├── CatalogTree.java (List<DomainNode> domains)
│   ├── DomainNode.java (domainName, sourceCount, List<SourceNode> sources)
│   ├── SourceNode.java (sourceId, sourceName, sourceType, lastSyncAt, totalSynced, List<EntityNode> entities)
│   └── EntityNode.java (entityName, entityType[TABL/FILE/API], recordCount)
└── dto/
    └── DomainRoutingResult.java       ← matchedDomains, matchedEntities, fallbackToGlobal
```

### 9.3 类详细规格

#### KnowledgeCatalogService
- `@Service`，注入DataSourceMapper+DocumentMapper+ChunkMapper
- `getCatalogTree()`: 查ACTIVE数据源→按domain分组→构建DomainNode→填充SourceNode→获取EntityNode列表
- `onDataETLed(DataETLedEvent)`: `@EventListener @Async`→更新目录索引

#### DomainRouter (关键词方式，P2)
- `@Component`，注入KnowledgeCatalogService
- `route(String query)`: 分词(extractKeywords)→获取CatalogTree→遍历计算域匹配分数(域名+0.5/源名+0.3/实体名+0.4)→score>0加入matchedDomains→空则fallbackToGlobal→返回DomainRoutingResult
- `buildRetrievalFilter(DomainRoutingResult routing)`: fallback→空Map，否则→`Map.of("domain", "HR,FINANCE")`

#### DomainRoutingResult
- 字段：`Map<String,Double> matchedDomains`, `List<String> matchedEntities`, `boolean fallbackToGlobal`
- `getPrimaryDomain()`: 返回最高分域
- `static fallback()`: 返回fallbackToGlobal=true的空结果

### 9.4 子 Agent 任务清单

- [ ] 创建agent-qr-catalog/目录+pom.xml
- [ ] 创建 4个实体(CatalogTree/DomainNode/SourceNode/EntityNode)
- [ ] 创建 KnowledgeCatalogService / DomainRouter / DomainRoutingResult

---

## 十、第 6 步：agent-qr-data-quality ★ 新模块

### 10.1 模块信息

| 属性 | 值 |
|------|-----|
| **Maven GAV** | `org.example:agent-qr-data-quality:0.0.1-SNAPSHOT` |
| **包路径** | `org.example.agent_qr.dataquality` |
| **依赖** | `agent-qr-common`, `juniversalchardet`, `spring-boot-starter` |
| **类型** | P2 全新模块 |

### 10.2 包结构

```
agent-qr-data-quality/src/main/java/org/example/agent_qr/dataquality/
├── checker/
│   └── DataQualityChecker.java       ← 规则链引擎+阻断判定
├── rule/
│   ├── QualityRule.java              ← 接口(getName/evaluate)
│   ├── CompletenessRule.java        ← 完整性检查
│   ├── EncodingRule.java            ← 编码检查(CharsetDetector)
│   └── FormatRule.java              ← 格式检查(日期/数字/百分比)
├── entity/
│   ├── QualityReport.java           ← batchId/total/pass/fail/rate/blocked/failures
│   ├── RuleResult.java              ← passed/reason+静态工厂pass()/fail()
│   └── QualityFailure.java          ← ruleName/recordIndex/reason
└── util/
    └── CharsetDetector.java         ← juniversalchardet+回退编码序列
```

### 10.3 类详细规格

#### QualityRule 接口
```java
public interface QualityRule {
    String getName();
    RuleResult evaluate(Map<String, Object> record);
}
```

#### 三个规则实现
- **CompletenessRule**: getName()→"完整性", 检查content/text是否为空
- **EncodingRule**: getName()→"编码", 注入CharsetDetector, 遍历所有String字段检测非UTF-8
- **FormatRule**: getName()→"格式", 检查日期(yyyy-MM-dd)/数字(合法Decimal)/百分比([0,100])

#### CharsetDetector
- 使用juniversalchardet: 置信度≥0.8→返回检测编码; <0.8→回退尝试[UTF-8,GBK,GB2312,ISO-8859-1,Windows-1252]

#### DataQualityChecker
- `@Component`, 注入`List<QualityRule>`, 配置blockThreshold(0.5)
- `check(batchId, rawData)`: 规则链顺序执行(完整性→编码→格式)→跨记录MD5去重→统计passCount/failCount→passRate<blockThreshold→setBlocked(true)→返回QualityReport

### 10.4 子 Agent 任务清单

- [ ] 创建agent-qr-data-quality/目录+pom.xml
- [ ] 创建 QualityRule接口+3个规则实现+CharsetDetector
- [ ] 创建 DataQualityChecker+QualityReport+RuleResult+QualityFailure

---

## 十一、第 7 步：agent-qr-etl ★ 新模块

### 11.1 模块信息

| 属性 | 值 |
|------|-----|
| **Maven GAV** | `org.example:agent-qr-etl:0.0.1-SNAPSHOT` |
| **包路径** | `org.example.agent_qr.etl` |
| **依赖** | `agent-qr-common`, `agent-qr-datasource`, `spring-boot-starter` |
| **类型** | P2 全新模块 |

### 11.2 包结构

```
agent-qr-etl/src/main/java/org/example/agent_qr/etl/
├── enums/
│   └── DataType.java                    ← STRUCTURED/SEMI_STRUCTURED/UNSTRUCTURED
├── entity/
│   ├── CanonicalRecord.java            ← sourceSystem/domain/dataType/canonicalText/metadata/datasourceId/syncBatchId
│   └── FieldMapping.java              ← canonicalField/sourceField/displayName/template/unit/transformRule/dictMapping/priority/status
├── engine/
│   └── FieldMappingEngine.java         ← apply(源字段→Canonical字段)+字典翻译+格式转换
├── converter/
│   └── StructuredDataConverter.java    ← 结构化数据→自然语言段落(模板替换)
└── normalizer/
    └── DataNormalizer.java             ← 分类→映射→转换→CanonicalRecord输出
```

### 11.3 类详细规格

#### DataType 枚举
STRUCTURED(结构化/数据表), SEMI_STRUCTURED(半结构/JSON), UNSTRUCTURED(非结构/文件)

#### CanonicalRecord
sourceSystem, domain, dataType(DataType枚举), canonicalText(标准化文本), metadata(Map), datasourceId, syncBatchId

#### FieldMapping
canonicalField(目标标准字段), sourceField(源字段), displayName(中文), template("{字段中文名}为{值}{单位}"), unit, transformRule(DATE_TO_CHINESE/MONEY_FORMAT/PERCENTAGE), dictMapping(Map), priority(int), status(ACTIVE/INACTIVE)

#### FieldMappingEngine
- `apply(rawRecord, config)`: 获取config的fieldMappings→逐字段从rawRecord提取sourceField→存入canonicalField
- `applyDictionary(rawValue, dictMapping)`: 字典翻译(D01→研发部)
- `applyFormat(value, field)`: DATE_TO_CHINESE→"2024年1月15日", MONEY_FORMAT→"15,000", PERCENTAGE→"85.0%"

#### StructuredDataConverter
- `convert(mappedRecord, config)`: 添加段落标题+按priority排序+逐字段应用模板生成自然语言+用"，"连接→返回完整段落
- 输出示例: "【HR数据库】员工张三，所属部门为研发部，月薪15,000元，入职日期为2024年1月15日。"

#### DataNormalizer
- `normalize(rawData, config)`: 遍历→classify分类(JDBC→STRUCTURED, 含_file_type→UNSTRUCTURED)→fieldMappingEngine.apply→按dataType生成canonicalText→构建CanonicalRecord→返回List

### 11.4 子 Agent 任务清单

- [ ] 创建agent-qr-etl/目录+pom.xml
- [ ] 创建 DataType/CanonicalRecord/FieldMapping
- [ ] 创建 FieldMappingEngine/StructuredDataConverter/DataNormalizer

---

## 十二、第 8 步：agent-qr-rag P2 扩展

### 12.1 模块信息

| 属性 | 值 |
|------|-----|
| **包路径** | `org.example.agent_qr.rag` |
| **P1 已有类** | LLMProvider, EmbeddingProvider, ProviderFactory, DeepSeekLLMProvider, DeepSeekEmbeddingProvider, ChromaRetriever, ChatQueryService, ConversationService, PromptTemplate, Conversation, Message, 相关Mapper, ChatController, RetrievedDocument |
| **P2 新增类 (13个)** | OllamaLLMProvider, OllamaEmbeddingProvider, HybridRetriever, BM25Retriever, RerankerService, LLMCircuitBreaker, StructuredFilterService, ChunkStructured, ChunkStructuredMapper, FilterCondition, BatchEmbeddingService |
| **P2 扩展类 (5个)** | LLMProvider(新增generateStream), EmbeddingProvider(新增embedBatch), ProviderFactory(新增Ollama+getFallback), ChatQueryService(新增askStream), ChatController(新增SSE端点+feedback) |
| **P2 新增依赖** | `lucene-core/queryparser/analyzers-smartcn`, `agent-qr-catalog` |

### 12.2 包结构（P2 新增部分）

```
agent-qr-rag/src/main/java/org/example/agent_qr/rag/
├── provider/
│   ├── [P2 MODIFY] LLMProvider.java              ← 新增 generateStream 默认方法
│   ├── [P2 MODIFY] EmbeddingProvider.java         ← 新增 embedBatch
│   ├── [P2 MODIFY] ProviderFactory.java           ← 新增Ollama注入+getFallbackLLMProvider
│   └── ollama/
│       ├── [P2 NEW] OllamaLLMProvider.java
│       └── [P2 NEW] OllamaEmbeddingProvider.java
├── retriever/
│   ├── [P2 NEW] HybridRetriever.java              ← 双路召回+RRF融合+Rerank精排
│   ├── [P2 NEW] BM25Retriever.java                ← Lucene内存索引+IK Analyzer
│   └── [P2 NEW] RerankerService.java              ← bge-reranker-v2-m3精排
├── filter/
│   ├── [P2 NEW] StructuredFilterService.java      ← MySQL B+树前置过滤
│   ├── [P2 NEW] FilterCondition.java
│   └── mapper/
│       └── [P2 NEW] ChunkStructuredMapper.java
├── circuitbreaker/
│   └── [P2 NEW] LLMCircuitBreaker.java            ← 状态机熔断(CLOSED→OPEN→HALF_OPEN)
├── embedding/
│   └── [P2 NEW] BatchEmbeddingService.java        ← BlockingQueue攒批
├── entity/
│   └── [P2 NEW] ChunkStructured.java
├── service/
│   └── [P2 MODIFY] ChatQueryService.java          ← 新增 askStream
└── controller/
    └── [P2 MODIFY] ChatController.java            ← SSE端点+feedback端点
```

### 12.3 类详细规格

#### LLMProvider 接口扩展 [P2 MODIFY]
新增 `default Flux<String> generateStream(List<ChatMessage> messages) { return Flux.empty(); }`

#### EmbeddingProvider 接口扩展 [P2 MODIFY]
新增 `List<float[]> embedBatch(List<String> texts);`

#### OllamaLLMProvider [P2 NEW]
- 实现LLMProvider，`@Component`
- 配置: baseUrl(`http://localhost:11434`), model(`qwen2.5:7b`)
- `generate(messages)`: 调用Ollama `/api/chat` 同步接口
- `generateStream(messages)`: 调用Ollama `/api/chat` 流式接口→返回Flux<String>

#### OllamaEmbeddingProvider [P2 NEW]
- 实现EmbeddingProvider，`@Component`
- 配置: baseUrl, model(`nomic-embed-text`)
- `embed(text)`: 调用 `/api/embeddings`→float[]
- `embedBatch(texts)`: 循环调用(Ollama原生不支持批量)

#### ProviderFactory 扩展 [P2 MODIFY]
- 新增注入: `@Autowired(required=false) OllamaLLMProvider` / `OllamaEmbeddingProvider`
- 改造getLLMProvider/getEmbeddingProvider支持ollama切换
- 新增 `getFallbackLLMProvider()`: 返回DeepSeek作为降级Provider。实现需处理 DeepSeek 不可用的边界情况：
```java
public LLMProvider getFallbackLLMProvider() {
    if (deepSeekLLMProvider != null) {
        return deepSeekLLMProvider;
    }
    log.error("降级链路不可用：DeepSeek Provider 未配置");
    throw new BusinessException("所有 LLM Provider 均不可用");
}
```

#### HybridRetriever — 混合检索器 [P2 NEW]
- `@Component`, 注入ChromaRetriever/BM25Retriever/RerankerService/StructuredFilterService/DomainRouter
- 配置: semanticWeight(0.6), keywordWeight(0.4), wideTopK(20), finalTopK(5)
- `hybridSearch(query, queryEmbedding, routing, filterConditions)`:
  1. Step0: 结构化过滤→candidateChunkIds
  2. Step1: 双路宽召回(并行)→语义Top20 + BM25 Top20
  3. Step2: RRF加权融合去重→score = w1/(k+rank_semantic) + w2/(k+rank_bm25), k=60
  4. Step3: Rerank精排→rerankerService.rerank(query, fused, finalTopK)→返回Top5

#### BM25Retriever — Lucene关键词检索 [P2 NEW]
- `@Component`, 注入ChunkMapper
- `@PostConstruct buildIndex()`: 从MySQL加载全量READY切片→Lucene内存索引
- `buildIndexAsync()`: `@Async("indexBuilderExecutor")`+`@EventListener(ApplicationReadyEvent)`→分页加载磁盘索引
- `keywordSearch(query, topK)`: IK Analyzer分词→QueryParser→返回List<RetrievedDocument>
- `addToIndex(chunk)` / `removeFromIndex(chunkId)` / `onEmbeddingCompleted(event)`: 增量更新

#### RerankerService — 精排 [P2 NEW]
- `rerank(query, candidates, topK)`: 候选≤topK直接返回, 否则调用bge-reranker-v2-m3计算query-doc相关性分数→降序取TopK

#### LLMCircuitBreaker — 熔断器 [P2 NEW]
- `@Component`, 内部枚举State{CLOSED,OPEN,HALF_OPEN}, AtomicInteger failureCount, volatile openTimestamp
- 配置: failureThreshold(3), openDurationMs(30000)
- `getActiveProvider()`: CLOSED→默认, OPEN→检查时长→HALF_OPEN或降级, HALF_OPEN→探测
- `recordSuccess()`: HALF_OPEN→CLOSED重置; `recordFailure()`: CLOSED达阈值→OPEN, HALF_OPEN→OPEN

#### StructuredFilterService — MySQL前置过滤 [P2 NEW]
- `filterChunkIds(domain, conditions)`: 按fieldType分派Mapper(NUMBER/DATE/ENUM/STRING)→多条件交集AND→截断500条→返回chunk_id列表

#### ChunkStructured 实体 [P2 NEW]
- `@Data @TableName("kb_chunk_structured")`
- 字段: id, chunkId, domain, fieldName, fieldValue, numericValue, dateValue, fieldType

#### ChunkStructuredMapper [P2 NEW]
- 继承BaseMapper<ChunkStructured>
- selectChunkIdsByNumberRange/DateRange/StringValue → 每条SQL LIMIT 500

#### BatchEmbeddingService — 批量向量化攒批 [P2 NEW]
- `@Component`, 注入ProviderFactory
- `BlockingQueue<ChunkEmbedTask>`容量2000, 配置batchSize(32)/batchTimeoutMs(100)
- `@PostConstruct startConsumers()`: 启动N个消费者(N=CPU核数)
- `submit(Chunk chunk)`: 提交到攒批队列→返回CompletableFuture<float[]>
- `consumeBatch()`: poll任务→攒满OR超时→executeBatch(batch)
- `executeBatch(batch)`: 一次API处理整批→失败降级逐条重试
- 内部record `ChunkEmbedTask(Chunk chunk, CompletableFuture<float[]> future)`

#### ChatQueryService 扩展 [P2 MODIFY]
新增 `askStream(query, conversationId, userId, SseEmitter)`:
1. 会话管理(无则新建) 2. 保存用户提问 3. Embedding+hybridRetriever混合检索 4. 构建Prompt 5. circuitBreaker.getActiveProvider()+generateStream 6. token→SSE push("token") 7. error→recordFailure+push("error") 8. complete→recordSuccess+push("done", {conversationId,sources}) 9. 保存AI回答+发布AnswerGeneratedEvent
- 新增私有方法 `sendSseEvent(emitter, eventName, data)`, `saveMessage(...)`
- 保留P1 `ask()` 方法不变

#### ChatController 扩展 [P2 MODIFY]
- 改造 `POST /api/chat/ask` → 返回SseEmitter(超时5分钟)→委托askStream
- 新增 `POST /api/chat/feedback/{messageId}`: 接收feedback/reason→委托FeedbackService

### 12.4 DDL — kb_chunk_structured 表

```sql
CREATE TABLE IF NOT EXISTS kb_chunk_structured (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chunk_id BIGINT NOT NULL,
    domain VARCHAR(32),
    field_name VARCHAR(64),
    field_value VARCHAR(255),
    numeric_value DECIMAL(18,4),
    date_value DATE,
    field_type VARCHAR(16) COMMENT 'NUMBER/DATE/ENUM/STRING',
    INDEX idx_domain_field_number (domain, field_name, numeric_value),
    INDEX idx_domain_field_date (domain, field_name, date_value),
    INDEX idx_chunk_id (chunk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 12.5 子 Agent 任务清单

- [ ] 改造 LLMProvider/EmbeddingProvider 接口
- [ ] 创建 OllamaLLMProvider / OllamaEmbeddingProvider
- [ ] 改造 ProviderFactory(Ollama注入+getFallback)
- [ ] 创建 HybridRetriever(RRF融合) / BM25Retriever(Lucene) / RerankerService
- [ ] 创建 LLMCircuitBreaker(状态机)
- [ ] 创建 StructuredFilterService / FilterCondition / ChunkStructured / ChunkStructuredMapper
- [ ] 创建 BatchEmbeddingService(BlockingQueue攒批)
- [ ] 改造 ChatQueryService(askStream+sendSseEvent+saveMessage)
- [ ] 改造 ChatController(SSE端点+feedback端点)
- [ ] 更新pom.xml(lucene+agent-qr-catalog)

---

## 十三、第 9 步：agent-qr-knowledge P2 扩展

### 13.1 模块信息

| 属性 | 值 |
|------|-----|
| **包路径** | `org.example.agent_qr.knowledge` |
| **P1 已有类** | KnowledgeController, DocumentCommandService, DocumentQueryService, FileStorageService, DocumentParserService, PdfParser(基础), DocxParser(基础), TextParser, TextSplitter, DocumentStatus, Document(基础), Chunk(基础), 相关Mapper, DocumentParseListener, ChunkEmbeddingListener |
| **P2 扩展类 (10个)** | Document(新增domain/sensitivity/deleted), Chunk(新增deleted), DocumentMapper(新增softDelete), ChunkMapper(新增4方法), DocumentCommandService(新增requestDeleteDocument+改造uploadDocument), PdfParser(流式+表格+OCR), DocxParser(表格Markdown), DocumentParseListener(DLQ), ChunkEmbeddingListener(DLQ+BatchEmbedding) |
| **P2 新增依赖** | `tika-parsers` |
| **★ 禁止依赖** | `agent-qr-compensation`（软删除通过发布DocumentDeleteRequestedEvent事件解耦） |

### 13.2 变更详情

#### Document 实体 [P2 MODIFY]
新增: private String domain; private Integer sensitivityLevel; private String sensitivityLabel; private Integer deleted(0/1)
P1的errorMsg字段保留不变。不要使用@TableLogic。

#### Chunk 实体 [P2 MODIFY]
新增: private Integer deleted(0/1)

#### DocumentMapper [P2 MODIFY]
新增: `@Update("UPDATE kb_document SET deleted = 1 WHERE id = #{documentId}") int softDelete(Long documentId);`
P1的updateStatus/updateErrorMsg保留不变。

#### ChunkMapper [P2 MODIFY]
新增:
- `softDeleteByDocumentId(Long documentId)`: UPDATE kb_chunk SET deleted=1 WHERE document_id=?
- `selectChromaIdsByDocumentId(Long documentId)`: SELECT chroma_id FROM kb_chunk WHERE document_id=?
- `selectAllReadyChunks()`: SELECT * FROM kb_chunk WHERE status='READY' AND deleted=0
- `selectReadyChunksPaged(int offset, int limit)`: 分页查询就绪切片

#### DocumentCommandService [P2 MODIFY]
- **改造 uploadDocument**: 新增domain和sensitivityLevel参数，写入Document实体后保存。保留P1原有逻辑。
- **联动改造 KnowledgeController.upload()**：`POST /api/knowledge/upload` 需要同步新增 `domain` 和 `sensitivityLevel` 请求参数（从 `@RequestParam` 获取），并将这两个参数传递给 `DocumentCommandService.uploadDocument()`。
- **新增 requestDeleteDocument(Long documentId)**: 标记DELETING→收集chunkIds/chromaIds→发布DocumentDeleteRequestedEvent→立即返回。**不注入compensation的任何类！**
- P1的deleteDocument保留但标注@Deprecated

#### PdfParser [P2 MODIFY]
改造parse方法:
1. 文件>maxMemoryMb(256MB)→流式分支parseStreaming
2. 否则PDFBox逐页+Tika表格→Markdown
3. ocrEnabled且判定扫描件(平均每页<50字符)→Tesseract OCR
新增私有方法: parseStreaming, isScannedPdf, performOcr, extractTablesAsMarkdown

#### DocxParser [P2 MODIFY]
改造parse: 遍历IBodyElement→段落提取文本+XWPFTable→tableToMarkdown(Markdown表格)
新增私有方法 tableToMarkdown(XWPFTable)

#### DocumentParseListener [P2 MODIFY]
新增注入DeadLetterQueue。解析失败catch块→`deadLetterQueue.enqueue("PARSE", docId, payload, error)`

#### ChunkEmbeddingListener [P2 MODIFY]
新增注入DeadLetterQueue+BatchEmbeddingService。失败→DLQ入队。向量化步骤改为`batchEmbeddingService.submit(chunk)`攒批处理。

### 13.3 子 Agent 任务清单

- [ ] Document/Chunk 新增deleted字段；Document新增domain/sensitivity字段
- [ ] DocumentMapper 新增softDelete；ChunkMapper 新增4个方法
- [ ] DocumentCommandService 新增requestDeleteDocument+改造uploadDocument签名
- [ ] PdfParser 改造(流式+Tika表格+OCR)
- [ ] DocxParser 改造(tableToMarkdown)
- [ ] DocumentParseListener/ChunkEmbeddingListener DLQ+BatchEmbedding集成
- [ ] 更新pom.xml(tika-parsers)
- [ ] 确认**不依赖**agent-qr-compensation

---

## 十四、第 10 步：agent-qr-compensation ★ 新模块

### 14.1 模块信息

| 属性 | 值 |
|------|-----|
| **Maven GAV** | `org.example:agent-qr-compensation:0.0.1-SNAPSHOT` |
| **包路径** | `org.example.agent_qr.compensation` |
| **依赖** | `agent-qr-common`, `agent-qr-knowledge`, `langchain4j-chroma`, `spring-boot-starter` |
| **★ 关键** | 单向依赖knowledge，knowledge不依赖compensation（事件驱动解耦） |

### 14.2 包结构

```
agent-qr-compensation/src/main/java/org/example/agent_qr/compensation/
├── listener/
│   └── DocumentDeleteListener.java       ← ★ 事件驱动核心
├── service/
│   └── DocumentDeleteServiceV2.java      ← ChromaDB物理删除+重试
├── scanner/
│   └── OrphanVectorScanner.java          ← @Scheduled 每30分钟孤儿清理
├── entity/
│   └── DeleteTask.java
└── mapper/
    └── DeleteTaskMapper.java
```

### 14.3 类详细规格

#### DocumentDeleteListener ★ [P2 NEW]
- `@Component`, 注入DocumentMapper/ChunkMapper/DocumentDeleteServiceV2/DeadLetterQueue
- `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async("deleteExecutor")`
- `handleDocumentDeleteRequested(DocumentDeleteRequestedEvent event)`:
  1. MySQL逻辑删除: chunkMapper.softDeleteByDocumentId + documentMapper.softDelete
  2. ChromaDB物理删除: documentDeleteServiceV2.asyncPhysicalDelete(docId, chromaIds)
  3. 异常: deadLetterQueue.enqueue("DELETE", ...)

#### DocumentDeleteServiceV2 [P2 NEW]
- `@Service`, 注入ChromaVectorStore/DeleteTaskMapper/DeadLetterQueue
- `@Async("deleteExecutor")`
- `asyncPhysicalDelete(Long documentId, List<String> chromaIds)`:
  1. 创建DeleteTask记录(PENDING)
  2. chromaVectorStore.deleteByIds(chromaIds)
  3. 成功→updateStatus(DONE), 失败→incrementRetryCount+deadLetterQueue.enqueue

#### OrphanVectorScanner [P2 NEW]
- `@Component`, 注入ChunkMapper/ChromaVectorStore/DocumentMapper
- `@Scheduled(fixedDelay = 1800000)` scanAndCleanOrphanVectors():
  1. 获取ChromaDB所有document_id元数据
  2. MySQL中查询哪些已不存在或deleted=1
  3. 批量chromaVectorStore.deleteByMetadata("document_id", docId)

#### DeleteTask 实体 [P2 NEW]
- `@Data @TableName("delete_task")`
- 字段: id, documentId, chromaIds(TEXT/JSON数组), status(PENDING/DONE/FAILED), retryCount, createTime

#### DeleteTaskMapper [P2 NEW]
- 继承BaseMapper + updateStatus + incrementRetryCount

### 14.4 整体一致性保证链

```
用户删除 → KnowledgeController.delete → DocumentCommandService.requestDeleteDocument
  → 标记DELETING → 发布DocumentDeleteRequestedEvent → return 200
  → DocumentDeleteListener(@TransactionalEventListener+@Async)
    → MySQL逻辑删除(softDelete) → ChromaDB物理删除(asyncPhysicalDelete)
      → 失败: DLQ重试(3s→9s→27s→81s, 最多4次)
        → 耗尽: 标记DEAD
          → OrphanVectorScanner 最终兜底(@Scheduled 每30分钟)
```

### 14.5 子 Agent 任务清单

- [ ] 创建agent-qr-compensation/目录+pom.xml
- [ ] 创建 DocumentDeleteListener / DocumentDeleteServiceV2 / OrphanVectorScanner
- [ ] 创建 DeleteTask / DeleteTaskMapper

---

## 十五、第 11 步：agent-qr-statistics P2 扩展

### 15.1 模块信息

| 属性 | 值 |
|------|-----|
| **包路径** | `org.example.agent_qr.statistics` |
| **P1 已有类** | StatisticsQueryService, DailyStats, DailyStatsMapper, DashboardVO, StatisticsController, StatisticsUpdateListener |
| **P2 新增类 (2个)** | FeedbackService, FeedbackDTO |
| **P2 扩展类 (5个)** | DailyStats(新增positiveCount/negativeCount), DailyStatsMapper(新增2方法), DashboardVO(新增4字段), MessageMapper(rag模块,新增updateFeedback), StatisticsController(新增feedback端点) |
| **P2 新增依赖** | 无 |

### 15.2 变更详情

#### DailyStats [P2 MODIFY] — 新增
```java
private Integer positiveCount = 0;   // 点赞数
private Integer negativeCount = 0;   // 点踩数
```

#### DailyStatsMapper [P2 MODIFY] — 新增
```java
@Update("UPDATE stat_daily SET positive_count = positive_count + 1 WHERE stat_date = #{date}")
int incrementPositiveCount(@Param("date") LocalDate date);

@Update("UPDATE stat_daily SET negative_count = negative_count + 1 WHERE stat_date = #{date}")
int incrementNegativeCount(@Param("date") LocalDate date);
```

#### DashboardVO [P2 MODIFY] — 新增
```java
private Integer todayPositive;
private Integer todayNegative;
private Double satisfactionRate;     // positive/(positive+negative)
private Integer totalFeedbackCount;
```

#### StatisticsQueryService [P2 MODIFY]
注入MessageMapper(来自rag)。getDashboard()新增todayPositive/todayNegative/satisfactionRate/totalFeedbackCount统计项。

#### MessageMapper(rag模块) [P2 MODIFY]
```java
@Update("UPDATE chat_message SET feedback = #{feedback}, feedback_reason = #{reason} WHERE id = #{messageId}")
int updateFeedback(@Param("messageId") Long messageId, @Param("feedback") String feedback, @Param("reason") String reason);
```

#### FeedbackService [P2 NEW]
- `@Service`, 注入MessageMapper+DailyStatsMapper
- `submitFeedback(messageId, feedback, reason, userId)`:
  1. 校验消息存在且role=="assistant"
  2. messageMapper.updateFeedback
  3. positive→incrementPositiveCount / negative→incrementNegativeCount

#### StatisticsController [P2 MODIFY]
新增 `POST /api/statistics/feedback/{messageId}`: 接收FeedbackDTO→feedbackService.submitFeedback()

### 15.3 子 Agent 任务清单

- [ ] DailyStats/DailyStatsMapper/DashboardVO 扩展
- [ ] StatisticsQueryService 扩展(含满意度)
- [ ] MessageMapper(rag模块) 新增updateFeedback
- [ ] 创建 FeedbackService / FeedbackDTO
- [ ] StatisticsController 新增feedback端点
- [ ] 确认pom.xml无需变更

---

## 十六、第 12 步：agent-qr-web P2 扩展

### 16.1 模块信息

| 属性 | 值 |
|------|-----|
| **包路径** | `org.example.agent_qr` + `org.example.agent_qr.web` |
| **P1 已有类** | AgentQrApplication, GlobalExceptionHandler, CorsConfig, SecurityConfig(基础), AsyncConfig(双池), application.yml, application-p1.yml, p1-schema.sql |
| **P2 新增** | AsyncConfigV2, DlqRetryScheduler, application-p2.yml, p2-schema.sql |
| **P2 扩展** | SecurityConfig(@EnableMethodSecurity+ABAC路由), GlobalExceptionHandler(TraceId+AccessDenied), agent-qr-web/pom.xml(新增5依赖) |

### 16.2 agent-qr-web/pom.xml 新增依赖

> 根 pom.xml 的 `<modules>` 注册已在**第 0 步（前置）**完成，此处只处理 agent-qr-web 自身的依赖。

新增 5 个 P2 模块依赖：

### 16.3 agent-qr-web/pom.xml 新增依赖

新增 5 个 P2 模块依赖: agent-qr-compensation, agent-qr-datasource, agent-qr-data-quality, agent-qr-etl, agent-qr-catalog

### 16.4 类详细规格

#### AsyncConfigV2 — 四池隔离 [P2 NEW]
**路径**: `org.example.agent_qr.web.config.AsyncConfigV2`
- `@Configuration @EnableAsync implements AsyncConfigurer`
- Bean定义:
  - `parseExecutor`: core=2, max=4, queue=50, prefix="parse-", MdcTaskDecorator, CallerRunsPolicy
  - `chunkExecutor`: core=2, max=4, queue=100, prefix="chunk-", MdcTaskDecorator
  - `embedExecutor`: core=4, max=8, queue=200, prefix="embed-", MdcTaskDecorator
  - `deleteExecutor`: core=2, max=4, queue=50, prefix="delete-", MdcTaskDecorator
  - `indexBuilderExecutor`: core=2, max=2, prefix="index-builder-"
  - `statExecutor`: core=2, max=4, queue=50, prefix="stat-"
- **明确指令**：P1 的 `AsyncConfig` 必须重命名为 `AsyncConfigP1` 并标注 `@Deprecated`（或直接删除），`AsyncConfigV2` 作为唯一生效的线程池配置。**不能**使用 `@Primary` 方式共存，因为 `docProcessExecutor` Bean 名与 P1 重复会导致 Spring Bean 冲突。

#### DlqRetryScheduler — DLQ定时重试 [P2 NEW]
**路径**: `org.example.agent_qr.web.scheduler.DlqRetryScheduler`
- `@Component`, 注入DeadLetterQueue/DlqMessageMapper
- `@Autowired(required=false) DocumentParserService parserService`(来自knowledge)
- `@Autowired(required=false) ChunkEmbeddingListener chunkEmbeddingListener`(来自knowledge)
- `@Autowired(required=false) DocumentDeleteServiceV2 documentDeleteServiceV2`(来自compensation)
- `@Scheduled(fixedDelay = 30000) retryDeadLetters()`: 扫描到期PENDING记录→按eventType分派(PARSE→retryParse, CHUNK→retryChunk, EMBED→retryEmbed, DELETE→retryDelete)→成功updateRetryResult(true)→失败updateRetryResult(false)

#### SecurityConfig 改造 [P2 MODIFY]
- 新增 `@EnableMethodSecurity(prePostEnabled = true)`
- 路由更新: `/api/auth/refresh`→permitAll, `/api/catalog/**`→authenticated
- 保留CSRF禁用/无状态Session/JWT过滤器/BCryptPasswordEncoder

#### GlobalExceptionHandler [P2 MODIFY]
确认AccessDeniedException处理器存在。所有异常响应从MDC获取TraceId并返回。

### 16.5 application-p2.yml [P2 NEW]

**路径**: `agent-qr-web/src/main/resources/application-p2.yml`

包含配置节: JWT双Token(access-expiration:1800/refresh-expiration:604800), Ollama Provider(llm/embedding), 混合检索(semantic-weight/keyword-weight/wide-top-k/final-top-k), Reranker(bge-reranker-v2-m3), 熔断器(failure-threshold/open-duration-ms), 批量向量化(batch.size/timeout-ms), DLQ(max-retries/backoff-base), 数据质量(block-threshold), PDF解析(max-memory-mb/ocr.enabled), Caffeine缓存(max-size/ttl-hours)

### 16.6 p2-schema.sql [P2 NEW]

**路径**: `agent-qr-web/src/main/resources/db/p2-schema.sql`

汇聚全部P2 DDL:

**CREATE TABLE (5张)**: dlq_message, token_refresh, delete_task, data_source_config, kb_chunk_structured

**ALTER TABLE (5张)**:
- sys_user: ADD department, clearance_level, allowed_domains, title
- kb_document: ADD domain, sensitivity_level, sensitivity_label, deleted + 索引
- kb_chunk: ADD deleted + 索引
- chat_message: ADD feedback, feedback_reason
- stat_daily: ADD positive_count, negative_count

### 16.7 子 Agent 任务清单

- [ ] 创建 AsyncConfigV2.java(四池隔离+MdcTaskDecorator)
- [ ] 创建 DlqRetryScheduler.java(@Scheduled 30s+按eventType分派)
- [ ] 改造 SecurityConfig.java(@EnableMethodSecurity+更新路由)
- [ ] 改造 GlobalExceptionHandler.java(TraceId+AccessDeniedException)
- [ ] 创建 application-p2.yml
- [ ] 创建 p2-schema.sql(汇聚所有P2 DDL)
- [ ] 更新 agent-qr-web/pom.xml(新增5个P2模块依赖)
- [ ] 确认 `AgentQrApplication` 的 `@ComponentScan(basePackages = "org.example.agent_qr")` 包含所有新增模块的包路径（★ 5个新模块已在第0步注册）

---

## 十七、子 Agent 创建指南

当你（主控 Agent）为每个模块创建子 Agent 时，使用以下模板：

```
你是 agent-qr 项目的代码生成 Agent。你的任务是完成 [模块名] 模块的 P2 阶段全部代码实现。

## 上下文
- 项目：基于 LangChain4j 的 RAG 企业内部知识库问答系统
- Java 21 + Spring Boot 3.5.15 + MyBatis-Plus 3.x
- 基础包路径：org.example.agent_qr
- P2 阶段（增强）：在 P1 已完成代码基础上扩展

## P1 已有代码（可直接引用）
[列出该模块 P1 已有的类路径和关键内容]

## P2 需要修改的已有类（扩展，保留 P1 代码）
[列出需要扩展的类及具体变更：新增哪些字段、哪些方法、哪些注解]

## P2 需要新建的类（从零创建）
[列出需要创建的类及完整规格：字段、方法签名、注解、依赖注入]

## 跨模块依赖
[列出该模块引用的其他模块的类和 Maven 依赖]

## pom.xml 变更
[列出需要新增的 Maven 依赖]

## DDL 变更（如果有）
[列出本模块需要的 CREATE TABLE 或 ALTER TABLE 语句，将收敛到 p2-schema.sql]

## 要求
1. **扩展已有类时**：在原有文件基础上新增字段/方法/注解，保留所有 P1 代码不变
2. **创建新类时**：放在正确的包路径下，使用正确的包名
3. **新模块**：从零创建完整目录结构 + pom.xml + 所有类
4. 添加所有必要的 import 语句
5. 添加完整的 Javadoc 注释
6. 正确使用 Lombok(@Data/@Slf4j等)和 Spring(@Service/@Component/@Autowired等)
7. 异常处理：业务异常用 BusinessException，系统异常记录 log.error
8. 不实现标记为 P3 的功能
9. 完成后列出你创建/修改的所有文件路径
```

### 子 Agent 特殊注意事项

| 模块 | 特殊注意事项 |
|------|-------------|
| **知识库 (knowledge)** | ★ 禁止注入 compensation 的任何类。requestDeleteDocument 只发布事件，不调用删除服务。 |
| **补偿 (compensation)** | ★ 单向依赖 knowledge。使用 @TransactionalEventListener(phase=AFTER_COMMIT) 而非 @EventListener。 |
| **RAG** | HybridRetriever 注入 DomainRouter(来自 catalog)。ChatQueryService.askStream 方法需处理 SSE 各种事件类型(token/done/error)。 |
| **Web** | DlqRetryScheduler 必须放在 web 模块(非 common)，因为它需要注入各业务模块的类。AsyncConfigV2 需要和 P1 的 AsyncConfig 共存。 |

---

## 十八、P2 vs P3 边界说明

以下功能在 P2 阶段**不做**，留到 P3：

| 功能 | P3 实现类 | P2 替代方案 |
|------|----------|------------|
| CQRS 读写分离 | ReadWriteRoutingDataSource + ReadWriteDataSourceAspect | P2 使用单数据源 |
| Embedding 语义域路由 | DomainRouterV2 (Embedding 余弦相似度匹配) | P2 使用关键词匹配 DomainRouter |
| Embedding 维度管理 | EmbeddingDimensionManager (Collection 隔离+维度检测) | P2 手动管理 Collection（切换 Provider 时需手动修改 `application-p2.yml` 中的 `langchain4j.chroma.collection-name` 为不同的值，避免维度冲突） |
| Provider 切换决策优化 | 自动决策算法 | P2 通过 yml 配置手动切换 + 熔断降级 |

---

## 十九、验证检查清单

所有 12 个步骤完成后，主控 Agent 应验证：

### 代码层面
- [ ] 所有 Java 文件存在于正确的包路径下
- [ ] 扩展已有类时，P1 原有代码完整保留
- [ ] 所有 import 语句正确，无未解析的引用
- [ ] Lombok/Spring 注解使用正确
- [ ] 事务方法使用 @Transactional
- [ ] 异步方法对应的 @Async 线程池 Bean 名称正确

### 模块层面
- [ ] 5 个新模块的目录结构 + pom.xml 完整
- [ ] 各模块 pom.xml 依赖与本文档一致
- [ ] knowledge 模块**不依赖** compensation 模块 ★
- [ ] 跨模块引用均指向已存在的类

### 全局层面
- [ ] 根 pom.xml `<modules>` 包含全部 12 个模块
- [ ] agent-qr-web/pom.xml 依赖全部 11 个业务模块
- [ ] application-p2.yml 配置完整
- [ ] p2-schema.sql 汇聚全部 P2 DDL (5 CREATE + 10+ ALTER)
- [ ] 可执行 `mvn compile`（如环境允许）

### P2 核心功能验证要点
- [ ] ABAC: UserPrincipal.fromClaims 正确解析 JWT → AbacEvaluator 所有规则方法实现正确
- [ ] 双 Token: 签发+刷新+轮换+撤销流程完整
- [ ] SSE: ChatQueryService.askStream 事件推送正常(token/done/error 三种事件)
- [ ] 混合检索: RRF 融合公式正确(score=w1/(k+rank1)+w2/(k+rank2), k=60)
- [ ] LLM 熔断: 状态机 CLOSED→OPEN→HALF_OPEN→CLOSED 转换逻辑正确
- [ ] DLQ: 指数退避 3s→9s→27s→81s 计算正确，超 maxRetries(4) 标记 DEAD
- [ ] 软删除链路: knowledge(事件)→compensation(@TransactionalEventListener)→MySQL逻辑删→ChromaDB物理删→DLQ兜底
- [ ] 批量向量化: BlockingQueue 攒批+批量失败降级逐条重试
- [ ] 数据质量: 规则链顺序(完整性→编码→格式)，passRate<blockThreshold(0.5) 阻断
- [ ] ETL: FieldMappingEngine 字段映射+StructuredDataConverter 自然语言转换正确

---

> **文档版本**：v1.0
>
> **生成日期**：2026-06-24
>
> **输入文档**：系统详细设计说明书 v1.0、p2-tasks 文件夹全部内容、p1-prompt.md（结构参考）
