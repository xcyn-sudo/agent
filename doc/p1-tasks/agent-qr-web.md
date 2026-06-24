# agent-qr-web — P1 任务清单

> Web 启动模块：启动类（含全局扫描）、全局异常处理、CORS 配置、SecurityConfig、AsyncConfig、application.yml / application-p1.yml。

---

## 1. 模块重命名

- [ ] **1.1** 将 `gent-qr-web` 目录重命名为 `agent-qr-web`
- [ ] **1.2** 更新根 `pom.xml` 中 `<module>gent-qr-web</module>` 为 `<module>agent-qr-web</module>`
- [ ] **1.3** 更新 `agent-qr-web/pom.xml` 中的 `artifactId` 和依赖关系

---

## 2. 根 pom.xml 更新

- [ ] **2.1** 在根 `pom.xml` 的 `<modules>` 中新增：
  - `<module>agent-qr-user</module>`
  - `<module>agent-qr-rag</module>`
  - 修正 `<module>gent-qr-web</module>` → `<module>agent-qr-web</module>`

---

## 3. 启动类 AgentQrApplication

- [ ] **3.1** 创建/修改 `org.example.agent_qr.AgentQrApplication` 类
  - 注解 `@SpringBootApplication`
  - 手动添加 `@ComponentScan(basePackages = "org.example.agent_qr")` — 确保扫描所有模块
  - `main()` 方法：`SpringApplication.run(AgentQrApplication.class, args)`

---

## 4. 全局异常处理 GlobalExceptionHandler

- [ ] **4.1** 创建 `org.example.agent_qr.web.config.GlobalExceptionHandler` 类
  - 注解 `@RestControllerAdvice`、`@Slf4j`
  - `@ExceptionHandler(BusinessException.class)` → `Result<Void>`（code + message）
  - `@ExceptionHandler(MethodArgumentNotValidException.class)` → `Result<Void>`（400 + 校验消息拼接）
  - `@ExceptionHandler(AccessDeniedException.class)` → `Result<Void>`（403）
  - `@ExceptionHandler(Exception.class)` → `Result<Void>`（500 + 通用错误）

---

## 5. 跨域配置 CorsConfig

- [ ] **5.1** 创建 `org.example.agent_qr.web.config.CorsConfig` 类
  - 注解 `@Configuration`
  - 实现 `WebMvcConfigurer`
  - 重写 `addCorsMappings()`：允许所有来源（开发阶段）、允许 GET/POST/PUT/DELETE、允许 `Authorization` 头

---

## 6. Spring Security 配置 SecurityConfig

- [ ] **6.1** 创建 `org.example.agent_qr.web.config.SecurityConfig` 类
  - 注解 `@Configuration`、`@EnableWebSecurity`、`@EnableMethodSecurity`
  - 注入 `JwtAuthenticationFilter`（来自 auth 模块）
  - `SecurityFilterChain filterChain(HttpSecurity http)`：
    1. 禁用 CSRF
    2. 无状态 Session（`SessionCreationPolicy.STATELESS`）
    3. 路由权限：
       - `/api/auth/login`、`/api/auth/register` → `permitAll()`
       - `/api/admin/**` → `hasRole("ADMIN")`
       - `/api/knowledge/**` → `hasRole("ADMIN")`
       - `/api/chat/**` → `authenticated()`
       - 其他 → `authenticated()`
    4. `addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)`
  - `PasswordEncoder passwordEncoder()`：返回 `BCryptPasswordEncoder(12)`

---

## 7. 异步线程池配置 AsyncConfig

- [ ] **7.1** 创建 `org.example.agent_qr.web.config.AsyncConfig` 类
  - 注解 `@Configuration`、`@EnableAsync`
  - `Executor docProcessExecutor()`（Bean 名 `docProcessExecutor`）：
    - corePoolSize=4, maxPoolSize=8, queueCapacity=100
    - 前缀 `doc-process-`
    - 拒绝策略 `CallerRunsPolicy`
  - `Executor statExecutor()`（Bean 名 `statExecutor`）：
    - corePoolSize=2, maxPoolSize=4, queueCapacity=50
    - 前缀 `stat-`
    - 拒绝策略 `CallerRunsPolicy`

---

## 8. 配置文件 application.yml

- [ ] **8.1** 创建 `src/main/resources/application.yml`（公共配置）
  ```yaml
  spring:
    application.name: agent-qr
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

- [ ] **8.2** 创建 `src/main/resources/application-p1.yml`（P1 阶段配置）
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

## 9. pom.xml 依赖

- [ ] **9.1** 在 `agent-qr-web/pom.xml` 中配置依赖（依赖所有业务模块）
  - `agent-qr-common`
  - `agent-qr-auth`
  - `agent-qr-user`
  - `agent-qr-knowledge`
  - `agent-qr-rag`
  - `agent-qr-statistics`
  - `spring-boot-starter-web`
  - `spring-boot-starter-security`
  - `mysql-connector-j`
  - `spring-boot-starter-validation`

---

## 10. 数据库初始化 SQL（可选）

- [ ] **10.1** 创建 `src/main/resources/db/p1-schema.sql`（建表语句）
  - `sys_user` 表
  - `kb_document` 表
  - `kb_chunk` 表
  - `chat_conversation` 表
  - `chat_message` 表
  - `stat_daily` 表
