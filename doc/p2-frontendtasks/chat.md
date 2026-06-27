# 问答模块 - P2 前端任务列表

> 对应设计文档：p2-frontend-design.md 第七章 7.1、第八章 8.1-8.2
> 涉及页面：`ChatView.vue`
> 涉及组件：`ChatInput.vue`、`MessageBubble.vue`、`ConversationList.vue`
> 模块类型：P1 升级（△）
> 状态：未开始

---

## 1. ChatInput 组件升级（`components/chat/ChatInput.vue`）

### 1.1 Props 与 Emits 扩展

- [ ] `props` 新增 `loading: boolean`（是否正在发送中）
- [ ] `props` 新增 `disabled: boolean`（是否禁用输入）
- [ ] `props` 新增 `domains: { value: string; label: string }[]`（可选业务域列表）
- [ ] `emits` 升级 `send` 事件签名：`[content: string, domain?: string]`
- [ ] `emits` 新增 `stop` 事件

### 1.2 域选择器

- [ ] 输入框左侧新增业务域下拉选择器（`el-select`）
- [ ] 域下拉默认选项为「全部域」（value 为空字符串）
- [ ] 域下拉选项根据 `props.domains` 动态渲染
- [ ] 选中域后 `selectedDomain` ref 同步更新
- [ ] `handleSend` 时透传 `selectedDomain` 值到 `emit('send', content, domain)`

### 1.3 停止生成按钮

- [ ] 发送按钮区域新增「停止」按钮（仅在 `props.loading === true` 时显示）
- [ ] 停止按钮使用 `.btn-stop-generate` 样式类
- [ ] 停止按钮点击触发 `emit('stop')`
- [ ] 非发送状态时仅显示「发送」按钮

### 1.4 交互细节

- [ ] `props.loading` 为 true 时禁用输入框和发送按钮
- [ ] Enter 键发送（Shift+Enter 换行）逻辑保留
- [ ] 空内容不允许发送（trim 后判断）

---

## 2. MessageBubble 组件升级（`components/chat/MessageBubble.vue`）

### 2.1 Props 扩展

- [ ] 新增 `sources?: SourceVO[]` prop（引用来源列表）
- [ ] 新增 `loading?: boolean` prop（是否加载中）
- [ ] 新增 `streaming?: boolean` prop（是否正在流式接收中）
- [ ] 新增 `feedback?: 'positive' | 'negative' | null` prop（已有反馈状态）

### 2.2 Emits 扩展

- [ ] 新增 `feedback` emit：`[type: 'positive' | 'negative']`

### 2.3 流式光标动画

- [ ] AI 消息气泡在 `streaming === true` 时添加 CSS class `message-bubble--streaming`
- [ ] 使用 `::after` 伪元素显示闪烁光标 `▊`
- [ ] 闪烁动画使用 `blink-cursor` keyframes（1s step-end infinite）

### 2.4 引用来源展示

- [ ] AI 消息底部（非 streaming 且非 loading 状态）展示引用来源列表
- [ ] 每条来源显示文档标题（`documentTitle`）+ 相似度百分比（`similarity`）
- [ ] 使用 `📎` 图标前缀
- [ ] 来源区域有上边框分隔（`border-top: 1px solid #eee`）
- [ ] 来源文档标题可点击（如果有文档预览链接则跳转）

### 2.5 反馈按钮

- [ ] AI 消息底部（非 streaming 且非 loading 状态）展示反馈按钮组
- [ ] 按钮组包含「👍 有帮助」和「👎 无帮助」两个按钮
- [ ] 已评价状态：对应按钮高亮（`.feedback-btn--active`），另一按钮禁用
- [ ] 点击「👍」→ `emit('feedback', 'positive')`
- [ ] 点击「👎」→ `emit('feedback', 'negative')`（后续由父组件处理弹窗）

### 2.6 模板结构调整

- [ ] `.message-bubble` 根元素根据 `role` 添加 class（`message-bubble--user` / `message-bubble--assistant`）
- [ ] `.message-bubble` 根元素根据 `streaming` 添加 class（`message-bubble--streaming`）
- [ ] 消息内容使用 `v-html` 或 `v-text` 渲染（支持 Markdown 转 HTML）

---

## 3. ChatView 页面升级（`views/chat/ChatView.vue`）

### 3.1 状态管理

