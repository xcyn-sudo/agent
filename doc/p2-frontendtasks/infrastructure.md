# 基础设施与公共组件 - P2 前端任务列表

> 对应设计文档：p2-frontend-design.md 第二~六章、第八~十二章
> 模块类型：P1 升级 + P2 新增
> 状态：未开始

---

## 1. 依赖与环境配置

- [ ] 安装 `@microsoft/fetch-event-source` 依赖（`npm install @microsoft/fetch-event-source`）
- [ ] `.env.development` 新增 `VITE_SSE_TIMEOUT=300000`
- [ ] `.env.development` 新增 `VITE_TOKEN_REFRESH_AHEAD=60000`
- [ ] `.env.development` 新增 `VITE_SSE_MAX_RECONNECT=3`

---

## 2. 类型定义（`types/index.ts`）

### 2.1 已有类型扩展（△）

- [ ] `UserInfo` 接口新增 `department: string`
- [ ] `UserInfo` 接口新增 `clearanceLevel: number`
- [ ] `UserInfo` 接口新增 `allowedDomains: string`（后端返回逗号分隔，前端解析为数组）
- [ ] `UserInfo` 接口新增 `title: string`
- [ ] `DocumentInfo` 接口新增 `domain: string`
- [ ] `DocumentInfo` 接口新增 `sensitivityLevel: number`
- [ ] `DocumentInfo` 接口新增 `sensitivityLabel: string`
- [ ] `DashboardVO` 接口新增 `todayPositiveCount: number`
- [ ] `DashboardVO` 接口新增 `todayNegativeCount: number`
- [ ] `DashboardVO` 接口新增 `satisfactionRate: number`

### 2.2 SSE 流式相关类型（★）

- [ ] 新增 `SSETokenEvent` 接口（`event: 'token', data: string`）
- [ ] 新增 `SSEDoneEvent` 接口（`event: 'done', data: { conversationId, sources }`）
- [ ] 新增 `SSEErrorEvent` 接口（`event: 'error', data: string`）
- [ ] 新增 `SourceVO` 接口（`documentId, documentTitle, content, similarity`）
- [ ] 新增 `FeedbackDTO` 接口（`messageId, feedback: 'positive'|'negative', reason?`）

### 2.3 数据源管理相关类型（★）

- [ ] 新增 `DataSourceConfig` 接口（id, sourceName, sourceType, domain, status, syncStrategy, cronExpression, lastSyncAt, totalSynced, connectionConfig, createTime, updateTime）
- [ ] 新增 `DataSourceForm` 接口（sourceName, sourceType, domain, syncStrategy, cronExpression?, connectionConfig）
- [ ] 新增 `ConnectionTestResult` 接口（success, latencyMs, dbProduct?, dbVersion?, errorMsg?）
- [ ] 新增 `SyncRecord` 接口（id, datasourceId, syncType, status, totalRows, errorMsg?, syncAt）

### 2.4 知识目录相关类型（★）

- [ ] 新增 `CatalogTree` 接口（domains: DomainNode[]）
- [ ] 新增 `DomainNode` 接口（domainName, sourceCount, totalEntities, sources: SourceNode[]）
- [ ] 新增 `SourceNode` 接口（sourceId, sourceName, sourceType, lastSyncAt, totalSynced, entities: EntityNode[]）
- [ ] 新增 `EntityNode` 接口（entityName, recordCount, lastUpdated）

### 2.5 数据质量相关类型（★）

- [ ] 新增 `QualityReport` 接口（batchId, datasourceId, sourceName, totalCount, passCount, failCount, passRate, blocked, failures: QualityFailure[], checkTime）
- [ ] 新增 `QualityFailure` 接口（ruleName, recordIndex, reason）

### 2.6 枚举常量（★）

- [ ] 新增 `DOMAINS` 常量（'HR' | 'FINANCE' | 'RD' | 'SALES' | 'COMMON'）
- [ ] 新增 `SENSITIVITY_LEVELS` 常量（{ value, label } 公开/内部/机密/绝密）
- [ ] 新增 `TITLES` 常量（'employee' | 'manager' | 'director'）
- [ ] 新增 `DEPARTMENTS` 常量（{ value, label } 人力资源/财务管理/研发中心/销售管理/公共部门）
- [ ] 新增 `SOURCE_TYPES` 常量（'JDBC' | 'REST' | 'S3'）
- [ ] 新增 `LoginVO` 接口（双Token登录响应：accessToken, refreshToken, expiresIn, userId, username, role, department, clearanceLevel, allowedDomains, title）

---

## 3. Token 工具（`utils/token.ts`）

