import request from './index'
import type { ApiResult, UserInfo, LoginVO } from '@/types'

export const authApi = {
  login(data: { username: string; password: string }) {
    return request.post<any, ApiResult<LoginVO>>('/api/auth/login', data)
  },
  register(data: { username: string; password: string; realName?: string; email?: string; phone?: string }) {
    return request.post<any, ApiResult<void>>('/api/auth/register', data)
  },
  getUserInfo() {
    return request.get<any, ApiResult<UserInfo>>('/api/auth/info')
  },
  // ★ P2 新增：刷新 Access Token
  refreshToken(refreshToken: string) {
    return request.post<any, ApiResult<{ accessToken: string; refreshToken: string; expiresIn: number }>>('/api/auth/refresh', { refreshToken })
  },
  // ★ P2 新增：撤销 Refresh Token（登出）
  revokeToken() {
    return request.post<any, ApiResult<void>>('/api/auth/revoke')
  },
}
