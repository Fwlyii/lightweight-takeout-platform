<template>
    <div class="bottom-nav">
      <router-link
        to="/admin/home"
        class="nav-item"
        :class="{ active: $route.path === '/admin/home' }"
      >
        <i class="fas fa-home"></i>
        <span>首页</span>
      </router-link>
      <router-link
        to="/admin/user"
        class="nav-item"
        :class="{ active: $route.path === '/admin/user' }"
      >
        <i class="fas fa-user-friends"></i>
        <span>用户管理</span>
      </router-link>
      <router-link
        to="/admin/business"
        class="nav-item"
        :class="{ active: $route.path === '/admin/business' }"
      >
        <i class="fas fa-store"></i>
        <span>商铺管理</span>
      </router-link>
      <router-link
        to="/admin/riders"
        class="nav-item"
        :class="{ active: $route.path === '/admin/riders' }"
      >
        <i class="fas fa-motorcycle"></i>
        <span>配送调度</span>
      </router-link>
    </div>
  </template>

  <script setup>
  import { useRoute } from 'vue-router';

  const $route = useRoute();
  </script>

  <style scoped>
  .bottom-nav {
    position: fixed;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    max-width: 960px;
    width: 100%;
    height: calc(60px + env(safe-area-inset-bottom));
    padding-bottom: env(safe-area-inset-bottom);
    background: white;
    display: flex;
    justify-content: space-around;
    align-items: center;
    box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
    z-index: 100;
  }

  .nav-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #666;
    text-decoration: none;
    font-size: 0.8rem;
    gap: 4px;
    flex: 1;
    height: 100%;
    transition: color 180ms ease;
  }

  .nav-item i {
    font-size: 1.2rem;
    transform-origin: center;
    transition: color 180ms ease;
  }

  .nav-item span {
    opacity: .78;
    transform: translateY(2px);
    transition: opacity 180ms ease, transform 180ms ease;
  }

  .nav-item.active {
    color: var(--admin-nav-active, #2196F3);
  }

  .nav-item.active i {
    animation: admin-nav-icon-pop 320ms cubic-bezier(.22, .61, .36, 1) both;
  }

  .nav-item.active span {
    opacity: 1;
    transform: translateY(0);
    animation: admin-nav-label-in 240ms ease-out both;
  }

  @keyframes admin-nav-icon-pop {
    0%, 100% { transform: scale(1); }
    50% { transform: scale(1.15); }
  }

  @keyframes admin-nav-label-in {
    from { opacity: .35; transform: translateY(3px); }
    to { opacity: 1; transform: translateY(0); }
  }

  @media (prefers-reduced-motion: reduce) {
    .nav-item,
    .nav-item i,
    .nav-item span {
      transition: none;
      animation: none;
      transform: none;
    }

    .nav-item span,
    .nav-item.active span {
      opacity: 1;
    }
  }
  </style>
