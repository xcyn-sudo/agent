<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import HeaderBar from './HeaderBar.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>

<template>
  <div class="main-layout">
    <div class="main-layout__sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
      <Sidebar />
    </div>
    <div class="main-layout__right">
      <HeaderBar />
      <main class="main-layout__content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped lang="scss">
.main-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;

  &__sidebar {
    width: $sidebar-width;
    flex-shrink: 0;
    transition: width 0.3s;

    &.collapsed {
      width: $sidebar-collapsed-width;
    }
  }

  &__right {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  &__content {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    background-color: $bg-color;
  }
}
</style>
