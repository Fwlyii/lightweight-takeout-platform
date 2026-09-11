<template>
  <main class="login-page">
    <section class="login-shell" aria-label="账号登录">
      <aside class="login-intro" aria-hidden="true">
        <div class="intro-orb intro-orb-one"></div>
        <div class="intro-orb intro-orb-two"></div>

        <div class="brand-row">
          <div class="intro-logo"><i class="fa fa-cutlery"></i></div>
          <div>
            <p class="brand-name">饿了么</p>
            <p class="brand-note">轻松点餐 · 高效配送</p>
          </div>
        </div>

        <div class="intro-copy">
          <p class="eyebrow">WELCOME BACK</p>
          <h1>今天想吃点<br><span>什么？</span></h1>
          <p class="intro-description">从发现喜欢的餐食，到完成配送，一次登录即可开始。</p>
        </div>

        <div class="delivery-visual">
          <div class="dish-card">
            <div class="dish-icon"><i class="fa fa-shopping-bag"></i></div>
            <div>
              <strong>今日好味</strong>
              <span>新鲜 · 便捷 · 快速</span>
            </div>
            <span class="dish-badge">推荐</span>
          </div>
          <div class="delivery-route">
            <span class="route-node active"><i class="fa fa-cutlery"></i></span>
            <span class="route-line"></span>
            <span class="route-node"><i class="fa fa-check"></i></span>
            <span class="route-line"></span>
            <span class="route-node active"><i class="fa fa-motorcycle"></i></span>
          </div>
          <div class="delivery-status">
            <span><i class="fa fa-clock-o"></i> 下单后快速送达</span>
            <i class="fa fa-arrow-right"></i>
          </div>
        </div>
      </aside>

      <form class="login-card" @submit.prevent="login">
        <div class="card-heading">
          <p class="card-kicker">账号登录</p>
          <h2>{{ activeRole.title }}</h2>
          <p>{{ activeRole.subtitle }}</p>
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

        <label class="field" for="login-username">
          <span>用户名 / 手机号</span>
          <div class="input-wrap">
            <i class="fa fa-user"></i>
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
          <span>密码</span>
          <div class="input-wrap">
            <i class="fa fa-lock"></i>
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
          <span class="continue-hint">{{ continueHint }}</span>
        </div>

        <p v-if="inlineError" class="inline-error" role="alert">
          <i class="fa fa-exclamation-circle"></i>
          {{ inlineError }}
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

        <p v-if="selectedRole === 'user'" class="register-hint">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </p>

        <div class="security-note">
          <i class="fa fa-lock"></i>
          登录信息仅用于身份验证
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
const continueHint = computed(() => {
  const redirect = route.query.redirect;
  return typeof redirect === 'string' && redirect.startsWith('/')
    ? '登录后继续刚才的操作'
    : '选择身份后进入对应工作台';
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

const waitForSuccessFeedback = () => new Promise(resolve => setTimeout(resolve, 260));

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

    // 登录身份切换时先清掉上一个账号的认证信息，避免路由守卫读到旧的 localStorage。
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
  min-height: 100dvh;
  padding: 28px 24px;
  display: grid;
  place-items: center;
  overflow: hidden;
  color: #24405c;
  background:
    radial-gradient(circle at 8% 12%, rgba(0, 151, 255, .07), transparent 24%),
    radial-gradient(circle at 92% 88%, rgba(0, 151, 255, .05), transparent 26%),
    #f5f9fd;
}

.login-shell {
  width: min(1080px, 100%);
  min-height: 640px;
  display: grid;
  grid-template-columns: 36% 64%;
  overflow: hidden;
  border: 1px solid #deebf6;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 24px 70px rgba(36, 90, 137, .12);
}

.login-intro {
  position: relative;
  padding: 54px 44px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid #dcecf9;
  background: linear-gradient(155deg, #eaf5ff 0%, #e4f2ff 100%);
}

.intro-orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  background: rgba(255, 255, 255, .5);
}

.intro-orb-one { width: 190px; height: 190px; top: -74px; right: -74px; }
.intro-orb-two { width: 110px; height: 110px; left: -46px; bottom: 74px; }

.brand-row {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 14px;
}

.intro-logo {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  flex: 0 0 52px;
  border-radius: 15px;
  color: #fff;
  background: #0097ff;
  box-shadow: 0 10px 22px rgba(0, 151, 255, .22);
  font-size: 21px;
}

.brand-name { color: #173b60; font-size: 18px; font-weight: 700; letter-spacing: .03em; }
.brand-note { margin-top: 4px; color: #6f8ca7; font-size: 12px; }

.intro-copy {
  position: relative;
  z-index: 1;
  margin-top: 72px;
}

.eyebrow {
  margin: 0 0 13px;
  color: #4f8ac0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .16em;
}

.intro-copy h1 {
  margin: 0;
  color: #173b60;
  font-size: clamp(38px, 4vw, 48px);
  line-height: 1.12;
  letter-spacing: -.04em;
}

.intro-copy h1 span { color: #0097ff; }

.intro-description {
  max-width: 280px;
  margin-top: 20px;
  color: #69839d;
  font-size: 14px;
  line-height: 1.85;
}

.delivery-visual {
  position: relative;
  z-index: 1;
  margin-top: auto;
  padding-top: 42px;
}

.dish-card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 15px;
  border: 1px solid rgba(255, 255, 255, .82);
  border-radius: 16px;
  background: rgba(255, 255, 255, .76);
  box-shadow: 0 14px 32px rgba(50, 112, 164, .10);
  backdrop-filter: blur(8px);
}

.dish-icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  color: #0097ff;
  background: #e6f5ff;
}

.dish-card strong { display: block; color: #274c6f; font-size: 13px; }
.dish-card div span { display: block; margin-top: 4px; color: #8399ad; font-size: 10px; }
.dish-badge { padding: 5px 8px; border-radius: 999px; color: #0089e9; background: #e8f6ff; font-size: 10px; }

.delivery-route {
  display: flex;
  align-items: center;
  margin: 18px 10px;
}

.route-node {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  flex: 0 0 28px;
  border-radius: 50%;
  color: #6f93b2;
  background: #fff;
  border: 1px solid #d2e6f5;
  font-size: 10px;
}

.route-node.active { color: #fff; background: #0097ff; border-color: #0097ff; }
.route-line { height: 1px; flex: 1; background: linear-gradient(90deg, #79c9ff, #b9ddf6); }

.delivery-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #6786a1;
  font-size: 11px;
}

.delivery-status span { display: flex; align-items: center; gap: 7px; }
.delivery-status > i { color: #64afe2; }

.login-card {
  width: min(100%, 620px);
  margin: auto;
  padding: 62px 74px;
}

.card-kicker {
  margin: 0 0 8px;
  color: #5d8db5;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: .08em;
}

.card-heading h2 {
  margin: 0;
  color: #183b5e;
  font-size: 32px;
  line-height: 1.25;
  letter-spacing: -.02em;
}

.card-heading > p:last-child {
  margin: 9px 0 0;
  color: #8096ab;
  font-size: 13px;
}

.role-switcher {
  margin: 30px 0 28px;
  padding: 5px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 5px;
  border: 1px solid #e1edf7;
  border-radius: 14px;
  background: #f4f9fd;
}

.role-item {
  height: 54px;
  padding: 0 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 0;
  border-radius: 10px;
  color: #7890a5;
  background: transparent;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  transition: color .18s ease, background .18s ease, box-shadow .18s ease, transform .18s ease;
}

.role-item i { font-size: 14px; }
.role-item:hover:not(:disabled) { color: #2d6b9c; }
.role-item.selected {
  color: #007fd8;
  background: #fff;
  box-shadow: 0 5px 16px rgba(60, 119, 166, .10);
  transform: translateY(-1px);
}
.role-item:disabled { cursor: wait; opacity: .65; }

.field {
  display: block;
  margin-bottom: 19px;
}

.field > span {
  display: block;
  margin-bottom: 8px;
  color: #526f8b;
  font-size: 12px;
  font-weight: 600;
}

.input-wrap {
  height: 52px;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 15px;
  border: 1px solid #d9e7f2;
  border-radius: 12px;
  background: #fbfdff;
  transition: border-color .18s ease, box-shadow .18s ease, background .18s ease;
}

.input-wrap:focus-within {
  border-color: #62baf2;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(0, 151, 255, .08);
}

.input-wrap > i {
  width: 15px;
  color: #9aafc1;
  font-size: 14px;
  text-align: center;
}

.input-wrap input {
  min-width: 0;
  flex: 1;
  border: 0;
  outline: 0;
  color: #29455f;
  background: transparent;
  font: inherit;
  font-size: 14px;
}

.input-wrap input::placeholder { color: #a8b8c6; }

.password-toggle {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 8px;
  color: #7892a9;
  background: transparent;
  cursor: pointer;
}
.password-toggle:hover { color: #168bd2; background: #edf7fe; }

.form-options {
  min-height: 24px;
  margin: 0 0 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  color: #8297aa;
  font-size: 11px;
}

.remember-option {
  display: flex;
  align-items: center;
  gap: 7px;
  white-space: nowrap;
  cursor: pointer;
}
.remember-option input { width: 16px; height: 16px; accent-color: #0097ff; }
.continue-hint { text-align: right; }

.inline-error {
  margin: -6px 0 14px;
  display: flex;
  align-items: center;
  gap: 7px;
  color: #c94a49;
  font-size: 12px;
  line-height: 1.5;
}

.login-button {
  width: 100%;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-radius: 12px;
  color: #fff;
  background: #0097ff;
  box-shadow: 0 9px 22px rgba(0, 151, 255, .22);
  cursor: pointer;
  font: inherit;
  font-size: 15px;
  font-weight: 600;
  transition: transform .18s ease, box-shadow .18s ease, background .18s ease;
}

.login-button:hover:not(:disabled) {
  transform: translateY(-1px);
  background: #008ae9;
  box-shadow: 0 12px 26px rgba(0, 151, 255, .27);
}
.login-button:active:not(:disabled) { transform: translateY(0); }
.login-button:disabled { cursor: wait; opacity: .76; }
.login-button.success { background: #178bd4; opacity: 1; }

.register-hint {
  margin: 18px 0 0;
  color: #8297aa;
  text-align: center;
  font-size: 12px;
}
.register-hint a { color: #008be7; font-weight: 600; }

.security-note {
  margin-top: 24px;
  color: #a2b1bf;
  text-align: center;
  font-size: 10px;
}
.security-note i { margin-right: 5px; color: #84b7da; }

button:focus-visible,
input:focus-visible,
a:focus-visible {
  outline: 2px solid #168bd2;
  outline-offset: 2px;
}

@media (max-width: 860px) {
  .login-page { padding: 18px; }
  .login-shell { grid-template-columns: 34% 66%; min-height: 600px; }
  .login-intro { padding: 42px 30px; }
  .intro-copy { margin-top: 58px; }
  .intro-copy h1 { font-size: 36px; }
  .dish-badge { display: none; }
  .login-card { padding: 46px 42px; }
}

@media (max-width: 680px) {
  .login-page { padding: 0; display: block; background: #fff; }
  .login-shell {
    min-height: 100dvh;
    display: block;
    border: 0;
    border-radius: 0;
    box-shadow: none;
  }
  .login-intro {
    min-height: 190px;
    padding: 28px 24px 24px;
    border-right: 0;
    border-bottom: 1px solid #dcecf9;
  }
  .brand-row { gap: 11px; }
  .intro-logo { width: 44px; height: 44px; flex-basis: 44px; border-radius: 13px; font-size: 18px; }
  .brand-name { font-size: 16px; }
  .brand-note { font-size: 10px; }
  .intro-copy { margin-top: 25px; }
  .eyebrow { display: none; }
  .intro-copy h1 { font-size: 30px; }
  .intro-copy h1 br { display: none; }
  .intro-description { margin-top: 10px; font-size: 12px; line-height: 1.6; }
  .delivery-visual { display: none; }
  .login-card { width: 100%; padding: 34px 24px 40px; }
  .card-heading h2 { font-size: 27px; }
  .role-switcher { margin: 25px 0 24px; }
  .role-item { height: 50px; flex-direction: column; gap: 3px; font-size: 10px; }
  .role-item i { font-size: 13px; }
  .form-options { align-items: flex-start; }
  .continue-hint { max-width: 55%; line-height: 1.4; }
}

@media (prefers-reduced-motion: reduce) {
  .role-item,
  .input-wrap,
  .password-toggle,
  .login-button { transition: none; }
}
</style>
