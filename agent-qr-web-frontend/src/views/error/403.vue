<script setup lang="ts">
import { useRouter } from 'vue-router'
import FlatIllustration from '@/components/common/FlatIllustration.vue'

const router = useRouter()

function goHome() {
  router.push('/chat')
}
</script>

<template>
  <div class="error-page">
    <div class="error-page__illustration">
      <FlatIllustration name="error-403" />
    </div>
    <div class="error-page__code">403</div>
    <p class="error-page__desc">抱歉，您没有权限访问此页面</p>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </div>
</template>

<style scoped lang="scss">
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;

  &__illustration {
    width: 200px;
    margin-bottom: var(--space-4);
  }

  &__code {
    font-size: var(--font-size-4xl);
    color: $text-secondary;
    font-weight: var(--font-weight-bold);
    line-height: 1;
    margin-bottom: var(--space-4);
  }

  &__desc {
    font-size: $font-size-large;
    color: $text-regular;
    margin-bottom: var(--space-6);
  }
}
</style>
