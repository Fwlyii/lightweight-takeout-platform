<template>
  <span class="rider-action-copy">
    <span :class="{ 'rider-label-hidden': busy || success }">{{ label }}</span>
    <span v-if="busy || success" class="rider-action-overlay" aria-live="polite">
      <i v-if="busy" class="rider-action-spinner" aria-hidden="true"></i>
      <i v-else class="fa fa-check rider-action-check" aria-hidden="true"></i>
      {{ busy ? pending : successLabel }}
    </span>
  </span>
</template>

<script setup>
defineProps({ label: String, pending: String, busy: Boolean, success: Boolean, successLabel: { type: String, default: '操作成功' } });
</script>
