<template>
  <main class="profile-page">
    <header class="profile-header">
      <button type="button" aria-label="返回" @click="back">‹</button>
      <h1>{{ title }}</h1>
      <span aria-hidden="true"></span>
    </header>
    <section class="profile-content"><slot /></section>
  </main>
</template>
<script setup>
import { useRouter } from 'vue-router';
const props = defineProps({ title: String, backTo: { type: String, default: '/myInformation' } });
const router = useRouter();
const back = () => router.push(props.backTo);
</script>
<style>
.profile-page { width:100%; max-width:600px; margin:0 auto; min-height:100vh; background:#f4f7fa; color:#253544; font:16px/1.5 -apple-system,BlinkMacSystemFont,'Microsoft YaHei',sans-serif; }
.profile-page *, .profile-page *::before, .profile-page *::after { box-sizing:border-box; }
.profile-page .profile-header { display:grid; grid-template-columns:48px minmax(0,1fr) 48px; align-items:center; background:#0097ff; color:#fff; min-height:56px; padding:0 8px; }
.profile-page h1 { margin:0; text-align:center; font-size:19px; font-weight:600; }
.profile-page .profile-header button { background:transparent; color:white; border:0; font-size:32px; min-height:44px; }
.profile-page .profile-content { padding:16px 14px calc(96px + env(safe-area-inset-bottom)); }
.profile-page .panel { background:#fff; border:1px solid #e4ebf0; border-radius:10px; padding:16px; margin-bottom:14px; overflow-wrap:anywhere; }
.profile-page h2 { font-size:17px; margin:0 0 12px; }
.profile-page p { margin:8px 0; }
.profile-page button, .profile-page input, .profile-page select { font:inherit; }
.profile-page button, .profile-page .action-link { cursor:pointer; border:1px solid #d5e1ea; background:#fff; color:#0879c6; border-radius:7px; padding:9px 13px; min-height:44px; text-decoration:none; display:inline-flex; align-items:center; justify-content:center; }
.profile-page button:disabled { opacity:.55; cursor:not-allowed; }
.profile-page .primary { background:#0097ff; border-color:#0097ff; color:#fff; }
.profile-page .wide { width:100%; }
.profile-page .actions { display:flex; flex-wrap:wrap; gap:8px; margin-top:14px; }
.profile-page .muted { color:#687b8b; font-size:14px; }
.profile-page .notice { border-radius:6px; padding:10px 12px; background:#e9f5ff; color:#086caf; margin-bottom:12px; overflow-wrap:anywhere; }
.profile-page .error { color:#ac2934; background:#fff1f2; }
.profile-page fieldset { min-width:0; border:0; padding:0; margin:0; }
.profile-page label { display:block; margin:0 0 14px; font-size:14px; color:#445968; }
.profile-page input, .profile-page select { display:block; width:100%; min-height:44px; padding:10px; margin-top:6px; border:1px solid #cbd8e2; border-radius:7px; background:#fff; color:#253544; }
.profile-page .split { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
.profile-page a:focus-visible, .profile-page button:focus-visible, .profile-page input:focus-visible { outline:3px solid #84caff; outline-offset:2px; }
</style>
