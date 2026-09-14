<template>
  <button class="role-logout-button" type="button" aria-label="退出登录" @click="logout">
    <i class="fas fa-sign-out-alt" aria-hidden="true"></i>
    <span>退出登录</span>
  </button>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { clearAuth } from '../utils/auth';

const props = defineProps({
  role: { type: String, required: true }
});

const router = useRouter();

const logout = () => {
  clearAuth();
  router.replace({ path: '/login', query: { role: props.role } });
};
</script>

<style scoped>
.role-logout-button {
  position: absolute;
  top: 14px;
  right: 14px;
  z-index: 20;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  gap: 5px;
  min-width: 76px !important;
  height: 34px !important;
  padding: 0 10px !important;
  border: 1px solid #b9d9ef !important;
  border-radius: 7px !important;
  background: #fff !important;
  color: #0879c7 !important;
  font: inherit !important;
  font-size: 12px !important;
  font-weight: 600 !important;
  line-height: 1 !important;
  cursor: pointer !important;
  box-shadow: 0 2px 8px rgba(32, 101, 157, .12);
  transition: background .18s ease, border-color .18s ease, transform .18s ease;
}

.role-logout-button:hover,
.role-logout-button:focus-visible {
  background: #f1f8fe !important;
  border-color: #78b8df !important;
  outline: none !important;
}

.role-logout-button:active { transform: scale(.97); }
.role-logout-button i { font-size: 13px; }
</style>
