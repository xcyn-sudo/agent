# P2 阶段前端代码生成 Prompt

> **角色**：你是 P2 前端工程的主 Agent（Orchestrator），负责统筹协调整个 P2 阶段前端代码的实现。
>
> **目标**：在现有 P1 前端代码基础上，完成 P2 阶段全部 8 个模块的前端代码开发。
>
> **工作方式**：你通过生成子 Agent 来执行各模块的具体编码任务，你自身负责进度跟踪、依赖管理和集成验证。

---

## 一、项目背景

### 1.1 项目概述

本项目是一个**基于 LangChain 的 RAG 企业内部知识库问答 Agent 系统**，P2 阶段在 P1 基础上进行以下核心升级：

| 维度 | P1 实现 | P2 升级 |
|------|---------|---------|
| 问答模式 | 同步：发送→加载→一次性返回 | **SSE 流式**：逐 token 打字机效果 + 停止生成 |
| 权限模型 | 简单角色判断（user/admin） | **ABAC 属性权限**（department + clearanceLevel + allowedDomains + title） |
| Token 管理 | 单一 Access Token（24h） | **双 Token**（Access 30min + Refresh 7day），静默刷新 |
| 用户管理 | 基础字段 | **新增** department / clearanceLevel / allowedDomains / title |
| 文档管理 | 文件 + 标题 | **新增** domain（业务域）+ sensitivityLevel（密级） |
| 仪表盘 | 4 卡片 + 基础图表 | **7 卡片**（+满意度）+ 反馈分布饼图 |
| 数据接入 | — | **★ 新增**：多源数据接入管理（JDBC/REST/S3） |
| 知识目录 | — | **★ 新增**：三级目录树浏览 |
| 质量报告 | — | **★ 新增**：数据质量报告列表 + 展开明细 |
| AI 反馈 | — | **★ 新增**：答案点赞/点踩评价 |

### 1.2 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4+ | Composition API + `<script setup>` |
| TypeScript | 5.x | 严格模式 |
| Vite | 5.x | 构建工具 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由管理 |
| Element Plus | 2.x | UI 组件库 |
| ECharts | 5.x | 图表 |
| Axios | 1.x | HTTP 请求 |
| **@microsoft/fetch-event-source** | **新增** | SSE 流式请求（支持 POST + 自定义头） |

### 1.3 关键参考文档

> **重要**：以下文档包含全部实现细节，子 Agent 实现时必须严格参照。

| 文档 | 路径 | 内容 |
|------|------|------|
| P2 前端详细设计 | `doc/p2-frontend-design.md` | 全部设计细节、API 定义、组件结构、代码示例 |
| 基础设施任务 | `doc/p2-frontendtasks/infrastructure.md` | 类型/Token/SSE/Axios/路由/Store/样式 共 123 子任务 |
| 问答模块任务 | `doc/p2-frontendtasks/chat.md` | ChatInput/MessageBubble/ChatView SSE 流式升级 共 58 子任务 |
| 知识库模块任务 | `doc/p2-frontendtasks/knowledge.md` | UploadDialog/DocumentTable/StatusTag 升级 共 32 子任务 |
| 用户管理任务 | `doc/p2-frontendtasks/user.md` | UserFormDialog/UserTable ABAC 字段扩展 共 28 子任务 |
| 仪表盘任务 | `doc/p2-frontendtasks/dashboard.md` | 满意度卡片 + ECharts 升级 共 25 子任务 |
| 数据源管理任务 | `doc/p2-frontendtasks/datasource.md` | 全新模块 CRUD + 连通性测试 共 58 子任务 |
| 知识目录任务 | `doc/p2-frontendtasks/catalog.md` | 全新模块 el-tree 三级目录 共 27 子任务 |
| 数据质量任务 | `doc/p2-frontendtasks/quality.md` | 全新模块质检报告列表 共 24 子任务 |
| P2 总进度 | `doc/p2-frontendtasks/p2-frontendprogress.md` | 模块进度、开发顺序、里程碑 |

---

## 二、主 Agent 职责

### 2.1 你需要做的事情

