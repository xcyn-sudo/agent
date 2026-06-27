# 数据源管理模块 - P2 前端任务列表

> 对应设计文档：p2-frontend-design.md 第七章 7.5
> 涉及页面：`DataSourceView.vue` ★
> 涉及组件：`DataSourceTable.vue` ★、`DataSourceFormDialog.vue` ★、`SyncStatusTag.vue` ★
> 涉及 API：`api/datasource.ts` ★
> 模块类型：P2 新增（★）
> 状态：未开始

---

## 1. SyncStatusTag 组件（`components/datasource/SyncStatusTag.vue`）★

- [ ] 新建文件 `components/datasource/SyncStatusTag.vue`
- [ ] 定义 props：`status: 'ACTIVE' | 'INACTIVE' | 'ERROR'`
- [ ] ACTIVE → 绿色标签（`el-tag type="success"`），文字「活跃」
- [ ] ERROR → 红色标签（`el-tag type="danger"`），文字「异常」
- [ ] INACTIVE → 灰色标签（`el-tag type="info"`），文字「停用」
- [ ] 标签旁显示对应小图标（`el-icon`）

---

## 2. DataSourceFormDialog 组件（`components/datasource/DataSourceFormDialog.vue`）★

### 2.1 基本结构

- [ ] 新建文件 `components/datasource/DataSourceFormDialog.vue`
- [ ] 使用 `el-dialog` 弹窗容器
- [ ] 定义 props：`modelValue: boolean`（v-model 控制显示）、`editData?: DataSourceConfig`（编辑模式数据）
- [ ] 定义 emits：`update:modelValue`, `submit`, `test-connection`
- [ ] 表单使用 `el-form` + `el-form-item`

### 2.2 基本字段表单

- [ ] 「数据源名称」文本输入框（必填，`el-input`）
- [ ] 「数据源类型」下拉选择（必填，`el-select`）：JDBC / REST / S3
- [ ] 「业务域」下拉选择（必填，`el-select`）：从 `DOMAINS` 常量获取
- [ ] 「同步策略」单选（`el-radio-group`）：全量 (FULL) / 增量 (INCREMENTAL)
- [ ] 「定时表达式」文本输入框（选填，`el-input`，placeholder: `0 0 2 * * ?`）

### 2.3 动态连接配置表单

- [ ] 根据 `sourceType` 动态切换连接配置区域
- [ ] **JDBC 配置**（`sourceType === 'JDBC'` 时显示）：
  - [ ] 「JDBC URL」文本输入框（必填，placeholder: `jdbc:mysql://host:port/db`）
  - [ ] 「用户名」文本输入框（必填）
  - [ ] 「密码」密码输入框（必填，`el-input type="password" show-password`）
  - [ ] 「表名列表」动态添加/删除（`el-tag` + `el-input` 组合，支持多个表名）
  - [ ] 「+ 添加表名」按钮
- [ ] **REST 配置**（`sourceType === 'REST'` 时显示）：
  - [ ] 「Base URL」文本输入框（必填，placeholder: `https://api.example.com`）
  - [ ] 「认证头」文本输入框（选填，placeholder: `Authorization: Bearer xxx`）
  - [ ] 「分页参数」文本输入框（选填，placeholder: `page={page}&size={size}`）
- [ ] **S3 配置**（`sourceType === 'S3'` 时显示）：
  - [ ] 「Bucket」文本输入框（必填）
  - [ ] 「Prefix」文本输入框（选填）
  - [ ] 「Access Key」文本输入框（必填）
  - [ ] 「Secret Key」密码输入框（必填，`show-password`）

### 2.4 配置数据管理

- [ ] 连接配置使用 `connectionConfig: Record<string, any>` 存储（key-value 对象）
- [ ] 切换 `sourceType` 时清空之前的连接配置字段
- [ ] 编辑模式下根据已有 `connectionConfig` 预填各字段

### 2.5 弹窗操作按钮

- [ ] 底部按钮：「取消」（关闭弹窗）、「测试连接」（`emit('test-connection')`）、「保存」（`emit('submit')`）
- [ ] 「测试连接」按钮：仅在新增模式且有连接配置填写时可用
- [ ] 测试连接期间按钮显示 loading 状态
- [ ] 测试连接结果以 `ElMessage` 提示：成功显示延迟和数据量，失败显示错误消息

### 2.6 表单校验

- [ ] 数据源名称：必填，最长 100 字符
- [ ] 数据源类型：必填
- [ ] 业务域：必填
- [ ] 同步策略：必选
- [ ] JDBC：URL + 用户名 + 密码 + 至少一个表名
- [ ] REST：Base URL 必填
- [ ] S3：Bucket + Access Key + Secret Key 必填

