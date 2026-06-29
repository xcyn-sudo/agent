<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { View, Hide } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
})

const showPassword = ref(false)
const isTyping = ref(false)
const isError = ref(false)
const registerError = ref('')

const rules: FormRules = {
  username: [{ required: true, message: () => t('auth.pleaseInputUsername'), trigger: 'blur' }],
  password: [{ required: true, message: () => t('auth.pleaseInputPassword'), trigger: 'blur' }],
  email: [
    {
      pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
      message: () => t('auth.invalidEmail'),
      trigger: 'blur',
    },
  ],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: () => t('auth.invalidPhone'),
      trigger: 'blur',
    },
  ],
}

const loading = ref(false)

function onUsernameFocus() {
  isTyping.value = true
}

function onUsernameBlur() {
  setTimeout(() => {
    if (!form.username && !form.password && !form.realName) isTyping.value = false
  }, 1000)
}

function onErrorDone() {
  isError.value = false
}

async function handleRegister() {
  registerError.value = ''
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data: { username: string; password: string; realName?: string; email?: string; phone?: string } = {
      username: form.username,
      password: form.password,
    }
    if (form.realName) data.realName = form.realName
    if (form.email) data.email = form.email
    if (form.phone) data.phone = form.phone

    await authStore.register(data)
    ElMessage.success(t('auth.registerSuccess'))
    router.push('/login')
  } catch (e: any) {
    registerError.value = e?.message || t('auth.registerFailed')
    isError.value = true
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-split">
    <!-- ========== 左侧装饰面板 ========== -->
    <div class="register-split__left">
      <!-- 顶部 Logo -->
      <div class="register-split__brand">
        <div class="register-split__logo-icon" />
        <span class="register-split__logo-text">Agent-QR</span>
      </div>

      <!-- 角色动画 -->
      <div class="register-split__chars">
        <AnimatedCharacters
          :is-typing="isTyping"
          :show-password="showPassword"
          :password-length="form.password.length"
          :is-error="isError"
          @error-done="onErrorDone"
        />
      </div>

      <!-- 底部链接 -->
      <div class="register-split__bottom-links">
        <span>{{ $t('sidebar.subtitle') }}</span>
      </div>
    </div>

    <!-- ========== 右侧表单面板 ========== -->
    <div class="register-split__right">
      <!-- 移动端 Logo -->
      <div class="register-split__mobile-brand">
        <div class="register-split__logo-icon" />
        <span class="register-split__logo-text">Agent-QR</span>
      </div>

      <div class="register-split__form-wrapper">
        <!-- 标题 -->
        <h1 class="register-split__title">{{ $t('auth.registerTitle') }}</h1>
        <p class="register-split__subtitle">{{ $t('auth.registerSubtitle') }}</p>

        <!-- 错误提示 -->
        <div v-if="registerError" class="register-split__error">
          {{ registerError }}
        </div>

        <!-- 表单 -->
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="register-split__form"
          @keyup.enter="handleRegister"
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
            <div class="register-split__password-wrap">
              <el-input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                :placeholder="$t('auth.pleaseInputPassword')"
                size="large"
              />
              <button
                type="button"
                class="register-split__password-toggle"
                @click="showPassword = !showPassword"
              >
                <el-icon :size="18">
                  <View v-if="!showPassword" />
                  <Hide v-else />
                </el-icon>
              </button>
            </div>
          </el-form-item>

          <el-form-item prop="realName">
            <el-input
              v-model="form.realName"
              :placeholder="$t('common.optional')"
              size="large"
            >
              <template #prepend>
                {{ $t('auth.realName') }}
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="email">
            <el-input
              v-model="form.email"
              :placeholder="$t('common.optional')"
              size="large"
            >
              <template #prepend>
                {{ $t('auth.email') }}
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="phone">
            <el-input
              v-model="form.phone"
              :placeholder="$t('common.optional')"
              size="large"
            >
              <template #prepend>
                {{ $t('auth.phone') }}
              </template>
            </el-input>
          </el-form-item>

          <!-- 注册按钮 -->
          <el-form-item class="register-split__btn-item">
            <InteractiveButton :loading="loading" @click="handleRegister">
              {{ loading ? $t('auth.registering') : $t('auth.register') }}
            </InteractiveButton>
          </el-form-item>
        </el-form>

        <!-- 底部登录链接 -->
        <div class="register-split__link">
          {{ $t('auth.hasAccount') }}
          <router-link to="/login">{{ $t('auth.goLogin') }}</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
// ==================== 分屏布局 ====================
.register-split {
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
    align-items: flex-start;
    justify-content: center;
    padding: 40px 32px;
    background: #fff;
    overflow-y: auto;

    @media (min-width: 1024px) {
      align-items: center;
    }
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
    margin-bottom: 28px;
  }

  // ========== 表单 ==========
  &__form {
    :deep(.el-form-item) {
      margin-bottom: 16px;
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

    :deep(.el-input-group__prepend) {
      border-radius: 12px 0 0 12px;
      background: $bg-color-light;
      font-size: $font-size-small;
      font-weight: 500;
      color: $text-secondary;
      border: 1px solid $border-color-light;
      border-right: none;
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

  // ========== 按钮 ==========
  &__btn-item {
    margin-top: 8px;
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

  // ========== 登录链接 ==========
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
  .register-split {
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
