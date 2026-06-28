# P3 阶段代码生成 Prompt

> **版本**：v1.0
> **日期**：2026-06-28
> **前提**：P1 + P2 阶段已全部实现并编译通过
> **目标**：CQRS 读写分离 + Embedding 语义域路由 + 向量维度管理 + Provider 切换决策 + 前端全面增强

---

## 一、您的角色

您是一个 **P3 阶段主控 Agent（编排器）**。您的职责是：

1. **理解 P3 完整需求**：阅读本文档的全部内容，理解 5 个模块的 P3 变更范围
2. **按顺序推进**：严格遵循第三节定义的开发顺序，不得跳过或打乱步骤
3. **为每个模块创建子 Agent**：将本 Prompt 中该模块的完整规格传递给子 Agent
4. **验证子 Agent 输出**：检查类是否在正确的包路径、P1/P2 代码是否被保留、注解是否正确、依赖是否完整
5. **维护全局进度清单**：使用第四节定义的模板跟踪进度，每完成一个模块更新状态

**关键原则**：
- P3 是对 P1+P2 的**扩展**，不是重写。所有 P1/P2 已有代码必须保留
- 每个子 Agent 只负责**一个模块**，互不交叉
- 模块间有严格的依赖顺序，前置模块未完成不得启动后续模块

---

## 二、项目概览

### 2.1 项目信息

| 项目 | 说明 |
|------|------|
| 项目名称 | agent-qr（企业知识中枢） |
| 技术栈 | Java 21 + Spring Boot 3.5.15 + MyBatis-Plus 3.5.14 + LangChain4j 1.16.3 |
| 前端 | Vue 3 + TypeScript + Pinia + Vite |
| 构建工具 | Maven 多模块 |
| 根 POM | `D:\Javacode\agent-qr\pom.xml` |

### 2.2 P3 阶段目标（14 项改进）

| # | 改进项 | 说明 |
|---|--------|------|
| 1 | CQRS 读写分离 | 基于 `AbstractRoutingDataSource` + AOP，`@Transactional(readOnly)` 自动路由 |
| 2 | Embedding 语义域路由 | 替代 P2 关键词匹配，使用余弦相似度匹配用户问题到业务域 |
| 3 | 向量维度自动管理 | Collection 命名嵌入模型 ID，启动时检测维度不一致 |
| 4 | Provider 熔断自动切换 | 根据熔断器状态自动选择 LLM/Embedding Provider |
| 5 | P3 阶段配置 | `application-p3.yml` 统一管理读写分离、语义路由、Provider 决策参数 |
| 6 | 目录语义路由集成 | catalog 模块基于目录树动态生成域 Embedding，定时刷新 |
| 7 | WebSocket 双向通信 | 前端 SSE 升级为 STOMP over WebSocket |
| 8 | i18n 国际化 | `vue-i18n` 中英文语言包，替换全站硬编码中文 |
| 9 | 移动端响应式适配 | 断点系统 + Sidebar 抽屉化 + 表格卡片化 |
| 10 | ABAC 细粒度权限 | `v-permission` 指令，按钮/字段级权限控制 |
| 11 | 知识图谱可视化 | ECharts graph 力导向图，知识目录图谱视图 |
| 12 | 实时质检规则配置 | 规则 CRUD 页面 + 可视化规则编辑器 |
| 13 | ECharts 图表增强 | 时间范围拖拽、下钻、导出 PNG/PDF |
| 14 | 语音输入 | Web Speech API，中英文语音识别 |

### 2.3 P3 涉及模块

| # | 模块 | 类型 | 新类数 | 修改类数 |
|---|------|------|--------|----------|
| 1 | **agent-qr-common** | P1 扩展 | 2 | 1 |
| 2 | **agent-qr-web** | P1 扩展 | 2（含 1 配置） | 1 |
| 3 | **agent-qr-rag** | P1 扩展 | 3 | 4 |
| 4 | **agent-qr-catalog** | P2 扩展 | 1 | 2 |
| 5 | **agent-qr-web-frontend** | P2 扩展 | 10+（组件/composable） | 8+（页面/布局） |

**不涉及的模块**（P3 无变更）：`agent-qr-auth`、`agent-qr-user`、`agent-qr-knowledge`、`agent-qr-statistics`、`agent-qr-compensation`、`agent-qr-datasource`、`agent-qr-data-quality`、`agent-qr-etl`

### 2.4 模块依赖图

```
agent-qr-common (CQRS 基础组件)
    ├──→ agent-qr-web (CQRS Bean 装配 + application-p3.yml)
    │         └──→ agent-qr-rag (读取 P3 配置)
    │                   └──→ agent-qr-catalog (依赖 EmbeddingProvider)
    │
    └──→ agent-qr-web-frontend (依赖所有后端 API)
```

### 2.5 模块依赖规则

| 模块 | 可依赖的模块 | 禁止依赖 |
|------|-------------|----------|
| agent-qr-common | 无 | 任何业务模块 |
| agent-qr-web | common, auth, user, knowledge, rag, statistics, catalog, compensation, datasource, data-quality, etl | 无 |
| agent-qr-rag | common, user, auth, catalog | compensation, datasource, data-quality, etl |
| agent-qr-catalog | common, datasource, rag | 无 |
| agent-qr-web-frontend | —（前端项目） | — |

---

## 三、开发顺序

**严格按以下 5 步顺序执行，不得跳过或并行启动。**

```
步骤 1：agent-qr-common P3（CQRS 基础组件）
          ↓
步骤 2：agent-qr-web P3（P3 配置文件 + CQRS Bean 装配）
          ↓
步骤 3：agent-qr-rag P3（EmbeddingDimensionManager + DomainRouterV2 + ProviderDecisionEngine）
          ↓
步骤 4：agent-qr-catalog P3（DomainRouterV2 目录语义集成）
          ↓
步骤 5：agent-qr-web-frontend P3（前端全面增强）
```

**顺序说明**：
- **步骤 1 必须先做**：common 的 CQRS 组件是基础设施，被 web 和 rag 依赖
- **步骤 2 必须在 rag 之前**：`application-p3.yml` 提供全阶段配置，rag 模块需要读取
- **步骤 3 必须在 catalog 之前**：catalog 的 `DomainRouterV2` 需要 rag 的 `EmbeddingProvider`
- **步骤 5 最后做**：前端依赖所有后端 API 稳定，且工作量最大

---

## 四、主控 Agent 工作流程

### 4.1 初始化

启动时，先执行以下检查：

```
1. 确认 P1+P2 全部代码存在且编译通过
   → 运行：mvn compile -pl agent-qr-web -am（至少 web 模块及依赖模块能编译）
2. 确认 doc/p3-tasks/ 目录下所有任务文件可读
3. 初始化进度清单（见 4.3）
```

### 4.2 逐模块推进

对每个步骤（模块）：

1. **读取本 Prompt 中该模块的完整规格**（包结构、类设计、修改点、pom.xml 变更）
2. **创建子 Agent**，使用第十节定义的子 Agent 模板
3. **等待子 Agent 完成**，检查输出：
   - 所有新建类是否在正确的包路径下
   - 所有修改类是否保留了 P1/P2 原有代码
   - 注解是否正确（`@Component`、`@Aspect`、`@Configuration` 等）
   - import 语句是否完整
   - 是否有编译错误
4. **运行编译验证**：`mvn compile -pl <模块名> -am`
5. **更新进度清单**

### 4.3 进度清单模板

每完成一个模块后，更新以下表格：

```
| 步骤 | 模块 | 状态 | 新建类 | 修改类 | 编译 | 备注 |
|------|------|------|--------|--------|------|------|
| 1 | agent-qr-common | ⬜ | 0/2 | 0/1 | ⬜ | |
| 2 | agent-qr-web | ⬜ | 0/2 | 0/1 | ⬜ | |
| 3 | agent-qr-rag | ⬜ | 0/3 | 0/4 | ⬜ | |
| 4 | agent-qr-catalog | ⬜ | 0/1 | 0/2 | ⬜ | |
| 5 | agent-qr-web-frontend | ⬜ | 0/10+ | 0/8+ | ⬜ | |
```

状态图例：`⬜ 未开始` → `🔄 进行中` → `✅ 已完成` → `❌ 阻塞`

---

## 五、步骤 1：agent-qr-common P3（CQRS 读写分离基础组件）

### 5.1 模块信息

| 属性 | 值 |
|------|-----|
| Maven GAV | `org.example:agent-qr-common` |
| 包路径 | `org.example.agent_qr.common` |
| P3 新子包 | `datasource/` |
| 依赖 | 无新增依赖（基于 Spring 内置 `spring-boot-starter-aop`、`spring-boot-starter-jdbc`） |
| 类型 | P1 扩展 |

