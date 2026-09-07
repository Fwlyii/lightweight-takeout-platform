<template>
  <main class="registration-page">
    <header class="registration-header">
      <router-link :to="loginRoute" aria-label="返回登录">‹</router-link>
      <h1>注册账号</h1>
      <span aria-hidden="true"></span>
    </header>

    <form class="registration-form" @submit.prevent="register">
      <fieldset :disabled="submitting">
        <div class="avatar-field">
          <img :src="previewUrl || DEFAULT_AVATAR_URL" alt="头像预览" width="72" height="72">
          <label for="avatar">上传头像（选填）</label>
          <input id="avatar" type="file" accept="image/jpeg,image/png,image/webp" @change="selectAvatar">
          <small>支持 JPG、PNG、WebP，大小不超过 5MB</small>
        </div>

        <label for="username">用户名</label>
        <input id="username" v-model.trim="form.username" name="username" autocomplete="username"
               required maxlength="20" placeholder="请输入用户名">

        <label for="phone">手机号</label>
        <input id="phone" v-model.trim="form.phone" name="phone" type="tel" autocomplete="tel"
               required pattern="1[3-9][0-9]{9}" maxlength="11" placeholder="请输入11位手机号">

        <label for="password">密码</label>
        <input id="password" v-model="form.password" name="password" type="password" autocomplete="new-password"
               required minlength="8" maxlength="32" aria-describedby="password-hint">
        <small id="password-hint">8–32位，包含字母和数字</small>

        <label for="confirm-password">确认密码</label>
        <input id="confirm-password" v-model="confirmation" type="password" autocomplete="new-password"
               required minlength="8" maxlength="32" placeholder="请再次输入密码">

        <label for="email">邮箱（选填）</label>
        <input id="email" v-model.trim="form.email" name="email" type="email" autocomplete="email" maxlength="255">

        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <p v-if="completed" class="form-success" role="status">注册成功，请登录</p>
        <button v-if="!completed" class="submit-button" type="submit">{{ submitting ? '注册中…' : '注册' }}</button>
        <router-link v-else class="submit-button" :to="loginRoute">去登录</router-link>
      </fieldset>
      <router-link v-if="!completed" class="login-link" :to="loginRoute">已有账号？去登录</router-link>
    </form>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import request from '../utils/request';
import { DEFAULT_AVATAR_URL } from '../utils/profileDefaults';

const route = useRoute();
const form = reactive({ username: '', phone: '', password: '', email: '' });
const confirmation = ref('');
const avatar = ref(null);
const previewUrl = ref('');
const error = ref('');
const submitting = ref(false);
const completed = ref(false);
const loginRoute = computed(() => ({
  path: '/login',
  query: { role: ['user', 'merchant', 'rider'].includes(route.query.role) ? route.query.role : 'user' }
}));

function releasePreview() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value);
  previewUrl.value = '';
}

function selectAvatar(event) {
  releasePreview();
  avatar.value = null;
  error.value = '';
  const file = event.target.files?.[0];
  if (!file) return;
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024) {
    error.value = '请选择5MB以内的 JPG、PNG 或 WebP 图片';
    event.target.value = '';
    return;
  }
  avatar.value = file;
  previewUrl.value = URL.createObjectURL(file);
}

async function register() {
  if (submitting.value || completed.value) return;
  error.value = '';
  if (!form.username || form.username.length > 20 || !/^1[3-9]\d{9}$/.test(form.phone)) {
    error.value = '请填写用户名和11位有效手机号';
    return;
  }
  if (form.password.length < 8 || form.password.length > 32 || !/[A-Za-z]/.test(form.password) || !/\d/.test(form.password)) {
    error.value = '密码需为8–32位，且同时包含字母和数字';
    return;
  }
  if (form.password !== confirmation.value) {
    error.value = '两次输入的密码不一致';
    return;
  }
  submitting.value = true;
  try {
    // 只发送注册需要的字段；权限、账号状态与默认头像由后端决定。
    const user = { username: form.username, phone: form.phone, password: form.password, email: form.email || null };
    let body = user;
    if (avatar.value) {
      body = new FormData();
      body.append('user', new Blob([JSON.stringify(user)], { type: 'application/json' }));
      body.append('avatar', avatar.value);
    }
    const response = await request.post('/api/register', body, { skipAuthRedirect: true });
    if (!response.success) {
      error.value = response.message || '注册失败，请重试';
      return;
    }
    completed.value = true;
    form.password = '';
    confirmation.value = '';
  } catch (failure) {
    error.value = failure.response?.data?.message || (failure.response ? '注册失败，请重试' : '网络连接失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}

onBeforeUnmount(releasePreview);
</script>

<style scoped>
.registration-page { min-height: 100dvh; background: #f5f7fa; color: #263442; padding-bottom: 32px; }
.registration-page * { box-sizing: border-box; }
.registration-header { display: grid; grid-template-columns: 44px minmax(0, 1fr) 44px; align-items: center; padding: 8px 12px; background: #fff; border-bottom: 1px solid #e4eaf0; }
.registration-header h1 { margin: 0; text-align: center; font-size: 18px; line-height: 44px; }
.registration-header a { text-align: center; font: 32px/44px Arial, sans-serif; color: #168bd2; text-decoration: none; }
.registration-form { width: calc(100% - 24px); max-width: 440px; margin: 20px auto 0; padding: 24px; background: #fff; border-radius: 10px; }
fieldset { min-width: 0; margin: 0; padding: 0; border: 0; }
label { display: block; margin: 18px 0 7px; font-size: 14px; }
input { display: block; width: 100%; min-width: 0; min-height: 44px; padding: 10px 12px; border: 1px solid #d9e1e8; border-radius: 6px; background: #fff; color: #263442; font: inherit; font-size: 16px; }
input:focus-visible { outline: 2px solid #168bd2; outline-offset: 2px; }
small { display: block; margin-top: 7px; color: #687786; font-size: 12px; line-height: 1.5; }
.avatar-field { text-align: center; }
.avatar-field img { border-radius: 50%; object-fit: cover; }
.avatar-field label { margin-top: 8px; }
.avatar-field input { font-size: 12px; padding: 9px; }
.submit-button { display: block; width: 100%; margin: 24px 0 0; padding: 13px; border: 0; border-radius: 6px; background: #168bd2; color: #fff; font: inherit; font-size: 16px; text-align: center; text-decoration: none; cursor: pointer; }
fieldset:disabled .submit-button { opacity: .6; cursor: wait; }
.login-link { display: block; margin-top: 18px; color: #168bd2; font-size: 14px; text-align: center; text-decoration: none; }
.form-error, .form-success { margin: 16px 0 0; font-size: 14px; line-height: 1.5; overflow-wrap: anywhere; }
.form-error { color: #c0392b; }
.form-success { color: #167bb5; }
@media (max-width: 360px) { .registration-form { padding: 18px; } }
</style>
