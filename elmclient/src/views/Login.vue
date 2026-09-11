<template>
  <main class="login-page">
    <header class="brand-header">
      <router-link class="brand-link" to="/index" aria-label="返回首页">
        <span class="brand-mark" aria-hidden="true"><i class="fa fa-cutlery"></i></span>
        <span class="brand-name">饿了么</span>
        <span class="brand-divider" aria-hidden="true"></span>
        <span class="brand-product">校园外卖</span>
      </router-link>
      <router-link class="home-link" to="/index">返回首页</router-link>
    </header>

    <section class="hero-shell" aria-label="账号登录">
      <div class="hero-copy" aria-hidden="true">
        <p class="hero-kicker">ELEME CAMPUS</p>
        <h1>想吃什么，<br><span>现在就点。</span></h1>
        <p class="hero-description">附近好店、便捷下单、即时配送。<br>登录后，继续你的这一餐。</p>
        <div class="hero-line"></div>
        <p class="hero-note">让校园里的每一餐，更简单一点。</p>
      </div>

      <form
        class="login-panel"
        :class="{ 'error-shake': errorPulse, 'success-state': loginSucceeded }"
        @submit.prevent="login"
      >
        <div class="panel-heading">
          <p class="panel-kicker">账号登录</p>
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
            <span>记住我</span>
          </label>
          <span class="identity-note">{{ activeRole.label }}端</span>
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
          <i v-else-if="loginSucceeded" class="fa fa-check"></i>
          <span>{{ loginButtonText }}</span>
        </button>

        <transition name="hint-fade" mode="out-in">
          <p v-if="selectedRole === 'user'" key="register" class="register-line">
            还没有账号？<router-link to="/register">立即注册</router-link>
          </p>
          <p v-else key="role-note" class="register-line muted">请使用对应身份账号登录</p>
        </transition>
      </form>
    </section>

    <footer class="login-footer">ELEME · 校园外卖服务平台</footer>
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
const queryRole = typeof route.query.role === 'string' && roleMap[route.query.role]
  ? route.query.role
  : 'user';

const selectedRole = ref(queryRole);
const userName = ref(localStorage.getItem('savedUserName') || '');
const password = ref('');
const showPassword = ref(false);
const rememberMe = ref(false);
const submitting = ref(false);
const loginSucceeded = ref(false);
const inlineError = ref('');
const errorPulse = ref(false);

const activeRole = computed(() => roleMap[selectedRole.value]);
const activeRoleIndex = computed(() => Math.max(0, roleOptions.findIndex(item => item.key === selectedRole.value)));
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

const selectRole = (key) => {
  if (submitting.value || key === selectedRole.value) return;
  selectedRole.value = key;
  inlineError.value = '';
  router.replace({ query: { ...route.query, role: key } });
};

const triggerError = (message) => {
  inlineError.value = message;
  errorPulse.value = false;
  requestAnimationFrame(() => {
    errorPulse.value = true;
    window.setTimeout(() => { errorPulse.value = false; }, 320);
  });
};

const waitForSuccessFeedback = () => new Promise(resolve => setTimeout(resolve, 280));

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
  color: #173b60;
  background: #f8fbfe;
}

.login-page::before {
  content: '';
  position: absolute;
  width: 720px;
  height: 720px;
  left: -250px;
  bottom: -330px;
  border-radius: 50%;
  background: linear-gradient(145deg, rgba(0, 151, 255, .16), rgba(72, 183, 255, .03));
  pointer-events: none;
  animation: ambient-left 13s ease-in-out infinite alternate;
  transform-origin: 52% 48%;
}

.login-page::after {
  content: '';
  position: absolute;
  width: 420px;
  height: 420px;
  right: -190px;
  top: -230px;
  border-radius: 50%;
  background: rgba(0, 151, 255, .055);
  pointer-events: none;
  animation: ambient-right 16s ease-in-out infinite alternate;
  transform-origin: 42% 56%;
}

.brand-header {
  position: relative;
  z-index: 2;
  width: min(1180px, calc(100% - 64px));
  height: 92px;
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
  background: #0097ff;
  font-size: 17px;
  box-shadow: 0 8px 22px rgba(0, 151, 255, .16);
  transition: transform .22s ease, box-shadow .22s ease;
}

.brand-link:hover .brand-mark {
  transform: translateY(-1px) rotate(-2deg);
  box-shadow: 0 11px 25px rgba(0, 151, 255, .21);
}

.brand-name {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: .02em;
  color: #173b60;
}

