// API 通用响应
export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 分页
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 用户
export interface UserInfo {
  id: number
  username: string
  realName: string
  email: string
  phone: string
  role: string
  status: number
  createTime: string
  updateTime: string
}

// 文档（设计文档中名为 Document，避免与 DOM API 冲突命名为 DocumentInfo）
export interface DocumentInfo {
  id: number
  title: string
  fileName: string
  filePath: string
  fileType: string
  fileSize: number
  status: string
  uploadUserId: number
  errorMsg: string
  createTime: string
  updateTime: string
}

// 会话
export interface Conversation {
  id: number
  userId: number
  title: string
  messageCount: number
  createTime: string
  updateTime: string
}

// 消息
export interface Message {
  id: number
  conversationId: number
  role: 'user' | 'assistant'
  content: string
  sources: string
  createTime: string
}

// 问答响应
export interface AskResponse {
  answer: string
  conversationId: number
  sources: RetrievedDocument[]
}

// 检索来源
export interface RetrievedDocument {
  documentId: string
  documentTitle: string
  content: string
  similarity: number
}

// 仪表盘
export interface DashboardVO {
  todayQA: number
  todayNewUsers: number
  totalDocuments: number
  totalChunks: number
  totalUsers: number
  weeklyTrend: DailyStats[]
  docTypeDistribution: Record<string, number>
}

export interface DailyStats {
  id: number
  statDate: string
  qaCount: number
  userQuestionCount: number
  activeUserCount: number
  docUploadCount: number
}
