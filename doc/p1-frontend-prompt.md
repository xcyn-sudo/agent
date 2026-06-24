# P1 阶段前端代码生成 Prompt

> 项目名称：基于 LangChain 的 RAG 企业内部知识库问答 Agent 系统 — 前端
>
> Prompt 版本：v1.0
>
> 日期：2026-06-22
>
> 目标项目目录：`agent-qr-web-frontend/`

---

## 一、角色定义

### 主Agent（你）

你是 P1 阶段前端工程的**总指挥 Agent**，负责：

1. **进度跟踪**：维护 7 个模块的开发进度，每个模块按子任务逐项推进
2. **子Agent 调度**：按照开发顺序依次（或按依赖关系并行）生成子Agent，下发模块实现任务
3. **质量把控**：每个模块完成后，确认子Agent 的输出满足验证标准
4. **问题上报**：遇到无法自动解决的冲突或设计矛盾时，暂停并向用户提问
5. **状态记录**：每完成一个模块，更新 `doc/p1-qtasks/p1-qprogress.md` 中的进度状态

### 子Agent

每个子Agent 负责**一个模块的完整实现**，包括：

1. 阅读本 Prompt 中该模块的详细规格
2. 生成/修改对应的源代码文件到 `agent-qr-web-frontend/` 目录
3. 确保代码符合技术栈约定和代码风格
4. 完成后向主Agent 汇报结果

---

## 二、项目基线状态

### 2.1 已有内容

`agent-qr-web-frontend/` 已通过 `npm create vite@latest` 完成初始化，当前状态：

| 文件/目录 | 状态 | 说明 |
|-----------|------|------|
| `package.json` | 已有 | 含 vue3、pinia、vue-router、vite、typescript、vitest、playwright、eslint、prettier |
| `vite.config.ts` | 已有 | `@` 别名已配置（→ `src/`），含 vue、vueJsx、vueDevTools 插件 |
| `tsconfig.json` | 已有 | 项目引用模式（tsconfig.node.json / tsconfig.app.json / tsconfig.vitest.json） |
| `src/main.ts` | 已有 | 已注册 Pinia + Router，需更新 |
| `src/App.vue` | 模板 | 占位内容，需替换为 `<router-view />` |
| `src/router/index.ts` | 骨架 | 空路由表，需完整填充 |
| `src/stores/counter.ts` | 示例 | 需删除，替换为 auth.ts + app.ts |
| `index.html` | 已有 | 需更新标题为"Agent-QR 企业知识库问答系统" |
| 测试/E2E/Lint | 已有 | vitest + playwright + eslint + oxlint + prettier 已配置 |

### 2.2 需要新增的依赖

```bash
npm install element-plus axios echarts vue-echarts
npm install -D sass unplugin-auto-import unplugin-vue-components
```

### 2.3 需要创建的目录结构

```
src/
├── api/           # 新建
├── components/    # 已存在，需新建子目录
│   ├── layout/
│   ├── chat/
│   ├── knowledge/
│   ├── user/
│   └── common/
├── styles/        # 新建
├── types/         # 新建
├── utils/         # 新建
└── views/         # 已存在，需新建子目录
    ├── login/
    ├── register/
    ├── chat/
    ├── knowledge/
    ├── user/
    ├── dashboard/
    └── error/
```

---

## 三、技术栈与约定

### 3.1 技术版本

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5+ | Composition API + `<script setup lang="ts">` |
| Vite | 8.x | 构建工具 |
| Pinia | 3.x | 状态管理 |
| Vue Router | 5.x | 路由管理 |
| Axios | 1.x | HTTP 请求 |
| Element Plus | 2.x | UI 组件库 |
| ECharts | 5.x | 图表（通过 vue-echarts 封装） |
| TypeScript | ~6.0 | 类型安全 |
| SCSS/Sass | 最新 | 样式预处理 |

### 3.2 代码约定

- **所有 `.vue` 文件**使用 `<script setup lang="ts">` 语法
- **所有 `.ts` 文件**使用 ES Module 导出
- **路径别名**：使用 `@/` 代替 `src/`
- **组件命名**：PascalCase 文件名（如 `MessageBubble.vue`）
- **类型定义**：集中在 `src/types/index.ts`
- **API 函数**：按业务模块分文件（`api/auth.ts`、`api/chat.ts` 等）
- **样式**：优先使用 Element Plus 组件样式，自定义样式用 SCSS，写在 `<style scoped lang="scss">` 中
- **全局样式变量**：定义在 `src/styles/variables.scss`

### 3.3 设计文档参考

- **前端详细设计**：`doc/p1-frontend-design.md`（页面布局、组件树、交互说明、类型定义）
- **系统详细设计**：`doc/系统详细设计说明书.md`（后端 API 接口、数据模型）
- **各模块任务清单**：`doc/p1-qtasks/*.md`

---

## 四、模块开发顺序与依赖

```
第 1 步：init       → 项目脚手架补充（安装依赖 + 更新配置）
第 2 步：infra      → 基础设施层（API 客户端 + 路由 + 布局 + 类型 + 工具）
第 3 步：auth       → 认证模块（登录/注册 + 权限守卫 + Store）
第 4 步：chat       → 问答模块（核心功能，依赖 auth）
第 5 步：knowledge  → 知识库管理（管理员，依赖 infra）
第 6 步：user       → 用户管理（管理员，依赖 infra）
第 7 步：dashboard  → 数据仪表盘（管理员，依赖 infra）
```

依赖关系图：

```
        ┌─────────┐
        │  init    │
        └────┬────┘
             │
        ┌────┴────┐
        │  infra   │
        └────┬────┘
             │
    ┌────────┼────────┬────────┬────────┐
    │        │        │        │        │
┌───┴──┐ ┌───┴──┐ ┌───┴───┐ ┌───┴───┐ ┌───┴──────┐
│ auth │ │ chat │ │knowl- │ │ user  │ │dashboard │
│      │ │      │ │edge   │ │       │ │          │
└──────┘ └──┬───┘ └───────┘ └───────┘ └──────────┘
            │
      chat 依赖 auth（需要登录状态）
      其余模块均依赖 infra
```

