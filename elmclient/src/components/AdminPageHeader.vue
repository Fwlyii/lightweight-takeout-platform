<template>
  <header class="admin-page-header">
    <button v-if="backTo" type="button" @click="router.push(backTo)" aria-label="返回管理页面">
      <span aria-hidden="true">‹</span> 返回
    </button>
    <span v-else aria-hidden="true"></span>
    <h1>{{ title }}</h1>
    <span aria-hidden="true"></span>
  </header>
</template>
<script setup>
import { useRouter } from 'vue-router';
defineProps({ title: String, backTo: { type: String, default: '/admin/home' } });
const router = useRouter();
</script>
<style scoped>
.admin-page-header {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr) 76px;
  align-items: center;
  width: 100%;
  min-height: 64px;
  padding: 8px 12px;
  background: var(--skin-brand, #087ecc);
  color: white;
  flex: none;
}
.admin-page-header h1 { margin: 0; text-align: center; font-size: 20px; line-height: 1.4; overflow-wrap: anywhere; }
.admin-page-header button { display: inline-flex; align-items: center; justify-content: center; gap: 6px; min-height: 44px; padding: 6px; border: 1px solid #ffffff66; border-radius: 8px; color: white; background: transparent; font: inherit; cursor: pointer; }
.admin-page-header button span { font-size: 26px; line-height: 1; }
.admin-page-header button:focus-visible { outline: 3px solid #ffe088; outline-offset: 2px; }
@media (max-width: 420px) { .admin-page-header { grid-template-columns: 68px minmax(0, 1fr) 68px; padding: 8px; } .admin-page-header h1 { font-size: 17px; } }
</style>