1. **读取并理解**全部参考文档，建立对 P2 工程的全局认知
2. **按阶段顺序**依次生成子 Agent 执行各模块编码任务
3. **跟踪每个模块**的完成状态，记录到 `p2-frontendprogress.md`
4. **处理模块间依赖**：确保基础设施完成后才启动上层模块
5. **集成验证**：所有子 Agent 完成后，进行全模块联调检查

### 2.2 你不需要做的事情

- **不要自己写代码**：编码工作由子 Agent 完成
- **不要修改设计**：严格按设计文档执行，不要自行发挥
- **不要跳过依赖**：基础设施未完成前不要启动依赖它的模块

---

## 三、模块架构与依赖关系

### 3.1 8 个模块总览

```
模块 1: 基础设施与公共组件 (123 子任务) ← 所有模块的基础，必须最先完成
   ├── 1.1 依赖安装 (@microsoft/fetch-event-source)
   ├── 1.2 类型定义（types/index.ts）
   ├── 1.3 Token 工具（utils/token.ts）
   ├── 1.4 SSE 流式工具（utils/sse.ts）★
   ├── 1.5 格式化工具（utils/format.ts）
   ├── 1.6 Axios 实例升级（api/index.ts）
   ├── 1.7-1.13 全部 API 模块（auth/chat/knowledge/statistics/datasource/catalog/dataquality）
   ├── 1.14 路由配置升级（router/index.ts）
   ├── 1.15 Auth Store 升级（stores/auth.ts）
   ├── 1.16 全局样式（styles/global.scss）
   ├── 1.17 Sidebar 菜单升级
   └── 1.18 HeaderBar 升级

模块 2: 问答模块 (58 子任务) ← 依赖模块1，P2 核心差异化功能
   ├── ChatInput 升级（域选择器 + 停止按钮）
   ├── MessageBubble 升级（流式光标 + 反馈按钮 + 引用来源）
   ├── ChatView 升级（SSE 流式核心逻辑 + 反馈评价交互）
   └── ConversationList（不变）

模块 3: 知识库模块 (32 子任务) ← 依赖模块1
   ├── UploadDialog 升级（业务域 + 密级选择器）
   ├── DocumentTable 升级（新增域/密级列）
   ├── StatusTag 升级（新增 DELETING 状态）
   └── KnowledgeView 升级（筛选栏 + 软删除）

模块 4: 用户管理模块 (28 子任务) ← 依赖模块1
   ├── UserFormDialog 升级（ABAC 字段表单）
   ├── UserTable 升级（ABAC 字段列）
   └── UserManageView 升级（搜索扩展）

模块 5: 仪表盘模块 (25 子任务) ← 依赖模块1
   ├── 统计卡片扩展（+3 满意度卡片）
   ├── ECharts 图表升级（满意度折线 + 反馈饼图）
   └── 数据处理

模块 6: 数据源管理模块 (58 子任务) ← 依赖模块1，全新模块
   ├── SyncStatusTag 组件 ★
   ├── DataSourceFormDialog 组件 ★（动态连接配置表单）
   ├── DataSourceTable 组件 ★
   └── DataSourceView 页面 ★

模块 7: 知识目录模块 (27 子任务) ← 依赖模块1，全新模块
   ├── CatalogTree 组件 ★（el-tree 三级目录）
   └── CatalogView 页面 ★

模块 8: 数据质量模块 (24 子任务) ← 依赖模块1，全新模块
   └── QualityReportView 页面 ★（展开行详情 + 规则分布图）
```

### 3.2 依赖图

```
阶段 A（基础设施 — 模块 1）
  └── 必须先完成，所有其他模块依赖它

阶段 B（P1 升级 — 模块 2/3/4/5）
  └── 依赖阶段 A，4 个模块可并行

阶段 C（新增页面 — 模块 6/7/8）
  └── 依赖阶段 A，3 个模块可并行，也可与阶段 B 并行

阶段 D（集成验证）
  └── 依赖阶段 B + 阶段 C 全部完成
```

---

## 四、开发执行流程

### 4.1 阶段 A：基础设施（模块 1）

**执行时机**：立即启动，第一优先级

**子 Agent 生成**：