- [ ] 常量 `ACCESS_TOKEN_KEY` 改为 `'access_token'`
- [ ] 新增常量 `REFRESH_TOKEN_KEY = 'refresh_token'`
- [ ] 新增常量 `TOKEN_EXPIRES_KEY = 'token_expires_at'`
- [ ] 实现 `getAccessToken(): string | null`
- [ ] 实现 `setAccessToken(token: string): void`
- [ ] 实现 `getRefreshToken(): string | null`
- [ ] 实现 `setRefreshToken(token: string): void`
- [ ] 实现 `getTokenExpiresAt(): number | null`
- [ ] 实现 `setTokenExpiresAt(timestamp: number): void`
- [ ] 实现 `removeAllTokens(): void`（清除 access + refresh + expires + user）
- [ ] 保留旧版兼容别名：`getToken → getAccessToken`, `setToken → setAccessToken`, `removeToken → remove(ACCESS_TOKEN_KEY)`

---

## 4. SSE 流式请求工具（`utils/sse.ts`）★

- [ ] 定义 `SSEOptions` 接口（url, method?, body?, headers?, signal?, onMessage, onError?, onClose?）
- [ ] 实现 `createSSERequest(options: SSEOptions): AbortController`
- [ ] 内部创建 `AbortController` 并合并外部 signal
- [ ] 自动注入 `Authorization: Bearer <token>` 请求头
- [ ] 设置 `Content-Type: application/json` 请求头
- [ ] 使用 `fetchEventSource` 发起请求（POST 方式）
- [ ] 实现 `onopen` 回调：校验 Content-Type 为 `text/event-stream`
- [ ] 实现 `onmessage` 回调：透传 event 和 data 到 `options.onMessage`
- [ ] 实现 `onerror` 回调：调用 `options.onError` + throw（不自动重连）
- [ ] 实现 `onclose` 回调：调用 `options.onClose`
- [ ] 返回 `AbortController` 实例（供调用方取消）
- [ ] 实现 `combineSignals(...signals: AbortSignal[]): AbortSignal` 辅助函数

---

## 5. 格式化工具（`utils/format.ts`）

- [ ] 新增 `formatSensitivityLevel(level: number): string`（0→公开, 1→内部, 2→机密, 3→绝密）
- [ ] 新增 `formatDomain(domain: string): string`（转中文标签或原值返回）
- [ ] 新增 `formatSourceType(type: string): string`（JDBC→数据库, REST→REST API, S3→文件系统）
- [ ] 新增 `formatSyncStatus(status: string): string`（ACTIVE→活跃, INACTIVE→停用, ERROR→异常）
- [ ] 新增 `formatPassRate(rate: number): string`（小数→百分比字符串，如 0.96 → '96.0%'）
- [ ] 新增 `parseAllowedDomains(raw: string): string[]`（逗号分隔字符串→数组）

---

## 6. Axios 实例升级（`api/index.ts`）

- [ ] `timeout` 从默认值延长至 `30000`（30s，支持大文件上传）
- [ ] 新增 `isRefreshing` 状态变量（标记是否正在刷新 Token）
- [ ] 新增 `pendingRequests` 队列（等待刷新期间暂存的请求）
- [ ] 实现 `processQueue(error, token?)` 函数（刷新完成后处理等待队列）
- [ ] 请求拦截器新增 `X-Trace-Id` 请求头自动注入
- [ ] 实现 `generateTraceId(): string` 函数（16位 hex，使用 crypto.getRandomValues）
- [ ] 响应拦截器 `code === 401` 改为调用 `handleTokenRefresh` 而非直接跳登录
- [ ] 实现 `handleTokenRefresh(failedConfig): Promise<any>` 函数
- [ ] `handleTokenRefresh` 中：如果正在刷新→加入等待队列返回 Promise
- [ ] `handleTokenRefresh` 中：调用 `/api/auth/refresh` 获取新 Token
- [ ] `handleTokenRefresh` 中：更新 Token 并 `processQueue(null, newToken)`
- [ ] `handleTokenRefresh` 中：刷新失败 → `processQueue(error)` → `removeAllTokens()` → 跳 `/login`
- [ ] 响应拦截器新增 SSE 流式请求（`responseType === 'stream'`）错误特殊处理

---

## 7. 认证 API（`api/auth.ts`）

- [ ] `login` 函数响应类型更新为 `LoginVO`（双Token + ABAC字段）
- [ ] 新增 `refreshToken(refreshToken: string)` 函数（`POST /api/auth/refresh`）
- [ ] 新增 `revokeToken()` 函数（`POST /api/auth/revoke`）
- [ ] `getUserInfo` 函数响应类型新增 ABAC 字段

---

## 8. 问答 API（`api/chat.ts`）