### 5.2 包结构

```
org.example.agent_qr.common/
├── datasource/                              [P3 NEW]
│   ├── ReadWriteRoutingDataSource.java      [P3 NEW]
│   └── ReadWriteDataSourceAspect.java       [P3 NEW]
├── config/
│   └── MybatisPlusConfig.java               [P3 MODIFY]
├── dlq/                                     [P2 已有 — 保持不变]
├── event/                                   [P2 已有 — 保持不变]
├── executor/                                [P2 已有 — 保持不变]
├── filter/                                  [P2 已有 — 保持不变]
├── rag/                                     [P2 已有 — 保持不变]
├── util/                                    [P1 已有 — 保持不变]
├── Result.java                              [P1 已有 — 保持不变]
└── BusinessException.java                   [P1 已有 — 保持不变]
```

### 5.3 ReadWriteRoutingDataSource — CQRS 读写数据源路由

```
[P3 NEW]
包路径：org.example.agent_qr.common.datasource.ReadWriteRoutingDataSource
继承：org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
注解：@Component, @Slf4j

字段：
  private static final ThreadLocal<Boolean> READ_ONLY_HOLDER = new ThreadLocal<>()

静态方法：
  public static void setReadOnly(boolean readOnly)
    → READ_ONLY_HOLDER.set(readOnly)

  public static void clear()
    → READ_ONLY_HOLDER.remove()

重写方法：
  @Override
  protected Object determineCurrentLookupKey()
    → return Boolean.TRUE.equals(READ_ONLY_HOLDER.get()) ? "read" : "write"

初始化方法：
  @PostConstruct
  public void initDataSources()
    1. 创建 Map<Object, Object> targetDataSources = new HashMap<>()
    2. 构建写库 DataSource：通过 DataSourceBuilder 读取 spring.datasource.write.* 配置
       - 若写库配置不存在，降级读取 spring.datasource.*（兼容 P1/P2 配置）
       - 使用 HikariDataSource
    3. 构建读库 DataSource：通过 DataSourceBuilder 读取 spring.datasource.read.* 配置
       - 若读库配置不存在，复用写库实例（初期从库可指向主库）
       - log.warn("读库配置不存在，回退到主库: read-replica-fallback-to-primary=true")
    4. targetDataSources.put("write", writeDataSource)
    5. targetDataSources.put("read", readDataSource)
    6. this.setTargetDataSources(targetDataSources)
    7. this.setDefaultTargetDataSource(writeDataSource)
    8. this.afterPropertiesSet()  // 必须调用，AbstractRoutingDataSource 要求
    9. log.info("CQRS 读写分离数据源已初始化: write={}, read={}", writeUrl, readUrl)

设计要点：
  - ThreadLocal 确保线程安全，每个请求独立路由
  - determineCurrentLookupKey() 返回 "read" 或 "write" 作为 lookup key
  - 写库为默认数据源（setDefaultTargetDataSource）
  - 读库初期可与写库同一实例，通过配置控制是否启用读库
```

### 5.4 ReadWriteDataSourceAspect — CQRS 读写分离切面

```
[P3 NEW]
包路径：org.example.agent_qr.common.datasource.ReadWriteDataSourceAspect
注解：@Aspect, @Component, @Order(-1), @Slf4j

切面方法：
  @Around("@annotation(transactional)")
  public Object routeDataSource(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable
    1. boolean readOnly = transactional.readOnly()
    2. log.debug("事务只读标志: readOnly={}, method={}", readOnly, pjp.getSignature())
    3. ReadWriteRoutingDataSource.setReadOnly(readOnly)
    4. try {
         return pjp.proceed()
       } finally {
         ReadWriteRoutingDataSource.clear()  // 必须清理，防止 ThreadLocal 泄漏
       }

设计要点：
  - @Order(-1) 确保在 Spring 事务拦截器之前执行
  - 从 @Transactional(readOnly) 注解读取读写标志
  - finally 块中清理 ThreadLocal 是必须的，否则会污染线程池中的线程
  - 异常不吞掉：finally 只做清理，异常自然向上传播
```

### 5.5 MybatisPlusConfig — 兼容多数据源

```
[P3 MODIFY]
当前路径：org.example.agent_qr.common.config.MybatisPlusConfig

当前代码（保持不变）：
  @Bean public MybatisPlusInterceptor mybatisPlusInterceptor()
    → 注册 PaginationInnerInterceptor(DbType.MYSQL)
  @Bean public ConfigurationCustomizer configurationCustomizer()
    → 禁用 MapUnderscoreToCamelCase

P3 新增（如需）：
  - 检查 SqlSessionFactory 使用的 DataSource 是否为 ReadWriteRoutingDataSource
  - 当前 MybatisPlusConfig 未直接引用 DataSource，通常无需修改
  - 若 MyBatis-Plus 自动注入的 DataSource 不是 ReadWriteRoutingDataSource，
    需显式注入 ReadWriteRoutingDataSource 并设置到 SqlSessionFactoryBean

验证点：确保 MyBatis-Plus 的所有数据库操作都经过 ReadWriteRoutingDataSource 路由
```

### 5.6 pom.xml 变更

```
无变更。P3 所需依赖（spring-boot-starter-aop、spring-boot-starter-jdbc）已在 P1 引入。
```

### 5.7 子 Agent 任务清单

- [ ] **1.1** 创建 `org.example.agent_qr.common.datasource` 包
- [ ] **1.2** 创建 `ReadWriteRoutingDataSource` 类（完整实现：ThreadLocal + determineCurrentLookupKey + initDataSources）
- [ ] **1.3** 创建 `ReadWriteDataSourceAspect` 类（完整实现：@Around + @Order(-1) + finally 清理）
- [ ] **1.4** 检查 `MybatisPlusConfig` 与多数据源的兼容性，必要时修改
- [ ] **1.5** 编译验证：`mvn compile -pl agent-qr-common`

---

## 六、步骤 2：agent-qr-web P3（P3 配置 + CQRS Bean 装配）

### 6.1 模块信息

| 属性 | 值 |
|------|-----|
| Maven GAV | `org.example:agent-qr-web` |
| 包路径 | `org.example.agent_qr.web` |
| 依赖 | agent-qr-common（已有）+ 所有其他模块（已有） |
| 类型 | P1 扩展 |

### 6.2 包结构

```
agent-qr-web/src/main/java/org/example/agent_qr/web/
├── config/
│   ├── SecurityConfig.java              [P3 MODIFY — 如需 WebSocket 放行]
│   ├── CqrsDataSourceConfig.java        [P3 NEW]
│   └── ... (其他已有配置保持不变)
└── ...

agent-qr-web/src/main/resources/
├── application.yml                      [P3 MODIFY — 添加 p3 profile]
├── application-p1.yml                   [P1 已有 — 保持不变]
├── application-p2.yml                   [P2 已有 — 保持不变]
└── application-p3.yml                   [P3 NEW]
```

### 6.3 application-p3.yml — P3 阶段配置

```yaml
# ============================================================
# P3 阶段配置：CQRS 读写分离 + Embedding 语义路由 + Provider 切换
# 激活方式：spring.profiles.active=p3 或 spring.profiles.active=p1,p2,p3
# ============================================================

spring:
  datasource:
    # 写库（主库）— 与 P1/P2 配置一致
    write:
      url: jdbc:mysql://localhost:3308/agent_qr?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      username: root
      password: ${DB_PASSWORD:root}
      driver-class-name: com.mysql.cj.jdbc.Driver
      hikari:
        maximum-pool-size: 20
        minimum-idle: 5
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
    # 读库（从库）— 初期可与主库同实例
    read:
      url: jdbc:mysql://localhost:3309/agent_qr?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      username: root
      password: ${DB_PASSWORD:root}
      driver-class-name: com.mysql.cj.jdbc.Driver
      hikari:
        maximum-pool-size: 10
        minimum-idle: 2
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000

# ============================================================
# P3 专属配置
# ============================================================
agent-qr:
  cqrs:
    enabled: true                          # 启用 CQRS 读写分离
    read-replica-fallback-to-primary: true # 初期从库不存在时回退到主库

  routing:
    mode: semantic                         # 域路由模式：keyword | semantic | auto
    similarity-threshold: 0.3              # Embedding 余弦相似度阈值
    top-k: 2                               # 返回 Top K 个匹配域

  provider:
    auto-failover: true                    # 熔断时自动切换备用 Provider
    preferred-llm: deepseek                # 首选 LLM Provider
    preferred-embedding: deepseek          # 首选 Embedding Provider

  embedding:
    collection-prefix: kb                  # ChromaDB Collection 前缀
    auto-dimension-check: true             # 启动时自动检测向量维度一致性
```

