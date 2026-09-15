<template>
  <ProfilePage
    title="个人信息"
    :back-to="getRoleDefinition(getAuthRole()).target"
  >
    <p v-if="loading" role="status">正在加载个人信息…</p>
    <p
      v-if="message"
      class="notice"
      :class="{ error: failed }"
      :role="failed ? 'alert' : 'status'"
    >
      {{ message }}
    </p>
    <template v-if="profile">
      <div class="panel identity">
        <img
          :src="avatarUrl(profile.photo)"
          alt="头像"
          @error="avatarFallback"
        />
        <div>
          <h2>{{ profile.username }}</h2>
          <p class="muted">{{ profile.phone }}</p>
          <label class="avatar-upload">
            更换头像
            <input type="file" accept="image/jpeg,image/png,image/webp" :disabled="busy" @change="changeAvatar" />
          </label>
          <small class="muted">JPG / PNG / WebP，最大 5MB</small>
        </div>
      </div>
      <nav
        v-if="getAuthRole() === 'user'"
        class="panel personal-links"
        aria-label="个人服务"
      >
        <router-link to="/userAddress">收货地址 <span>›</span></router-link>
        <router-link to="/favorites">我的收藏 <span>›</span></router-link>
        <router-link to="/notifications">消息与通知 <span>›</span></router-link>
        <router-link to="/assets">钱包与优惠 <span>›</span></router-link>
        <router-link to="/preferences">个性化偏好与外观 <span>›</span></router-link>
      </nav>
      <nav v-else-if="getAuthRole() === 'rider'" class="panel personal-links" aria-label="骑手工具">
        <router-link to="/rider/dashboard?tab=active">配送中的订单 <span>›</span></router-link>
        <router-link to="/rider/dashboard?tab=history">历史配送 <span>›</span></router-link>
        <router-link to="/notifications?role=rider">消息与通知 <span>›</span></router-link>
      </nav>
      <form class="panel" @submit.prevent="save">
        <h2>基本资料</h2>
        <fieldset :disabled="busy">
          <div class="split">
            <label
              >姓<input
                v-model.trim="form.firstName"
                maxlength="40"
                autocomplete="family-name"
            /></label>
            <label
              >名<input
                v-model.trim="form.lastName"
                maxlength="40"
                autocomplete="given-name"
            /></label>
          </div>
          <label
            >手机号<input
              v-model.trim="form.phone"
              required
              pattern="1[3-9][0-9]{9}"
              maxlength="11"
              inputmode="tel"
              autocomplete="tel"
          /></label>
          <label
            >邮箱（选填）<input
              v-model.trim="form.email"
              type="email"
              maxlength="255"
              autocomplete="email"
          /></label>
          <label
            >性别<select v-model="form.gender">
              <option value="">不填写</option>
              <option>男</option>
              <option>女</option>
              <option>其他</option>
            </select></label
          >
          <button class="primary wide" type="submit">
            {{ busy ? "保存中…" : "保存资料" }}
          </button>
        </fieldset>
      </form>
    </template>
    <button v-if="failed && !profile" type="button" class="wide" @click="load">
      重新加载
    </button>
    <button type="button" class="wide" :disabled="busy" @click="logout">
      退出登录
    </button>
  </ProfilePage>
</template>
<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import ProfilePage from "../components/ProfilePage.vue";
import { getMyProfile, updateMyProfile, updateMyAvatar } from "../services/profileService";
import { clearAuth, getAuthRole } from "../utils/auth";
import { getRoleDefinition } from "../utils/roles";
import { apiBaseUrl } from "../utils/endpoints";
const avatarUrl = (value) =>
  value?.startsWith("/uploads/")
    ? apiBaseUrl + value
    : value || "/images/default-user-avatar.png";
const router = useRouter(),
  profile = ref(null),
  loading = ref(true),
  busy = ref(false),
  message = ref(""),
  failed = ref(false);
const form = reactive({
  firstName: "",
  lastName: "",
  phone: "",
  email: "",
  gender: "",
});
function fill(data) {
  profile.value = data;
  for (const key of Object.keys(form)) form[key] = data[key] || "";
}
async function load() {
  loading.value = true;
  message.value = "";
  failed.value = false;
  try {
    fill(await getMyProfile());
  } catch (e) {
    failed.value = true;
    message.value = e.response?.data?.message || "个人信息加载失败，请重试";
  } finally {
    loading.value = false;
  }
}
async function save() {
  if (busy.value) return;
  busy.value = true;
  failed.value = false;
  message.value = "";
  try {
    fill(await updateMyProfile({ ...form }));
    message.value = "资料已保存";
  } catch (e) {
    failed.value = true;
    message.value =
      e.response?.data?.message || e.message || "保存失败，请重试";
  } finally {
    busy.value = false;
  }
}
async function changeAvatar(event) {
  const file = event.target.files?.[0];
  if (!file || busy.value) return;
  event.target.value = '';
  failed.value = false;
  message.value = '';
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024) {
    failed.value = true;
    message.value = '请选择 5MB 以内的 JPG、PNG 或 WebP 图片';
    return;
  }
  busy.value = true;
  try { fill(await updateMyAvatar(file)); message.value = '头像已更新~'; }
  catch (e) { failed.value = true; message.value = e.response?.data?.message || '头像更新失败，请重试'; }
  finally { busy.value = false; }
}
function avatarFallback(event) {
  if (!event.target.dataset.fallback) {
    event.target.dataset.fallback = "true";
    event.target.src = "/images/default-user-avatar.png";
  }
}
function logout() {
  const role = getAuthRole();
  clearAuth();
  router.replace({ path: "/login", query: { role } });
}
onMounted(load);
</script>
<style scoped>
.avatar-upload { margin: 8px 0; color: #087ecc; }
.avatar-upload input { max-width: 100%; font-size: 12px; }
.identity {
  display: flex;
  gap: 14px;
  align-items: center;
}
.identity img {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 50%;
}
.identity div {
  min-width: 0;
}
.identity h2 {
  margin: 0;
}
.personal-links a {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #253544;
  text-decoration: none;
  padding: 13px 0;
  min-height: 48px;
}
.personal-links a + a {
  border-top: 1px solid #edf1f4;
}
</style>
