import request, { ensureValidToken } from './index'
import type { ApiResult, Conversation, Message, AskResponse, SourceVO } from '@/types'
import { fetchEventSource } from '@microsoft/fetch-event-source'

export const chatApi = {
  // [P1 保留] 同步问答
  ask(query: string, conversationId?: number) {
    return request.post<any, ApiResult<AskResponse>>('/api/chat/ask', { query, conversationId })
  },

  // ★ [P2 新增] SSE 流式问答
  askStream(
    query: string,
    domain: string | null,
    conversationId: number | null,
    callbacks: {
      onToken: (token: string) => void
      onDone: (data: { answer: string; conversationId: number; messageId: number; sources: SourceVO[] }) => void
      onError: (error: string) => void
    },
  ): AbortController {
    const controller = new AbortController()

    const doFetch = async () => {
      // ★ 主动刷新即将过期的 token（避免 SSE 请求发出后中途过期）
      const validToken = await ensureValidToken()
      if (!validToken) {
        callbacks.onError('登录已过期，请重新登录')
        return
      }

      fetchEventSource('/api/chat/ask/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${validToken}`,
        },
        body: JSON.stringify({ query, conversationId, domain }),
        signal: controller.signal,
        openWhenHidden: true,
        onmessage(event) {
          switch (event.event) {
            case 'token':
              callbacks.onToken(event.data)
              break
            case 'done':
              callbacks.onDone(JSON.parse(event.data))
              break
            case 'error':
              callbacks.onError(event.data)
              break
          }
        },
        onerror(err) {
          // 主动取消则静默返回
          if (controller.signal.aborted) {
            return
          }
          // ★ return（而非 throw）阻止 fetchEventSource 重试
          //    throw 反而会触发库的内部重试机制！
          controller.abort()
          callbacks.onError('连接异常，请重试')
          return
        },
      })
    }

    doFetch()
    return controller
  },

  // ★ [P2 新增] 提交反馈评价
  submitFeedback(messageId: number, feedback: 'positive' | 'negative', reason?: string) {
    return request.post<any, ApiResult<void>>(`/api/statistics/feedback/${messageId}`, { feedback, reason })
  },

  // 以下不变
  listConversations() {
    return request.get<any, ApiResult<Conversation[]>>('/api/chat/conversations')
  },

  getMessages(conversationId: number) {
    return request.get<any, ApiResult<Message[]>>(`/api/chat/conversations/${conversationId}/messages`)
  },

  deleteConversation(id: number) {
    return request.delete<any, ApiResult<void>>(`/api/chat/conversations/${id}`)
  },
}
