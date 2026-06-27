<script setup lang="ts">
defineProps<{
  status: string
  errorMsg?: string
}>()

const statusMap: Record<string, { type: 'info' | 'success' | 'danger' | 'warning' | ''; label: string; loading: boolean }> = {
  UPLOADED: { type: 'info', label: '已上传', loading: false },
  PARSING: { type: '', label: '解析中', loading: true },
  CHUNKING: { type: '', label: '切片中', loading: true },
  EMBEDDING: { type: '', label: '向量化中', loading: true },
  READY: { type: 'success', label: '就绪', loading: false },
  FAILED: { type: 'danger', label: '失败', loading: false },
  DELETING: { type: '', label: '删除中', loading: true },
}
</script>

<template>
  <el-tooltip
    v-if="status === 'FAILED' && errorMsg"
    :content="errorMsg"
    placement="top"
  >
    <el-tag :type="statusMap[status]?.type || 'info'">
      <el-icon v-if="statusMap[status]?.loading" class="is-loading">
        <Loading />
      </el-icon>
      {{ statusMap[status]?.label || status }}
    </el-tag>
  </el-tooltip>
  <el-tag v-else :type="statusMap[status]?.type || 'info'">
    <el-icon v-if="statusMap[status]?.loading" class="is-loading">
      <Loading />
    </el-icon>
    {{ statusMap[status]?.label || status }}
  </el-tag>
</template>

<style scoped lang="scss">
.is-loading {
  margin-right: 4px;
}
</style>
