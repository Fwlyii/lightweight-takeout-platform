<template>
  <main class="login-page">
    <div class="ambient ambient-left" aria-hidden="true"></div>
    <div class="ambient ambient-right" aria-hidden="true"></div>

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
      <div class="hero-side" aria-hidden="true">
        <div class="hero-copy">
          <p class="hero-kicker">ELEME CAMPUS</p>
          <h1>想吃什么，<br><span>现在就点。</span></h1>
          <p class="hero-description">附近好店、便捷下单、即时配送。<br>登录后，继续你的这一餐。</p>
          <div class="hero-line"></div>
          <p class="hero-note">让校园里的每一餐，更简单一点。</p>
        </div>

        <div class="food-visual">
          <img src="/images/login-food-bg.webp?v=20260912-1" alt="" />
        </div>
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
            <span class="checkbox-ui" aria-hidden="true"><i class="fa fa-check"></i></span>
            <span>记住我</span>
          </label>
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

        <transition name="hint-fade">
          <p v-if="selectedRole === 'user'" class="register-line">
            还没有账号？<router-link to="/register">立即注册</router-link>
          </p>
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
  background: linear-gradient(135deg, #fbfdff 0%, #f7fbfe 48%, #fafdff 100%);
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
  background: rgba(0, 151, 255, .07);
  animation: ambient-left 14s ease-in-out infinite alternate;
}

.ambient-right {
  width: 420px;
  height: 420px;
  right: -190px;
  top: -230px;
  background: rgba(0, 151, 255, .055);
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
  color: #173b60;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: .02em;
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
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #70899f;
  font-size: 13px;
  text-decoration: none;
  transition: color .18s ease, transform .18s ease;
}

.home-link i {
  font-size: 11px;
  transition: transform .18s ease;
}

.home-link:hover {
  color: #0097ff;
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
  color: #4e91c5;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .17em;
}

.hero-copy h1 {
  margin: 0;
  color: #173b60;
  font-size: clamp(48px, 4.5vw, 64px);
  line-height: 1.045;
  letter-spacing: -.048em;
  font-weight: 800;
}

.hero-copy h1 span { color: #0097ff; }

.hero-description {
  margin: 23px 0 0;
  color: #748da3;
  font-size: 14px;
  line-height: 1.8;
}

.hero-line {
  width: 42px;
  height: 3px;
  margin-top: 25px;
  border-radius: 999px;
  background: #0097ff;
}

.hero-note {
  margin: 11px 0 0;
  color: #9aaab7;
  font-size: 11px;
}

.food-visual {
  position: absolute;
  z-index: 1;
  left: -34px;
  bottom: -22px;
  width: min(590px, 104%);
  height: 292px;
  opacity: .78;
  pointer-events: none;
  animation: food-float 11s ease-in-out infinite alternate;
  -webkit-mask-image: radial-gradient(ellipse 72% 74% at 44% 58%, #000 0%, #000 47%, rgba(0,0,0,.82) 62%, rgba(0,0,0,.25) 78%, transparent 92%);
  mask-image: radial-gradient(ellipse 72% 74% at 44% 58%, #000 0%, #000 47%, rgba(0,0,0,.82) 62%, rgba(0,0,0,.25) 78%, transparent 92%);
}

.food-visual img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  object-position: left 58%;
  filter: saturate(.9) contrast(.97) brightness(1.025);
  border-radius: 34px;
}

.login-panel {
  width: 100%;
  padding: 31px 34px 27px;
  border: 1px solid rgba(207, 223, 235, .88);
  border-radius: 18px;
  background: rgba(255, 255, 255, .965);
  box-shadow: 0 18px 46px rgba(50, 83, 112, .075);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  animation: panel-rise .58s cubic-bezier(.22, .61, .36, 1) .17s both;
  transition: border-color .22s ease, box-shadow .22s ease, transform .22s ease;
}

.login-panel:hover {
  border-color: #d3e4ef;
  box-shadow: 0 20px 50px rgba(50, 83, 112, .09);
}

.login-panel.success-state { border-color: rgba(32, 165, 107, .28); }

.panel-heading {
  min-height: 70px;
  margin-bottom: 17px;
}

.panel-kicker {
  margin: 0 0 6px;
  color: #5c8db4;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: .02em;
}

.panel-heading h2 {
  margin: 0;
  color: #173b60;
  font-size: 27px;
  line-height: 1.22;
  letter-spacing: -.028em;
}

.panel-heading-copy > p {
  margin: 7px 0 0;
  color: #8a9dac;
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
  border-bottom: 1px solid #e8eff4;
}

.role-tabs button {
  position: relative;
  z-index: 1;
  height: 40px;
  padding: 0 4px;
  border: 0;
  background: transparent;
  color: #8297aa;
  font: inherit;
  font-size: 12px;
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
  width: 58%;
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
  margin: -8px 0 17px;
  color: #6f90a9;
  font-size: 10px;
}

.redirect-hint i { color: #0097ff; }

.field {
  display: block;
  margin-bottom: 15px;
}

.field > span {
  display: block;
  margin-bottom: 7px;
  color: #425f78;
  font-size: 12px;
  font-weight: 600;
}

.input-shell {
  height: 48px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  border: 1px solid #dbe7ef;
  border-radius: 11px;
  background: #fbfdff;
  transition: border-color .18s ease, box-shadow .18s ease, background .18s ease, transform .18s ease;
}

.input-shell:hover { border-color: #cddfea; }

.input-shell:focus-within {
  border-color: #62b9f1;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(0, 151, 255, .065);
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
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  flex: 0 0 30px;
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
  margin: 1px 0 18px;
  color: #7f95a7;
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
  border: 1px solid #c7d7e3;
  border-radius: 5px;
  background: #fff;
  color: transparent;
  font-size: 9px;
  transition: background .18s ease, border-color .18s ease, box-shadow .18s ease, transform .18s ease;
}

.remember-option:hover .checkbox-ui {
  border-color: #8dcdf7;
  transform: translateY(-1px);
}

.remember-option input:checked + .checkbox-ui {
  border-color: #0097ff;
  background: #0097ff;
  color: #fff;
  box-shadow: 0 3px 8px rgba(0, 151, 255, .18);
}

.remember-option input:focus-visible + .checkbox-ui {
  box-shadow: 0 0 0 3px rgba(0, 151, 255, .12);
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
  background: #0097ff;
  color: #fff;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 9px 22px rgba(0, 151, 255, .16);
  transition: transform .18s ease, box-shadow .18s ease, background .18s ease;
}

.login-button:hover:not(:disabled) {
  background: #008ae8;
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(0, 151, 255, .21);
}

.login-button:active:not(:disabled) { transform: translateY(1px) scale(.995); }
.login-button:disabled { cursor: wait; opacity: .72; }
.login-button.success { background: #20a56b; box-shadow: 0 9px 22px rgba(32, 165, 107, .18); }

.register-line {
  margin: 16px 0 0;
  text-align: center;
  color: #879aa9;
  font-size: 11px;
}

.register-line a {
  margin-left: 5px;
  color: #0097ff;
  font-weight: 600;
  text-decoration: none;
}

.register-line a:hover { text-decoration: underline; }

.login-footer {
  position: relative;
  z-index: 2;
  padding: 0 20px 20px;
  text-align: center;
  color: #bfccd5;
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

  .food-visual { width: 520px; opacity: .68; }
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
  .hero-note,
  .food-visual { display: none; }

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
</style>