- **init 和 infra 必须串行**（infra 依赖 init 的配置）
- **auth / knowledge / user / dashboard 可并行开发**（均仅依赖 infra）
- **chat 必须在 auth 之后**（问答页需要登录状态）

---

## 五、各模块详细规格

---

### 模块 1：init — 项目初始化补充

**目标**：在已有脚手架基础上，补全 P1 阶段所需的依赖和配置。

**子Agent 输入**：本节的子任务清单 + 当前项目文件状态

#### 子任务

##### 1.1 安装新增依赖

执行以下命令安装 P1 所需依赖：

```bash
cd agent-qr-web-frontend
npm install element-plus axios echarts vue-echarts
npm install -D sass unplugin-auto-import unplugin-vue-components
```

##### 1.2 更新 `vite.config.ts`

在现有配置基础上添加：

- Element Plus 自动导入插件（`unplugin-auto-import` + `unplugin-vue-components`）
- 开发服务器代理：`/api` → `http://localhost:9090`
- SCSS 全局变量注入

参考配置（合并到现有配置中，不要覆盖已有的 plugins）：

```typescript
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// plugins 中追加:
AutoImport({ resolvers: [ElementPlusResolver()] }),
Components({ resolvers: [ElementPlusResolver()] }),

// server 配置:
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:9090',
      changeOrigin: true,
    },
  },
},

// css 配置:
css: {
  preprocessorOptions: {
    scss: {
      additionalData: `@use "@/styles/variables.scss" as *;`,
    },
  },
},
```

##### 1.3 创建环境变量文件

创建 `.env.development`：

```
VITE_API_BASE_URL=http://localhost:9090
```

创建 `.env.production`：

```
VITE_API_BASE_URL=/api
```

##### 1.4 更新 `index.html`

- 标题改为：`Agent-QR 企业知识库问答系统`
- 语言设为：`zh-CN`

##### 1.5 创建空目录结构

确保以下目录存在（用 `.gitkeep` 占位）：

```
src/api/
src/components/layout/
src/components/chat/
src/components/knowledge/
src/components/user/
src/components/common/
src/styles/
src/types/
src/utils/
src/views/login/
src/views/register/
src/views/chat/
src/views/knowledge/
src/views/user/
src/views/dashboard/
src/views/error/
```

##### 1.6 删除示例文件

- 删除 `src/stores/counter.ts`
- 删除 `src/__tests__/App.spec.ts`（后续按模块重新编写）
- 删除 `e2e/vue.spec.ts`

##### 1.7 更新 `src/App.vue`

替换为最简单的 `<router-view />`：

```vue
<script setup lang="ts"></script>

<template>
  <router-view />
</template>
```

#### 验证标准

- [ ] `npm run dev` 正常启动，控制台无报错
- [ ] 浏览器访问 `http://localhost:5173` 显示空白页面（无报错）
- [ ] `npm run build` 构建成功

---

### 模块 2：infra — 基础设施层

**目标**：搭建 API 通信层、路由体系、布局框架、类型定义、工具函数、全局样式。

**子Agent 输入**：本节的子任务清单 + `doc/p1-frontend-design.md` 第二/三/四/五/七/九章

#### 子任务

##### 2.1 创建全局类型定义 (`src/types/index.ts`)

完整复制 `doc/p1-frontend-design.md` 第九章全部类型定义：

- `ApiResult<T>` — 统一响应
- `PageResult<T>` — 分页结果
- `UserInfo` — 用户信息
- `DocumentInfo`（设计文档中名为 `Document`，为避免与 DOM API 冲突命名为 `DocumentInfo`）— 文档信息
- `Conversation` — 会话
- `Message` — 消息
- `AskResponse` — 问答响应
- `RetrievedDocument` — 检索来源
- `DashboardVO` — 仪表盘数据
- `DailyStats` — 每日统计

##### 2.2 封装 Axios 实例 (`src/api/index.ts`)

按照 `doc/p1-frontend-design.md` 5.1 节实现：

- `baseURL`：从环境变量读取
- `timeout`：15000ms
- **请求拦截器**：从 localStorage 读取 token，添加到 `Authorization: Bearer <token>` 头
- **响应拦截器**：
  - `code === 200` → 返回 `response.data`
  - `code === 401` → 清除 token → 跳转 `/login`
  - `code === 403` → ElMessage.error("权限不足")
  - `code === 500` → ElMessage.error("服务器内部错误")
  - 其他错误码 → ElMessage.error(response.data.message)
  - 网络异常 → ElMessage.error("网络连接失败")

##### 2.3 创建所有 API 模块

按照 `doc/p1-frontend-design.md` 5.2 节 API 接口映射表实现：

**`src/api/auth.ts`**：
```typescript
import request from './index'
import type { ApiResult, UserInfo } from '@/types'

export const authApi = {
  login(data: { username: string; password: string }) {
    return request.post<any, ApiResult<{ token: string; userId: number; username: string; role: string }>>('/api/auth/login', data)
  },
  register(data: { username: string; password: string; realName?: string; email?: string; phone?: string }) {
    return request.post<any, ApiResult<void>>('/api/auth/register', data)
  },
  getUserInfo() {
    return request.get<any, ApiResult<UserInfo>>('/api/auth/info')
  },
}
```

**`src/api/user.ts`**：
```typescript
import request from './index'
import type { ApiResult, PageResult, UserInfo } from '@/types'

export const userApi = {
  listUsers(params: { page: number; size: number; keyword?: string }) {
    return request.get<any, ApiResult<PageResult<UserInfo>>>('/api/admin/users', { params })
  },
  createUser(data: { username: string; password: string; realName?: string; email?: string; phone?: string }) {
    return request.post<any, ApiResult<void>>('/api/admin/users', data)
  },
  updateUser(id: number, data: { realName?: string; email?: string; phone?: string; role?: string }) {
    return request.put<any, ApiResult<void>>(`/api/admin/users/${id}`, data)
  },
  toggleStatus(id: number, status: number) {
    return request.put<any, ApiResult<void>>(`/api/admin/users/${id}/status`, { status })
  },
}
```

