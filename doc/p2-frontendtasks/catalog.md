# 知识目录模块 - P2 前端任务列表

> 对应设计文档：p2-frontend-design.md 第七章 7.6
> 涉及页面：`CatalogView.vue` ★
> 涉及组件：`CatalogTree.vue` ★
> 涉及 API：`api/catalog.ts` ★
> 模块类型：P2 新增（★）
> 状态：未开始

---

## 1. CatalogTree 组件（`components/catalog/CatalogTree.vue`）★

### 1.1 基本结构

- [ ] 新建文件 `components/catalog/CatalogTree.vue`
- [ ] 使用 Element Plus `el-tree` 组件
- [ ] 定义 props：`data: CatalogTree`
- [ ] 定义 props：`searchKeyword: string`（搜索关键词，用于高亮）
- [ ] 定义 emits：`entity-click(entity: EntityNode, source: SourceNode, domain: DomainNode)`
- [ ] 数据转换：将 `CatalogTree` 转为 `el-tree` 需要的 `data` 格式

### 1.2 树节点渲染

- [ ] **一级节点（业务域）**：显示 `📁 domainName (sourceCount 个数据源 · totalEntities 个实体)`
- [ ] 一级节点使用 `el-tree` 的 `node-key` 为 `domainName`
- [ ] 一级节点默认展开（`default-expand-all` 或指定展开的一级节点）
- [ ] **二级节点（数据源）**：显示 `📂 sourceName (sourceType)  最后同步: lastSyncAt · totalSynced 条`
- [ ] 二级节点默认折叠
- [ ] **三级节点（数据实体）**：显示 `📄 entityName  recordCount 条`
- [ ] 三级节点为叶子节点（不可展开）

### 1.3 自定义节点内容（slot）

- [ ] 使用 `el-tree` 的 `default` slot 自定义每个节点渲染
- [ ] 一级节点：加粗字体 + 业务域颜色标识
- [ ] 二级节点：正常字体 + 灰色同步信息 + 数据源类型标签
- [ ] 三级节点：正常字体 + 蓝色记录数标签
- [ ] 实体节点添加点击 hover 效果（cursor: pointer, 下划线）
- [ ] 点击实体节点触发 `emit('entity-click', entity, source, domain)`

### 1.4 搜索过滤

- [ ] 监听 `searchKeyword` prop 变化
- [ ] 使用 `el-tree` 的 `filter-node-method` 过滤节点
- [ ] 过滤逻辑：匹配 `domainName` / `sourceName` / `entityName`（大小写不敏感）
- [ ] 匹配到关键字的节点自动展开
- [ ] 匹配到的文本高亮显示（使用 `highlight` 样式或 `v-html`）

### 1.5 Props/Emits 类型

- [ ] `props.data: CatalogTree`（`{ domains: DomainNode[] }`）
- [ ] `props.loading: boolean`
- [ ] `emit('entity-click', entity: EntityNode, source: SourceNode, domain: DomainNode)`

---

## 2. CatalogView 页面（`views/catalog/CatalogView.vue`）★

### 2.1 页面结构

- [ ] 新建文件 `views/catalog/CatalogView.vue`
- [ ] 页面标题：「知识目录」
- [ ] 顶部搜索栏：`el-input` 搜索框（placeholder: 「搜索域/数据源/实体...」）
- [ ] 主体内容：`CatalogTree` 组件
- [ ] 搜索框带有防抖（300ms debounce）

### 2.2 状态管理

- [ ] `catalogData: Ref<CatalogTree | null>` 目录树数据
- [ ] `loading: Ref<boolean>` 加载状态
- [ ] `searchKeyword: Ref<string>` 搜索关键词
- [ ] `domainStats: Ref<any>` 域统计（可选显示概览）

### 2.3 数据加载

- [ ] `fetchCatalog()` 函数：调用 `catalogApi.getCatalogTree()`
- [ ] `onMounted` 时加载数据
- [ ] 加载状态传递给 `CatalogTree` 的 `loading` prop

### 2.4 实体详情弹窗

- [ ] 实现 `handleEntityClick(entity, source, domain)` 函数
- [ ] 弹出实体详情对话框（`el-dialog` 或 `el-drawer`）
- [ ] 对话框标题：`entity.entityName`
- [ ] 对话框内容：
  - [ ] 所属域：`domain.domainName`
  - [ ] 所属数据源：`source.sourceName`（`source.sourceType`）
  - [ ] 记录数量：`entity.recordCount`
  - [ ] 最后更新：`entity.lastUpdated`（格式化显示）
- [ ] 对话框底部：关闭按钮

### 2.5 搜索功能

- [ ] 搜索输入框 v-model 绑定 `searchKeyword`
- [ ] 使用 `useDebounceFn` 或手动 `setTimeout` 防抖 300ms
- [ ] `searchKeyword` 变化时传递给 `CatalogTree` 的 `searchKeyword` prop
- [ ] 搜索结果高亮匹配文本

### 2.6 域统计概览（可选）

- [ ] 树上方显示域统计概览卡片（可选）
- [ ] 卡片内容：总域数、总数据源数、总实体数
- [ ] 数据来自 `catalogApi.getDomainStats()`

### 2.7 错误处理与空状态

- [ ] 数据加载失败显示错误提示
- [ ] 树数据为空时显示 `el-empty`（「暂无知识目录数据」）
- [ ] 搜索无匹配结果时显示友好提示

---

> **统计**：共 2 大类，约 27 个子任务
