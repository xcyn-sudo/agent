# P1 前端 — user 模块任务清单

> 模块：用户管理模块
>
> 依赖：infra、auth
>
> 开发顺序：第 6 步
>
> 权限：仅 ADMIN 角色可访问

---

## 子任务

- [ ] **6.1 实现用户列表表格** (`components/user/UserTable.vue`)
  - 表格列：ID、用户名、姓名、角色、状态、创建时间、操作
  - 状态列：绿色标签"启用" / 红色标签"禁用"
  - 角色列：蓝色标签"admin" / 默认标签"user"
  - 操作列：
    - 「编辑」按钮 → 打开 UserFormDialog（编辑模式）
    - 「启用/禁用」按钮 → 二次确认 → `userApi.toggleStatus(id, newStatus)`
  - 空状态："暂无用户"

- [ ] **6.2 实现用户表单弹窗** (`components/user/UserFormDialog.vue`)
  - Props：`mode: 'create' | 'edit'`、`userData?: UserInfo`
  - 创建模式：
    - 用户名（必填）、密码（必填）、真实姓名、邮箱、手机号
  - 编辑模式：
    - 真实姓名、邮箱、手机号、角色（下拉选择 admin/user）
    - 不含密码字段
  - 表单校验：
    - 用户名：必填，2-20 字符
    - 密码：必填（创建时），6-30 字符
    - 邮箱：选填，邮箱格式
    - 手机号：选填，11 位数字
  - 提交 → 调用 `userApi.createUser()` 或 `userApi.updateUser()`
  - 成功后关闭弹窗 → 触发父组件刷新列表

- [ ] **6.3 实现用户管理页** (`views/user/UserManageView.vue`)
  - 「+ 新增用户」按钮 → 打开 UserFormDialog（创建模式）
  - 搜索框：`el-input` + 搜索图标，输入关键词回车搜索
  - UserTable 列表展示
  - Pagination 分页（调用 `userApi.listUsers(page, size, keyword)`）
  - 搜索时重置到第 1 页

- [ ] **6.4 添加路由配置**
  - 路由路径：`/admin/users`
  - meta：`requiresAuth: true`、`requiresAdmin: true`
  - 标题："用户管理"

---

## 验证标准

- [ ] 用户列表正确加载（分页正常）
- [ ] 搜索关键词 → 列表正确过滤
- [ ] 新增用户 → 表单校验 → 提交成功 → 列表刷新
- [ ] 编辑用户 → 数据回填 → 提交成功 → 列表刷新
- [ ] 启用/禁用用户 → 二次确认 → 状态切换
- [ ] 非 admin 用户无法访问 `/admin/users`（跳转 403）

---

> 预计耗时：0.75 天
