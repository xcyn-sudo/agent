<template>
  <div class="rules-manager">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ $t('quality.ruleManager') }}</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            {{ $t('quality.addRule') }}
          </el-button>
        </div>
      </template>

      <el-table :data="rules" stripe v-loading="loading">
        <el-table-column prop="ruleName" :label="$t('quality.ruleName')" min-width="150" />
        <el-table-column prop="ruleType" :label="$t('quality.ruleType')" width="120">
          <template #default="{ row }">
            <el-tag>{{ ruleTypeLabel(row.ruleType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('quality.ruleParam')" min-width="200">
          <template #default="{ row }">
            <span v-if="row.ruleType === 'completeness' || row.ruleType === 'uniqueness'">
              {{ $t('quality.passRate') }} ≥ {{ row.threshold }}%
            </span>
            <span v-else-if="row.ruleType === 'format'">
              {{ row.pattern }}
            </span>
            <span v-else-if="row.ruleType === 'encoding'">
              {{ row.encodingCharset }}
            </span>
            <span v-else-if="row.ruleType === 'length'">
              {{ row.minLength }}-{{ row.maxLength }}
            </span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('quality.ruleStatus')" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              @change="toggleRule(row)"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="150" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">
              {{ $t('common.edit') }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && rules.length === 0" :description="$t('common.noData')" />
    </el-card>

    <RuleEditor
      :visible="editorVisible"
      :rule="editingRule"
      @update:visible="editorVisible = $event"
      @save="handleSave"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import RuleEditor from '@/components/quality/RuleEditor.vue'
import type { QualityRule } from '@/components/quality/RuleEditor.vue'

const { t } = useI18n()

const rules = ref<QualityRule[]>([])
const loading = ref(false)
const editorVisible = ref(false)
const editingRule = ref<QualityRule | null>(null)

function ruleTypeLabel(type: string): string {
  const map: Record<string, string> = {
    completeness: t('quality.ruleTypes.completeness'),
    uniqueness: t('quality.ruleTypes.uniqueness'),
    format: t('quality.ruleTypes.format'),
    encoding: t('quality.ruleTypes.encoding'),
    length: t('quality.ruleTypes.length')
  }
  return map[type] || type
}

function loadRules() {
  loading.value = true
  // 从 localStorage 加载规则（后续对接后端 API）
  try {
    const saved = localStorage.getItem('quality-rules')
    if (saved) {
      rules.value = JSON.parse(saved)
    } else {
      // 默认规则
      rules.value = [
        { id: 1, ruleName: '字段非空检查', ruleType: 'completeness', threshold: 95, enabled: true },
        { id: 2, ruleName: '主键唯一性', ruleType: 'uniqueness', threshold: 100, enabled: true },
        { id: 3, ruleName: '邮箱格式验证', ruleType: 'format', pattern: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$', enabled: true },
        { id: 4, ruleName: 'UTF-8编码检查', ruleType: 'encoding', encodingCharset: 'UTF-8', enabled: true },
        { id: 5, ruleName: '字段长度限制', ruleType: 'length', minLength: 1, maxLength: 500, enabled: false }
      ]
      saveRules()
    }
  } catch {
    rules.value = []
  }
  loading.value = false
}

function saveRules() {
  localStorage.setItem('quality-rules', JSON.stringify(rules.value))
}

function handleAdd() {
  editingRule.value = null
  editorVisible.value = true
}

function handleEdit(rule: QualityRule) {
  editingRule.value = { ...rule }
  editorVisible.value = true
}

function handleSave(rule: QualityRule) {
  if (rule.id) {
    const idx = rules.value.findIndex((r) => r.id === rule.id)
    if (idx >= 0) {
      rules.value[idx] = rule
    }
  } else {
    rule.id = Date.now()
    rules.value.push(rule)
  }
  saveRules()
  ElMessage.success(t('common.success'))
}

function handleDelete(rule: QualityRule) {
  ElMessageBox.confirm(
    t('quality.deleteRule') + '?',
    t('common.tips'),
    { confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel'), type: 'warning' }
  ).then(() => {
    rules.value = rules.value.filter((r) => r.id !== rule.id)
    saveRules()
    ElMessage.success(t('common.success'))
  }).catch(() => {})
}

function toggleRule(rule: QualityRule) {
  saveRules()
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
