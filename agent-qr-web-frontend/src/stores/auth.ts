import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import {
  getAccessToken,
  setAccessToken,
  getRefreshToken,
  setRefreshToken,
  getTokenExpiresAt,
  setTokenExpiresAt,
  removeAllTokens,
  getUserFromStorage,
  setUserToStorage,
} from '@/utils/token'
import { parseAllowedDomains } from '@/utils/format'

// ★ P2 ABAC 扩展用户主体
export interface UserPrincipal {
  id: number
  username: string
  realName: string
  role: string
  email: string
  phone: string
  // ★ P2 ABAC 字段
  department: string
  clearanceLevel: number
  allowedDomains: string[]
  title: string
}

export const useAuthStore = defineStore('auth', () => {
  const router = useRouter()

  // State
  const accessToken = ref<string | null>(getAccessToken())
  const refreshToken = ref<string | null>(getRefreshToken())
  const tokenExpiresAt = ref<number | null>(getTokenExpiresAt())
  const user = ref<UserPrincipal | null>(getUserFromStorage() as UserPrincipal | null)

  // Getters
  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'admin')

  // ★ P2 ABAC Getters
  function hasDomain(domain: string): boolean {
    return user.value?.allowedDomains?.includes(domain) ?? false
  }

  function hasClearance(level: number): boolean {
    return (user.value?.clearanceLevel ?? 0) >= level
  }

  function isManager(): boolean {
    return user.value?.title === 'manager'
  }

  function isDirector(): boolean {
    return user.value?.title === 'director'
  }

  // Actions
  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    const data = res.data
    // ★ 保存双 Token
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    tokenExpiresAt.value = Date.now() + data.expiresIn * 1000
    setAccessToken(data.accessToken)
    setRefreshToken(data.refreshToken)
    setTokenExpiresAt(Date.now() + data.expiresIn * 1000)

    // ★ 构建含 ABAC 属性的用户数据
    const userData: UserPrincipal = {
      id: data.userId,
      username: data.username,
      realName: '',
      role: data.role,
      email: '',
      phone: '',
      department: data.department || '',
      clearanceLevel: data.clearanceLevel || 0,
      allowedDomains: parseAllowedDomains(data.allowedDomains || ''),
      title: data.title || 'employee',
    }
    user.value = userData
    setUserToStorage(userData as any)
  }

  async function register(data: { username: string; password: string; realName?: string; email?: string; phone?: string }) {
    await authApi.register(data)
  }

  async function fetchUserInfo() {
    const res = await authApi.getUserInfo()
    const u = res.data
    const userData: UserPrincipal = {
      id: u.id,
      username: u.username,
      realName: u.realName,
      role: u.role,
      email: u.email,
      phone: u.phone,
      department: u.department || '',
      clearanceLevel: u.clearanceLevel || 0,
      allowedDomains: parseAllowedDomains(u.allowedDomains || ''),
      title: u.title || 'employee',
    }
    user.value = userData
    setUserToStorage(userData as any)
  }

  // ★ P2 新增：静默刷新 Access Token
  async function refreshAccessToken() {
    if (!refreshToken.value) {
      throw new Error('无可用的 Refresh Token')
    }
    const res = await authApi.refreshToken(refreshToken.value)
    const { accessToken: newAccess, refreshToken: newRefresh, expiresIn } = res.data
    accessToken.value = newAccess
    refreshToken.value = newRefresh
    tokenExpiresAt.value = Date.now() + expiresIn * 1000
    setAccessToken(newAccess)
    setRefreshToken(newRefresh)
    setTokenExpiresAt(Date.now() + expiresIn * 1000)
  }

  // ★ P2 升级：登出时撤销 Refresh Token
  async function logout() {
    try {
      await authApi.revokeToken()
    } catch {
      // 忽略撤销失败
    }
    accessToken.value = null
    refreshToken.value = null
    tokenExpiresAt.value = null
    user.value = null
    removeAllTokens()
    router.push('/login')
  }

  return {
    accessToken,
    refreshToken,
    tokenExpiresAt,
    user,
    isLoggedIn,
    isAdmin,
    login,
    register,
    fetchUserInfo,
    refreshAccessToken,
    logout,
    hasDomain,
    hasClearance,
    isManager,
    isDirector,
  }
})