```
生成 1 个子 Agent，负责「基础设施与公共组件」全部任务。

子 Agent 提示词要点：
- 读取 doc/p2-frontend-design.md 第二~六章、第八~十三章
- 读取 doc/p2-frontendtasks/infrastructure.md
- 严格按照 1.1 → 1.18 的顺序逐项实现（后面的依赖前面的）
- 先读取每个需要修改的现有文件，理解 P1 基线后再修改
- 新增文件（api/datasource.ts, api/catalog.ts, api/dataquality.ts, utils/sse.ts）从零创建
- 完成一个子类后勾选 p2-frontendprogress.md 中对应的 checkbox
```

**完成标准**：
- `npm install @microsoft/fetch-event-source` 成功
- TypeScript 编译无错误（`npx vue-tsc --noEmit`）
- 所有 123 个子任务 checkbox 已勾选

**阶段 A 完成前，不允许启动阶段 B 或阶段 C 的任何模块。**

### 4.2 阶段 B：P1 页面升级（模块 2/3/4/5）

**执行时机**：阶段 A 完成后

**子 Agent 生成**：并行生成 4 个子 Agent

#### 子 Agent B1：问答模块 SSE 流式升级

```
读取：doc/p2-frontend-design.md 第七章 7.1、第八章 8.1-8.2
读取：doc/p2-frontendtasks/chat.md

实现顺序：
1. ChatInput.vue — Props/Emits 扩展 → 域选择器 → 停止按钮 → 交互细节
2. MessageBubble.vue — Props 扩展 → 流式光标动画 → 引用来源 → 反馈按钮
3. ChatView.vue — 状态管理 → 流式问答核心逻辑（handleSend 重写）→ 停止生成 → 反馈评价 → 模板改造 → 边角情况

关键注意：
- askStream 返回 AbortController，需在 onUnmounted 中 abort
- streaming 消息使用 triggerRef 触发响应式更新
- 发送中（sending=true）禁止再次发送
- P1 同步 ask() 方法保留但不再使用
```

#### 子 Agent B2：知识库模块升级

```
读取：doc/p2-frontend-design.md 第七章 7.2、第八章 8.5
读取：doc/p2-frontendtasks/knowledge.md

实现顺序：
1. UploadDialog.vue — allowedDomains/maxClearanceLevel props → 域选择器 → 密级选择器 → 上传参数传递
2. DocumentTable.vue — 新增域列 + 密级列 → 列顺序调整 → 删除行为变更
3. StatusTag.vue — 新增 DELETING 状态
4. KnowledgeView.vue — 筛选栏升级 → 域/密级筛选 → 上传按钮改造

关键注意：
- 域下拉选项根据用户 allowedDomains 过滤
- 密级选项不超过用户 clearanceLevel
- 删除变为软删除，状态显示 DELETING 而非立即消失
```

#### 子 Agent B3：用户管理模块升级

```
读取：doc/p2-frontend-design.md 第七章 7.3
读取：doc/p2-frontendtasks/user.md

实现顺序：
1. UserFormDialog.vue — ABAC 字段表单（部门/密级/允许域/职级）→ 表单校验 → 数据提交
2. UserTable.vue — 新增 ABAC 列 → 列顺序调整
3. UserManageView.vue — 搜索扩展

关键注意：
- allowedDomains 在提交时数组转逗号分隔字符串，读取时反之
- 编辑模式下预填已有 ABAC 字段值
- 使用 el-divider 分隔 ABAC 属性区域
```

#### 子 Agent B4：仪表盘模块升级

```
读取：doc/p2-frontend-design.md 第七章 7.4
读取：doc/p2-frontendtasks/dashboard.md

实现顺序：
1. 新增 3 个统计卡片（点赞/点踩/满意度）
2. ECharts 折线图升级（满意度右轴系列）
3. ECharts 新增反馈分布饼图
4. 数据处理与 API 对接

关键注意：
- 满意度率使用条件颜色（≥80% 绿，50-80% 橙，<50% 红）
- 折线图满意度使用右侧 Y 轴，区间 [0, 100]
- 无反馈数据时饼图显示空状态
```

**阶段 B 完成标准**：4 个子模块各自 TypeScript 编译无错误，所有任务 checkbox 已勾选。

### 4.3 阶段 C：新增页面（模块 6/7/8）+ 布局组件（模块 1.17/1.18）

