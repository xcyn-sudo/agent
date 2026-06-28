<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
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

const rules: FormRules = {
  username: [{ required: true, message: () => t('auth.pleaseInputUsername'), trigger: 'blur' }],
  password: [{ required: true, message: () => t('auth.pleaseInputPassword'), trigger: 'blur' }],
  email: [
    {
      pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
      message: () => t('auth.pleaseInputEmail'),
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

async function handleRegister() {
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
    ElMessage.error(e?.message || t('auth.registerFailed'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-page__card">
      <h2 class="register-page__title">{{ $t('auth.register') }}</h2>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="handleRegister"
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
        <el-form-item :label="$t('auth.realName')" prop="realName">
          <el-input
            v-model="form.realName"
            :placeholder="$t('common.optional')"
          />
        </el-form-item>
        <el-form-item :label="$t('auth.email')" prop="email">
          <el-input
            v-model="form.email"
            :placeholder="$t('common.optional')"
          />
        </el-form-item>
        <el-form-item :label="$t('auth.phone')" prop="phone">
          <el-input
            v-model="form.phone"
            :placeholder="$t('common.optional')"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="register-page__btn"
            @click="handleRegister"
          >
            {{ $t('auth.register') }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="register-page__link">
        {{ $t('auth.hasAccount') }}
        <router-link to="/login">{{ $t('auth.goLogin') }}</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.register-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  &__card {
    width: 420px;
    padding: 32px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }

  &__title {
    font-size: 20px;
    color: $text-primary;
    margin-bottom: 24px;
    text-align: center;
  }

  &__btn {
    width: 100%;
  }

  &__link {
    text-align: center;
    font-size: $font-size-small;
    color: $text-secondary;
  }
}

// P3 移动端适配
@media (max-width: 767px) {
  .register-page {
    padding: 16px;

    &__card {
      width: 100%;
      max-width: 420px;
      padding: 24px;
    }

    &__title {
      font-size: 18px;
    }
  }
}
</style>