### 6.4 CqrsDataSourceConfig — CQRS 数据源 Bean 装配

```
[P3 NEW]
包路径：org.example.agent_qr.web.config.CqrsDataSourceConfig
注解：@Configuration, @ConditionalOnProperty(name = "agent-qr.cqrs.enabled", havingValue = "true"), @Slf4j

方法：
  @Bean
  @Primary
  @ConfigurationProperties(prefix = "spring.datasource.write")
  public DataSource writeDataSource()
    → 返回 HikariDataSource（Spring Boot 自动配置）
    → @Primary 确保其他模块默认注入写库
    → log.info("CQRS 写库已配置: HikariDataSource")

  @Bean
  @ConfigurationProperties(prefix = "spring.datasource.read")
  public DataSource readDataSource()
    → 返回 HikariDataSource（Spring Boot 自动配置）
    → 若读库配置不可用：检查 agent-qr.cqrs.read-replica-fallback-to-primary
      - true → 返回 writeDataSource() 实例（回退到主库）
      - false → 抛出 BeanCreationException
    → log.info("CQRS 读库已配置: HikariDataSource (fallback={})", fallback)

设计要点：
  - @ConditionalOnProperty 确保仅 P3 profile 激活时才创建这些 Bean
  - P1/P2 模式下不会加载此类，保持向后兼容
  - ReadWriteRoutingDataSource 已标记 @Component，由组件扫描自动发现
  - 无需额外配置来注册 ReadWriteRoutingDataSource —— Spring 会自动注入
```

### 6.5 SecurityConfig — WebSocket 放行（如需）

```
[P3 MODIFY — 仅当前端 P3 实现 WebSocket 时需要]
当前路径：org.example.agent_qr.web.config.SecurityConfig

当前代码结构：
  - @EnableMethodSecurity(prePostEnabled = true)
  - SecurityFilterChain 定义了公开端点和认证端点
  - JwtAuthenticationFilter 在 UsernamePasswordAuthenticationFilter 之前

P3 修改点（仅 WebSocket 场景）：
  1. 新增放行端点：.requestMatchers("/ws/**", "/ws/info").permitAll()
  2. 若使用 STOMP over WebSocket，端点通常为 /ws
  3. 关闭 WebSocket 端点的 CSRF 保护（如适用）
  4. 保留所有 P1/P2 已有配置不变
```

### 6.6 启动验证

```
P3 profile 启动测试：
  命令：mvn spring-boot:run -Dspring-boot.run.profiles=p1,p2,p3
  预期日志：
    1. "CQRS 写库已配置: HikariDataSource (pool=20)"
    2. "CQRS 读库已配置: HikariDataSource (fallback=true)" 或 "(pool=10)"
    3. "CQRS 读写分离数据源已初始化: write=jdbc:mysql://localhost:3308/..., read=jdbc:mysql://localhost:3309/..."
    4. 应用正常启动，无 DataSource 相关的 Bean 创建异常
```

### 6.7 pom.xml 变更

```
无变更。所有依赖已在 P1/P2 引入。CqrsDataSourceConfig 仅依赖 spring-boot-starter（已有）。
```

### 6.8 子 Agent 任务清单

- [ ] **2.1** 创建 `application-p3.yml`（包含读写分离数据源 + P3 专属配置）
- [ ] **2.2** 更新 `application.yml`：在 `spring.profiles.active` 中添加 `p3`（或保持不变，通过启动参数激活）
- [ ] **2.3** 创建 `CqrsDataSourceConfig` 类
- [ ] **2.4** 检查 `SecurityConfig`，如需 WebSocket 放行则修改（前端 P3 实施时再改）
- [ ] **2.5** 编译验证：`mvn compile -pl agent-qr-web -am`
- [ ] **2.6** 启动验证：以 p3 profile 启动，检查 CQRS 相关日志

---

## 七、步骤 3：agent-qr-rag P3（语义路由 + 维度管理 + Provider 决策）

### 7.1 模块信息

| 属性 | 值 |
|------|-----|
| Maven GAV | `org.example:agent-qr-rag` |
| 包路径 | `org.example.agent_qr.rag` |
| 依赖 | agent-qr-common, agent-qr-user, agent-qr-auth, agent-qr-catalog |
| 类型 | P1 扩展 |

### 7.2 包结构

```
org.example.agent_qr.rag/
├── embedding/
│   ├── BatchEmbeddingService.java             [P2 已有 — P3 MODIFY]
│   └── EmbeddingDimensionManager.java         [P3 NEW]
├── router/
│   └── DomainRouterV2.java                    [P3 NEW]
├── provider/
│   ├── ProviderFactory.java                   [P2 已有 — P3 MODIFY]
│   └── ProviderDecisionEngine.java            [P3 NEW]
├── service/
│   └── ChatQueryService.java                  [P2 已有 — P3 MODIFY]
├── retriever/
│   └── HybridRetriever.java                   [P2 已有 — P3 MODIFY]
├── controller/                                [P2 已有 — 保持不变]
├── circuitbreaker/                            [P2 已有 — 保持不变]
├── filter/                                    [P2 已有 — 保持不变]
├── config/                                    [P2 已有 — 保持不变]
├── prompt/                                    [P2 已有 — 保持不变]
├── entity/                                    [P2 已有 — 保持不变]
└── mapper/                                    [P2 已有 — 保持不变]
```

### 7.3 EmbeddingDimensionManager — Collection 维度检测与隔离

```
[P3 NEW]
包路径：org.example.agent_qr.rag.embedding.EmbeddingDimensionManager
注解：@Component, @Slf4j

注入：
  @Autowired private ProviderFactory providerFactory
  @Autowired private ChromaClient chromaClient（或通过 ChromaConfig 获取）

字段：
  private final Map<String, Boolean> collectionCache = new ConcurrentHashMap<>()

方法：
  public String getCollectionName()
    1. String providerType = providerFactory.getEmbeddingProviderType()  // "deepseek" 或 "ollama"
    2. String modelName = providerFactory.getEmbeddingModelName()        // 如 "deepseek-embedding"
    3. String safeName = modelName.replace(":", "-")                    // ChromaDB 不允许冒号
    4. return "kb_" + providerType + "_" + safeName
    // 示例：kb_deepseek_deepseek-embedding, kb_ollama_nomic-embed-text

  @EventListener(ApplicationReadyEvent.class)
  public void checkDimension()
    1. String col = getCollectionName()
    2. if (chromaClient.collectionExists(col)) {
         log.info("Embedding 维度一致性检查通过: collection={}", col)
       } else {
         log.warn("检测到新 Embedding 模型: collection={} 不存在于 ChromaDB，" +
                  "需执行全量向量重建。当前嵌入维度可能与已有数据不兼容。", col)
       }
    3. collectionCache.put(col, true)

  public boolean ensureCollection(String collectionName)
    1. if (collectionCache.containsKey(collectionName)) return true
    2. boolean exists = chromaClient.collectionExists(collectionName)
    3. if (!exists) {
         chromaClient.createCollection(collectionName)
         log.info("已创建 ChromaDB Collection: {}", collectionName)
       }
    4. collectionCache.put(collectionName, true)
    5. return true

设计要点：
  - Collection 命名嵌入 Provider 类型和模型名，切换 Embedding Provider 自动使用不同 Collection
  - 这确保了不同维度的向量不会混在同一个 Collection 中导致检索失败
  - 启动时检测：Collection 不存在意味着用户切换了 Embedding 模型，需要全量重建
  - ConcurrentHashMap 缓存避免重复检查
```

### 7.4 DomainRouterV2 — Embedding 语义域路由