**执行时机**：阶段 A 完成后（可与阶段 B 并行）

**子 Agent 生成**：并行生成 5 个子 Agent

#### 子 Agent C1：Sidebar + HeaderBar 升级

```
Sidebar（doc/p2-frontend-design.md 第八章 8.3）：
- 新增 3 个 P2 菜单项（数据接入/知识目录/质量报告）
- 实现 manager/director 职级可看到知识目录

HeaderBar（doc/p2-frontend-design.md 第八章 8.4）：
- Token 过期倒计时（setInterval 10s）
- 剩余 ≤60s 红色警告
- 静默刷新提示
- onUnmounted 清除定时器
```

#### 子 Agent C2：数据源管理模块

```
读取：doc/p2-frontend-design.md 第七章 7.5
读取：doc/p2-frontendtasks/datasource.md

实现顺序：
1. SyncStatusTag.vue — 三态标签组件（ACTIVE/ERROR/INACTIVE）
2. DataSourceFormDialog.vue — 基本字段 + JDBC/REST/S3 动态连接配置表单 + 测试连接
3. DataSourceTable.vue — 列表 + 操作按钮
4. DataSourceView.vue — 页面组装 + CRUD + 同步/测试/删除交互

关键注意：
- 连接配置表单根据 sourceType 动态切换（JDBC/REST/S3 三种布局）
- 切换 sourceType 时清空之前的连接配置
- 测试连接结果显示延迟和数据量
- 同步操作需二次确认
```

#### 子 Agent C3：知识目录模块

```
读取：doc/p2-frontend-design.md 第七章 7.6
读取：doc/p2-frontendtasks/catalog.md

实现顺序：
1. CatalogTree.vue — el-tree 三级渲染 + 自定义节点 slot + 搜索过滤
2. CatalogView.vue — 页面结构 + 搜索防抖 + 实体详情弹窗

关键注意：
- 一级节点默认展开，二级默认折叠
- 搜索过滤大小写不敏感，匹配节点自动展开
- 实体点击弹出详情对话框（el-dialog）
```

#### 子 Agent C4：数据质量模块

```
读取：doc/p2-frontend-design.md 第七章 7.7
读取：doc/p2-frontendtasks/quality.md

实现顺序：
1. QualityReportView.vue — 报告列表 + 展开行详情 + 规则分布进度条 + 筛选排序

关键注意：
- 阻断批次行红色浅底高亮（row-class-name）
- 展开行显示不合格明细表 + 按规则分布的水平进度条
- 合格率列条件颜色（≥90% 绿，60-90% 橙，<60% 红）
- 支持按合格率排序和阻断筛选
```

**阶段 C 完成标准**：5 个子模块各自 TypeScript 编译无错误，所有任务 checkbox 已勾选。

### 4.4 阶段 D：集成验证

**执行时机**：阶段 B 和阶段 C 全部完成后

**你需要做的事情**（不需要子 Agent）：

1. **编译检查**：运行 `npx vue-tsc --noEmit` 确保整个项目无 TS 类型错误
2. **构建验证**：运行 `npm run build`（或 `npx vite build`）确保 Vite 构建成功
3. **路由完整性**：验证所有 10 个路由均可正确懒加载：
   - `/login`, `/register`, `/chat`
   - `/admin/users`, `/admin/knowledge`, `/admin/dashboard`
   - `/admin/datasource`, `/admin/catalog`, `/admin/quality`
   - `/403`, `/:pathMatch(.*)*`
4. **API 一致性**：检查 `api/` 目录下所有函数签名与设计文档一致
5. **组件引用检查**：确保所有新增组件已正确导入和注册
6. **进度更新**：将 `p2-frontendprogress.md` 中的里程碑 M1-M4 全部标记为完成

---

## 五、子 Agent 通用规范

每个子 Agent 在被生成时，必须遵循以下规范：

### 5.1 工作流程

```
1. 阅读分配给自己的设计文档章节和任务文件
2. 读取需要修改的现有文件，理解 P1 基线
3. 按任务文件中的顺序逐项实现
4. 每完成一个子类任务，更新对应 checkbox 状态
5. 全部完成后报告完成状态
```

