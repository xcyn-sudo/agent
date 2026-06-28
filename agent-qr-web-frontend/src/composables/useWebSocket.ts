import { ref, onUnmounted, type Ref } from 'vue'

export type ConnectionState = 'disconnected' | 'connecting' | 'connected'

/**
 * WebSocket STOMP 连接管理 Composable（P3 新增）。
 * <p>
 * 使用 STOMP over WebSocket 实现双向通信，
 * 连接失败时保留降级到 SSE 的能力。
 * </p>
 *
 * @param brokerURL WebSocket broker URL（默认 /ws）
 * @returns 连接管理方法与状态
 */
export function useWebSocket(brokerURL?: string) {
  const connectionState: Ref<ConnectionState> = ref('disconnected')
  // @ts-ignore — stompjs 和 sockjs-client 由 pnpm 安装后可用
  let stompClient: any = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let reconnectAttempts = 0
  const maxReconnectAttempts = 10

  const wsUrl = brokerURL || `${import.meta.env.VITE_WS_URL || 'http://localhost:9090'}/ws`

  /**
   * 建立 STOMP over WebSocket 连接。
   *
   * @param token JWT access token
   */
  async function connect(token: string): Promise<void> {
    if (connectionState.value === 'connected' || connectionState.value === 'connecting') {
      return
    }
    connectionState.value = 'connecting'

    try {
      // 动态导入 stompjs（避免编译时依赖缺失）
      const { Client } = await import('@stomp/stompjs')
      // SockJS 用于不支持原生 WebSocket 的浏览器
      const SockJS = (await import('sockjs-client')).default

      stompClient = new Client({
        webSocketFactory: () => new SockJS(wsUrl),
        connectHeaders: {
          Authorization: `Bearer ${token}`
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        debug: (msg: string) => {
          if (import.meta.env.DEV) {
            console.debug('[STOMP]', msg)
          }
        },

        onConnect: () => {
          connectionState.value = 'connected'
          reconnectAttempts = 0
          console.log('[WebSocket] 已连接')
        },

        onDisconnect: () => {
          connectionState.value = 'disconnected'
          console.log('[WebSocket] 已断开')
        },

        onStompError: (frame: any) => {
          console.error('[STOMP] 错误:', frame.headers?.message || frame)
          connectionState.value = 'disconnected'
        },

        onWebSocketClose: () => {
          connectionState.value = 'disconnected'
          if (reconnectAttempts < maxReconnectAttempts) {
            scheduleReconnect(token)
          }
        }
      })

      stompClient.activate()
    } catch (e) {
      console.warn('[WebSocket] STOMP 初始化失败，将降级使用 SSE:', e)
      connectionState.value = 'disconnected'
    }
  }

  /** 计划重连 */
  function scheduleReconnect(token: string) {
    if (reconnectTimer) return
    connectionState.value = 'connecting'
    const delay = Math.min(5000 * Math.pow(2, reconnectAttempts), 60000)
    console.log(`[WebSocket] ${delay / 1000}s 后重连 (attempt ${reconnectAttempts + 1})`)
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      reconnectAttempts++
      connect(token)
    }, delay)
  }

  /** 断开连接 */
  function disconnect(): void {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (stompClient) {
      try {
        stompClient.deactivate()
      } catch (e) {
        console.warn('[WebSocket] 断开异常:', e)
      }
      stompClient = null
    }
    connectionState.value = 'disconnected'
  }

  /**
   * 发送消息到 STOMP 目标。
   *
   * @param destination STOMP 目标路径（如 /app/chat/ask）
   * @param body 消息体
   */
  function send(destination: string, body: unknown): void {
    if (!stompClient || connectionState.value !== 'connected') {
      console.warn('[WebSocket] 未连接，无法发送消息')
      return
    }
    stompClient.publish({
      destination,
      body: typeof body === 'string' ? body : JSON.stringify(body)
    })
  }

  /**
   * 订阅 STOMP 目标。
   *
   * @param destination 订阅路径
   * @param callback 消息回调
   * @returns 订阅对象（可用于取消订阅）
   */
  function subscribe(destination: string, callback: (message: any) => void): any {
    if (!stompClient || connectionState.value !== 'connected') {
      console.warn('[WebSocket] 未连接，无法订阅')
      return null
    }
    return stompClient.subscribe(destination, (message: any) => {
      try {
        const body = JSON.parse(message.body)
        callback(body)
      } catch {
        callback(message.body)
      }
    })
  }

  /** 检查 WebSocket 是否可用 */
  function isAvailable(): boolean {
    return connectionState.value === 'connected'
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    connectionState,
    connect,
    disconnect,
    send,
    subscribe,
    isAvailable
  }
}
