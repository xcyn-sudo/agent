<script setup lang="ts">
</script>

<template>
  <div class="guest-layout">
    <div class="guest-layout__content">
      <router-view />
    </div>
  </div>
</template>

<style scoped lang="scss">
.guest-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: $bg-color;

  &__content {
    width: 100%;
  }
}
</style>
