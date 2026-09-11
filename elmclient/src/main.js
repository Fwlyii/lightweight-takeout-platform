import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { installAuthGuard } from './router/authGuard';
import 'font-awesome/css/font-awesome.min.css';
import './assets/styles/global.css';
import './assets/styles/ai-assistant.css';
import './assets/styles/ai-compact-tools.css';
import './assets/styles/ai-redesign-v3.css';
import './assets/styles/ai-final-polish.css';

installAuthGuard(router);
createApp(App).use(router).mount('#app');
