<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { getUserFromStorage } from '@/utils/token'

const authStore = useAuthStore()
const appStore = useAppStore()

const user = computed(() => {
  const u = getUserFromStorage()
  return u ? (u as { username: string }).username : ''
})

function handleLogout() {
  authStore.logout()
}
</script>

<template>
  <header class="header-bar">
    <div class="header-bar__left">
      <el-icon class="collapse-btn" @click="appStore.toggleSidebar()">
        <Fold />
      </el-icon>
      <span class="system-name">Agent-QR 企业知识库</span>
    </div>
    <div class="header-bar__right">
      <span class="username">{{ user }}</span>
      <el-button type="danger" text @click="handleLogout">退出</el-button>
    </div>
  </header>
</template>

<style scoped lang="scss">
.header-bar {
  height: $header-height;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  z-index: 10;

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  &:hover {
    color: $primary-color;
  }
}

.system-name {
  font-size: $font-size-large;
  font-weight: 600;
  color: $text-primary;
}

.username {
  color: $text-regular;
}
</style>
