<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { getUserRoleFromLocalStorage } from '@/utils/token'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const userRole = computed(() => getUserRoleFromLocalStorage())

const defaultActive = computed(() => {
  const path = route.path
  if (path.startsWith('/admin/knowledge')) return '/admin/knowledge'
  if (path.startsWith('/admin/users')) return '/admin/users'
  if (path.startsWith('/admin/dashboard')) return '/admin/dashboard'
  return '/chat'
})

const menuItems = computed(() => {
  const items = [
    { path: '/chat', title: '问答', icon: 'ChatDotRound' },
  ]
  if (userRole.value === 'admin') {
    items.push(
      { path: '/admin/knowledge', title: '知识库管理', icon: 'Document' },
      { path: '/admin/users', title: '用户管理', icon: 'User' },
      { path: '/admin/dashboard', title: '数据仪表盘', icon: 'DataAnalysis' },
    )
  }
  return items
})

function handleSelect(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="sidebar">
    <el-menu
      :default-active="defaultActive"
      :collapse="appStore.sidebarCollapsed"
      :router="false"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
      @select="handleSelect"
    >
      <template v-for="item in menuItems" :key="item.path">
        <el-menu-item :index="item.path">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </template>
    </el-menu>
  </div>
</template>

<style scoped lang="scss">
.sidebar {
  height: 100%;
  background-color: $sidebar-bg;
  overflow-y: auto;

  .el-menu {
    border-right: none;
    height: 100%;
  }
}
</style>
