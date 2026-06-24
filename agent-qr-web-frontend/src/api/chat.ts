import request from './index'
import type { ApiResult, Conversation, Message, AskResponse } from '@/types'

export const chatApi = {
  ask(query: string, conversationId?: number) {
    return request.post<any, ApiResult<AskResponse>>('/api/chat/ask', { query, conversationId })
  },
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
