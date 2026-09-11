<template>
  <main class="login-page">
    <div class="ambient ambient-one" aria-hidden="true"></div>
    <div class="ambient ambient-two" aria-hidden="true"></div>

    <section class="login-wrap" aria-label="账号登录">
      <header class="brand-bar">
        <div class="brand-mark" aria-hidden="true">
          <i class="fa fa-cutlery"></i>
        </div>
        <div class="brand-copy">
          <strong>饿了么</strong>
          <span>校园外卖服务平台</span>
        </div>
      </header>

      <form class="login-card" @submit.prevent="login">
        <div class="heading-row">
          <div>
            <p class="eyebrow">WELCOME BACK</p>
            <h1>{{ activeRole.title }}</h1>
            <p class="subtitle">{{ activeRole.subtitle }}</p>
          </div>
          <div class="role-badge" aria-hidden="true">
            <i :class="roleIcon(selectedRole)"></i>
          </div>
        </div>

        <div class="role-switcher" role="tablist" aria-label="登录身份">
          <button
            v-for="item in roleOptions"
            :key="item.key"
            type="button"
            class="role-item"
            role="tab"
            :aria-selected="selectedRole === item.key"
            :class="{ selected: selectedRole === item.key }"
            :disabled="submitting"
            @click="selectRole(item.key)"
          >
            <i :class="roleIcon(item.key)"></i>
            <span>{{ item.label }}</span>
          </button>
        </div>

        <p v-if="hasRedirect" class="context-hint">
          <i class="fa fa-arrow-circle-right"></i>
          登录后将返回刚才的页面
        </p>

        <div class="form-body">
          <label class="field" for="login-username">
            <span class="field-label">用户名 / 手机号</span>
            <div class="input-wrap">
              <i class="fa fa-user input-icon"></i>
              <input
                id="login-username"
                v-model.trim="userName"
                type="text"
                autocomplete="username"
                maxlength="100"
                placeholder="请输入用户名或手机号"
              />
            </div>
          </label>

          <label class="field" for="login-password">
            <span class="field-label">密码</span>
            <div class="input-wrap">
              <i class="fa fa-lock input-icon"></i>
              <input
                id="login-password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="current-password"
                maxlength="72"
                placeholder="请输入密码"
              />
              <button
                class="password-toggle"
                type="button"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                <i :class="showPassword ? 'fa fa-eye-slash' : 'fa fa-eye'"></i>
              </button>
            </div>
          </label>

          <div class="form-options">
            <label class="remember-option">
              <input v-model="rememberMe" type="checkbox" />
              <span>记住我</span>
            </label>
            <span class="role-tip">当前：{{ activeRole.label }}端</span>
          </div>

          <p v-if="inlineError" class="inline-error" role="alert">
            <i class="fa fa-exclamation-circle"></i>
            <span>{{ inlineError }}</span>
          </p>

          <button
            class="login-button"
            :class="{ success: loginSucceeded }"
            type="submit"
            :disabled="submitting || loginSucceeded"
          >
            <i v-if="submitting" class="fa fa-spinner fa-spin"></i>
            <i v-else-if="loginSucceeded" class="fa fa-check"></i>
            <span>{{ loginButtonText }}</span>
          </button>
        </div>

        <div class="card-footer">
          <p v-if="selectedRole === 'user'" class="register-hint">
            还没有账号？
            <router-link to="/register">立即注册</router-link>
          </p>
          <p v-else class="register-hint muted">请使用对应身份账号登录</p>

          <p class="security-note">
            <i class="fa fa-lock"></i>
            登录信息仅用于身份验证
          </p>
        </div>
      </form>
    </section>
  </main>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '../utils/request';
import { toast } from '../utils/toast';
import { ROLE_DEFINITIONS, roleCanEnter } from '../utils/roles';
import { clearAuth, saveAuth, updateStoredUser } from '../utils/auth';

const router = useRouter();
const route = useRoute();
const roleMap = ROLE_DEFINITIONS;
const roleOptions = Object.values(roleMap);
const queryRole = typeof route.query.role === 'string' && roleMap[route.query.role] ? route.query.role : 'user';
const selectedRole = ref(queryRole);
const userName = ref(localStorage.getItem('savedUserName') || '');
const password = ref('');
const showPassword = ref(false);
const rememberMe = ref(false);
const submitting = ref(false);
const loginSucceeded = ref(false);
const inlineError = ref('');
const activeRole = computed(() => roleMap[selectedRole.value]);
const hasRedirect = computed(() => {
  const redirect = route.query.redirect;
  return typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//');
});
const loginButtonText = computed(() => {
  if (loginSucceeded.value) return '登录成功';
  if (submitting.value) return '正在登录…';
  return activeRole.value.button;
});

watch(() => route.query.role, value => {
  if (typeof value === 'string' && roleMap[value]) selectedRole.value = value;
  inlineError.value = '';
});

