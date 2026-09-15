<template>
  <main class="login-page" :class="{ 'reference-background-ready': referenceBackgroundReady }">
    <div class="background-layer" aria-hidden="true">
      <picture>
        <img
          src="/images/login-reference-mobile.png"
          alt=""
          @load="referenceBackgroundReady = true"
        />
      </picture>
    </div>
    <div class="background-tint" aria-hidden="true"></div>

    <header class="brand-header">
      <router-link class="brand-link" to="/index" aria-label="返回首页">
        <span class="brand-mark" aria-hidden="true"><i class="fa fa-cutlery"></i></span>
        <span class="brand-name">饿了么</span>
        <span class="brand-divider" aria-hidden="true"></span>
        <span class="brand-product">校园外卖</span>
      </router-link>
      <router-link class="home-link" to="/index">
        <i class="fa fa-arrow-left" aria-hidden="true"></i>
        <span>返回首页</span>
      </router-link>
    </header>

    <section class="hero-shell" aria-label="账号登录">
      <div class="hero-side">
        <div class="hero-copy">
          <h1>美味即刻到达<br><span>校园生活更精彩</span></h1>
          <p class="hero-description">附近好店 · 便捷下单 · 即时配送<br>让每一餐都充满幸福感</p>
          <div class="hero-line"></div>
        </div>
      </div>

      <form
        class="login-panel"
        :class="{ 'error-shake': errorPulse, 'success-state': loginSucceeded }"
        @submit.prevent="login"
      >
        <div class="panel-heading">
          <transition name="role-heading" mode="out-in">
            <div :key="selectedRole" class="panel-heading-copy">
              <h2>{{ activeRole.title }}</h2>
              <p>{{ activeRole.subtitle }}</p>
            </div>
          </transition>
        </div>

        <div
          class="role-tabs"
          role="tablist"
          aria-label="登录身份"
          :style="{ '--role-index': activeRoleIndex }"
        >
          <button
            v-for="item in roleOptions"
            :key="item.key"
            type="button"
            role="tab"
            :aria-selected="selectedRole === item.key"
            :class="{ active: selectedRole === item.key }"
            :disabled="submitting"
            @click="selectRole(item.key)"
          >
            {{ item.label }}
          </button>
          <span class="role-indicator" aria-hidden="true"></span>
        </div>

        <transition name="hint-fade">
          <p v-if="hasRedirect" class="redirect-hint">
            <i class="fa fa-angle-right"></i>
            登录后将返回刚才的页面
          </p>
        </transition>

        <label class="field" for="login-username">
          <span>用户名 / 手机号</span>
          <div class="input-shell">
            <i class="fa fa-user-o"></i>
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
          <div class="input-shell">
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

        <div class="form-row">
          <label class="remember-option">
            <input v-model="rememberMe" type="checkbox" />
            <span class="checkbox-ui" aria-hidden="true"><i class="fa fa-check"></i></span>
            <span>记住我</span>
          </label>
          <button class="forgot-link" type="button" @click="showForgotPasswordMessage">忘记密码？</button>
        </div>

        <transition name="error-fade">
          <p v-if="inlineError" class="inline-error" role="alert">
            <i class="fa fa-exclamation-circle"></i>
            <span>{{ inlineError }}</span>
          </p>
        </transition>

        <button
          class="login-button"
          :class="{ success: loginSucceeded }"
          type="submit"
          :disabled="submitting || loginSucceeded"
        >
          <i v-if="submitting" class="fa fa-spinner fa-spin"></i>
          <i v-else-if="loginSucceeded" class="fa fa-spinner fa-spin"></i>
          <span>{{ loginButtonText }}</span>
        </button>

        <transition name="hint-fade">
          <p v-if="selectedRole === 'user'" class="register-line">
            还没有账号？<router-link to="/register">立即注册</router-link>
          </p>
        </transition>
      </form>
    </section>

    <transition name="success-feedback">
      <div v-if="loginSucceeded" class="success-feedback" role="status" aria-live="polite">
        <span class="success-feedback-icon" aria-hidden="true"><i class="fa fa-check"></i></span>
        <span class="success-feedback-copy">
          <strong>登录成功</strong>
          <small>正在进入首页，请稍候...</small>
        </span>
      </div>
    </transition>

    <footer class="login-footer" aria-label="校园外卖服务优势">
      <div v-for="feature in features" :key="feature.title" class="feature-item">
        <span class="feature-icon" :class="feature.color" aria-hidden="true"><i :class="feature.icon"></i></span>
        <span class="feature-copy">
          <strong>{{ feature.title }}</strong>
          <small>{{ feature.description }}</small>
        </span>
      </div>
    </footer>
  </main>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '../utils/request';
import { toast } from '../utils/toast';
import { ROLE_DEFINITIONS } from '../utils/roles';
import { clearAuth, saveAuth } from '../utils/auth';
import { createAuthenticationClient, selectLoginDestination } from '../utils/authenticationClient';
import { settleRouteWipe, startRouteWipe } from '../utils/routeMotion';

const router = useRouter();
const route = useRoute();
const authentication = createAuthenticationClient(request, { clearAuth, saveAuth });
const roleMap = ROLE_DEFINITIONS;
const roleOptions = Object.values(roleMap);
const queryRole = typeof route.query.role === 'string' && roleMap[route.query.role]
  ? route.query.role
  : 'user';

const selectedRole = ref(queryRole);
const rememberedName = () => {
  try { return localStorage.getItem('savedUserName') || ''; } catch (_) { return ''; }
};
const userName = ref(rememberedName());
const password = ref('');
const showPassword = ref(false);
const rememberMe = ref(false);
const submitting = ref(false);
const loginSucceeded = ref(false);
const inlineError = ref('');
const errorPulse = ref(false);
const referenceBackgroundReady = ref(false);

const activeRole = computed(() => roleMap[selectedRole.value]);
const activeRoleIndex = computed(() => Math.max(0, roleOptions.findIndex(item => item.key === selectedRole.value)));
const hasRedirect = computed(() => {
  const redirect = route.query.redirect;
  return selectLoginDestination({ role: selectedRole.value }, redirect, router.resolve) !== activeRole.value.target;
});
const loginButtonText = computed(() => {
  if (loginSucceeded.value) return '登录成功';
  if (submitting.value) return '正在登录…';
  return activeRole.value.button;
});
const features = [
  { title: '海量美食', description: '周边好店任你选', icon: 'fa fa-shopping-bag', color: 'blue' },
  { title: '快速配送', description: '美味即时送达', icon: 'fa fa-bolt', color: 'sky' },
  { title: '品质保障', description: '安全卫生有保障', icon: 'fa fa-leaf', color: 'green' },
  { title: '校园专属', description: '专属于你的校园外卖', icon: 'fa fa-heart', color: 'coral' }
];

