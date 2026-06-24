# P1 前端 — chat 模块任务清单

> 模块：智能问答模块（核心用户功能）
>
> 依赖：infra、auth
>
> 开发顺序：第 4 步

---

## 子任务

- [ ] **4.1 实现会话列表组件** (`components/chat/ConversationList.vue`)
  - 页面加载时调用 `chatApi.listConversations()` 获取会话列表
  - 显示会话标题（取第一个问题前 30 字，超长截断）
  - 显示最后更新时间
  - 「+ 新会话」按钮：创建空会话（视觉上新增一项，实际提问时创建）
  - 点击会话项 → 高亮选中 → 加载该会话消息
  - 悬停显示删除按钮 → 二次确认后调用 `chatApi.deleteConversation()`
  - 空状态：显示"暂无会话"

- [ ] **4.2 实现消息气泡组件** (`components/chat/MessageBubble.vue`)
  - Props：`role`（user/assistant）、`content`、`sources`
  - `user` 消息：右对齐，蓝色背景气泡
  - `assistant` 消息：左对齐，白色背景气泡
  - assistant 消息底部显示引用来源列表（`sources` JSON 解析）
  - 支持 Markdown 渲染（可选引入 `marked` 库）

- [ ] **4.3 实现输入框组件** (`components/chat/ChatInput.vue`)
  - 多行文本输入框（`el-input type="textarea"`）
  - Enter 发送，Shift+Enter 换行
  - 「发送」按钮（输入为空时禁用）
  - Props：`loading`（发送中禁用输入）

- [ ] **4.4 实现问答页** (`views/chat/ChatView.vue`)
  - 左侧：ConversationList（宽度约 280px，可折叠）
  - 右侧：消息列表 + 输入框
  - 默认选中最近会话；无会话时显示欢迎语
  - 切换会话时调用 `chatApi.getMessages(id)` 加载历史消息
  - 发送消息流程：
    1. 调用 `chatApi.ask(query, conversationId)`（同步请求）
    2. 显示 loading 状态（"思考中..."动画）
    3. 返回后渲染 AI 回答（含引用来源）
    4. 如果是新会话，更新 conversationId，刷新会话列表
  - 消息列表自动滚动到底部（新消息到达时）
  - 处理空检索结果：显示"知识库中暂无相关信息"

- [ ] **5.5 接入 MainLayout**
  - ChatView 使用 MainLayout（含侧边栏和顶栏）

---

## 验证标准

- [ ] 会话列表正确加载，点击切换正常
- [ ] 创建新会话 → 提问 → 会话出现在列表中
- [ ] 发送问题 → loading 动画 → 回答正确显示（含引用来源）
- [ ] 删除会话 → 二次确认 → 列表刷新
- [ ] 无会话/无消息时有合理的空状态提示
- [ ] 消息列表自动滚到底部

---

> 预计耗时：1.5 天
