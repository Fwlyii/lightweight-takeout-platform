<template>
  <button class="merchant-logout-button" type="button" aria-label="退出商家登录" @click="logout">
    <i class="fas fa-sign-out-alt" aria-hidden="true"></i>
    <span>退出登录</span>
  </button>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { clearAuth } from '../utils/auth';

const router = useRouter();

const logout = () => {
  clearAuth();
  router.replace({ path: '/login', query: { role: 'merchant' } });
};
</script>

<style scoped>
.merchant-logout-button {
  position: absolute;
  top: 50%;
  right: 14px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  min-width: 76px !important;
  height: 34px !important;
  padding: 0 10px !important;
  border: 1px solid rgba(255, 255, 255, .72) !important;
  border-radius: 7px !important;
  background: rgba(255, 255, 255, .16) !important;
  color: #fff !important;
  font: inherit !important;
  font-size: 12px !important;
  font-weight: 600 !important;
  line-height: 1 !important;
  cursor: pointer !important;
  transform: translateY(-50%) !important;
  transition: background .18s ease, border-color .18s ease, transform .18s ease;
}

.merchant-logout-button:hover,
.merchant-logout-button:focus-visible {
  background: rgba(255, 255, 255, .28) !important;
  border-color: #fff !important;
  outline: none !important;
}

.merchant-logout-button:active { transform: translateY(-50%) scale(.97); }
.merchant-logout-button i { font-size: 13px; }
</style>
