import { ref } from 'vue'
import { defineStore } from 'pinia'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import {
  getToken,
  setToken,
  removeToken,
  getUserFromStorage,
  setUserToStorage,
  removeUserFromStorage,
} from '@/utils/token'

interface AuthUser {
  id: number
  username: string
  realName: string
  role: string
  email: string
  phone: string
}

export const useAuthStore = defineStore('auth', () => {
  const router = useRouter()

  // State
  const token = ref<string | null>(getToken())
  const user = ref<AuthUser | null>(getUserFromStorage())

  // Actions
  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    const { token: newToken, userId, role: userRole } = res.data
    token.value = newToken
    setToken(newToken)

    const userData: AuthUser = {
      id: userId,
      username,
      realName: '',
      role: userRole,
      email: '',
      phone: '',
    }
    user.value = userData
    setUserToStorage(userData)
  }

  async function register(data: { username: string; password: string; realName?: string; email?: string; phone?: string }) {
    await authApi.register(data)
  }

  async function fetchUserInfo() {
    const res = await authApi.getUserInfo()
    const userData: AuthUser = {
      id: res.data.id,
      username: res.data.username,
      realName: res.data.realName,
      role: res.data.role,
      email: res.data.email,
      phone: res.data.phone,
    }
    user.value = userData
    setUserToStorage(userData)
  }

  function logout() {
    token.value = null
    user.value = null
    removeToken()
    removeUserFromStorage()
    router.push('/login')
  }

  return { token, user, login, register, fetchUserInfo, logout }
})
