<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import type { QualityReport, QualityFailure } from '@/types'
import { dataqualityApi } from '@/api/dataquality'
import { formatPassRate, formatDateTime } from '@/utils/format'
import Pagination from '@/components/common/Pagination.vue'

const { t } = useI18n()
const router = useRouter()

// --- 状态 ---
const reports = ref<QualityReport[]>([])
const total = ref(0)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const filterBlocked = ref<string>('')

// --- 规则分布颜色池 ---
const RULE_COLORS = [
  '#409eff',
  '#67c23a',
  '#e6a23c',
  '#f56c6c',
  '#909399',
  '#722ed1',
  '#13c2c2',
  '#eb2f96',
]

// --- 计算规则分布 (按 ruleName 分组) ---
interface RuleDistribution {
  ruleName: string
  count: number
  percentage: number
  color: string
}

function calcRuleDistribution(
  failures: QualityFailure[],
  failCount: number,
): RuleDistribution[] {
  if (!failures || failures.length === 0) return []
  const groupMap = new Map<string, number>()
  failures.forEach((f) => {
    groupMap.set(f.ruleName, (groupMap.get(f.ruleName) || 0) + 1)
  })
  let colorIdx = 0
  const dist: RuleDistribution[] = []
  groupMap.forEach((count, ruleName) => {
    dist.push({
      ruleName,
      count,
      percentage: failCount > 0 ? Math.round((count / failCount) * 100) : 0,
      color: RULE_COLORS[colorIdx % RULE_COLORS.length],
    })
    colorIdx++
  })
  return dist
}

// --- 获取报告列表 ---
async function fetchReports() {
  loading.value = true
  try {
    const params: { page: number; size: number; blocked?: boolean } = {
      page: currentPage.value,
      size: pageSize.value,
    }
    if (filterBlocked.value === 'blocked') {
      params.blocked = true
    } else if (filterBlocked.value === 'passed') {
      params.blocked = false
    }
    const res = await dataqualityApi.listReports(params)
    reports.value = res.data.records
    total.value = res.data.total
  } catch {
    // 错误已在拦截器中统一处理
  } finally {
    loading.value = false
  }
}

// --- 筛选变化 → 重置页码并重新加载 ---
function handleFilterChange() {
  currentPage.value = 1
  fetchReports()
}

// --- 分页变化 ---
function handlePageChange() {
  fetchReports()
}

// --- 行样式：阻断批次红色背景 ---
function tableRowClassName({ row }: { row: QualityReport }) {
  return row.blocked ? 'row--blocked' : ''
}

// --- 合格率单元格颜色 ---
function passRateColor(rate: number): string {
  if (rate >= 0.9) return '#67c23a'
  if (rate >= 0.6) return '#e6a23c'
  return '#f56c6c'
}

// --- 生命周期 ---
onMounted(() => {
  fetchReports()
})
</script>

