<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { datasourceApi } from '@/api/datasource'
import { DOMAINS } from '@/types'
import type { DataSourceConfig, DataSourceForm } from '@/types'
import DataSourceTable from '@/components/datasource/DataSourceTable.vue'
import DataSourceFormDialog from '@/components/datasource/DataSourceFormDialog.vue'
import Pagination from '@/components/common/Pagination.vue'

const { t } = useI18n()

const dataSources = ref<DataSourceConfig[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filterDomain = ref('')
const showFormDialog = ref(false)
const editingRow = ref<DataSourceConfig | undefined>(undefined)

async function fetchDataSources() {
  loading.value = true
  try {
    const params: { page: number; size: number; domain?: string } = {
      page: currentPage.value,
      size: pageSize.value,
    }
    if (filterDomain.value) {
      params.domain = filterDomain.value
    }
    const res = await datasourceApi.list(params)
    dataSources.value = res.data.records
    total.value = res.data.total
  } catch {
    // 错误已在拦截器中统一处理
  } finally {
    loading.value = false
  }
}

function handlePageChange() {
  fetchDataSources()
}

function handleFilterChange() {
  currentPage.value = 1
  fetchDataSources()
}

function handleAdd() {
  editingRow.value = undefined
  showFormDialog.value = true
}

function handleEdit(row: DataSourceConfig) {
  editingRow.value = row
  showFormDialog.value = true
}

async function handleSubmit(formData: DataSourceForm) {
  try {
    if (editingRow.value) {
      await datasourceApi.update(editingRow.value.id, formData)
      ElMessage.success(t('datasource.updateSuccess'))
    } else {
      await datasourceApi.create(formData)
      ElMessage.success(t('datasource.createSuccess'))
    }
    showFormDialog.value = false
    fetchDataSources()
  } catch {
    // 错误已在拦截器中处理
  }
}

async function handleTestConnection(formData: DataSourceForm) {
  try {
    // 先创建/更新数据源以获取 ID，用于测试连接
    let datasourceId: number
    if (editingRow.value) {
      const res = await datasourceApi.update(editingRow.value.id, formData)
      datasourceId = res.data.id
    } else {
      const res = await datasourceApi.create(formData)
      datasourceId = res.data.id
    }
    showFormDialog.value = false

    const result = await datasourceApi.testConnection(datasourceId)
    if (result.data.success) {
      ElMessage.success(t('datasource.connectionSuccess', { latency: result.data.latencyMs }))
    } else {
      ElMessage.error(result.data.errorMsg || t('datasource.connectionFailed'))
    }
    fetchDataSources()
  } catch {
    // 错误已在拦截器中处理
  }
}

async function handleTest(id: number) {
  try {
    const result = await datasourceApi.testConnection(id)
    if (result.data.success) {
      ElMessage.success(t('datasource.connectionSuccess', { latency: result.data.latencyMs }))
    } else {
      ElMessage.error(result.data.errorMsg || t('datasource.connectionFailed'))
    }
  } catch {
    // 错误已在拦截器中处理
  }
}

async function handleSync(id: number) {
  try {
    await ElMessageBox.confirm(t('datasource.syncConfirm'), t('datasource.syncConfirmTitle'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'info',
    })
    await datasourceApi.triggerSync(id)
    ElMessage.success(t('datasource.syncSuccess'))
  } catch {
    // 用户取消或操作失败（错误已在拦截器中处理）
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm(t('datasource.deleteConfirmMsg'), t('datasource.deleteConfirmTitle'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
    await datasourceApi.delete(id)
    ElMessage.success(t('datasource.deleteSuccess'))
    fetchDataSources()
  } catch {
    // 用户取消或删除失败（错误已在拦截器中处理）
  }
}

onMounted(() => {
  fetchDataSources()
})
</script>

<template>
  <div class="datasource-view datasource-page">
    <div class="page-header">
      <h2 class="page-title">{{ $t('datasource.title') }}</h2>
    </div>

    <div class="page-toolbar">
      <el-button v-permission="'canConfigureDatasource'" type="primary" @click="handleAdd">
        + {{ $t('datasource.addSource') }}
      </el-button>
      <el-select
        v-model="filterDomain"
        :placeholder="$t('common.all')"
        clearable
        style="width: 160px"
        @change="handleFilterChange"
      >
        <el-option :label="$t('common.all')" value="" />
        <el-option
          v-for="domain in DOMAINS"
          :key="domain"
          :label="domain"
          :value="domain"
        />
      </el-select>
    </div>

    <DataSourceTable
      :data="dataSources"
      :loading="loading"
      @sync="handleSync"
      @test="handleTest"
      @edit="handleEdit"
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

    <div v-if="!loading && dataSources.length === 0 && !filterDomain" class="empty-wrapper">
      <el-empty :description="$t('datasource.noDataSource')" />
    </div>

    <DataSourceFormDialog
      v-model="showFormDialog"
      :edit-data="editingRow"
      @submit="handleSubmit"
      @test-connection="handleTestConnection"
    />
  </div>
</template>

<style scoped lang="scss">
.datasource-page {
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

  .page-toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
  }

  .empty-wrapper {
    margin-top: 60px;
  }
}

@media (max-width: 767px) {
  .datasource-view {
    .el-form-item .el-input,
    .el-form-item .el-select {
      width: 100%;
    }
  }
}
</style>