**`src/api/knowledge.ts`**：
```typescript
import request from './index'
import type { ApiResult, PageResult, DocumentInfo } from '@/types'

export const knowledgeApi = {
  upload(file: File, title?: string) {
    const formData = new FormData()
    formData.append('file', file)
    if (title) formData.append('title', title)
    return request.post<any, ApiResult<DocumentInfo>>('/api/knowledge/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  listDocuments(params: { page: number; size: number }) {
    return request.get<any, ApiResult<PageResult<DocumentInfo>>>('/api/knowledge/documents', { params })
  },
  getDocument(id: number) {
    return request.get<any, ApiResult<DocumentInfo>>(`/api/knowledge/documents/${id}`)
  },
  deleteDocument(id: number) {
    return request.delete<any, ApiResult<void>>(`/api/knowledge/documents/${id}`)
  },
  getStatus(id: number) {
    return request.get<any, ApiResult<{ status: string; errorMsg?: string }>>(`/api/knowledge/documents/${id}/status`)
  },
  getChunks(id: number) {
    return request.get<any, ApiResult<any[]>>(`/api/knowledge/documents/${id}/chunks`)
  },
}
```

**`src/api/chat.ts`**：
```typescript
import request from './index'
import type { ApiResult, PageResult, Conversation, Message, AskResponse } from '@/types'

export const chatApi = {
  ask(query: string, conversationId?: number) {
    return request.post<any, ApiResult<AskResponse>>('/api/chat/ask', { query, conversationId })
  },
  listConversations() {
    return request.get<any, ApiResult<Conversation[]>>('/api/chat/conversations')
  },
  getMessages(conversationId: number) {
    return request.get<any, ApiResult<Message[]>>(`/api/chat/conversations/${conversationId}/messages`)
  },
  deleteConversation(id: number) {
    return request.delete<any, ApiResult<void>>(`/api/chat/conversations/${id}`)
  },
}
```

**`src/api/statistics.ts`**：
```typescript
import request from './index'
import type { ApiResult, DashboardVO } from '@/types'

export const statisticsApi = {
  getDashboard() {
    return request.get<any, ApiResult<DashboardVO>>('/api/statistics/dashboard')
  },
}
```

##### 2.4 创建工具函数 (`src/utils/`)

**`src/utils/token.ts`**：
```typescript
const TOKEN_KEY = 'auth_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}
```

**`src/utils/format.ts`**：
```typescript
/**
 * 格式化文件大小
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + units[i]
}

/**
 * 格式化日期时间
 */
export function formatDateTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}`
}

/**
 * 截断文本
 */
export function truncateText(text: string, maxLength: number): string {
  if (!text) return ''
  return text.length > maxLength ? text.slice(0, maxLength) + '...' : text
}
```

##### 2.5 配置路由 (`src/router/index.ts`)

完整替换现有空路由，实现 `doc/p1-frontend-design.md` 第三章：

路由表：

| 路径 | 视图组件 | meta | 说明 |
|------|----------|------|------|
| `/login` | `@/views/login/LoginView.vue` | `{ guest: true }` | 登录页 |
| `/register` | `@/views/register/RegisterView.vue` | `{ guest: true }` | 注册页 |
| `/chat` | `@/views/chat/ChatView.vue` | `{ requiresAuth: true }` | 问答主页 |
| `/admin/users` | `@/views/user/UserManageView.vue` | `{ requiresAuth: true, requiresAdmin: true }` | 用户管理 |
| `/admin/knowledge` | `@/views/knowledge/KnowledgeView.vue` | `{ requiresAuth: true, requiresAdmin: true }` | 知识库管理 |
| `/admin/dashboard` | `@/views/dashboard/DashboardView.vue` | `{ requiresAuth: true, requiresAdmin: true }` | 统计仪表盘 |
| `/403` | `@/views/error/403.vue` | `{}` | 无权限 |
| `/:pathMatch(.*)*` | `@/views/error/404.vue` | `{}` | 404 |

**路由守卫逻辑**（按设计文档 3.2 节实现）：

```
router.beforeEach((to, from, next) => {
  const token = getToken()
  
  if (to.path === '/login' || to.path === '/register') {
    // 已登录用户访问登录/注册页 → 重定向到 /chat
    token ? next('/chat') : next()
  } else {
    if (!token) {
      // 未登录 → 重定向到 /login，携带 redirect 参数
      next(`/login?redirect=${to.path}`)
    } else if (to.meta.requiresAdmin) {
      // 需要管理员权限 → 从 store 或 localStorage 中检查角色
      const userRole = getUserRoleFromLocalStorage()
      if (userRole !== 'admin') {
        next('/403')
      } else {
        next()
      }
    } else {
      next()
    }
  }
})
```

> **重要**：由于此时 auth store 还未创建，路由守卫中先使用 `localStorage` 直接读取 token 和角色来判断。后续 auth 模块完成后，子Agent 需回来更新路由守卫，改为使用 auth store。

##### 2.6 创建全局样式 (`src/styles/`)

**`src/styles/variables.scss`**：
```scss
// 主题色
$primary-color: #409eff;
$success-color: #67c23a;
$warning-color: #e6a23c;
$danger-color: #f56c6c;
$info-color: #909399;

// 文本色
$text-primary: #303133;
$text-regular: #606266;
$text-secondary: #909399;
$text-placeholder: #c0c4cc;

// 边框色
$border-color: #dcdfe6;
$border-color-light: #e4e7ed;

