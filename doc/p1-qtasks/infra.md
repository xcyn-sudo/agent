# P1 前端 — infra 模块任务清单

> 模块：基础设施层
>
> 依赖：init
>
> 开发顺序：第 2 步（auth/chat/knowledge/user/dashboard 均依赖）

---

## 子任务

- [ ] **2.1 封装 Axios 实例** (`api/index.ts`)
  - 创建 Axios 实例，配置 `baseURL`、`timeout`
  - 请求拦截器：自动附加 `Authorization: Bearer <token>`
  - 响应拦截器：统一解包 `response.data`，处理 `code !== 200` 异常
  - 401 → 清除 Token → 跳转 `/login`
  - 403 → Toast 提示"权限不足"
  - 500 → Toast 提示"服务器内部错误"

- [ ] **2.2 创建所有 API 模块** (`api/*.ts`)
  - `api/auth.ts`：`login()`、`register()`、`getUserInfo()`
  - `api/user.ts`：`listUsers()`、`createUser()`、`updateUser()`、`toggleStatus()`
  - `api/knowledge.ts`：`upload()`、`listDocuments()`、`getDocument()`、`deleteDocument()`、`getStatus()`、`getChunks()`
  - `api/chat.ts`：`ask()`、`listConversations()`、`getMessages()`、`deleteConversation()`
  - `api/statistics.ts`：`getDashboard()`

- [ ] **2.3 定义全局 TypeScript 类型** (`types/index.ts`)
  - `ApiResult<T>`、`PageResult<T>`
  - `UserInfo`、`DocumentInfo`、`Conversation`、`Message`
  - `AskResponse`、`RetrievedDocument`
  - `DashboardVO`、`DailyStats`

- [ ] **2.4 配置路由** (`router/index.ts`)
  - 定义所有路由（login、register、chat、admin/users、admin/knowledge、admin/dashboard、403、404）
  - 路由 meta：`requiresAuth`、`requiresAdmin`
  - 全局前置守卫：Token 校验 + 角色权限判断
  - 未登录 → 重定向 `/login?redirect=xxx`
  - 已登录访问登录页 → 重定向 `/chat`
  - 非 admin 访问 `/admin/*` → 重定向 `/403`

- [ ] **2.5 创建布局组件** (`components/layout/`)
  - `GuestLayout.vue`：居中卡片布局（登录/注册页用）
  - `MainLayout.vue`：侧边栏 + 顶栏 + 内容区（主应用用）
  - `Sidebar.vue`：根据角色渲染菜单项（问答 / 知识库管理 / 用户管理 / 仪表盘）
  - `HeaderBar.vue`：Logo + 用户名显示 + 退出按钮

- [ ] **2.6 创建全局样式** (`styles/`)
  - `variables.scss`：主题色、边框色、字体大小
  - `global.scss`：重置样式、布局基础样式、滚动条样式

- [ ] **2.7 创建工具函数** (`utils/`)
  - `token.ts`：`getToken()`、`setToken()`、`removeToken()`（localStorage 存取）
  - `format.ts`：`formatFileSize()`、`formatDateTime()`、`truncateText()`

- [ ] **2.8 创建通用组件** (`components/common/`)
  - `Pagination.vue`：封装 Element Plus 分页组件，统一风格

---

## 验证标准

- [ ] 路由跳转守卫正常工作（登录/权限拦截）
- [ ] Axios 拦截器正常处理 401/403/500
- [ ] 布局组件渲染正常，侧边栏菜单根据角色变化

---

> 预计耗时：1.5 天
