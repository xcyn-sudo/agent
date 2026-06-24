# P1 阶段前端详细设计说明书

> 项目名称：基于 LangChain 的 RAG 企业内部知识库问答 Agent 系统
>
> 文档版本：v1.0
>
> 日期：2026-06-22
>
> 文档状态：已评审（基于后端 API 接口反推）

---

## 一、前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4+ | 前端框架（Composition API） |
| Vite | 5.x | 构建工具 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由管理 |
| Axios | 1.x | HTTP 请求 |
| Element Plus | 2.x | UI 组件库 |
| ECharts | 5.x | 图表（Dashboard） |
| TypeScript | 5.x | 类型安全 |

---

## 二、项目结构

```
agent-qr-web-frontend/
├── public/
│   └── favicon.ico
├── src/
│   ├── api/                        # API 请求层
│   │   ├── index.ts                # Axios 实例 + 拦截器
│   │   ├── auth.ts                 # 认证 API
│   │   ├── user.ts                 # 用户管理 API
│   │   ├── knowledge.ts            # 知识库 API
│   │   ├── chat.ts                 # 问答 API
│   │   └── statistics.ts           # 统计 API
│   ├── router/
│   │   └── index.ts                # 路由配置 + 导航守卫
│   ├── stores/
│   │   ├── auth.ts                 # 认证状态（用户信息、Token）
│   │   └── app.ts                  # 全局状态（侧边栏折叠等）
│   ├── views/
│   │   ├── login/
│   │   │   └── LoginView.vue       # 登录页
│   │   ├── register/
│   │   │   └── RegisterView.vue    # 注册页
│   │   ├── chat/
│   │   │   └── ChatView.vue        # 问答页（核心）
│   │   ├── knowledge/
│   │   │   └── KnowledgeView.vue   # 知识库管理页
│   │   ├── user/
│   │   │   └── UserManageView.vue  # 用户管理页
│   │   ├── dashboard/
│   │   │   └── DashboardView.vue   # 统计仪表盘页
│   │   └── error/
│   │       ├── 404.vue             # 404 页面
│   │       └── 403.vue             # 403 页面
│   ├── components/
│   │   ├── layout/
│   │   │   ├── MainLayout.vue      # 主布局（侧边栏 + 顶栏 + 内容区）
│   │   │   ├── GuestLayout.vue     # 访客布局（登录/注册）
│   │   │   ├── Sidebar.vue         # 侧边导航
│   │   │   └── HeaderBar.vue       # 顶栏（用户信息、退出）
│   │   ├── chat/
│   │   │   ├── ConversationList.vue # 会话列表
│   │   │   ├── MessageBubble.vue    # 消息气泡
│   │   │   └── ChatInput.vue        # 输入框组件
│   │   ├── knowledge/
│   │   │   ├── DocumentTable.vue    # 文档列表表格
│   │   │   ├── UploadDialog.vue     # 上传文档弹窗
│   │   │   └── StatusTag.vue        # 文档状态标签
│   │   ├── user/
│   │   │   ├── UserTable.vue        # 用户列表表格
│   │   │   └── UserFormDialog.vue   # 新增/编辑用户弹窗
│   │   └── common/
│   │       └── Pagination.vue       # 分页组件（封装）
│   ├── utils/
│   │   ├── token.ts                 # Token 存取工具
│   │   └── format.ts               # 日期/文件大小格式化
│   ├── types/
│   │   └── index.ts                 # 全局 TypeScript 类型定义
│   ├── styles/
│   │   ├── global.scss              # 全局样式
│   │   └── variables.scss           # SCSS 变量
│   ├── App.vue
│   └── main.ts
├── index.html
├── vite.config.ts
├── tsconfig.json
├── package.json
└── .env.development                 # 开发环境变量（API_BASE_URL）
```

---

## 三、路由设计

### 3.1 路由表

| 路径 | 视图 | 权限 | 说明 |
|------|------|------|------|
| `/login` | LoginView | 公开 | 登录页 |
| `/register` | RegisterView | 公开 | 注册页 |
| `/chat` | ChatView | 登录用户 | 问答主页 |
| `/admin/users` | UserManageView | ADMIN | 用户管理 |
| `/admin/knowledge` | KnowledgeView | ADMIN | 知识库管理 |
| `/admin/dashboard` | DashboardView | ADMIN | 统计仪表盘 |
| `/403` | 403.vue | 公开 | 无权限提示 |
| `/:pathMatch(.*)*` | 404.vue | 公开 | 404 页面 |

