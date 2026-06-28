# agent-qr-web-frontend -- P3 任务清单

> 前端 P3 增强：WebSocket 双向通信 + i18n 国际化 + 移动端适配 + ABAC 细粒度权限 + 知识图谱可视化
> 更新日期：2026-06-28
> 阶段目标：全面提升用户体验，从可用到好用

---

## 1. WebSocket 双向通信

- [ ] **1.1** 安装 WebSocket 客户端依赖
  - 安装 `sockjs-client` 和 `@stomp/stompjs`（STOMP over WebSocket）
  - 或使用原生 WebSocket + 自定义封装
  - 命令：`pnpm add sockjs-client @stomp/stompjs`

- [ ] **1.2** 创建 `useWebSocket` Composable
  - 路径：`src/composables/useWebSocket.ts`
  - 功能：连接管理、自动重连、心跳检测、消息队列
  - 导出：`connect()`、`disconnect()`、`send()`、`onMessage()`、`connectionState`

- [ ] **1.3** 改造聊天页 — SSE 升级为 WebSocket
  - 路径：`src/views/chat/index.vue`
  - 改造点：
    1. 使用 `useWebSocket` 替代 `fetchEventSource`（SSE）
    2. 支持双向消息（客户端可中途取消生成）
    3. 保持流式输出体验（逐 token 渲染）
    4. 降级方案：WebSocket 不可用时回退到 SSE
  - 保留对话历史、满意度反馈等 P2 功能

- [ ] **1.4** 语音输入（可选增强）
  - 使用浏览器 `Web Speech API`（`SpeechRecognition`）
  - 创建 `useSpeechRecognition` Composable
  - 输入框旁添加麦克风按钮
  - 支持中英文语音识别

---

## 2. i18n 国际化

- [ ] **2.1** 安装国际化依赖
  - 安装 `vue-i18n`（Vue 3 官方国际化方案）
  - 命令：`pnpm add vue-i18n@9`

- [ ] **2.2** 创建 i18n 基础设施
  - 路径：`src/i18n/index.ts` — VueI18n 实例创建与配置
  - 路径：`src/i18n/locales/zh-CN.ts` — 中文语言包
  - 路径：`src/i18n/locales/en-US.ts` — 英文语言包
  - 支持语言：中文（默认）、英文

- [ ] **2.3** 提取硬编码中文文本
  - 逐页面替换硬编码中文为 `$t('key')` 调用：
    - `src/views/auth/` — 登录/注册页面
    - `src/views/chat/` — 聊天页面
    - `src/views/knowledge/` — 知识库管理
    - `src/views/user/` — 用户管理
    - `src/views/dashboard/` — 仪表盘
    - `src/views/datasource/` — 数据源管理
    - `src/views/catalog/` — 知识目录
    - `src/views/quality/` — 数据质量
    - `src/components/layout/` — 布局组件（Sidebar、Header 等）
    - `src/components/common/` — 公共组件

- [ ] **2.4** 语言切换 UI
  - 在 Header 或 Sidebar 底部添加语言切换下拉框
  - 语言偏好持久化到 `localStorage`
  - 切换时刷新页面或动态切换（`vue-i18n` 支持）

---

## 3. 移动端响应式适配

- [ ] **3.1** 引入响应式断点系统
  - 定义断点：`mobile (< 768px)`、`tablet (768-1024px)`、`desktop (> 1024px)`
  - 创建 `useBreakpoint` Composable：`src/composables/useBreakpoint.ts`
  - 使用 CSS `@media` 查询 + JS `window.matchMedia`

- [ ] **3.2** 改造布局组件
  - `Sidebar.vue`：移动端改为抽屉式（hamburger 菜单 + overlay）
  - `Header.vue`：移动端简化，折叠次要操作
  - 主内容区：移动端全宽，去除多余 padding

- [ ] **3.3** 改造核心页面移动端适配
  - 聊天页：移动端优化输入框高度、消息气泡宽度
  - 登录页：移动端居中卡片、简化注册表单
  - 知识库：表格改为卡片列表
  - 仪表盘：图表单列堆叠
  - 数据源管理：表单全宽

- [ ] **3.4** 移动端触摸优化
  - 增大可点击区域（最小 44x44px）
  - 长按菜单支持
  - 下拉刷新手势

---

## 4. ABAC 细粒度按钮/字段级权限

