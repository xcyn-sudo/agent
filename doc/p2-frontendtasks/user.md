# 用户管理模块 - P2 前端任务列表

> 对应设计文档：p2-frontend-design.md 第七章 7.3
> 涉及页面：`UserManageView.vue`
> 涉及组件：`UserTable.vue`、`UserFormDialog.vue`
> 模块类型：P1 升级（△）
> 状态：未开始

---

## 1. UserFormDialog 组件升级（`components/user/UserFormDialog.vue`）

### 1.1 ABAC 字段表单（★）

- [ ] 表单新增「部门」选择器（`el-select`），标签为「部门」，放在角色字段之后
- [ ] 部门选项从 `DEPARTMENTS` 常量获取（HR/FINANCE/RD/SALES/COMMON）
- [ ] 部门选项显示中文标签（人力资源/财务管理/研发中心/销售管理/公共部门）
- [ ] 表单新增「数据密级」选择器（`el-select`），标签为「数据密级」
- [ ] 密级选项从 `SENSITIVITY_LEVELS` 常量获取（公开/内部/机密/绝密）
- [ ] 表单新增「允许访问域」多选组件（`el-checkbox-group` 或 `el-select[multiple]`），标签为「允许访问域」
- [ ] 允许访问域选项从 `DOMAINS` 常量获取
- [ ] 表单新增「职级」选择器（`el-select`），标签为「职级」
- [ ] 职级选项从 `TITLES` 常量获取（employee/manager/director），显示中文标签

### 1.2 表单布局

- [ ] ABAC 字段区域添加视觉分隔（`el-divider` 标题为「ABAC 属性」）
- [ ] 表单字段顺序：用户名 → 密码（新增时）→ 真实姓名 → 邮箱 → 手机号 → 角色 → 分隔线 → 部门 → 数据密级 → 允许访问域 → 职级
- [ ] 编辑模式下：ABAC 字段允许修改（由 admin 操作）

### 1.3 数据提交

- [ ] `handleSubmit` 提交数据时包含 `department` 字段
- [ ] `handleSubmit` 提交数据时包含 `clearanceLevel` 字段
- [ ] `handleSubmit` 提交数据时包含 `allowedDomains` 字段（数组转逗号分隔字符串作为传输格式）
- [ ] `handleSubmit` 提交数据时包含 `title` 字段
- [ ] 编辑模式下预填已有 ABAC 字段值

### 1.4 表单校验

- [ ] 部门字段：必填校验
- [ ] 数据密级字段：必填校验
- [ ] 允许访问域：至少选一个域
- [ ] 职级字段：必填校验

---

## 2. UserTable 组件升级（`components/user/UserTable.vue`）

### 2.1 新增列

- [ ] `el-table` 新增「部门」列（`prop="department"`）
- [ ] 部门列使用 `DEPARTMENTS` 常量映射显示中文标签
- [ ] `el-table` 新增「密级」列（`prop="clearanceLevel"`）
- [ ] 密级列使用 `formatSensitivityLevel` 格式化 + `.sensitivity-tag--*` 样式
- [ ] `el-table` 新增「允许域」列（`prop="allowedDomains"`）
- [ ] 允许域列使用 `.domain-tag` 样式渲染（多个域显示多个标签）
- [ ] `el-table` 新增「职级」列（`prop="title"`）
- [ ] 职级列显示中文标签（employee→员工, manager→经理, director→总监）

### 2.2 列顺序调整

- [ ] 列顺序调整为：ID → 用户名 → 姓名 → 角色 → 部门 → 密级 → 允许域 → 职级 → 状态 → 创建时间 → 操作
- [ ] 适当调整各列宽度以适应新增列

---

## 3. UserManageView 页面升级（`views/user/UserManageView.vue`）

### 3.1 搜索功能扩展

- [ ] 搜索框支持按部门筛选（搜索参数新增 `department` 字段）
- [ ] 搜索框支持按职级筛选（搜索参数新增 `title` 字段）
- [ ] 搜索区域可新增部门下拉筛选和职级下拉筛选

### 3.2 列表接口调用

- [ ] 列表请求参数保持不变（分页 + 搜索关键词），后端返回数据包含新字段
- [ ] 列表数据传递到 `UserTable` 时包含完整 ABAC 字段

### 3.3 新增/编辑用户

- [ ] `UserFormDialog` 传递完整用户数据（含 ABAC 字段）
- [ ] 新增用户成功后刷新列表

### 3.4 页面布局

- [ ] 搜索区域补充 ABAC 相关筛选条件（可选：部门下拉 + 职级下拉）
- [ ] 无其他布局变更

---

> **统计**：共 3 大类，约 28 个子任务
