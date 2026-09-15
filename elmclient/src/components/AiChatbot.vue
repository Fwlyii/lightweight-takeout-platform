<template>
  <div class="ai-chat-entry">
    <button
      class="ai-chat-launcher"
      type="button"
      aria-label="打开AI点餐助手"
      title="AI点餐助手"
      @click="openAssistant"
    >
      <span class="launcher-halo" aria-hidden="true"></span>
      <span class="launcher-icon" aria-hidden="true">
        <i class="fa fa-robot"></i>
      </span>
      <span class="launcher-copy">
        <strong>AI点餐</strong>
        <small>帮你快速点单</small>
      </span>
      <i class="fa fa-angle-right launcher-arrow" aria-hidden="true"></i>
    </button>

    <!-- 抽屉式助手：语音、图片、推荐都在抽屉内完成，不再跳整页 -->
    <AiAssistantDrawer :open="drawerOpen" @close="drawerOpen = false" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AiAssistantDrawer from './AiAssistantDrawer.vue'

const drawerOpen = ref(false)
const openAssistant = () => { drawerOpen.value = true }
</script>

<style scoped>
.ai-chat-launcher {
  position: fixed;
  right: max(20px, calc((100vw - var(--app-width, 600px)) / 2 + 20px));
  bottom: 94px;
  z-index: 40;
  min-width: 108px;
  height: 48px;
  padding: 0 13px 0 9px;
  border: 1px solid rgba(255, 255, 255, .7);
  border-radius: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  background: linear-gradient(135deg, var(--skin-brand, #16a7ff) 0%, var(--skin-brand, #008cf0) 100%);
  box-shadow: 0 10px 28px rgba(var(--skin-brand-rgb, 0, 132, 225), 0.24), 0 2px 8px rgba(var(--skin-brand-strong-rgb, 25, 72, 108), 0.08);
  cursor: pointer;
  overflow: visible;
  isolation: isolate;
  transform-origin: right center;
  transition: transform .22s ease, box-shadow .22s ease, filter .22s ease;
  animation:
    launcher-enter .42s cubic-bezier(.22, .72, .28, 1) both,
    launcher-breathe 2s ease-in-out 1.1s infinite;
}

.ai-chat-launcher:hover {
  transform: translateY(-3px);
  box-shadow: 0 15px 34px rgba(var(--skin-brand-rgb, 0, 132, 225), 0.31), 0 4px 12px rgba(var(--skin-brand-strong-rgb, 25, 72, 108), 0.1);
  filter: saturate(1.04);
  animation-play-state: paused;
}

.ai-chat-launcher:active {
  animation: launcher-press 220ms cubic-bezier(.22, 1, .36, 1);
}

.launcher-halo {
  position: absolute;
  inset: -6px;
  z-index: -1;
  border-radius: 24px;
  border: 1px solid rgba(var(--skin-brand-rgb, 0, 151, 255), 0.2);
  opacity: 0;
  animation: launcher-pulse 2s ease-out 1.1s infinite;
}

.launcher-icon {
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  color: var(--skin-brand, #008df0);
  background: rgba(255, 255, 255, .94);
  font-size: 15px;
  box-shadow: inset 0 0 0 1px rgba(var(--skin-brand-rgb, 0, 151, 255), 0.08);
}

.launcher-copy {
  min-width: 0;
  display: grid;
  line-height: 1.08;
  text-align: left;
}

.launcher-copy strong {
  font-size: 13px;
  font-weight: 750;
  letter-spacing: .01em;
  white-space: nowrap;
}

.launcher-copy small {
  margin-top: 3px;
  color: rgba(255, 255, 255, .78);
  font-size: 9px;
  white-space: nowrap;
}

.launcher-arrow {
  margin-left: 1px;
  color: rgba(255, 255, 255, .72);
  font-size: 12px;
  transition: transform .2s ease;
}

.ai-chat-launcher:hover .launcher-arrow { transform: translateX(2px); }

@keyframes launcher-enter {
  from { opacity: 0; transform: translateY(12px) scale(.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes launcher-breathe {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

@keyframes launcher-press {
  0% { transform: scale(1); }
  45% { transform: scale(.95); }
  100% { transform: scale(1); }
}

@keyframes launcher-pulse {
  0% { opacity: 0; transform: scale(.98); }
  45% { opacity: .22; transform: scale(1.06); }
  72% { opacity: .08; transform: scale(1.12); }
  100% { opacity: 0; transform: scale(1.16); }
}

@media (max-width: 760px) {
  .ai-chat-launcher {
    bottom: 82px;
    min-width: 50px;
    width: 50px;
    height: 50px;
    padding: 0;
    justify-content: center;
    border-radius: 17px;
  }

  .launcher-icon { width: 34px; height: 34px; flex-basis: 34px; }
  .launcher-copy, .launcher-arrow { display: none; }
}

@media (prefers-reduced-motion: reduce) {
  .ai-chat-launcher,
  .launcher-halo,
  .launcher-arrow { animation: none !important; transition: none !important; }
}
</style>
