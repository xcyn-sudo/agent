<script setup lang="ts">
import { ref, watch, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { UserInfo } from '@/types'
import { DEPARTMENTS, SENSITIVITY_LEVELS, DOMAINS } from '@/types'
import { parseAllowedDomains, formatDomain } from '@/utils/format'
import { userApi } from '@/api/user'

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
    email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
    phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
    department: [{ required: true, message: '请选择部门', trigger: 'change' }],
    clearanceLevel: [{ required: true, message: '请选择数据密级', trigger: 'change' }],
    title: [{ required: true, message: '请选择职级', trigger: 'change' }],
    allowedDomains: [
      {
        type: 'array',
        required: true,
        validator: (_rule, value, callback) => {
          if (!value || value.length === 0) {
            callback(new Error('请至少选择一个允许访问域'))
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
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 2, max: 20, message: '用户名长度 2-20 个字符', trigger: 'blur' },
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 30, message: '密码长度 6-30 个字符', trigger: 'blur' },
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
      ElMessage.success('创建用户成功')
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
      ElMessage.success('更新用户成功')
    }
    emit('success')
    handleClose()
  } catch (err: any) {
    ElMessage.error(err?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="isCreate ? '新增用户' : '编辑用户'"
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
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" maxlength="20" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" maxlength="30" show-password />
        </el-form-item>
      </template>
      <el-form-item label="真实姓名" prop="realName">
        <el-input v-model="form.realName" placeholder="请输入真实姓名" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="请输入邮箱" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="角色" prop="role">
        <el-select v-model="form.role" style="width: 100%">
          <el-option label="管理员" value="admin" />
          <el-option label="普通用户" value="user" />
        </el-select>
      </el-form-item>

      <el-divider content-position="left">ABAC 属性</el-divider>

      <el-form-item label="部门" prop="department">
        <el-select v-model="form.department" style="width: 100%">
          <el-option
            v-for="item in DEPARTMENTS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="数据密级" prop="clearanceLevel">
        <el-select v-model="form.clearanceLevel" style="width: 100%">
          <el-option
            v-for="item in SENSITIVITY_LEVELS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="允许访问域" prop="allowedDomains">
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
      <el-form-item label="职级" prop="title">
        <el-select v-model="form.title" style="width: 100%">
          <el-option label="员工" value="employee" />
          <el-option label="经理" value="manager" />
          <el-option label="总监" value="director" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
</style>