- [ ] 新增 `sending: Ref<boolean>` 状态（是否正在发送/接收）
- [ ] 新增 `streamingContent: Ref<string>` 状态（当前流式接收的内容）
- [ ] 新增 `currentSources: Ref<SourceVO[]>` 状态（当前回答的引用来源）
- [ ] 新增 `abortController: AbortController | null` 引用
- [ ] 新增 `availableDomains` 域列表（从用户 `allowedDomains` 属性获取，映射为 `{ value, label }` 格式）

### 3.2 流式问答核心逻辑

- [ ] 新增 `LocalMessage` 接口（扩展 `Message`，新增 `streaming?: boolean`, `sources?: SourceVO[]`）
- [ ] 重写 `handleSend(content, domain?)` 函数
- [ ] `handleSend` 步骤①：添加用户消息到 `messages`（id 临时为负时间戳）
- [ ] `handleSend` 步骤②：添加空 assistant 消息（`streaming: true`, content: ''）
- [ ] `handleSend` 步骤③：调用 `chatApi.askStream(query, domain, conversationId, callbacks)`
- [ ] `handleSend` 步骤④：保存返回的 `AbortController` 到 `abortController`
- [ ] `onToken` 回调：追加 token 到 assistant 消息 `content` 字段
- [ ] `onToken` 回调：触发响应式更新 + 自动滚动到底部
- [ ] `onDone` 回调：设置 `streaming: false`，更新 `sources`、`id`
- [ ] `onDone` 回调：如果无 `activeConversationId`，设置为返回的 `conversationId` 并刷新会话列表
- [ ] `onDone` 回调：设置 `sending = false`，清空 `abortController`
- [ ] `onError` 回调：设置 `streaming: false`，显示错误消息，`sending = false`，清空 `abortController`

### 3.3 停止生成

- [ ] 实现 `handleStopGeneration()` 函数
- [ ] 调用 `abortController?.abort()`
- [ ] 在最后一条 AI 消息末尾追加 `\n\n_（已停止生成）_`
- [ ] 设置 `streaming: false`, `sending: false`

### 3.4 反馈评价交互

- [ ] 实现 `handleFeedback(messageId: number, type: 'positive' | 'negative')` 函数
- [ ] 正面反馈：直接调用 `chatApi.submitFeedback(messageId, 'positive')` → 提示「感谢反馈」
- [ ] 负面反馈：弹出原因选择对话框（`ElMessageBox` 或自定义弹窗）
- [ ] 负面反馈对话框包含预设原因选项：回答不准确 / 不完整 / 与问题无关 / 其他
- [ ] 负面反馈提交：调用 `chatApi.submitFeedback(messageId, 'negative', reason)` → 提示「感谢反馈，我们会持续改进」
- [ ] 更新消息的反馈状态（`feedback` 字段），防止重复评价

### 3.5 模板改造

- [ ] 替换原有的同步问答调用为流式调用（`handleSend` 改为新实现）
- [ ] 传递 `availableDomains` 给 `ChatInput` 的 `domains` prop
- [ ] 传递 `loading` prop 给 `ChatInput`（`sending` 状态）
- [ ] 绑定 `ChatInput` 的 `@send` → `handleSend`
- [ ] 绑定 `ChatInput` 的 `@stop` → `handleStopGeneration`
- [ ] `MessageBubble` 传递 `streaming` prop
- [ ] `MessageBubble` 传递 `sources` prop
- [ ] `MessageBubble` 传递 `feedback` prop（已有评价状态）
- [ ] `MessageBubble` 绑定 `@feedback` → `handleFeedback`
- [ ] 自动滚动到底部逻辑（`watch` messages 变化 + `nextTick`）

### 3.6 边角情况处理

- [ ] 发送中不允许再次发送（`sending.value === true` 时 `handleSend` 直接 return）
- [ ] 页面切换/卸载时取消进行中的 SSE 请求（`onUnmounted` 中 `abortController?.abort()`）
- [ ] SSE 连接超时处理（根据 `VITE_SSE_TIMEOUT` 配置）
- [ ] P1 同步问答 `ask()` 方法保留（向下兼容，但前端默认使用 SSE）

---

## 4. ConversationList（`components/chat/ConversationList.vue`）

- [ ] 不变（P1 设计保持不变，无 P2 改动）

---

> **统计**：共 4 大类，约 58 个子任务
