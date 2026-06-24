<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import type { Conversation } from '@/types'
import { truncateText, formatDateTime } from '@/utils/format'

const props = defineProps<{
  conversations: Conversation[]
  activeId?: number
  loading: boolean
}>()

const emit = defineEmits<{
  select: [conversationId: number]
  delete: [conversationId: number]
  create: []
}>()

const deletingId = defineModel<number | null>('deletingId', { default: null })

async function handleDelete(id: number, event: Event) {
  event.stopPropagation()
  try {
    await ElMessageBox.confirm('确定删除此会话？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    deletingId.value = id
    emit('delete', id)
  } catch {
    // 用户取消
  }
}
</script>

<template>
  <div class="conversation-list">
    <div class="conversation-list__header">
      <el-button
        type="primary"
        class="conversation-list__create-btn"
        :loading="loading"
        @click="emit('create')"
      >
        + 新会话
      </el-button>
    </div>

    <div class="conversation-list__body">
      <div v-if="conversations.length === 0 && !loading" class="conversation-list__empty">
        <el-empty description="暂无会话" />
      </div>

      <div
        v-for="conv in conversations"
        :key="conv.id"
        class="conversation-list__item"
        :class="{ 'conversation-list__item--active': conv.id === activeId }"
        @click="emit('select', conv.id)"
      >
        <div class="conversation-list__item-content">
          <div class="conversation-list__item-title">
            {{ truncateText(conv.title, 30) || '新会话' }}
          </div>
          <div class="conversation-list__item-meta">
            <span class="conversation-list__item-count">{{ conv.messageCount }} 条消息</span>
            <span class="conversation-list__item-time">{{ formatDateTime(conv.updateTime) }}</span>
          </div>
        </div>
        <el-button
          class="conversation-list__item-delete"
          :loading="deletingId === conv.id"
          text
          size="small"
          @click="handleDelete(conv.id, $event)"
        >
          <span v-if="deletingId !== conv.id">&times;</span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.conversation-list {
  width: 280px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid $border-color-light;

  &__header {
    padding: 12px;
    flex-shrink: 0;
  }

  &__create-btn {
    width: 100%;
  }

  &__body {
    flex: 1;
    overflow-y: auto;
    padding: 0 8px 8px;
  }

  &__empty {
    padding-top: 40px;
  }

  &__item {
    display: flex;
    align-items: center;
    padding: 10px 12px;
    border-radius: 6px;
    cursor: pointer;
    transition: background-color 0.2s;
    margin-bottom: 4px;

    &:hover {
      background-color: $bg-color;

      .conversation-list__item-delete {
        opacity: 1;
      }
    }

    &--active {
      background-color: lighten($primary-color, 40%);

      &:hover {
        background-color: lighten($primary-color, 40%);
      }
    }
  }

  &__item-content {
    flex: 1;
    min-width: 0;
  }

  &__item-title {
    font-size: $font-size-base;
    color: $text-primary;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin-bottom: 4px;
  }

  &__item-meta {
    display: flex;
    justify-content: space-between;
    font-size: $font-size-small;
    color: $text-secondary;
  }

  &__item-count {
    flex-shrink: 0;
  }

  &__item-time {
    flex-shrink: 0;
  }

  &__item-delete {
    opacity: 0;
    flex-shrink: 0;
    margin-left: 4px;
    font-size: 16px;
    color: $text-secondary;
    transition: opacity 0.2s;

    &:hover {
      color: $danger-color;
    }
  }
}
</style>