### 3.2 路由守卫逻辑

```
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  const hasToken = authStore.token

  if (to.path === '/login' || to.path === '/register') {
    // 已登录用户访问登录/注册页 → 重定向到 /chat
    hasToken ? next('/chat') : next()
  } else {
    if (!hasToken) {
      // 未登录 → 重定向到 /login，携带 redirect 参数
      next(`/login?redirect=${to.path}`)
    } else if (to.meta.requiresAdmin && authStore.user?.role !== 'admin') {
      // 需要管理员权限但用户不是 admin → 403
      next('/403')
    } else {
      next()
    }
  }
})
```

---

## 四、状态管理设计

### 4.1 Auth Store (`stores/auth.ts`)

```typescript
interface AuthState {
  token: string | null
  user: {
    id: number
    username: string
    realName: string
    role: string
    email: string
    phone: string
  } | null
}

// Actions:
// - login(username, password): 调用 POST /api/auth/login → 保存 token + user
// - register(data): 调用 POST /api/auth/register
// - fetchUserInfo(): 调用 GET /api/auth/info → 刷新 user
// - logout(): 清除 token + user → 跳转 /login
```

### 4.2 App Store (`stores/app.ts`)

```typescript
interface AppState {
  sidebarCollapsed: boolean   // 侧边栏折叠状态
}

// Actions:
// - toggleSidebar()
```

---

## 五、API 层设计

### 5.1 Axios 实例配置 (`api/index.ts`)

```typescript
// 请求拦截器：
//   自动添加 Authorization: Bearer <token>
//
// 响应拦截器：
//   统一提取 response.data
//   code !== 200 → 提示 message
//   401 → 清除 Token → 跳转 /login
//   403 → 提示"权限不足"
//   500 → 提示"服务器错误"
```

### 5.2 API 接口映射

| 后端接口 | 前端函数 | 说明 |
|----------|----------|------|
| `POST /api/auth/login` | `authApi.login(data)` | 登录 |
| `POST /api/auth/register` | `authApi.register(data)` | 注册 |
| `GET /api/auth/info` | `authApi.getUserInfo()` | 获取当前用户 |
| `GET /api/admin/users` | `userApi.listUsers(params)` | 用户列表 |
| `POST /api/admin/users` | `userApi.createUser(data)` | 新增用户 |
| `PUT /api/admin/users/{id}` | `userApi.updateUser(id, data)` | 编辑用户 |
| `PUT /api/admin/users/{id}/status` | `userApi.toggleStatus(id, status)` | 启/禁用户 |
| `POST /api/knowledge/upload` | `knowledgeApi.upload(file, title)` | 上传文档 |
| `GET /api/knowledge/documents` | `knowledgeApi.listDocuments(params)` | 文档列表 |
| `GET /api/knowledge/documents/{id}` | `knowledgeApi.getDocument(id)` | 文档详情 |
| `DELETE /api/knowledge/documents/{id}` | `knowledgeApi.deleteDocument(id)` | 删除文档 |
| `GET /api/knowledge/documents/{id}/status` | `knowledgeApi.getStatus(id)` | 文档状态 |
| `GET /api/knowledge/documents/{id}/chunks` | `knowledgeApi.getChunks(id)` | 文档切片 |
| `POST /api/chat/ask` | `chatApi.ask(query, conversationId)` | 提问 |
| `GET /api/chat/conversations` | `chatApi.listConversations()` | 会话列表 |
| `GET /api/chat/conversations/{id}/messages` | `chatApi.getMessages(id)` | 消息历史 |
| `DELETE /api/chat/conversations/{id}` | `chatApi.deleteConversation(id)` | 删除会话 |
| `GET /api/statistics/dashboard` | `statisticsApi.getDashboard()` | 仪表盘数据 |

---

## 六、页面详细设计

### 6.1 登录页 (`LoginView.vue`)