### 5.2 代码规范

- **使用 Composition API + `<script setup lang="ts">`**，与现有代码风格保持一致
- **TypeScript 严格模式**：所有 props/emits/ref 明确类型
- **Element Plus 组件**：优先使用 el-* 组件，保持 UI 一致性
- **样式**：优先使用 scoped SCSS，全局样式仅写入 `styles/global.scss`
- **API 调用**：统一通过 `api/` 目录下的模块导出函数，不在组件中直接使用 axios
- **Token 处理**：统一使用 `utils/token.ts` 导出的函数，不直接操作 localStorage
- **错误处理**：API 级别错误由 Axios 拦截器统一处理；组件级别仅处理业务异常
- **命名规范**：
  - 组件文件：PascalCase（如 `DataSourceFormDialog.vue`）
  - 文件夹：kebab-case（如 `datasource/`）
  - 函数/变量：camelCase
  - 接口/类型：PascalCase
  - 常量：UPPER_SNAKE_CASE

### 5.3 新增文件清单（需从零创建）

以下文件在 P1 中不存在，需要全新创建：

| 文件 | 对应模块 |
|------|----------|
| `src/utils/sse.ts` | 模块1（基础设施） |
| `src/api/datasource.ts` | 模块1（基础设施） |
| `src/api/catalog.ts` | 模块1（基础设施） |
| `src/api/dataquality.ts` | 模块1（基础设施） |
| `src/views/datasource/DataSourceView.vue` | 模块6 |
| `src/components/datasource/SyncStatusTag.vue` | 模块6 |
| `src/components/datasource/DataSourceFormDialog.vue` | 模块6 |
| `src/components/datasource/DataSourceTable.vue` | 模块6 |
| `src/views/catalog/CatalogView.vue` | 模块7 |
| `src/components/catalog/CatalogTree.vue` | 模块7 |
| `src/views/dataquality/QualityReportView.vue` | 模块8 |

### 5.4 修改文件清单（在 P1 基础上增量修改）

以下文件已存在，需要按设计文档进行修改。**必须先 Read 理解 P1 基线再做修改**：

| 文件 | 修改类型 | 对应模块 |
|------|----------|----------|
| `src/types/index.ts` | 新增类型 + 扩展已有接口 | 模块1 |
| `src/utils/token.ts` | 双 Token 机制重构 | 模块1 |
| `src/utils/format.ts` | 新增格式化函数 | 模块1 |
| `src/api/index.ts` | 双 Token 静默刷新 + TraceId | 模块1 |
| `src/api/auth.ts` | 新增 refresh/revoke 方法 | 模块1 |
| `src/api/chat.ts` | 新增 askStream/submitFeedback | 模块1 |
| `src/api/knowledge.ts` | upload 新增参数 | 模块1 |
| `src/api/statistics.ts` | 响应类型更新 | 模块1 |
| `src/router/index.ts` | 新增路由 + ABAC 守卫 | 模块1 |
| `src/stores/auth.ts` | ABAC 字段 + 双 Token | 模块1 |
| `src/styles/global.scss` | 新增 P2 样式 | 模块1 |
| `.env.development` | 新增 SSE 相关环境变量 | 模块1 |
| `package.json` | 新增依赖（手动安装） | 模块1 |
| `src/components/layout/Sidebar.vue` | 新增菜单项 | 模块1 |
| `src/components/layout/HeaderBar.vue` | Token 倒计时 | 模块1 |
| `src/components/chat/ChatInput.vue` | 域选择器 + 停止按钮 | 模块2 |
| `src/components/chat/MessageBubble.vue` | 流式光标 + 反馈 + 来源 | 模块2 |
| `src/views/chat/ChatView.vue` | SSE 流式核心逻辑 | 模块2 |
| `src/components/knowledge/UploadDialog.vue` | 域选择 + 密级选择 | 模块3 |
| `src/components/knowledge/DocumentTable.vue` | 新增列 | 模块3 |
| `src/components/knowledge/StatusTag.vue` | DELETING 状态 | 模块3 |
| `src/views/knowledge/KnowledgeView.vue` | 筛选栏升级 | 模块3 |
| `src/components/user/UserFormDialog.vue` | ABAC 字段表单 | 模块4 |
| `src/components/user/UserTable.vue` | ABAC 字段列 | 模块4 |
| `src/views/user/UserManageView.vue` | 搜索扩展 | 模块4 |
| `src/views/dashboard/DashboardView.vue` | 满意度卡片 + 图表升级 | 模块5 |

