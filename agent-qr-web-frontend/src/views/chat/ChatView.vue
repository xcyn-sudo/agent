<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { chatApi } from '@/api/chat'
import type { Conversation, Message, AskResponse, RetrievedDocument } from '@/types'
import ConversationList from '@/components/chat/ConversationList.vue'
import MessageBubble from '@/components/chat/MessageBubble.vue'
import ChatInput from '@/components/chat/ChatInput.vue'

// ========== 本地消息类型 ==========
interface LocalMessage {
  id: number | string
  role: 'user' | 'assistant'
  content: string
  sources?: RetrievedDocument[]
  loading?: boolean
}

// ========== 状态 ==========
const conversations = ref<Conversation[]>([])
const conversationsLoading = ref(false)
const activeConversationId = ref<number | null>(null)
const messages = ref<LocalMessage[]>([])
const sending = ref(false)
const messagesLoading = ref(false)
const deletingId = ref<number | null>(null)
const sidebarCollapsed = ref(false)
const messagesContainerRef = ref<HTMLElement | null>(null)

// ========== 工具函数 ==========
function parseSources(sourcesStr: string): RetrievedDocument[] {
  if (!sourcesStr) return []
  try {
    const parsed = JSON.parse(sourcesStr)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

// ========== 加载会话列表 ==========
async function loadConversations() {
  conversationsLoading.value = true
  try {
    const res = await chatApi.listConversations()
    conversations.value = res.data || []
  } catch {
    conversations.value = []
  } finally {
    conversationsLoading.value = false
  }
}

// ========== 加载历史消息 ==========
async function loadMessages(conversationId: number) {
  messagesLoading.value = true
  try {
    const res = await chatApi.getMessages(conversationId)
    const rawMessages: Message[] = res.data || []
    messages.value = rawMessages.map((msg) => ({
      id: msg.id,
      role: msg.role,
      content: msg.content,
      sources: msg.role === 'assistant' ? parseSources(msg.sources) : undefined,
    }))
  } catch {
    messages.value = []
  } finally {
    messagesLoading.value = false
  }
}

// ========== 选中会话 ==========
async function handleSelectConversation(conversationId: number) {
  if (activeConversationId.value === conversationId) return
  activeConversationId.value = conversationId
  await loadMessages(conversationId)
  scrollToBottom()
}

// ========== 创建新会话 ==========
function handleCreateConversation() {
  activeConversationId.value = null
  messages.value = []
  sidebarCollapsed.value = false
}

// ========== 删除会话 ==========
async function handleDeleteConversation(conversationId: number) {
  try {
    await chatApi.deleteConversation(conversationId)
    conversations.value = conversations.value.filter((c) => c.id !== conversationId)

    // 如果删除的是当前选中的会话，回退到欢迎页
    if (activeConversationId.value === conversationId) {
      activeConversationId.value = null
      messages.value = []
    }
  } catch {
    // 错误已在拦截器中处理
  } finally {
    deletingId.value = null
  }
}

// ========== 发送消息 ==========
async function handleSend(content: string) {
  if (!content.trim() || sending.value) return

  const query = content.trim()
  const conversationId = activeConversationId.value

  // 1. 添加用户消息
  const userMsgId = -Date.now()
  messages.value.push({
    id: userMsgId,
    role: 'user',
    content: query,
  })

  // 2. 添加 loading 消息
  const loadingMsgId = -(Date.now() + 1)
  messages.value.push({
    id: loadingMsgId,
    role: 'assistant',
    content: '',
    loading: true,
  })

  sending.value = true
  scrollToBottom()

  try {
    const res = await chatApi.ask(query, conversationId ?? undefined)
    const data: AskResponse = res.data

    // 3. 替换 loading 消息为实际回答
    const loadingIndex = messages.value.findIndex((m) => m.id === loadingMsgId)
    if (loadingIndex !== -1) {
      const answerContent = data.answer || ''
      const hasSources = data.sources && data.sources.length > 0

      if (!answerContent && !hasSources) {
        // 空检索结果
        messages.value[loadingIndex] = {
          id: data.conversationId || loadingMsgId,
          role: 'assistant',
          content: '知识库中暂无相关信息，请联系管理员上传相关文档',
          sources: [],
        }
      } else {
        messages.value[loadingIndex] = {
          id: data.conversationId || loadingMsgId,
          role: 'assistant',
          content: answerContent,
          sources: data.sources || [],
        }
      }
    }

    // 4. 如果之前没有选中会话（新会话），更新 activeConversationId
    if (!activeConversationId.value && data.conversationId) {
      activeConversationId.value = data.conversationId
      // 刷新会话列表以获取新会话
      await loadConversations()
    }
  } catch {
    // 5. 错误处理
    const loadingIndex = messages.value.findIndex((m) => m.id === loadingMsgId)
    if (loadingIndex !== -1) {
      messages.value[loadingIndex] = {
        id: loadingMsgId,
        role: 'assistant',
        content: '抱歉，请求失败，请重试',
        sources: [],
      }
    }
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

// ========== 滚动到底部 ==========
function scrollToBottom() {
  nextTick(() => {
    const container = messagesContainerRef.value
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  })
}

// ========== 切换会话时滚动 ==========
watch(activeConversationId, () => {
  if (activeConversationId.value !== null) {
    nextTick(() => scrollToBottom())
  }
})

// ========== 初始化 ==========
onMounted(() => {
  loadConversations().then(() => {
    // 默认选中最近会话（第一个）
    if (conversations.value.length > 0) {
      handleSelectConversation(conversations.value[0].id)
    }
  })
})
</script>

<template>
  <div class="chat-view">
    <!-- 侧边栏切换按钮 -->
    <div
      class="chat-view__toggle"
      :class="{ 'chat-view__toggle--collapsed': sidebarCollapsed }"
      @click="sidebarCollapsed = !sidebarCollapsed"
    >
      <span v-if="sidebarCollapsed">☰</span>
      <span v-else>✕</span>
    </div>

    <!-- 左侧会话列表 -->
    <div class="chat-view__sidebar" :class="{ 'chat-view__sidebar--collapsed': sidebarCollapsed }">
      <ConversationList
        :conversations="conversations"
        :active-id="activeConversationId ?? undefined"
        :loading="conversationsLoading"
        v-model:deleting-id="deletingId"
        @select="handleSelectConversation"
        @delete="handleDeleteConversation"
        @create="handleCreateConversation"
      />
    </div>

    <!-- 右侧聊天区 -->
    <div class="chat-view__main">
      <!-- 欢迎语（无会话且无消息时显示） -->
      <div v-if="activeConversationId === null && messages.length === 0" class="chat-view__welcome">
        <div class="chat-view__welcome-icon">🤖</div>
        <h2 class="chat-view__welcome-title">您好！我是企业知识库助手</h2>
        <p class="chat-view__welcome-desc">请问有什么可以帮助您的？</p>
      </div>

      <!-- 聊天消息区 -->
      <template v-else>
        <div ref="messagesContainerRef" class="chat-view__messages">
          <div v-if="messagesLoading" class="chat-view__messages-loading">
            <el-skeleton :rows="3" animated />
          </div>
          <template v-else>
            <div v-if="messages.length === 0" class="chat-view__messages-empty">
              <el-empty description="暂无消息，开始提问吧" />
            </div>
            <MessageBubble
              v-for="msg in messages"
              :key="msg.id"
              :role="msg.role"
              :content="msg.content"
              :sources="msg.sources"
              :loading="msg.loading"
            />
          </template>
        </div>
      </template>

      <!-- 底部输入区 -->
      <ChatInput
        :loading="sending"
        :disabled="false"
        @send="handleSend"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.chat-view {
  display: flex;
  height: 100%;
  position: relative;

  &__toggle {
    position: absolute;
    top: 12px;
    left: 284px;
    z-index: 10;
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fff;
    border: 1px solid $border-color;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
    color: $text-regular;
    transition: left 0.3s;

    &--collapsed {
      left: 12px;
    }
  }

  &__sidebar {
    width: 280px;
    flex-shrink: 0;
    height: 100%;
    overflow: hidden;
    transition: width 0.3s;

    &--collapsed {
      width: 0;
    }
  }

  &__main {
    flex: 1;
    display: flex;
    flex-direction: column;
    height: 100%;
    min-width: 0;
  }

  &__welcome {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 20px;
  }

  &__welcome-icon {
    font-size: 64px;
    margin-bottom: 16px;
  }

  &__welcome-title {
    font-size: 22px;
    color: $text-primary;
    margin-bottom: 8px;
  }

  &__welcome-desc {
    font-size: $font-size-base;
    color: $text-secondary;
  }

  &__messages {
    flex: 1;
    overflow-y: auto;
    padding: 16px 20px;
  }

  &__messages-loading {
    padding: 20px;
  }

  &__messages-empty {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
  }
}
</style>
