<template>
  <div class="app-container">
    <BackButton v-if="showBackButton" />
    <div class="content">
      <router-view v-slot="{ Component, route: viewRoute }">
        <transition :name="viewRoute.name === 'Login' ? 'auth-route' : 'page-route'" mode="out-in">
          <component :is="Component" :key="viewRoute.path" />
        </transition>
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
import { computed, onMounted, nextTick, watch } from 'vue';
import { useRoute } from 'vue-router';
import request from './utils/request';
import { applyTheme, getStoredTheme } from './utils/theme';
import { getAuthRole, getToken } from './utils/auth';

const ROUTES_WITHOUT_GLOBAL_BACK = new Set([
  'Index', 'Login', 'MyInformation', 'SuccessfulPayment', 'BusinessInfo',
  'UserAddress', 'Assets', 'AiChat', 'AiRecommend', 'AiVoiceOrder',
  'AiDishRecognition', 'Favorites', 'ListDetail'
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

    // 路由切换时把应用自己的滚动容器归零，避免从长列表进入个人页时标题被“顶”到视口中间。
    watch(() => route.fullPath, () => {
      nextTick(() => {
        document.querySelector('.content')?.scrollTo({ top: 0, left: 0, behavior: 'auto' });
      });
    }, { immediate: true });

    // 先应用本地主题避免刷新闪白；登录用户再从服务端恢复跨设备偏好。
    onMounted(async () => {
      applyTheme(getStoredTheme());
      if (!getToken() || getAuthRole() !== 'user') return;
      try {
        const preference = await request.get('/api/v1/preferences/me');
        if (preference?.success && preference.data?.theme) applyTheme(preference.data.theme);
      } catch (_) {
        // 主题读取失败不应阻塞页面，保留本地主题继续使用。
      }
    });

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

    return { showFooter, showBusinessFooter, showAdminFooter, showRiderFooter, showBackButton };
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
ol {
  list-style: none;
}

a {
  text-decoration: none;
}

.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.content {
  flex: 1;
  overflow-y: auto;
}

/* 普通页面保持轻量淡入，避免路由切换出现生硬闪烁。 */
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

/* 登录页进入时稍慢一些，形成独立但克制的身份切换反馈。 */
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

/* 登录页美食背景：只在桌面端轻量展示，向右逐渐淡出，避免干扰登录表单。 */
@media (min-width: 881px) {
  .login-page {
    background: #f8fbfe !important;
  }

  .login-page::before {
    inset: 0 !important;
    width: 100% !important;
    height: 100% !important;
    border-radius: 0 !important;
    background: url('/images/login-food-bg.webp') left center / cover no-repeat !important;
    opacity: .42 !important;
    filter: saturate(.82) contrast(.94) brightness(1.04);
    -webkit-mask-image: linear-gradient(90deg, #000 0%, rgba(0, 0, 0, .96) 38%, rgba(0, 0, 0, .58) 52%, transparent 70%);
    mask-image: linear-gradient(90deg, #000 0%, rgba(0, 0, 0, .96) 38%, rgba(0, 0, 0, .58) 52%, transparent 70%);
    animation: none !important;
    pointer-events: none;
  }

  .login-page .hero-copy {
    position: relative;
    z-index: 1;
    text-shadow: 0 1px 0 rgba(255, 255, 255, .45);
  }

  .login-page .login-panel,
  .login-page .brand-header,
  .login-page .login-footer {
    position: relative;
    z-index: 2;
  }
}

@media (max-width: 880px) {
  .login-page::before {
    background: none !important;
  }
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
  .page-route-enter-active,
  .page-route-leave-active,
  .auth-route-enter-active,
  .auth-route-leave-active {
    transition: none;
  }
}
</style>
