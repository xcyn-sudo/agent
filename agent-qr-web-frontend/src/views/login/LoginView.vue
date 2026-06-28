<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
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

const rules: FormRules = {
  username: [{ required: true, message: () => t('auth.pleaseInputUsername'), trigger: 'blur' }],
  password: [{ required: true, message: () => t('auth.pleaseInputPassword'), trigger: 'blur' }],
}

const loading = ref(false)

async function handleLogin() {
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
    ElMessage.error(e?.message || t('auth.loginFailed'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <h1 class="login-page__title">{{ $t('auth.welcomeTitle') }}</h1>
    <div class="login-page__card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="handleLogin"
      >
        <el-form-item :label="$t('auth.username')" prop="username">
          <el-input
            v-model="form.username"
            :placeholder="$t('auth.pleaseInputUsername')"
          />
        </el-form-item>
        <el-form-item :label="$t('auth.password')" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="$t('auth.pleaseInputPassword')"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-page__btn"
            @click="handleLogin"
          >
            {{ $t('auth.login') }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-page__link">
        {{ $t('auth.noAccount') }}
        <router-link to="/register">{{ $t('auth.goRegister') }}</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  &__title {
    font-size: 24px;
    color: $text-primary;
    margin-bottom: 32px;
    text-align: center;
  }

  &__card {
    width: 400px;
    padding: 32px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }

  &__btn {
    width: 100%;
  }

  &__link {
    text-align: center;
    a {
      font-size: $font-size-small;
    }
  }
}

// P3 移动端适配
@media (max-width: 767px) {
  .login-page {
    padding: 16px;

    &__title {
      font-size: 20px;
    }

    &__card {
      width: 100%;
      max-width: 400px;
      padding: 24px;
    }
  }
}
</style>
