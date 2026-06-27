<script setup lang="ts">
import type { DataSourceConfig } from '@/types'
import { formatSourceType, formatDomain, formatDateTime } from '@/utils/format'
import SyncStatusTag from './SyncStatusTag.vue'

defineProps<{
  data: DataSourceConfig[]
  loading: boolean
}>()

const emit = defineEmits<{
  sync: [id: number]
  test: [id: number]
  edit: [row: DataSourceConfig]
  delete: [id: number]
}>()

function formatNumber(val: number | null | undefined): string {
  if (val == null) return '—'
  return val.toLocaleString()
}
</script>

<template>
  <el-table
    :data="data"
    v-loading="loading"
    stripe
    style="width: 100%"
  >
    <el-table-column
      prop="sourceName"
      label="数据源名称"
      min-width="150"
      show-overflow-tooltip
    />

    <el-table-column
      label="类型"
      width="110"
    >
      <template #default="{ row }">
        {{ formatSourceType(row.sourceType) }}
      </template>
    </el-table-column>

    <el-table-column
      label="业务域"
      width="100"
    >
      <template #default="{ row }">
        {{ formatDomain(row.domain) }}
      </template>
    </el-table-column>

    <el-table-column
      label="状态"
      width="100"
    >
      <template #default="{ row }">
        <SyncStatusTag :status="row.status" />
      </template>
    </el-table-column>

    <el-table-column
      label="最近同步"
      width="160"
    >
      <template #default="{ row }">
        {{ row.lastSyncAt ? formatDateTime(row.lastSyncAt) : '—' }}
      </template>
    </el-table-column>

    <el-table-column
      label="同步量"
      width="100"
      align="right"
    >
      <template #default="{ row }">
        {{ formatNumber(row.totalSynced) }}
      </template>
    </el-table-column>

    <el-table-column
      label="操作"
      width="220"
      fixed="right"
    >
      <template #default="{ row }">
        <el-button type="primary" size="small" link @click="emit('sync', row.id)">
          同步
        </el-button>
        <el-button type="warning" size="small" link @click="emit('test', row.id)">
          测试
        </el-button>
        <el-button type="primary" size="small" link @click="emit('edit', row)">
          编辑
        </el-button>
        <el-button type="danger" size="small" link @click="emit('delete', row.id)">
          删除
        </el-button>
      </template>
    </el-table-column>

    <template #empty>
      <el-empty description="暂无数据源，请新增" />
    </template>
  </el-table>
</template>

<style scoped lang="scss">
</style>
