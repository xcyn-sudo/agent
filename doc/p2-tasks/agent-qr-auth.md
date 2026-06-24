# agent-qr-auth — P2 任务清单

> 认证授权模块 P2 扩展：ABAC 属性访问控制、双 Token 机制（Access 30min + Refresh 7day）、JWT 属性编码。

---

## 1. ABAC 扩展用户主体 — UserPrincipal

- [ ] **1.1** 创建 `org.example.agent_qr.auth.principal.UserPrincipal` 类
  - 注解 `@Data`
  - 属性：`Long userId`、`String username`、`String role`、`String department`、`Integer clearanceLevel`、`List<String> allowedDomains`、`String title`
  - 静态方法 `fromClaims(Claims claims)`：从 JWT Claims 构建，allowedDomains 在 JWT 中存为逗号分隔字符串
  - 方法 `isAdmin()`：判断 role == "admin"
  - 方法 `hasDomainAccess(String domain)`：检查 allowedDomains 是否包含 domain 或 domain == department
  - 方法 `hasClearance(int resourceLevel)`：检查 clearanceLevel >= resourceLevel

---

## 2. ABAC 属性策略评估器 — AbacEvaluator

- [ ] **2.1** 创建 `org.example.agent_qr.auth.evaluator.AbacEvaluator` 类
  - 注解 `@Component("abac")`、`@Slf4j`
  - 属性：`DocumentMapper documentMapper`
  - 方法 `canQueryDomain(UserPrincipal user, String domain)`：admin→允许，user.allowedDomains 包含 domain→允许，user.department==domain→允许，否则拒绝
  - 方法 `canAccessDocument(UserPrincipal user, Long documentId)`：admin→允许，用户密级≥文档密级 AND 用户有文档域权限→允许
  - 方法 `canUploadToDomain(UserPrincipal user, String domain)`：admin→允许，canQueryDomain + 职级要求 manager/director
  - 方法 `canDeleteDocument(UserPrincipal user, Long documentId)`：admin→允许，canAccessDocument + 职级要求 director
  - 方法 `canModifyUser(UserPrincipal user, Long targetUserId)`：admin→允许所有，本人→允许改自己
  - 方法 `canManageDatasource(UserPrincipal user)`：仅 admin 允许
  - 所有拒绝分支记录 `log.warn` 结构化日志

---

## 3. JwtUtil P2 扩展（ABAC 属性编码 + 双 Token）

- [ ] **3.1** 扩展 `org.example.agent_qr.auth.util.JwtUtil` 类
  - 新增属性：`accessExpiration`（默认 1800s = 30min）、`refreshExpiration`（默认 604800s = 7day）
  - 新增方法 `generateAccessToken(SysUser user)`：JWT Payload 含 userId/role/department/clearanceLevel/allowedDomains/title
  - 新增方法 `generateRefreshToken(SysUser user)`：JWT Payload 仅含 userId/tokenType="refresh"，7 天过期
  - 新增方法 `parseUserPrincipal(String token)`：解析 JWT → 返回 `UserPrincipal`
  - 保留 P1 原有方法 `generateToken`、`getUsernameFromToken`、`validateToken`（向后兼容）

---

## 4. JwtAuthenticationFilter P2 扩展（注入 ABAC UserPrincipal）

- [ ] **4.1** 改造 `org.example.agent_qr.auth.filter.JwtAuthenticationFilter` 类
  - Token 验证通过后，调用 `jwtUtil.parseUserPrincipal(token)` 替代原来的 SysUser 查询
  - 构建 `UsernamePasswordAuthenticationToken` 时，principal 设为 `UserPrincipal` 实例（替代 P1 的 SysUser）
  - 将 allowedDomains 编码为 `SimpleGrantedAuthority("DOMAIN_" + d)` 用于 hasAuthority 匹配
  - 保留 `extractToken` 方法（从 Authorization 头提取 Bearer Token）

---

## 5. SecurityConfig P2 改造（@EnableMethodSecurity + ABAC 路由）

- [ ] **5.1** 改造 `org.example.agent_qr.auth.config.SecurityConfig` 类
  - 新增注解 `@EnableMethodSecurity(prePostEnabled = true)` 开启 @PreAuthorize
  - 路由规则更新：
    - `/api/auth/login`、`/api/auth/register`、`/api/auth/refresh` → `permitAll()`
    - `/api/admin/**` → `hasRole("ADMIN")`
    - `/api/knowledge/**`、`/api/chat/**`、`/api/catalog/**` → `authenticated()`
    - 其余 → `authenticated()`
  - 保留 P1 的 CSRF 禁用、无状态 Session、JWT 过滤器注入、BCryptPasswordEncoder Bean

