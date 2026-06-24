<script setup lang="ts">
import type { UserInfo } from '@/types'
import { formatDateTime } from '@/utils/format'

defineProps<{
  users: UserInfo[]
  loading: boolean
}>()

const emit = defineEmits<{
  edit: [user: UserInfo]
  toggleStatus: [user: UserInfo]
}>()
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
</style>
