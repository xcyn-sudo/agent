const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'
const TOKEN_EXPIRES_KEY = 'token_expires_at'
const USER_KEY = 'auth_user'

// ==================== Access Token ====================

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function setAccessToken(token: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, token)
}

// ==================== Refresh Token ====================

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

// ==================== Token 过期时间戳 ====================

export function setTokenExpiresAt(timestamp: number): void {
  localStorage.setItem(TOKEN_EXPIRES_KEY, String(timestamp))
}

export function getTokenExpiresAt(): number | null {
  const val = localStorage.getItem(TOKEN_EXPIRES_KEY)
  return val ? Number(val) : null
}

// ==================== 清除所有 Token ====================

export function removeAllTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(TOKEN_EXPIRES_KEY)
  localStorage.removeItem(USER_KEY)
}

// ==================== 兼容旧版别名 ====================

export const getToken = getAccessToken
export const setToken = setAccessToken
export const removeToken = () => localStorage.removeItem(ACCESS_TOKEN_KEY)

// ==================== User 存取 ====================

export function getUserFromStorage() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function setUserToStorage(user: Record<string, unknown>): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function removeUserFromStorage(): void {
  localStorage.removeItem(USER_KEY)
}

export function getUserRoleFromLocalStorage(): string | null {
  const user = getUserFromStorage()
  return user ? (user as { role: string }).role : null
}
