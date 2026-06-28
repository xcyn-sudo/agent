import { ref, computed, onMounted, onUnmounted, type Ref, type ComputedRef } from 'vue'

export type Breakpoint = 'mobile' | 'tablet' | 'desktop'

const MOBILE_MAX = 767
const TABLET_MAX = 1023

/**
 * 响应式断点检测 Composable（P3 新增）。
 * <p>
 * 使用 window.matchMedia 监听视口宽度变化，
 * 配合 CSS @media 查询实现移动端适配。
 * </p>
 *
 * @returns 断点信息与响应式标志
 */
export function useBreakpoint() {
  const width: Ref<number> = ref(window.innerWidth)

  const breakpoint: ComputedRef<Breakpoint> = computed(() => {
    if (width.value <= MOBILE_MAX) return 'mobile'
    if (width.value <= TABLET_MAX) return 'tablet'
    return 'desktop'
  })

  const isMobile: ComputedRef<boolean> = computed(() => width.value <= MOBILE_MAX)
  const isTablet: ComputedRef<boolean> = computed(() => width.value > MOBILE_MAX && width.value <= TABLET_MAX)
  const isDesktop: ComputedRef<boolean> = computed(() => width.value > TABLET_MAX)

  let debounceTimer: ReturnType<typeof setTimeout> | null = null

  function handleResize() {
    if (debounceTimer) clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => {
      width.value = window.innerWidth
    }, 200)
  }

  onMounted(() => {
    window.addEventListener('resize', handleResize)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    if (debounceTimer) clearTimeout(debounceTimer)
  })

  return {
    width,
    breakpoint,
    isMobile,
    isTablet,
    isDesktop
  }
}