```
[P3 NEW]
包路径：org.example.agent_qr.rag.router.DomainRouterV2
注解：@Component, @Slf4j

注入：
  @Autowired private EmbeddingProvider embeddingProvider
  @Autowired(required = false) private KnowledgeCatalogService catalogService
  （KnowledgeCatalogService 来自 agent-qr-catalog 模块，required=false 避免硬依赖）

字段：
  private final Map<String, float[]> domainEmbeddings = new ConcurrentHashMap<>()
  private volatile long lastRefreshTime = 0
  private static final long REFRESH_INTERVAL_MS = 300_000  // 5 分钟

方法：
  @PostConstruct
  public void init()
    1. 定义域描述 Map（硬编码初始值，后续由 catalog 模块动态更新）：
       "HR"      → "人力资源管理，员工信息，薪酬福利，绩效考核，组织架构，招聘培训"
       "FINANCE" → "财务管理，会计核算，预算控制，成本管理，财务报表，审计合规"
       "RD"      → "研发管理，产品设计，技术标准，代码管理，测试管理，项目管理"
       "SALES"   → "销售管理，客户关系，合同管理，销售业绩，市场推广，渠道管理"
    2. 遍历每个域描述 → embeddingProvider.embed(desc)
    3. 存入 domainEmbeddings Map
    4. log.info("DomainRouterV2 初始化完成: 已加载 {} 个域的 Embedding", domainEmbeddings.size())

  public DomainRoutingResult route(String query)
    1. float[] queryEmb = embeddingProvider.embed(query)
    2. Map<String, Double> scores = new LinkedHashMap<>()
    3. for (Map.Entry<String, float[]> entry : domainEmbeddings.entrySet()) {
         double sim = cosineSimilarity(queryEmb, entry.getValue())
         if (sim > 0.3) {  // 硬编码阈值，低于此值视为不相关
           scores.put(entry.getKey(), sim)
         }
       }
    4. if (scores.isEmpty()) {
         log.debug("语义路由未匹配到任何域 (threshold=0.3)，回退全局检索")
         return DomainRoutingResult.fallback()
       }
    5. // 按相似度降序排列，取 Top 2
       Map<String, Double> top2 = scores.entrySet().stream()
         .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
         .limit(2)
         .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                   (e1, e2) -> e1, LinkedHashMap::new))
    6. log.debug("语义路由结果: {}", top2)
    7. return DomainRoutingResult.routed(top2)

  private double cosineSimilarity(float[] a, float[] b)
    1. if (a == null || b == null || a.length != b.length) return 0.0
    2. double dotProduct = 0, normA = 0, normB = 0
    3. for (int i = 0; i < a.length; i++) {
         dotProduct += a[i] * b[i]
         normA += a[i] * a[i]
         normB += b[i] * b[i]
       }
    4. if (normA == 0 || normB == 0) return 0.0
    5. return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB))

  @Scheduled(fixedRate = 300000)  // 每 5 分钟刷新
  public void refreshDomainEmbeddings()
    1. log.debug("开始刷新域 Embedding 缓存...")
    2. // 如果 catalogService 可用，从目录树动态获取域描述
       if (catalogService != null) {
         CatalogTree tree = catalogService.getCatalogTree()
         // 遍历 DomainNode，构建域描述
       }
    3. // 否则保持硬编码的域描述，只重新计算 Embedding（模型可能热切换）
       domainEmbeddings.forEach((domain, oldEmb) -> {
         // 重新 embed 域描述
       })
    4. lastRefreshTime = System.currentTimeMillis()
    5. log.debug("域 Embedding 缓存刷新完成")

设计要点：
  - 与 P2 DomainRouter 共存：P3 优先使用 DomainRouterV2，不可用时降级 P2 关键词路由
  - 余弦相似度阈值 0.3 为经验值，可通过 application-p3.yml 配置
  - Top 2 域 + 全局回退保证检索覆盖率
  - 5 分钟定时刷新适配目录结构变化和模型热切换
  - DomainRoutingResult 复用 P2 已有的 DTO 类：agent-qr-catalog 的 DomainRoutingResult
```

### 7.5 ProviderDecisionEngine — Provider 切换自动决策

```
[P3 NEW]
包路径：org.example.agent_qr.rag.provider.ProviderDecisionEngine
注解：@Component, @Slf4j

注入：
  @Autowired private ProviderFactory providerFactory
  @Autowired private LLMCircuitBreaker circuitBreaker

配置：
  @Value("${agent-qr.provider.preferred-llm:deepseek}") private String preferredLLM
  @Value("${agent-qr.provider.preferred-embedding:deepseek}") private String preferredEmbedding
  @Value("${agent-qr.provider.auto-failover:true}") private boolean autoFailover

方法：
  public String decideLLMProvider()
    1. if (!autoFailover) return preferredLLM
    2. CircuitBreaker.State state = circuitBreaker.getState(preferredLLM)
    3. switch (state) {
         case CLOSED:
           log.debug("熔断器关闭，使用首选 LLM Provider: {}", preferredLLM)
           return preferredLLM
         case OPEN:
           String fallback = providerFactory.getFallbackLLMProviderType()
           log.warn("熔断器打开，切换 LLM Provider: {} → {}", preferredLLM, fallback)
           return fallback
         case HALF_OPEN:
           log.info("熔断器半开，尝试首选 LLM Provider: {}", preferredLLM)
           return preferredLLM  // 尝试首选，失败后由熔断器再次打开
         default:
           return preferredLLM
       }

  public String decideEmbeddingProvider()
    // 同 decideLLMProvider() 逻辑，使用 embedding 相关的配置
    1. if (!autoFailover) return preferredEmbedding
    2. 检查 Embedding Provider 的熔断器状态
    3. 返回决策结果
    // 注意：Embedding Provider 可能没有独立的熔断器
    // 若没有，直接返回 preferredEmbedding

设计要点：
  - 读取 application-p3.yml 中的 agent-qr.provider.* 配置
  - 熔断器状态驱动决策：CLOSED → 首选，OPEN → 备用，HALF_OPEN → 尝试首选
  - auto-failover=false 时直接使用首选，不做切换（用于调试）
  - 所有决策结果记录日志，便于运维排查
```

### 7.6 ProviderFactory — 集成决策引擎

```
[P3 MODIFY]
当前路径：org.example.agent_qr.rag.provider.ProviderFactory

当前代码结构（P2）：
  - @Value("${llm.provider:deepseek}") private String llmProvider
  - @Value("${embedding.provider:deepseek}") private String embeddingProvider
  - getLLMProvider(): switch(llmProvider) → 返回 DeepSeekLLMProvider 或 OllamaLLMProvider
  - getEmbeddingProvider(): switch(embeddingProvider) → 返回 DeepSeekEmbeddingProvider 或 OllamaEmbeddingProvider
  - getFallbackLLMProviderType(): 返回备用 Provider 类型
  - getLLMProviderType(): 返回当前 LLM Provider 类型
  - getEmbeddingProviderType(): 返回当前 Embedding Provider 类型

P3 新增注入：
  @Autowired(required = false) private ProviderDecisionEngine decisionEngine

P3 修改点 — getLLMProvider():
  1. String provider;
  2. if (decisionEngine != null) {
       provider = decisionEngine.decideLLMProvider()  // P3 决策引擎优先
     } else {
       provider = llmProvider  // 降级到配置值
     }
  3. switch(provider) → 返回对应 Provider 实例
  4. 保留原有 switch 逻辑不变

P3 修改点 — getEmbeddingProvider():
  1. 同 getLLMProvider() 修改方式
  2. if (decisionEngine != null) → decisionEngine.decideEmbeddingProvider()
  3. else → embeddingProvider（配置值）

★ 重要：保留所有 P2 原有逻辑作为降级方案
★ ProviderFactory 已提供的 getFallbackLLMProviderType()、getLLMProviderType()、
  getEmbeddingProviderType()、getEmbeddingModelName() 方法保持 P2 实现不变
```

### 7.7 ChatQueryService — 接入 DomainRouterV2

```
[P3 MODIFY]
当前路径：org.example.agent_qr.rag.service.ChatQueryService

当前代码结构（P2）：
  - @Autowired(required = false) private DomainRouter domainRouter  // P2 关键词路由
  - resolveRouting(): 调用 domainRouter.route()，不可用则降级全局检索
  - ask(): 同步问答，调用 resolveRouting()
  - askStream(): 流式问答，调用 resolveRouting()

P3 新增注入：
  @Autowired(required = false) private DomainRouterV2 domainRouterV2

P3 修改点 — resolveRouting():
  1. // 优先使用 P3 语义路由
     if (domainRouterV2 != null) {
       try {
         DomainRoutingResult result = domainRouterV2.route(query)
         if (!result.isFallbackToGlobal()) return result
       } catch (Exception e) {
         log.warn("DomainRouterV2 语义路由异常，降级到关键词路由", e)
       }
     }
  2. // 降级到 P2 关键词路由
     if (domainRouter != null) {
       try {
         return domainRouter.route(query)
       } catch (Exception e) {
         log.warn("DomainRouter 关键词路由异常，降级到全局检索", e)
       }
     }
  3. // 最终降级：全局检索
     return DomainRoutingResult.fallback()

★ 重要：
  - domainRouterV2 使用 @Autowired(required = false)，避免编译时硬依赖
  - P2 的 domainRouter 保留不变，作为降级链中的一环
  - 异常安全：任何路由环节出错都自动降级，不影响问答主流程
  - ask() 和 askStream() 方法不需要修改，它们共用 resolveRouting()
```

