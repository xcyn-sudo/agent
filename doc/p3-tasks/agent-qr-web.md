# agent-qr-web -- P3 任务清单

> Web 启动模块 P3 扩展：CQRS 配置 + P3 阶段配置文件
> 更新日期：2026-06-28
> 阶段目标：装配读写分离数据源 Bean，提供 P3 阶段专属配置

---

## 1. P3 阶段配置文件 — application-p3.yml

- [ ] **1.1** 创建 `application-p3.yml`
  - 路径：`agent-qr-web/src/main/resources/application-p3.yml`
  - 配置内容：

```yaml
# ============================================================
# P3 阶段配置：CQRS 读写分离 + Embedding 语义路由
# 激活方式：spring.profiles.active=p3
# ============================================================

spring:
  # --- 读写分离数据源 ---
  datasource:
    write:
      url: jdbc:mysql://localhost:3308/agent_qr?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      username: root
      password: ${DB_PASSWORD:root}
      driver-class-name: com.mysql.cj.jdbc.Driver
      hikari:
        maximum-pool-size: 20
        minimum-idle: 5
    read:
      url: jdbc:mysql://localhost:3309/agent_qr?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      username: root
      password: ${DB_PASSWORD:root}
      driver-class-name: com.mysql.cj.jdbc.Driver
      hikari:
        maximum-pool-size: 10
        minimum-idle: 2

  # --- 激活 P3 Profile ---
  profiles:
    active: p3

# --- P3 专属配置 ---
agent-qr:
  # CQRS 读写分离
  cqrs:
    enabled: true
    # 初期从库可指向主库同一实例（逐步迁移）
    read-replica-fallback-to-primary: true

  # 语义路由
  routing:
    mode: semantic           # keyword | semantic | auto
    similarity-threshold: 0.3 # 余弦相似度阈值
    top-k: 2                 # 返回 Top K 个匹配域

  # Provider 自动切换
  provider:
    auto-failover: true      # 熔断时自动切换备用 Provider
    preferred-llm: deepseek  # 首选 LLM Provider
    preferred-embedding: deepseek  # 首选 Embedding Provider

  # 向量维度管理
  embedding:
    collection-prefix: kb    # ChromaDB Collection 前缀
    auto-dimension-check: true  # 启动时自动检测维度一致性
```

- [ ] **1.2** 保留 `application.yml` 公共配置
  - 确认公共配置（如 `server.port`、`spring.application.name`）保留在 `application.yml`
  - P3 专属配置仅在 `application-p3.yml` 中
  - 通过 `spring.profiles.active=p3` 激活（启动参数或环境变量）

---

## 2. CQRS 数据源 Bean 装配

- [ ] **2.1** 创建 `CqrsDataSourceConfig` 配置类
  - 路径：`org.example.agent_qr.web.config.CqrsDataSourceConfig`
  - 注解：`@Configuration`、`@ConditionalOnProperty(name = "agent-qr.cqrs.enabled", havingValue = "true")`
  - 注解：`@Slf4j`

- [ ] **2.2** 定义主库 DataSource Bean
  - 方法：`@Bean @Primary @ConfigurationProperties("spring.datasource.write") public DataSource writeDataSource()`
  - 返回 `HikariDataSource`
  - 标记 `@Primary` 确保其他模块默认注入主库

- [ ] **2.3** 定义从库 DataSource Bean
  - 方法：`@Bean @ConfigurationProperties("spring.datasource.read") public DataSource readDataSource()`
  - 返回 `HikariDataSource`
  - 初期若无从库：通过 `agent-qr.cqrs.read-replica-fallback-to-primary=true` 返回主库实例

- [ ] **2.4** 确认 `ReadWriteRoutingDataSource` 自动装配
  - `ReadWriteRoutingDataSource` 在 common 模块已标记 `@Component`
  - 确保组件扫描覆盖 `org.example.agent_qr.common.datasource`
  - 无需额外 Bean 定义，Spring 自动发现

---

## 3. SecurityConfig 适配

- [ ] **3.1** 更新 `SecurityConfig` — 确认 P3 端点放行
  - 路径：`org.example.agent_qr.web.config.SecurityConfig`
  - 检查项：
    - `/api/catalog/route`（如新增）加入认证端点列表
    - WebSocket 端点（如 P3 前端引入）需配置放行
  - 保留 P2 已有配置不变

- [ ] **3.2** 添加 WebSocket 支持（如需要）
  - 仅当前端 P3 实现 WebSocket 通信时需要
  - 配置 `WebSocketConfig` 类
  - 路径：`org.example.agent_qr.web.config.WebSocketConfig`

---

## 4. 启动验证

- [ ] **4.1** 验证 P3 Profile 启动
  - 启动命令：`mvn spring-boot:run -Dspring-boot.run.profiles=p3`
  - 验证项：
    1. `ReadWriteRoutingDataSource` 成功加载
    2. 主库/从库 DataSource Bean 正确创建
    3. 日志输出 `"CQRS 读写分离已启用: write=xxx, read=xxx"`
    4. `ReadWriteDataSourceAspect` AOP 切面生效

- [ ] **4.2** 更新 `agent-qr-web/pom.xml`（如需要）
  - P3 无新增模块依赖（CQRS 组件在 common 模块，已依赖）
  - 如需 WebSocket：添加 `spring-boot-starter-websocket`

---

> **依赖关系**：
> - `CqrsDataSourceConfig` → `ReadWriteRoutingDataSource`（common 模块）
> - `CqrsDataSourceConfig` → `application-p3.yml`（本模块配置）
>
> **统计**：共 4 大类，约 10 个子任务
> 预计耗时：0.5 天
