<template>
  <span class="merchant-action-label">
    <span :class="{ 'merchant-label-hidden': busy }">{{ label }}</span>
    <span v-if="busy" class="merchant-action-pending" aria-live="polite"><i class="merchant-motion-spinner" aria-hidden="true"></i>{{ pending }}</span>
  </span>
</template>
<script setup>
defineProps({ label: String, pending: { type: String, default: '提交中' }, busy: Boolean });
</script>
