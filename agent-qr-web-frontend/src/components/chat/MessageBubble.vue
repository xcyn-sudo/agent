<script setup lang="ts">
import { computed } from 'vue'
import type { RetrievedDocument } from '@/types'

const props = defineProps<{
  role: 'user' | 'assistant'
  content: string
  sources?: RetrievedDocument[] | null
  loading?: boolean
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
  if (props.loading) return ''
  return parseMarkdown(props.content)
})

const hasSources = computed(() => {
  return props.sources && props.sources.length > 0
})
</script>

<template>
  <div class="message-bubble" :class="`message-bubble--${role}`">
    <!-- 加载状态 -->
    <div v-if="loading && role === 'assistant'" class="message-bubble__loading">
      <span class="loading-dots">思考中<span class="dot">.</span><span class="dot">.</span><span class="dot">.</span></span>
    </div>

    <!-- 正常内容 -->
    <div v-else class="message-bubble__content">
      <div class="message-bubble__text" v-html="renderedContent" />

      <!-- 引用来源 -->
      <div v-if="hasSources" class="message-bubble__sources">
        <div class="message-bubble__sources-title">📎 引用来源：</div>
        <div class="message-bubble__sources-list">
          <el-tag
            v-for="(source, idx) in sources"
            :key="idx"
            size="small"
            type="info"
            class="message-bubble__source-tag"
          >
            {{ source.documentTitle }}
          </el-tag>
        </div>
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
      font-family: 'Consolas', 'Monaco', monospace;
      font-size: 13px;
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
    cursor: default;
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
</style>
