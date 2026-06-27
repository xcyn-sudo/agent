# 知识库模块 - P2 前端任务列表

> 对应设计文档：p2-frontend-design.md 第七章 7.2、第八章 8.5
> 涉及页面：`KnowledgeView.vue`
> 涉及组件：`UploadDialog.vue`、`DocumentTable.vue`、`StatusTag.vue`
> 模块类型：P1 升级（△）
> 状态：未开始

---

## 1. UploadDialog 组件升级（`components/knowledge/UploadDialog.vue`）

### 1.1 Props 扩展

- [ ] 新增 `allowedDomains: string[]` prop（当前用户可访问的业务域列表）
- [ ] 新增 `maxClearanceLevel: number` prop（当前用户的数据密级上限）

### 1.2 业务域选择器（★）

- [ ] 表单中新增「业务域」下拉选择器（`el-select`），标签为「业务域 *」
- [ ] 域下拉选项根据 `allowedDomains` prop 动态生成（仅显示用户有权访问的域）
- [ ] 域下拉绑定 `selectedDomain` ref，必填校验
- [ ] 域标签使用 `formatDomain` 显示中文名称

### 1.3 密级选择器（★）

- [ ] 表单中新增「密级」选择器（`el-radio-group` 或 `el-select`），标签为「密级 *」
- [ ] 密级选项根据 `maxClearanceLevel` prop 动态过滤（用户不能选择高于自己密级的级别）
- [ ] 密级选项从 `SENSITIVITY_LEVELS` 常量获取，显示标签文本
- [ ] 密级绑定 `selectedSensitivityLevel` ref，必填
- [ ] 密级标签使用 `.sensitivity-tag--*` 样式类区分颜色

### 1.4 上传参数传递

- [ ] `handleUpload` 确认时，传递 `domain` 参数（`selectedDomain.value`）
- [ ] `handleUpload` 确认时，传递 `sensitivityLevel` 参数（`selectedSensitivityLevel.value`）
- [ ] 调用 `knowledgeApi.upload(file, title, domain, sensitivityLevel)` 携带新参数

### 1.5 布局调整

- [ ] 表单布局调整：文件选择 → 文档标题 → 业务域 → 密级 → 操作按钮
- [ ] 必填标识（*）正确标注在业务域和密级字段

---

## 2. DocumentTable 组件升级（`components/knowledge/DocumentTable.vue`）

### 2.1 新增列

- [ ] `el-table` 新增「业务域」列（`prop="domain"`）
- [ ] 业务域列使用 `.domain-tag` 样式渲染（`el-tag` 或 `span`）
- [ ] `el-table` 新增「密级」列（`prop="sensitivityLabel"`）
- [ ] 密级列使用 `.sensitivity-tag--*` 样式根据密级值渲染不同颜色
- [ ] 密级列使用 `formatSensitivityLevel` 格式化显示

### 2.2 列顺序调整

- [ ] 调整列顺序为：文件名 → 类型 → 大小 → 业务域 → 密级 → 状态 → 上传时间 → 操作
- [ ] 类型列宽度适当调整（新增两列后压缩其他列宽度）

### 2.3 删除行为变更

- [ ] 删除按钮点击 → 调用 `knowledgeApi.deleteDocument(id)`（后端变为软删除）
- [ ] 删除成功后刷新列表（文档状态变为 DELETING，不会立即消失）
- [ ] 二次确认对话框文案调整为「确认删除该文档？删除后系统将异步清理相关数据」

---

## 3. StatusTag 组件升级（`components/knowledge/StatusTag.vue`）

### 3.1 新增状态

- [ ] `STATUS_COLORS` 映射新增 `DELETING` 状态
- [ ] `DELETING` 状态颜色：灰色（`color: #909399`）
- [ ] `DELETING` 状态图标：加载中旋转动画（`el-icon-loading` 或自定义 spinner）
- [ ] `DELETING` 状态标签文本：「删除中」

### 3.2 样式（如使用 scoped）

- [ ] 新增 `.status-tag--deleting` 样式类
- [ ] 包含灰色文字 + 半透明背景
- [ ] 可选：添加旋转加载小图标

---

## 4. KnowledgeView 页面升级（`views/knowledge/KnowledgeView.vue`）

### 4.1 筛选栏升级

- [ ] 筛选栏新增「业务域筛选」下拉（`el-select`），选项包含「全部」+ 用户 `allowedDomains`
- [ ] 筛选栏新增「密级筛选」下拉（`el-select`），选项包含「全部」+ 用户密级范围内的级别
- [ ] 筛选条件作为参数传递给 `knowledgeApi.listDocuments(params)`

### 4.2 域筛选逻辑

- [ ] 从 Auth Store 获取当前用户的 `allowedDomains` 和 `clearanceLevel`
- [ ] 业务域筛选下拉：选项为 `['全部', ...allowedDomains]`
- [ ] 密级筛选下拉：选项为 `['全部', ...SENSITIVITY_LEVELS.filter(l => l.value <= userClearanceLevel)]`

### 4.3 上传按钮改造

- [ ] `UploadDialog` 传递 `allowedDomains` prop
- [ ] `UploadDialog` 传递 `maxClearanceLevel` prop（当前用户密级）
- [ ] 上传成功后自动刷新列表

### 4.4 列表刷新逻辑

- [ ] 删除操作后列表刷新频率调整（软删除为异步，允许轮询或手动刷新）
- [ ] 可选：处于 DELETING 状态的文档不支持再次删除操作

### 4.5 搜索功能

- [ ] 搜索框保留（P1 无搜索框设计，可选 P2 增强）
- [ ] 如需搜索：支持按文件名 + 业务域 + 密级组合筛选

---

> **统计**：共 4 大类，约 32 个子任务
