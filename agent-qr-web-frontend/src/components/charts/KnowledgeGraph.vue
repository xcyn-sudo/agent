<template>
  <div class="knowledge-graph" ref="chartRef" v-loading="loading"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import type { CatalogTree, DomainNode, SourceNode, EntityNode } from '@/types'

const props = defineProps<{
  catalogTree: CatalogTree | null
}>()

const chartRef = ref<HTMLElement | null>(null)
const loading = ref(false)
let chartInstance: echarts.ECharts | null = null

interface GraphNode {
  id: string
  name: string
  category: number
  symbolSize: number
  itemStyle?: { color: string }
  label?: { show: boolean; fontSize: number }
}

interface GraphLink {
  source: string
  target: string
  lineStyle?: { color: string; width: number; curveness: number }
}

const CATEGORY_COLORS: Record<string, string> = {
  domain: '#409EFF',
  source: '#67C23A',
  entity: '#E6A23C'
}

function buildGraphData(tree: CatalogTree): { nodes: GraphNode[]; links: GraphLink[] } {
  const nodes: GraphNode[] = []
  const links: GraphLink[] = []
  const nodeIds = new Set<string>()

  for (const domain of tree.domains || []) {
    const domainId = `domain:${domain.domainName}`
    if (!nodeIds.has(domainId)) {
      nodes.push({
        id: domainId,
        name: domain.domainName,
        category: 0,
        symbolSize: 50,
        itemStyle: { color: CATEGORY_COLORS.domain }
      })
      nodeIds.add(domainId)
    }

    for (const source of domain.sources || []) {
      const sourceId = `source:${source.sourceId}`
      if (!nodeIds.has(sourceId)) {
        nodes.push({
          id: sourceId,
          name: source.sourceName,
          category: 1,
          symbolSize: 30,
          itemStyle: { color: CATEGORY_COLORS.source }
        })
        nodeIds.add(sourceId)
      }
      links.push({
        source: domainId,
        target: sourceId,
        lineStyle: { color: '#C0C4CC', width: 1, curveness: 0.1 }
      })

      for (const entity of source.entities || []) {
        const entityId = `entity:${source.sourceId}:${entity.entityName}`
        if (!nodeIds.has(entityId)) {
          nodes.push({
            id: entityId,
            name: entity.entityName,
            category: 2,
            symbolSize: 15,
            itemStyle: { color: CATEGORY_COLORS.entity }
          })
          nodeIds.add(entityId)
        }
        links.push({
          source: sourceId,
          target: entityId,
          lineStyle: { color: '#E4E7ED', width: 0.5, curveness: 0.2 }
        })
      }
    }
  }

  return { nodes, links }
}

function initChart() {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  renderChart()
}

function renderChart() {
  if (!chartInstance || !props.catalogTree) return

  const { nodes, links } = buildGraphData(props.catalogTree)

  if (nodes.length === 0) return

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        if (params.dataType === 'node') {
          const cat = params.data.category === 0 ? '域' : params.data.category === 1 ? '数据源' : '实体'
          return `<b>${cat}: ${params.name}</b>`
        }
        return ''
      }
    },
    legend: {
      data: ['业务域', '数据源', '实体'],
      bottom: 10
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        roam: true,
        draggable: true,
        force: {
          repulsion: 300,
          gravity: 0.1,
          edgeLength: [80, 200],
          layoutAnimation: true
        },
        data: nodes.map((n) => ({
          ...n,
          label: {
            show: n.category === 0,
            fontSize: n.category === 0 ? 14 : 10
          }
        })),
        links: links,
        categories: [
          { name: '业务域', itemStyle: { color: CATEGORY_COLORS.domain } },
          { name: '数据源', itemStyle: { color: CATEGORY_COLORS.source } },
          { name: '实体', itemStyle: { color: CATEGORY_COLORS.entity } }
        ],
        emphasis: {
          focus: 'adjacency',
          lineStyle: { width: 3 }
        },
        scaleLimit: {
          min: 0.3,
          max: 3
        }
      }
    ]
  }

  chartInstance.setOption(option, true)
}

watch(() => props.catalogTree, () => {
  if (props.catalogTree) {
    loading.value = false
    nextTick(() => renderChart())
  } else {
    loading.value = true
  }
}, { deep: true })

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  nextTick(() => initChart())
  // ResizeObserver 同时覆盖窗口缩放和标签页切换场景
  if (chartRef.value) {
    resizeObserver = new ResizeObserver(() => {
      chartInstance?.resize()
    })
    resizeObserver.observe(chartRef.value)
  }
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
})
</script>

<style scoped>
.knowledge-graph {
  width: 100%;
  height: 600px;
  min-height: 400px;
}

@media (max-width: 767px) {
  .knowledge-graph {
    height: 400px;
  }
}
</style>
