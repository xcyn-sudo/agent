import request from './index'
import type { ApiResult, DashboardVO } from '@/types'

export const statisticsApi = {
  getDashboard() {
    return request.get<any, ApiResult<DashboardVO>>('/api/statistics/dashboard')
  },
}
