# P2 阶段前端详细设计说明书

> 项目名称：基于 LangChain 的 RAG 企业内部知识库问答 Agent 系统
>
> 文档版本：v1.0
>
> 日期：2026-06-26
>
> 文档状态：待评审
>
> **说明**：本文档为增量设计文档，仅记录 P2 阶段相对于 P1 的新增和变更内容。P1 不变的设计请参见 `p1-frontend-design.md`，后端设计依据请参见 `系统详细设计说明书.md`。

---

## 一、P2 前端技术栈变更

| 技术 | P1 版本 | P2 版本/新增 | 用途 |
|------|---------|-------------|------|
| Vue | 3.4+ | 不变 | — |
| Vite | 5.x | 不变 | — |
| Pinia | 2.x | 不变 | — |
| Vue Router | 4.x | 不变 | — |
| Axios | 1.x | 不变 | — |
| Element Plus | 2.x | 不变 | — |
| ECharts | 5.x | 不变 | — |
| TypeScript | 5.x | 不变 | — |
| **EventSource** | — | **新增（原生）** | SSE 流式接收（Chat 问答） |
| **@microsoft/fetch-event-source** | — | **新增** | SSE 增强库（支持 POST + 自定义头 + 自动重连） |

> **说明**：P2 前端技术栈基本不变，仅新增 SSE 流式接收能力。推荐使用 `@microsoft/fetch-event-source` 库，它基于 Fetch API 实现 SSE，支持 POST 请求、自定义请求头和自动重连，比原生 EventSource 更适合本项目的 RAG 问答场景（需要携带 Authorization 头）。

---

## 二、P2 项目结构变更

以下列出 P2 阶段**新增（★）**和**修改（△）**的文件：

```
agent-qr-web-frontend/
├── src/
│   ├── api/
│   │   ├── index.ts                 # △ Axios 实例升级（双 Token 静默刷新 + TraceId）
│   │   ├── auth.ts                  # △ 新增 refreshToken() / revokeToken()
│   │   ├── user.ts                  # △ 新增 ABAC 字段传递（department/clearanceLevel/allowedDomains/title）
│   │   ├── knowledge.ts             # △ upload 新增 domain/sensitivityLevel 参数
│   │   ├── chat.ts                  # △ 新增 askStream() SSE 方法 / submitFeedback()
│   │   ├── statistics.ts            # △ getDashboard 响应新增满意度字段
│   │   ├── datasource.ts            # ★ 多源数据接入 API
│   │   ├── catalog.ts               # ★ 知识目录 API
│   │   └── dataquality.ts           # ★ 数据质量报告 API
│   ├── router/
│   │   └── index.ts                 # △ 新增路由 + ABAC 路由守卫升级
│   ├── stores/
│   │   ├── auth.ts                  # △ ABAC 用户字段扩展 + 双 Token 管理
│   │   └── app.ts                   # 不变
│   ├── views/
│   │   ├── login/LoginView.vue      # 不变
│   │   ├── register/RegisterView.vue # 不变
│   │   ├── chat/
│   │   │   └── ChatView.vue         # △ SSE 流式渲染 + 反馈评价 + 域选择
│   │   ├── knowledge/
│   │   │   └── KnowledgeView.vue    # △ domain/sensitivityLevel 列 + 软删除
│   │   ├── user/
│   │   │   └── UserManageView.vue   # △ ABAC 字段（部门/密级/域/职级）
│   │   ├── dashboard/
│   │   │   └── DashboardView.vue    # △ 满意度指标卡片 + 趋势图
│   │   ├── datasource/              # ★ 多源数据接入管理
│   │   │   └── DataSourceView.vue
│   │   ├── catalog/                 # ★ 知识目录浏览
│   │   │   └── CatalogView.vue
│   │   ├── dataquality/             # ★ 数据质量报告
│   │   │   └── QualityReportView.vue
│   │   └── error/
│   │       ├── 404.vue              # 不变
│   │       └── 403.vue              # 不变
│   ├── components/
│   │   ├── layout/
│   │   │   ├── MainLayout.vue       # 不变
│   │   │   ├── GuestLayout.vue      # 不变
│   │   │   ├── Sidebar.vue          # △ ABAC 菜单动态可见性
│   │   │   └── HeaderBar.vue        # △ Token 过期倒计时 + 静默刷新提示
│   │   ├── chat/
│   │   │   ├── ConversationList.vue # 不变
│   │   │   ├── MessageBubble.vue    # △ 流式逐字渲染 + 反馈按钮（👍/👎）
│   │   │   └── ChatInput.vue        # △ 域选择器 + 停止生成按钮
│   │   ├── knowledge/
│   │   │   ├── DocumentTable.vue    # △ 新增 domain/sensitivityLevel 列
│   │   │   ├── UploadDialog.vue     # △ 新增域选择 + 密级选择
│   │   │   └── StatusTag.vue        # △ 新增 DELETING 状态样式
│   │   ├── user/
│   │   │   ├── UserTable.vue        # △ 新增 ABAC 字段列
│   │   │   └── UserFormDialog.vue   # △ 新增 ABAC 字段表单
│   │   ├── datasource/              # ★ 数据源组件
│   │   │   ├── DataSourceTable.vue
│   │   │   ├── DataSourceFormDialog.vue
│   │   │   └── SyncStatusTag.vue
│   │   ├── catalog/                 # ★ 知识目录组件
│   │   │   └── CatalogTree.vue
│   │   └── common/
│   │       └── Pagination.vue       # 不变
│   ├── utils/
│   │   ├── token.ts                 # △ 双 Token 存取（access + refresh）
│   │   ├── format.ts               # △ 新增密级/域/编码格式化函数
│   │   └── sse.ts                   # ★ SSE 流式请求工具
│   ├── types/
│   │   └── index.ts                 # △ 新增 ABAC/DataSource/Catalog/Quality 等类型
│   ├── styles/
│   │   ├── global.scss              # △ 流式光标动画 / 反馈按钮样式
│   │   └── variables.scss           # 不变
│   ├── App.vue                      # 不变
│   └── main.ts                      # 不变
├── .env.development                 # △ 新增 VITE_SSE_TIMEOUT 等配置
└── package.json                     # △ 新增 @microsoft/fetch-event-source 依赖
```

---

## 三、P2 路由设计变更

### 3.1 路由表变更

| 路径 | 视图 | 权限 | 说明 | 阶段 |
|------|------|------|------|------|
| `/login` | LoginView | 公开 | 登录页（不变） | P1 |
| `/register` | RegisterView | 公开 | 注册页（不变） | P1 |
| `/chat` | ChatView | 登录用户 | 问答主页（△ SSE流式升级） | P1→P2 |
| `/admin/users` | UserManageView | ADMIN | 用户管理（△ ABAC字段） | P1→P2 |
| `/admin/knowledge` | KnowledgeView | ADMIN | 知识库管理（△ domain/密级） | P1→P2 |
| `/admin/dashboard` | DashboardView | ADMIN | 统计仪表盘（△ 满意度） | P1→P2 |
| `/admin/datasource` | DataSourceView | **ADMIN** | ★ 多源数据接入管理 | **P2 新增** |
| `/admin/catalog` | CatalogView | **ADMIN** | ★ 知识目录浏览 | **P2 新增** |
| `/admin/quality` | QualityReportView | **ADMIN** | ★ 数据质量报告 | **P2 新增** |
| `/403` | 403.vue | 公开 | 无权限提示（不变） | P1 |
| `/:pathMatch(.*)*` | 404.vue | 公开 | 404 页面（不变） | P1 |

### 3.2 ABAC 路由守卫升级

