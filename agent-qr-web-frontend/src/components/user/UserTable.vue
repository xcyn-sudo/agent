<script setup lang="ts">
import type { UserInfo } from '@/types'
import { DEPARTMENTS } from '@/types'
import { formatDateTime, formatSensitivityLevel } from '@/utils/format'

defineProps<{
  users: UserInfo[]
  loading: boolean
}>()

const emit = defineEmits<{
  edit: [user: UserInfo]
  toggleStatus: [user: UserInfo]
}>()

function getDepartmentLabel(department: string): string {
  const item = DEPARTMENTS.find((d) => d.value === department)
  return item?.label || department
}

function getTitleLabel(title: string): string {
  const map: Record<string, string> = {
    employee: '员工',
    manager: '经理',
    director: '总监',
  }
  return map[title] || title
}

function parseDomains(raw: string): string[] {
  if (!raw) return []
  return raw.split(',').map((d) => d.trim()).filter(Boolean)
}
</script>

<template>
  <el-table :data="users" v-loading="loading" stripe border style="width: 100%">
    <template #empty>
      <el-empty description="暂无用户" />
    </template>

    <el-table-column prop="id" label="ID" width="60" align="center" />
    <el-table-column prop="username" label="用户名" width="120" />
    <el-table-column prop="realName" label="姓名" width="100" />
    <el-table-column prop="role" label="角色" width="100" align="center">
      <template #default="{ row }">
        <el-tag v-if="row.role === 'admin'" type="danger">admin</el-tag>
        <el-tag v-else type="info">user</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="department" label="部门" width="110" align="center">
      <template #default="{ row }">
        {{ getDepartmentLabel(row.department) }}
      </template>
    </el-table-column>
    <el-table-column prop="clearanceLevel" label="密级" width="80" align="center">
      <template #default="{ row }">
        <span :class="`sensitivity-tag sensitivity-tag--${row.clearanceLevel}`">
          {{ formatSensitivityLevel(row.clearanceLevel) }}
        </span>
      </template>
    </el-table-column>
    <el-table-column prop="allowedDomains" label="允许域" width="200">
      <template #default="{ row }">
        <span
          v-for="domain in parseDomains(row.allowedDomains)"
          :key="domain"
          class="domain-tag"
        >
          {{ domain }}
        </span>
      </template>
    </el-table-column>
    <el-table-column prop="title" label="职级" width="80" align="center">
      <template #default="{ row }">
        {{ getTitleLabel(row.title) }}
      </template>
    </el-table-column>
    <el-table-column prop="status" label="状态" width="80" align="center">
      <template #default="{ row }">
        <el-tag v-if="row.status === 1" type="success">启用</el-tag>
        <el-tag v-else type="danger">禁用</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="创建时间" width="160" align="center">
      <template #default="{ row }">
        {{ formatDateTime(row.createTime) }}
      </template>
    </el-table-column>
    <el-table-column label="操作" width="140" fixed="right" align="center">
      <template #default="{ row }">
        <el-button type="primary" link @click="emit('edit', row)">编辑</el-button>
        <el-button v-if="row.status === 1" link @click="emit('toggleStatus', row)">禁用</el-button>
        <el-button v-else link @click="emit('toggleStatus', row)">启用</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped lang="scss">
.sensitivity-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;

  &--0 {
    color: #67c23a;
    background: #f0f9eb;
  }
  &--1 {
    color: #409eff;
    background: #ecf5ff;
  }
  &--2 {
    color: #e6a23c;
    background: #fdf6ec;
  }
  &--3 {
    color: #f56c6c;
    background: #fef0f0;
  }
}

.domain-tag {
  display: inline-block;
  padding: 2px 6px;
  margin: 2px 4px 2px 0;
  border-radius: 4px;
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
}
</style>