// 背景色
$bg-color: #f5f7fa;
$bg-color-light: #fafafa;

// 侧边栏
$sidebar-width: 220px;
$sidebar-collapsed-width: 64px;
$sidebar-bg: #304156;

// 顶栏
$header-height: 60px;

// 字体
$font-size-base: 14px;
$font-size-small: 12px;
$font-size-large: 16px;
```

**`src/styles/global.scss`**：
```scss
// 全局重置样式
*,
*::before,
*::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html, body, #app {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
  font-size: $font-size-base;
  color: $text-primary;
  background-color: $bg-color;
}

// 滚动条样式
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}
::-webkit-scrollbar-track {
  background: transparent;
}

// 链接
a {
  color: $primary-color;
  text-decoration: none;
  &:hover {
    color: lighten($primary-color, 10%);
  }
}
```

##### 2.7 创建布局组件 (`src/components/layout/`)

**`GuestLayout.vue`**：
- 简单居中布局，用于登录/注册页
- 中间放置 `<router-view />`
- 背景色为 `$bg-color`，最小高度 100vh

**`MainLayout.vue`**：
- 左侧 Sidebar + 右侧（HeaderBar + 内容区）
- 使用 CSS flex 布局
- 内容区使用 `<router-view />`
- 结构：
```
┌──────────────────────────────────────┐
│            HeaderBar                  │
├────────┬─────────────────────────────┤
│ Sidebar│   <router-view />           │
│        │                             │
└────────┴─────────────────────────────┘
```

**`Sidebar.vue`**：
- 使用 `el-menu` 组件（Element Plus），垂直模式
- 菜单项根据用户角色动态显示：
  - **所有登录用户**：问答（图标：ChatDotRound，路径：/chat）
  - **ADMIN 用户额外**：
    - 知识库管理（图标：Document，路径：/admin/knowledge）
    - 用户管理（图标：User，路径：/admin/users）
    - 数据仪表盘（图标：DataAnalysis，路径：/admin/dashboard）
- 使用 `router` 属性实现路由跳转
- 根据当前路由高亮对应菜单项
- 支持折叠（由 App Store 的 `sidebarCollapsed` 状态控制）

**`HeaderBar.vue`**：
- 左侧：折叠按钮（切换侧边栏）+ 系统名称 "Agent-QR 企业知识库"
- 右侧：用户名显示 + 退出按钮
- 退出按钮点击 → 调用 auth store logout() → 跳转登录页

##### 2.8 创建通用组件 (`src/components/common/`)

**`Pagination.vue`**：
- 封装 `el-pagination` 组件
- Props：`total`、`currentPage`、`pageSize`
- Emits：`update:currentPage`、`update:pageSize`、`change`
- 默认 pageSize=10，pageSizes=[10, 20, 50]
- 显示 total、prev、pager、next、sizes、jumper

##### 2.9 创建 App Store (`src/stores/app.ts`)

```typescript
interface AppState {
  sidebarCollapsed: boolean
}
// actions: toggleSidebar()
```

#### 验证标准

- [ ] TypeScript 类型定义完整，编译无报错
- [ ] API 模块函数签名与后端接口一一对应
- [ ] dev server 代理 `/api` 到后端正常
- [ ] 路由跳转守卫逻辑正确（白名单路径可访问，受保护路径拦截未登录用户）
- [ ] 布局组件渲染正常，侧边栏菜单根据角色变化
- [ ] Axios 拦截器正确处理 401/403/500 响应

---

### 模块 3：auth — 认证模块

**目标**：实现登录/注册页、Auth Store、403/404 错误页、路由守卫完善。

**子Agent 输入**：本节的子任务清单 + `doc/p1-frontend-design.md` 第四/六章（4.1、6.1、6.2）

#### 子任务

##### 3.1 实现 Auth Store (`src/stores/auth.ts`)

```typescript
// State
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
// login(username, password)
//   1. 调用 authApi.login()
//   2. 保存 token 到 state + localStorage
//   3. 保存 user 到 state + localStorage（序列化为 JSON）
//   4. 返回 Promise
//
// register(data)
//   1. 调用 authApi.register()
//   2. 返回 Promise（不自动登录）
//
// fetchUserInfo()
//   1. 调用 authApi.getUserInfo()
//   2. 更新 user state
//
// logout()
//   1. 清除 state 中的 token 和 user
//   2. 清除 localStorage 中的 token 和 user
//   3. router.push('/login')
//
// 初始化时从 localStorage 恢复 token 和 user
```

**`src/stores/auth.ts`** 参考实现要点：
- 使用 Composition API 风格的 Pinia Store（`defineStore` + setup 函数）
- `token` 和 `user` 同时存储在 state 和 localStorage 中
- store 初始化时检查 localStorage 恢复状态

##### 3.2 实现登录页 (`views/login/LoginView.vue`)

按照设计文档 6.1 节实现：

- 使用 `el-form` + `el-input`
- 用户名输入框（`el-input`，必填校验，`v-model="form.username"`）
- 密码输入框（`el-input type="password"`，必填校验，支持 show-password 切换）
- 「登录」按钮：
  - `el-button type="primary"`，整行宽度
  - loading 状态（防重复提交）
  - 调用 `authStore.login()`
- 表单校验规则：用户名必填、密码必填
- 成功后跳转：优先 `route.query.redirect`，否则 `/chat`
- 失败处理：ElMessage.error 显示后端返回的错误消息
- 「立即注册」链接：`router-link to="/register"`

外观：居中卡片式布局，使用 `GuestLayout`。

##### 3.3 实现注册页 (`views/register/RegisterView.vue`)

按照设计文档 6.2 节实现：

- 用户名（必填，`el-input`）
- 密码（必填，`el-input type="password"`）
- 真实姓名（选填，`el-input`）
- 邮箱（选填，`el-input`，邮箱格式校验）
- 手机号（选填，`el-input`，手机号格式校验）
- 「注册」按钮（loading 状态）
- 成功后：ElMessage.success("注册成功，请登录") → `router.push('/login')`
- 失败后：ElMessage.error 显示后端错误消息
- 「立即登录」链接 → `/login`

外观：居中卡片式布局，使用 `GuestLayout`。

##### 3.4 实现 403 页面 (`views/error/403.vue`)

- 居中显示 "403" 大号文字
- 副标题："抱歉，您没有权限访问此页面"
- `el-button`「返回首页」→ 跳转 `/chat`

##### 3.5 实现 404 页面 (`views/error/404.vue`)

- 居中显示 "404" 大号文字
- 副标题："页面不存在"
- `el-button`「返回首页」→ 跳转 `/chat`

##### 3.6 更新路由守卫

修改 `src/router/index.ts` 的路由守卫，改为使用 auth store：

```typescript
// 原守卫中使用 getToken() / getUserRoleFromLocalStorage() 的地方
// 替换为使用 useAuthStore()
```

**注意**：在 router 文件中使用 Pinia Store 时，需要先在 `createRouter` 之后获取 store 实例。推荐方式：

```typescript
import { useAuthStore } from '@/stores/auth'