### 7.8 HybridRetriever — 接入 DomainRouterV2

```
[P3 MODIFY]
当前路径：org.example.agent_qr.rag.retriever.HybridRetriever

当前代码结构（P2）：
  - @Autowired(required = false) private DomainRouter domainRouter
  - hybridSearch(): 接收 DomainRoutingResult 参数，调用 StructuredFilterService 过滤

P3 新增注入：
  @Autowired(required = false) private DomainRouterV2 domainRouterV2

P3 修改点：
  - hybridSearch() 方法签名不变（仍接收 DomainRoutingResult）
  - 路由决策在 ChatQueryService.resolveRouting() 中统一处理
  - HybridRetriever 无需感知路由来源（P2 vs P3）
  - 只需确认 domainRouterV2 注入用于日志输出或调试

★ 注意：HybridRetriever 的修改最小化。路由逻辑统一在 ChatQueryService 中，
  HybridRetriever 只是检索执行器，不负责路由决策。
```

### 7.9 BatchEmbeddingService — 接入 EmbeddingDimensionManager

```
[P3 MODIFY]
当前路径：org.example.agent_qr.rag.embedding.BatchEmbeddingService

当前代码结构（P2）：
  - 批量攒批消费队列，异步写入 ChromaDB
  - 使用固定的 Collection 名称（来自 ChromaConfig 或 application.yml 配置）

P3 新增注入：
  @Autowired private EmbeddingDimensionManager dimensionManager

P3 修改点：
  1. 获取 Collection 名称时：
     - P2 方式：从 chromaConfig.getCollectionName() 或 @Value 读取固定名称
     - P3 方式：优先使用 dimensionManager.getCollectionName()
     - 若 dimensionManager 不可用（required=false），降级到 P2 方式
  2. 写入 ChromaDB 前：
     - 调用 dimensionManager.ensureCollection(collectionName) 确保 Collection 存在

★ 重要：保留 P2 降级路径，确保 EmbeddingDimensionManager 不可用时服务不中断
```

### 7.10 pom.xml 变更

```
无变更。P3 所需依赖（langchain4j、webflux、lucene）已在 P2 引入。
DomainRouterV2 依赖的 EmbeddingProvider 接口和 DomainRoutingResult DTO 均已存在。
```

### 7.11 子 Agent 任务清单

- [ ] **3.1** 创建 `EmbeddingDimensionManager` 类（getCollectionName + checkDimension + ensureCollection）
- [ ] **3.2** 创建 `DomainRouterV2` 类（init + route + cosineSimilarity + refreshDomainEmbeddings）
- [ ] **3.3** 创建 `ProviderDecisionEngine` 类（decideLLMProvider + decideEmbeddingProvider）
- [ ] **3.4** 修改 `ProviderFactory`（注入 decisionEngine，getLLMProvider/getEmbeddingProvider 优先使用决策引擎）
- [ ] **3.5** 修改 `ChatQueryService`（注入 domainRouterV2，resolveRouting 优先语义路由）
- [ ] **3.6** 修改 `HybridRetriever`（注入 domainRouterV2，最小化修改）
- [ ] **3.7** 修改 `BatchEmbeddingService`（注入 dimensionManager，动态获取 Collection 名称）
- [ ] **3.8** 编译验证：`mvn compile -pl agent-qr-rag -am`

---

## 八、步骤 4：agent-qr-catalog P3（目录语义路由集成）

### 8.1 模块信息

| 属性 | 值 |
|------|-----|
| Maven GAV | `org.example:agent-qr-catalog` |
| 包路径 | `org.example.agent_qr.catalog` |
| 依赖 | agent-qr-common, agent-qr-datasource, agent-qr-rag（提供 EmbeddingProvider） |
| 类型 | P2 扩展 |

### 8.2 包结构

```
org.example.agent_qr.catalog/
├── router/
│   ├── DomainRouter.java                     [P2 已有 — 保持不变作为降级]
│   └── DomainRouterV2.java                   [P3 NEW]
├── service/
│   └── KnowledgeCatalogService.java          [P2 已有 — P3 MODIFY]
├── controller/
│   └── CatalogController.java                [P2 已有 — P3 MODIFY]
├── entity/                                   [P2 已有 — 保持不变]
└── dto/                                      [P2 已有 — 保持不变]
```

### 8.3 DomainRouterV2 — 目录语义路由（catalog 版）

```
[P3 NEW]
包路径：org.example.agent_qr.catalog.router.DomainRouterV2
注解：@Component, @Slf4j

说明：
  此版本与 rag 模块的 DomainRouterV2 是互补关系：
  - catalog 版：负责基于目录树动态生成域描述 → Embedding 预计算
  - rag 版：负责在问答流程中使用 Embedding 做路由匹配
  catalog 版提供域 Embedding 给 rag 版消费（通过共享的 ConcurrentHashMap 或事件通知）

注入：
  @Autowired private EmbeddingProvider embeddingProvider（来自 agent-qr-rag）
  @Autowired private KnowledgeCatalogService catalogService

字段：
  private volatile Map<String, float[]> cachedDomainEmbeddings = new ConcurrentHashMap<>()
  private volatile long lastRefreshTime = 0

方法：
  public Map<String, float[]> getDomainEmbeddings()
    1. if (cachedDomainEmbeddings.isEmpty() || isStale()) → refresh()
    2. return Collections.unmodifiableMap(cachedDomainEmbeddings)

  private boolean isStale()
    → return System.currentTimeMillis() - lastRefreshTime > 300_000  // 5 分钟

  @PostConstruct
  public void init()
    → refresh()

  @Scheduled(fixedRate = 300000)
  public void refresh()
    1. CatalogTree tree = catalogService.getCatalogTree()
    2. Map<String, String> domainDescriptions = buildDomainDescriptions(tree)
    3. Map<String, float[]> newEmbeddings = new ConcurrentHashMap<>()
    4. for (Map.Entry<String, String> entry : domainDescriptions.entrySet()) {
         float[] emb = embeddingProvider.embed(entry.getValue())
         newEmbeddings.put(entry.getKey(), emb)
       }
    5. cachedDomainEmbeddings = newEmbeddings  // 原子替换
    6. lastRefreshTime = System.currentTimeMillis()
    7. log.info("目录域 Embedding 刷新完成: {} 个域", newEmbeddings.size())

  private Map<String, String> buildDomainDescriptions(CatalogTree tree)
    1. Map<String, StringBuilder> descriptions = new LinkedHashMap<>()
    2. for (DomainNode domain : tree.getDomains()) {
         StringBuilder sb = new StringBuilder()
         sb.append(domain.getName()).append("，")
         // 拼接子数据源名称（Top 20）
         domain.getSources().stream()
           .limit(20)
           .forEach(s -> sb.append(s.getName()).append("，"))
         // 拼接子实体名称（Top 50）
         domain.getSources().stream()
           .flatMap(s -> s.getEntities().stream())
           .limit(50)
           .forEach(e -> sb.append(e.getName()).append("，"))
         descriptions.put(domain.getName(), sb.toString())
       }
    3. return descriptions  // Map<域ID → 自然语言描述>

  @EventListener(DataSyncCompletedEvent.class)
  public void onCatalogChanged(DataSyncCompletedEvent event)
    1. log.info("检测到目录变更事件，将触发域 Embedding 刷新")
    2. // 不立即刷新，标记为过期，下次访问时懒加载
       lastRefreshTime = 0

设计要点：
  - 与 rag 模块的 DomainRouterV2 通过共享数据协同：
    → catalog 版写 cachedDomainEmbeddings
    → rag 版通过依赖注入或静态方法读取
  - 实际方案：rag 模块的 DomainRouterV2 在 refreshDomainEmbeddings() 中
    通过 catalogService 获取目录树和域描述，从而消除重复
  - 两个模块各有 DomainRouterV2 但职责不同：
    catalog: 域描述生成 + Embedding 预计算
    rag: 查询路由匹配 + Top-K 选择
```

### 8.4 KnowledgeCatalogService — 支持双路由

```
[P3 MODIFY]
当前路径：org.example.agent_qr.catalog.service.KnowledgeCatalogService

当前代码结构（P2）：
  - 构建目录树（从数据源配置 + ETL 产出）
  - 发布/监听目录变更事件
  - 提供 getCatalogTree()、getStats() 方法

P3 新增：
  - 无需大幅修改。DomainRouterV2 通过注入 catalogService 获取目录树
  - 确保 getCatalogTree() 方法高效（已有 Caffeine 缓存则无需改动）

★ KnowledgeCatalogService 本身不直接调用 DomainRouterV2，路由由 rag 模块执行。
  此服务的职责仍是维护目录数据，P3 仅需确保 API 稳定给 DomainRouterV2 消费。
```

