# P1 前端 — knowledge 模块任务清单

> 模块：知识库管理模块
>
> 依赖：infra、auth
>
> 开发顺序：第 5 步
>
> 权限：仅 ADMIN 角色可访问

---

## 子任务

- [ ] **5.1 实现文档状态标签组件** (`components/knowledge/StatusTag.vue`)
  - Props：`status: string`
  - 映射：
    - `UPLOADED` → 灰色标签"已上传"
    - `PARSING` → 蓝色标签 + loading 图标"解析中"
    - `CHUNKING` → 蓝色标签 + loading 图标"切片中"
    - `EMBEDDING` → 蓝色标签 + loading 图标"向量化中"
    - `READY` → 绿色标签"就绪"
    - `FAILED` → 红色标签"失败" + Tooltip 显示 errorMsg
    - `DELETING` → 橙色标签"删除中"

- [ ] **5.2 实现上传文档弹窗** (`components/knowledge/UploadDialog.vue`)
  - `el-dialog` 弹窗
  - 文件选择区域：支持点击选择 + 拖拽上传（`el-upload`）
  - 文档标题输入框（选填，默认取文件名）
  - 上传前校验：
    - 文件类型：仅 pdf/docx/txt/md
    - 文件大小：≤ 50MB
  - 上传进度显示（`el-progress`）
  - 上传成功 → 关闭弹窗 → 触发父组件刷新列表
  - 上传失败 → toast 显示错误消息

- [ ] **5.3 实现文档列表表格** (`components/knowledge/DocumentTable.vue`)
  - 表格列：文件名、类型、大小、状态（StatusTag）、上传时间、操作
  - 文件大小使用 `formatFileSize()` 格式化
  - 操作列：「删除」按钮
  - 删除需二次确认 → `knowledgeApi.deleteDocument(id)` → 刷新列表
  - 空状态："暂无文档，请上传"

- [ ] **5.4 实现知识库管理页** (`views/knowledge/KnowledgeView.vue`)
  - 「+ 上传文档」按钮 → 打开 UploadDialog
  - DocumentTable 列表展示
  - Pagination 分页（调用 `knowledgeApi.listDocuments(page, size)`）
  - 上传成功后自动刷新第 1 页
  - 页面标题："知识库管理"

- [ ] **5.5 添加路由配置**
  - 路由路径：`/admin/knowledge`
  - meta：`requiresAuth: true`、`requiresAdmin: true`
  - 标题："知识库管理"

---

## 验证标准

- [ ] 文档列表正确加载（分页正常）
- [ ] 上传 PDF/DOCX/TXT/MD → 成功 → 列表刷新
- [ ] 上传不支持的文件类型 → 前端拦截提示
- [ ] 上传超大文件（> 50MB）→ 前端拦截提示
- [ ] 删除文档 → 二次确认 → 列表刷新
- [ ] 文档状态标签颜色和文字正确
- [ ] 非 admin 用户无法访问 `/admin/knowledge`（跳转 403）

---

> 预计耗时：1 天