router.beforeEach((to, from, next) => {
  // Pinia 在 app.use 之后才可用，但 router 在 app 创建之前创建
  // 解决方案：在 beforeEach 中动态获取 store
  const authStore = useAuthStore()
  // ...
})
```

> **重要**：如果 `useAuthStore()` 在 router 文件中调用时报 Pinia 未初始化错误，可以采用备选方案：直接在守卫中检查 localStorage，不依赖 Pinia Store。这样 guard 不需要修改，保持 infra 模块的实现即可。

#### 验证标准

- [ ] 登录成功 → Token 存入 localStorage → 跳转 `/chat`
- [ ] 登录失败 → 显示错误消息（"用户名或密码错误"）
- [ ] 注册成功 → 提示"注册成功，请登录" → 跳转 `/login`
- [ ] 注册失败 → 显示后端错误（如"用户名已存在"）
- [ ] 未登录访问 `/chat` → 自动跳转 `/login?redirect=/chat`
- [ ] 登录后访问 `/login` → 自动跳转 `/chat`
- [ ] 退出登录 → Token 清除 → 跳转 `/login`
- [ ] 页面刷新后登录状态保持
- [ ] 403/404 页面显示正确

---

### 模块 4：chat — 智能问答模块

**目标**：实现核心问答页面，含会话列表、消息气泡、输入框。

**子Agent 输入**：本节的子任务清单 + `doc/p1-frontend-design.md` 6.3 节 + 第四章类型定义

#### 子任务

##### 4.1 实现会话列表组件 (`components/chat/ConversationList.vue`)

- 页面加载时调用 `chatApi.listConversations()` 获取会话列表
- Props：`conversations: Conversation[]`、`activeId?: number`、`loading: boolean`
- Emits：`select(conversationId: number)`、`delete(conversationId: number)`、`create`
- 顶部「+ 新会话」按钮（`el-button`，type="primary"，整行宽度）
- 列表项显示：
  - 会话标题（第一个问题前 30 字，超长使用 `truncateText()` 截断）
  - 消息数量
  - 更新时间（使用 `formatDateTime()`）
- 点击列表项 → `emit('select', id)`，高亮选中项
- 悬停显示删除按钮（`el-icon-close`）→ 二次确认（`ElMessageBox.confirm`）→ `emit('delete', id)`
- 空状态：`el-empty` 显示"暂无会话"
- 删除操作时，被操作的项显示 loading 状态

##### 4.2 实现消息气泡组件 (`components/chat/MessageBubble.vue`)

- Props：`role: 'user' | 'assistant'`、`content: string`、`sources?: RetrievedDocument[]`、`loading?: boolean`
- **user 消息**：
  - 右对齐
  - 蓝色背景（`$primary-color`）白色文字气泡
  - 显示 "👤" 或用户名头像
- **assistant 消息**：
  - 左对齐
  - 白色背景灰色边框气泡
  - 显示 "🤖" 或机器人头像
  - 支持 Markdown 渲染（P1 阶段可用简单的 `v-html` + 换行处理，或引入 `marked` 库）
  - **引用来源**：底部显示来源卡片
    - 使用 `el-tag` 或自定义样式展示来源文档标题
    - 点击可展开查看引用片段（可选实现）
- **loading 状态**（assistant 角色）：
  - 显示"思考中..."加载动画（可使用 `el-skeleton` 或 CSS 点点动画）
- `sources` 为 `null` 或空数组时不显示来源区域

##### 4.3 实现输入框组件 (`components/chat/ChatInput.vue`)

- Props：`loading: boolean`、`disabled: boolean`
- Emits：`send(content: string)`
- 多行文本输入（`el-input type="textarea"`，rows=3）
- Enter 发送（阻止默认换行），Shift+Enter 换行
- 右侧「发送」按钮（`el-button type="primary"`，带图标）
- `loading=true` 或输入为空时发送按钮禁用
- 输入框在 loading 时也禁用
- placeholder："请输入您的问题... (Enter 发送，Shift+Enter 换行)"

##### 4.4 实现问答页 (`views/chat/ChatView.vue`)

按照设计文档 6.3 节实现完整布局：

- **整体布局**：左侧 ConversationList（宽度 280px，可折叠）+ 右侧聊天区
- **左侧会话列表**：
  - 页面加载时调用 `chatApi.listConversations()` 加载会话
  - 默认选中最近会话（第一个）
  - 无会话时选中 `null`
- **右侧聊天区**（默认状态）：
  - 未选中会话时：显示欢迎语
    - "🤖 您好！我是企业知识库助手，请问有什么可以帮助您的？"
  - 选中会话时：
    - 加载历史消息（`chatApi.getMessages(conversationId)`）
    - 渲染消息列表（使用 `MessageBubble` × N）
    - 最新消息在底部
- **发送消息流程**（P1 同步模式）：
  1. 用户输入问题，点击发送
  2. 在消息列表中新增一条 user 消息（本地乐观渲染）
  3. 在消息列表末尾新增一条 loading 状态的 assistant 消息
  4. 调用 `chatApi.ask(query, conversationId)`（**同步请求**）
  5. 返回后：
     - 将 loading 消息替换为完整的 AI 回答（含引用来源）
     - 如果 `conversationId` 为 null（新会话），更新 conversationId，刷新左侧会话列表
  6. 错误处理：loading 消息替换为错误提示
- **空检索结果处理**：API 返回的 answer 为空或 retrieved documents 为空时，显示"知识库中暂无相关信息，请联系管理员上传相关文档"
- **滚动行为**：新消息到达时自动滚动到底部（使用 `nextTick` + `scrollIntoView` 或 ref 操作）
- **切换会话**：点击会话列表项 → 加载该会话的消息历史 → 滚动到底部
- 使用 `MainLayout`（聊天页作为主布局的默认内容）

#### 验证标准

- [ ] 会话列表正确加载，点击切换正常
- [ ] 创建新会话 → 提问 → 会话出现在列表中（标题用第一个问题前 30 字）
- [ ] 发送问题 → loading 动画 → 回答正确显示（含引用来源）
- [ ] 删除会话 → 二次确认 → 列表刷新 → 如果删除当前会话则回退到欢迎页
- [ ] 无会话时显示欢迎语
- [ ] 无消息时显示合理空状态
- [ ] 消息列表自动滚到底部
- [ ] 空检索结果有明确提示

---

### 模块 5：knowledge — 知识库管理模块

**目标**：实现知识库文档管理页面（仅 ADMIN 可见）。

**子Agent 输入**：本节的子任务清单 + `doc/p1-frontend-design.md` 6.4 节

#### 子任务

##### 5.1 实现文档状态标签 (`components/knowledge/StatusTag.vue`)

- Props：`status: string`、`errorMsg?: string`
- 映射关系：

| status 值 | 标签颜色 | 显示文字 | 图标/动画 |
|-----------|---------|---------|----------|
| `UPLOADED` | info（灰）| 已上传 | — |
| `PARSING` | primary（蓝）| 解析中 | `el-icon-loading` |
| `CHUNKING` | primary（蓝）| 切片中 | `el-icon-loading` |
| `EMBEDDING` | primary（蓝）| 向量化中 | `el-icon-loading` |
| `READY` | success（绿）| 就绪 | — |
| `FAILED` | danger（红）| 失败 | 鼠标悬停 Tooltip 显示 `errorMsg` |
| `DELETING` | warning（橙）| 删除中 | `el-icon-loading` |

使用 `el-tag` 组件实现，type 属性按上表设置。

##### 5.2 实现上传文档弹窗 (`components/knowledge/UploadDialog.vue`)

- Props：`visible: boolean`
- Emits：`update:visible`、`success`
- `el-dialog` 弹窗，标题"上传文档"，宽度 520px
- 文件选择区域：
  - 使用 `el-upload` 组件（`drag` 模式，支持点击 + 拖拽）
  - 上传前校验（`before-upload` 钩子）：
    - 文件类型：仅 `.pdf`、`.docx`、`.txt`、`.md`
    - 文件大小：≤ 50MB（50 * 1024 * 1024 bytes）
    - 不符合时返回 `false` 并 `ElMessage.warning` 提示
  - `auto-upload=false`（手动控制上传时机）
- 文档标题输入框（`el-input`，选填，placeholder="默认使用文件名"）
- 底部按钮：
  - 「取消」→ 关闭弹窗 + 清空已选文件
  - 「确认上传」→ 调用 `knowledgeApi.upload(file, title)` → 上传成功后 `emit('success')` → 关闭弹窗
- 上传过程中显示 loading 遮罩

##### 5.3 实现文档列表表格 (`components/knowledge/DocumentTable.vue`)

- Props：`documents: DocumentInfo[]`、`loading: boolean`
- Emits：`delete(documentId: number)`
- 使用 `el-table` 组件
- 表格列：
  - 文件名（`fileName`，`min-width="180"`，`show-overflow-tooltip`）
  - 类型（`fileType`，`width="80"`，显示大写如 PDF/DOCX/TXT/MD）
  - 大小（`fileSize`，`width="100"`，使用 `formatFileSize()` 格式化）
  - 状态（`status`，`width="120"`，使用 `StatusTag` 组件）
  - 上传时间（`createTime`，`width="160"`，使用 `formatDateTime()` 格式化）
  - 操作（`width="100"`，fixed="right"）
    - 「删除」按钮 → `emit('delete', id)`
- 空状态插槽：`<el-empty description="暂无文档，请上传" />`
- 表格高度自适应（`max-height` 设置）

##### 5.4 实现知识库管理页 (`views/knowledge/KnowledgeView.vue`)

- 页面标题："知识库管理"
- 顶部操作栏：
  - 「+ 上传文档」按钮（`el-button type="primary"`）→ 打开 `UploadDialog`
- `DocumentTable` 列表展示
- `Pagination` 分页（数据从 `knowledgeApi.listDocuments(page, size)` 获取）
- 页面加载时获取第 1 页数据
- 上传成功后自动刷新第 1 页
- 删除：二次确认 → `knowledgeApi.deleteDocument(id)` → 刷新当前页
- 使用 `MainLayout`

##### 5.5 路由配置

在 `src/router/index.ts` 中添加：
```typescript
{
  path: '/admin/knowledge',
  name: 'Knowledge',
  component: () => import('@/views/knowledge/KnowledgeView.vue'),
  meta: { requiresAuth: true, requiresAdmin: true, title: '知识库管理' },
}
```

#### 验证标准

- [ ] 文档列表正确加载（分页正常）
- [ ] 上传 PDF/DOCX/TXT/MD → 成功 → 列表刷新
- [ ] 上传不支持的文件类型 → 前端拦截提示
- [ ] 上传超大文件（> 50MB）→ 前端拦截提示
- [ ] 删除文档 → 二次确认 → 列表刷新
- [ ] 文档状态标签颜色和文字正确
- [ ] 非 admin 用户无法访问 `/admin/knowledge`（跳转 403）

---

### 模块 6：user — 用户管理模块

**目标**：实现用户管理页面（仅 ADMIN 可见）。

**子Agent 输入**：本节的子任务清单 + `doc/p1-frontend-design.md` 6.5 节

#### 子任务

##### 6.1 实现用户列表表格 (`components/user/UserTable.vue`)

- Props：`users: UserInfo[]`、`loading: boolean`
- Emits：`edit(user: UserInfo)`、`toggleStatus(user: UserInfo)`
- 使用 `el-table` 组件
- 表格列：
  - ID（`id`，`width="60"`）
  - 用户名（`username`，`width="120"`）
  - 姓名（`realName`，`width="100"`）
  - 角色（`role`，`width="100"`）
    - `admin` → `el-tag type="danger"` 红色
    - `user` → `el-tag type="info"` 灰色
  - 状态（`status`，`width="80"`）
    - `1`（启用）→ `el-tag type="success"` 绿色"启用"
    - `0`（禁用）→ `el-tag type="danger"` 红色"禁用"
  - 创建时间（`createTime`，`width="160"`，使用 `formatDateTime()` 格式化）
  - 操作（`width="140"`，fixed="right"）
    - 「编辑」按钮（`el-button type="primary" link`）→ `emit('edit', user)`
    - 「启用/禁用」按钮（`el-button link`）
      - 启用中 → 显示 `el-icon-lock`"禁用"
      - 已禁用 → 显示 `el-icon-unlock`"启用"
      - → `emit('toggleStatus', user)`
- 空状态：`<el-empty description="暂无用户" />`

##### 6.2 实现用户表单弹窗 (`components/user/UserFormDialog.vue`)

- Props：`visible: boolean`、`mode: 'create' | 'edit'`、`userData?: UserInfo`
- Emits：`update:visible`、`success`
- `el-dialog` 弹窗，宽度 480px
  - 创建模式标题："新增用户"
  - 编辑模式标题："编辑用户"
- `el-form` 表单：
  - **创建模式**：
    - 用户名（`el-input`，必填，2-20 字符）
    - 密码（`el-input type="password"`，必填，6-30 字符）
    - 真实姓名（`el-input`，选填）
    - 邮箱（`el-input`，选填，邮箱格式校验）
    - 手机号（`el-input`，选填，11 位数字校验）
  - **编辑模式**：
    - 真实姓名（`el-input`）
    - 邮箱（`el-input`，邮箱格式校验）
    - 手机号（`el-input`，11 位数字校验）
    - 角色（`el-select`：admin / user）
    - **不含**密码字段
- 编辑模式下用 `userData` 回填表单
- 底部：「取消」+「确认」按钮
- 提交：
  - 创建 → `userApi.createUser(data)`
  - 编辑 → `userApi.updateUser(userData.id, data)`
- 成功后 → `emit('success')` → `ElMessage.success` → 关闭弹窗

##### 6.3 实现用户管理页 (`views/user/UserManageView.vue`)

- 页面标题："用户管理"
- 顶部操作栏：
  - 「+ 新增用户」按钮（`el-button type="primary"`）→ 打开 `UserFormDialog`（创建模式）
  - 搜索框（`el-input`，`clearable`，`placeholder="搜索用户名/姓名..."`，`suffix-icon="Search"`）
    - 输入关键词按回车搜索
    - 清空时恢复全部列表
- `UserTable` 列表展示
- `Pagination` 分页
- 搜索时重置到第 1 页
- 编辑/新增成功后刷新当前页
- 启用/禁用 → 二次确认 → `userApi.toggleStatus(id, newStatus)` → 刷新列表
- 使用 `MainLayout`

##### 6.4 路由配置

在 `src/router/index.ts` 中添加：
```typescript
{
  path: '/admin/users',
  name: 'UserManage',
  component: () => import('@/views/user/UserManageView.vue'),
  meta: { requiresAuth: true, requiresAdmin: true, title: '用户管理' },
}
```

#### 验证标准

- [ ] 用户列表正确加载（分页正常）
- [ ] 搜索关键词 → 列表正确过滤
- [ ] 新增用户 → 表单校验 → 提交成功 → 列表刷新
- [ ] 编辑用户 → 数据回填正确 → 提交成功 → 列表刷新
- [ ] 启用/禁用用户 → 二次确认 → 状态正确切换
- [ ] 非 admin 用户无法访问 `/admin/users`（跳转 403）

---

### 模块 7：dashboard — 数据仪表盘

**目标**：实现数据仪表盘页面（仅 ADMIN 可见），含统计卡片 + ECharts 图表。

**子Agent 输入**：本节的子任务清单 + `doc/p1-frontend-design.md` 6.6 节

#### 子任务

##### 7.1 实现统计卡片区

- 4 个 `el-card` 卡片，水平排列（使用 `el-row` + `el-col :span="6"`）
- 每个卡片内容：
  - 数字（大号字体，32px，加粗，主色）
  - 标签文字（小号，灰色）
- 卡片列表：
  - 今日问答：`dashboard.todayQA`
  - 今日新增用户：`dashboard.todayNewUsers`
  - 文档总数：`dashboard.totalDocuments`
  - 用户总数：`dashboard.totalUsers`
- 加载状态：使用 `el-skeleton` 显示骨架屏
- 数据为 0 时正常显示 "0"

##### 7.2 实现近 7 天问答趋势图（ECharts 折线图）

- 使用 `vue-echarts` 的 `<v-chart>` 组件
- 折线图配置：
  - X 轴（`xAxis`）：日期（`weeklyTrend[].statDate`），格式化为 MM-DD
  - Y 轴（`yAxis`）：问答数量
  - 数据系列（`series`）：折线（`type: 'line'`），平滑（`smooth: true`）
  - 颜色：使用 `$primary-color`
  - 数据：`weeklyTrend[].qaCount`
  - tooltip 显示详细信息
  - grid 留白合理
- 容器高度约 350px
- 无数据时显示 `el-empty` 替代图表

##### 7.3 实现文档类型分布图（ECharts 饼图）

- 使用 `vue-echarts` 的 `<v-chart>` 组件
- 饼图配置：
  - 类型：`type: 'pie'`
  - 数据：从 `docTypeDistribution`（Record<string, number>）转换
    - 格式：`[{ name: 'pdf', value: 15 }, { name: 'docx', value: 10 }, ...]`
  - 显示百分比标签（`label: { formatter: '{b}: {d}%' }`）
  - 颜色：使用 Element Plus 配色方案
  - radius：`['40%', '70%']`（环形饼图，更好看）
- 容器高度约 350px
- 无数据时显示 `el-empty` 替代图表

##### 7.4 实现仪表盘页面 (`views/dashboard/DashboardView.vue`)

- 页面标题："数据仪表盘"
- 页面加载时调用 `statisticsApi.getDashboard()` 获取数据
- 布局：
  - 顶部：统计卡片区（4 个卡片一行）
  - 下方：`el-row` 两栏布局（`el-col :span="12"`）
    - 左：近 7 天问答趋势图（`el-card` 包裹）
    - 右：文档类型分布图（`el-card` 包裹）
- 数据请求失败 → `ElMessage.error("数据加载失败")`
- API 返回数据全部为 0 时正常展示（不显示空状态）
- 图表组件仅在周趋势有数据/文档分布有数据时渲染，否则显示空状态
- 使用 `MainLayout`

##### 7.5 路由配置

在 `src/router/index.ts` 中添加：
```typescript
{
  path: '/admin/dashboard',
  name: 'Dashboard',
  component: () => import('@/views/dashboard/DashboardView.vue'),
  meta: { requiresAuth: true, requiresAdmin: true, title: '数据仪表盘' },
}
```

#### 验证标准

- [ ] 4 个统计卡片显示正确数据
- [ ] 折线图正确渲染近 7 天趋势（日期和数值对应）
- [ ] 饼图正确渲染文档类型分布（百分比正确）
- [ ] API 返回空数据（weeklyTrend=[] 或 docTypeDistribution={}）时图表区显示空状态
- [ ] API 调用失败时有 toast 提示
- [ ] 非 admin 用户无法访问 `/admin/dashboard`（跳转 403）

---

## 六、主Agent 工作流程

### 6.1 启动流程

1. **读取本 Prompt 文件**，理解全部 7 个模块的内容
2. **检查 `agent-qr-web-frontend/` 当前状态**：
   - 确认 `package.json` 中的依赖版本
   - 确认已有文件列表
3. **按顺序调度子Agent**：
   - 第 1 步：生成 **init 子Agent**，完成 init 模块
   - 等待 init 完成并验证 → 更新进度
   - 第 2 步：生成 **infra 子Agent**，完成 infra 模块
   - 等待 infra 完成并验证 → 更新进度
   - 第 3-7 步：**并行生成**（如果条件允许）auth、knowledge、user、dashboard 子Agent
     - 但 chat 必须排在 auth 之后（第 4 步在 auth 第 3 步完成后）
   - 实际上，auth 完成后可以同时启动 chat、knowledge、user、dashboard（它们互不依赖）

### 6.2 生成子Agent 的模板

为每个模块生成子Agent 时，主Agent 应发送以下内容：

```
你是一个专业的前端开发 Agent。请实现 P1 阶段前端工程的 [模块名称] 模块。