- [ ] 导入 `fetchEventSource` from `@microsoft/fetch-event-source`
- [ ] 导入 `getAccessToken` from `@/utils/token`
- [ ] 新增 `askStream(query, domain, conversationId, callbacks): AbortController` 函数
- [ ] `askStream` 内部创建 `AbortController` 实例
- [ ] `askStream` 使用 `fetchEventSource` 发起 POST SSE 请求到 `/api/chat/ask`
- [ ] `askStream` 请求体携带 `{ query, conversationId, domain }`
- [ ] `askStream` 请求头携带 `Authorization: Bearer <token>`
- [ ] `askStream` 处理 `onmessage`：event='token' → `callbacks.onToken(data)`
- [ ] `askStream` 处理 `onmessage`：event='done' → `callbacks.onDone(JSON.parse(data))`
- [ ] `askStream` 处理 `onmessage`：event='error' → `callbacks.onError(data)`
- [ ] `askStream` 处理 `onerror`：调用 `callbacks.onError('连接异常，请重试')` + throw（停止重连）
- [ ] `askStream` 返回 `AbortController`（供调用方 `abort()` 停止生成）
- [ ] 新增 `submitFeedback(messageId, feedback, reason?)` 函数（`POST /api/chat/messages/{id}/feedback`）

---

## 9. 知识库 API（`api/knowledge.ts`）

- [ ] `upload` 函数新增 `domain?: string` 参数
- [ ] `upload` 函数新增 `sensitivityLevel?: number` 参数
- [ ] `upload` 函数 FormData 新增 `domain` 字段（条件追加）
- [ ] `upload` 函数 FormData 新增 `sensitivityLevel` 字段（条件追加，转为字符串）

---

## 10. 统计 API（`api/statistics.ts`）

- [ ] `getDashboard` 函数响应类型更新为包含满意度字段的新 `DashboardVO`
- [ ] 确保 `DashboardVO` 类型包含 `todayPositiveCount`, `todayNegativeCount`, `satisfactionRate`

---

## 11. 数据源 API（`api/datasource.ts`）★

- [ ] 新建文件 `api/datasource.ts`
- [ ] 实现 `list(params: { page, size })` → `GET /api/datasource/list`
- [ ] 实现 `getById(id: number)` → `GET /api/datasource/{id}`
- [ ] 实现 `create(data: DataSourceForm)` → `POST /api/datasource`
- [ ] 实现 `update(id: number, data: Partial<DataSourceForm>)` → `PUT /api/datasource/{id}`
- [ ] 实现 `delete(id: number)` → `DELETE /api/datasource/{id}`
- [ ] 实现 `testConnection(id: number)` → `POST /api/datasource/{id}/test`
- [ ] 实现 `triggerSync(id: number)` → `POST /api/datasource/{id}/sync`
- [ ] 实现 `getSyncHistory(id: number, params: { page, size })` → `GET /api/datasource/{id}/sync-history`

---

## 12. 知识目录 API（`api/catalog.ts`）★

- [ ] 新建文件 `api/catalog.ts`
- [ ] 实现 `getCatalogTree()` → `GET /api/catalog/tree`
- [ ] 实现 `getDomainStats()` → `GET /api/catalog/stats`

---

## 13. 数据质量 API（`api/dataquality.ts`）★

- [ ] 新建文件 `api/dataquality.ts`
- [ ] 实现 `listReports(params: { page, size })` → `GET /api/dataquality/reports`
- [ ] 实现 `getReport(batchId: string)` → `GET /api/dataquality/reports/{batchId}`

---

## 14. 路由配置升级（`router/index.ts`）

- [ ] 新增路由记录 `/admin/datasource` → `DataSourceView.vue`，meta: `{ requiresAdmin: true, layout: 'main', title: '数据接入' }`
- [ ] 新增路由记录 `/admin/catalog` → `CatalogView.vue`，meta: `{ requiresAdmin: true, layout: 'main', title: '知识目录' }`
- [ ] 新增路由记录 `/admin/quality` → `QualityReportView.vue`，meta: `{ requiresAdmin: true, layout: 'main', title: '质量报告' }`
- [ ] 路由 Meta 类型新增 `requiresDomain?: string` 字段
- [ ] 路由 Meta 类型新增 `requiresTitle?: string[]` 字段
- [ ] 路由守卫从 `stores/auth` 导入 `UserPrincipal` 类型
- [ ] 路由守卫新增 ABAC 域检查：`to.meta.requiresDomain` → 检查 `user.allowedDomains?.includes(requiredDomain)`
- [ ] 路由守卫新增 ABAC 职级检查：`to.meta.requiresTitle` → 检查 `allowedTitles.includes(user.title)`
- [ ] 路由守卫不通过 ABAC 检查时重定向到 `/403`
- [ ] 路由守卫兼容 Pinia store 未初始化情况（try/catch + fallback 从 localStorage 读取）

