<script setup lang="ts">
import { computed } from 'vue'
import type { SourceVO } from '@/types'

const props = defineProps<{
  role: 'user' | 'assistant'
  content: string
  sources?: SourceVO[] | null
  loading?: boolean
  streaming?: boolean
  feedback?: 'positive' | 'negative' | null
}>()

const emit = defineEmits<{
  feedback: [type: 'positive' | 'negative']
}>()

/**
 * 简单的 Markdown → HTML 转换
 * 1. 先转义 HTML 特殊字符
 * 2. 处理代码块 ```
 * 3. 处理行内代码 `
 * 4. 处理加粗 **text**
 * 5. 处理换行 \n → <br>
 */
function parseMarkdown(text: string): string {
  if (!text) return ''

  // 保护代码块内容，避免被后续规则错误处理
  const codeBlocks: string[] = []
  let processed = text.replace(/```(\w*)\n?([\s\S]*?)```/g, (_, lang, code) => {
    const idx = codeBlocks.length
    codeBlocks.push(
      `<pre><code>${escapeHtml(code.trim())}</code></pre>`
    )
    return `__CODE_BLOCK_${idx}__`
  })

  // 转义 HTML
  processed = escapeHtml(processed)

  // 行内代码
  processed = processed.replace(/`([^`]+)`/g, '<code>$1</code>')

  // 加粗
  processed = processed.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')

  // 换行
  processed = processed.replace(/\n/g, '<br>')

  // 还原代码块
  processed = processed.replace(/__CODE_BLOCK_(\d+)__/g, (_, idx) => {
    return codeBlocks[parseInt(idx)]
  })

  return processed
}

function escapeHtml(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

const renderedContent = computed(() => {
  if (props.loading && !props.streaming) return ''
  return parseMarkdown(props.content)
})

const hasSources = computed(() => {
  return props.sources && props.sources.length > 0
})

const showFeedback = computed(() => {
  return props.role === 'assistant' && !props.streaming && !props.loading && props.content
})
</script>

<template>
  <div
    class="message-bubble"
    :class="[
      `message-bubble--${role}`,
      { 'message-bubble--streaming': streaming },
    ]"
  >
    <!-- 加载状态（旧同步方式） -->
    <div v-if="loading && !streaming && role === 'assistant'" class="message-bubble__loading">
      <span class="loading-dots">思考中<span class="dot">.</span><span class="dot">.</span><span class="dot">.</span></span>
    </div>

    <!-- 正常内容 / 流式内容 -->
    <div v-else class="message-bubble__content">
      <div class="message-bubble__text" v-html="renderedContent" />

      <!-- 引用来源 -->
      <div v-if="hasSources" class="message-bubble__sources">
        <div class="message-bubble__sources-title">📎 引用来源：</div>
        <div class="message-bubble__sources-list">
          <el-popover
            v-for="(source, idx) in sources"
            :key="idx"
            placement="top"
            :width="360"
            trigger="hover"
          >
            <template #reference>
              <el-tag
                size="small"
                type="info"
                class="message-bubble__source-tag"
              >
                {{ source.documentTitle }}
              </el-tag>
            </template>
            <div class="message-bubble__source-detail">
              <div class="message-bubble__source-detail-title">{{ source.documentTitle }}</div>
              <div class="message-bubble__source-detail-content">{{ source.content }}</div>
            </div>
          </el-popover>
        </div>
      </div>

      <!-- 反馈评价按钮 -->
      <div v-if="showFeedback" class="message-bubble__feedback">
        <button
          class="feedback-btn"
          :class="{ 'feedback-btn--active': feedback === 'positive' }"
          :disabled="feedback === 'negative'"
          @click="emit('feedback', 'positive')"
        >
          👍 有帮助
        </button>
        <button
          class="feedback-btn"
          :class="{ 'feedback-btn--active': feedback === 'negative' }"
          :disabled="feedback === 'positive'"
          @click="emit('feedback', 'negative')"
        >
          👎 无帮助
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.message-bubble {
  display: flex;
  margin-bottom: 16px;

  &--user {
    justify-content: flex-end;
  }

  &--assistant {
    justify-content: flex-start;
  }

  // 流式输出光标
  &--streaming &__text::after {
    content: '▊';
    animation: cursor-blink 1s step-end infinite;
    color: $primary-color;
    font-weight: bold;
  }

  &__content {
    max-width: 70%;
  }

  &__text {
    padding: 10px 14px;
    border-radius: 12px;
    font-size: $font-size-base;
    line-height: 1.6;
    word-break: break-word;

    :deep(code) {
      background: rgba(0, 0, 0, 0.06);
      padding: 2px 6px;
      border-radius: 3px;
      font-family: var(--font-family-mono);
      font-size: var(--font-size-sm);
    }

    :deep(pre) {
      background: #282c34;
      color: #abb2bf;
      padding: 12px 16px;
      border-radius: 6px;
      overflow-x: auto;
      margin: 8px 0;

      code {
        background: transparent;
        padding: 0;
        color: inherit;
      }
    }

    :deep(strong) {
      font-weight: 600;
    }
  }

  &--user &__text {
    background-color: $primary-color;
    color: #fff;
    border-top-right-radius: 4px;
  }

  &--assistant &__text {
    background-color: #fff;
    border: 1px solid $border-color-light;
    border-top-left-radius: 4px;
    color: $text-primary;
  }

  &__loading {
    padding: 10px 14px;
    background-color: #fff;
    border: 1px solid $border-color-light;
    border-radius: 12px;
    border-top-left-radius: 4px;
    max-width: 70%;
  }

  &__sources {
    margin-top: 8px;
    padding: 10px 12px;
    background: $bg-color;
    border-radius: 6px;
  }

  &__sources-title {
    font-size: $font-size-small;
    color: $text-secondary;
    margin-bottom: 6px;
  }

  &__sources-list {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  &__source-tag {
    cursor: pointer;
  }

  &__source-detail {
    max-height: 200px;
    overflow-y: auto;
  }

  &__source-detail-title {
    font-size: $font-size-small;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 8px;
  }

  &__source-detail-content {
    font-size: $font-size-small;
    color: $text-regular;
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-word;
  }

  &__feedback {
    display: flex;
    gap: 8px;
    margin-top: 8px;
  }
}

.feedback-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border: 1px solid $border-color-light;
  border-radius: var(--radius-xs);
  background: #fff;
  font-size: var(--font-size-sm);
  color: $text-secondary;
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast);

  &:hover:not(:disabled) {
    background: $bg-color;
    border-color: $border-color;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &--active {
    color: $primary-color;
    border-color: $primary-color;
    background: rgba($primary-color, 0.05);
  }
}

// 加载动画
.loading-dots {
  color: $text-secondary;
  font-size: $font-size-base;

  .dot {
    animation: dot-blink 1.4s infinite;
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes dot-blink {
  0%, 20% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0; }
}

@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
