<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { getTokenExpiresAt } from '@/utils/token'

const { t, locale } = useI18n()
const authStore = useAuthStore()
const appStore = useAppStore()

// ★ P2 Token 过期倒计时
const tokenRemainingSeconds = ref(0)
const isRefreshingToken = ref(false)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const user = computed(() => {
  return authStore.user?.username || ''
})

const tokenCountdownDisplay = computed(() => {
  if (tokenRemainingSeconds.value <= 0) return ''
  const minutes = Math.floor(tokenRemainingSeconds.value / 60)
  const seconds = tokenRemainingSeconds.value % 60
  if (minutes > 0) {
    return `${minutes}分${seconds}秒后过期`
  }
  return `${seconds}秒后过期`
})

const isTokenWarning = computed(() => {
  return tokenRemainingSeconds.value > 0 && tokenRemainingSeconds.value <= 60
})

function updateCountdown() {
  const expiresAt = getTokenExpiresAt()
  if (expiresAt) {
    tokenRemainingSeconds.value = Math.max(0, Math.floor((expiresAt - Date.now()) / 1000))
  } else {
    tokenRemainingSeconds.value = 0
  }
}

function startCountdown() {
  updateCountdown()
  countdownTimer = setInterval(updateCountdown, 10000)
}

function handleLogout() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  authStore.logout()
}

// P3 新增：语言切换
function switchLanguage(lang: string) {
  locale.value = lang
  localStorage.setItem('locale', lang)
}

onMounted(() => {
  startCountdown()
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})
</script>

<template>
  <header class="header-bar">
    <div class="header-bar__left">
      <el-icon class="collapse-btn" @click="appStore.toggleSidebar()">
        <Fold />
      </el-icon>
      <span class="system-name">{{ $t('sidebar.subtitle') }}</span>
    </div>
    <div class="header-bar__right">
      <!-- ★ P2 Token 过期倒计时 -->
      <span
        v-if="tokenRemainingSeconds > 0"
        class="token-countdown"
        :class="{ 'token-countdown--warning': isTokenWarning }"
      >
        {{ tokenCountdownDisplay }}
      </span>
      <!-- ★ 静默刷新中提示 -->
      <span v-if="isRefreshingToken" class="refreshing-hint">正在刷新凭证...</span>
      <span class="username">{{ user }}</span>
      <!-- P3 新增：语言切换 -->
      <el-dropdown @command="switchLanguage" trigger="click">
        <el-button text size="small">
          {{ locale === 'zh-CN' ? '中文' : 'English' }}
          <el-icon><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="zh-CN">中文</el-dropdown-item>
            <el-dropdown-item command="en-US">English</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button type="danger" text @click="handleLogout">{{ $t('auth.logout') }}</el-button>
    </div>
  </header>
</template>

<style scoped lang="scss">
.header-bar {
  height: $header-height;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  z-index: 10;

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  &:hover {
    color: $primary-color;
  }
}

.system-name {
  font-size: $font-size-large;
  font-weight: 600;
  color: $text-primary;
}

.username {
  color: $text-regular;
}

.token-countdown {
  font-size: 12px;
  color: #909399;
  margin-right: 8px;

  &--warning {
    color: #f56c6c;
    font-weight: 600;
  }
}

.refreshing-hint {
  font-size: 12px;
  color: #409eff;
}
</style>
