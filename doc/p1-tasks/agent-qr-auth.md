# agent-qr-auth — P1 任务清单

> 认证授权模块：实现用户登录、注册，JWT 生成与验证，JwtAuthenticationFilter，SecurityConfig。

---

## 1. DTO / VO

- [ ] **1.1** 创建 `org.example.agent_qr.auth.dto.LoginDTO` 类
  - 字段：`String username`、`String password`
  - 校验注解：`@NotBlank`

- [ ] **1.2** 创建 `org.example.agent_qr.auth.dto.RegisterDTO` 类
  - 字段：`String username`、`String password`、`String realName`、`String email`、`String phone`
  - 校验注解：`@NotBlank`（username、password）

- [ ] **1.3** 创建 `org.example.agent_qr.auth.dto.LoginVO` 类
  - 字段：`String token`、`Long userId`、`String username`、`String role`

---

## 2. 密码工具 PasswordUtil

- [ ] **2.1** 创建 `org.example.agent_qr.auth.util.PasswordUtil` 类
  - 注解 `@Component`
  - 持有 `BCryptPasswordEncoder(12)` 实例
  - 方法：`String encode(String rawPassword)` — BCrypt 加密
  - 方法：`boolean matches(String rawPassword, String encodedPassword)` — 验证匹配

---

## 3. JWT 工具 JwtUtil

- [ ] **3.1** 创建 `org.example.agent_qr.auth.util.JwtUtil` 类
  - 注解 `@Component`
  - 从配置读取：`${jwt.secret}`、`${jwt.expiration:86400000}`（默认 24h）
  - 方法：`String generateToken(Long userId, String username, String role)` — 生成 JWT
  - 方法：`String getUsernameFromToken(String token)` — 从 Token 解析用户名
  - 方法：`boolean validateToken(String token)` — 验证 Token 有效性
  - 私有方法：`Claims getClaimsFromToken(String token)` — 解析 Claims

---

## 4. JWT 认证过滤器 JwtAuthenticationFilter

- [ ] **4.1** 创建 `org.example.agent_qr.auth.filter.JwtAuthenticationFilter` 类
  - 继承 `OncePerRequestFilter`，注解 `@Component`
  - 注入：`JwtUtil`、`SysUserMapper`
  - `doFilterInternal()`：
    1. 从 `Authorization` 头提取 `Bearer Token`
    2. 验证 Token 有效性
    3. 解析用户名 → 查 `sys_user` 表
    4. 构建 `UsernamePasswordAuthenticationToken` 注入 `SecurityContext`
  - 私有方法：`extractToken(HttpServletRequest)` — 提取 Bearer Token

---

## 5. 认证服务 AuthService

- [ ] **5.1** 创建 `org.example.agent_qr.auth.service.AuthService` 接口
  - `LoginVO login(LoginDTO dto)`
  - `void register(RegisterDTO dto)`
  - `SysUser getCurrentUser()`

- [ ] **5.2** 创建 `org.example.agent_qr.auth.service.impl.AuthServiceImpl` 类
  - 注解 `@Service`
  - 注入：`SysUserMapper`、`PasswordUtil`、`JwtUtil`
  - `login()` 流程：
    1. 查 `sys_user` by username
    2. 用户不存在 → `BusinessException("用户名或密码错误")`
    3. status=0 → `BusinessException("账号已被禁用")`
    4. BCrypt.matches 验证密码
    5. 不匹配 → `BusinessException("用户名或密码错误")`
    6. 生成 JWT → 返回 `LoginVO`
  - `register()` 流程：
    1. 校验用户名唯一性
    2. BCrypt 加密密码
    3. 保存用户（默认 role=user, status=1）
  - `getCurrentUser()`：从 `SecurityContextHolder` 获取

---

## 6. 认证控制器 AuthController

- [ ] **6.1** 创建 `org.example.agent_qr.auth.controller.AuthController` 类
  - 注解 `@RestController`、`@RequestMapping("/api/auth")`
  - 注入 `AuthService`
  - `POST /api/auth/login` → `Result<LoginVO>`（参数 `@Valid @RequestBody LoginDTO`）
  - `POST /api/auth/register` → `Result<Void>`（参数 `@Valid @RequestBody RegisterDTO`）
  - `GET /api/auth/info` → `Result<SysUser>`

---

## 7. pom.xml 依赖

- [ ] **7.1** 在 `agent-qr-auth/pom.xml` 中配置依赖
  - `agent-qr-common`（模块依赖）
  - `agent-qr-user`（模块依赖 — 需要 SysUser/SysUserMapper）
  - `spring-boot-starter-security`
  - `jjwt-api` / `jjwt-impl` / `jjwt-jackson`（JWT 库）