<template>
  <div class="quality-report">
    <div class="quality-report__header">
      <h2 class="quality-report__title">{{ $t('quality.title') }}</h2>
      <el-button type="primary" @click="router.push('/admin/quality/rules')">
        {{ $t('quality.ruleManager') }}
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="quality-report__toolbar">
      <div class="quality-report__filter">
        <span class="filter-label">{{ $t('quality.blockingFilter') }}</span>
        <el-select
          v-model="filterBlocked"
          :placeholder="$t('common.all')"
          style="width: 140px"
          clearable
          @change="handleFilterChange"
        >
          <el-option :label="$t('common.all')" value="" />
          <el-option :label="$t('quality.onlyBlocked')" value="blocked" />
          <el-option :label="$t('quality.onlyPassed')" value="passed" />
        </el-select>
      </div>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="reports"
      :row-class-name="tableRowClassName"
      stripe
      border
      style="width: 100%"
      @sort-change="handleFilterChange"
    >
      <!-- 展开行 -->
      <el-table-column type="expand">
        <template #default="scope">
          <div class="expand-content">
            <!-- 空失败列表提示 -->
            <template v-if="!scope.row.failures || scope.row.failures.length === 0">
              <el-empty :description="$t('quality.noFailures')" :image-size="80" />
            </template>

            <!-- 不合格明细表 -->
            <template v-else>
              <div class="expand-section">
                <h4 class="expand-section__title">{{ $t('quality.detail') }}</h4>
                <el-table
                  :data="scope.row.failures"
                  size="small"
                  border
                  style="width: 100%"
                >
                  <el-table-column prop="ruleName" :label="$t('quality.ruleName')" min-width="150" />
                  <el-table-column prop="recordIndex" :label="$t('quality.recordIndex')" width="100" align="center" />
                  <el-table-column prop="reason" :label="$t('quality.reason')" min-width="250" show-overflow-tooltip />
                </el-table>
              </div>

              <!-- 按规则分布（进度条） -->
              <div class="expand-section">
                <h4 class="expand-section__title">{{ $t('quality.ruleDistribution') }}</h4>
                <div
                  v-for="item in calcRuleDistribution(scope.row.failures, scope.row.failCount)"
                  :key="item.ruleName"
                  class="rule-dist-item"
                >
                  <span class="rule-dist-item__name">{{ item.ruleName }}</span>
                  <div class="rule-dist-item__bar">
                    <el-progress
                      :percentage="item.percentage"
                      :max="100"
                      :color="item.color"
                      :show-text="false"
                    />
                  </div>
                  <span class="rule-dist-item__count">{{ item.count }}</span>
                  <span class="rule-dist-item__percent">{{ item.percentage }}%</span>
                </div>
              </div>
            </template>
          </div>
        </template>
      </el-table-column>

      <!-- 批次号 -->
      <el-table-column prop="batchId" :label="$t('quality.batchId')" min-width="180" show-overflow-tooltip />

      <!-- 数据源 -->
      <el-table-column prop="sourceName" :label="$t('quality.sourceName')" min-width="140" show-overflow-tooltip />

      <!-- 总数 -->
      <el-table-column prop="totalCount" :label="$t('quality.totalCount')" width="90" align="center" />

      <!-- 合格 -->
      <el-table-column prop="passCount" :label="$t('quality.passCount')" width="90" align="center" />

      <!-- 不合格 -->
      <el-table-column prop="failCount" :label="$t('quality.failCount')" width="90" align="center" />

      <!-- 合格率 ★ 条件颜色 + sortable -->
      <el-table-column
        prop="passRate"
        :label="$t('quality.passRate')"
        width="100"
        align="center"
        sortable="custom"
      >
        <template #default="scope">
          <span :style="{ color: passRateColor(scope.row.passRate), fontWeight: 600 }">
            {{ formatPassRate(scope.row.passRate) }}
          </span>
        </template>
      </el-table-column>

      <!-- 是否阻断 -->
      <el-table-column :label="$t('quality.isBlocked')" width="100" align="center">
        <template #default="scope">
          <el-tag v-if="scope.row.blocked" type="danger" size="small">{{ $t('quality.blocked') }}</el-tag>
          <el-tag v-else type="success" size="small">{{ $t('quality.passed') }}</el-tag>
        </template>
      </el-table-column>

      <!-- 检查时间 ★ sortable 默认降序 -->
      <el-table-column
        prop="checkTime"
        :label="$t('quality.checkTime')"
        width="170"
        align="center"
        sortable="custom"
      >
        <template #default="scope">
          {{ formatDateTime(scope.row.checkTime) }}
        </template>
      </el-table-column>
    </el-table>

    <!-- 空数据 -->
    <el-empty v-if="!loading && reports.length === 0" :description="$t('quality.noReportData')" />

    <!-- 分页 -->
    <Pagination
      v-if="total > 0"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      @change="handlePageChange"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.quality-report {
  padding: 20px;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;
  }

  &__title {
    font-size: 20px;
    font-weight: 600;
    color: $text-primary;
    margin: 0;
  }

  &__toolbar {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    margin-bottom: 16px;
  }

  &__filter {
    display: flex;
    align-items: center;
    gap: 8px;

    .filter-label {
      font-size: 14px;
      color: $text-secondary;
      white-space: nowrap;
    }
  }
}

// 阻断行高亮
:deep(.row--blocked) {
  background-color: #fef0f0;
}

// 展开行内容
.expand-content {
  padding: 16px 24px;
}

.expand-section {
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }

  &__title {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin: 0 0 12px 0;
    padding-left: 8px;
    border-left: 3px solid $primary-color;
  }
}

// 规则分布条目
.rule-dist-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;

  &:last-child {
    margin-bottom: 0;
  }

  &__name {
    min-width: 120px;
    font-size: 13px;
    color: $text-primary;
    flex-shrink: 0;
  }

  &__bar {
    flex: 1;
  }

  &__count {
    font-size: 13px;
    font-weight: 600;
    color: $text-primary;
    min-width: 36px;
    text-align: right;
  }

  &__percent {
    font-size: 12px;
    color: $text-secondary;
    min-width: 40px;
    text-align: right;
  }
}
</style>
