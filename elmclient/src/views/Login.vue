<template>
  <main class="login-page">
    <form class="login-card" @submit.prevent="login">
      <h1>{{ activeRole.title }}</h1>
      <fieldset :disabled="submitting">
        <legend>选择登录端</legend>
        <div class="role-options">
          <button v-for="role in roleOptions" :key="role.key" type="button"
                  :aria-pressed="selectedRole === role.key" :class="{ selected: selectedRole === role.key }"
                  @click="selectRole(role.key)">{{ role.label }}</button>
        </div>
        <label for="login-username">用户名 / 手机号</label>
        <input id="login-username" v-model.trim="username" autocomplete="username" required maxlength="100"
               placeholder="请输入用户名或手机号">
        <label for="login-password">密码</label>
        <div class="password-row">
          <input id="login-password" v-model="password" :type="showPassword ? 'text' : 'password'"
                 autocomplete="current-password" required maxlength="72" placeholder="请输入密码">
          <button type="button" @click="showPassword = !showPassword">{{ showPassword ? '隐藏' : '显示' }}</button>
        </div>
        <label class="remember"><input v-model="rememberMe" type="checkbox">记住我</label>
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <button class="login-button" type="submit">{{ submitting ? '正在登录…' : '登录' }}</button>
        <router-link v-if="selectedRole !== 'admin'" class="register-link"
                     :to="{ path: '/register', query: { role: selectedRole } }">还没有账号？立即注册</router-link>
      </fieldset>
    </form>
  </main>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '../utils/request';
import { saveAuth, clearAuth } from '../utils/auth';
import { ROLE_DEFINITIONS } from '../utils/roles';
import { createAuthenticationClient, selectLoginDestination } from '../utils/authenticationClient';

const route = useRoute();
const router = useRouter();
const selectedRole = ref('user');
const roleOptions = Object.values(ROLE_DEFINITIONS);
const activeRole = computed(() => ROLE_DEFINITIONS[selectedRole.value]);
const username = ref('');
const password = ref('');
const showPassword = ref(false);
const rememberMe = ref(false);
const submitting = ref(false);
const error = ref('');
const authentication = createAuthenticationClient(request, { saveAuth, clearAuth });

watch(() => route.query.role, role => {
  selectedRole.value = Object.hasOwn(ROLE_DEFINITIONS, role) ? role : 'user';
  error.value = '';
}, { immediate: true });

function selectRole(role) {
  if (!submitting.value) router.replace({ query: { ...route.query, role } });
}

async function login() {
  if (submitting.value) return;
  if (!username.value || !password.value) { error.value = '请输入账号和密码'; return; }
  submitting.value = true;
  error.value = '';
  try {
    const session = await authentication.login({
      username: username.value, password: password.value, role: selectedRole.value, rememberMe: rememberMe.value
    });
    password.value = '';
    const target = selectLoginDestination(session, route.query.redirect, path => router.resolve(path));
    await router.replace(target);
  } catch (failure) {
    error.value = failure.response?.data?.message
      || (failure.isAxiosError ? '网络连接失败，请稍后重试' : failure.message || '登录失败，请重试');
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.login-page { box-sizing: border-box; min-height: 100dvh; display: grid; place-items: center; padding: 24px 12px; background: #f5f7fa; color: #263442; }
.login-page * { box-sizing: border-box; }
.login-card { width: 100%; max-width: 430px; padding: 28px; border-radius: 10px; background: #fff; }
h1 { margin: 0 0 26px; font-size: 24px; text-align: center; }
fieldset { border: 0; min-width: 0; padding: 0; margin: 0; }
legend { padding: 0; margin-bottom: 10px; font-size: 14px; color: #607182; }
.role-options { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; margin-bottom: 24px; }
.role-options button { min-height: 42px; padding: 8px 2px; border: 1px solid #d9e1e8; border-radius: 6px; background: #fff; color: #526371; font: inherit; font-size: 14px; }
.role-options .selected { background: #edf7ff; color: #087bbb; border-color: #168bd2; }
label { display: block; margin: 18px 0 8px; font-size: 14px; }
input:not([type=checkbox]) { width: 100%; min-width: 0; height: 44px; padding: 10px 12px; border: 1px solid #d9e1e8; border-radius: 6px; background: #fff; color: inherit; font: inherit; font-size: 16px; }
input:focus-visible, button:focus-visible { outline: 2px solid #168bd2; outline-offset: 2px; }
.password-row { display: flex; gap: 8px; }
.password-row button { flex-shrink: 0; padding: 0 10px; background: #f1f6fa; border: 0; border-radius: 6px; color: #168bd2; }
.remember { display: flex; align-items: center; gap: 6px; color: #607182; }
.remember input { accent-color: #168bd2; width: 16px; height: 16px; }
.error { color: #c0392b; font-size: 14px; line-height: 1.6; overflow-wrap: anywhere; }
.login-button { width: 100%; min-height: 44px; margin-top: 16px; background: #168bd2; color: #fff; border: 0; border-radius: 6px; font: inherit; cursor: pointer; }
fieldset:disabled { opacity: .65; }
.register-link { display: block; margin-top: 20px; text-align: center; color: #168bd2; font-size: 14px; text-decoration: none; }
@media (max-width: 360px) { .login-card { padding: 22px 18px; } }
</style>
