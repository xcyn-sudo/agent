<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  loading: boolean
  disabled: boolean
}>()

const emit = defineEmits<{
  send: [content: string]
}>()

const inputValue = ref('')

function handleSend() {
  const content = inputValue.value.trim()
  if (!content || props.loading || props.disabled) return
  emit('send', content)
  inputValue.value = ''
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}
</script>

<template>
  <div class="chat-input">
    <el-input
      v-model="inputValue"
      type="textarea"
      :rows="3"
      :disabled="loading || disabled"
      placeholder="请输入您的问题... (Enter 发送，Shift+Enter 换行)"
      resize="none"
      class="chat-input__textarea"
      @keydown="handleKeydown"
    />
    <el-button
      type="primary"
      :disabled="!inputValue.trim() || loading || disabled"
      :loading="loading"
      class="chat-input__btn"
      @click="handleSend"
    >
      发送
    </el-button>
  </div>
</template>

<style scoped lang="scss">
.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid $border-color-light;

  &__textarea {
    flex: 1;
  }

  &__btn {
    flex-shrink: 0;
    height: 40px;
    margin-bottom: 2px;
  }
}
</style>
