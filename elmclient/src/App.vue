<template>
  <div class="app-container" :class="{ 'admin-area': showAdminFooter, 'auth-area': isAuthPage }">
    <div
      v-if="routeMotion.active"
      class="route-wipe"
      :class="{ settling: routeMotion.settling }"
      :style="routeWipeStyle"
      aria-hidden="true"
    ></div>
    <BackButton v-if="showBackButton" />
    <div class="content">
      <router-view v-slot="{ Component, route: viewRoute }">
        <!-- 路由页面可包含弹窗等多个根节点，不能直接放入 out-in Transition。
             保留页面自身的入场动效，切页不等待不可靠的离场回调。 -->
        <component :is="Component" :key="viewRoute.path === '/rider/dashboard' ? viewRoute.path : viewRoute.fullPath" />
      </router-view>
    </div>
    <Footer v-if="showFooter" />
    <BusinessFooter v-if="showBusinessFooter" />
    <AdminFooter v-if="showAdminFooter" />
    <RiderFooter v-if="showRiderFooter" />
  </div>
</template>

<script>
import BackButton from './components/BackButton.vue';
import Footer from './components/Footer.vue';
import BusinessFooter from './components/BusinessFooter.vue';
import AdminFooter from './components/AdminFooter.vue';
import RiderFooter from './components/RiderFooter.vue';
import { computed, nextTick, watch } from 'vue';
import { useRoute } from 'vue-router';
import request from './utils/request';
import { applyTheme, getStoredTheme, themeForRoute } from './utils/theme';
import { getAuthRole, getToken } from './utils/auth';
import { routeMotion } from './utils/routeMotion';

const ROUTES_WITHOUT_GLOBAL_BACK = new Set([
  'Index', 'Login', 'MyInformation', 'SuccessfulPayment', 'BusinessInfo',
  'UserAddress', 'Assets', 'Preferences', 'Payment', 'Cart', 'AiChat', 'AiRecommend', 'AiVoiceOrder',
  'AiDishRecognition', 'Favorites', 'ListDetail', 'Notifications', 'Search', 'OrderList',
  'AddUserAddress', 'EditUserAddress', 'Register'
]);

const ROUTES_WITHOUT_CUSTOMER_FOOTER = new Set([
  'BusinessInfo', 'Payment', 'SuccessfulPayment', 'Cart', 'Favorites',
  'Notifications', 'UserAddress', 'AddUserAddress', 'ListDetail',
  'Register', 'Login', 'EditUserAddress', 'AiChat', 'AiRecommend',
  'AiVoiceOrder', 'AiDishRecognition'
]);