---

## 六、进度跟踪

### 6.1 进度文件

使用 `doc/p2-frontendtasks/p2-frontendprogress.md` 跟踪进度。每个子 Agent 完成后，由你（主 Agent）更新对应模块的状态。

状态标记：
- `⬜` 未开始
- `🔄` 进行中
- `✅` 已完成

### 6.2 里程碑

| 里程碑 | 内容 | 完成条件 |
|--------|------|----------|
| M1 - 基础设施就绪 | 模块1 全部完成 | TypeScript 编译无错误 + 123 子任务全部勾选 |
| M2 - P1 升级完成 | 模块2/3/4/5 全部完成 | TypeScript 编译无错误 + 4 个模块子任务全部勾选 |
| M3 - P2 新增完成 | 模块6/7/8 + 布局升级全部完成 | TypeScript 编译无错误 + 4 个模块子任务全部勾选 |
| M4 - P2 前端交付 | 阶段 D 集成验证通过 | 构建成功 + 全模块联调通过 |

---

## 七、执行指令

现在，请按以下顺序开始工作：

### Step 1：初始化

1. 读取 `doc/p2-frontend-design.md` 全文，建立对 P2 工程的完整理解
2. 读取 `doc/p2-frontendtasks/p2-frontendprogress.md`，了解总进度
3. 确认 `agent-qr-web-frontend/` 目录下 P1 代码存在且可访问

### Step 2：启动阶段 A

生成子 Agent 执行「模块 1：基础设施与公共组件」。

子 Agent 提示词：

```
你是 P2 前端基础设施模块的实现者。

任务范围：doc/p2-frontendtasks/infrastructure.md 中的全部 123 个子任务。

设计依据：doc/p2-frontend-design.md 第二~六章、第八~十三章。

工作目录：agent-qr-web-frontend/

要求：
1. 先读取 doc/p2-frontend-design.md 了解设计
2. 再读取 doc/p2-frontendtasks/infrastructure.md 获取详细任务清单
3. 按 1.1 → 1.18 的顺序依次实现（后续任务依赖前面的）
4. 每个需要修改的现有文件，必须先 Read 理解 P1 基线后再改
5. 完成一个子类后立即用 Edit 更新 p2-frontendprogress.md 中的 checkbox 状态
6. 全部完成后报告「模块1 基础设施完成」

注意：
- 不要修改设计文档
- 代码风格与现有 P1 代码保持一致
- 遇到不明确的地方停下来确认，不要猜测
```

### Step 3：等待阶段 A 完成后，启动阶段 B + 阶段 C 并行

#### 并行启动 4 个阶段 B 子 Agent：

**子 Agent B1**（问答模块）：
```
你是 P2 问答模块的实现者。
任务范围：doc/p2-frontendtasks/chat.md 的 58 个子任务。
设计依据：doc/p2-frontend-design.md 第七章 7.1、第八章 8.1-8.2。
工作目录：agent-qr-web-frontend/
按任务文件顺序实现（ChatInput → MessageBubble → ChatView），完成后报告。
```

**子 Agent B2**（知识库模块）：
```
你是 P2 知识库模块的实现者。
任务范围：doc/p2-frontendtasks/knowledge.md 的 32 个子任务。
设计依据：doc/p2-frontend-design.md 第七章 7.2、第八章 8.5。
工作目录：agent-qr-web-frontend/
按任务文件顺序实现（UploadDialog → DocumentTable → StatusTag → KnowledgeView），完成后报告。
```

**子 Agent B3**（用户管理模块）：
```
你是 P2 用户管理模块的实现者。
任务范围：doc/p2-frontendtasks/user.md 的 28 个子任务。
设计依据：doc/p2-frontend-design.md 第七章 7.3。
工作目录：agent-qr-web-frontend/
按任务文件顺序实现（UserFormDialog → UserTable → UserManageView），完成后报告。
```