### 8.5 CatalogController — 可选新增路由调试端点

```
[P3 MODIFY — 可选]
当前路径：org.example.agent_qr.catalog.controller.CatalogController

当前端点（P2）：
  - GET /api/catalog/tree  → 获取完整目录树
  - GET /api/catalog/stats → 获取目录统计

P3 新增端点（可选，用于调试语义路由）：
  GET /api/catalog/route?q=查询文本
    → 调用 DomainRouterV2.route("查询文本")
    → 返回匹配的域及其相似度分数
    → 便于观察语义路由效果，无需进入完整问答流程

  若添加此端点：
    - 注入 @Autowired(required = false) private DomainRouterV2 domainRouterV2
    - 直接在 Controller 方法中调用 route() 返回结果
    - 标记为调试接口，可在 SecurityConfig 中限制访问
```

### 8.6 pom.xml 变更

```
无变更。agent-qr-rag 依赖已在 P2 引入（提供 EmbeddingProvider 接口）。
```

### 8.7 子 Agent 任务清单

- [ ] **4.1** 创建 `DomainRouterV2` 类（catalog 版：buildDomainDescriptions + refresh + onCatalogChanged）
- [ ] **4.2** 检查 `KnowledgeCatalogService` 与语义路由的兼容性（确保 getCatalogTree() 高效可用）
- [ ] **4.3** 可选：`CatalogController` 新增 `GET /api/catalog/route` 调试端点
- [ ] **4.4** 编译验证：`mvn compile -pl agent-qr-catalog -am`

---

## 九、步骤 5：agent-qr-web-frontend P3（前端全面增强）

### 9.1 模块信息

| 属性 | 值 |
|------|-----|
| 路径 | `agent-qr-web-frontend/` |
| 技术栈 | Vue 3 + TypeScript + Pinia + Vite |
| 包管理 | pnpm |
| 类型 | P2 扩展 |

### 9.2 文件结构概览

```
agent-qr-web-frontend/src/
├── composables/
│   ├── useWebSocket.ts                        [P3 NEW]
│   ├── useBreakpoint.ts                       [P3 NEW]
│   └── useSpeechRecognition.ts                [P3 NEW]
├── i18n/
│   ├── index.ts                               [P3 NEW]
│   └── locales/
│       ├── zh-CN.ts                           [P3 NEW]
│       └── en-US.ts                           [P3 NEW]
├── directives/
│   └── permission.ts                          [P3 NEW]
├── components/
│   ├── charts/
│   │   └── KnowledgeGraph.vue                 [P3 NEW]
│   ├── quality/
│   │   └── RuleEditor.vue                     [P3 NEW]
│   └── layout/
│       ├── Sidebar.vue                        [P3 MODIFY]
│       └── Header.vue                         [P3 MODIFY]
├── views/
│   ├── chat/
│   │   └── index.vue                          [P3 MODIFY]
│   ├── catalog/
│   │   └── index.vue                          [P3 MODIFY]
│   ├── quality/
│   │   ├── index.vue                          [P3 MODIFY]
│   │   └── RulesManager.vue                   [P3 NEW]
│   ├── dashboard/
│   │   └── index.vue                          [P3 MODIFY]
│   ├── knowledge/
│   │   └── index.vue                          [P3 MODIFY]
│   ├── datasource/
│   │   └── index.vue                          [P3 MODIFY]
│   ├── auth/
│   │   └── Login.vue                          [P3 MODIFY — i18n]
│   └── user/
│       └── index.vue                          [P3 MODIFY — ABAC + i18n]
├── stores/
│   └── auth.ts                                [P3 MODIFY]
├── router/
│   └── index.ts                               [P3 MODIFY]
└── main.ts                                    [P3 MODIFY — 注册 i18n + directives]
```

### 9.3 第一批（核心）：WebSocket 通信

#### 9.3.1 依赖安装

```
pnpm add sockjs-client @stomp/stompjs
pnpm add -D @types/sockjs-client
```

#### 9.3.2 useWebSocket Composable

```
[P3 NEW]
路径：src/composables/useWebSocket.ts

导出：
  - connect(token: string): Promise<void>  → 建立 STOMP 连接
  - disconnect(): void                      → 断开连接
  - send(destination: string, body: any): void → 发送消息
  - subscribe(destination: string, callback: Function): Subscription
  - connectionState: Ref<'disconnected' | 'connecting' | 'connected'>

功能要点：
  1. 使用 @stomp/stompjs 的 Client 类
  2. brokerURL: `${VITE_WS_URL}/ws`（例如 http://localhost:9090/ws）
  3. connectHeaders: { Authorization: `Bearer ${token}` }
  4. 自动重连：reconnectDelay: 5000，指数退避
  5. 心跳：heartbeatIncoming: 10000, heartbeatOutgoing: 10000
  6. 连接状态响应式追踪
  7. onConnect: 自动订阅 /user/queue/chat（用户私有消息队列）
```

#### 9.3.3 聊天页改造

```
[P3 MODIFY]
路径：src/views/chat/index.vue

当前代码（P2）：
  - 使用 fetchEventSource（SSE）发送问题并接收流式回答
  - POST /api/chat/ask/stream

P3 改造点：
  1. 初始化时调用 useWebSocket.connect()
  2. 发送消息：通过 WebSocket 发送到 /app/chat/ask（STOMP 目标）
  3. 接收消息：订阅 /user/queue/chat/stream，逐 token 追加到消息列表
  4. 中断生成：通过 WebSocket 发送取消消息到 /app/chat/cancel
  5. 降级方案：WebSocket 连接失败时回退到 SSE（保留 P2 的 fetchEventSource 代码路径）
  6. onUnmounted: 调用 useWebSocket.disconnect()

新增 UI：
  - 连接状态指示器（绿色圆点 = 已连接，红色 = 断开，黄色 = 重连中）
  - 语音输入按钮（麦克风图标，使用 useSpeechRecognition）
```

### 9.4 第一批（核心）：i18n 国际化

#### 9.4.1 依赖安装

```
pnpm add vue-i18n@9
```

#### 9.4.2 i18n 基础设施

```
[P3 NEW]
路径：src/i18n/index.ts
  - 创建 createI18n 实例
  - legacy: false（使用 Composition API 模式）
  - locale: localStorage.getItem('locale') || 'zh-CN'
  - fallbackLocale: 'zh-CN'
  - messages: { 'zh-CN': zhCN, 'en-US': enUS }

路径：src/i18n/locales/zh-CN.ts  [P3 NEW]
  - 导出中文语言包对象
  - 按功能模块分组：common（通用）、auth（认证）、chat（聊天）、knowledge（知识库）、
    user（用户）、dashboard（仪表盘）、datasource（数据源）、catalog（目录）、
    quality（质量）、sidebar（侧边栏）、header（顶栏）
  - 覆盖全部硬编码中文文本

路径：src/i18n/locales/en-US.ts  [P3 NEW]
  - 导出英文语言包对象，与 zh-CN 结构完全对应

路径：src/main.ts  [P3 MODIFY]
  - import i18n from './i18n'
  - app.use(i18n)
```

#### 9.4.3 页面 i18n 替换

```
需替换的页面（逐页面进行）：
  - src/views/auth/Login.vue          → 登录表单标签、按钮、提示信息
  - src/views/chat/index.vue          → 输入框占位符、发送按钮、加载提示
  - src/views/knowledge/index.vue     → 表格列头、操作按钮、上传表单
  - src/views/user/index.vue          → 用户列表列头、编辑表单
  - src/views/dashboard/index.vue     → 图表标题、统计卡片
  - src/views/datasource/index.vue    → 数据源列表、连接配置表单
  - src/views/catalog/index.vue       → 树节点标签、统计面板
  - src/views/quality/index.vue       → 报告标题、规则列表
  - src/components/layout/Sidebar.vue → 菜单项标签
  - src/components/layout/Header.vue  → 用户菜单、语言切换

替换方式：将所有硬编码中文替换为 {{ $t('module.key') }}
示例：
  <span>用户名</span>  →  <span>{{ $t('auth.username') }}</span>
  '登录成功'           →  t('auth.loginSuccess')
```

#### 9.4.4 语言切换 UI

```
[P3 MODIFY]
路径：src/components/layout/Header.vue（或 Sidebar.vue 底部）

新增：
  - 语言切换下拉框（中文 / English）
  - 切换时：
    1. locale.value = 'zh-CN' | 'en-US'
    2. localStorage.setItem('locale', locale.value)
    3. 页面即时切换（vue-i18n 响应式支持）
```

### 9.5 第一批（核心）：ABAC 细粒度权限

