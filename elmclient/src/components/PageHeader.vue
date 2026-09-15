<template>
  <header class="unified-page-header">
    <button type="button" aria-label="返回" @click="back">‹</button>
    <h1>{{ title }}</h1>
    <div class="header-extra"><slot /></div>
  </header>
</template>
<script setup>
import { useRoute, useRouter } from 'vue-router';
import { navigateBack } from '../utils/backNavigation';
const props = defineProps({ title: String, backTo: [String, Object], backAction: Function });
const router = useRouter(), route = useRoute();
const back = () => props.backAction ? props.backAction() : props.backTo ? router.push(props.backTo) : navigateBack(router, route);
</script>
<style>
.unified-page-header { position: sticky; top: 0; z-index: 30; width: 100%; box-sizing: border-box; min-height: 68px; padding: 12px 20px; display: grid; grid-template-columns: 44px minmax(0,1fr) 44px; align-items: center; gap: 12px; background: rgba(255,255,255,.96); border-bottom: 1px solid var(--skin-border); color: var(--skin-ink); backdrop-filter: blur(16px); }
.unified-page-header h1 { margin: 0; font-size: 19px; font-weight: 700; line-height: 1.4; text-align: center; color: var(--skin-ink); }
.unified-page-header > button { width: 40px; height: 40px; min-height: 40px; padding: 0; border: 1px solid var(--skin-border); border-radius: 12px; display: grid; place-items: center; background: var(--skin-surface); color: var(--skin-ink); font: 30px/1 Arial,sans-serif; cursor: pointer; }
@media(max-width:600px) { .unified-page-header { min-height: 60px; padding: 8px 12px; } }
</style>