---

## 3. DataSourceTable 组件（`components/datasource/DataSourceTable.vue`）★

- [ ] 新建文件 `components/datasource/DataSourceTable.vue`
- [ ] 使用 `el-table` 展示数据源列表
- [ ] 列定义：数据源名称 → 类型 → 业务域 → 状态 → 最近同步 → 同步量 → 操作
- [ ] 「类型」列使用 `formatSourceType` 格式化（JDBC/REST/S3 → 中文）
- [ ] 「业务域」列使用 `formatDomain` 格式化
- [ ] 「状态」列使用 `SyncStatusTag` 组件渲染
- [ ] 「最近同步」列格式化时间（`lastSyncAt`，空值显示「—」）
- [ ] 「同步量」列格式化数字（`totalSynced`，空值显示「—」）
- [ ] 「操作」列按钮：🔄同步、🧪测试、✏️编辑、🗑删除
- [ ] 操作按钮间距使用 `el-button` + `link` 类型
- [ ] 触发 emits：`sync`, `test`, `edit`, `delete`

### 3.1 Props 和 Emits

- [ ] `props.data: DataSourceConfig[]`
- [ ] `props.loading: boolean`
- [ ] `emit('sync', id: number)`
- [ ] `emit('test', id: number)`
- [ ] `emit('edit', row: DataSourceConfig)`
- [ ] `emit('delete', id: number)`

---

## 4. DataSourceView 页面（`views/datasource/DataSourceView.vue`）★

### 4.1 页面结构

- [ ] 新建文件 `views/datasource/DataSourceView.vue`
- [ ] 使用 MainLayout 布局（通过路由配置）
- [ ] 页面标题：「多源数据接入管理」
- [ ] 顶部操作栏：[+ 新增数据源] 按钮 + [域筛选] 下拉

### 4.2 列表状态管理

- [ ] `dataSources: Ref<DataSourceConfig[]>` 列表数据
- [ ] `loading: Ref<boolean>` 加载状态
- [ ] `pagination: { page: number, size: number, total: number }` 分页状态
- [ ] `filterDomain: Ref<string>` 域筛选

### 4.3 数据加载

- [ ] `fetchDataSources()` 函数：调用 `datasourceApi.list({ page, size })`
- [ ] 支持域筛选参数传递
- [ ] `onMounted` 时加载数据
- [ ] 分页变化时重新加载

### 4.4 新增/编辑

- [ ] `showFormDialog: Ref<boolean>` 控制弹窗显示
- [ ] `editingRow: Ref<DataSourceConfig | null>` 编辑数据
- [ ] 「新增」按钮：`editingRow = null`，打开弹窗
- [ ] 「编辑」按钮：`editingRow = row`，打开弹窗（预填数据）
- [ ] 弹窗 `@submit` 处理：新增调用 `create()`，编辑调用 `update(id, data)`
- [ ] 提交成功后关闭弹窗 + 刷新列表 + 提示成功

### 4.5 连通性测试

- [ ] `testConnection(id: number)` 函数：调用 `datasourceApi.testConnection(id)`
- [ ] 测试中显示 loading 遮罩或按钮 loading
- [ ] 成功 → `ElMessage.success('连接成功，延迟: {latencyMs}ms')`
- [ ] 失败 → `ElMessage.error('连接失败: {errorMsg}')`

### 4.6 手动同步

- [ ] `triggerSync(id: number)` 函数：调用 `datasourceApi.triggerSync(id)`
- [ ] 同步触发前二次确认（`ElMessageBox.confirm`）
- [ ] 同步触发成功 → `ElMessage.success('同步任务已启动')` → 刷新列表
- [ ] 同步触发失败 → `ElMessage.error('同步触发失败: {message}')`

### 4.7 删除

- [ ] 删除前二次确认（`ElMessageBox.confirm`）
- [ ] 调用 `datasourceApi.delete(id)` → 刷新列表

### 4.8 错误处理与空状态

- [ ] 列表数据为空时显示 `el-empty` 组件（「暂无数据源，点击新增添加」）
- [ ] API 错误统一由 Axios 拦截器处理，页面级别仅处理业务逻辑
- [ ] 同步/测试操作中的异常单独 try/catch 并提示

---

## 5. 同步状态展示增强（可选）

- [ ] 同步按钮点击后：对应行状态显示「同步中」临时状态
- [ ] 可选：同步历史弹出侧边栏或对话框（使用 `getSyncHistory` 数据）
- [ ] 可选：支持查看最近一次同步的详细结果
- [ ] 可选：支持 Cron 表达式的合法性前端校验提示

---

> **统计**：共 5 大类，约 58 个子任务