#### 9.5.1 useAuthStore 扩展

```
[P3 MODIFY]
路径：src/stores/auth.ts

当前代码（P2）：
  - user: { userId, username, role, title, department, clearance }
  - canViewDashboard: computed → director + clearance===3
  - canManageUsers: computed → title>=2 + clearance>=2

P3 新增计算属性：
  canEditKnowledge: computed    → 知识库编辑权限（基于 role + department）
  canDeleteKnowledge: computed  → 知识库删除权限（基于 role + clearance）
  canConfigureDatasource: computed → 数据源配置权限（基于 role）
  canExportReport: computed     → 报表导出权限（基于 department + clearance）
  fieldLevel: {
    salary: computed            → 薪资字段可见性（title>=3 + clearance>=3）
    performance: computed       → 绩效字段可见性（title>=2 + clearance>=2）
  }

★ 权限逻辑基于 ABAC 属性：user.title（职级 1-4）、user.clearance（密级 1-3）、
  user.department（部门）、user.role（角色）
```

#### 9.5.2 v-permission 自定义指令

```
[P3 NEW]
路径：src/directives/permission.ts

导出：
  - v-permission 指令
  - 用法示例：
    <button v-permission="'canEditKnowledge'">编辑</button>
    <td v-permission="'fieldLevel.salary'">{{ user.salary }}</td>

实现：
  1. 注册全局指令（在 main.ts 中 app.directive('permission', permission)）
  2. mounted 钩子：检查 authStore 中的对应权限
  3. 无权限时：el.style.display = 'none' 或 el.disabled = true
  4. 支持修饰符：v-permission:disable（禁用而非隐藏）

main.ts [P3 MODIFY]:
  - import permission from './directives/permission'
  - app.directive('permission', permission)
```

#### 9.5.3 逐页添加权限控制

```
需修改的页面：
  - src/views/knowledge/index.vue   → 上传/编辑/删除按钮加 v-permission
  - src/views/datasource/index.vue  → 新增/配置按钮加 v-permission
  - src/views/user/index.vue        → 敏感字段（薪资/绩效）加 v-permission
  - src/views/dashboard/index.vue   → 导出按钮加 v-permission
```

### 9.6 第二批（体验）：移动端响应式适配

#### 9.6.1 useBreakpoint Composable

```
[P3 NEW]
路径：src/composables/useBreakpoint.ts

导出：
  - breakpoint: Ref<'mobile' | 'tablet' | 'desktop'>
  - isMobile: ComputedRef<boolean>   (width < 768px)
  - isTablet: ComputedRef<boolean>   (768px <= width < 1024px)
  - isDesktop: ComputedRef<boolean>  (width >= 1024px)

实现：
  - 使用 window.matchMedia + resize 事件监听
  - 防抖处理（debounce 200ms）
```

#### 9.6.2 Sidebar 抽屉化

```
[P3 MODIFY]
路径：src/components/layout/Sidebar.vue

移动端（< 768px）：
  - 默认隐藏 Sidebar
  - 显示 hamburger 菜单按钮（Header 左侧）
  - 点击 hamburger → 从左侧滑入抽屉式 Sidebar + 半透明 overlay
  - 点击 overlay 或菜单项 → 关闭抽屉
  - 过渡动画：transform translateX + opacity

桌面端（>= 768px）：
  - 保留 P2 的固定侧边栏行为不变
```

#### 9.6.3 页面移动端适配

```
需修改的页面：
  - src/views/chat/index.vue      → 输入框全宽，消息气泡 max-width 调整
  - src/views/auth/Login.vue      → 表单卡片居中，去除多余 padding
  - src/views/knowledge/index.vue → 表格改为卡片列表（移动端）
  - src/views/dashboard/index.vue → 图表单列堆叠
  - src/views/datasource/index.vue → 表单全宽
  - src/views/catalog/index.vue   → 树节点增大触摸区域

CSS 策略：
  - 使用 CSS @media 查询 + Tailwind/CSS 响应式类
  - 移动端触摸优化：最小可点击区域 44x44px
```

### 9.7 第三批（增值）：知识图谱 + 质检规则 + 图表增强

#### 9.7.1 KnowledgeGraph 组件

```
[P3 NEW]
路径：src/components/charts/KnowledgeGraph.vue

功能：
  - 使用 ECharts graph 类型图表
  - 节点（3 种颜色）：域节点（蓝）、数据源节点（绿）、实体节点（橙）
  - 边：包含关系、关联关系
  - 布局：力导向（force layout）
  - 交互：
    1. 节点拖拽
    2. 滚轮缩放
    3. 点击节点展开/折叠子节点
    4. 搜索框高亮匹配节点

Props:
  - catalogTree: CatalogTree  → 后端 GET /api/catalog/tree 返回的数据

数据转换：
  catalogTree.domains[] → nodes[]:
    { id, name, category: 'domain', symbolSize: 50 }
  catalogTree.domains[].sources[] → nodes[]:
    { id, name, category: 'source', symbolSize: 30 }
  catalogTree.domains[].sources[].entities[] → nodes[]:
    { id, name, category: 'entity', symbolSize: 15 }
  links[]:
    { source: domain.id, target: source.id }
    { source: source.id, target: entity.id }
```

#### 9.7.2 知识目录页改造

```
[P3 MODIFY]
路径：src/views/catalog/index.vue

当前代码（P2）：
  - 三级树形目录（使用 Element Plus Tree 组件）

P3 新增：
  - Tab 切换："树形视图" | "图谱视图"
  - 图谱视图 Tab 嵌入 KnowledgeGraph 组件
  - 两个视图共享同一份 catalogTree 数据
```

#### 9.7.3 质检规则配置 UI

```
[P3 NEW]
路径：src/views/quality/RulesManager.vue

功能：
  - 规则列表表格（CRUD）
  - 规则类型：完整性、唯一性、格式、编码、长度
  - 每条规则：名称、类型、参数（阈值/正则/枚举）、启用状态
  - 操作：新增、编辑、删除、启用/禁用

路径：src/components/quality/RuleEditor.vue  [P3 NEW]
  - 规则编辑表单
  - 类型选择器 → 动态显示对应的参数配置项
  - 实时预览区：输入样例数据 → 查看规则是否匹配
```

#### 9.7.4 仪表盘图表增强

```
[P3 MODIFY]
路径：src/views/dashboard/index.vue

当前代码（P2）：
  - 静态时间范围统计卡片
  - 满意度饼图
  - 热门问答 Top N 柱状图

P3 新增：
  1. 时间范围拖拽选择器（date range picker）
  2. 热门问答柱状图点击 → 下钻显示该问答的详细对话记录
  3. 导出按钮：ECharts 图表导出为 PNG（使用 getDataURL() + download）
```

### 9.8 语音输入（可选）

```
[P3 NEW]
路径：src/composables/useSpeechRecognition.ts

实现：
  - 基于浏览器 Web Speech API（window.SpeechRecognition 或 webkitSpeechRecognition）
  - 导出：
    - isListening: Ref<boolean>
    - transcript: Ref<string>
    - startListening(lang: 'zh-CN' | 'en-US'): void
    - stopListening(): void
    - isSupported: ComputedRef<boolean>  // 检查浏览器兼容性

集成到聊天页：
  - 输入框旁新增麦克风按钮
  - 长按或点击开始录音 → 语音识别结果填入输入框
  - 不支持时隐藏按钮
```

### 9.9 pom.xml 变更（前端）

```
无 Maven 变更。前端通过 pnpm 管理依赖：
  - pnpm add sockjs-client @stomp/stompjs vue-i18n@9
  - pnpm add -D @types/sockjs-client
```

### 9.10 子 Agent 任务清单

**第一批（核心 — 优先实施）**：
- [ ] **5.1** 安装依赖：`sockjs-client`、`@stomp/stompjs`、`vue-i18n@9`
- [ ] **5.2** 创建 `useWebSocket.ts` Composable
- [ ] **5.3** 改造聊天页 `chat/index.vue`（SSE → WebSocket + 降级）
- [ ] **5.4** 创建 `src/i18n/` 基础设施（index.ts + zh-CN.ts + en-US.ts）
- [ ] **5.5** 注册 i18n 到 `main.ts`
- [ ] **5.6** 逐页替换硬编码中文为 `$t()`（10 个页面/组件）
- [ ] **5.7** Header 添加语言切换 UI
- [ ] **5.8** 扩展 `auth.ts` — 新增 ABAC 细粒度计算属性
- [ ] **5.9** 创建 `v-permission` 指令（`directives/permission.ts`）
- [ ] **5.10** 逐页添加权限控制按钮/字段

