<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { catalogApi } from '@/api/catalog'
import type { CatalogTree, EntityNode, SourceNode, DomainNode } from '@/types'
import { formatDateTime } from '@/utils/format'
import CatalogTreeComponent from '@/components/catalog/CatalogTree.vue'

// ==================== 状态管理 ====================
const catalogData = ref<CatalogTree | null>(null)
const loading = ref(false)
const searchKeyword = ref('')
const debouncedKeyword = ref('')

// 实体详情弹窗
const entityDialogVisible = ref(false)
const selectedEntity = ref<EntityNode | null>(null)
const selectedSource = ref<SourceNode | null>(null)
const selectedDomain = ref<DomainNode | null>(null)

// ==================== 防抖 ====================
let debounceTimer: ReturnType<typeof setTimeout> | null = null

function onSearchInput(value: string) {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(() => {
    debouncedKeyword.value = value
  }, 300)
}

onUnmounted(() => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
})

// ==================== 数据加载 ====================
async function fetchCatalog() {
  loading.value = true
  try {
    const res = await catalogApi.getCatalogTree()
    if (res?.data) {
      catalogData.value = res.data
    }
  } catch {
    ElMessage.error('知识目录加载失败')
  } finally {
    loading.value = false
  }
}

// ==================== 实体点击 ====================
function onEntityClick(entity: EntityNode, source: SourceNode, domain: DomainNode) {
  selectedEntity.value = entity
  selectedSource.value = source
  selectedDomain.value = domain
  entityDialogVisible.value = true
}

// ==================== 生命周期 ====================
onMounted(() => {
  fetchCatalog()
})
</script>

<template>
  <div class="catalog-view">
    <!-- 页面标题 -->
    <h2 class="catalog-title">知识目录</h2>

    <!-- 顶部搜索栏 -->
    <div class="catalog-search">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索域/数据源/实体..."
        clearable
        @input="onSearchInput"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 目录树 -->
    <el-card shadow="hover" class="catalog-content">
      <template v-if="!loading && catalogData && catalogData.domains && catalogData.domains.length > 0">
        <CatalogTreeComponent
          :data="catalogData"
          :loading="loading"
          :search-keyword="debouncedKeyword"
          @entity-click="onEntityClick"
        />
      </template>

      <!-- 加载中 / 空数据 -->
      <el-empty
        v-if="!loading && (!catalogData || !catalogData.domains || catalogData.domains.length === 0)"
        description="暂无知识目录数据"
      />
    </el-card>

    <!-- 实体详情弹窗 -->
    <el-dialog
      v-model="entityDialogVisible"
      :title="selectedEntity?.entityName ?? '实体详情'"
      width="520px"
      destroy-on-close
    >
      <template v-if="selectedEntity && selectedSource && selectedDomain">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="实体名称">
            {{ selectedEntity.entityName }}
          </el-descriptions-item>
          <el-descriptions-item label="所属域">
            {{ selectedDomain.domainName }}
          </el-descriptions-item>
          <el-descriptions-item label="所属数据源">
            {{ selectedSource.sourceName }}
          </el-descriptions-item>
          <el-descriptions-item label="记录数量">
            {{ selectedEntity.recordCount }} 条
          </el-descriptions-item>
          <el-descriptions-item label="最后更新时间">
            {{ formatDateTime(selectedEntity.lastUpdated) || '—' }}
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <template #footer>
        <el-button @click="entityDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.catalog-view {
  padding: 20px;
}

.catalog-title {
  font-size: 20px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 20px;
}

.catalog-search {
  margin-bottom: 20px;
  max-width: 480px;
}

.catalog-content {
  min-height: 400px;
}
</style>
