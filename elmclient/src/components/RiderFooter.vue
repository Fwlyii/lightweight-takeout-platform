<template>
  <nav class="role-footer" aria-label="骑手导航">
    <router-link
      to="/rider/dashboard?tab=available"
      class="nav-item"
      :class="{ active: isActive('available') }"
    >
      <i class="fas fa-list-alt"></i>
      <span>接单广场</span>
    </router-link>
    <router-link
      to="/rider/dashboard?tab=active"
      class="nav-item"
      :class="{ active: isActive('active') }"
    >
      <i class="fas fa-route"></i>
      <span>我的订单</span>
    </router-link>
    <router-link
      to="/myInformation?role=rider"
      class="nav-item"
      :class="{ active: route.path === '/myInformation' }"
    >
      <i class="fas fa-user"></i>
      <span>我的</span>
    </router-link>
  </nav>
</template>

<script setup>
import { useRoute } from 'vue-router';

const route = useRoute();
const isActive = (tab) => {
  if (route.path !== '/rider/dashboard') return false;
  const currentTab = route.query.tab || 'available';
  return tab === 'active' ? ['active', 'history'].includes(currentTab) : currentTab === tab;
};
</script>

<style scoped>
.role-footer {
  position: fixed;
  z-index: 1200;
  left: 0;
  right: 0;
  bottom: 0;
  height: calc(82px + env(safe-area-inset-bottom));
  padding-bottom: env(safe-area-inset-bottom);
  display: flex;
  align-items: stretch;
  justify-content: center;
  background: #fff;
  border-top: 1px solid color-mix(in srgb, var(--skin-brand, #0097ff) 11%, white);
  border-radius: 18px 18px 0 0;
  box-shadow: 0 -3px 12px rgba(var(--skin-brand-strong-rgb, 31, 75, 122), 0.06);
}

.nav-item {
  width: min(33.333%, 220px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  position: relative;
  color: var(--skin-muted, #8393a7);
  text-decoration: none;
  font-size: 16px;
  transition: color .18s ease;
}

.nav-item i { font-size: 25px; }
.nav-item.active { color: var(--skin-brand, #0095ff); font-weight: 700; }
.nav-item.active:after { content: ""; position: absolute; bottom: 6px; width: 38px; height: 4px; border-radius: 4px; background: var(--skin-brand, #0095ff); }
.nav-item:hover { color: var(--skin-brand, #0095ff); }
@media(max-width:600px){.role-footer{height:calc(72px + env(safe-area-inset-bottom))}.nav-item{font-size:13px}.nav-item i{font-size:22px}.nav-item.active:after{bottom:4px;width:30px;height:3px}}
</style>
