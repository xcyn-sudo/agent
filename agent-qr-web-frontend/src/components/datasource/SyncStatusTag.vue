<script setup lang="ts">
defineProps<{ status: 'ACTIVE' | 'INACTIVE' | 'ERROR' }>()
</script>

<template>
  <el-tag v-if="status === 'ACTIVE'" type="success">
    <el-icon><CircleCheck /></el-icon> 活跃
  </el-tag>
  <el-tag v-else-if="status === 'ERROR'" type="danger">
    <el-icon><CircleClose /></el-icon> 异常
  </el-tag>
  <el-tag v-else type="info">
    <el-icon><Remove /></el-icon> 停用
  </el-tag>
</template>

<style scoped lang="scss">
</style>
