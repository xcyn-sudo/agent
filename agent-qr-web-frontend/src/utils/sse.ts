/**
 * SSE 流式请求工具
 * 封装 @microsoft/fetch-event-source，提供统一的 SSE 请求能力
 */
import { fetchEventSource, EventStreamContentType } from '@microsoft/fetch-event-source'
import { getAccessToken } from './token'

export interface SSEOptions {
  url: string
  method?: 'GET' | 'POST'
  body?: any
  headers?: Record<string, string>
  signal?: AbortSignal
  onMessage: (event: string, data: string) => void
  onError?: (error: string) => void
  onClose?: () => void
}

/**
 * 发起 SSE 流式请求
 * 支持 POST + 自定义 Header（含 Bearer Token）
 * 返回 AbortController 供调用方取消
 */
export function createSSERequest(options: SSEOptions): AbortController {
  const controller = new AbortController()
  const mergedSignal = options.signal
    ? combineSignals(options.signal, controller.signal)
    : controller.signal

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.headers,
  }

  const token = getAccessToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  fetchEventSource(options.url, {
    method: options.method || 'POST',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
    signal: mergedSignal,
    async onopen(response) {
      if (response.ok && response.headers.get('content-type')?.includes(EventStreamContentType)) {
        return // 连接成功
      }
      throw new Error(`SSE 连接失败: HTTP ${response.status}`)
    },
    onmessage(event) {
      options.onMessage(event.event, event.data)
    },
    onerror(err) {
      options.onError?.(err.message)
      throw err // 不自动重连
    },
    onclose() {
      options.onClose?.()
    },
  })

  return controller
}

/** 合并多个 AbortSignal */
function combineSignals(...signals: AbortSignal[]): AbortSignal {
  const controller = new AbortController()
  signals.forEach((signal) => {
    if (signal.aborted) {
      controller.abort(signal.reason)
      return
    }
    signal.addEventListener('abort', () => controller.abort(signal.reason))
  })
  return controller.signal
}