export default {
  components: {
    BackButton,
    Footer,
    BusinessFooter,
    AdminFooter,
    RiderFooter,
  },
  setup() {
    const route = useRoute();
    const isAuthPage = computed(() => ['Login', 'Register'].includes(route.name));

    watch(() => route.fullPath, () => {
      applyTheme(themeForRoute(route.path, getStoredTheme()), { persist: false });
      nextTick(() => {
        document.querySelector('.content')?.scrollTo({ top: 0, left: 0, behavior: 'auto' });
      });
    }, { immediate: true });

    let preferenceSession = null;
    watch(() => route.fullPath, async () => {
      const token = getToken();
      if (isAuthPage.value || !token || getAuthRole() !== 'user') { preferenceSession = null; return; }
      if (token === preferenceSession) return;
      preferenceSession = token;
      try {
        const preference = await request.get('/api/v1/preferences/me');
        if (getToken() === token && !isAuthPage.value && preference?.success && preference.data?.theme) applyTheme(preference.data.theme);
      } catch (_) {
        // 主题读取失败不阻塞页面，继续使用本地主题。
      }
    }, { immediate: true });

    const showBackButton = computed(() => {
      if (route.path.startsWith('/merchant') || route.path.startsWith('/admin') || route.path.startsWith('/rider')) {
        return false;
      }
      return !ROUTES_WITHOUT_GLOBAL_BACK.has(route.name);
    });

    const showFooter = computed(() => {
      const isMerchantApplication = route.name === 'MerchantApply';
      if ((route.path.startsWith('/merchant') && !isMerchantApplication) || route.path.startsWith('/admin') || isRiderArea.value) {
        return false;
      }
      return !ROUTES_WITHOUT_CUSTOMER_FOOTER.has(route.name);
    });

    const showBusinessFooter = computed(() => {
      if (['MerchantBusinessInfo', 'MerchantApply'].includes(route.name)) {
        return false;
      }
      return route.path.startsWith('/merchant');
    });

    const isRiderArea = computed(() => {
      return route.path.startsWith('/rider') ||
        (route.query.role === 'rider' && ['/myInformation', '/notifications'].includes(route.path));
    });

    const isRiderContext = computed(() => {
      return route.path === '/rider/dashboard' ||
        (route.query.role === 'rider' && ['/myInformation', '/notifications'].includes(route.path));
    });

    const showRiderFooter = computed(() => isRiderContext.value);
    const showAdminFooter = computed(() => route.path.startsWith('/admin'));

    const routeWipeStyle = computed(() => ({
      '--route-wipe-x': `${routeMotion.x}px`,
      '--route-wipe-y': `${routeMotion.y}px`
    }));

    return {
      isAuthPage,
      showFooter,
      showBusinessFooter,
      showAdminFooter,
      showRiderFooter,
      showBackButton,
      routeMotion,
      routeWipeStyle
    };
  },
};
</script>

<style>
html,
body,
div,
span,
h1,
h2,
h3,
h4,
h5,
h6,
ul,
ol,
li,
p {
  margin: 0;
  padding: 0;
}

html,
body,
#app {
  width: 100%;
  height: 100%;
  font-family: "微软雅黑";
}

html,
body {
  margin: 0;
  padding: 0;
  height: 100%;
}

ul,
ol { list-style: none; }
a { text-decoration: none; }

.app-container {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
}

.app-container > .content {
  flex: 1;
  min-height: 0;
  min-width: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.route-wipe {
  position: fixed;
  inset: 0;
  z-index: 10000;
  pointer-events: none;
  opacity: 1;
  background: var(--tertiary-color, #0097ff);
  clip-path: circle(0 at var(--route-wipe-x) var(--route-wipe-y));
  animation: route-wipe-expand .5s cubic-bezier(.22, .75, .2, 1) forwards;
}

.route-wipe.settling {
  animation: route-wipe-expand .5s cubic-bezier(.22, .75, .2, 1) forwards,
    route-wipe-fade .3s ease .02s forwards;
}

@keyframes route-wipe-expand {
  to { clip-path: circle(150vmax at var(--route-wipe-x) var(--route-wipe-y)); }
}

@keyframes route-wipe-fade {
  to { opacity: 0; }
}

.page-route-enter-active,
.page-route-leave-active {
  transition: opacity .16s ease, transform .16s ease;
}

.page-route-enter-from {
  opacity: 0;
  transform: translateY(4px);
}

.page-route-leave-to {
  opacity: 0;
  transform: translateY(-2px);
}

.auth-route-enter-active {
  transition: opacity .24s ease, transform .24s cubic-bezier(.22, .61, .36, 1);
}

.auth-route-leave-active {
  transition: opacity .14s ease, transform .14s ease;
}

.auth-route-enter-from {
  opacity: 0;
  transform: translateY(12px) scale(.992);
}

.auth-route-leave-to {
  opacity: 0;
  transform: scale(.995);
}

@media (max-width: 680px) {
  .auth-route-enter-from {
    opacity: 0;
    transform: translateX(22px);
  }

  .auth-route-leave-to {
    opacity: 0;
    transform: translateX(-10px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .route-wipe {
    animation: none;
    clip-path: none;
    opacity: 0;
  }

  .page-route-enter-active,
  .page-route-leave-active,
  .auth-route-enter-active,
  .auth-route-leave-active {
    transition: none;
  }
}
</style>
