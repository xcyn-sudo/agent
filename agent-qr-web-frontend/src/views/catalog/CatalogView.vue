<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { catalogApi } from '@/api/catalog'
import type { CatalogTree, EntityNode, SourceNode, DomainNode } from '@/types'
import { formatDateTime } from '@/utils/format'
import CatalogTreeComponent from '@/components/catalog/CatalogTree.vue'
import KnowledgeGraph from '@/components/charts/KnowledgeGraph.vue'

const { t } = useI18n()

// ==================== 状态管理 ====================
const catalogData = ref<CatalogTree | null>(null)
const loading = ref(false)
const searchKeyword = ref('')
const debouncedKeyword = ref('')
const activeTab = ref('tree')

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
    ElMessage.error(t('catalog.loadError'))
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
    <h2 class="catalog-title">{{ $t('catalog.title') }}</h2>

    <!-- 顶部搜索栏 -->
    <div class="catalog-search">
      <el-input
        v-model="searchKeyword"
        :placeholder="$t('catalog.searchPlaceholder')"
        clearable
        @input="onSearchInput"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 标签页切换 -->
    <el-tabs v-model="activeTab" class="catalog-tabs">
      <el-tab-pane :label="$t('catalog.treeView')" name="tree">
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
            :description="$t('catalog.noCatalogData')"
          />
        </el-card>
      </el-tab-pane>

      <el-tab-pane :label="$t('catalog.graphView')" name="graph">
        <KnowledgeGraph v-if="catalogData" :catalog-tree="catalogData" />
        <el-empty v-else :description="$t('common.noData')" />
      </el-tab-pane>
    </el-tabs>

    <!-- 实体详情弹窗 -->
    <el-dialog
      v-model="entityDialogVisible"
      :title="selectedEntity?.entityName ?? $t('catalog.entityDetail')"
      width="520px"
      destroy-on-close
    >
      <template v-if="selectedEntity && selectedSource && selectedDomain">
        <el-descriptions :column="1" border>
          <el-descriptions-item :label="$t('catalog.entityName')">
            {{ selectedEntity.entityName }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('catalog.belongDomain')">
            {{ selectedDomain.domainName }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('catalog.belongSource')">
            {{ selectedSource.sourceName }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('catalog.recordCount')">
            {{ selectedEntity.recordCount }} {{ t('catalog.recordCountUnit') }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('catalog.lastUpdated')">
            {{ formatDateTime(selectedEntity.lastUpdated) || '—' }}
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <template #footer>
        <el-button @click="entityDialogVisible = false">{{ $t('common.close') }}</el-button>
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
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: $text-primary;
  margin-bottom: var(--space-5);
}

.catalog-search {
  margin-bottom: var(--space-5);
  max-width: 480px;
}

.catalog-tabs {
  margin-bottom: var(--space-5);
}

.catalog-content {
  min-height: 400px;
}
</style>
