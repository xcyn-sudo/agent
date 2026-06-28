<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? $t('quality.editRule') : $t('quality.addRule')"
    width="600px"
    @update:model-value="$emit('update:visible', $event)"
    @close="resetForm"
  >
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
      <el-form-item :label="$t('quality.ruleName')" prop="ruleName">
        <el-input v-model="form.ruleName" :placeholder="$t('quality.ruleName')" />
      </el-form-item>

      <el-form-item :label="$t('quality.ruleType')" prop="ruleType">
        <el-select v-model="form.ruleType" style="width: 100%">
          <el-option :label="$t('quality.ruleTypes.completeness')" value="completeness" />
          <el-option :label="$t('quality.ruleTypes.uniqueness')" value="uniqueness" />
          <el-option :label="$t('quality.ruleTypes.format')" value="format" />
          <el-option :label="$t('quality.ruleTypes.encoding')" value="encoding" />
          <el-option :label="$t('quality.ruleTypes.length')" value="length" />
        </el-select>
      </el-form-item>

      <!-- 完整性：阈值 -->
      <el-form-item v-if="form.ruleType === 'completeness'" :label="$t('quality.ruleParam')" prop="threshold">
        <el-input-number v-model="form.threshold" :min="0" :max="100" :precision="1" style="width: 100%">
          <template #suffix>%</template>
        </el-input-number>
      </el-form-item>

      <!-- 唯一性：阈值 -->
      <el-form-item v-if="form.ruleType === 'uniqueness'" :label="$t('quality.ruleParam')" prop="threshold">
        <el-input-number v-model="form.threshold" :min="0" :max="100" :precision="1" style="width: 100%">
          <template #suffix>%</template>
        </el-input-number>
      </el-form-item>

      <!-- 格式：正则表达式 -->
      <el-form-item v-if="form.ruleType === 'format'" :label="$t('quality.ruleParam')" prop="pattern">
        <el-input v-model="form.pattern" placeholder="例如: ^[A-Za-z0-9]+$" />
      </el-form-item>

      <!-- 编码：字符集 -->
      <el-form-item v-if="form.ruleType === 'encoding'" :label="$t('quality.ruleParam')" prop="encodingCharset">
        <el-select v-model="form.encodingCharset" style="width: 100%">
          <el-option label="UTF-8" value="UTF-8" />
          <el-option label="GBK" value="GBK" />
          <el-option label="GB2312" value="GB2312" />
          <el-option label="ISO-8859-1" value="ISO-8859-1" />
        </el-select>
      </el-form-item>

      <!-- 长度：最小/最大 -->
      <el-form-item v-if="form.ruleType === 'length'" :label="$t('quality.ruleParam')">
        <el-row :gutter="10">
          <el-col :span="11">
            <el-input-number v-model="form.minLength" :min="0" placeholder="最小长度" style="width: 100%" />
          </el-col>
          <el-col :span="2" style="text-align: center; line-height: 32px">-</el-col>
          <el-col :span="11">
            <el-input-number v-model="form.maxLength" :min="0" placeholder="最大长度" style="width: 100%" />
          </el-col>
        </el-row>
      </el-form-item>

      <el-form-item :label="$t('quality.ruleStatus')" prop="enabled">
        <el-switch v-model="form.enabled" />
      </el-form-item>

      <!-- 实时预览 -->
      <el-divider>{{ $t('quality.ruleEditor.preview') }}</el-divider>
      <el-form-item :label="$t('quality.ruleEditor.sampleData')">
        <el-input v-model="sampleInput" placeholder="输入样例数据测试规则" @input="testRule" />
      </el-form-item>
      <el-form-item v-if="sampleInput" :label="$t('quality.ruleEditor.matchResult')">
        <el-tag :type="matchResult === null ? 'info' : matchResult ? 'success' : 'danger'">
          {{ matchResult === null ? '—' : matchResult ? $t('quality.ruleEditor.matched') : $t('quality.ruleEditor.notMatched') }}
        </el-tag>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" @click="handleSave">{{ $t('common.save') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import type { FormInstance } from 'element-plus'

export interface QualityRule {
  id?: number
  ruleName: string
  ruleType: 'completeness' | 'uniqueness' | 'format' | 'encoding' | 'length'
  threshold?: number
  pattern?: string
  encodingCharset?: string
  minLength?: number
  maxLength?: number
  enabled: boolean
}

const props = defineProps<{
  visible: boolean
  rule?: QualityRule | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  save: [rule: QualityRule]
}>()

const isEdit = ref(false)
const formRef = ref<FormInstance>()
const sampleInput = ref('')
const matchResult = ref<boolean | null>(null)

const form = reactive<QualityRule>({
  ruleName: '',
  ruleType: 'completeness',
  threshold: 95,
  pattern: '',
  encodingCharset: 'UTF-8',
  minLength: 0,
  maxLength: 255,
  enabled: true
})

const rules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  threshold: [{ required: true, message: '请设置阈值', trigger: 'blur' }],
  pattern: [{ required: true, message: '请输入正则表达式', trigger: 'blur' }],
  encodingCharset: [{ required: true, message: '请选择字符集', trigger: 'change' }]
}

watch(() => props.visible, (val) => {
  if (val) {
    if (props.rule) {
      isEdit.value = true
      Object.assign(form, props.rule)
    } else {
      isEdit.value = false
      resetForm()
    }
  }
})

function testRule() {
  if (!sampleInput.value) {
    matchResult.value = null
    return
  }
  switch (form.ruleType) {
    case 'completeness':
      matchResult.value = sampleInput.value.trim().length > 0
      break
    case 'uniqueness':
      matchResult.value = true
      break
    case 'format':
      if (form.pattern) {
        try {
          matchResult.value = new RegExp(form.pattern).test(sampleInput.value)
        } catch {
          matchResult.value = false
        }
      }
      break
    case 'encoding':
      matchResult.value = true
      break
    case 'length':
      const len = sampleInput.value.length
      matchResult.value = len >= (form.minLength || 0) && len <= (form.maxLength || Infinity)
      break
  }
}

function handleSave() {
  formRef.value?.validate((valid) => {
    if (valid) {
      emit('save', { ...form })
      emit('update:visible', false)
    }
  })
}

function resetForm() {
  form.ruleName = ''
  form.ruleType = 'completeness'
  form.threshold = 95
  form.pattern = ''
  form.encodingCharset = 'UTF-8'
  form.minLength = 0
  form.maxLength = 255
  form.enabled = true
  sampleInput.value = ''
  matchResult.value = null
  isEdit.value = false
}
</script>