watch(() => route.query.role, value => {
  if (typeof value === 'string' && roleMap[value]) selectedRole.value = value;
  inlineError.value = '';
});

const selectRole = (key) => {
  if (submitting.value || key === selectedRole.value) return;
  selectedRole.value = key;
  inlineError.value = '';
  router.replace({ query: { ...route.query, role: key } });
};

const showForgotPasswordMessage = () => {
  toast.info('请联系管理员重置密码');
};

const triggerError = (message) => {
  inlineError.value = message;
  errorPulse.value = false;
  requestAnimationFrame(() => {
    errorPulse.value = true;
    window.setTimeout(() => { errorPulse.value = false; }, 320);
  });
};

const prefersReducedMotion = () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
const waitForSuccessFeedback = () => prefersReducedMotion()
  ? Promise.resolve()
  : new Promise(resolve => setTimeout(resolve, 280));

const navigateAfterLogin = async (target) => {
  if (prefersReducedMotion()) {
    await router.push(target);
    return;
  }
  startRouteWipe(document.querySelector('.login-button'));
  await new Promise(resolve => setTimeout(resolve, 390));
  await router.push(target);
  await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
  settleRouteWipe();
};

const login = async () => {
  if (!userName.value) {
    triggerError('请输入用户名或手机号');
    return;
  }
  if (!password.value) {
    triggerError('请输入密码');
    return;
  }

  submitting.value = true;
  inlineError.value = '';
  try {
    const session = await authentication.login({
      username: userName.value,
      password: password.value,
      rememberMe: rememberMe.value,
      role: selectedRole.value
    });
    try {
      if (rememberMe.value) localStorage.setItem('savedUserName', userName.value);
      else localStorage.removeItem('savedUserName');
    } catch (_) { /* 记住用户名不可用时不影响已验证的登录。 */ }

    loginSucceeded.value = true;
    if (session.applicationOnly) toast.info(`请先完成${activeRole.value.label}入驻申请`);
    else toast.success(`已进入${activeRole.value.label}端`);
    await waitForSuccessFeedback();

    await navigateAfterLogin(selectLoginDestination(session, route.query.redirect, router.resolve));
  } catch (error) {
    clearAuth();
    loginSucceeded.value = false;
    triggerError(error.response?.data?.message || error.message || '用户名或密码错误');
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
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: var(--skin-brand-strong, #173b60);
  background: linear-gradient(135deg, var(--skin-surface, #fbfdff) 0%, var(--skin-surface, #f7fbfe) 48%, var(--skin-surface, #fafdff) 100%);
}

.ambient {
  position: absolute;
  pointer-events: none;
  border-radius: 50%;
}

.ambient-left {
  width: 520px;
  height: 520px;
  left: -300px;
  bottom: -290px;
  background: rgba(var(--skin-brand-rgb, 0, 151, 255), 0.07);
  animation: ambient-left 14s ease-in-out infinite alternate;
}

.ambient-right {
  width: 420px;
  height: 420px;
  right: -190px;
  top: -230px;
  background: rgba(var(--skin-brand-rgb, 0, 151, 255), 0.055);
  animation: ambient-right 16s ease-in-out infinite alternate;
}

.brand-header {
  position: relative;
  z-index: 5;
  width: min(1180px, calc(100% - 64px));
  height: 88px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  animation: fade-down .48s cubic-bezier(.22, .61, .36, 1) both;
}

.brand-link {
  display: inline-flex;
  align-items: center;
  color: inherit;
  text-decoration: none;
}

.brand-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  margin-right: 11px;
  border-radius: 12px;
  color: #fff;
  background: var(--skin-brand, #0097ff);
  font-size: 17px;
  box-shadow: 0 8px 22px rgba(var(--skin-brand-rgb, 0, 151, 255), 0.16);
  transition: transform .22s ease, box-shadow .22s ease;
}

.brand-link:hover .brand-mark {
  transform: translateY(-1px) rotate(-2deg);
  box-shadow: 0 11px 25px rgba(var(--skin-brand-rgb, 0, 151, 255), 0.21);
}

.brand-name {
  color: var(--skin-brand-strong, #173b60);
  font-size: 20px;
  font-weight: 800;
  letter-spacing: .02em;
}

.brand-divider {
  width: 1px;
  height: 18px;
  margin: 0 12px;
  background: var(--skin-border, #d7e4ef);
}

.brand-product {
  color: var(--skin-muted, #7c94a8);
  font-size: 13px;
}

.home-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--skin-muted, #70899f);
  font-size: 13px;
  text-decoration: none;
  transition: color .18s ease, transform .18s ease;
}

.home-link i {
  font-size: 11px;
  transition: transform .18s ease;
}

.home-link:hover {
  color: var(--skin-brand, #0097ff);
  transform: translateX(-2px);
}

.home-link:hover i { transform: translateX(-2px); }

.hero-shell {
  position: relative;
  z-index: 2;
  width: min(1100px, calc(100% - 64px));
  margin: auto;
  padding: 18px 0 54px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 410px;
  align-items: center;
  gap: 78px;
}

.hero-side {
  position: relative;
  min-width: 0;
  min-height: 540px;
  display: flex;
  align-items: center;
}

.hero-copy {
  position: relative;
  z-index: 3;
  padding-left: 8px;
  transform: translateY(-58px);
}

.hero-kicker,
.hero-copy h1,
.hero-description,
.hero-line,
.hero-note {
  opacity: 0;
  animation: hero-rise .56s cubic-bezier(.22, .61, .36, 1) forwards;
}

.hero-kicker { animation-delay: .08s; }
.hero-copy h1 { animation-delay: .15s; }
.hero-description { animation-delay: .22s; }
.hero-line { animation-delay: .29s; }
.hero-note { animation-delay: .34s; }

.hero-kicker {
  margin: 0 0 15px;
  color: var(--skin-brand, #4e91c5);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .17em;
}

.hero-copy h1 {
  margin: 0;
  color: var(--skin-brand-strong, #173b60);
  font-size: clamp(48px, 4.5vw, 64px);
  line-height: 1.045;
  letter-spacing: -.048em;
  font-weight: 800;
}

.hero-copy h1 span { color: var(--skin-brand, #0097ff); }

.hero-description {
  margin: 23px 0 0;
  color: var(--skin-muted, #748da3);
  font-size: 14px;
  line-height: 1.8;
}

.hero-line {
  width: 42px;
  height: 3px;
  margin-top: 25px;
  border-radius: 999px;
  background: var(--skin-brand, #0097ff);
}

.hero-note {
  margin: 11px 0 0;
  color: var(--skin-subtle, #9aaab7);
  font-size: 11px;
}

.food-visual {
  position: absolute;
  z-index: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: min(64vh, 590px);
  opacity: .98;
  pointer-events: none;
  overflow: hidden;
}

.food-visual::before,
.food-visual::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}

.food-visual::before {
  background: linear-gradient(180deg, rgba(var(--skin-surface-rgb, 248, 251, 254), 0.76) 0%, rgba(var(--skin-surface-rgb, 248, 251, 254), 0.38) 34%, rgba(var(--skin-surface-rgb, 248, 251, 254), 0.08) 72%, rgba(var(--skin-surface-rgb, 248, 251, 254), 0) 100%);
}

.food-visual::after {
  background: linear-gradient(90deg, rgba(var(--skin-surface-rgb, 248, 251, 254), 0) 0%, rgba(var(--skin-surface-rgb, 248, 251, 254), 0.05) 38%, rgba(var(--skin-surface-rgb, 248, 251, 254), 0.62) 72%, var(--skin-surface, #f8fbfe) 100%);
}

.food-visual img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  object-position: left center;
  filter: saturate(1.04) contrast(1.02) brightness(1.02);
}

.login-panel {
  width: 100%;
  padding: 31px 34px 27px;
  border: 1px solid rgba(var(--skin-border-rgb, 207, 223, 235), 0.88);
  border-radius: 18px;
  background: rgba(255, 255, 255, .965);
  box-shadow: 0 18px 46px rgba(var(--skin-ink-rgb, 50, 83, 112), 0.075);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  animation: panel-rise .58s cubic-bezier(.22, .61, .36, 1) .17s both;
  transition: border-color .22s ease, box-shadow .22s ease, transform .22s ease;
}

.login-panel:hover {
  border-color: var(--skin-border, #d3e4ef);
  box-shadow: 0 20px 50px rgba(var(--skin-ink-rgb, 50, 83, 112), 0.09);
}

.login-panel.success-state { border-color: rgba(32, 165, 107, .28); }

.panel-heading {
  min-height: 70px;
  margin-bottom: 17px;
}

.panel-kicker {
  margin: 0 0 6px;
  color: var(--skin-muted, #5c8db4);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: .02em;
}

.panel-heading h2 {
  margin: 0;
  color: var(--skin-brand-strong, #173b60);
  font-size: 27px;
  line-height: 1.22;
  letter-spacing: -.028em;
}

.panel-heading-copy > p {
  margin: 7px 0 0;
  color: var(--skin-muted, #8a9dac);
  font-size: 12px;
}

.role-heading-enter-active,
.role-heading-leave-active {
  transition: opacity .16s ease, transform .16s ease;
}

.role-heading-enter-from { opacity: 0; transform: translateY(5px); }
.role-heading-leave-to { opacity: 0; transform: translateY(-4px); }

.role-tabs {
  --role-index: 0;
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin-bottom: 21px;
  border-bottom: 1px solid var(--skin-border, #e8eff4);
}

.role-tabs button {
  position: relative;
  z-index: 1;
  height: 40px;
  padding: 0 4px;
  border: 0;
  background: transparent;
  color: var(--skin-muted, #8297aa);
  font: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: color .2s ease, transform .18s ease;
}

.role-tabs button:hover:not(:disabled) {
  color: var(--skin-brand, #267ebc);
  transform: translateY(-1px);
}

.role-tabs button.active { color: var(--skin-brand, #008de9); font-weight: 600; }
.role-tabs button:disabled { cursor: wait; opacity: .55; }

.role-indicator {
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 25%;
  height: 2px;
  pointer-events: none;
  transform: translateX(calc(var(--role-index) * 100%));
  transition: transform .36s cubic-bezier(.22, .9, .32, 1.18);
}

.role-indicator::after {
  content: '';
  display: block;
  width: 58%;
  height: 100%;
  margin: 0 auto;
  border-radius: 999px;
  background: var(--skin-brand, #0097ff);
  box-shadow: 0 1px 5px rgba(var(--skin-brand-rgb, 0, 151, 255), 0.18);
}

.redirect-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: -8px 0 17px;
  color: var(--skin-muted, #6f90a9);
  font-size: 10px;
}

.redirect-hint i { color: var(--skin-brand, #0097ff); }

.field {
  display: block;
  margin-bottom: 15px;
}

.field > span {
  display: block;
  margin-bottom: 7px;
  color: var(--skin-muted, #425f78);
  font-size: 12px;
  font-weight: 600;
}

.input-shell {
  height: 48px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  border: 1px solid var(--skin-border, #dbe7ef);
  border-radius: 11px;
  background: var(--skin-surface, #fbfdff);
  transition: border-color .18s ease, box-shadow .18s ease, background .18s ease, transform .18s ease;
}

.input-shell:hover { border-color: var(--skin-border, #cddfea); }

.input-shell:focus-within {
  border-color: var(--skin-brand-soft, #62b9f1);
  background: #fff;
  box-shadow: 0 0 0 4px rgba(var(--skin-brand-rgb, 0, 151, 255), 0.065);
  transform: translateY(-1px);
}

.input-shell > i {
  width: 16px;
  flex: 0 0 16px;
  text-align: center;
  color: var(--skin-subtle, #a1b3c1);
  font-size: 13px;
  transition: color .18s ease;
}

.input-shell:focus-within > i { color: var(--skin-brand, #0097ff); }

.input-shell input {
  min-width: 0;
  flex: 1;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--skin-ink, #294861);
  font: inherit;
  font-size: 14px;
}

.input-shell input::placeholder { color: var(--skin-subtle, #b1c0cc); }

.password-toggle {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  flex: 0 0 30px;
  border: 0;
  background: transparent;
  color: var(--skin-muted, #8ea4b6);
  cursor: pointer;
  transition: color .18s ease, transform .18s ease;
}

.password-toggle:hover { color: var(--skin-brand, #0097ff); transform: scale(1.06); }

.form-row {
  display: flex;
  align-items: center;
  margin: 1px 0 18px;
  color: var(--skin-muted, #7f95a7);
  font-size: 12px;
}

.remember-option {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.remember-option input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.checkbox-ui {
  width: 16px;
  height: 16px;
  display: grid;
  place-items: center;
  border: 1px solid var(--skin-subtle, #c7d7e3);
  border-radius: 5px;
  background: #fff;
  color: transparent;
  font-size: 9px;
  transition: background .18s ease, border-color .18s ease, box-shadow .18s ease, transform .18s ease;
}

.remember-option:hover .checkbox-ui {
  border-color: var(--skin-brand-soft, #8dcdf7);
  transform: translateY(-1px);
}

.remember-option input:checked + .checkbox-ui {
  border-color: var(--skin-brand, #0097ff);
  background: var(--skin-brand, #0097ff);
  color: #fff;
  box-shadow: 0 3px 8px rgba(var(--skin-brand-rgb, 0, 151, 255), 0.18);
}

.remember-option input:focus-visible + .checkbox-ui {
  box-shadow: 0 0 0 3px rgba(var(--skin-brand-rgb, 0, 151, 255), 0.12);
}

.inline-error {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  margin: -4px 0 13px;
  padding: 9px 11px;
  border-radius: 9px;
  background: #fff4f2;
  color: #c04c42;
  font-size: 11px;
  line-height: 1.5;
}

.inline-error i { margin-top: 2px; }

.error-fade-enter-active,
.error-fade-leave-active,
.hint-fade-enter-active,
.hint-fade-leave-active {
  transition: opacity .18s ease, transform .18s ease;
}

.error-fade-enter-from,
.hint-fade-enter-from { opacity: 0; transform: translateY(-4px); }

.error-fade-leave-to,
.hint-fade-leave-to { opacity: 0; transform: translateY(3px); }

.login-button {
  width: 100%;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-radius: 11px;
  background: var(--skin-brand, #0097ff);
  color: #fff;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 9px 22px rgba(var(--skin-brand-rgb, 0, 151, 255), 0.16);
  transition: transform .18s ease, box-shadow .18s ease, background .18s ease;
}

.login-button:hover:not(:disabled) {
  background: var(--skin-brand, #008ae8);
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(var(--skin-brand-rgb, 0, 151, 255), 0.21);
}

.login-button:active:not(:disabled) { transform: translateY(1px) scale(.995); }
.login-button:disabled { cursor: wait; opacity: .72; }
.login-button.success { background: #20a56b; box-shadow: 0 9px 22px rgba(32, 165, 107, .18); }

.register-line {
  margin: 16px 0 0;
  text-align: center;
  color: var(--skin-muted, #879aa9);
  font-size: 11px;
}

.register-line a {
  margin-left: 5px;
  color: var(--skin-brand, #0097ff);
  font-weight: 600;
  text-decoration: none;
}

.register-line a:hover { text-decoration: underline; }

.login-footer {
  position: relative;
  z-index: 2;
  padding: 0 20px 20px;
  text-align: center;
  color: var(--skin-subtle, #bfccd5);
  font-size: 9px;
  letter-spacing: .07em;
  animation: footer-fade .5s ease .34s both;
}

.error-shake { animation: panel-shake .3s ease both; }

@keyframes fade-down {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes hero-rise {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes panel-rise {
  from { opacity: 0; transform: translateY(14px) scale(.994); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes footer-fade {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes food-float {
  from { transform: translate3d(0, 0, 0) scale(1); }
  to { transform: translate3d(8px, -6px, 0) scale(1.012); }
}

@keyframes ambient-left {
  from { transform: translate3d(0, 0, 0) scale(1); }
  to { transform: translate3d(10px, -6px, 0) scale(1.025); }
}

@keyframes ambient-right {
  from { transform: translate3d(0, 0, 0) scale(1); }
  to { transform: translate3d(-9px, 10px, 0) scale(1.035); }
}

@keyframes panel-shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-5px); }
  40% { transform: translateX(5px); }
  60% { transform: translateX(-3px); }
  80% { transform: translateX(3px); }
}

@media (max-width: 980px) {
  .hero-shell {
    gap: 52px;
    grid-template-columns: minmax(0, 1fr) 400px;
  }

  .food-visual { width: 64%; opacity: .9; }
}

@media (max-width: 880px) {
  .brand-header {
    width: min(100% - 36px, 640px);
    height: 76px;
  }

  .brand-product,
  .brand-divider { display: none; }

  .home-link span { display: none; }
  .home-link { width: 36px; height: 36px; justify-content: center; }

  .hero-shell {
    width: min(100% - 36px, 460px);
    grid-template-columns: 1fr;
    gap: 26px;
    padding: 14px 0 40px;
  }

  .hero-side {
    min-height: auto;
    display: block;
  }

  .hero-copy {
    padding: 2px 2px 0;
    text-align: center;
    transform: none;
  }

  .hero-kicker,
  .hero-line,
  .hero-note { display: none; }

  .food-visual {
    display: block;
    height: 240px;
    opacity: .82;
  }

  .food-visual::after {
    background: linear-gradient(90deg, rgba(var(--skin-surface-rgb, 248, 251, 254), 0) 0%, rgba(var(--skin-surface-rgb, 248, 251, 254), 0.42) 56%, var(--skin-surface, #f8fbfe) 100%);
  }

  .hero-copy h1 {
    font-size: 35px;
    line-height: 1.12;
    letter-spacing: -.04em;
  }

  .hero-copy h1 br { display: none; }
  .hero-copy h1 span::before { content: ' '; }

  .hero-description {
    margin-top: 11px;
    font-size: 13px;
    line-height: 1.7;
  }

  .hero-description br { display: none; }

  .login-panel {
    padding: 28px 25px 26px;
    border-radius: 17px;
  }
}

@media (max-width: 420px) {
  .brand-header,
  .hero-shell { width: calc(100% - 28px); }

  .brand-header { height: 68px; }
  .brand-mark { width: 38px; height: 38px; border-radius: 11px; }
  .brand-name { font-size: 18px; }

  .hero-shell { padding-top: 2px; gap: 20px; }
  .hero-copy h1 { font-size: 30px; }
  .hero-description { font-size: 12px; }

  .login-panel { padding: 25px 19px 23px; }
  .panel-heading h2 { font-size: 24px; }

  .role-tabs button { height: 39px; font-size: 12px; }
  .input-shell { height: 47px; }
  .login-button { height: 47px; }
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    scroll-behavior: auto !important;
    transition: none !important;
    animation: none !important;
  }
}

/* Reference layout: a full-bleed campus scene with the sign-in surface anchored on the right. */
.login-page {
  min-height: 100dvh;
  color: var(--skin-ink, #24364c);
  background: var(--skin-surface, #eaf7ff);
  isolation: isolate;
}

.background-layer,
.background-tint {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.background-layer { z-index: -3; overflow: hidden; }
.background-layer picture { display: block; width: 100%; height: 100%; }

.background-layer img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  object-position: center center;
  filter: saturate(1.06) brightness(1.04);
  animation: background-drift 18s ease-in-out infinite alternate;
}

.background-tint {
  z-index: -2;
  background:
    linear-gradient(90deg, rgba(255,255,255,.9) 0%, rgba(255,255,255,.69) 29%, rgba(255,255,255,.2) 53%, rgba(var(--skin-surface-rgb, 241, 250, 255), 0.25) 73%, rgba(var(--skin-surface-rgb, 239, 249, 255), 0.77) 100%),
    linear-gradient(0deg, rgba(255,255,255,.28) 0%, rgba(255,255,255,.03) 45%, rgba(255,255,255,.18) 100%);
}

.ambient { z-index: -1; opacity: .35; }

.brand-header {
  width: min(1532px, calc(100% - 10.8vw));
  height: 91px;
  margin: 0 auto;
  justify-content: flex-start;
  animation: fade-down .48s cubic-bezier(.22, .61, .36, 1) both;
}

.brand-mark {
  width: 54px;
  height: 54px;
  margin-right: 14px;
  border-radius: 16px;
  font-size: 23px;
  box-shadow: 0 9px 20px rgba(var(--skin-brand-rgb, 0, 130, 245), 0.2);
}

.brand-name {
  color: var(--skin-ink, #152033);
  font-size: 25px;
  letter-spacing: .01em;
}

.brand-divider { height: 26px; margin: 0 17px; background: var(--skin-border, #c9d7e4); }
.brand-product { color: var(--skin-muted, #52799f); font-size: 18px; }
.home-link { display: none; }

.hero-shell {
  width: min(1532px, calc(100% - 10.8vw));
  min-height: 0;
  flex: 1;
  margin: 0 auto;
  padding: 18px 0 22px;
  grid-template-columns: minmax(0, 1fr) 554px;
  align-items: center;
  gap: 68px;
}

.hero-side {
  min-height: 0;
  align-items: center;
  padding: 0 0 16px 8px;
}

.hero-copy {
  padding-left: 0;
  transform: translateY(-20px);
}

.hero-copy h1 {
  color: var(--skin-ink, #172333);
  font-size: 62px;
  line-height: 1.12;
  letter-spacing: 0;
  font-weight: 900;
  text-shadow: 0 2px 0 rgba(255,255,255,.42);
}

.hero-copy h1 span { color: var(--skin-brand, #087cf0); }

.hero-description {
  margin-top: 24px;
  color: var(--skin-muted, #4c6580);
  font-size: 20px;
  line-height: 1.72;
  font-weight: 500;
}

.hero-line {
  width: 230px;
  height: 7px;
  margin-top: 18px;
  border-radius: 99px;
  background: var(--skin-brand, #087cf0);
  transform: rotate(-2deg);
  box-shadow: 0 2px 4px rgba(var(--skin-brand-rgb, 8, 124, 240), 0.13);
}

.campus-doodle {
  position: absolute;
  z-index: 1;
  color: var(--skin-brand-strong, #124e87);
  font-family: 'KaiTi', 'STKaiti', cursive;
  font-size: 20px;
  line-height: 1.5;
  letter-spacing: .04em;
  pointer-events: none;
  text-align: center;
}

.campus-doodle span { color: var(--skin-brand, #087cf0); font-family: Arial, sans-serif; }
.doodle-top { top: 43px; right: 6%; transform: rotate(-8deg); }
.doodle-top::after,
.doodle-bottom::after {
  content: '';
  display: block;
  width: 178px;
  height: 9px;
  margin: -2px auto 0;
  border-bottom: 2px solid var(--skin-brand, #1684ee);
  border-radius: 50%;
  transform: rotate(-3deg);
}

.doodle-mid { top: 15%; left: 51%; transform: rotate(-10deg); color: rgba(255,255,255,.96); font-size: 22px; }
.doodle-mid::after { content: '↗'; display: block; margin-top: 6px; font: 40px/1 Georgia, serif; }
.doodle-bottom { right: 6%; bottom: 39px; transform: rotate(-8deg); }
.doodle-bottom::after { width: 163px; }

.login-panel {
  width: 554px;
  min-height: 622px;
  padding: 52px 47px 39px;
  border: 0;
  border-radius: 25px;
  background: rgba(255,255,255,.94);
  box-shadow: 0 22px 55px rgba(var(--skin-ink-rgb, 50, 91, 125), 0.14), 0 2px 9px rgba(var(--skin-ink-rgb, 50, 91, 125), 0.05);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  animation: panel-rise .58s cubic-bezier(.22, .61, .36, 1) .17s both;
}

.login-panel:hover { border-color: transparent; box-shadow: 0 24px 60px rgba(var(--skin-ink-rgb, 50, 91, 125), 0.17), 0 2px 9px rgba(var(--skin-ink-rgb, 50, 91, 125), 0.05); }
.panel-heading { min-height: 82px; margin-bottom: 12px; }
.panel-heading h2 { color: var(--skin-ink, #111820); font-size: 32px; line-height: 1.2; letter-spacing: 0; font-weight: 800; }
.panel-heading-copy > p { margin-top: 11px; color: var(--skin-muted, #8297ad); font-size: 16px; }

.role-tabs { margin-bottom: 29px; border-bottom-color: var(--skin-border, #dce5ed); }
.role-tabs button { height: 47px; color: var(--skin-muted, #607188); font-size: 16px; font-weight: 500; }
.role-tabs button.active { color: var(--skin-brand, #087cf0); font-weight: 700; }
.role-indicator { height: 3px; }
.role-indicator::after { width: 58%; background: var(--skin-brand, #087cf0); box-shadow: 0 2px 8px rgba(var(--skin-brand-rgb, 8, 124, 240), 0.26); }

.redirect-hint { margin: -9px 0 18px; font-size: 12px; }
.field { margin-bottom: 20px; }
.field > span { display: none; }

.input-shell {
  height: 64px;
  gap: 16px;
  padding: 0 20px;
  border: 1px solid var(--skin-border, #e4ebf1);
  border-radius: 18px;
  background: rgba(var(--skin-surface-rgb, 247, 250, 252), 0.92);
}

.input-shell:hover { border-color: var(--skin-border, #d2e1ec); background: var(--skin-surface, #f9fcfe); }
.input-shell:focus-within { border-color: var(--skin-brand-soft, #7abaf1); box-shadow: 0 0 0 4px rgba(var(--skin-brand-rgb, 8, 124, 240), 0.08); transform: none; }
.input-shell > i { width: 20px; flex-basis: 20px; color: var(--skin-muted, #71869d); font-size: 18px; }
.input-shell input { color: var(--skin-ink, #3b4c61); font-size: 18px; }
.input-shell input::placeholder { color: var(--skin-muted, #8d9caf); }
.password-toggle { width: 35px; height: 35px; flex-basis: 35px; font-size: 17px; }

.form-row {
  justify-content: space-between;
  margin: 2px 0 31px;
  color: var(--skin-muted, #6d8196);
  font-size: 16px;
}

.remember-option { gap: 9px; }
.checkbox-ui { width: 22px; height: 22px; border-color: var(--skin-subtle, #b7c8d8); border-radius: 6px; font-size: 12px; }
.forgot-link { padding: 0; border: 0; background: transparent; color: var(--skin-muted, #58718c); font: inherit; font-size: 16px; cursor: pointer; }
.forgot-link:hover { color: var(--skin-brand, #087cf0); }

.inline-error { margin: -13px 0 17px; padding: 11px 13px; font-size: 13px; }

.login-button {
  height: 66px;
  border-radius: 17px;
  background: linear-gradient(180deg, var(--skin-brand, #1a94ff) 0%, var(--skin-brand, #087cf0) 100%);
  font-size: 22px;
  box-shadow: 0 12px 24px rgba(var(--skin-brand-rgb, 8, 124, 240), 0.23);
}

.login-button:hover:not(:disabled) { background: linear-gradient(180deg, var(--skin-brand, #2b9dff) 0%, var(--skin-brand, #0877e5) 100%); box-shadow: 0 15px 28px rgba(var(--skin-brand-rgb, 8, 124, 240), 0.28); }
.login-button.success { background: #20a56b; }

.register-line { margin-top: 24px; color: var(--skin-muted, #7e91a5); font-size: 16px; }
.register-line a { margin-left: 7px; color: var(--skin-brand, #087cf0); font-weight: 700; }

.login-footer {
  position: relative;
  z-index: 2;
  width: min(1532px, calc(100% - 10.8vw));
  margin: 0 auto;
  padding: 7px 0 32px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 66px;
  color: var(--skin-ink, #263c53);
  font-size: 16px;
  letter-spacing: 0;
  text-align: left;
  animation: footer-fade .5s ease .34s both;
}

.feature-item { display: inline-flex; align-items: center; gap: 14px; min-width: 205px; }
.feature-icon { width: 59px; height: 59px; display: grid; place-items: center; flex: 0 0 59px; border-radius: 50%; color: #fff; font-size: 25px; box-shadow: 0 7px 14px rgba(var(--skin-brand-rgb, 31, 120, 220), 0.17); }
.feature-icon.blue { background: var(--skin-brand, #1687f5); }
.feature-icon.sky { background: var(--skin-brand, #2a9eea); }
.feature-icon.green { background: #35c886; }
.feature-icon.coral { background: #f35c67; }
.feature-copy { display: grid; gap: 4px; }
.feature-copy strong { color: var(--skin-ink, #1d344c); font-size: 18px; font-weight: 800; white-space: nowrap; }
.feature-copy small { color: var(--skin-muted, #67809b); font-size: 14px; white-space: nowrap; }

@keyframes background-drift {
  from { transform: scale(1); }
  to { transform: scale(1.018); }
}

@media (max-width: 1280px) {
  .brand-header, .hero-shell, .login-footer { width: min(1160px, calc(100% - 64px)); }
  .hero-shell { grid-template-columns: minmax(0, 1fr) 480px; gap: 42px; }
  .login-panel { width: 480px; padding: 43px 38px 34px; }
  .hero-copy h1 { font-size: 52px; }
  .hero-description { font-size: 17px; }
  .login-footer { gap: 28px; }
  .feature-item { min-width: 175px; gap: 10px; }
  .feature-icon { width: 50px; height: 50px; flex-basis: 50px; font-size: 21px; }
  .feature-copy strong { font-size: 16px; }
  .feature-copy small { font-size: 12px; }
}

@media (max-width: 880px) {
  .login-page { overflow-y: auto; }
  .background-layer img { object-position: 35% center; }
  .background-tint { background: linear-gradient(180deg, rgba(var(--skin-surface-rgb, 239, 249, 255), 0.2) 0%, rgba(255,255,255,.83) 36%, rgba(255,255,255,.97) 73%); }
  .brand-header { width: min(100% - 36px, 620px); height: 78px; }
  .brand-mark { width: 46px; height: 46px; border-radius: 14px; font-size: 19px; }
  .brand-name { font-size: 21px; }
  .brand-product, .brand-divider { display: none; }
  .hero-shell { width: min(100% - 36px, 520px); display: grid; grid-template-columns: 1fr; gap: 28px; padding: 12px 0 24px; }
  .hero-side { display: block; padding: 0; }
  .hero-copy { text-align: center; transform: none; }
  .hero-copy h1 { font-size: 42px; line-height: 1.16; }
  .hero-copy h1 br { display: block; }
  .hero-description { margin-top: 14px; font-size: 15px; line-height: 1.65; }
  .hero-line { width: 130px; height: 5px; margin: 13px auto 0; }
  .doodle-mid, .doodle-bottom { display: none; }
  .doodle-top { top: 77px; right: 8px; font-size: 14px; }
  .login-panel { width: 100%; min-height: 0; padding: 33px 28px 30px; border-radius: 22px; }
  .panel-heading { min-height: 68px; }
  .panel-heading h2 { font-size: 28px; }
  .panel-heading-copy > p { font-size: 14px; }
  .role-tabs button { font-size: 14px; }
  .login-footer { width: min(100% - 36px, 520px); display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px 12px; padding: 8px 0 28px; }
  .feature-item { min-width: 0; gap: 9px; }
  .feature-icon { width: 43px; height: 43px; flex-basis: 43px; font-size: 18px; }
  .feature-copy strong { font-size: 13px; }
  .feature-copy small { font-size: 10px; }
}

@media (max-width: 420px) {
  .brand-header, .hero-shell, .login-footer { width: calc(100% - 28px); }
  .brand-header { height: 68px; }
  .brand-mark { width: 40px; height: 40px; border-radius: 12px; }
  .brand-name { font-size: 19px; }
  .hero-shell { gap: 21px; padding-top: 4px; }
  .hero-copy h1 { font-size: 34px; }
  .hero-description { font-size: 13px; }
  .login-panel { padding: 28px 19px 25px; }
  .panel-heading h2 { font-size: 25px; }
  .input-shell { height: 56px; border-radius: 15px; padding: 0 16px; }
  .input-shell input { font-size: 16px; }
  .form-row, .forgot-link, .register-line { font-size: 13px; }
  .login-button { height: 56px; border-radius: 15px; font-size: 19px; }
  .feature-item { gap: 7px; }
  .feature-icon { width: 38px; height: 38px; flex-basis: 38px; font-size: 16px; }
  .feature-copy strong { font-size: 12px; }
  .feature-copy small { font-size: 9px; }
}

/* Final responsive polish. Keep the Chinese typography neutral and legible across Windows, macOS and Android. */
.login-page,
.login-page button,
.login-page input {
  font-family: "PingFang SC", "Microsoft YaHei", "Noto Sans CJK SC", "Source Han Sans SC", system-ui, sans-serif;
}

.hero-side::before {
  content: '';
  position: absolute;
  z-index: -1;
  width: 820px;
  height: 820px;
  left: -340px;
  top: 50%;
  border-radius: 50%;
  background: rgba(255, 255, 255, .57);
  transform: translateY(-50%);
  filter: blur(1px);
}

.hero-copy h1,
.panel-heading h2,
.feature-copy strong {
  font-family: "PingFang SC", "Microsoft YaHei", "Noto Sans CJK SC", "Source Han Sans SC", system-ui, sans-serif;
}

.hero-copy h1 { font-weight: 800; }
.hero-description { font-weight: 400; }
.brand-name { font-weight: 800; }
.campus-doodle { display: none; }

@media (min-width: 881px) {
  .background-layer img { object-position: center center; }
  .reference-background-ready .background-tint { background: rgba(255,255,255,.02); }
  .reference-background-ready .hero-copy { visibility: hidden; }
  .reference-background-ready .login-footer { visibility: hidden; }
  .reference-background-ready .hero-side::before { display: none; }
  .hero-shell { padding-bottom: 12px; }
  .hero-copy { transform: translateY(-14px); }
  .hero-copy h1 { font-size: 58px; line-height: 1.16; }
  .login-panel { min-height: 606px; }
}

@media (max-width: 880px) {
  .login-page {
    min-height: 100%;
    overflow-x: hidden;
    overflow-y: auto;
    background: #fff;
  }

  .background-layer {
    inset: 0 0 auto;
    height: 272px;
    overflow: hidden;
  }

  .background-layer img {
    height: 272px;
    object-fit: cover;
    object-position: 48% center;
    filter: saturate(1.02) brightness(1.06);
    animation: none;
  }

  .background-tint {
    inset: 0 0 auto;
    height: 312px;
    background: linear-gradient(180deg, rgba(var(--skin-surface-rgb, 232, 247, 255), 0.08) 0%, rgba(var(--skin-surface-rgb, 240, 250, 255), 0.1) 16%, rgba(255,255,255,.57) 54%, #fff 100%);
  }

  .background-layer picture { height: 272px; }
  .reference-background-ready .brand-header { height: 0; padding: 0; visibility: hidden; overflow: hidden; }
  .reference-background-ready .hero-copy { visibility: hidden; }
  .reference-background-ready .hero-side { min-height: 272px; }

  .brand-header { position: relative; z-index: 3; }
  .hero-shell { position: relative; z-index: 2; }
  .hero-side::before { display: none; }
  .hero-copy h1 { color: var(--skin-ink, #172333); font-size: 40px; font-weight: 800; text-shadow: 0 1px 0 rgba(255,255,255,.8); }
  .hero-copy h1 span { color: var(--skin-brand, #087cf0); }
  .hero-description { color: var(--skin-muted, #506a82); font-weight: 400; }
  .login-panel { box-shadow: 0 13px 35px rgba(var(--skin-ink-rgb, 57, 96, 125), 0.14); }
}

@media (max-width: 560px) {
  .brand-header { padding-top: 2px; }
  .hero-shell { padding-top: 20px; }
  .hero-copy h1 { font-size: 32px; line-height: 1.2; }
  .hero-description { margin-top: 10px; font-size: 14px; }
  .hero-line { margin-top: 11px; }
  .login-panel { padding: 28px 20px 25px; }
  .panel-heading { min-height: 65px; }
  .panel-heading h2 { font-size: 26px; }
  .panel-heading-copy > p { margin-top: 8px; font-size: 14px; }
  .role-tabs { margin-bottom: 23px; }
  .role-tabs button { height: 43px; font-size: 14px; }
  .field { margin-bottom: 14px; }
  .input-shell { height: 55px; border-radius: 15px; }
  .form-row { margin-bottom: 23px; }
  .login-button { height: 56px; }
  .register-line { margin-top: 18px; }
}

@media (max-width: 370px) {
  .brand-header, .hero-shell, .login-footer { width: calc(100% - 24px); }
  .brand-name { font-size: 18px; }
  .hero-copy h1 { font-size: 29px; }
  .login-panel { padding-left: 16px; padding-right: 16px; }
  .feature-copy small { display: none; }
}

/* The supplied reference artwork already contains the brand, headline and footer art.
   Keep the functional form on top, but do not duplicate those visual elements. */
.brand-header,
.hero-copy,
.login-footer {
  visibility: hidden;
  pointer-events: none;
}

.background-layer img {
  filter: saturate(1.08) contrast(1.045) brightness(1.025);
}

.background-tint {
  background: rgba(255, 255, 255, .055);
}

.login-panel {
  border: 1px solid rgba(255, 255, 255, .82);
  background: linear-gradient(145deg, rgba(255, 255, 255, .91), rgba(var(--skin-surface-rgb, 247, 252, 255), 0.78));
  box-shadow: 0 24px 64px rgba(var(--skin-brand-strong-rgb, 40, 88, 126), 0.18), 0 2px 10px rgba(255, 255, 255, .34) inset;
  backdrop-filter: blur(20px) saturate(1.16);
  -webkit-backdrop-filter: blur(20px) saturate(1.16);
}

.login-panel:hover {
  border-color: rgba(255, 255, 255, .95);
  box-shadow: 0 28px 70px rgba(var(--skin-brand-strong-rgb, 40, 88, 126), 0.22), 0 2px 12px rgba(255, 255, 255, .4) inset;
}

@media (min-width: 881px) {
  .reference-background-ready .background-tint { background: rgba(255, 255, 255, .055); }
  .reference-background-ready .brand-header,
  .reference-background-ready .hero-copy,
  .reference-background-ready .login-footer {
    visibility: hidden;
  }
}

@media (max-width: 880px) {
  .brand-header {
    height: 0;
    padding: 0;
    overflow: hidden;
  }

  .background-layer img {
    filter: saturate(1.06) contrast(1.035) brightness(1.035);
  }

  .background-tint {
    background: linear-gradient(180deg, rgba(var(--skin-surface-rgb, 239, 249, 255), 0.08) 0%, rgba(255, 255, 255, .52) 54%, rgba(255, 255, 255, .96) 100%);
  }

  .hero-side {
    min-height: 272px;
  }

  .login-panel {
    background: linear-gradient(145deg, rgba(255, 255, 255, .92), rgba(var(--skin-surface-rgb, 247, 252, 255), 0.84));
    box-shadow: 0 17px 42px rgba(var(--skin-brand-strong-rgb, 43, 94, 128), 0.18), 0 2px 10px rgba(255, 255, 255, .4) inset;
  }
}

/* Login is intentionally presented as a mobile portrait experience at every viewport. */
.login-page {
  --reference-visual-height: min(343px, 61.25vw);
  width: min(100%, 560px);
  min-height: 100dvh;
  margin: 0 auto;
  overflow-x: hidden;
  overflow-y: auto;
  background: #fff;
}

.brand-header {
  width: 100%;
  height: 0;
  padding: 0;
  overflow: hidden;
}

.background-layer {
  inset: 0 0 auto;
  height: var(--reference-visual-height);
  overflow: hidden;
}

.background-layer picture {
  width: 100%;
  height: var(--reference-visual-height);
}

.background-layer img {
  width: 100%;
  height: auto;
  min-height: 0;
  margin-top: clamp(-42px, -7.2vw, -27px);
  object-fit: initial;
  object-position: top center;
  filter: saturate(1.06) contrast(1.035) brightness(1.035);
  animation: none;
}

.background-tint {
  inset: 0 0 auto;
  height: calc(var(--reference-visual-height) + 40px);
  background: linear-gradient(180deg, rgba(var(--skin-surface-rgb, 239, 249, 255), 0.05) 0%, rgba(255, 255, 255, .2) 60%, rgba(255, 255, 255, .92) 100%);
}

.hero-shell {
  width: calc(100% - 36px);
  max-width: 520px;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 18px;
  padding: 0 0 24px;
  margin: 0 auto;
}

.hero-side {
  display: block;
  min-height: var(--reference-visual-height);
  padding: 0;
}

.hero-copy,
.login-footer {
  visibility: hidden;
}

.login-panel {
  width: 100%;
  min-width: 0;
  min-height: 0;
  padding: 28px 28px 25px;
  border-radius: 22px;
  background: linear-gradient(145deg, rgba(255, 255, 255, .92), rgba(var(--skin-surface-rgb, 247, 252, 255), 0.84));
  box-shadow: 0 17px 42px rgba(var(--skin-brand-strong-rgb, 43, 94, 128), 0.18), 0 2px 10px rgba(255, 255, 255, .4) inset;
}

.panel-heading { min-height: 68px; }
.panel-heading h2 { font-size: 28px; }
.panel-heading-copy > p { font-size: 14px; }
.role-tabs { margin-bottom: 23px; }
.role-tabs button { height: 43px; font-size: 14px; }
.input-shell { height: 55px; border-radius: 15px; }
.input-shell input { font-size: 16px; }
.form-row { margin-bottom: 23px; }
.login-button { height: 56px; border-radius: 15px; font-size: 19px; }
.register-line { margin-top: 18px; }

.success-feedback {
  position: fixed;
  z-index: 20;
  top: 50%;
  left: 50%;
  width: min(420px, calc(100% - 40px));
  min-height: 154px;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 25px 34px;
  border: 1px solid rgba(255, 255, 255, .78);
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(var(--skin-surface-rgb, 249, 253, 255), 0.93), rgba(var(--skin-surface-rgb, 231, 245, 255), 0.82));
  box-shadow: 0 24px 56px rgba(var(--skin-brand-rgb, 53, 116, 162), 0.2), 0 2px 12px rgba(255, 255, 255, .6) inset;
  backdrop-filter: blur(18px) saturate(1.12);
  -webkit-backdrop-filter: blur(18px) saturate(1.12);
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.success-feedback-icon {
  width: 76px;
  height: 76px;
  display: grid;
  place-items: center;
  flex: 0 0 76px;
  border: 10px solid rgba(var(--skin-brand-soft-rgb, 122, 197, 246), 0.2);
  border-radius: 50%;
  background: linear-gradient(145deg, var(--skin-brand, #27a6ff), var(--skin-brand, #087cf0));
  color: #fff;
  font-size: 30px;
  box-shadow: 0 8px 20px rgba(var(--skin-brand-rgb, 8, 124, 240), 0.2);
}

.success-feedback-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.success-feedback-copy strong {
  color: var(--skin-brand-strong, #172b43);
  font-size: 27px;
  line-height: 1.15;
  font-weight: 800;
}

.success-feedback-copy small {
  color: var(--skin-muted, #6685a4);
  font-size: 14px;
  line-height: 1.45;
}

.success-feedback-enter-active,
.success-feedback-leave-active {
  transition: opacity 180ms ease, transform 220ms cubic-bezier(.22, .61, .36, 1);
}

.success-feedback-enter-from,
.success-feedback-leave-to {
  opacity: 0;
  transform: translate(-50%, calc(-50% + 10px)) scale(.96);
}

@media (max-width: 370px) {
  .hero-shell { width: calc(100% - 24px); }
  .login-panel { padding-left: 16px; padding-right: 16px; }
  .panel-heading h2 { font-size: 25px; }
  .form-row, .forgot-link, .register-line { font-size: 13px; }
}

@media (max-width: 420px) {
  .success-feedback {
    width: calc(100% - 32px);
    min-height: 126px;
    gap: 16px;
    padding: 20px 21px;
    border-radius: 20px;
  }

  .success-feedback-icon {
    width: 62px;
    height: 62px;
    flex-basis: 62px;
    border-width: 8px;
    font-size: 24px;
  }

  .success-feedback-copy { gap: 6px; }
  .success-feedback-copy strong { font-size: 23px; }
  .success-feedback-copy small { font-size: 12px; }
}

@media (prefers-reduced-motion: reduce) {
  .success-feedback-enter-active,
  .success-feedback-leave-active {
    transition: none;
  }
}
</style>
