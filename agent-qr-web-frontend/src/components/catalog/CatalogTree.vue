<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { ElTree } from 'element-plus'
import type { CatalogTree, DomainNode, SourceNode, EntityNode } from '@/types'
import { formatSourceType, formatDateTime } from '@/utils/format'

const props = defineProps<{
  data: CatalogTree | null
  loading: boolean
  searchKeyword: string
}>()

const emit = defineEmits<{
  'entity-click': [entity: EntityNode, source: SourceNode, domain: DomainNode]
}>()

const treeRef = ref<InstanceType<typeof ElTree>>()

// ==================== 将 CatalogTree 转换为 el-tree 可用格式 ====================
const treeData = computed(() => {
  if (!props.data?.domains) return []
  return props.data.domains.map((domain) => ({
    id: domain.domainName,
    label: domain.domainName,
    domain: domain,
    children: domain.sources.map((source) => ({
      id: `${domain.domainName}-${source.sourceId}`,
      label: source.sourceName,
      source: source,
      domain: domain,
      children: source.entities.map((entity) => ({
        id: `${domain.domainName}-${source.sourceId}-${entity.entityName}`,
        label: entity.entityName,
        entity: entity,
        source: source,
        domain: domain,
        isLeaf: true,
      })),
    })),
  }))
})

// ==================== 搜索过滤 ====================
function filterNodeMethod(value: string, data: any): boolean {
  if (!value) return true
  const keyword = value.toLowerCase()
  return (
    data.domain?.domainName?.toLowerCase().includes(keyword) ||
    data.source?.sourceName?.toLowerCase().includes(keyword) ||
    data.entity?.entityName?.toLowerCase().includes(keyword)
  )
}

watch(
  () => props.searchKeyword,
  (keyword) => {
    treeRef.value?.filter(keyword)
  },
)
</script>

<template>
  <div class="catalog-tree" v-loading="loading">
    <el-empty v-if="!loading && (!treeData || treeData.length === 0)" description="暂无知识目录数据" />

    <el-tree
      v-else
      ref="treeRef"
      :data="treeData"
      node-key="id"
      default-expand-all
      highlight-current
      :filter-node-method="filterNodeMethod"
      :expand-on-click-node="false"
    >
      <template #default="{ data: nodeData }">
        <!-- 一级节点：业务域 -->
        <span
          v-if="nodeData.domain && !nodeData.source && !nodeData.entity"
          class="tree-node tree-node--domain"
        >
          <span class="tree-node-icon">📁</span>
          <span class="tree-node-label">{{ nodeData.domain.domainName }}</span>
          <span class="tree-node-meta">
            ({{ nodeData.domain.sourceCount }}个数据源 · {{ nodeData.domain.totalEntities }}个实体)
          </span>
        </span>

        <!-- 二级节点：数据源 -->
        <span
          v-else-if="nodeData.source && !nodeData.entity"
          class="tree-node tree-node--source"
        >
          <span class="tree-node-icon">📂</span>
          <span class="tree-node-label">{{ nodeData.source.sourceName }}</span>
          <span class="tree-node-meta">
            ({{ formatSourceType(nodeData.source.sourceType) }})
            最后同步: {{ formatDateTime(nodeData.source.lastSyncAt) || '—' }} · {{ nodeData.source.totalSynced }}条
          </span>
        </span>

        <!-- 三级节点：实体（叶子） -->
        <span
          v-else-if="nodeData.entity"
          class="tree-node tree-node--entity"
          @click="emit('entity-click', nodeData.entity, nodeData.source, nodeData.domain!)"
        >
          <span class="tree-node-icon">📄</span>
          <span class="tree-node-label">{{ nodeData.entity.entityName }}</span>
          <span class="tree-node-meta">{{ nodeData.entity.recordCount }}条</span>
        </span>
      </template>
    </el-tree>
  </div>
</template>

<style scoped lang="scss">
.catalog-tree {
  min-height: 200px;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  flex: 1;

  &-icon {
    flex-shrink: 0;
    font-size: 16px;
  }

  &-label {
    font-weight: 500;
    color: #303133;
  }

  &-meta {
    font-size: 12px;
    color: #909399;
  }
}

.tree-node--entity {
  cursor: pointer;

  &:hover {
    color: #409eff;
    .tree-node-label {
      color: #409eff;
    }
  }
}
</style>
