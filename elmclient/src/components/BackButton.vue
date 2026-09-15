<template>
  <button type="button" class="back-button" aria-label="返回" @click="goBack">
    <span class="back-arrow" aria-hidden="true">‹</span>
  </button>
</template>

<script>
import { useRoute, useRouter } from 'vue-router';
import { navigateBack } from '../utils/backNavigation';

export default {
  name: 'BackButton',
  setup() {
    const router = useRouter();
    const route = useRoute();
    const goBack = () => navigateBack(router, route);
    return { goBack };
  }
};
</script>

<style scoped>
.back-button {
  position: fixed;
  left: 16px;
  top: 14px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: #fff;
  color: var(--skin-brand, #167bc0);
  border: 1px solid var(--skin-border, #d7e8f6);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(var(--skin-brand-strong-rgb, 31, 93, 145), 0.08);
  user-select: none;
  z-index: 1005;
  font-size: 16px;
}
.back-button:hover { background: var(--skin-surface, #f1f8ff); color: var(--skin-brand, #0097ff); }
.back-arrow { font-family: Arial, sans-serif; font-size: 30px; line-height: 1; transform: translateY(-1px); }
</style>