## 项目上下文
- 目标项目目录：agent-qr-web-frontend/
- 技术栈：Vue3 + TypeScript + Element Plus + Pinia + Vue Router + Axios + ECharts
- 已有的基础设施：[列出已完成的模块]

## 实现规格
[从本 Prompt 复制该模块的详细子任务]

## 要求
1. 所有 .vue 文件必须使用 <script setup lang="ts">
2. 所有代码必须严格遵循规格说明
3. 完成后汇报创建/修改了哪些文件
```

### 6.3 进度更新

每完成一个模块，主Agent 必须更新 `doc/p1-qtasks/p1-qprogress.md`：

- 将该模块的状态从 ⬜ 改为 ✅
- 更新对应模块的完成数

### 6.4 异常处理

- 子Agent 返回不完整或不符合规格 → 重新生成子Agent，给出更详细的纠正指令
- 文件冲突 → 以子Agent 的输出为准，覆盖已有文件
- TypeScript 编译报错 → 生成修复子Agent

---

## 七、P1 阶段约束与简化

以下功能在 P1 阶段**不实现**，留到 P2/P3：

| 功能 | P1 处理 | P2/P3 增强 |
|------|---------|-----------|
| 问答模式 | 同步问答（loading → 一次性返回） | SSE 流式逐字渲染 |
| 路由权限 | 简单角色判断（user/admin） | ABAC 细粒度权限 |
| Token 管理 | 单一 Access Token（24h） | 双 Token + 静默刷新 |
| 错误处理 | 统一 toast 提示 | 错误码分类 + 重试 |
| 国际化 | 硬编码中文 | i18n 多语言 |
| 响应式 | 桌面端优先 | 移动端适配 |
| ECharts | 基础折线图 + 饼图 | 可交互钻取图表 |

---

> **参考文档**：
> - 前端详细设计：`doc/p1-frontend-design.md`
> - 系统详细设计：`doc/系统详细设计说明书.md`
> - 各模块任务清单：`doc/p1-qtasks/`
>
> **输出目录**：`agent-qr-web-frontend/`
>
> **Prompt 版本**：v1.0 | **日期**：2026-06-22
