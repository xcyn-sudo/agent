# 数据质量模块 - P2 前端任务列表

> 对应设计文档：p2-frontend-design.md 第七章 7.7
> 涉及页面：`QualityReportView.vue` ★
> 涉及 API：`api/dataquality.ts` ★
> 模块类型：P2 新增（★）
> 状态：未开始

---

## 1. QualityReportView 页面（`views/dataquality/QualityReportView.vue`）★

### 1.1 页面结构

- [ ] 新建文件 `views/dataquality/QualityReportView.vue`
- [ ] 页面标题：「数据质量报告」
- [ ] 顶部筛选栏：筛选条件（可选：按合格率范围、是否阻断筛选）
- [ ] 主体内容：报告列表（`el-table`）+ 展开行详情
- [ ] 底部分页：`Pagination` 组件

### 1.2 状态管理

- [ ] `reports: Ref<QualityReport[]>` 报告列表数据
- [ ] `loading: Ref<boolean>` 加载状态
- [ ] `pagination: { page: number, size: number, total: number }` 分页状态
- [ ] `expandedRows: Ref<Set<string>>` 展开行集合（用于控制展开行）
- [ ] `expandedDetail: Ref<QualityReport | null>` 当前展开的详情数据
- [ ] `filterBlocked: Ref<boolean | null>` 阻断筛选（null=全部, true=仅阻断, false=仅通过）

### 1.3 数据加载

- [ ] `fetchReports()` 函数：调用 `dataqualityApi.listReports({ page, size })`
- [ ] `onMounted` 时加载数据
- [ ] 分页变化时重新加载
- [ ] 支持阻断筛选参数传递（后端支持的话）

### 1.4 报告列表表格

- [ ] `el-table` 列定义：批次号 → 数据源 → 总数 → 合格 → 不合格 → 合格率 → 是否阻断 → 检查时间
- [ ] 「合格率」列：格式化显示百分比（`(passRate * 100).toFixed(1) + '%'`）
- [ ] 「合格率」列：使用条件颜色（≥90% 绿色, 60%-90% 橙色, <60% 红色）
- [ ] 「是否阻断」列：`blocked === true` → 「🚫阻断」红色标签；`blocked === false` → 「✅通过」绿色标签
- [ ] 「检查时间」列格式化显示
- [ ] 阻断批次行使用红色浅底高亮（`row-class-name` 函数判断）
- [ ] 表格支持点击行展开（`el-table` 的 `expand` 或 `@row-click` 切换展开）

### 1.5 展开行详情

- [ ] 使用 `el-table` 的 `type="expand"` 插槽或手动控制展开区域
- [ ] 展开区域标题：`{batchId} 质检明细 ({sourceName})`
- [ ] **不合格明细表格**：
  - [ ] 小型 `el-table` 展示 `failures: QualityFailure[]`
  - [ ] 列：规则名称（`ruleName`）→ 记录索引（`recordIndex`）→ 原因说明（`reason`）
  - [ ] 支持不合格明细翻页（如果数量多）
- [ ] **按规则分布图**（水平进度条）：
  - [ ] 统计各规则的不合格数量（computed：从 `failures` 按 `ruleName` 分组计数）
  - [ ] 每个规则显示：规则名称 + 水平进度条 + 数量 + 百分比
  - [ ] 进度条颜色区分不同规则
  - [ ] 进度条最大值为总不合格数（`failCount`）

### 1.6 排序与筛选

- [ ] 合格率列支持排序（`sortable`）
- [ ] 检查时间列支持排序（`sortable`，默认降序）
- [ ] 阻断筛选下拉（`el-select`）：全部 / 仅阻断 / 仅通过
- [ ] 阻断筛选变化时重新加载数据（带筛选参数）

### 1.7 错误处理与空状态

- [ ] 列表数据为空时显示 `el-empty`（「暂无质量报告数据」）
- [ ] 展开行加载失败提示
- [ ] API 错误统一由 Axios 拦截器处理

### 1.8 批量查看（可选）

- [ ] 展开行支持同时展开多个（`el-table` 默认支持多行展开）
- [ ] 可选：「一键展开全部阻断批次」按钮

---

## 2. 数据格式化与工具函数

### 2.1 质量相关 computed

- [ ] `ruleDistribution` computed：按规则名称分组统计不合格数量
- [ ] `rulePercentage(ruleCount: number, totalFail: number): string`：计算规则占比百分比
- [ ] `passRateColor(rate: number): string`：根据合格率返回颜色 class

### 2.2 表格行样式

- [ ] `tableRowClassName({ row })` 函数：阻断批次返回 `'row--blocked'`
- [ ] `.row--blocked` 样式：`background-color: #fef0f0`（浅红色背景）

---

> **统计**：共 2 大类，约 24 个子任务
