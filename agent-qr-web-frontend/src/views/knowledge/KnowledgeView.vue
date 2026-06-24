<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { knowledgeApi } from '@/api/knowledge'
import type { DocumentInfo } from '@/types'
import UploadDialog from '@/components/knowledge/UploadDialog.vue'
import DocumentTable from '@/components/knowledge/DocumentTable.vue'
import Pagination from '@/components/common/Pagination.vue'

/** 处理中状态集合，文档处于这些状态时需持续轮询 */
const PROCESSING_STATUSES = new Set(['UPLOADED', 'PARSING', 'CHUNKING', 'EMBEDDING'])

const documents = ref<DocumentInfo[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const uploadDialogVisible = ref(false)
let pollTimer: ReturnType<typeof setTimeout> | null = null

async function fetchDocuments() {
  loading.value = true
  try {
    const res = await knowledgeApi.listDocuments({ page: currentPage.value, size: pageSize.value })
    documents.value = res.data.records
    total.value = res.data.total
  } catch {
    // 错误已在拦截器中统一处理
  } finally {
    loading.value = false
  }
}

/** 检查是否存在处理中的文档，有则启动轮询 */
function schedulePollIfNeeded() {
  const hasProcessing = documents.value.some(d => PROCESSING_STATUSES.has(d.status))
  if (hasProcessing) {
    pollTimer = setTimeout(async () => {
      await fetchDocuments()
      schedulePollIfNeeded()
    }, 2000)
  }
}

/** 停止轮询定时器 */
function clearPoll() {
  if (pollTimer !== null) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

function handlePageChange() {
  clearPoll()
  fetchDocuments().then(schedulePollIfNeeded)
}

function handleUploadSuccess() {
  currentPage.value = 1
  clearPoll()
  fetchDocuments().then(schedulePollIfNeeded)
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该文档吗？删除后数据将无法恢复。', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await knowledgeApi.deleteDocument(id)
    ElMessage.success('删除成功')
    fetchDocuments().then(schedulePollIfNeeded)
  } catch {
    // 用户取消或删除失败（错误已在拦截器中处理）
  }
}

onMounted(() => {
  fetchDocuments().then(schedulePollIfNeeded)
})

onUnmounted(() => {
  clearPoll()
})
</script>

<template>
  <div class="knowledge-page">
    <div class="page-header">
      <h2 class="page-title">知识库管理</h2>
      <el-button type="primary" @click="uploadDialogVisible = true">
        + 上传文档
      </el-button>
    </div>

    <DocumentTable
      :documents="documents"
      :loading="loading"
      @delete="handleDelete"
    />

    <Pagination
      :total="total"
      :current-page="currentPage"
      :page-size="pageSize"
      @update:current-page="currentPage = $event"
      @update:page-size="pageSize = $event"
      @change="handlePageChange"
    />

    <UploadDialog
      v-model:visible="uploadDialogVisible"
      @success="handleUploadSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
.knowledge-page {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;

    .page-title {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }
  }
}
</style>