```
┌──────────────────────────────────────────┐
│                                          │
│        Agent-QR 企业知识库问答系统          │
│                                          │
│     ┌────────────────────────────┐       │
│     │  用户名                     │       │
│     │  [____________________]    │       │
│     │                            │       │
│     │  密码                       │       │
│     │  [____________________]    │       │
│     │                            │       │
│     │  [      登 录      ]       │       │
│     │                            │       │
│     │  还没有账号？立即注册 →       │       │
│     └────────────────────────────┘       │
│                                          │
└──────────────────────────────────────────┘
```

**交互说明**：
- 表单校验：用户名和密码必填
- 成功后跳转到 `/chat`（或 redirect 参数指定的路径）
- 失败显示错误消息（后端统一返回"用户名或密码错误"）
- 「立即注册」链接跳转到 `/register`

### 6.2 注册页 (`RegisterView.vue`)

```
┌──────────────────────────────────────────┐
│                                          │
│            创建新账号                      │
│                                          │
│     ┌────────────────────────────┐       │
│     │  用户名 *                   │       │
│     │  [____________________]    │       │
│     │  密码 *                     │       │
│     │  [____________________]    │       │
│     │  真实姓名                   │       │
│     │  [____________________]    │       │
│     │  邮箱                       │       │
│     │  [____________________]    │       │
│     │  手机号                     │       │
│     │  [____________________]    │       │
│     │                            │       │
│     │  [      注 册      ]       │       │
│     │                            │       │
│     │  已有账号？立即登录 →       │       │
│     └────────────────────────────┘       │
└──────────────────────────────────────────┘
```

**交互说明**：
- 用户名、密码必填
- 注册成功 → 提示"注册成功，请登录" → 跳转 `/login`
- 用户名已存在 → 提示错误消息

### 6.3 问答页 (`ChatView.vue`)

```
┌──────────────────────────────────────────────────────────────┐
│  [≡]  Agent-QR 企业知识库         [通知] 用户名 ▼ [退出]      │
├────────────────┬─────────────────────────────────────────────┤
│  ┌──────────┐ │                                             │
│  │ + 新会话  │ │  ┌─────────────────────────────────────┐   │
│  ├──────────┤ │  │                                     │   │
│  │ 会话 1    │ │  │  🤖 您好！我是企业知识库助手，       │   │
│  │ 会话 2    │ │  │     请问有什么可以帮助您的？         │   │
│  │ 会话 3    │ │  │                                     │   │
│  │           │ │  │  👤 研发部2024年的绩效考核标准？     │   │
│  │           │ │  │                                     │   │
│  │           │ │  │  🤖 根据知识库中《研发部绩效考核      │   │
│  │           │ │  │     制度v2024》文档...               │   │
│  │           │ │  │     引用来源：                       │   │
│  │           │ │  │     📎 研发部绩效考核制度v2024.pdf   │   │
│  │           │ │  │                                     │   │
│  └──────────┘ │  └─────────────────────────────────────┘   │
│               │  ┌─────────────────────────────────────┐   │
│               │  │ [请输入您的问题...]          [发送]  │   │
│               │  └─────────────────────────────────────┘   │
└──────────────┴─────────────────────────────────────────────┘
```

**交互说明**：
- **左侧会话列表**：
  - 「+ 新会话」按钮创建新对话，自动以第一个问题前30字为标题
  - 点击会话 → 加载该会话的历史消息
  - 悬停显示删除按钮
- **右侧聊天区**：
  - 默认显示欢迎语
  - 用户消息：右对齐，蓝色气泡
  - AI 回答：左对齐，白色气泡，底部显示引用来源
  - P1 版本为同步问答：发送后加载状态 → 一次性返回完整回答
- **底部输入区**：
  - Enter 发送（Shift+Enter 换行）
  - 空内容不允许发送

### 6.4 知识库管理页 (`KnowledgeView.vue`)

