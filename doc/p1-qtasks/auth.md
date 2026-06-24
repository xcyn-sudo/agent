# P1 前端 — auth 模块任务清单

> 模块：认证模块
>
> 依赖：infra
>
> 开发顺序：第 3 步（chat 模块依赖登录状态）

---

## 子任务

- [ ] **3.1 实现 Auth Store** (`stores/auth.ts`)
  - State：`token`、`user`
  - `login(username, password)`：调用 `authApi.login()` → 保存 token 到 localStorage + state → 保存 user 到 state
  - `register(data)`：调用 `authApi.register()` → 返回结果
  - `fetchUserInfo()`：调用 `authApi.getUserInfo()` → 更新 user state
  - `logout()`：清除 token + user → 跳转 `/login`
  - 页面刷新时从 localStorage 恢复 token（`persist: true` 或手动处理）

- [ ] **3.2 实现登录页** (`views/login/LoginView.vue`)
  - 用户名输入框（必填校验）
  - 密码输入框（必填校验，支持显示/隐藏切换）
  - 「登录」按钮（loading 状态，防重复提交）
  - 错误提示：toast 显示后端返回的错误消息
  - 成功后跳转（优先跳 `redirect` 参数，否则 `/chat`）
  - 「立即注册」链接跳转 `/register`

- [ ] **3.3 实现注册页** (`views/register/RegisterView.vue`)
  - 用户名输入框（必填）
  - 密码输入框（必填）
  - 真实姓名输入框（选填）
  - 邮箱输入框（选填，邮箱格式校验）
  - 手机号输入框（选填，手机号格式校验）
  - 「注册」按钮（loading 状态）
  - 成功后 toast 提示"注册成功" → 跳转 `/login`
  - 失败后 toast 显示后端错误消息（如"用户名已存在"）
  - 「立即登录」链接跳转 `/login`

- [ ] **3.4 实现 403 页面** (`views/error/403.vue`)
  - 显示"403 — 权限不足，无法访问该页面"
  - 「返回首页」按钮 → `/chat`

- [ ] **3.5 实现 404 页面** (`views/error/404.vue`)
  - 显示"404 — 页面不存在"
  - 「返回首页」按钮 → `/chat`

- [ ] **3.6 集成 GuestLayout**
  - LoginView 和 RegisterView 使用 GuestLayout
  - 更新路由配置使用布局组件

---

## 验证标准

- [ ] 登录成功 → Token 正确存储 → 跳转到 `/chat`
- [ ] 登录失败 → 显示错误消息
- [ ] 注册成功 → 提示成功 → 跳转到 `/login`
- [ ] 未登录访问 `/chat` → 自动跳转 `/login?redirect=/chat`
- [ ] 登录后访问 `/login` → 自动跳转 `/chat`
- [ ] 退出登录 → Token 清除 → 跳转 `/login`
- [ ] 页面刷新后登录状态保持

---

> 预计耗时：1 天