P2 路由守卫在 P1 角色判断基础上，增加 ABAC 属性检查：

```typescript
// router/index.ts — P2 升级

import type { UserPrincipal } from '@/stores/auth'

router.beforeEach((to, _from, next) => {
  let token: string | null = null
  let user: UserPrincipal | null = null

  try {
    const authStore = useAuthStore()
    token = authStore.accessToken
    user = authStore.user
  } catch {
    token = getAccessToken()
    user = getUserFromStorage()
  }

  if (to.path === '/login' || to.path === '/register') {
    token ? next('/chat') : next()
  } else {
    if (!token) {
      next(`/login?redirect=${to.path}`)
      return
    }

    // ★ P1 角色判断保留
    if (to.meta.requiresAdmin && user?.role !== 'admin') {
      next('/403')
      return
    }

    // ★ P2 ABAC 扩展：页面级权限
    if (to.meta.requiresDomain && user) {
      const requiredDomain = to.meta.requiresDomain as string
      if (!user.allowedDomains?.includes(requiredDomain)) {
        next('/403')
        return
      }
    }

    // ★ P2 ABAC 扩展：职级限制
    if (to.meta.requiresTitle && user) {
      const allowedTitles = to.meta.requiresTitle as string[]
      if (!allowedTitles.includes(user.title || '')) {
        next('/403')
        return
      }
    }

    next()
  }
})
```

**路由 Meta 扩展定义**：

| Meta 字段 | 类型 | 说明 |
|-----------|------|------|
| `requiresAuth` | `boolean` | 是否需要登录（P1 已有） |
| `requiresAdmin` | `boolean` | 是否需要 admin 角色（P1 已有） |
| `requiresDomain` | `string` | ★ 需要的业务域访问权限 |
| `requiresTitle` | `string[]` | ★ 需要的职级列表 |
| `layout` | `'main' \| 'guest'` | 布局类型（P1 已有） |
| `title` | `string` | 页面标题（P1 已有） |

---

## 四、P2 状态管理变更

### 4.1 Auth Store 升级（`stores/auth.ts`）

```typescript
// ★ P2 ABAC 扩展用户主体
interface UserPrincipal {
  id: number
  username: string
  realName: string
  role: string
  email: string
  phone: string
  // ★ P2 ABAC 字段
  department: string           // 所属部门 HR/FINANCE/RD/SALES/COMMON
  clearanceLevel: number       // 数据密级 0=公开 1=内部 2=机密 3=绝密
  allowedDomains: string[]     // 允许访问的业务域列表
  title: string                // 职级 employee/manager/director
}

interface AuthState {
  accessToken: string | null      // ★ Access Token（30min）
  refreshToken: string | null     // ★ Refresh Token（7day）
  tokenExpiresAt: number | null   // ★ Access Token 过期时间戳
  user: UserPrincipal | null
}

// Actions:
// - login(username, password):
//     调用 POST /api/auth/login
//     → 保存 accessToken + refreshToken + tokenExpiresAt + user（含ABAC属性）
//
// - register(data): 不变
//
// - fetchUserInfo():
//     调用 GET /api/auth/info
//     → 刷新 user（含ABAC属性）
//
// - refreshAccessToken(): ★ 新增
//     调用 POST /api/auth/refresh
//     → 用 refreshToken 换取新的 accessToken + refreshToken
//     → 更新 accessToken + refreshToken + tokenExpiresAt
//
// - logout():
//     调用 POST /api/auth/revoke（★ 新增，撤销 refreshToken）
//     → 清除所有 Token + user → 跳转 /login
//
// - hasDomain(domain: string): boolean  ★ 新增
// - hasClearance(level: number): boolean ★ 新增
// - isManager(): boolean  ★ 新增
// - isDirector(): boolean ★ 新增
```

**P2 登录响应数据结构变更**：

```typescript
// 登录 API 返回（P2 双Token响应）
interface LoginVO {
  accessToken: string          // Access Token，有效期 30 分钟
  refreshToken: string         // Refresh Token，有效期 7 天
  expiresIn: number            // Access Token 有效期（秒），1800
  userId: number
  username: string
  role: string
  department: string           // ★
  clearanceLevel: number       // ★
  allowedDomains: string       // ★ 逗号分隔，前端转为数组
  title: string                // ★
}
```

### 4.2 App Store（不变）

P2 阶段 App Store 保持 P1 设计不变。

---

## 五、P2 API 层变更

### 5.1 Axios 实例升级（`api/index.ts`）— 双 Token 静默刷新

```typescript
// ★ P2 核心变更：双 Token 机制 + 请求重放队列

import axios from 'axios'
import { ElMessage } from 'element-plus'
import {
  getAccessToken,
  getRefreshToken,
  setAccessToken,
  setRefreshToken,
  setTokenExpiresAt,
  removeAllTokens,
} from '@/utils/token'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000, // ★ P2 延长至 30s（大文件上传需要更长）
})

// ★ 是否正在刷新 Token
let isRefreshing = false
// ★ 等待刷新期间暂存的请求队列
let pendingRequests: Array<{
  resolve: (token: string) => void
  reject: (error: Error) => void
}> = []

// ★ 处理等待队列
function processQueue(error: Error | null, token?: string) {
  pendingRequests.forEach(({ resolve, reject }) => {
    if (error || !token) reject(error || new Error('Token 刷新失败'))
    else resolve(token)
  })
  pendingRequests = []
}

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // ★ 添加 TraceId（从前端生成，便于全链路追踪）
    const traceId = generateTraceId()
    config.headers['X-Trace-Id'] = traceId
    return config
  },
  (error) => Promise.reject(error),
)

// ★ 响应拦截器 — P2 双 Token 静默刷新逻辑
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) return res

    if (res.code === 401) {
      // ★ 401 不再直接跳登录，先尝试刷新 Token
      const refreshTokenValue = getRefreshToken()
      if (refreshTokenValue && !isRefreshing) {
        return handleTokenRefresh(response.config)
      }
      // 无可用的 refreshToken → 跳登录
      removeAllTokens()
      window.location.href = '/login'
      return Promise.reject(new Error(res.message || '未授权'))
    }
    // 403/500 及其他错误处理同 P1
    if (res.code === 403) {
      ElMessage.error('权限不足')
      return Promise.reject(new Error(res.message || '权限不足'))
    }
    if (res.code === 500) {
      ElMessage.error('服务器内部错误')
      return Promise.reject(new Error(res.message || '服务器内部错误'))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    // SSE 流式请求错误特殊处理
    if (error.config?.responseType === 'stream') {
      return Promise.reject(error)
    }
    ElMessage.error('网络连接失败')
    return Promise.reject(error)
  },
)

/**
 * ★ 调用 Refresh Token 接口，静默刷新 Access Token
 * 使用队列机制保证并发请求只刷新一次
 */
async function handleTokenRefresh(failedConfig: any): Promise<any> {
  if (isRefreshing) {
    // 已有刷新请求在进行中 → 加入等待队列
    return new Promise((resolve, reject) => {
      pendingRequests.push({ resolve, reject })
    }).then((token) => {
      failedConfig.headers.Authorization = `Bearer ${token}`
      return request(failedConfig)
    })
  }

  isRefreshing = true
  try {
    const refreshTokenStr = getRefreshToken()
    const res = await axios.post(
      `${import.meta.env.VITE_API_BASE_URL}/api/auth/refresh`,
      { refreshToken: refreshTokenStr },
    )
    const { accessToken, refreshToken, expiresIn } = res.data.data
    setAccessToken(accessToken)
    setRefreshToken(refreshToken)
    setTokenExpiresAt(Date.now() + expiresIn * 1000)

    processQueue(null, accessToken)

    failedConfig.headers.Authorization = `Bearer ${accessToken}`
    return request(failedConfig)
  } catch (error) {
    processQueue(error as Error)
    removeAllTokens()
    window.location.href = '/login'
    return Promise.reject(error)
  } finally {
    isRefreshing = false
  }
}

/** ★ 生成前端 TraceId（16位 hex） */
function generateTraceId(): string {
  const arr = new Uint8Array(8)
  crypto.getRandomValues(arr)
  return Array.from(arr, (b) => b.toString(16).padStart(2, '0')).join('')
}

export default request
```

