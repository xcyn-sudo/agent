<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { View, Hide } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: '',
})

const rememberMe = ref(false)
const showPassword = ref(false)
const isTyping = ref(false)
const isError = ref(false)
const loginError = ref('')

const rules: FormRules = {
  username: [{ required: true, message: () => t('auth.pleaseInputUsername'), trigger: 'blur' }],
  password: [{ required: true, message: () => t('auth.pleaseInputPassword'), trigger: 'blur' }],
}

const loading = ref(false)

function onUsernameFocus() {
  isTyping.value = true
}

function onUsernameBlur() {
  setTimeout(() => {
    if (!form.username && !form.password) isTyping.value = false
  }, 1000)
}

function onErrorDone() {
  isError.value = false
}

async function handleLogin() {
  loginError.value = ''
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage.success(t('auth.loginSuccess'))
    const redirect = (route.query.redirect as string) || '/chat'
    router.push(redirect)
  } catch (e: any) {
    loginError.value = e?.message || t('auth.loginFailed')
    isError.value = true
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-split">
    <!-- ========== 左侧装饰面板 ========== -->
    <div class="login-split__left">
      <!-- 顶部 Logo -->
      <div class="login-split__brand">
        <div class="login-split__logo-icon" />
        <span class="login-split__logo-text">Agent-QR</span>
      </div>

      <!-- 角色动画 -->
      <div class="login-split__chars">
        <AnimatedCharacters
          :is-typing="isTyping"
          :show-password="showPassword"
          :password-length="form.password.length"
          :is-error="isError"
          @error-done="onErrorDone"
        />
      </div>

      <!-- 底部链接 -->
      <div class="login-split__bottom-links">
        <span>{{ $t('sidebar.subtitle') }}</span>
      </div>
    </div>

    <!-- ========== 右侧表单面板 ========== -->
    <div class="login-split__right">
      <!-- 移动端 Logo -->
      <div class="login-split__mobile-brand">
        <div class="login-split__logo-icon" />
        <span class="login-split__logo-text">Agent-QR</span>
      </div>

      <div class="login-split__form-wrapper">
        <!-- 标题 -->
        <h1 class="login-split__title">{{ $t('auth.welcomeBack') }}</h1>
        <p class="login-split__subtitle">{{ $t('auth.pleaseEnterDetails') }}</p>

        <!-- 错误提示 -->
        <div v-if="loginError" class="login-split__error">
          {{ loginError }}
        </div>

        <!-- 表单 -->
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-split__form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              :placeholder="$t('auth.pleaseInputUsername')"
              size="large"
              @focus="onUsernameFocus"
              @blur="onUsernameBlur"
            />
          </el-form-item>

          <el-form-item prop="password">
            <div class="login-split__password-wrap">
              <el-input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                :placeholder="$t('auth.pleaseInputPassword')"
                size="large"
              />
              <button
                type="button"
                class="login-split__password-toggle"
                @click="showPassword = !showPassword"
              >
                <el-icon :size="18">
                  <View v-if="!showPassword" />
                  <Hide v-else />
                </el-icon>
              </button>
            </div>
          </el-form-item>

          <!-- 记住我 -->
          <div class="login-split__row">
            <el-checkbox v-model="rememberMe" size="small">
              {{ $t('auth.rememberMe') }}
            </el-checkbox>
          </div>

          <!-- 登录按钮 -->
          <el-form-item class="login-split__btn-item">
            <InteractiveButton :loading="loading" @click="handleLogin">
              {{ loading ? $t('auth.loggingIn') : $t('auth.login') }}
            </InteractiveButton>
          </el-form-item>
        </el-form>

        <!-- 底部注册链接 -->
        <div class="login-split__link">
          {{ $t('auth.noAccount') }}
          <router-link to="/register">{{ $t('auth.goRegister') }}</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
// ==================== 分屏布局 ====================
.login-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 100vh;
  max-height: 100vh;
  overflow: hidden;

  // ========== 左侧面板 ==========
  &__left {
    display: none;
    position: relative;
    flex-direction: column;
    align-items: center;
    justify-content: space-between;
    padding: 32px;
    background: linear-gradient(135deg, $login-panel-bg-start, $login-panel-bg-end);
    overflow: hidden;

    @media (min-width: 1024px) {
      display: flex;
    }
  }

  &__brand {
    display: flex;
    align-items: center;
    gap: 10px;
    z-index: 2;
    color: #fff;
    font-weight: 700;
    font-size: 20px;
  }

  &__logo-icon {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    background: linear-gradient(135deg, #fff, rgba(255,255,255,0.7));
    box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  }

  &__logo-text {
    letter-spacing: -0.5px;
  }

  &__chars {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    z-index: 1;
  }

  &__bottom-links {
    z-index: 2;
    color: rgba(255,255,255,0.5);
    font-size: $font-size-small;
  }

  // ========== 右侧面板 ==========
  &__right {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 32px;
    background: #fff;
    overflow-y: auto;
  }

  &__mobile-brand {
    display: flex;
    align-items: center;
    gap: 10px;
    position: absolute;
    top: 24px;
    left: 50%;
    transform: translateX(-50%);
    font-weight: 700;
    font-size: 18px;
    color: $text-primary;

    @media (min-width: 1024px) {
      display: none;
    }
  }

  // ========== 表单容器 ==========
  &__form-wrapper {
    width: 100%;
    max-width: 420px;
  }

  &__title {
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 8px;
    letter-spacing: -0.5px;
  }

  &__subtitle {
    font-size: $font-size-base;
    color: $text-secondary;
    margin-bottom: 32px;
  }

  // ========== 表单 ==========
  &__form {
    :deep(.el-form-item) {
      margin-bottom: 18px;
    }

    :deep(.el-input__wrapper) {
      border-radius: 12px;
      height: 48px;
      box-shadow: none;
      border: 1px solid $border-color-light;
      transition: border-color 0.3s, box-shadow 0.3s;
      padding: 0 16px;

      &:hover {
        border-color: $border-color;
      }

      &.is-focus {
        border-color: $primary-color;
        box-shadow: 0 0 0 3px rgba($primary-color, 0.08);
      }
    }

    :deep(.el-input__inner) {
      font-size: $font-size-base;
    }
  }

  // ========== 密码可见性切换 ==========
  &__password-wrap {
    position: relative;
    width: 100%;
  }

  &__password-toggle {
    position: absolute;
    right: 12px;
    top: 50%;
    transform: translateY(-50%);
    z-index: 2;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border: none;
    background: transparent;
    color: $text-secondary;
    cursor: pointer;
    border-radius: 6px;
    transition: color 0.2s, background 0.2s;

    &:hover {
      color: $text-primary;
      background: rgba(0, 0, 0, 0.04);
    }
  }

  // ========== 记住我 + 忘记密码 ==========
  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 24px;

    :deep(.el-checkbox__label) {
      font-size: $font-size-small;
      color: $text-secondary;
    }
  }

  &__forgot {
    font-size: $font-size-small;
    font-weight: 500;

    &:hover {
      text-decoration: underline;
    }
  }

  // ========== 按钮 ==========
  &__btn-item {
    margin-bottom: 24px;
  }

  // ========== 错误提示 ==========
  &__error {
    padding: 12px 16px;
    font-size: $font-size-small;
    color: $danger-color;
    background: rgba($danger-color, 0.08);
    border: 1px solid rgba($danger-color, 0.25);
    border-radius: 10px;
    margin-bottom: 20px;
  }

  // ========== 注册链接 ==========
  &__link {
    text-align: center;
    font-size: $font-size-small;
    color: $text-secondary;

    a {
      font-weight: 500;
      margin-left: 4px;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

// ==================== 移动端适配 ====================
@media (max-width: 1023px) {
  .login-split {
    grid-template-columns: 1fr;

    &__right {
      padding: 80px 24px 40px;
    }

    &__title {
      font-size: 24px;
    }

    &__form-wrapper {
      max-width: 100%;
    }

    &__form {
      :deep(.el-input__wrapper) {
        height: 44px;
      }
    }
  }
}
</style>
