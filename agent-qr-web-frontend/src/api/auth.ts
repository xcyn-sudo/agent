import request from './index'
import type { ApiResult, UserInfo } from '@/types'

export const authApi = {
  login(data: { username: string; password: string }) {
    return request.post<any, ApiResult<{ token: string; userId: number; username: string; role: string }>>('/api/auth/login', data)
  },
  register(data: { username: string; password: string; realName?: string; email?: string; phone?: string }) {
    return request.post<any, ApiResult<void>>('/api/auth/register', data)
  },
  getUserInfo() {
    return request.get<any, ApiResult<UserInfo>>('/api/auth/info')
  },
}
