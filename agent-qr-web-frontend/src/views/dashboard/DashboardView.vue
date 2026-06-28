<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import { PieChart, LineChart } from 'echarts/charts'
import { TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { statisticsApi } from '@/api/statistics'
import type { DashboardVO, DailyStats } from '@/types'

// 注册 ECharts 组件
use([CanvasRenderer, PieChart, LineChart, TooltipComponent, GridComponent, LegendComponent])

const { t } = useI18n()

// ---------- 状态 ----------
const loading = ref(false)
const dashboard = ref<DashboardVO>({
  todayQA: 0,
  todayNewUsers: 0,
  totalDocuments: 0,
  totalChunks: 0,
  totalUsers: 0,
  todayPositive: 0,
  todayNegative: 0,
  satisfactionRate: 0,
  weeklyTrend: [],
  docTypeDistribution: {},
})

const dateRange = ref<[Date, Date] | null>(null)
const trendChartRef = ref<any>(null)
const feedbackChartRef = ref<any>(null)
const docTypeChartRef = ref<any>(null)

// ---------- 计算属性 ----------

/** 满意度百分比格式化 */
const satisfactionPercent = computed(() => {
  const rate = dashboard.value.satisfactionRate ?? 0
  return (rate * 100).toFixed(1) + '%'
})

/** 满意度颜色 */
const satisfactionColor = computed(() => {
  const rate = (dashboard.value.satisfactionRate ?? 0) * 100
  if (rate >= 80) return '#67c23a'
  if (rate >= 50) return '#e6a23c'
  return '#f56c6c'
})

/** 折线图 option */
const lineChartOption = computed(() => {
  const data = dashboard.value.weeklyTrend
  const dates = data.map((item: DailyStats) => formatDate(item.statDate))
  const qaCounts = data.map((item: DailyStats) => item.qaCount)
  const satRates = data.map((item: DailyStats) =>
    item.satisfactionRate != null ? +(item.satisfactionRate * 100).toFixed(1) : null,
  )

  return {
    tooltip: {
      trigger: 'axis' as const,
      formatter: (params: any[]) => {
        let result = params[0]?.axisValue || ''
        params.forEach((p: any) => {
          if (p.seriesName === t('dashboard.satisfaction')) {
            result += `<br/>${p.marker} ${p.seriesName}: ${p.value != null ? p.value + '%' : '-'}`
          } else {
            result += `<br/>${p.marker} ${p.seriesName}: ${p.value ?? '-'}`
          }
        })
        return result
      },
    },
    legend: {
      data: [t('dashboard.qaCount'), t('dashboard.satisfaction')],
      bottom: 0,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      containLabel: true,
    },
    xAxis: {
      type: 'category' as const,
      boundaryGap: false,
      data: dates,
    },
    yAxis: [
      {
        type: 'value' as const,
        name: t('dashboard.qaAxisName'),
        minInterval: 1,
      },
      {
        type: 'value' as const,
        name: t('dashboard.satisfactionAxis'),
        min: 0,
        max: 100,
      },
    ],
    series: [
      {
        name: t('dashboard.qaCount'),
        type: 'line' as const,
        smooth: true,
        data: qaCounts,
        itemStyle: { color: '#409eff' },
        lineStyle: { color: '#409eff' },
        areaStyle: {
          color: {
            type: 'linear' as const,
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.05)' },
            ],
          },
        },
      },
      {
        name: t('dashboard.satisfaction'),
        type: 'line' as const,
        yAxisIndex: 1,
        smooth: true,
        data: satRates,
        itemStyle: { color: '#67c23a' },
        lineStyle: {
          color: '#67c23a',
          type: 'dashed',
        },
      },
    ],
  }
})

/** 文档类型饼图 option */
const pieChartOption = computed(() => {
  const dist = dashboard.value.docTypeDistribution
  const data = Object.entries(dist).map(([name, value]) => ({ name, value }))
  const colorPalette = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399']

  return {
    tooltip: {
      trigger: 'item' as const,
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      orient: 'vertical' as const,
      right: '5%',
      top: 'center',
    },
    color: colorPalette,
    series: [
      {
        name: t('dashboard.chartDocType'),
        type: 'pie' as const,
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          formatter: '{b}: {d}%',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
          },
        },
        data,
      },
    ],
  }
})

/** 反馈分布饼图 option */
const feedbackPieOption = computed(() => {
  const positive = dashboard.value.todayPositive ?? 0
  const negative = dashboard.value.todayNegative ?? 0
  const data = [
    { name: t('dashboard.feedbackPositive'), value: positive, itemStyle: { color: '#67c23a' } },
    { name: t('dashboard.feedbackNegative'), value: negative, itemStyle: { color: '#f56c6c' } },
  ]

  return {
    tooltip: {
      trigger: 'item' as const,
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      orient: 'vertical' as const,
      right: '5%',
      top: 'center',
    },
    series: [
      {
        name: t('dashboard.feedbackDistribution'),
        type: 'pie' as const,
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          formatter: '{b}: {d}%',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
          },
        },
        data,
      },
    ],
  }
})

/** 是否有反馈数据 */
const hasFeedbackData = computed(() => {
  return (dashboard.value.todayPositive ?? 0) > 0 || (dashboard.value.todayNegative ?? 0) > 0
})

