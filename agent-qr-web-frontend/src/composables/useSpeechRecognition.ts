import { ref, computed, onUnmounted, type Ref, type ComputedRef } from 'vue'

/**
 * 语音识别 Composable（P3 新增，可选功能）。
 * <p>
 * 基于浏览器 Web Speech API (SpeechRecognition)，
 * 支持中英文语音识别，将语音转为文本填入输入框。
 * </p>
 *
 * @returns 语音识别控制方法与状态
 */
export function useSpeechRecognition() {
  const isListening: Ref<boolean> = ref(false)
  const transcript: Ref<string> = ref('')
  const error: Ref<string | null> = ref(null)

  // @ts-ignore — 浏览器原生 SpeechRecognition API
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  let recognition: any = null

  /** 浏览器是否支持语音识别 */
  const isSupported: ComputedRef<boolean> = computed(() => {
    return !!SpeechRecognition
  })

  /**
   * 开始语音识别。
   *
   * @param lang 识别语言（'zh-CN' | 'en-US'）
   */
  function startListening(lang: 'zh-CN' | 'en-US' = 'zh-CN'): void {
    if (!isSupported.value) {
      error.value = '您的浏览器不支持语音识别'
      return
    }

    if (isListening.value) return

    try {
      recognition = new SpeechRecognition()
      recognition.lang = lang
      recognition.interimResults = true
      recognition.continuous = false
      recognition.maxAlternatives = 1

      recognition.onstart = () => {
        isListening.value = true
        transcript.value = ''
        error.value = null
      }

      recognition.onresult = (event: any) => {
        let finalTranscript = ''
        let interimTranscript = ''
        for (let i = event.resultIndex; i < event.results.length; i++) {
          const result = event.results[i]
          if (result.isFinal) {
            finalTranscript += result[0].transcript
          } else {
            interimTranscript += result[0].transcript
          }
        }
        transcript.value = finalTranscript || interimTranscript
      }

      recognition.onerror = (event: any) => {
        console.warn('[Speech] 识别错误:', event.error)
        error.value = event.error === 'no-speech' ? '未检测到语音' : `语音识别错误: ${event.error}`
        isListening.value = false
      }

      recognition.onend = () => {
        isListening.value = false
      }

      recognition.start()
    } catch (e) {
      error.value = '语音识别启动失败'
      isListening.value = false
    }
  }

  /** 停止语音识别 */
  function stopListening(): void {
    if (recognition) {
      try {
        recognition.stop()
      } catch (e) {
        // 忽略停止异常
      }
      recognition = null
    }
    isListening.value = false
  }

  onUnmounted(() => {
    stopListening()
  })

  return {
    isListening,
    transcript,
    error,
    isSupported,
    startListening,
    stopListening
  }
}