```
┌──────────────────────────────────────────────────────────────┐
│  [≡]  Agent-QR 企业知识库         [通知] 用户名 ▼ [退出]      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  知识库管理                                                   │
│                                                              │
│  [ + 上传文档 ]                                              │
│                                                              │
│  ┌──────────────────────────────────────────────────────────┐│
│  │ 文件名        │ 类型  │ 大小   │ 状态   │ 上传时间 │ 操作 ││
│  ├──────────────────────────────────────────────────────────┤│
│  │ 绩效考核.pdf   │ pdf   │ 2.3MB │ ✅就绪 │ 06-22   │ 🗑   ││
│  │ 员工手册.docx  │ docx  │ 1.1MB │ 🔄解析 │ 06-21   │ 🗑   ││
│  │ 安全规范.pdf   │ pdf   │ 5.0MB │ ❌失败 │ 06-20   │ 🗑   ││
│  └──────────────────────────────────────────────────────────┘│
│                                                              │
│  [ < ]  [ 1 ]  [ 2 ]  [ 3 ]  [ > ]   共 25 条               │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**交互说明**：
- **上传**：
  - 点击「+ 上传文档」→ 弹出 UploadDialog
  - 支持拖拽或点击选择文件
  - 文件类型限制：pdf / docx / txt / md
  - 大小限制：≤ 50MB
  - 上传成功后刷新列表
- **列表**：
  - 状态使用 StatusTag 组件，不同状态不同颜色：
    - 就绪(READY) → 绿色
    - 解析中/切片中/向量化中 → 蓝色加载动画
    - 失败(FAILED) → 红色
  - 删除需二次确认
- **分页**：支持翻页

### 6.5 用户管理页 (`UserManageView.vue`)

```
┌──────────────────────────────────────────────────────────────┐
│  [≡]  Agent-QR 企业知识库         [通知] 用户名 ▼ [退出]      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  用户管理                                                     │
│                                                              │
│  [ + 新增用户 ]          [ 🔍 搜索用户名/姓名... ]            │
│                                                              │
│  ┌──────────────────────────────────────────────────────────┐│
│  │ ID │ 用户名  │ 姓名  │ 角色   │ 状态 │ 创建时间 │ 操作   ││
│  ├──────────────────────────────────────────────────────────┤│
│  │ 1  │ admin   │ 管理员│ admin  │ ✅启用│ 06-01   │ ✏️ 🔒 ││
│  │ 2  │ zhangsan│ 张三  │ user   │ ✅启用│ 06-10   │ ✏️ 🔒 ││
│  │ 3  │ lisi    │ 李四  │ user   │ ❌禁用│ 06-15   │ ✏️ 🔓 ││
│  └──────────────────────────────────────────────────────────┘│
│                                                              │
│  [ < ]  [ 1 ]  [ 2 ]  [ 3 ]  [ > ]   共 30 条               │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**交互说明**：
- **搜索**：输入关键词按回车搜索（匹配用户名/真实姓名）
- **新增**：弹出 UserFormDialog，填写用户名、密码、姓名、邮箱、手机号
- **编辑**：弹出 UserFormDialog（不含密码字段），修改姓名、邮箱、手机号、角色
- **启用/禁用**：点击锁图标切换状态，需二次确认

### 6.6 统计仪表盘页 (`DashboardView.vue`)