### 5.2 API 接口变更汇总

#### 5.2.1 认证 API 变更（`api/auth.ts`）

```
[P1] POST /api/auth/login        →  不变（响应结构变更为双Token）
[P1] POST /api/auth/register     →  不变
[P1] GET  /api/auth/info         →  响应新增 ABAC 字段
[P2] POST /api/auth/refresh      →  ★ 刷新 Access Token
[P2] POST /api/auth/revoke       →  ★ 撤销 Refresh Token（登出）
```

```typescript
// api/auth.ts — P2 升级
export const authApi = {
  login(data: { username: string; password: string }) {
    return request.post('/api/auth/login', data)
    // ★ 返回: { accessToken, refreshToken, expiresIn, userId, username, role,
    //           department, clearanceLevel, allowedDomains, title }
  },
  register(data) { /* 不变 */ },
  getUserInfo() {
    return request.get('/api/auth/info')
    // ★ 返回新增: department, clearanceLevel, allowedDomains, title
  },
  refreshToken(refreshToken: string) {  // ★ 新增
    return request.post('/api/auth/refresh', { refreshToken })
  },
  revokeToken() {  // ★ 新增
    return request.post('/api/auth/revoke')
  },
}
```

#### 5.2.2 问答 API 变更（`api/chat.ts`）

```
[P1] POST /api/chat/ask                              →  保留（兼容），但推荐使用 SSE
[P2] POST /api/chat/ask           (Accept: text/event-stream) → ★ SSE 流式问答
[P1] GET  /api/chat/conversations                     →  不变
[P1] GET  /api/chat/conversations/{id}/messages       →  不变
[P1] DELETE /api/chat/conversations/{id}              →  不变
[P2] POST /api/chat/messages/{id}/feedback            →  ★ 答案反馈评价
```

```typescript
// api/chat.ts — P2 升级
import { fetchEventSource } from '@microsoft/fetch-event-source'
import { getAccessToken } from '@/utils/token'

export const chatApi = {
  // [P1 保留] 同步问答
  ask(query: string, conversationId?: number) { /* 不变 */ },

  // ★ [P2 新增] SSE 流式问答
  askStream(
    query: string,
    domain: string | null,
    conversationId: number | null,
    callbacks: {
      onToken: (token: string) => void,
      onDone: (data: { conversationId: number; sources: SourceVO[] }) => void,
      onError: (error: string) => void,
    },
  ): AbortController {
    const controller = new AbortController()
    const token = getAccessToken()

    fetchEventSource('/api/chat/ask', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ query, conversationId, domain }),
      signal: controller.signal,
      onmessage(event) {
        switch (event.event) {
          case 'token':
            callbacks.onToken(event.data)
            break
          case 'done':
            callbacks.onDone(JSON.parse(event.data))
            break
          case 'error':
            callbacks.onError(event.data)
            break
        }
      },
      onerror(err) {
        callbacks.onError('连接异常，请重试')
        throw err // 停止重连
      },
    })

    return controller // ★ 返回 AbortController 供前端取消生成
  },

  // ★ [P2 新增] 提交反馈评价
  submitFeedback(messageId: number, feedback: 'positive' | 'negative', reason?: string) {
    return request.post(`/api/chat/messages/${messageId}/feedback`, { feedback, reason })
  },

  // 以下不变
  listConversations() { /* 不变 */ },
  getMessages(conversationId: number) { /* 不变 */ },
  deleteConversation(id: number) { /* 不变 */ },
}
```

#### 5.2.3 知识库 API 变更（`api/knowledge.ts`）

```
[P1] POST   /api/knowledge/upload                  →  △ 新增 domain/sensitivityLevel 参数
[P1] GET    /api/knowledge/documents               →  △ 响应新增 domain/sensitivityLevel/sensitivityLabel 字段
[P1] GET    /api/knowledge/documents/{id}          →  △ 响应新增 ABAC 字段
[P1] DELETE /api/knowledge/documents/{id}          →  △ 变为事件驱动软删除（立即返回200，后台异步）
[P1] GET    /api/knowledge/documents/{id}/status   →  △ 新增 DELETING 状态
[P1] GET    /api/knowledge/documents/{id}/chunks    →  不变
```

```typescript
// api/knowledge.ts — P2 升级
export const knowledgeApi = {
  upload(file: File, title?: string, domain?: string, sensitivityLevel?: number) {
    const formData = new FormData()
    formData.append('file', file)
    if (title) formData.append('title', title)
    if (domain) formData.append('domain', domain)            // ★
    if (sensitivityLevel != null) formData.append('sensitivityLevel', String(sensitivityLevel)) // ★
    return request.post('/api/knowledge/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  // listDocuments / getDocument / deleteDocument / getStatus / getChunks 不变
}
```

#### 5.2.4 统计 API 变更（`api/statistics.ts`）

```
[P1] GET /api/statistics/dashboard → △ 响应新增满意度/点赞/点踩统计数据
```

```typescript
// DashboardVO 新增字段:
interface DashboardVO {
  // ... P1 字段不变 ...
  todayPositiveCount: number    // ★ 今日点赞数
  todayNegativeCount: number    // ★ 今日点踩数
  satisfactionRate: number      // ★ 满意度百分比
}
```

#### 5.2.5 ★ 多源数据接入 API（`api/datasource.ts`）— 新增

```typescript
// api/datasource.ts — P2 新增
export const datasourceApi = {
  list(params: { page: number; size: number }) {
    return request.get('/api/datasource/list', { params })
  },
  getById(id: number) {
    return request.get(`/api/datasource/${id}`)
  },
  create(data: DataSourceForm) {
    return request.post('/api/datasource', data)
  },
  update(id: number, data: Partial<DataSourceForm>) {
    return request.put(`/api/datasource/${id}`, data)
  },
  delete(id: number) {
    return request.delete(`/api/datasource/${id}`)
  },
  testConnection(id: number) {  // ★ 连通性测试
    return request.post(`/api/datasource/${id}/test`)
  },
  triggerSync(id: number) {     // ★ 手动触发同步
    return request.post(`/api/datasource/${id}/sync`)
  },
  getSyncHistory(id: number, params: { page: number; size: number }) {
    return request.get(`/api/datasource/${id}/sync-history`, { params })
  },
}
```

#### 5.2.6 ★ 知识目录 API（`api/catalog.ts`）— 新增

```typescript
// api/catalog.ts — P2 新增
export const catalogApi = {
  getCatalogTree() {
    return request.get('/api/catalog/tree')
    // ★ 返回三级树：业务域 → 数据源 → 数据实体
  },
  getDomainStats() {
    return request.get('/api/catalog/stats')
  },
}
```

#### 5.2.7 ★ 数据质量报告 API（`api/dataquality.ts`）— 新增

```typescript
// api/dataquality.ts — P2 新增
export const dataqualityApi = {
  listReports(params: { page: number; size: number }) {
    return request.get('/api/dataquality/reports', { params })
  },
  getReport(batchId: string) {
    return request.get(`/api/dataquality/reports/${batchId}`)
  },
}
```

---

## 六、P2 类型定义变更

