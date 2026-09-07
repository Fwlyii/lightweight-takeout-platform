<template>
  <main class="session-page">
    <p class="preview-note">账号功能联调 · 其他业务页面将在后续接入</p>
    <section>
      <h1>当前账号</h1>
      <p v-if="loading">正在读取账号信息…</p>
      <p v-else-if="error" role="alert">{{ error }}</p>
      <template v-else-if="profile">
        <img :src="avatar" alt="账号头像" width="72" height="72">
        <dl>
          <dt>用户名</dt><dd>{{ profile.username }}</dd>
          <dt>手机号</dt><dd>{{ profile.phone || '未填写' }}</dd>
          <dt>登录端</dt><dd>{{ role.label }}端</dd>
          <dt>账号身份</dt><dd>{{ accountRole?.label || '未配置' }}</dd>
        </dl>
      </template>
      <button type="button" @click="logout">退出登录</button>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';
import { clearAuth, getAuthRole } from '../utils/auth';
import { getAccountRole, getRoleDefinition } from '../utils/roles';
import { DEFAULT_AVATAR_URL } from '../utils/profileDefaults';
import { apiBaseUrl } from '../utils/endpoints';

const router = useRouter();
const role = getRoleDefinition(getAuthRole());
const profile = ref(null);
const loading = ref(true);
const error = ref('');
const accountRole = computed(() => profile.value ? getRoleDefinition(getAccountRole(profile.value)) : null);
const avatar = computed(() => profile.value?.photo?.startsWith('/uploads/')
  ? new URL(profile.value.photo, apiBaseUrl).href : DEFAULT_AVATAR_URL);
onMounted(async () => {
  try { profile.value = await request.get('/api/user'); }
  catch (failure) { error.value = failure.response?.data?.message || '读取失败，请重试'; }
  finally { loading.value = false; }
});
function logout() {
  clearAuth();
  router.replace({ path: '/login', query: { role: role.key } });
}
</script>

<style scoped>
.session-page { max-width: 440px; padding: 24px 14px; margin: auto; color: #263442; }
.preview-note { color: #607182; font-size: 13px; line-height: 1.6; }
section { background: #fff; border-radius: 10px; padding: 24px; }
h1 { font-size: 22px; }
img { border-radius: 50%; object-fit: cover; }
dl { display: grid; grid-template-columns: 80px minmax(0, 1fr); gap: 14px; }
dt { color: #607182; } dd { margin: 0; overflow-wrap: anywhere; }
button { width: 100%; margin-top: 20px; padding: 12px; background: #168bd2; color: white; border: 0; border-radius: 6px; }
</style>
