<script setup lang="ts">
import { ref, watch, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import type { FormInstance, FormRules } from 'element-plus'
import type { UserInfo } from '@/types'
import { DEPARTMENTS, SENSITIVITY_LEVELS, DOMAINS } from '@/types'
import { parseAllowedDomains, formatDomain } from '@/utils/format'
import { userApi } from '@/api/user'

const { t } = useI18n()

const props = defineProps<{
  visible: boolean
  mode: 'create' | 'edit'
  userData?: UserInfo
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  role: 'user',
  department: 'COMMON',
  clearanceLevel: 1,
  allowedDomains: [] as string[],
  title: 'employee',
})

const isCreate = computed(() => props.mode === 'create')

const rules = computed<FormRules>(() => {
  const baseRules: FormRules = {
    email: [{ type: 'email', message: () => t('user.invalidEmail'), trigger: 'blur' }],
    phone: [{ pattern: /^1[3-9]\d{9}$/, message: () => t('user.invalidPhone'), trigger: 'blur' }],
    department: [{ required: true, message: () => t('user.pleaseSelectDepartment'), trigger: 'change' }],
    clearanceLevel: [{ required: true, message: () => t('user.pleaseSelectClearance'), trigger: 'change' }],
    title: [{ required: true, message: () => t('user.pleaseSelectTitle'), trigger: 'change' }],
    allowedDomains: [
      {
        type: 'array',
        required: true,
        validator: (_rule, value, callback) => {
          if (!value || value.length === 0) {
            callback(new Error(t('user.pleaseSelectDomain')))
          } else {
            callback()
          }
        },
        trigger: 'change',
      },
    ],
  }
  if (isCreate.value) {
    return {
      ...baseRules,
      username: [
        { required: true, message: () => t('user.pleaseInputUsername'), trigger: 'blur' },
        { min: 2, max: 20, message: () => t('user.usernameLength'), trigger: 'blur' },
      ],
      password: [
        { required: true, message: () => t('user.pleaseInputPassword'), trigger: 'blur' },
        { min: 6, max: 30, message: () => t('user.passwordLength'), trigger: 'blur' },
      ],
    }
  }
  return baseRules
})

watch(
  () => [props.visible, props.userData],
  () => {
    if (props.visible) {
      if (props.mode === 'edit' && props.userData) {
        form.username = props.userData.username
        form.password = ''
        form.realName = props.userData.realName || ''
        form.email = props.userData.email || ''
        form.phone = props.userData.phone || ''
        form.role = props.userData.role || 'user'
        form.department = props.userData.department || 'COMMON'
        form.clearanceLevel = props.userData.clearanceLevel || 1
        form.allowedDomains = parseAllowedDomains(props.userData.allowedDomains || '')
        form.title = props.userData.title || 'employee'
      } else {
        form.username = ''
        form.password = ''
        form.realName = ''
        form.email = ''
        form.phone = ''
        form.role = 'user'
        form.department = 'COMMON'
        form.clearanceLevel = 1
        form.allowedDomains = []
        form.title = 'employee'
      }
      formRef.value?.clearValidate()
    }
  },
)

function handleClose() {
  emit('update:visible', false)
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (isCreate.value) {
      await userApi.createUser({
        username: form.username,
        password: form.password,
        realName: form.realName || undefined,
        email: form.email || undefined,
        phone: form.phone || undefined,
        role: form.role,
        department: form.department,
        clearanceLevel: form.clearanceLevel,
        allowedDomains: form.allowedDomains.join(','),
        title: form.title,
      })
      ElMessage.success(t('user.createSuccess'))
    } else if (props.userData) {
      await userApi.updateUser(props.userData.id, {
        realName: form.realName || undefined,
        email: form.email || undefined,
        phone: form.phone || undefined,
        role: form.role,
        department: form.department,
        clearanceLevel: form.clearanceLevel,
        allowedDomains: form.allowedDomains.join(','),
        title: form.title,
      })
      ElMessage.success(t('user.updateSuccess'))
    }
    emit('success')
    handleClose()
  } catch (err: any) {
    ElMessage.error(err?.message || t('common.operationFailed'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="isCreate ? $t('user.createUser') : $t('user.editUser')"
    width="520px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      @submit.prevent
    >
      <template v-if="isCreate">
        <el-form-item :label="$t('user.username')" prop="username">
          <el-input v-model="form.username" :placeholder="$t('user.pleaseInputUsername')" maxlength="20" />
        </el-form-item>
        <el-form-item :label="$t('user.password')" prop="password">
          <el-input v-model="form.password" type="password" :placeholder="$t('user.pleaseInputPassword')" maxlength="30" show-password />
        </el-form-item>
      </template>
      <el-form-item :label="$t('user.realName')" prop="realName">
        <el-input v-model="form.realName" :placeholder="$t('user.pleaseInputRealName')" />
      </el-form-item>
      <el-form-item :label="$t('user.email')" prop="email">
        <el-input v-model="form.email" :placeholder="$t('user.pleaseInputEmail')" />
      </el-form-item>
      <el-form-item :label="$t('user.phone')" prop="phone">
        <el-input v-model="form.phone" :placeholder="$t('user.pleaseInputPhone')" />
      </el-form-item>
      <el-form-item :label="$t('user.role')" prop="role">
        <el-select v-model="form.role" style="width: 100%">
          <el-option :label="$t('user.roleAdmin')" value="admin" />
          <el-option :label="$t('user.roleUser')" value="user" />
        </el-select>
      </el-form-item>

      <el-divider content-position="left">{{ $t('user.abacAttributes') }}</el-divider>

      <el-form-item :label="$t('user.department')" prop="department">
        <el-select v-model="form.department" style="width: 100%">
          <el-option
            v-for="item in DEPARTMENTS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('user.dataClearance')" prop="clearanceLevel">
        <el-select v-model="form.clearanceLevel" style="width: 100%">
          <el-option
            v-for="item in SENSITIVITY_LEVELS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('user.allowedDomains')" prop="allowedDomains">
        <el-checkbox-group v-model="form.allowedDomains">
          <el-checkbox
            v-for="domain in DOMAINS"
            :key="domain"
            :label="domain"
            :value="domain"
          >
            {{ formatDomain(domain) }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item :label="$t('user.title')" prop="title">
        <el-select v-model="form.title" style="width: 100%">
          <el-option :label="$t('user.titleEmployee')" value="employee" />
          <el-option :label="$t('user.titleManager')" value="manager" />
          <el-option :label="$t('user.titleDirector')" value="director" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ $t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
</style>
