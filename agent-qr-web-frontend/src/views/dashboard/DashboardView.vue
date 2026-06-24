<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
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

// ---------- 状态 ----------
const loading = ref(false)
const dashboard = ref<DashboardVO>({
  todayQA: 0,
  todayNewUsers: 0,
  totalDocuments: 0,
  totalChunks: 0,
  totalUsers: 0,
  weeklyTrend: [],
  docTypeDistribution: {},
})

// ---------- 计算属性 ----------

/** 折线图 option */
const lineChartOption = computed(() => {
  const data = dashboard.value.weeklyTrend
  return {
    tooltip: {
      trigger: 'axis' as const,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category' as const,
      boundaryGap: false,
      data: data.map((item: DailyStats) => formatDate(item.statDate)),
    },
    yAxis: {
      type: 'value' as const,
      minInterval: 1,
    },
    series: [
      {
        name: '问答数量',
        type: 'line' as const,
        smooth: true,
        data: data.map((item: DailyStats) => item.qaCount),
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
    ],
  }
})

/** 饼图 option */
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
        name: '文档类型',
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

// ---------- 数据加载 ----------
async function fetchDashboard() {
  loading.value = true
  try {
    const res = await statisticsApi.getDashboard()
    if (res?.data) {
      dashboard.value = res.data
    }
  } catch {
    ElMessage.error('数据加载失败')
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
    <h2 class="dashboard-title">数据仪表盘</h2>

    <!-- 统计卡片区 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-number">{{ dashboard.todayQA }}</div>
          <div class="stat-label">今日问答</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-number">{{ dashboard.todayNewUsers }}</div>
          <div class="stat-label">今日新增用户</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-number">{{ dashboard.totalDocuments }}</div>
          <div class="stat-label">文档总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-number">{{ dashboard.totalUsers }}</div>
          <div class="stat-label">用户总数</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="20" class="chart-section">
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span class="chart-card-title">近 7 天问答趋势</span>
          </template>
          <v-chart
            v-if="dashboard.weeklyTrend.length > 0"
            :option="lineChartOption"
            style="height: 350px"
          />
          <el-empty v-else description="暂无趋势数据" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span class="chart-card-title">文档类型分布</span>
          </template>
          <v-chart
            v-if="Object.keys(dashboard.docTypeDistribution).length > 0"
            :option="pieChartOption"
            style="height: 350px"
          />
          <el-empty v-else description="暂无文档数据" />
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
  .chart-card-title {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
  }
}
</style>