.brand-divider {
  width: 1px;
  height: 18px;
  margin: 0 12px;
  background: #d7e4ef;
}

.brand-product {
  color: #7c94a8;
  font-size: 13px;
}

.home-link {
  position: relative;
  color: #70899f;
  font-size: 13px;
  text-decoration: none;
  transition: color .18s ease;
}

.home-link::after {
  content: '';
  position: absolute;
  left: 0;
  right: 100%;
  bottom: -5px;
  height: 1px;
  background: #0097ff;
  transition: right .2s ease;
}

.home-link:hover { color: #0097ff; }
.home-link:hover::after { right: 0; }

.hero-shell {
  position: relative;
  z-index: 1;
  width: min(1120px, calc(100% - 64px));
  margin: auto;
  padding: 38px 0 68px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 430px;
  align-items: center;
  gap: 92px;
}

.hero-copy {
  padding-left: 26px;
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
  margin: 0 0 17px;
  color: #4e91c5;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .22em;
}

.hero-copy h1 {
  margin: 0;
  color: #173b60;
  font-size: clamp(50px, 5vw, 70px);
  line-height: 1.06;
  letter-spacing: -.05em;
  font-weight: 800;
}

.hero-copy h1 span { color: #0097ff; }

.hero-description {
  margin: 28px 0 0;
  color: #748da3;
  font-size: 15px;
  line-height: 1.85;
}

.hero-line {
  width: 46px;
  height: 3px;
  margin-top: 31px;
  border-radius: 999px;
  background: #0097ff;
}

.hero-note {
  margin: 13px 0 0;
  color: #9aaab7;
  font-size: 12px;
}

.login-panel {
  width: 100%;
  padding: 36px 40px 32px;
  border: 1px solid #e1eaf1;
  border-radius: 18px;
  background: rgba(255, 255, 255, .97);
  box-shadow: 0 18px 48px rgba(50, 83, 112, .08);
  animation: panel-rise .58s cubic-bezier(.22, .61, .36, 1) .17s both;
  transition: border-color .22s ease, box-shadow .22s ease;
}

.login-panel:hover {
  border-color: #d7e7f2;
  box-shadow: 0 21px 52px rgba(50, 83, 112, .095);
}

.login-panel.success-state {
  border-color: rgba(32, 165, 107, .28);
}

.panel-heading { min-height: 78px; margin-bottom: 22px; }

.panel-kicker {
  margin: 0 0 7px;
  color: #5c8db4;
  font-size: 11px;
  font-weight: 600;
}

.panel-heading h2 {
  margin: 0;
  color: #173b60;
  font-size: 28px;
  line-height: 1.25;
  letter-spacing: -.03em;
}

.panel-heading-copy > p {
  margin: 8px 0 0;
  color: #8a9dac;
  font-size: 13px;
}

.role-heading-enter-active,
.role-heading-leave-active {
  transition: opacity .16s ease, transform .16s ease;
}

.role-heading-enter-from {
  opacity: 0;
  transform: translateY(5px);
}

.role-heading-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.role-tabs {
  --role-index: 0;
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin-bottom: 25px;
  border-bottom: 1px solid #e8eff4;
}

.role-tabs button {
  position: relative;
  z-index: 1;
  height: 42px;
  padding: 0 4px;
  border: 0;
  background: transparent;
  color: #8297aa;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: color .2s ease, transform .18s ease;
}

.role-tabs button:hover:not(:disabled) {
  color: #267ebc;
  transform: translateY(-1px);
}

.role-tabs button.active { color: #008de9; font-weight: 600; }
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
  width: 62%;
  height: 100%;
  margin: 0 auto;
  border-radius: 999px;
  background: #0097ff;
  box-shadow: 0 1px 5px rgba(0, 151, 255, .18);
}

.redirect-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: -10px 0 20px;
  color: #6f90a9;
  font-size: 11px;
}

.redirect-hint i { color: #0097ff; }

.field {
  display: block;
  margin-bottom: 18px;
}

.field > span {
  display: block;
  margin-bottom: 8px;
  color: #425f78;
  font-size: 12px;
  font-weight: 600;
}

.input-shell {
  height: 50px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  border: 1px solid #dbe7ef;
  border-radius: 11px;
  background: #fbfdff;
  transition: border-color .18s ease, box-shadow .18s ease, background .18s ease, transform .18s ease;
}

.input-shell:hover {
  border-color: #cddfea;
}

.input-shell:focus-within {
  border-color: #62b9f1;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(0, 151, 255, .07);
  transform: translateY(-1px);
}

.input-shell > i {
  width: 16px;
  flex: 0 0 16px;
  text-align: center;
  color: #a1b3c1;
  font-size: 13px;
  transition: color .18s ease;
}

.input-shell:focus-within > i { color: #0097ff; }

.input-shell input {
  min-width: 0;
  flex: 1;
  border: 0;
  outline: 0;
  background: transparent;
  color: #294861;
  font: inherit;
  font-size: 14px;
}

.input-shell input::placeholder { color: #b1c0cc; }

.password-toggle {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  flex: 0 0 32px;
  border: 0;
  background: transparent;
  color: #8ea4b6;
  cursor: pointer;
  transition: color .18s ease, transform .18s ease;
}

.password-toggle:hover { color: #0097ff; transform: scale(1.06); }

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 20px;
  color: #7f95a7;
  font-size: 12px;
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

.identity-note { color: #a0afba; }

.inline-error {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  margin: -5px 0 14px;
  padding: 10px 12px;
  border-radius: 9px;
  background: #fff4f2;
  color: #c04c42;
  font-size: 12px;
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
.hint-fade-enter-from {
  opacity: 0;
  transform: translateY(-4px);
}

.error-fade-leave-to,
.hint-fade-leave-to {
  opacity: 0;
  transform: translateY(3px);
}

.login-button {
  width: 100%;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-radius: 11px;
  background: #0097ff;
  color: #fff;
  font: inherit;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 9px 22px rgba(0, 151, 255, .17);
  transition: transform .18s ease, box-shadow .18s ease, background .18s ease;
}

.login-button:hover:not(:disabled) {
  background: #008ae8;
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(0, 151, 255, .22);
}

.login-button:active:not(:disabled) { transform: translateY(1px) scale(.995); }
.login-button:disabled { cursor: wait; opacity: .72; }
.login-button.success { background: #20a56b; box-shadow: 0 9px 22px rgba(32, 165, 107, .18); }

.register-line {
  margin: 18px 0 0;
  text-align: center;
  color: #879aa9;
  font-size: 12px;
}

.register-line a {
  margin-left: 5px;
  color: #0097ff;
  font-weight: 600;
  text-decoration: none;
}

.register-line a:hover { text-decoration: underline; }
.register-line.muted { color: #a0afba; }

.login-footer {
  position: relative;
  z-index: 1;
  padding: 0 20px 24px;
  text-align: center;
  color: #b1bec8;
  font-size: 10px;
  letter-spacing: .08em;
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
  from { opacity: 0; transform: translateY(16px) scale(.992); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes footer-fade {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes ambient-left {
  from { transform: translate3d(0, 0, 0) scale(1); }
  to { transform: translate3d(12px, -7px, 0) scale(1.025); }
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

@media (max-width: 880px) {
  .brand-header {
    width: min(100% - 36px, 640px);
    height: 78px;
  }

  .brand-product,
  .brand-divider,
  .home-link { display: none; }

  .hero-shell {
    width: min(100% - 36px, 460px);
    grid-template-columns: 1fr;
    gap: 28px;
    padding: 16px 0 46px;
  }

  .hero-copy {
    padding: 4px 2px 0;
    text-align: center;
  }

  .hero-kicker,
  .hero-line,
  .hero-note { display: none; }

  .hero-copy h1 {
    font-size: 36px;
    line-height: 1.12;
    letter-spacing: -.045em;
  }

  .hero-copy h1 br { display: none; }
  .hero-copy h1 span::before { content: ' '; }

  .hero-description {
    margin-top: 12px;
    font-size: 13px;
    line-height: 1.7;
  }

  .hero-description br { display: none; }

  .login-panel {
    padding: 30px 26px 28px;
    border-radius: 17px;
  }
}

@media (max-width: 420px) {
  .brand-header,
  .hero-shell { width: calc(100% - 28px); }

  .brand-header { height: 70px; }
  .brand-mark { width: 38px; height: 38px; border-radius: 11px; }
  .brand-name { font-size: 18px; }

  .hero-shell { padding-top: 4px; gap: 22px; }
  .hero-copy h1 { font-size: 31px; }
  .hero-description { font-size: 12px; }

  .login-panel { padding: 26px 20px 24px; }
  .panel-heading h2 { font-size: 25px; }

  .role-tabs button { height: 40px; font-size: 12px; }
  .input-shell { height: 48px; }
  .login-button { height: 48px; }
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    scroll-behavior: auto !important;
    transition: none !important;
    animation: none !important;
  }
}
</style>