```typescript
// types/index.ts — P2 新增和修改的类型

// ==================== △ 修改的已有类型 ====================

// 用户信息 — 新增 ABAC 字段
interface UserInfo {
  // ... P1 字段不变 ...
  department: string            // ★
  clearanceLevel: number        // ★
  allowedDomains: string        // ★ 后端返回逗号分隔字符串，前端解析为数组
  title: string                 // ★
}

// 文档信息 — 新增域和密级字段
interface DocumentInfo {
  // ... P1 字段不变 ...
  domain: string                // ★ 业务域
  sensitivityLevel: number      // ★ 密级 0=公开 1=内部 2=机密 3=绝密
  sensitivityLabel: string      // ★ 密级标签文本
}

// 仪表盘 — 新增满意度字段
interface DashboardVO {
  // ... P1 字段不变 ...
  todayPositiveCount: number    // ★
  todayNegativeCount: number    // ★
  satisfactionRate: number      // ★
}

// ==================== ★ P2 新增类型 ====================

// SSE 流式问答事件
interface SSETokenEvent {
  event: 'token'
  data: string  // 单个 token 文本
}

interface SSEDoneEvent {
  event: 'done'
  data: {
    conversationId: number
    sources: SourceVO[]
  }
}

interface SSEErrorEvent {
  event: 'error'
  data: string  // 错误消息
}

// 引用来源（提取为独立类型）
interface SourceVO {
  documentId: number
  documentTitle: string
  content: string
  similarity: number
}

// 反馈评价
interface FeedbackDTO {
  messageId: number
  feedback: 'positive' | 'negative'
  reason?: string
}

// 数据源配置
interface DataSourceConfig {
  id: number
  sourceName: string
  sourceType: 'JDBC' | 'REST' | 'S3'
  domain: string
  status: 'ACTIVE' | 'INACTIVE' | 'ERROR'
  syncStrategy: 'FULL' | 'INCREMENTAL'
  cronExpression: string
  lastSyncAt: string
  totalSynced: number
  connectionConfig: Record<string, any>
  createTime: string
  updateTime: string
}

interface DataSourceForm {
  sourceName: string
  sourceType: 'JDBC' | 'REST' | 'S3'
  domain: string
  syncStrategy: 'FULL' | 'INCREMENTAL'
  cronExpression?: string
  connectionConfig: Record<string, any>
}

interface ConnectionTestResult {
  success: boolean
  latencyMs: number
  dbProduct?: string
  dbVersion?: string
  errorMsg?: string
}

// 同步历史
interface SyncRecord {
  id: number
  datasourceId: number
  syncType: 'FULL' | 'INCREMENTAL'
  status: 'SUCCESS' | 'FAILED' | 'RUNNING'
  totalRows: number
  errorMsg?: string
  syncAt: string
}

// 知识目录
interface CatalogTree {
  domains: DomainNode[]
}

interface DomainNode {
  domainName: string
  sourceCount: number
  totalEntities: number
  sources: SourceNode[]
}

interface SourceNode {
  sourceId: number
  sourceName: string
  sourceType: string
  lastSyncAt: string
  totalSynced: number
  entities: EntityNode[]
}

interface EntityNode {
  entityName: string
  recordCount: number
  lastUpdated: string
}

// 数据质量报告
interface QualityReport {
  batchId: string
  datasourceId: number
  sourceName: string
  totalCount: number
  passCount: number
  failCount: number
  passRate: number
  blocked: boolean
  failures: QualityFailure[]
  checkTime: string
}

interface QualityFailure {
  ruleName: string
  recordIndex: number
  reason: string
}

// ==================== ★ 枚举常量 ====================

/** 业务域 */
const DOMAINS = ['HR', 'FINANCE', 'RD', 'SALES', 'COMMON'] as const
type Domain = (typeof DOMAINS)[number]

/** 密级选项 */
const SENSITIVITY_LEVELS = [
  { value: 0, label: '公开' },
  { value: 1, label: '内部' },
  { value: 2, label: '机密' },
  { value: 3, label: '绝密' },
] as const

/** 职级选项 */
const TITLES = ['employee', 'manager', 'director'] as const
type Title = (typeof TITLES)[number]

/** 部门选项（ABAC） */
const DEPARTMENTS = [
  { value: 'HR', label: '人力资源' },
  { value: 'FINANCE', label: '财务管理' },
  { value: 'RD', label: '研发中心' },
  { value: 'SALES', label: '销售管理' },
  { value: 'COMMON', label: '公共部门' },
] as const

/** 数据源类型 */
const SOURCE_TYPES = [
  { value: 'JDBC', label: '数据库 (JDBC)' },
  { value: 'REST', label: 'REST API' },
  { value: 'S3', label: '文件系统 (S3)' },
] as const
```

---

## 七、P2 页面详细设计（变更与新增）

### 7.1 问答页 P2 升级（`ChatView.vue`）△

#### 核心变更

| P1 行为 | P2 行为 |
|---------|---------|
| 同步问答：发送 → 加载动画 → 一次性返回完整回答 | **SSE 流式**：发送 → 逐 token 渲染 → done 回调 |
| 无业务域概念 | ★ 顶部域选择器（ChatInput 中），限定检索范围 |
| 无停止生成按钮 | ★ 发送后显示「停止生成」按钮（调用 AbortController） |
| 无反馈评价 | ★ AI 回答底部显示 👍/👎 反馈按钮 |
| 单一 Access Token | ★ 静默刷新，用户无感知 |

#### 页面布局（P2 增量）

```
┌──────────────────────────────────────────────────────────────────┐
│  [≡]  Agent-QR 企业知识库           [通知] 用户名 ▼ [退出]        │
├────────────────┬───────────────────────────────────────────────────┤
│  ┌──────────┐ │                                                   │
│  │ + 新会话  │ │  ┌───────────────────────────────────────────┐   │
│  ├──────────┤ │  │  🤖 您好！我是企业知识库助手...              │   │
│  │ 会话 1    │ │  │                                           │   │
│  │ 会话 2    │ │  │  👤 研发部2024年的绩效考核标准？           │   │
│  │ 会话 3    │ │  │                                           │   │
│  │           │ │  │  🤖 根据知识库中《研发部绩效考核制度        │   │
│  │           │ │  │     v2024》文档，2024年研发部的考核         │   │
│  │           │ │  │     标准如下：考核周期｜考核维度｜...       │   │
│  │           │ │  │                         █  ← 流式光标    │   │
│  │           │ │  │                                           │   │
│  │           │ │  │     引用来源：                             │   │
│  │           │ │  │     📎 研发部绩效考核制度v2024.pdf         │   │
│  │           │ │  │     [👍 有帮助] [👎 无帮助]  ← ★ 反馈      │   │
│  │           │ │  └───────────────────────────────────────────┘   │
│  │           │ │  ┌───────────────────────────────────────────┐   │
│  │           │ │  │ [HR ▼] ← 业务域 │ [输入问题...] [停止 ■] │   │
│  │           │ │  └───────────────────────────────────────────┘   │
│  └──────────┘ │                                                   │
└──────────────┴───────────────────────────────────────────────────┘
```

#### 流式问答核心流程

