import { createApp, h } from 'vue';
import { createRouter, createWebHistory, RouterView } from 'vue-router';
import { installAuthGuard } from '../router/authGuard';
import Login from '../views/Login.vue';
import Register from '../views/Register.vue';
import Session from './Session.vue';
import './preview.css';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'Login', component: Login, meta: { public: true } },
    { path: '/register', name: 'Register', component: Register, meta: { public: true } },
    { path: '/auth/session', name: 'AuthPreviewSession', component: Session },
    // No fake business pages: destinations not yet implemented resolve to the account acceptance screen.
    { path: '/:pathMatch(.*)*', redirect: '/auth/session' }
  ]
});
installAuthGuard(router);
createApp({ render: () => h(RouterView) }).use(router).mount('#app');
