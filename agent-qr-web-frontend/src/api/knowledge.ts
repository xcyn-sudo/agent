import request from './index'
import type { ApiResult, PageResult, DocumentInfo } from '@/types'

export const knowledgeApi = {
  // ★ P2 升级：新增 domain/sensitivityLevel 参数
  upload(file: File, title?: string, domain?: string, sensitivityLevel?: number) {
    const formData = new FormData()
    formData.append('file', file)
    if (title) formData.append('title', title)
    if (domain) formData.append('domain', domain)
    if (sensitivityLevel != null) formData.append('sensitivityLevel', String(sensitivityLevel))
    return request.post<any, ApiResult<DocumentInfo>>('/api/knowledge/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  listDocuments(params: { page: number; size: number; domain?: string; sensitivityLevel?: number; keyword?: string }) {
    return request.get<any, ApiResult<PageResult<DocumentInfo>>>('/api/knowledge/documents', { params })
  },

  getDocument(id: number) {
    return request.get<any, ApiResult<DocumentInfo>>(`/api/knowledge/documents/${id}`)
  },

  deleteDocument(id: number) {
    return request.delete<any, ApiResult<void>>(`/api/knowledge/documents/${id}`)
  },

  getStatus(id: number) {
    return request.get<any, ApiResult<{ status: string; errorMsg?: string }>>(`/api/knowledge/documents/${id}/status`)
  },

  getChunks(id: number) {
    return request.get<any, ApiResult<any[]>>(`/api/knowledge/documents/${id}/chunks`)
  },
}