```typescript
// ChatView.vue — P2 核心变更

const sending = ref(false)
const streamingContent = ref('')     // ★ 当前正在流式接收的内容
const currentSources = ref<SourceVO[]>([])
let abortController: AbortController | null = null

async function handleSend(content: string, domain?: string) {
  if (!content.trim() || sending.value) return

  // 1. 添加用户消息
  const userMsg: LocalMessage = {
    id: -Date.now(),
    role: 'user',
    content: content.trim(),
  }
  messages.value.push(userMsg)

  // 2. 添加空的 assistant 消息（流式填充）
  const assistantMsg: LocalMessage = {
    id: -(Date.now() + 1),
    role: 'assistant',
    content: '',
    streaming: true,     // ★ 标记为流式渲染中
  }
  messages.value.push(assistantMsg)
  sending.value = true
  scrollToBottom()

  // 3. ★ 发起 SSE 流式请求
  abortController = chatApi.askStream(
    content.trim(),
    domain || null,
    activeConversationId.value,
    {
      onToken(token: string) {
        // ★ 逐 token 追加到消息内容尾部
        assistantMsg.content += token
        // 触发响应式更新
        triggerRef(assistantMsg)
        scrollToBottom()
      },
      onDone(data) {
        // ★ 流式完成
        assistantMsg.streaming = false
        assistantMsg.sources = data.sources
        assistantMsg.id = data.conversationId

        if (!activeConversationId.value) {
          activeConversationId.value = data.conversationId
          loadConversations()
        }
        sending.value = false
        abortController = null
      },
      onError(error) {
        assistantMsg.streaming = false
        assistantMsg.content = assistantMsg.content || error
        sending.value = false
        abortController = null
      },
    },
  )
}

// ★ 停止生成
function handleStopGeneration() {
  abortController?.abort()
  sending.value = false
  // 在最后一条 AI 消息末尾追加标记
  const lastMsg = messages.value[messages.value.length - 1]
  if (lastMsg?.role === 'assistant' && lastMsg.streaming) {
    lastMsg.streaming = false
    lastMsg.content += '\n\n_（已停止生成）_'
  }
}
```

#### 反馈评价交互

```
用户在 AI 回答底部看到反馈按钮：
  [👍 有帮助]  [👎 无帮助]

点击「👍」→ 调用 POST /api/chat/messages/{id}/feedback { feedback: "positive" }
          → 提示"感谢反馈"

点击「👎」→ 弹出原因输入框（可选）：
          ┌──────────────────────┐
          │ 帮助我们改进回答质量    │
          │ [回答不准确/不完整/     │
          │  与问题无关/其他]      │
          │                      │
          │ [取消]    [提交]      │
          └──────────────────────┘
          → 调用 POST /api/chat/messages/{id}/feedback { feedback: "negative", reason: "xxx" }
          → 提示"感谢反馈，我们会持续改进"
```

### 7.2 知识库管理页 P2 升级（`KnowledgeView.vue`）△

#### 核心变更

| P1 | P2 |
|----|----|
| 上传只填文件名 | ★ 上传时选择 domain（业务域）+ sensitivityLevel（密级） |
| 删除为物理删除 | ★ 删除变为软删除请求（状态→DELETING，后台异步处理） |
| 无域/密级字段 | ★ 列表新增「业务域」「密级」列 |

#### 页面布局变更

```
┌──────────────────────────────────────────────────────────────────┐
│  知识库管理                                                       │
│                                                                   │
│  [ + 上传文档 ]   [ 业务域筛选: [全部 ▼] ]   [ 密级筛选: [全部 ▼] ] │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────────┐│
│  │文件名│类型│大小│业务域★│密级★│状态    │上传时间│操作           ││
│  ├────────────────────────────────────────────────────────────────┤│
│  │绩效  │pdf │2.3 │HR    │内部 │✅就绪  │06-22  │🗑              ││
│  │手册  │docx│1.1 │RD    │公开 │🔄解析  │06-21  │🗑              ││
│  │安全  │pdf │5.0 │COMMON│机密 │🗑删除中│06-20  │—              ││
│  └────────────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────┘
```

#### UploadDialog 升级

```
┌──────────────────────────────────┐
│         上传文档                  │
│                                  │
│  选择文件 *                      │
│  ┌────────────────────────────┐ │
│  │   拖拽文件到此处或点击上传   │ │
│  └────────────────────────────┘ │
│                                  │
│  文档标题（可选）                 │
│  [___________________________]  │
│                                  │
│  ★ 业务域 *                     │
│  [HR ▼]  (HR/FINANCE/RD/SALES/  │
│            COMMON)               │
│                                  │
│  ★ 密级 *                       │
│  ○ 公开  ● 内部  ○ 机密  ○ 绝密 │
│                                  │
│  [取消]            [确认上传]    │
└──────────────────────────────────┘
```

**约束说明**：
- 业务域下拉选项根据当前用户的 `allowedDomains` 属性动态显示（用户只能上传到自己能访问的域）
- 密级选项根据当前用户的 `clearanceLevel` 属性限制（用户不能选择高于自己密级的级别）
- 文件类型：pdf / docx / txt / md（不变）
- 大小限制：≤ 50MB（不变）

### 7.3 用户管理页 P2 升级（`UserManageView.vue`）△

#### 核心变更

| P1 | P2 |
|----|----|
| 用户字段：基础 + role + status | ★ 新增 ABAC 字段：department / clearanceLevel / allowedDomains / title |
| 无职级/部门概念 | ★ 表单新增部门选择、密级选择、允许域多选、职级选择 |

#### 页面布局变更

```
┌──────────────────────────────────────────────────────────────────────┐
│  用户管理                                                             │
│                                                                       │
│  [ + 新增用户 ]          [ 🔍 搜索用户名/姓名... ]                     │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────────────┐│
│  │ID│用户名│姓名│角色│部门★│密级★│允许域★│职级★│状态│创建时间│操作 ││
│  ├──────────────────────────────────────────────────────────────────┤│
│  │1 │admin │管理│admin│COMMON│绝密 │全部    │director│✅│06-01│✏️ 🔒││
│  │2 │zhangs│张三 │user │HR    │内部 │HR      │employee│✅│06-10│✏️ 🔒││
│  │3 │lisi  │李四 │user │RD    │机密 │RD,SALES│manager │❌│06-15│✏️ 🔓││
│  └──────────────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────┘
```

#### UserFormDialog 升级

```
┌──────────────────────────────────────┐
│     新增用户 / 编辑用户               │
│                                      │
│  用户名 *      [_______________]     │
│  密码 *        [_______________]     │ (编辑时隐藏)
│  真实姓名      [_______________]     │
│  邮箱          [_______________]     │
│  手机号        [_______________]     │
│  角色          [admin ▼]            │
│                                      │
│  —— ★ P2 ABAC 属性 ——               │
│  部门          [HR ▼]                │
│  数据密级      [内部 ▼]              │
│  允许访问域    [☑ HR  ☑ RD  ☐ ...] │ (多选)
│  职级          [employee ▼]         │
│                                      │
│  [取消]                  [保存]      │
└──────────────────────────────────────┘
```

### 7.4 统计仪表盘 P2 升级（`DashboardView.vue`）△

#### 核心变更

| P1 | P2 |
|----|----|
| 4 个统计卡片 | ★ 新增 3 个满意度卡片（共 7 个） |
| 基础问答趋势图 | ★ 趋势图新增满意度折线叠加 |
| 文档类型分布饼图 | 不变 |

#### 新增统计卡片

```
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ 今日问答  │ │ 今日新增  │ │ 文档总数  │ │ 用户总数  │ │ ★ 👍点赞 │ │ ★ 👎点踩 │ │ ★ 满意度 │
│   12     │ │   3      │ │   45     │ │   30     │ │   10     │ │    2     │ │  83.3%  │
└──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘
```

#### ECharts 图表升级

- **问答趋势折线图**：在原有基础上叠加一条满意度折线（右轴），便于关联分析
- **新增反馈分布饼图**：👍 正面 / 👎 负面反馈占比

### 7.5 多源数据接入管理页（`DataSourceView.vue`）★ 新增

