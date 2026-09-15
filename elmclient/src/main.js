import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { installAuthGuard } from './router/authGuard';
import { applyTheme, getStoredTheme, themeForRoute } from './utils/theme';
import 'font-awesome/css/font-awesome.min.css';
import './assets/styles/global.css';
import './assets/styles/ai-assistant.css';
import './assets/styles/layout-alignment.css';
import './assets/styles/interaction-motion.css';
import './assets/styles/admin-motion.css';
import './assets/styles/fwl-alignment.css';

// 在首屏挂载前恢复皮肤，避免刷新时先闪现默认蓝色。
applyTheme(themeForRoute(window.location.pathname, getStoredTheme()), { persist: false });
installAuthGuard(router);
createApp(App).use(router).mount('#app');
