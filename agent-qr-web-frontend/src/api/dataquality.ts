import request from './index'
import type { ApiResult, PageResult, QualityReport } from '@/types'

export const dataqualityApi = {
  listReports(params: { page: number; size: number; blocked?: boolean }) {
    return request.get<any, ApiResult<PageResult<QualityReport>>>('/api/dataquality/reports', { params })
  },

  getReport(batchId: string) {
    return request.get<any, ApiResult<QualityReport>>(`/api/dataquality/reports/${batchId}`)
  },
}
