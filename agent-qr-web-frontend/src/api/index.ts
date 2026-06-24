import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken, removeUserFromStorage } from '@/utils/token'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
})

// 请求拦截器：自动添加 Authorization 头
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    // 401: Token 无效或过期
    if (res.code === 401) {
      removeToken()
      removeUserFromStorage()
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
      return Promise.reject(new Error(res.message || '未授权'))
    }
    // 403: 权限不足
    if (res.code === 403) {
      ElMessage.error('权限不足')
      return Promise.reject(new Error(res.message || '权限不足'))
    }
    // 500: 服务器错误
    if (res.code === 500) {
      ElMessage.error('服务器内部错误')
      return Promise.reject(new Error(res.message || '服务器内部错误'))
    }
    // 其他业务错误码
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    // 网络异常
    ElMessage.error('网络连接失败')
    return Promise.reject(error)
  },
)

export default request