- [ ] **4.1** 扩展 `useAuthStore` — 新增权限计算属性
  - 路径：`src/stores/auth.ts`
  - 新增计算属性：
    - `canEditKnowledge`：知识库编辑权限
    - `canDeleteKnowledge`：知识库删除权限
    - `canConfigureDatasource`：数据源配置权限
    - `canExportReport`：报表导出权限
    - `fieldLevel.salary`：薪资字段可见性
    - `fieldLevel.performance`：绩效字段可见性
  - 基于 `user.title`、`user.clearance`、`user.department` 计算

- [ ] **4.2** 创建 `v-permission` 自定义指令
  - 路径：`src/directives/permission.ts`
  - 用法：`<button v-permission="'canEditKnowledge'">编辑</button>`
  - 无权限时自动隐藏或禁用元素

- [ ] **4.3** 逐页添加权限控制
  - 知识库页面：编辑/删除按钮受 `canEditKnowledge`/`canDeleteKnowledge` 控制
  - 数据源页面：配置按钮受 `canConfigureDatasource` 控制
  - 用户管理页面：敏感字段（薪资/绩效）受 `fieldLevel.*` 控制
  - 仪表盘：导出按钮受 `canExportReport` 控制

---

## 5. 知识图谱可视化

- [ ] **5.1** 安装图谱可视化依赖
  - 推荐：`vis-network`（轻量）或 `echarts`（已在项目中）
  - 使用 ECharts 的 `graph` 类型图表（无需新增依赖）
  - 或安装 `d3-force` 实现力导向图

- [ ] **5.2** 创建知识图谱组件
  - 路径：`src/components/charts/KnowledgeGraph.vue`
  - 数据模型：节点（域/数据源/实体）+ 边（包含/关联/引用）
  - 交互：节点拖拽、缩放、点击展开/折叠、搜索高亮
  - 力导向布局，自动聚类

- [ ] **5.3** 改造知识目录页
  - 路径：`src/views/catalog/index.vue`
  - 新增"图谱视图"Tab（与"树形视图"并列）
  - 嵌入 `KnowledgeGraph` 组件
  - 支持从目录树 API 数据转换为图谱数据

---

## 6. 实时质检规则配置 UI

- [ ] **6.1** 创建质量规则管理页面
  - 路径：`src/views/quality/RulesManager.vue`
  - 功能：
    - 规则列表（CRUD）
    - 规则类型：完整性、唯一性、格式、编码、长度
    - 规则参数配置（阈值、正则表达式等）
    - 规则启用/禁用开关

- [ ] **6.2** 创建规则编辑器组件
  - 路径：`src/components/quality/RuleEditor.vue`
  - 可视化规则配置表单
  - 实时预览（输入样例数据，查看规则匹配结果）

- [ ] **6.3** 扩展质量报告页
  - 路径：`src/views/quality/index.vue`
  - 新增：实时质检状态面板（WebSocket 推送质检进度）
  - 新增：自定义时间范围筛选

---

## 7. ECharts 图表增强

- [ ] **7.1** 仪表盘图表交互增强
  - 路径：`src/views/dashboard/index.vue`
  - 改造：满意度趋势图支持时间范围拖拽选择
  - 改造：热门问答 Top N 支持点击下钻查看详情
  - 新增：数据源同步状态实时面板

- [ ] **7.2** 统计图表导出
  - 图表导出为 PNG/PDF
  - 使用 ECharts 的 `getDataURL()` + `html2canvas`

---

> **依赖关系**：
> - WebSocket → 后端需同步支持 WebSocket 端点（agent-qr-web）
> - 知识图谱 → 后端 `GET /api/catalog/tree` 已有数据
> - 规则配置 → 后端 `agent-qr-data-quality` 需提供规则 CRUD API（如暂无，前端先做 UI 框架）
> - ABAC 细粒度 → 后端 `agent-qr-auth` 需提供字段级权限元数据 API
>
> **说明**：前端 P3 任务较后端更为庞大，建议按优先级分批实施：
> - **第一批（核心）**：WebSocket 通信 + i18n 国际化 + ABAC 细粒度权限
> - **第二批（体验）**：移动端适配 + ECharts 增强
> - **第三批（增值）**：知识图谱 + 实时质检规则 + 语音输入
>
> **统计**：共 7 大类，约 20+ 个子任务
> 预计耗时：5-7 天
