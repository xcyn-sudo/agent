import request from './index'
import type { ApiResult, CatalogTree } from '@/types'

export const catalogApi = {
  getCatalogTree() {
    return request.get<any, ApiResult<CatalogTree>>('/api/catalog/tree')
  },

  getDomainStats() {
    return request.get<any, ApiResult<{ totalDomains: number; totalSources: number; totalEntities: number }>>('/api/catalog/stats')
  },
}
