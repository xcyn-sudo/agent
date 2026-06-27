<script setup lang="ts">
import type { DocumentInfo } from '@/types'
import { formatFileSize, formatDateTime, formatDomain } from '@/utils/format'
import StatusTag from './StatusTag.vue'

defineProps<{
  documents: DocumentInfo[]
  loading: boolean
}>()

const emit = defineEmits<{
  delete: [documentId: number]
}>()
</script>

<template>
  <el-table
    :data="documents"
    v-loading="loading"
    stripe
    style="width: 100%"
  >
    <el-table-column
      prop="fileName"
      label="文件名"
      min-width="180"
      show-overflow-tooltip
    />
    <el-table-column
      prop="fileType"
      label="类型"
      width="80"
    >
      <template #default="{ row }">
        {{ row.fileType?.toUpperCase() }}
      </template>
    </el-table-column>
    <el-table-column
      prop="fileSize"
      label="大小"
      width="100"
    >
      <template #default="{ row }">
        {{ formatFileSize(row.fileSize) }}
      </template>
    </el-table-column>
    <el-table-column
      prop="domain"
      label="业务域"
      width="120"
    >
      <template #default="{ row }">
        <span class="domain-tag">{{ formatDomain(row.domain) }}</span>
      </template>
    </el-table-column>
    <el-table-column
      prop="sensitivityLabel"
      label="密级"
      width="80"
    >
      <template #default="{ row }">
        <span
          :class="[
            'sensitivity-tag',
            `sensitivity-tag--${['public', 'internal', 'confidential', 'topsecret'][row.sensitivityLevel] || 'public'}`
          ]"
        >
          {{ row.sensitivityLabel }}
        </span>
      </template>
    </el-table-column>
    <el-table-column
      prop="status"
      label="状态"
      width="120"
    >
      <template #default="{ row }">
        <StatusTag :status="row.status" :error-msg="row.errorMsg" />
      </template>
    </el-table-column>
    <el-table-column
      prop="createTime"
      label="上传时间"
      width="160"
    >
      <template #default="{ row }">
        {{ formatDateTime(row.createTime) }}
      </template>
    </el-table-column>
    <el-table-column
      label="操作"
      width="100"
      fixed="right"
    >
      <template #default="{ row }">
        <el-button
          type="danger"
          size="small"
          link
          @click="emit('delete', row.id)"
        >
          删除
        </el-button>
      </template>
    </el-table-column>

    <template #empty>
      <el-empty description="暂无文档，请上传" />
    </template>
  </el-table>
</template>

<style scoped lang="scss">
.domain-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  background-color: var(--el-color-info-light-9);
  color: var(--el-text-color-regular);
}

.sensitivity-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;

  &--public {
    background-color: #e8f5e9;
    color: #2e7d32;
  }

  &--internal {
    background-color: #e3f2fd;
    color: #1565c0;
  }

  &--confidential {
    background-color: #fff3e0;
    color: #e65100;
  }

  &--topsecret {
    background-color: #fce4ec;
    color: #c62828;
  }
}
</style>
