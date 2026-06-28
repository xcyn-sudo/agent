import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  /** P3 新增：移动端抽屉开关 */
  const mobileDrawerOpen = ref(false)

  /** P3 新增：是否为移动端（宽度 < 768px） */
  const isMobile = computed(() => window.innerWidth < 768)

  function toggleSidebar() {
    if (isMobile.value) {
      mobileDrawerOpen.value = !mobileDrawerOpen.value
    } else {
      sidebarCollapsed.value = !sidebarCollapsed.value
    }
  }

  /** P3 新增：关闭移动端抽屉 */
  function closeMobileDrawer() {
    mobileDrawerOpen.value = false
  }

  return {
    sidebarCollapsed,
    mobileDrawerOpen,
    isMobile,
    toggleSidebar,
    closeMobileDrawer
  }
})