```
┌──────────────────────────────────────────────────────────────┐
│  [≡]  Agent-QR 企业知识库         [通知] 用户名 ▼ [退出]      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  数据仪表盘                                                   │
│                                                              │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐            │
│  │ 今日问答 │ │ 今日新增 │ │ 文档总数 │ │ 用户总数 │            │
│  │   12    │ │   3     │ │   45    │ │   30    │            │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘            │
│                                                              │
│  ┌──────────────────────────┐ ┌────────────────────────┐     │
│  │     近7天问答趋势          │ │     文档类型分布         │     │
│  │     📈 折线图              │ │     🥧 饼图             │     │
│  │                          │ │                        │     │
│  └──────────────────────────┘ └────────────────────────┘     │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**交互说明**：
- 页面加载时请求 `GET /api/statistics/dashboard`
- 顶部 4 个统计卡片（数字 + 标签）
- 左下：近 7 天问答趋势折线图（ECharts）
- 右下：文档类型分布饼图（ECharts）

---

## 七、布局设计

### 7.1 访客布局 (`GuestLayout.vue`)

```
┌──────────────────────────────────────────┐
│                                          │
│            <router-view />               │
│         (登录/注册表单居中)               │
│                                          │
└──────────────────────────────────────────┘
```

简单的居中布局，用于 `/login` 和 `/register`。

### 7.2 主布局 (`MainLayout.vue`)

```
┌──────────────────────────────────────────────┐
│  HeaderBar (Logo + 用户名 + 退出)             │
├────────┬─────────────────────────────────────┤
│ Sidebar│                                     │
│        │        <router-view />              │
│ · 问答 │                                     │
│ · 知识库│                                     │
│ · 用户 │                                     │
│ · 仪表盘│                                     │
│        │                                     │
└────────┴─────────────────────────────────────┘
```

- **顶栏**：左侧 Logo + 系统名，右侧用户名 + 退出按钮
- **侧边栏**：根据角色显示菜单项
  - 所有用户：问答（/chat）
  - ADMIN 用户额外：知识库管理、用户管理、数据仪表盘
- **内容区**：`<router-view />`

---

## 八、组件树

```
App.vue
├── GuestLayout.vue                    [路由: /login, /register]
│   ├── LoginView.vue
│   └── RegisterView.vue
│
└── MainLayout.vue                     [路由: /chat, /admin/*]
    ├── Sidebar.vue
    ├── HeaderBar.vue
    └── <router-view>
        ├── ChatView.vue               [路由: /chat]
        │   ├── ConversationList.vue
        │   ├── MessageBubble.vue (×N)
        │   └── ChatInput.vue
        │
        ├── KnowledgeView.vue          [路由: /admin/knowledge]
        │   ├── UploadDialog.vue
        │   ├── DocumentTable.vue
        │   │   └── StatusTag.vue (×N)
        │   └── Pagination.vue
        │
        ├── UserManageView.vue         [路由: /admin/users]
        │   ├── UserFormDialog.vue
        │   ├── UserTable.vue
        │   └── Pagination.vue
        │
        └── DashboardView.vue          [路由: /admin/dashboard]
            └── ECharts (×2)
```

---

## 九、类型定义

```typescript
// types/index.ts

// API 通用响应
interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 分页
interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 用户
interface UserInfo {
  id: number
  username: string
  realName: string
  email: string
  phone: string
  role: string
  status: number
  createTime: string
  updateTime: string
}

// 文档
interface Document {
  id: number
  title: string
  fileName: string
  filePath: string
  fileType: string
  fileSize: number
  status: string
  uploadUserId: number
  errorMsg: string
  createTime: string
  updateTime: string
}

// 会话
interface Conversation {
  id: number
  userId: number
  title: string
  messageCount: number
  createTime: string
  updateTime: string
}

// 消息
interface Message {
  id: number
  conversationId: number
  role: 'user' | 'assistant'
  content: string
  sources: string
  createTime: string
}

// 问答响应
interface AskResponse {
  answer: string
  conversationId: number
  sources: RetrievedDocument[]
}

// 检索来源
interface RetrievedDocument {
  documentId: string
  documentTitle: string
  content: string
  similarity: number
}

// 仪表盘
interface DashboardVO {
  todayQA: number
  todayNewUsers: number
  totalDocuments: number
  totalChunks: number
  totalUsers: number
  weeklyTrend: DailyStats[]
  docTypeDistribution: Record<string, number>
}

interface DailyStats {
  id: number
  statDate: string
  qaCount: number
  userQuestionCount: number
  activeUserCount: number
  docUploadCount: number
}
```

---

## 十、P1 阶段约束与简化

| 功能 | P1 处理 | P2/P3 增强 |
|------|---------|-----------|
| 问答模式 | 同步问答（加载动画 → 一次性返回） | SSE 流式逐字渲染 |
| 路由方式 | 简单角色判断（user/admin） | ABAC 细粒度权限前端联动 |
| API 文档 | 手动维护 api/ 目录 | 自动生成 TypeScript 类型 |
| Token 管理 | 单一 Access Token（24h），过期跳登录 | 双 Token + 静默刷新 |
| 错误处理 | 统一 toast 提示 | 错误码分类 + 重试机制 |
| 国际化 | 硬编码中文 | i18n 多语言 |
| 响应式 | 桌面端优先 | 移动端适配 |
| ECharts | 基础折线图 + 饼图 | 可交互钻取图表 |

---

> **文档版本**：v1.0
>
> **编写日期**：2026-06-22
>
> **依据**：系统详细设计说明书 v1.0、p1-prompt.md、后端 API 接口反推
