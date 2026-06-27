<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { getUserFromStorage, getUserRoleFromLocalStorage } from '@/utils/token'
import type { UserPrincipal } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const userRole = computed(() => getUserRoleFromLocalStorage())
const userInfo = computed(() => getUserFromStorage() as UserPrincipal | null)

const defaultActive = computed(() => {
  const path = route.path
  if (path.startsWith('/admin/knowledge')) return '/admin/knowledge'
  if (path.startsWith('/admin/users')) return '/admin/users'
  if (path.startsWith('/admin/dashboard')) return '/admin/dashboard'
  if (path.startsWith('/admin/datasource')) return '/admin/datasource'
  if (path.startsWith('/admin/catalog')) return '/admin/catalog'
  if (path.startsWith('/admin/quality')) return '/admin/quality'
  return '/chat'
})

const menuItems = computed(() => {
  const items = [
    { path: '/chat', title: '问答', icon: 'ChatDotRound' },
  ]

  // ★ admin 用户额外菜单
  if (userRole.value === 'admin') {
    items.push(
      { path: '/admin/knowledge', title: '知识库管理', icon: 'Document' },
      { path: '/admin/users', title: '用户管理', icon: 'User' },
      { path: '/admin/dashboard', title: '数据仪表盘', icon: 'DataAnalysis' },
      // ★ P2 新增菜单页
      { path: '/admin/datasource', title: '数据接入', icon: 'Connection' },
      { path: '/admin/catalog', title: '知识目录', icon: 'FolderOpened' },
      { path: '/admin/quality', title: '质量报告', icon: 'Warning' },
    )
  }

  // ★ 非 admin 用户如果有特定职级也可以访问部分页面
  if (userRole.value !== 'admin' && userInfo.value) {
    const { title } = userInfo.value
    if (title === 'manager' || title === 'director') {
      items.push(
        { path: '/admin/catalog', title: '知识目录', icon: 'FolderOpened' },
      )
    }
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
