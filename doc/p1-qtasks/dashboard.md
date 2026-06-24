# P1 前端 — dashboard 模块任务清单

> 模块：数据仪表盘
>
> 依赖：infra、auth
>
> 开发顺序：第 7 步
>
> 权限：仅 ADMIN 角色可访问

---

## 子任务

- [ ] **7.1 实现统计卡片区**
  - 4 个 `el-card` 卡片：今日问答、今日新增用户、文档总数、用户总数
  - 每个卡片显示：数字（大号字体）+ 标签文字
  - 加载时显示骨架屏（`el-skeleton`）
  - 数据来源：`statisticsApi.getDashboard()`

- [ ] **7.2 实现近 7 天问答趋势图**
  - 使用 ECharts 折线图（`vue-echarts`）
  - X 轴：近 7 天日期
  - Y 轴：问答数
  - 数据来源：`dashboard.weeklyTrend`
  - 无数据时显示空状态

- [ ] **7.3 实现文档类型分布图**
  - 使用 ECharts 饼图
  - 扇区：pdf / docx / txt / md 各占比
  - 显示百分比标签
  - 数据来源：`dashboard.docTypeDistribution`
  - 无数据时显示空状态

- [ ] **7.4 实现仪表盘页面** (`views/dashboard/DashboardView.vue`)
  - 页面标题："数据仪表盘"
  - 页面加载时调用 `statisticsApi.getDashboard()` 获取数据
  - 顶部：统计卡片区（4 个卡片一行）
  - 下方：左右两栏布局
    - 左：近 7 天趋势折线图
    - 右：文档类型分布饼图
  - 数据请求失败 → toast 提示"数据加载失败"

- [ ] **7.5 添加路由配置**
  - 路由路径：`/admin/dashboard`
  - meta：`requiresAuth: true`、`requiresAdmin: true`
  - 标题："数据仪表盘"

---

## 验证标准

- [ ] 4 个统计卡片显示正确数据
- [ ] 折线图正确渲染近 7 天趋势
- [ ] 饼图正确渲染文档类型分布
- [ ] API 返回空数据时图表显示空状态
- [ ] API 调用失败时有 toast 提示
- [ ] 非 admin 用户无法访问 `/admin/dashboard`（跳转 403）

---

> 预计耗时：0.75 天