**第二批（体验）**：
- [ ] **5.11** 创建 `useBreakpoint.ts` Composable
- [ ] **5.12** 改造 `Sidebar.vue`（移动端抽屉化）
- [ ] **5.13** 改造聊天/登录/知识库/仪表盘/数据源页面移动端适配
- [ ] **5.14** 仪表盘图表增强（时间范围选择器 + 下钻 + 导出）

**第三批（增值）**：
- [ ] **5.15** 创建 `KnowledgeGraph.vue` 组件
- [ ] **5.16** 改造 `catalog/index.vue`（图谱视图 Tab）
- [ ] **5.17** 创建 `RulesManager.vue` 规则管理页
- [ ] **5.18** 创建 `RuleEditor.vue` 规则编辑器
- [ ] **5.19** 创建 `useSpeechRecognition.ts`（语音输入，可选）

**验证**：
- [ ] **5.20** `pnpm build` 编译通过，无 TypeScript 错误
- [ ] **5.21** `pnpm dev` 启动开发服务器，验证核心功能

---

## 十、子 Agent 创建指南

### 10.1 子 Agent Prompt 模板

为每个模块创建子 Agent 时，使用以下模板：

```
你是一个 P3 阶段代码实现 Agent，负责实现 agent-qr 项目
【{模块名}】模块的 P3 扩展。

## 项目上下文
- Java 21 + Spring Boot 3.5.15 + MyBatis-Plus 3.5.14 + LangChain4j 1.16.3
- 前端：Vue 3 + TypeScript + Pinia + Vite
- 根 POM：D:\Javacode\agent-qr\pom.xml
- 模块路径：D:\Javacode\agent-qr\{模块目录}

## 当前模块已有代码（P1/P2，必须保留）
{列出该模块已有的关键类文件及其用途}

## 需要新建的 P3 类
{从本 Prompt 对应的模块章节复制完整类规格 — 包路径、注解、字段、方法、设计要点}

## 需要修改的 P3 类
{从本 Prompt 对应的模块章节复制修改点 — 当前代码结构 + P3 新增/修改内容}

## POM 依赖变更
{如有，明确列出}

## DDL 变更
{如有，明确列出。P3 无 DDL 变更}

## 要求
1. ★ 保留所有 P1/P2 已有代码，只在已有类上新增方法/注入，不删除或重写原有逻辑
2. ★ 新类必须放在正确的包路径下（见每个类的规格说明）
3. ★ 所有 import 语句必须完整，不使用通配符 import
4. ★ 所有 public 方法必须有 Javadoc（描述 + @param + @return + @throws）
5. 使用 Lombok 注解（@Data、@Slf4j 等），遵循项目惯例
6. 使用 Spring 注解（@Component、@Service、@Autowired 等），遵循项目惯例
7. 业务异常使用 BusinessException（来自 agent-qr-common），不使用 RuntimeException
8. 不实现本文档未提及的任何功能
9. 完成后列出所有新建/修改的文件完整路径

## 特殊注意事项
{填入 10.2 特殊注意事项矩阵中对应模块的内容}
```

### 10.2 特殊注意事项矩阵

| 模块 | 注意事项 |
|------|---------|
| **agent-qr-common** | ① ThreadLocal 必须在 finally 中 clear()，防止线程池污染 ② `afterPropertiesSet()` 必须在 `initDataSources()` 末尾调用 ③ 不能依赖任何业务模块 |
| **agent-qr-web** | ① `CqrsDataSourceConfig` 必须用 `@ConditionalOnProperty` 条件化，P1/P2 profile 不加载 ② `writeDataSource()` 必须标记 `@Primary` ③ `application-p3.yml` 中的读库初期可指向主库同实例 |
| **agent-qr-rag** | ① `DomainRouterV2` 注入 `catalogService` 必须用 `required=false`，避免编译时硬依赖 ② `ChatQueryService.resolveRouting()` 的降级链：P3 语义 → P2 关键词 → 全局检索 ③ `cosineSimilarity()` 需处理零向量边界 ④ `EmbeddingDimensionManager` 的 Collection 名称中 `:` 替换为 `-` |
| **agent-qr-catalog** | ① P2 `DomainRouter` 代码完全保留，作为降级方案 ② `DomainRouterV2.catalog` 版与 `DomainRouterV2.rag` 版通过 `KnowledgeCatalogService` 解耦 ③ `@EventListener` 监听目录变更，但刷新是懒加载的（标记过期而非立即重算） |
| **agent-qr-web-frontend** | ① WebSocket 改造必须保留 SSE 降级路径 ② i18n 替换硬编码中文时要逐个页面完整替换，不遗漏 ③ 移动端适配使用 CSS `@media` 查询为主要手段，JS `matchMedia` 为辅助 ④ `v-permission` 指令的隐藏逻辑用 `display:none` 不占位，禁用逻辑用 `disabled` 属性 |

---

## 十一、验证清单

### 11.1 代码层面

- [ ] 所有新建类的包路径与本文档一致
- [ ] 所有 P1/P2 已有代码未被删除或重写
- [ ] 所有 `@Transactional(readOnly)` 正确触发读写分离路由
- [ ] `ThreadLocal` 在所有代码路径中都有 `finally` 清理
- [ ] `@ConditionalOnProperty` 确保 P3 Bean 仅 P3 profile 下加载
- [ ] 降级链完整：P3 语义路由 → P2 关键词路由 → 全局检索
- [ ] Collection 名称不包含非法字符（`:` 替换为 `-`）
- [ ] 前端 `v-permission` 覆盖所有敏感操作按钮
- [ ] 前端 i18n 无遗漏硬编码中文

### 11.2 模块层面

- [ ] agent-qr-common：`mvn compile -pl agent-qr-common` 通过
- [ ] agent-qr-web：`mvn compile -pl agent-qr-web -am` 通过
- [ ] agent-qr-rag：`mvn compile -pl agent-qr-rag -am` 通过
- [ ] agent-qr-catalog：`mvn compile -pl agent-qr-catalog -am` 通过
- [ ] agent-qr-web-frontend：`pnpm build` 通过

### 11.3 全局层面

- [ ] `mvn compile -pl agent-qr-web -am` 全量编译通过
- [ ] P3 profile 启动无异常：`mvn spring-boot:run -Dspring-boot.run.profiles=p1,p2,p3`
- [ ] P1/P2 profile 启动不受影响（向后兼容）：`mvn spring-boot:run -Dspring-boot.run.profiles=p1,p2`
- [ ] 前端 `pnpm dev` 开发服务器正常启动

### 11.4 核心功能验证点

| # | 验证点 | 预期行为 |
|---|--------|---------|
| 1 | CQRS 路由 | `@Transactional(readOnly=true)` → 数据源 lookup key = "read" |
| 2 | CQRS 路由 | `@Transactional(readOnly=false)` → 数据源 lookup key = "write" |
| 3 | 语义路由 | 输入"员工薪资怎么算" → 匹配 HR 域（相似度 > 0.3） |
| 4 | 语义路由 | 输入"今天天气怎么样" → 所有域相似度 < 0.3 → 回退全局检索 |
| 5 | 维度检测 | 切换 Embedding Provider 后重启 → 日志提示新 Collection 不存在 |
| 6 | Provider 决策 | 熔断器 OPEN → 自动切换到备用 Provider |
| 7 | 降级链路 | DomainRouterV2 异常 → 降级 DomainRouter → 降级全局检索 |
| 8 | 前端 WebSocket | 聊天发送消息 → WebSocket 流式接收回答 |
| 9 | 前端 i18n | 切换英文 → 所有页面 UI 文本即时切换 |
| 10 | 前端 ABAC | 普通用户 → 编辑/删除按钮隐藏；管理员 → 按钮可见 |
| 11 | 前端移动端 | 浏览器宽度 < 768px → Sidebar 变为抽屉式 |
| 12 | 前端知识图谱 | 目录页切换到图谱视图 → 力导向图展示域/数据源/实体关系 |

---

> **文档版本**：v1.0（2026-06-28）
>
> **输入文档**：
> - `doc/系统详细设计说明书.md` v1.0（P3 类设计 + CQRS 方案 + 算法描述）
> - `doc/p3-tasks/` 全部 6 个任务文件（子任务清单 + 进度追踪）
> - `doc/p2-prompt.md` v1.0（结构与格式参考）
> - 实际代码：`MybatisPlusConfig`、`ProviderFactory`、`DomainRouter`、`ChatQueryService`、`HybridRetriever`、`BatchEmbeddingService`、`KnowledgeCatalogService`、`CatalogController`、`SecurityConfig`、`application.yml`、`auth.ts`、`Sidebar.vue`
