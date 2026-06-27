import request from './index'
import type { ApiResult, PageResult, UserInfo } from '@/types'

export const userApi = {
  listUsers(params: { page: number; size: number; keyword?: string; department?: string; title?: string }) {
    return request.get<any, ApiResult<PageResult<UserInfo>>>('/api/admin/users', { params })
  },
  createUser(data: {
    username: string
    password: string
    realName?: string
    email?: string
    phone?: string
    role?: string
    // ★ P2 ABAC 字段
    department?: string
    clearanceLevel?: number
    allowedDomains?: string
    title?: string
  }) {
    return request.post<any, ApiResult<void>>('/api/admin/users', data)
  },
  updateUser(
    id: number,
    data: {
      realName?: string
      email?: string
      phone?: string
      role?: string
      // ★ P2 ABAC 字段
      department?: string
      clearanceLevel?: number
      allowedDomains?: string
      title?: string
    },
  ) {
    return request.put<any, ApiResult<void>>(`/api/admin/users/${id}`, data)
  },
  toggleStatus(id: number, status: number) {
    return request.put<any, ApiResult<void>>(`/api/admin/users/${id}/status`, { status })
  },
}
