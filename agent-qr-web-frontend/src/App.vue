<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import GuestLayout from '@/components/layout/GuestLayout.vue'
import MainLayout from '@/components/layout/MainLayout.vue'

const route = useRoute()

const layout = computed(() => {
  return route.meta.layout === 'main' ? 'main' : 'guest'
})
</script>

<template>
  <component :is="layout === 'main' ? MainLayout : GuestLayout" />
</template>
