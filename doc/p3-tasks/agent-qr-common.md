# agent-qr-common -- P3 任务清单

> 公共模块 P3 扩展：CQRS 读写分离数据源路由
> 更新日期：2026-06-28
> 阶段目标：基于 Spring AbstractRoutingDataSource 实现读写分离，通过 AOP 切面自动路由

---

## 1. CQRS 读写分离数据源 — ReadWriteRoutingDataSource

- [ ] **1.1** 创建 `org.example.agent_qr.common.datasource` 包
  - 在 `agent-qr-common/src/main/java/org/example/agent_qr/common/` 下新建 `datasource/` 目录

- [ ] **1.2** 创建 `ReadWriteRoutingDataSource` 类
  - 路径：`org.example.agent_qr.common.datasource.ReadWriteRoutingDataSource`
  - 继承 `org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource`
  - 注解：`@Component`
  - 静态属性：`private static final ThreadLocal<Boolean> READ_ONLY_HOLDER = new ThreadLocal<>()`
  - 静态方法：`public static void setReadOnly(boolean readOnly)` — 设置当前线程读写标志
  - 静态方法：`public static void clear()` — 清除 ThreadLocal，防止内存泄漏
  - 重写方法：`protected Object determineCurrentLookupKey()` — 返回 `"read"` 或 `"write"`
  - 初始化方法：`@PostConstruct void initDataSources()` — 构建 targetDataSources Map
    - key `"write"` → 主库 DataSource（默认）
    - key `"read"` → 从库 DataSource（初期可与主库同一实例）
    - 设置 `setDefaultTargetDataSource(writeDataSource)`

- [ ] **1.3** 注入主库/从库 DataSource
  - 通过 `@Value` 或 `@ConfigurationProperties` 读取 `application-p3.yml` 中的数据源配置
  - 主库：`spring.datasource.write.*` 或复用现有 `spring.datasource.*`
  - 从库：`spring.datasource.read.*`（初期可指向同一数据库实例）
  - 使用 `DataSourceBuilder` 构建 `HikariDataSource` 实例

---

## 2. CQRS 读写分离切面 — ReadWriteDataSourceAspect

- [ ] **2.1** 创建 `ReadWriteDataSourceAspect` 类
  - 路径：`org.example.agent_qr.common.datasource.ReadWriteDataSourceAspect`
  - 注解：`@Aspect`、`@Component`、`@Order(-1)`（确保在 Spring 事务拦截器之前执行）
  - 注解：`@Slf4j`

- [ ] **2.2** 实现切面方法 `routeDataSource`
  - 注解：`@Around("@annotation(transactional)")`
  - 参数：`ProceedingJoinPoint pjp`、`Transactional transactional`
  - 逻辑：
    1. 从 `transactional.readOnly()` 读取读写标志
    2. 调用 `ReadWriteRoutingDataSource.setReadOnly(readOnly)`
    3. 执行 `pjp.proceed()`
    4. 在 `finally` 块中调用 `ReadWriteRoutingDataSource.clear()` 清理 ThreadLocal
  - 异常处理：记录日志后重新抛出

---

## 3. 模块配置更新

- [ ] **3.1** 更新 `agent-qr-common/pom.xml`（如需要）
  - 确认 `spring-boot-starter-aop` 依赖已存在（用于 `@Aspect`）
  - 确认 `spring-boot-starter-jdbc` 依赖已存在（用于 `AbstractRoutingDataSource`）
  - P3 无新增外部依赖（基于 Spring 内置能力）

- [ ] **3.2** 确保 `MybatisPlusConfig` 兼容多数据源
  - 路径：`org.example.agent_qr.common.config.MybatisPlusConfig`
  - 确认 `SqlSessionFactory` 使用 `AbstractRoutingDataSource` 作为数据源
  - 如当前配置硬编码单数据源，需改为注入 `ReadWriteRoutingDataSource`

---

> **依赖关系**：本模块无模块间依赖（common 为基础模块）
>
> **统计**：共 3 大类，约 7 个子任务
> 预计耗时：0.5 天