---

## 6. ABAC 权限拒绝异常处理 — AbacAccessDeniedHandler

- [ ] **6.1** 创建 `org.example.agent_qr.auth.handler.AbacAccessDeniedHandler` 类
  - 注解 `@RestControllerAdvice`
  - 方法 `handleAccessDenied(AccessDeniedException e, HttpServletRequest request)`：`@ExceptionHandler(AccessDeniedException.class)`
  - 从 SecurityContext 提取 UserPrincipal（含 username、department）
  - 从 MDC 提取 traceId
  - 构建结构化审计日志 Map（user、department、uri、method、traceId、timestamp、reason）
  - `log.warn("ABAC 权限拒绝: {}", JSON.toJSONString(auditLog))`
  - 返回 `ResponseEntity.status(403).body(Result.error(403, "权限不足: " + e.getMessage()))`

---

## 7. RefreshTokenService — 双 Token 机制

- [ ] **7.1** 创建 `org.example.agent_qr.auth.service.RefreshTokenService` 类
  - 注解 `@Service`、`@Slf4j`
  - 属性：`TokenRefreshMapper`、`JwtUtil`
  - 方法 `issueTokens(SysUser user)`：签发 Access Token（30min）+ Refresh Token（7day），Refresh Token 写入 token_refresh 表，返回 `TokenPair(accessToken, refreshToken, expiresIn)`
  - 方法 `refresh(String refreshToken)`：验证 Refresh Token → 查询 DB 未撤销 → 删除旧 Refresh → 签发新令牌对（令牌轮换）
  - 方法 `revoke(Long userId)`：撤销该用户所有 Refresh Token

---

## 8. TokenRefresh 实体 & Mapper

- [ ] **8.1** 创建 `org.example.agent_qr.auth.entity.TokenRefresh` 实体
  - 注解 `@Data`、`@TableName("token_refresh")`
  - 属性：`Long id`、`Long userId`、`String token`（TEXT）、`Boolean revoked`（默认 false）、`LocalDateTime createTime`、`LocalDateTime expireTime`

- [ ] **8.2** 创建 `org.example.agent_qr.auth.mapper.TokenRefreshMapper` 接口
  - 继承 `BaseMapper<TokenRefresh>`
  - 方法 `selectByToken(String token)`：查询未撤销的 Refresh Token
  - 方法 `revokeByUserId(Long userId)`：撤销用户所有 Token

---

## 9. AuthController P2 扩展

- [ ] **9.1** 在 `AuthController` 中新增端点
  - `POST /api/auth/refresh`：接收 `RefreshDTO(refreshToken)` → 调用 `RefreshTokenService.refresh()` → 返回新 TokenPair
  - 登录接口 `POST /api/auth/login` 改造：返回双 Token（accessToken + refreshToken）替代原单 Token

---

## 10. Controller 层 ABAC 注解集成

- [ ] **10.1** 在 `KnowledgeController` 中标注 ABAC 注解
  - `GET /api/knowledge/documents/{id}` → `@PreAuthorize("@abac.canAccessDocument(principal, #id)")`
  - `POST /api/knowledge/upload` → `@PreAuthorize("@abac.canUploadToDomain(principal, #domain)")`
  - `DELETE /api/knowledge/{id}` → `@PreAuthorize("@abac.canDeleteDocument(principal, #id)")`

- [ ] **10.2** 在 `ChatController` 中标注 ABAC 注解
  - `POST /api/chat/ask` → `@PreAuthorize("@abac.canQueryDomain(principal, #request.domain)")`

- [ ] **10.3** 在 `AdminController` 中标注 ABAC 注解
  - `PUT /api/admin/users/{id}` → `@PreAuthorize("@abac.canModifyUser(principal, #id)")`
  - `POST /api/admin/datasource` → `@PreAuthorize("@abac.canManageDatasource(principal)")`

---

## 11. DDL — token_refresh 表

- [ ] **11.1** 编写 `token_refresh` 建表 SQL（追加到 p2-schema.sql）
  - 字段：`id BIGINT AUTO_INCREMENT PRIMARY KEY`、`user_id BIGINT NOT NULL`、`token TEXT NOT NULL`、`revoked TINYINT DEFAULT 0`、`create_time DATETIME`、`expire_time DATETIME`
  - 索引：`idx_token (token(64))`、`idx_user_id (user_id)`

---

## 12. pom.xml 依赖

- [ ] **12.1** 确认 `agent-qr-auth/pom.xml` 无需新增依赖（ABAC 为纯内存属性比较，双 Token 基于已有 jjwt）