// ---------- 工具方法 ----------
function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  // 兼容 yyyy-MM-dd 格式，提取 MM-dd
  const parts = dateStr.split('-')
  if (parts.length >= 3) {
    return `${parts[1]}-${parts[2]}`
  }
  return dateStr
}

function exportChart(chartInstance: any) {
  if (!chartInstance) return
  const url = chartInstance.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#fff' })
  const link = document.createElement('a')
  link.download = 'chart.png'
  link.href = url
  link.click()
}

function handleChartClick(params: any) {
  console.log('Chart drill-down:', params)
}

function handleDateRangeChange() {
  fetchDashboard()
}

// ---------- 数据加载 ----------
async function fetchDashboard() {
  loading.value = true
  try {
    const res = await statisticsApi.getDashboard()
    if (res?.data) {
      dashboard.value = res.data
    }
  } catch {
    ElMessage.error(t('dashboard.loadError'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDashboard()
})
</script>

<template>
  <div class="dashboard-view" v-loading="loading">
    <h2 class="dashboard-title">{{ $t('dashboard.title') }}</h2>

    <!-- 统计卡片区 - 第一行 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-number">{{ dashboard.todayQA }}</div>
          <div class="stat-label">{{ $t('dashboard.todayQA') }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-number">{{ dashboard.todayNewUsers }}</div>
          <div class="stat-label">{{ $t('dashboard.todayNewUsers') }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-number">{{ dashboard.totalDocuments }}</div>
          <div class="stat-label">{{ $t('dashboard.totalDocuments') }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-number">{{ dashboard.totalUsers }}</div>
          <div class="stat-label">{{ $t('dashboard.totalUsers') }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 统计卡片区 - 第二行（P2 新增） -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-number">{{ dashboard.todayPositive }}</div>
          <div class="stat-label">👍 {{ $t('dashboard.likes') }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-number">{{ dashboard.todayNegative }}</div>
          <div class="stat-label">👎 {{ $t('dashboard.dislikes') }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-number" :style="{ color: satisfactionColor }">{{ satisfactionPercent }}</div>
          <div class="stat-label">{{ $t('dashboard.satisfaction') }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 日期范围选择 -->
    <el-row :gutter="20" class="chart-section">
      <el-col :span="24">
        <el-card shadow="hover">
          <div style="display: flex; align-items: center; gap: 12px;">
            <span>{{ $t('dashboard.dateRange') }}:</span>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="-"
              :start-placeholder="$t('common.all')"
              :end-placeholder="$t('common.all')"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="handleDateRangeChange"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <!-- 第一行：趋势图全宽 -->
    <el-row :gutter="20" class="chart-section">
      <el-col :span="24">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="chart-card-header">
              <span class="chart-card-title">{{ $t('dashboard.qaTrend') }}</span>
              <el-button
                v-permission="'canExportReport'"
                size="small"
                @click="exportChart(trendChartRef?.chart || trendChartRef)"
              >{{ $t('dashboard.exportChart') }}</el-button>
            </div>
          </template>
          <v-chart
            ref="trendChartRef"
            v-if="dashboard.weeklyTrend.length > 0"
            :option="lineChartOption"
            style="height: 350px"
            @click="handleChartClick"
          />
          <el-empty v-else :description="$t('dashboard.noTrendData')" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行：反馈分布 + 文档类型分布 -->
    <el-row :gutter="20" class="chart-section">
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="chart-card-header">
              <span class="chart-card-title">{{ $t('dashboard.feedbackDistribution') }}</span>
              <el-button
                v-permission="'canExportReport'"
                size="small"
                @click="exportChart(feedbackChartRef?.chart || feedbackChartRef)"
              >{{ $t('dashboard.exportChart') }}</el-button>
            </div>
          </template>
          <v-chart
            ref="feedbackChartRef"
            v-if="hasFeedbackData"
            :option="feedbackPieOption"
            style="height: 350px"
            @click="handleChartClick"
          />
          <el-empty v-else :description="$t('dashboard.noFeedbackData')" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="chart-card-header">
              <span class="chart-card-title">{{ $t('dashboard.documentTypeDistribution') }}</span>
              <el-button
                v-permission="'canExportReport'"
                size="small"
                @click="exportChart(docTypeChartRef?.chart || docTypeChartRef)"
              >{{ $t('dashboard.exportChart') }}</el-button>
            </div>
          </template>
          <v-chart
            ref="docTypeChartRef"
            v-if="Object.keys(dashboard.docTypeDistribution).length > 0"
            :option="pieChartOption"
            style="height: 350px"
            @click="handleChartClick"
          />
          <el-empty v-else :description="$t('dashboard.noDocData')" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.dashboard-view {
  padding: 20px;
}

.dashboard-title {
  font-size: 20px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 20px;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;

  .stat-number {
    font-size: 32px;
    font-weight: 700;
    color: $primary-color;
    line-height: 1.2;
  }

  .stat-label {
    font-size: 13px;
    color: $text-secondary;
    margin-top: 8px;
  }
}

.chart-section {
  margin-bottom: 20px;
}

.chart-card {
  .chart-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .chart-card-title {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
  }
}
</style>