```
┌──────────────────────────────────────────────────────────────────────┐
│  [≡]  Agent-QR 企业知识库           [通知] 用户名 ▼ [退出]            │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  多源数据接入管理                                                      │
│                                                                       │
│  [ + 新增数据源 ]                    [ 域筛选: [全部 ▼] ]              │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────────────┐│
│  │数据源名称│类型  │业务域│状态 │最近同步       │同步量  │操作       ││
│  ├──────────────────────────────────────────────────────────────────┤│
│  │MySQL-财务│JDBC  │FINANCE│✅ACTIVE│06-26 09:00 │12,500 │🔄🧪✏️🗑 ││
│  │Sales API│REST  │SALES│⚠️ERROR │06-25 18:30 │8,200  │🔄🧪✏️🗑 ││
│  │政策文件桶│S3   │HR    │⏸INACTIVE│—            │—      │🔄🧪✏️🗑 ││
│  └──────────────────────────────────────────────────────────────────┘│
│                                                                       │
│  [ < ]  [ 1 ]  [ 2 ]  [ > ]   共 12 条                                │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

**交互说明**：
- **新增/编辑**：弹出 DataSourceFormDialog，根据 sourceType 动态展示不同的连接配置表单
  - JDBC：URL、用户名、密码、表名列表
  - REST：Base URL、认证头、分页参数
  - S3：Bucket、Prefix、Access Key、Secret Key
- **连通性测试**（🧪）：调用 `testConnection`，显示延迟和数据量预览
- **手动同步**（🔄）：调用 `triggerSync`，显示同步进度
- **删除**：二次确认
- **状态标识**：
  - ACTIVE → 绿色
  - ERROR → 红色
  - INACTIVE → 灰色

#### DataSourceFormDialog（新增数据源弹窗）

```
┌──────────────────────────────────────────────┐
│         新增数据源 / 编辑数据源               │
│                                              │
│  数据源名称 *    [__________________]        │
│  数据源类型 *    [JDBC ▼]                    │
│  业务域 *        [FINANCE ▼]                 │
│  同步策略        ○ 全量  ● 增量              │
│  定时表达式      [0 0 2 * * ?    ] (Cron)   │
│                                              │
│  —— 连接配置（根据 sourceType 动态切换）——    │
│  ┌ JDBC ─────────────────────────────────┐  │
│  │ JDBC URL *   [jdbc:mysql://...     ]  │  │
│  │ 用户名 *     [_______________]        │  │
│  │ 密码 *       [_______________]        │  │
│  │ 表名列表 *    [+ 添加表名 ]           │  │
│  └───────────────────────────────────────┘  │
│                                              │
│  [取消]                    [测试连接] [保存] │
└──────────────────────────────────────────────┘
```

### 7.6 知识目录浏览页（`CatalogView.vue`）★ 新增

```
┌──────────────────────────────────────────────────────────────────────┐
│  [≡]  Agent-QR 企业知识库           [通知] 用户名 ▼ [退出]            │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  知识目录                                                             │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │                                                                  │ │
│  │   📁 HR (人力资源)  2个数据源 · 15个实体                         │ │
│  │   ├── 📂 MySQL-员工库 (JDBC)  最后同步: 06-26 09:00  · 12,500条  │ │
│  │   │   ├── 📄 employee_info    员工基本信息  8,200条               │ │
│  │   │   ├── 📄 salary_record    薪资记录      3,500条               │ │
│  │   │   └── 📄 dept_org         组织架构        800条               │ │
│  │   └── 📂 政策文件桶 (S3)    最后同步: 06-25 18:00  · 245条        │ │
│  │       ├── 📄 绩效考核制度     2024版      12页                    │ │
│  │       └── 📄 员工手册         2024版      45页                    │ │
│  │                                                                  │ │
│  │   📁 FINANCE (财务管理)  1个数据源 · 8个实体                      │ │
│  │   └── 📂 MySQL-财务库 (JDBC)  最后同步: 06-26 09:00  · 8,200条    │ │
│  │       ├── 📄 account_record   会计凭证      5,000条               │ │
│  │       └── ...                                                    │ │
│  │                                                                  │ │
│  │   📁 RD (研发中心)  1个数据源 · 6个实体                           │ │
│  │   ...                                                            │ │
│  │                                                                  │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

**交互说明**：
- 使用 Element Plus `el-tree` 组件渲染三级目录树
- 默认展开所有域（一级节点），数据源默认折叠
- 点击实体节点：弹窗显示实体详细信息（字段列表、记录数、最后更新时间）
- 支持搜索过滤：输入关键词高亮匹配的域/数据源/实体

### 7.7 数据质量报告页（`QualityReportView.vue`）★ 新增

```
┌──────────────────────────────────────────────────────────────────────┐
│  [≡]  Agent-QR 企业知识库           [通知] 用户名 ▼ [退出]            │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  数据质量报告                                                         │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────────────┐│
│  │批次号      │数据源    │总数 │合格│不合格│合格率 │是否阻断│检查时间 ││
│  ├──────────────────────────────────────────────────────────────────┤│
│  │batch-001   │MySQL-财务│5000 │4800│200  │96.0% │✅通过  │06-26 09││
│  │batch-002   │Sales API │3200 │1500│1700 │46.9% │🚫阻断  │06-26 08││
│  │batch-003   │政策文件桶│245  │240 │5    │98.0% │✅通过  │06-25 18││
│  └──────────────────────────────────────────────────────────────────┘│
│                                                                       │
│  [ < ]  [ 1 ]  [ 2 ]  [ > ]   共 25 条                                │
│                                                                       │
│  —— ★ 点击行展开质检明细 ——                                           │
│  ┌──────────────────────────────────────────────────────────────────┐│
│  │ batch-002 质检明细 (Sales API)                                    ││
│  │                                                                   ││
│  │ 不合格明细:                                                        ││
│  │ ┌──────┬──────────────────────────────────────────────────────┐  ││
│  │ │ 规则  │ 说明                                                  │  ││
│  │ ├──────┼──────────────────────────────────────────────────────┤  ││
│  │ │ 完整性│ 第 45 条记录 "content" 字段为空                       │  ││
│  │ │ 编码  │ 第 128 条记录编码为 GBK，需转 UTF-8                   │  ││
│  │ │ 格式  │ 第 230 条记录日期格式不符合 yyyy-MM-dd                │  ││
│  │ │ 唯一性│ 第 310 条记录与第 89 条内容重复                        │  ││
│  │ │ ...   │ ...                                                   │  ││
│  │ └──────┴──────────────────────────────────────────────────────┘  ││
│  │                                                                   │
│  │ 按规则分布:                                                        ││
│  │ 完整性: ████████████ 120 (60%)                                    ││
│  │ 编码:   ██████ 45 (22.5%)                                        ││
│  │ 格式:   ██ 20 (10%)                                              ││
│  │ 唯一性: █ 15 (7.5%)                                              ││
│  └──────────────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────┘
```

**交互说明**：
- 列表支持按合格率排序筛选（如只看阻断批次的）
- 点击行展开质检详情（`el-table` 展开行）
- 使用进度条展示各类规则的不合格分布
- 红色高亮阻断批次的整行

### 7.8 ETL 管道监控页（★ 简化实现）

> 说明：ETL 模块在前端的主要可观测点是多源数据接入页中的同步状态。P2 阶段不单独创建 ETL 监控页面，而是将 ETL 处理状态集成到「数据源管理」和「数据质量报告」页面中。数据源管理页的同步按钮触发后，会展示从接入→质检→ETL→入库的管道状态流转。

---

## 八、P2 组件变更详情

### 8.1 ChatInput 升级（`ChatInput.vue`）△

```
┌────────────────────────────────────────────┐
│ [全部域 ▼] │ [请输入问题...]    │ [发送]   │
│            │                    │ [■ 停止] │ ← ★ 发送中显示停止按钮
└────────────────────────────────────────────┘
```

**新增 props / emits**：
```typescript
// ★ 新增
const props = defineProps<{
  loading: boolean
  disabled: boolean
  domains: { value: string; label: string }[]  // ★ 可选域列表
}>()

const emit = defineEmits<{
  send: [content: string, domain?: string]   // ★ 透传域参数
  stop: []                                     // ★ 停止生成
}>()

const selectedDomain = ref<string>('')         // ★ 当前选中的域（空=全部）

function handleSend() {
  if (!inputValue.value.trim() || props.loading) return
  emit('send', inputValue.value, selectedDomain.value || undefined)
  inputValue.value = ''
}
```

### 8.2 MessageBubble 升级（`MessageBubble.vue`）△

**新增特性**：
- ★ 流式光标动画：AI 消息在 `streaming` 状态下显示闪烁光标 `▊`
- ★ 反馈按钮：AI 消息完成后显示 👍/👎 按钮（仅非 loading 消息）
- ★ 引用来源：可点击跳转（如果前端有文档预览链接）

```typescript
defineProps<{
  role: 'user' | 'assistant'
  content: string
  sources?: SourceVO[]
  loading?: boolean
  streaming?: boolean       // ★ 是否正在流式接收
  feedback?: 'positive' | 'negative' | null  // ★ 已有的反馈状态
}>()

const emit = defineEmits<{
  feedback: [type: 'positive' | 'negative']  // ★
}>()
```

**样式变更**：
```scss
// ★ 流式光标动画
.message-bubble--streaming::after {
  content: '▊';
  animation: blink 1s step-end infinite;
}
@keyframes blink {
  50% { opacity: 0; }
}

// ★ 反馈按钮
.message-bubble__feedback {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #eee;

  .feedback-btn {
    font-size: 12px;
    padding: 2px 8px;
    border-radius: 4px;
    cursor: pointer;
    &--active { background: #ecf5ff; color: #409eff; }
  }
}
```

### 8.3 Sidebar 升级（`Sidebar.vue`）△ — ABAC 菜单动态可见性

```typescript
// ★ P2 ABAC 菜单逻辑
const userRole = computed(() => getUserRoleFromLocalStorage())
const userInfo = computed(() => getUserFromStorage() as UserPrincipal | null)

const menuItems = computed(() => {
  const items = [
    { path: '/chat', title: '问答', icon: 'ChatDotRound', meta: {} },
  ]

  // ★ admin 用户额外菜单
  if (userRole.value === 'admin') {
    items.push(
      { path: '/admin/knowledge', title: '知识库管理', icon: 'Document' },
      { path: '/admin/users', title: '用户管理', icon: 'User' },
      { path: '/admin/dashboard', title: '数据仪表盘', icon: 'DataAnalysis' },
      // ★ P2 新增菜单页
      { path: '/admin/datasource', title: '数据接入', icon: 'Connection' },
      { path: '/admin/catalog', title: '知识目录', icon: 'FolderOpened' },
      { path: '/admin/quality', title: '质量报告', icon: 'Warning' },
    )
  }

  // ★ 非 admin 用户如果有特定职级也可以访问部分页面
  // 例如：manager 和 director 职级可以看到知识目录
  if (userRole.value !== 'admin' && userInfo.value) {
    const { title } = userInfo.value
    if (title === 'manager' || title === 'director') {
      items.push(
        { path: '/admin/catalog', title: '知识目录', icon: 'FolderOpened' },
      )
    }
  }

  return items
})
```

### 8.4 HeaderBar 升级（`HeaderBar.vue`）△

**新增特性**：
- ★ Token 过期倒计时显示（Access Token 剩余有效时间）
- ★ 刷新 Token 时显示静默刷新提示（不影响用户操作）

### 8.5 StatusTag 升级（`StatusTag.vue`）△

新增 `DELETING` 状态样式：
```typescript
// ★ P2 新增状态
const STATUS_COLORS: Record<string, string> = {
  UPLOADED: 'info',
  PARSING: 'warning',
  CHUNKING: 'warning',
  EMBEDDING: 'warning',
  READY: 'success',
  FAILED: 'danger',
  DELETING: '', // ★ 灰色 + 加载动画
}
```

---

## 九、P2 Token 工具变更

```typescript
// utils/token.ts — P2 升级

const ACCESS_TOKEN_KEY = 'access_token'       // ★ 改名
const REFRESH_TOKEN_KEY = 'refresh_token'     // ★ 新增
const TOKEN_EXPIRES_KEY = 'token_expires_at'  // ★ 新增
const USER_KEY = 'auth_user'

// Access Token
export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}
export function setAccessToken(token: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, token)
}

// ★ Refresh Token
export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}
export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

// ★ Token 过期时间戳
export function setTokenExpiresAt(timestamp: number): void {
  localStorage.setItem(TOKEN_EXPIRES_KEY, String(timestamp))
}
export function getTokenExpiresAt(): number | null {
  const val = localStorage.getItem(TOKEN_EXPIRES_KEY)
  return val ? Number(val) : null
}

// ★ 清除所有 Token
export function removeAllTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(TOKEN_EXPIRES_KEY)
  localStorage.removeItem(USER_KEY)
}

// 兼容旧版（保留 getToken/setToken/removeToken 作为别名）
export const getToken = getAccessToken
export const setToken = setAccessToken
export const removeToken = () => localStorage.removeItem(ACCESS_TOKEN_KEY)

// user 存取不变...
```

---

## 十、P2 SSE 流式请求工具

```typescript
// utils/sse.ts — P2 新增
// 封装 @microsoft/fetch-event-source，提供统一的 SSE 请求能力

import { fetchEventSource, EventStreamContentType } from '@microsoft/fetch-event-source'
import { getAccessToken, getRefreshToken, setAccessToken, setRefreshToken, setTokenExpiresAt } from './token'

interface SSEOptions {
  url: string
  method?: 'GET' | 'POST'
  body?: any
  headers?: Record<string, string>
  signal?: AbortSignal
  onMessage: (event: string, data: string) => void
  onError?: (error: string) => void
  onClose?: () => void
}

/**
 * 发起 SSE 流式请求
 * 支持 POST + 自定义 Header（含 Bearer Token）
 * 返回 AbortController 供调用方取消
 */
export function createSSERequest(options: SSEOptions): AbortController {
  const controller = new AbortController()
  const mergedSignal = options.signal
    ? combineSignals(options.signal, controller.signal)
    : controller.signal

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.headers,
  }

  const token = getAccessToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  fetchEventSource(options.url, {
    method: options.method || 'POST',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
    signal: mergedSignal,
    async onopen(response) {
      if (response.ok && response.headers.get('content-type')?.includes(EventStreamContentType)) {
        return // 连接成功
      }
      throw new Error(`SSE 连接失败: HTTP ${response.status}`)
    },
    onmessage(event) {
      options.onMessage(event.event, event.data)
    },
    onerror(err) {
      options.onError?.(err.message)
      throw err // 不自动重连
    },
    onclose() {
      options.onClose?.()
    },
  })

  return controller
}

/** 合并多个 AbortSignal */
function combineSignals(...signals: AbortSignal[]): AbortSignal {
  const controller = new AbortController()
  signals.forEach((signal) => {
    if (signal.aborted) {
      controller.abort(signal.reason)
      return
    }
    signal.addEventListener('abort', () => controller.abort(signal.reason))
  })
  return controller.signal
}
```

---

## 十一、P2 组件树（全量 + 增量标注）

```
App.vue
├── GuestLayout.vue                      [路由: /login, /register]  (不变)
│   ├── LoginView.vue                    (不变)
│   └── RegisterView.vue                 (不变)
│
└── MainLayout.vue                       [路由: /chat, /admin/*]    (不变)
    ├── Sidebar.vue                      △ ABAC 菜单动态可见性
    ├── HeaderBar.vue                     △ Token 倒计时 + 静默刷新
    └── <router-view>
        ├── ChatView.vue                 △ [路由: /chat]
        │   ├── ConversationList.vue     (不变)
        │   ├── MessageBubble.vue (×N)   △ 流式渲染 + 反馈按钮
        │   └── ChatInput.vue             △ 域选择器 + 停止按钮
        │
        ├── KnowledgeView.vue            △ [路由: /admin/knowledge]
        │   ├── UploadDialog.vue          △ 域选择 + 密级选择
        │   ├── DocumentTable.vue         △ domain/sensitivityLevel 列
        │   │   └── StatusTag.vue (×N)    △ DELETING 状态
        │   └── Pagination.vue            (不变)
        │
        ├── UserManageView.vue           △ [路由: /admin/users]
        │   ├── UserFormDialog.vue        △ ABAC 字段表单
        │   ├── UserTable.vue             △ ABAC 字段列
        │   └── Pagination.vue            (不变)
        │
        ├── DashboardView.vue            △ [路由: /admin/dashboard]
        │   └── ECharts (×3)              △ 新增满意度折线 + 反馈饼图
        │
        ├── DataSourceView.vue           ★ [路由: /admin/datasource]
        │   ├── DataSourceFormDialog.vue  ★ 连接配置动态表单
        │   ├── DataSourceTable.vue       ★
        │   │   └── SyncStatusTag.vue     ★
        │   └── Pagination.vue
        │
        ├── CatalogView.vue              ★ [路由: /admin/catalog]
        │   └── CatalogTree.vue           ★ el-tree 三级目录
        │
        └── QualityReportView.vue        ★ [路由: /admin/quality]
            └── el-table (展开行详情)      ★
```

---

## 十二、P2 新增样式变更

```scss
// styles/global.scss — P2 新增样式

// ★ 流式光标闪烁动画
@keyframes blink-cursor {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.streaming-cursor::after {
  content: '▊';
  display: inline;
  animation: blink-cursor 1s step-end infinite;
  color: #409eff;
  font-weight: bold;
}

// ★ 反馈按钮组
.feedback-btn-group {
  display: inline-flex;
  gap: 6px;
  margin-top: 12px;

  .feedback-btn {
    font-size: 12px;
    padding: 4px 12px;
    border: 1px solid #dcdfe6;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.2s;
    background: #fff;
    color: #606266;

    &:hover {
      border-color: #409eff;
      color: #409eff;
    }

    &--active {
      background: #ecf5ff;
      border-color: #409eff;
      color: #409eff;
    }
  }
}

// ★ 密级标签颜色
.sensitivity-tag {
  &--public  { color: #67c23a; }  // 公开 - 绿色
  &--internal { color: #409eff; } // 内部 - 蓝色
  &--confidential { color: #e6a23c; } // 机密 - 橙色
  &--topsecret { color: #f56c6c; }    // 绝密 - 红色
}

// ★ 域标签
.domain-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  background: #ecf5ff;
  color: #409eff;
}

// ★ 停止生成按钮
.btn-stop-generate {
  background: #f56c6c;
  border-color: #f56c6c;
  color: #fff;
  &:hover {
    background: #f78989;
  }
}
```

---

## 十三、P2 环境变量变更

```bash
# .env.development — P2 新增配置

VITE_API_BASE_URL=http://localhost:8080

# ★ SSE 流式超时（毫秒，默认 5 分钟）
VITE_SSE_TIMEOUT=300000

# ★ Token 提前刷新时间（毫秒，默认提前 60 秒刷新）
VITE_TOKEN_REFRESH_AHEAD=60000

# ★ 最大重连次数（SSE 断线重连）
VITE_SSE_MAX_RECONNECT=3
```

---

## 十四、P2 与 P1 的差异总结

| 维度 | P1 实现 | P2 升级 |
|------|---------|---------|
| **问答模式** | 同步：发送→加载动画→一次性返回 | ★ SSE 流式：逐 token 打字机效果 + 停止生成 |
| **权限模型** | 简单角色判断（user/admin） | ★ ABAC 属性权限（department + clearanceLevel + allowedDomains + title） |
| **Token 管理** | 单一 Access Token（24h），过期跳登录 | ★ 双 Token（Access 30min + Refresh 7day），静默刷新 |
| **路由守卫** | `requiresAdmin` 角色检查 | ★ 增加 `requiresDomain` / `requiresTitle` ABAC 检查 |
| **用户管理** | 基础字段 + role + status | ★ 新增 department / clearanceLevel / allowedDomains / title 字段 |
| **文档上传** | 文件 + 标题 | ★ 增加 domain（业务域）+ sensitivityLevel（密级）选择 |
| **文档列表** | 基础列 | ★ 新增「业务域」「密级」列 + DELETING 状态 |
| **文档删除** | 同步物理删除 | ★ 事件驱动软删除（标记 DELETING → 后台异步清理） |
| **仪表盘** | 4 卡片 + 折线图 + 饼图 | ★ 7 卡片（增加满意度）+ 反馈分布饼图 |
| **数据接入** | — | ★ 多源数据接入管理页面（JDBC/REST/S3 CRUD + 连通性测试 + 同步） |
| **知识目录** | — | ★ 三级目录树（业务域→数据源→数据实体）浏览页面 |
| **质量报告** | — | ★ 数据质量报告列表 + 展开明细页面 |
| **AI 反馈** | — | ★ 答案点赞/点踩评价 + 原因反馈 |
| **域选择器** | — | ★ 问答输入框左侧业务域下拉选择 |
| **全链路追踪** | — | ★ 前端自动生成 TraceId 透传给后端 |
| **侧边栏菜单** | admin 角色固定菜单 | ★ 支持 manager/director 职级看到部分高级页面 |

---

## 十五、P2 约束与 P3 展望

| 功能 | P2 处理 | P3 增强 |
|------|---------|---------|
| 问答模式 | SSE 流式（POST 方式） | WebSocket 双向通信 + 语音输入 |
| 权限联动 | ABAC 属性前端校验（页面/菜单级） | ABAC 细粒度按钮/字段级控制 |
| 数据接入 | JDBC/REST/S3 手动配置 | 智能发现 + 自动建表 + CDC 实时同步 |
| 域路由 | 用户手动选择业务域 | Embedding 语义自动路由 |
| 知识目录 | 三级树静态浏览 | 可交互钻取 + 知识图谱可视化 |
| 质量报告 | 事后质检报告查看 | 实时质检规则配置 + 自定义规则引擎 |
| 国际化 | 硬编码中文 | i18n 多语言 |
| 响应式 | 桌面端优先 | 移动端适配 |
| ECharts | 新增满意度图表 | 可交互钻取图表 + 自定义时间范围 |
| API 文档 | 手动维护 api/ 目录 | 自动生成 TypeScript 类型（从 OpenAPI） |

---

> **文档版本**：v1.0
>
> **编写日期**：2026-06-26
>
> **依据文档**：
> - 系统详细设计说明书 v1.0（`doc/系统详细设计说明书.md`）
> - P1 前端详细设计说明书（`doc/p1-frontend-design.md`）
> - P1 前端源代码（`agent-qr-web-frontend/src/`）
