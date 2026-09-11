<template>
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
      <small>懂你想吃的</small>
    </span>
    <i class="fa fa-angle-right launcher-arrow" aria-hidden="true"></i>
  </button>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
const openAssistant = () => router.push('/ai-chat')
</script>

<style scoped>
.ai-chat-launcher {
  position: fixed;
  right: max(20px, calc((100vw - 600px) / 2 - 112px));
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
  background: linear-gradient(135deg, #16a7ff 0%, #008cf0 100%);
  box-shadow: 0 10px 28px rgba(0, 132, 225, .24), 0 2px 8px rgba(25, 72, 108, .08);
  cursor: pointer;
  overflow: visible;
  isolation: isolate;
  transition: transform .22s ease, box-shadow .22s ease, filter .22s ease;
  animation: launcher-enter .42s cubic-bezier(.22, .72, .28, 1) both;
}

.ai-chat-launcher:hover {
  transform: translateY(-3px);
  box-shadow: 0 15px 34px rgba(0, 132, 225, .31), 0 4px 12px rgba(25, 72, 108, .1);
  filter: saturate(1.04);
}

.ai-chat-launcher:active { transform: translateY(-1px) scale(.985); }

.launcher-halo {
  position: absolute;
  inset: -6px;
  z-index: -1;
  border-radius: 24px;
  border: 1px solid rgba(0, 151, 255, .2);
  opacity: 0;
  animation: launcher-pulse 4s ease-out 1.1s infinite;
}

.launcher-icon {
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  color: #008df0;
  background: rgba(255, 255, 255, .94);
  font-size: 15px;
  box-shadow: inset 0 0 0 1px rgba(0, 151, 255, .08);
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

@keyframes launcher-pulse {
  0%, 68% { opacity: 0; transform: scale(.96); }
  76% { opacity: .75; }
  94%, 100% { opacity: 0; transform: scale(1.12); }
}

@media (max-width: 760px) {
  .ai-chat-launcher {
    right: 14px;
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
