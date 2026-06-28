import type { Directive, DirectiveBinding } from 'vue'
import { useAuthStore } from '@/stores/auth'

/**
 * ABAC 细粒度权限指令（P3 新增）。
 * <p>
 * 通过检查 authStore 中对应的计算属性（如 canEditKnowledge、fieldLevel.salary）
 * 控制元素的可见性或可用性。
 * </p>
 *
 * <p><b>用法：</b></p>
 * <pre>
 * // 无权限时隐藏元素
 * &lt;button v-permission="'canEditKnowledge'"&gt;编辑&lt;/button&gt;
 *
 * // 无权限时禁用元素（不隐藏）
 * &lt;button v-permission:disable="'canDeleteKnowledge'"&gt;删除&lt;/button&gt;
 *
 * // 字段级权限
 * &lt;td v-permission="'fieldLevel.salary'"&gt;{{ salary }}&lt;/td&gt;
 * </pre>
 */
const permission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const authStore = useAuthStore()
    const permissionKey = binding.value as string

    if (!permissionKey) {
      console.warn('[v-permission] 缺少权限标识')
      return
    }

    const hasPermission = resolvePermission(authStore, permissionKey)

    if (binding.arg === 'disable') {
      // 无权限时禁用
      if (!hasPermission) {
        if (el instanceof HTMLButtonElement || el instanceof HTMLInputElement) {
          el.disabled = true
        }
        el.classList.add('is-permission-disabled')
        el.setAttribute('title', '权限不足')
      }
    } else {
      // 默认：无权限时隐藏
      if (!hasPermission) {
        el.style.display = 'none'
      }
    }
  },

  updated(el: HTMLElement, binding: DirectiveBinding) {
    const authStore = useAuthStore()
    const permissionKey = binding.value as string

    if (!permissionKey) return

    const hasPermission = resolvePermission(authStore, permissionKey)

    if (binding.arg === 'disable') {
      if (el instanceof HTMLButtonElement || el instanceof HTMLInputElement) {
        el.disabled = !hasPermission
      }
      if (!hasPermission) {
        el.classList.add('is-permission-disabled')
      } else {
        el.classList.remove('is-permission-disabled')
      }
    } else {
      el.style.display = hasPermission ? '' : 'none'
    }
  }
}

/**
 * 从 authStore 解析权限值。
 * <p>支持点分隔的嵌套路径，如 "fieldLevel.salary"。</p>
 */
function resolvePermission(authStore: ReturnType<typeof useAuthStore>, key: string): boolean {
  try {
    const keys = key.split('.')
    let value: any = authStore
    for (const k of keys) {
      value = value[k]
    }
    // computed 属性需要 .value 取值
    if (value && typeof value === 'object' && 'value' in value) {
      return !!value.value
    }
    return !!value
  } catch {
    return false
  }
}

export default permission
