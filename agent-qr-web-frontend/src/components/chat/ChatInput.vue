<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  loading: boolean
  disabled: boolean
  domains?: { value: string; label: string }[]
}>()

const emit = defineEmits<{
  send: [content: string, domain?: string]
  stop: []
}>()

const inputValue = ref('')
const selectedDomain = ref('')

function handleSend() {
  const content = inputValue.value.trim()
  if (!content || props.loading || props.disabled) return
  emit('send', content, selectedDomain.value || undefined)
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
    <el-select
      v-if="domains && domains.length > 0"
      v-model="selectedDomain"
      placeholder="全部域"
      class="chat-input__domain-select"
      :disabled="loading"
    >
      <el-option value="" label="全部域" />
      <el-option
        v-for="d in domains"
        :key="d.value"
        :value="d.value"
        :label="d.label"
      />
    </el-select>
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
      v-if="!loading"
      type="primary"
      :disabled="!inputValue.trim() || disabled"
      class="chat-input__btn"
      @click="handleSend"
    >
      发送
    </el-button>
    <el-button
      v-else
      type="danger"
      class="btn-stop-generate"
      @click="emit('stop')"
    >
      停止
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

  &__domain-select {
    width: 140px;
    flex-shrink: 0;
    margin-bottom: 2px;
  }

  &__textarea {
    flex: 1;
  }

  &__btn {
    flex-shrink: 0;
    height: 40px;
    margin-bottom: 2px;
  }
}

.btn-stop-generate {
  flex-shrink: 0;
  height: 40px;
  margin-bottom: 2px;
}
</style>