---

## 15. Auth Store 升级（`stores/auth.ts`）

- [ ] `UserPrincipal` 接口新增 `department: string`
- [ ] `UserPrincipal` 接口新增 `clearanceLevel: number`
- [ ] `UserPrincipal` 接口新增 `allowedDomains: string[]`
- [ ] `UserPrincipal` 接口新增 `title: string`
- [ ] `AuthState` 接口：`token` 改名为 `accessToken`
- [ ] `AuthState` 接口新增 `refreshToken: string | null`
- [ ] `AuthState` 接口新增 `tokenExpiresAt: number | null`
- [ ] `login` action：调用 API 后保存 `accessToken`, `refreshToken`, `tokenExpiresAt`, `user`（含ABAC属性）
- [ ] `login` action：将 `allowedDomains` 字符串按逗号分割为数组再保存
- [ ] 新增 `refreshAccessToken` action：调用 `authApi.refreshToken(refreshToken)` → 更新双Token + expiresAt
- [ ] `logout` action：调用 `authApi.revokeToken()`（catch 忽略错误）→ 调用 `removeAllTokens()` → 跳转 `/login`
- [ ] 新增 `hasDomain(domain: string): boolean` getter
- [ ] 新增 `hasClearance(level: number): boolean` getter
- [ ] 新增 `isManager(): boolean` getter（`user.title === 'manager'`）
- [ ] 新增 `isDirector(): boolean` getter（`user.title === 'director'`）
- [ ] `fetchUserInfo` action：更新 user 时携带 ABAC 字段并解析 `allowedDomains`

---

## 16. 全局样式（`styles/global.scss`）

- [ ] 新增 `@keyframes blink-cursor` 动画（0%/100% opacity:1, 50% opacity:0）
- [ ] 新增 `.streaming-cursor::after` 样式（content: '▊', animation: blink-cursor 1s step-end infinite, color: #409eff）
- [ ] 新增 `.feedback-btn-group` 样式（inline-flex, gap: 6px, margin-top: 12px）
- [ ] 新增 `.feedback-btn` 基础样式（font-size: 12px, padding: 4px 12px, border, border-radius: 12px, cursor: pointer, transition）
- [ ] 新增 `.feedback-btn:hover` 样式（border-color + color: #409eff）
- [ ] 新增 `.feedback-btn--active` 样式（background: #ecf5ff, border-color + color: #409eff）
- [ ] 新增 `.sensitivity-tag--public` 样式（color: #67c23a 绿色）
- [ ] 新增 `.sensitivity-tag--internal` 样式（color: #409eff 蓝色）
- [ ] 新增 `.sensitivity-tag--confidential` 样式（color: #e6a23c 橙色）
- [ ] 新增 `.sensitivity-tag--topsecret` 样式（color: #f56c6c 红色）
- [ ] 新增 `.domain-tag` 样式（inline-block, padding, border-radius, background: #ecf5ff, color: #409eff, font-size: 12px）
- [ ] 新增 `.btn-stop-generate` 样式（background: #f56c6c, border-color, color: #fff）
- [ ] 新增 `.btn-stop-generate:hover` 样式（background: #f78989）

---

## 17. Sidebar 菜单升级（`components/layout/Sidebar.vue`）

- [ ] 从 localStorage 读取 `UserPrincipal` 信息（`getUserFromStorage`）
- [ ] `menuItems` computed 中新增 P2 admin 菜单项：`{ path: '/admin/datasource', title: '数据接入', icon: 'Connection' }`
- [ ] `menuItems` computed 中新增 P2 admin 菜单项：`{ path: '/admin/catalog', title: '知识目录', icon: 'FolderOpened' }`
- [ ] `menuItems` computed 中新增 P2 admin 菜单项：`{ path: '/admin/quality', title: '质量报告', icon: 'Warning' }`
- [ ] 实现非 admin 用户职级菜单逻辑：`manager` 或 `director` 可看到「知识目录」菜单项
- [ ] 新增 Element Plus 图标导入：`Connection`, `FolderOpened`, `Warning`

---

## 18. HeaderBar 升级（`components/layout/HeaderBar.vue`）

- [ ] 新增 Token 过期倒计时显示（从 `getTokenExpiresAt()` 计算剩余秒数）
- [ ] 剩余时间 ≤ 60s 时显示警告样式（红色文字）
- [ ] 实现倒计时定时器（`setInterval` 每 10s 更新一次）
- [ ] 新增静默刷新进行中提示（`isRefreshing` 状态下显示「正在刷新凭证...」）
- [ ] 组件卸载时清除定时器（`onUnmounted`）
- [ ] 刷新完成后自动更新倒计时起始值

---

> **统计**：共 18 大类，约 123 个子任务