**子 Agent B4**（仪表盘模块）：
```
你是 P2 仪表盘模块的实现者。
任务范围：doc/p2-frontendtasks/dashboard.md 的 25 个子任务。
设计依据：doc/p2-frontend-design.md 第七章 7.4。
工作目录：agent-qr-web-frontend/
按任务文件顺序实现（统计卡片 → ECharts 升级 → 数据处理），完成后报告。
```

#### 并行启动 5 个阶段 C 子 Agent：

**子 Agent C1**（Sidebar + HeaderBar）：
```
你是 P2 布局组件升级的实现者。
任务范围：doc/p2-frontendtasks/infrastructure.md 中 17 和 18 两类任务。
设计依据：doc/p2-frontend-design.md 第八章 8.3-8.4。
工作目录：agent-qr-web-frontend/
完成 Sidebar 菜单项新增 + HeaderBar Token 倒计时，完成后报告。
```

**子 Agent C2**（数据源管理）：
```
你是 P2 数据源管理模块的实现者。
任务范围：doc/p2-frontendtasks/datasource.md 的 58 个子任务。
设计依据：doc/p2-frontend-design.md 第七章 7.5。
工作目录：agent-qr-web-frontend/
按任务文件顺序实现（SyncStatusTag → DataSourceFormDialog → DataSourceTable → DataSourceView），完成后报告。
```

**子 Agent C3**（知识目录）：
```
你是 P2 知识目录模块的实现者。
任务范围：doc/p2-frontendtasks/catalog.md 的 27 个子任务。
设计依据：doc/p2-frontend-design.md 第七章 7.6。
工作目录：agent-qr-web-frontend/
按任务文件顺序实现（CatalogTree → CatalogView），完成后报告。
```

**子 Agent C4**（数据质量）：
```
你是 P2 数据质量模块的实现者。
任务范围：doc/p2-frontendtasks/quality.md 的 24 个子任务。
设计依据：doc/p2-frontend-design.md 第七章 7.7。
工作目录：agent-qr-web-frontend/
实现 QualityReportView 全部功能，完成后报告。
```

### Step 4：启动阶段 D

所有子 Agent 完成后，执行集成验证：

1. 运行 `cd agent-qr-web-frontend && npx vue-tsc --noEmit` 检查类型
2. 运行 `cd agent-qr-web-frontend && npx vite build` 检查构建
3. 人工检查以下要点：
   - 所有新路由可正确懒加载
   - API 函数签名与设计一致
   - 新增组件已正确注册
   - 无遗漏的导入/导出
4. 将所有里程碑标记为完成
5. 输出最终报告摘要

---

## 八、异常处理

### 8.1 子 Agent 执行失败

如果某个子 Agent 报告无法完成任务：
1. 分析失败原因（是设计不明确还是技术障碍）
2. 如果是设计不明确：向我（用户）提问确认
3. 如果是技术障碍：尝试调整子 Agent 的实现策略，重新生成
4. 不要自行修改设计方案

### 8.2 模块间冲突

如果发现模块间存在代码冲突（如两个子 Agent 修改了同一文件的不同部分）：
1. 检查是否是由于阶段 A 的任务划分遗漏导致
2. 手动协调合并冲突
3. 记录冲突原因到进度文件

### 8.3 编译错误

如果集成验证阶段发现 TS 编译错误：
1. 定位错误文件和原因
2. 判断属于哪个模块的责任范围
3. 生成修复子 Agent 处理

---

## 九、总结

- **你的角色**：P2 前端工程的主 Agent（Orchestrator）
- **总任务数**：约 375 个子任务，分布在 8 个模块中
- **子 Agent 数量**：共 10 个（1 + 4 + 5）
  - 阶段 A：1 个（基础设施）
  - 阶段 B：4 个（问答/知识库/用户/仪表盘）
  - 阶段 C：5 个（布局升级 + 数据源/知识目录/数据质量）
- **依赖约束**：阶段 A 必须先完成，阶段 B/C 可并行，阶段 D 在 B+C 全部完成后
- **核心原则**：严格按设计文档执行，不猜测、不自行发挥

现在请开始 Step 1：读取设计文档和进度文件，建立全局认知。
