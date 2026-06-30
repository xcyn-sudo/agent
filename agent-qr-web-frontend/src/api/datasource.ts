import request from './index'
import type { ApiResult, PageResult, DataSourceConfig, DataSourceForm, ConnectionTestResult, SyncRecord } from '@/types'

export const datasourceApi = {
  list(params: { page: number; size: number; domain?: string }) {
    return request.get<any, ApiResult<PageResult<DataSourceConfig>>>('/api/datasource/list', { params })
  },

  getById(id: number) {
    return request.get<any, ApiResult<DataSourceConfig>>(`/api/datasource/${id}`)
  },

  create(data: DataSourceForm) {
    return request.post<any, ApiResult<DataSourceConfig>>('/api/datasource', data)
  },

  update(id: number, data: Partial<DataSourceForm>) {
    return request.put<any, ApiResult<DataSourceConfig>>(`/api/datasource/${id}`, data)
  },

  delete(id: number) {
    return request.delete<any, ApiResult<void>>(`/api/datasource/${id}`)
  },

  testConnection(id: number) {
    return request.post<any, ApiResult<ConnectionTestResult>>(`/api/datasource/${id}/test`)
  },

  detectColumns(connectionConfig: string, tableName: string, sourceType?: string) {
    return request.post<any, ApiResult<string[]>>('/api/datasource/detect-columns', {
      connectionConfig,
      tableName: tableName || '',
      sourceType: sourceType || 'JDBC',
    })
  },

  triggerSync(id: number) {
    return request.post<any, ApiResult<void>>(`/api/datasource/${id}/sync`)
  },

  getSyncHistory(id: number, params: { page: number; size: number }) {
    return request.get<any, ApiResult<PageResult<SyncRecord>>>(`/api/datasource/${id}/sync-history`, { params })
  },
}