const roleIcon = (key) => ({
  user: 'fa fa-user',
  merchant: 'fa fa-shopping-bag',
  rider: 'fa fa-motorcycle',
  admin: 'fa fa-shield'
}[key] || 'fa fa-user');

const selectRole = (key) => {
  if (submitting.value) return;
  selectedRole.value = key;
  inlineError.value = '';
  router.replace({ query: { ...route.query, role: key } });
};

const waitForSuccessFeedback = () => new Promise(resolve => setTimeout(resolve, 220));

const login = async () => {
  if (!userName.value) {
    inlineError.value = '请输入用户名或手机号';
    return;
  }
  if (!password.value) {
    inlineError.value = '请输入密码';
    return;
  }

  submitting.value = true;
  inlineError.value = '';
  try {
    const auth = await request.post('/api/auth', {
      username: userName.value,
      password: password.value,
      rememberMe: rememberMe.value,
      role: selectedRole.value
    });
    if (!auth?.id_token) throw new Error(auth?.message || '登录失败');

    saveAuth(auth.id_token, null, rememberMe.value, auth.role || selectedRole.value);
    const userRes = await request.get('/api/user');
    updateStoredUser(userRes);
    if (rememberMe.value) localStorage.setItem('savedUserName', userName.value);
    else localStorage.removeItem('savedUserName');

    if (auth.application_only && activeRole.value.applyTarget) {
      loginSucceeded.value = true;
      toast.info(`请先完成${activeRole.value.label}入驻申请`);
      await waitForSuccessFeedback();
      router.push(activeRole.value.applyTarget);
      return;
    }

    if (!roleCanEnter(userRes, selectedRole.value)) {
      clearAuth();
      throw new Error('账号权限刚刚发生变化，请重新登录');
    }

    loginSucceeded.value = true;
    toast.success(`已进入${activeRole.value.label}端`);
    await waitForSuccessFeedback();

    const redirect = typeof route.query.redirect === 'string'
      && route.query.redirect.startsWith('/')
      && !route.query.redirect.startsWith('//')
      ? route.query.redirect
      : null;
    router.push(selectedRole.value === 'user' && redirect ? redirect : activeRole.value.target);
  } catch (error) {
    clearAuth();
    loginSucceeded.value = false;
    inlineError.value = error.response?.data?.message
      || error.message
      || '用户名或密码错误';
    toast.error(inlineError.value);
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
* { box-sizing: border-box; }

.login-page {
  position: relative;
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: 40px 20px;
  overflow: hidden;
  color: #173b60;
  background:
    linear-gradient(180deg, #f8fbfe 0%, #f4f9fd 100%);
}

.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(31, 110, 171, .035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(31, 110, 171, .035) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: linear-gradient(to bottom, rgba(0,0,0,.5), transparent 78%);
}

.ambient {
  position: absolute;
  border-radius: 50%;
  filter: blur(2px);
  pointer-events: none;
}

.ambient-one {
  width: 420px;
  height: 420px;
  top: -210px;
  right: -90px;
  background: rgba(0, 151, 255, .08);
}

.ambient-two {
  width: 320px;
  height: 320px;
  bottom: -190px;
  left: -110px;
  background: rgba(69, 184, 255, .08);
}

.login-wrap {
  position: relative;
  z-index: 1;
  width: min(470px, 100%);
}

.brand-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 22px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  color: #fff;
  background: #0097ff;
  box-shadow: 0 10px 25px rgba(0, 151, 255, .22);
  font-size: 17px;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-copy strong {
  color: #173b60;
  font-size: 18px;
  line-height: 1.2;
  letter-spacing: .02em;
}

.brand-copy span {
  color: #7d96ac;
  font-size: 11px;
  letter-spacing: .02em;
}

.login-card {
  width: 100%;
  padding: 34px 36px 28px;
  border: 1px solid rgba(211, 228, 241, .95);
  border-radius: 24px;
  background: rgba(255, 255, 255, .94);
  box-shadow:
    0 28px 70px rgba(48, 88, 122, .10),
    0 3px 12px rgba(48, 88, 122, .04);
  backdrop-filter: blur(14px);
}

.heading-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.eyebrow {
  margin: 0 0 7px;
  color: #5b91bc;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: .18em;
}

.heading-row h1 {
  margin: 0;
  color: #173b60;
  font-size: 30px;
  line-height: 1.2;
  letter-spacing: -.03em;
}

.subtitle {
  margin: 7px 0 0;
  color: #8097aa;
  font-size: 13px;
}

.role-badge {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  flex: 0 0 46px;
  border-radius: 14px;
  color: #0097ff;
  background: #eef8ff;
  border: 1px solid #dceffc;
  font-size: 17px;
}

.role-switcher {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 5px;
  margin-top: 26px;
  padding: 5px;
  border: 1px solid #e0ebf4;
  border-radius: 14px;
  background: #f5f9fc;
}

.role-item {
  min-width: 0;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 8px;
  border: 0;
  border-radius: 10px;
  color: #718ba0;
  background: transparent;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: color .18s ease, background .18s ease, box-shadow .18s ease, transform .18s ease;
}

.role-item i { font-size: 12px; }

.role-item:hover:not(:disabled) {
  color: #2879b7;
  background: rgba(255, 255, 255, .7);
}

.role-item.selected {
  color: #008be9;
  background: #fff;
  box-shadow: 0 3px 12px rgba(42, 96, 139, .10);
}

.role-item:active:not(:disabled) { transform: scale(.98); }
.role-item:disabled { cursor: wait; opacity: .6; }

.context-hint {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 15px 1px 0;
  color: #5f87a7;
  font-size: 11px;
}

.context-hint i { color: #25a4f5; }

.form-body {
  margin-top: 24px;
}

.field {
  display: block;
  margin-bottom: 18px;
}

.field-label {
  display: block;
  margin-bottom: 8px;
  color: #45637d;
  font-size: 12px;
  font-weight: 600;
}

.input-wrap {
  height: 52px;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 15px;
  border: 1px solid #d8e5ef;
  border-radius: 13px;
  background: #fbfdff;
  transition: border-color .18s ease, box-shadow .18s ease, background .18s ease;
}

.input-wrap:focus-within {
  border-color: #65bcf3;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(0, 151, 255, .075);
}

.input-icon {
  width: 14px;
  color: #9ab0c2;
  font-size: 13px;
  text-align: center;
}

.input-wrap input {
  min-width: 0;
  flex: 1;
  border: 0;
  outline: 0;
  color: #244762;
  background: transparent;
  font: inherit;
  font-size: 14px;
}

.input-wrap input::placeholder { color: #b1bfca; }

.password-toggle {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  flex: 0 0 32px;
  border: 0;
  border-radius: 9px;
  color: #8199ab;
  background: transparent;
  cursor: pointer;
  transition: color .18s ease, background .18s ease;
}

.password-toggle:hover {
  color: #178fdc;
  background: #eff8ff;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: -1px 1px 20px;
  color: #7f95a7;
  font-size: 11px;
}

.remember-option {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
}

.remember-option input {
  width: 15px;
  height: 15px;
  margin: 0;
  accent-color: #0097ff;
}

.role-tip { white-space: nowrap; }

.inline-error {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: -4px 0 14px;
  padding: 10px 12px;
  border: 1px solid #ffd8d4;
  border-radius: 10px;
  color: #b64b43;
  background: #fff7f6;
  font-size: 11px;
  line-height: 1.5;
}

.inline-error i { margin-top: 2px; }

.login-button {
  width: 100%;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-radius: 13px;
  color: #fff;
  background: #0097ff;
  box-shadow: 0 10px 24px rgba(0, 151, 255, .22);
  font: inherit;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: transform .18s ease, box-shadow .18s ease, background .18s ease;
}

.login-button:hover:not(:disabled) {
  transform: translateY(-1px);
  background: #008be9;
  box-shadow: 0 13px 28px rgba(0, 151, 255, .26);
}

.login-button:active:not(:disabled) { transform: translateY(0); }
.login-button:disabled { cursor: wait; opacity: .72; }
.login-button.success { background: #2fa56d; box-shadow: 0 10px 24px rgba(47, 165, 109, .18); }

.card-footer {
  padding-top: 20px;
  text-align: center;
}

.register-hint {
  margin: 0;
  color: #8297a8;
  font-size: 12px;
}

.register-hint a {
  margin-left: 4px;
  color: #008be9;
  font-weight: 600;
  text-decoration: none;
}

.register-hint a:hover { text-decoration: underline; }
.register-hint.muted { color: #a0afbb; }

.security-note {
  margin: 13px 0 0;
  color: #a6b4bf;
  font-size: 10px;
}

.security-note i { margin-right: 5px; color: #83bde5; }

button:focus-visible,
input:focus-visible,
a:focus-visible {
  outline: 2px solid #4fb5f6;
  outline-offset: 2px;
}

@media (max-width: 560px) {
  .login-page {
    display: block;
    padding: 22px 14px 30px;
    overflow-y: auto;
  }

  .login-wrap {
    width: 100%;
    margin: 0 auto;
  }

  .brand-bar {
    justify-content: flex-start;
    margin: 4px 8px 18px;
  }

  .login-card {
    padding: 28px 22px 24px;
    border-radius: 20px;
  }

  .heading-row h1 { font-size: 27px; }
  .role-badge { width: 42px; height: 42px; flex-basis: 42px; }

  .role-switcher {
    gap: 3px;
    padding: 4px;
  }

  .role-item {
    height: 42px;
    padding: 0 4px;
    font-size: 11px;
  }

  .role-item i { display: none; }

  .input-wrap,
  .login-button { height: 50px; }
}

@media (max-width: 360px) {
  .login-card { padding: 26px 18px 22px; }
  .role-tip { display: none; }
}

@media (prefers-reduced-motion: reduce) {
  .role-item,
  .input-wrap,
  .password-toggle,
  .login-button {
    transition: none !important;
  }
}
</style>
