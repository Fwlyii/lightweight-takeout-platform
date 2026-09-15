<template>
  <transition name="ai-sheet">
    <div v-if="open" class="ai-sheet-mask" @click.self="close">
      <section class="ai-sheet" role="dialog" aria-modal="true" aria-label="AI点餐助手">
        <span class="sheet-grip" aria-hidden="true"></span>

        <header class="sheet-head">
          <span class="ai-avatar" aria-hidden="true"><i class="fa fa-robot"></i></span>
          <div class="sheet-title">
            <strong>AI点餐助手</strong>
            <small>{{ statusText }}</small>
          </div>
          <button type="button" class="sheet-close" aria-label="关闭AI助手" @click="close">
            <i class="fa fa-times"></i>
          </button>
        </header>

        <div ref="bodyRef" class="sheet-body">
          <div class="quick-chips" role="group" aria-label="快捷需求">
            <button v-for="chip in quickChips" :key="chip.label" type="button" :disabled="loading" @click="runChip(chip)">
              {{ chip.label }}
            </button>
          </div>

          <div class="chat-flow" aria-live="polite">
            <p class="chat-bubble assistant">{{ greeting }}</p>
            <p v-for="message in messages" :key="message.id" :class="['chat-bubble', message.role]">{{ message.text }}</p>
            <p v-if="loading" class="chat-bubble assistant loading"><i class="fa fa-spinner fa-spin"></i>正在为你挑选…</p>
          </div>

          <div v-if="candidates.length" class="candidate-list">
            <article v-for="candidate in candidates" :key="`${candidate.foodId}-${candidate.businessId}`" class="candidate-card">
              <img :src="candidate.foodImg || require('@/assets/food-default.png')" :alt="candidate.foodName" @error="handleImageError">
              <div class="candidate-copy">
                <strong>{{ candidate.foodName || '推荐菜品' }}</strong>
                <span>{{ candidate.businessName || '附近商家' }}</span>
                <div class="candidate-meta">
                  <b>★ {{ formatScore(candidate.businessScore) }}</b>
                  <em>¥{{ formatMoney(candidate.price) }}</em>
                  <em>{{ distanceText(candidate) }}km</em>
                </div>
              </div>
              <button type="button" class="candidate-action" @click="openBusiness(candidate)">立即查看</button>
            </article>
          </div>
        </div>

        <!-- 录音状态：不跳页面，直接在抽屉内完成 -->
        <div v-if="recording" class="recording-panel" role="status">
          <span class="recording-dot" aria-hidden="true"></span>
          <div class="recording-copy">
            <strong>正在录音…</strong>
            <small>说完点“完成”，我会帮你转成点餐需求</small>
          </div>
          <span class="recording-time">{{ recordingText }}</span>
          <button type="button" class="recording-cancel" @click="cancelRecording">取消</button>
          <button type="button" class="recording-done" @click="stopRecording">完成</button>
        </div>

        <!-- 图片上传/识别状态 -->
        <div v-else-if="imageState !== 'idle'" class="image-panel" role="status">
          <img v-if="imagePreview" :src="imagePreview" alt="待识别的菜品图片">
          <div class="image-copy">
            <strong>{{ imageState === 'recognizing' ? '正在识别图片…' : '识别结果' }}</strong>
            <small>{{ imageHint }}</small>
          </div>
          <button type="button" class="image-cancel" @click="resetImage">关闭</button>
        </div>

        <footer class="sheet-input">
          <input
            v-model="draft"
            type="text"
            maxlength="60"
            placeholder="想吃什么？例如：清淡牛肉面"
            aria-label="AI点餐输入框"
            @keyup.enter="send()">
          <button
            type="button"
            class="icon-button"
            :class="{ active: recording }"
            :disabled="!capabilities.speechRecognition || busy"
            :title="capabilities.speechRecognition ? '语音点餐' : '服务端未配置语音识别'"
            aria-label="语音点餐"
            @click="recording ? stopRecording() : startRecording()">
            <i class="fa" :class="recording ? 'fa-stop' : 'fa-microphone'"></i>
          </button>
          <button
            type="button"
            class="icon-button"
            :disabled="!capabilities.imageRecognition || busy"
            :title="capabilities.imageRecognition ? '上传菜品图片' : '服务端未配置图片识别'"
            aria-label="上传菜品图片"
            @click="imageInput?.click()">
            <i class="fa fa-picture-o"></i>
          </button>
          <input ref="imageInput" class="hidden-input" type="file" accept="image/jpeg,image/png,image/webp" @change="handleImageChange">
          <button type="button" class="send-button" :disabled="!draft.trim() || busy" aria-label="发送" @click="send()">
            <i class="fa fa-paper-plane"></i>
          </button>
        </footer>
      </section>
    </div>
  </transition>
</template>

<script>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import request from '@/utils/request';
import aiChatService from '@/services/aiChatService';
import { formatMoney } from '@/utils/formatters';
import { getBusinessDistanceKm } from '@/utils/businessPresentation';
import { toast } from '@/utils/toast';

/** 首页 AI 入口的抽屉式助手：语音、图片、推荐都在抽屉内完成，不再跳整页。 */
export default {
  name: 'AiAssistantDrawer',
  props: {
    open: { type: Boolean, default: false }
  },
  emits: ['close'],
  setup(props, { emit }) {
    const router = useRouter();
    const bodyRef = ref(null);
    const imageInput = ref(null);
    const draft = ref('');
    const messages = ref([]);
    const candidates = ref([]);
    const loading = ref(false);
    const sessionId = ref(null);
    const capabilities = ref({ textChat: true, imageRecognition: false, speechRecognition: false });
    const capabilitiesState = ref('idle');
    let messageSeed = 0;

    const greeting = '你好，我是 AI 点餐助手，说说预算和口味，我给你挑几道。';

    const quickChips = [
      { label: '20元以内', query: '饭', budget: 20 },
      { label: '早餐推荐', query: '早餐', budget: null },
      { label: '想吃辣', query: '辣', budget: null },
      { label: '减脂餐', query: '轻食', budget: null },
      { label: '查看附近商家', browse: true }
    ];

    const emptyHint = '没找到合适的，试试这些关键词：早餐 / 面 / 饭 / 辣 / 咖啡 / 轻食。';

    /** 本地关键词提取：自然语句里带出的菜名关键词再检索一次，仍走真实商品接口。 */
    const KEYWORD_HINTS = ['早餐', '咖啡', '咖啡饮品', '轻食', '沙拉', '炸鸡', '汉堡', '披萨', '面条', '面', '米饭', '饭', '米线', '粉', '粥', '包子', '汤', '辣', '甜', '饮品'];
    const extractKeyword = (text) => KEYWORD_HINTS.find(keyword => text.includes(keyword)) || '';

    const statusText = computed(() => {
      if (capabilitiesState.value === 'loading') return '正在连接';
      if (capabilities.value.textChat === false) return '助手暂不可用';
      return '在线 · 支持语音与图片点餐';
    });

    const busy = computed(() => loading.value || recording.value || imageState.value === 'recognizing');

    // ---- 录音 ----
    const recording = ref(false);
    const recordingSeconds = ref(0);
    const recordingText = computed(() => `${String(Math.floor(recordingSeconds.value / 60)).padStart(2, '0')}:${String(recordingSeconds.value % 60).padStart(2, '0')}`);
    let mediaRecorder = null;
    let mediaStream = null;
    let audioChunks = [];
    let recordTimer = null;

    // ---- 图片 ----
    const imageState = ref('idle');
    const imagePreview = ref('');
    const imageHint = ref('');

    const pushMessage = (role, text) => {
      messages.value = [...messages.value, { id: ++messageSeed, role, text }];
      scrollToBottom();
    };

    const scrollToBottom = () => {
      nextTick(() => {
        if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight;
      });
    };

    const formatScore = (score) => {
      const value = Number(score);
      return Number.isFinite(value) && value > 0 ? value.toFixed(1) : '暂无';
    };

    const distanceText = (candidate) => getBusinessDistanceKm({
      id: candidate.businessId,
      distanceKm: candidate.distanceKm,
      distance: candidate.distance
    }).toFixed(1);

    const handleImageError = (event) => {
      const image = event?.target;
      if (!image || image.dataset.fallbackApplied === 'true') return;
      image.dataset.fallbackApplied = 'true';
      image.src = require('@/assets/food-default.png');
    };

    const loadCapabilities = async () => {
      if (capabilitiesState.value === 'ready' || capabilitiesState.value === 'loading') return;
      capabilitiesState.value = 'loading';
      try {
        const result = await request.get('/api/v1/assistant/capabilities');
        if (result?.success) {
          capabilities.value = { textChat: true, ...result.data };
          capabilitiesState.value = 'ready';
        } else {
          capabilitiesState.value = 'error';
        }
      } catch (error) {
        console.error('读取AI能力失败:', error);
        capabilitiesState.value = 'error';
      }
    };

    const requestRecommendations = async (query, budget = null) => {
      loading.value = true;
      try {
        const result = await request.post('/api/v1/recommendations', { query, budget, usePreferences: true });
        candidates.value = result?.success && Array.isArray(result.data) ? result.data.slice(0, 4) : [];
        if (!candidates.value.length) pushMessage('assistant', emptyHint);
        return candidates.value;
      } catch (error) {
        console.error('AI 推荐失败:', error);
        candidates.value = [];
        pushMessage('assistant', error?.response?.data?.message || '推荐服务暂时不可用，请稍后再试。');
        return [];
      } finally {
        loading.value = false;
      }
    };

    const runChip = async (chip) => {
      if (chip.browse) {
        close();
        router.push({ path: '/businessList' });
        return;
      }
      pushMessage('user', chip.label);
      await requestRecommendations(chip.query, chip.budget);
    };

    const send = async (text = draft.value) => {
      const content = String(text || '').trim();
      if (!content || busy.value) return;
      draft.value = '';
      pushMessage('user', content);
      loading.value = true;
      try {
        const result = await aiChatService.sendMessage(content, 'food', sessionId.value);
        sessionId.value = result.data?.sessionId || sessionId.value;
        pushMessage('assistant', result.data?.message || '我看看有没有合适的。');
        const list = Array.isArray(result.data?.candidates) ? result.data.candidates : [];
        candidates.value = list.slice(0, 4);
        if (!list.length) {
          const keyword = extractKeyword(content);
          const found = await requestRecommendations(keyword || content, null);
          if (!found.length && keyword) pushMessage('assistant', emptyHint);
        }
      } finally {
        loading.value = false;
      }
    };

    const openBusiness = (candidate) => {
      const businessId = candidate?.businessId;
      close();
      if (businessId) router.push({ path: '/businessInfo', query: { businessId } });
    };

    // ---- 语音：抽屉内录音 + 转写，不跳页面 ----
    const stopMediaStream = () => {
      mediaStream?.getTracks().forEach(track => track.stop());
      mediaStream = null;
    };

    const transcribeAudio = async (blob) => {
      if (blob.size > 7 * 1024 * 1024) {
        pushMessage('assistant', '录音太长了，请控制在 7 MB 以内。');
        return;
      }
      loading.value = true;
      pushMessage('assistant', '正在识别你的语音…');
      try {
        const form = new FormData();
        form.append('audio', blob, 'voice.webm');
        const result = await request.post('/api/v1/voice-order-drafts', form, { timeout: 15000 });
        const transcript = result?.data?.transcript?.trim();
        if (transcript) {
          draft.value = result.data.query || transcript;
          pushMessage('user', `（语音）${draft.value}`);
          await requestRecommendations(result.data.query || transcript, result.data.budget || null);
        } else {
          pushMessage('assistant', '没听清，再说一次或直接打字告诉我。');
        }
      } catch (error) {
        console.error('语音识别失败:', error);
        pushMessage('assistant', error?.response?.data?.message || '语音识别失败，可以改用文字输入。');
      } finally {
        loading.value = false;
      }
    };

    const startRecording = async () => {
      if (!capabilities.value.speechRecognition) {
        toast.info('服务端未配置语音识别，先试试打字或图片吧');
        return;
      }
      try {
        mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
      } catch (error) {
        pushMessage('assistant', error?.name === 'NotAllowedError' ? '需要麦克风权限才能语音点餐。' : '无法启动录音，请改用文字输入。');
        return;
      }
      audioChunks = [];
      mediaRecorder = new MediaRecorder(mediaStream);
      mediaRecorder.ondataavailable = (event) => { if (event.data?.size) audioChunks.push(event.data); };
      mediaRecorder.onstop = async () => {
        stopMediaStream();
        const blob = new Blob(audioChunks, { type: mediaRecorder?.mimeType || 'audio/webm' });
        if (blob.size) await transcribeAudio(blob);
      };
      mediaRecorder.start();
      recording.value = true;
      recordingSeconds.value = 0;
      recordTimer = window.setInterval(() => { recordingSeconds.value += 1; }, 1000);
    };

    const clearRecordTimer = () => {
      if (recordTimer) window.clearInterval(recordTimer);
      recordTimer = null;
    };

    const stopRecording = () => {
      clearRecordTimer();
      recording.value = false;
      if (mediaRecorder?.state === 'recording') mediaRecorder.stop();
    };

    const cancelRecording = () => {
      clearRecordTimer();
      recording.value = false;
      audioChunks = [];
      if (mediaRecorder?.state === 'recording') {
        mediaRecorder.onstop = null;
        mediaRecorder.stop();
      }
      stopMediaStream();
    };

    // ---- 图片：抽屉内上传识别，不跳页面 ----
    const resetImage = () => {
      if (imagePreview.value) URL.revokeObjectURL(imagePreview.value);
      imagePreview.value = '';
      imageHint.value = '';
      imageState.value = 'idle';
    };

    const handleImageChange = async (event) => {
      const file = event.target.files?.[0];
      event.target.value = '';
      if (!file) return;
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024) {
        pushMessage('assistant', '请选择不超过 5 MB 的 JPG、PNG 或 WebP 图片。');
        return;
      }
      resetImage();
      imagePreview.value = URL.createObjectURL(file);
      imageState.value = 'recognizing';
      imageHint.value = '正在识别图片…';
      try {
        const form = new FormData();
        form.append('image', file);
        const result = await request.post('/api/v1/dish-recognitions', form, { timeout: 15000 });
        const keywords = Array.isArray(result?.data?.keywords) ? result.data.keywords.filter(Boolean) : [];
        if (keywords.length) {
          imageState.value = 'done';
          imageHint.value = `识别到：${keywords.slice(0, 3).join('、')}`;
          pushMessage('user', `（图片）${keywords.slice(0, 3).join('、')}`);
          await requestRecommendations(keywords.join(' '), null);
        } else {
          resetImage();
          pushMessage('assistant', '这张图没认出菜品，换一张或直接告诉我菜名。');
        }
      } catch (error) {
        console.error('图片识别失败:', error);
        resetImage();
        pushMessage('assistant', error?.response?.data?.message || '图片识别失败，可改用文字搜索。');
      }
    };

    const close = () => {
      cancelRecording();
      resetImage();
      emit('close');
    };

    watch(() => props.open, (value) => {
      if (value) {
        loadCapabilities();
        scrollToBottom();
      } else {
        cancelRecording();
      }
    });

    onBeforeUnmount(() => {
      clearRecordTimer();
      stopMediaStream();
      resetImage();
    });

    return {
      bodyRef,
      imageInput,
      draft,
      messages,
      candidates,
      loading,
      capabilities,
      statusText,
      greeting,
      quickChips,
      recording,
      recordingText,
      imageState,
      imagePreview,
      imageHint,
      busy,
      formatMoney,
      formatScore,
      distanceText,
      handleImageError,
      runChip,
      send,
      openBusiness,
      startRecording,
      stopRecording,
      cancelRecording,
      handleImageChange,
      resetImage,
      close
    };
  }
};
</script>

<style scoped>
/* 抽屉：与首页同色系（#1677FF）+ 20px 圆角 */
.ai-sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 13000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(var(--fwl-brand-strong-rgb, 12, 52, 86), 0.42);
}

.ai-sheet {
  position: relative;
  width: 100%;
  max-width: 600px;
  height: 70vh;
  display: flex;
  flex-direction: column;
  padding: 8px 0 0;
  box-sizing: border-box;
  border-radius: 20px 20px 0 0;
  background: #fff;
  box-shadow: 0 -14px 40px rgba(var(--fwl-brand-strong-rgb, 12, 52, 86), 0.2);
}

.sheet-grip {
  display: block;
  width: 42px;
  height: 4px;
  margin: 0 auto 6px;
  border-radius: 3px;
  background: var(--fwl-border, #dbe6ee);
}

.sheet-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 16px 10px;
  border-bottom: 1px solid var(--fwl-surface, #eef4f9);
}

.ai-avatar {
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  background: linear-gradient(135deg, var(--fwl-brand-soft, #4da3ff), var(--fwl-brand, #1677ff));
  color: #fff;
  font-size: 17px;
  box-shadow: 0 5px 14px rgba(var(--fwl-brand-rgb, 22, 119, 255), 0.26);
}

.sheet-title { min-width: 0; flex: 1; }
.sheet-title strong { display: block; color: var(--fwl-brand-strong, #17324f); font-size: 16px; font-weight: 800; }
.sheet-title small { display: block; margin-top: 2px; color: var(--fwl-muted, #8aa0b2); font-size: 11px; }

.sheet-close {
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  border: 0;
  border-radius: 50%;
  background: var(--fwl-surface, #f2f6fa);
  color: var(--fwl-muted, #7b93a8);
  font-size: 14px;
  cursor: pointer;
}

.sheet-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 16px 6px;
}

.quick-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-chips button {
  padding: 7px 13px;
  border: 1px solid var(--fwl-border, #d9e9f8);
  border-radius: 15px;
  background: var(--fwl-surface, #f4faff);
  color: var(--fwl-brand, #1677ff);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.quick-chips button:disabled { opacity: .55; cursor: not-allowed; }

.chat-flow { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }

.chat-bubble {
  max-width: 86%;
  margin: 0;
  padding: 9px 12px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
}

.chat-bubble.assistant { align-self: flex-start; background: var(--fwl-surface, #f2f7fd); color: var(--fwl-ink, #31556d); border-top-left-radius: 6px; }
.chat-bubble.user { align-self: flex-end; background: linear-gradient(135deg, var(--fwl-brand-soft, #4da3ff), var(--fwl-brand, #1677ff)); color: #fff; border-top-right-radius: 6px; }
.chat-bubble.loading i { margin-right: 6px; }

.candidate-list { display: grid; gap: 9px; margin-top: 12px; }

.candidate-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px;
  border: 1px solid var(--fwl-border, #e1edf7);
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 6px 16px rgba(var(--fwl-brand-strong-rgb, 39, 86, 114), 0.06);
}

.candidate-card img { width: 62px; height: 62px; flex: 0 0 62px; border-radius: 12px; object-fit: cover; background: var(--fwl-surface, #eef5f9); }
.candidate-copy { min-width: 0; flex: 1; }
.candidate-copy strong { display: block; overflow: hidden; color: var(--fwl-brand-strong, #17324f); font-size: 14px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.candidate-copy span { display: block; margin-top: 3px; overflow: hidden; color: var(--fwl-muted, #8aa0b2); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.candidate-meta { display: flex; align-items: baseline; gap: 8px; margin-top: 5px; }
.candidate-meta b { color: #f27635; font-size: 12px; }
.candidate-meta em { color: var(--fwl-brand, #1677ff); font-size: 12px; font-style: normal; font-weight: 700; }

.candidate-action {
  flex: 0 0 auto;
  padding: 7px 12px;
  border: 0;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--fwl-brand-soft, #4da3ff), var(--fwl-brand, #1677ff));
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

/* 录音 / 图片状态条 */
.recording-panel,
.image-panel {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 16px 8px;
  padding: 10px 12px;
  border-radius: 16px;
  background: var(--fwl-surface, #f2f8ff);
  border: 1px solid var(--fwl-border, #d9e9f8);
}

.recording-dot {
  width: 12px;
  height: 12px;
  flex: 0 0 12px;
  border-radius: 50%;
  background: #f4483b;
  animation: record-pulse 1.2s ease-in-out infinite;
}

@keyframes record-pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.35); opacity: .6; }
}

.recording-copy, .image-copy { min-width: 0; flex: 1; }
.recording-copy strong, .image-copy strong { display: block; color: var(--fwl-brand-strong, #17324f); font-size: 13px; }
.recording-copy small, .image-copy small { display: block; margin-top: 2px; overflow: hidden; color: var(--fwl-muted, #8aa0b2); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.recording-time { color: var(--fwl-brand, #1677ff); font-size: 13px; font-weight: 700; }

.recording-cancel, .image-cancel {
  flex: 0 0 auto;
  padding: 6px 11px;
  border: 1px solid var(--fwl-border, #d9e9f8);
  border-radius: 13px;
  background: #fff;
  color: var(--fwl-muted, #7b93a8);
  font-size: 12px;
  cursor: pointer;
}

.recording-done {
  flex: 0 0 auto;
  padding: 6px 13px;
  border: 0;
  border-radius: 13px;
  background: linear-gradient(135deg, var(--fwl-brand-soft, #4da3ff), var(--fwl-brand, #1677ff));
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.image-panel img { width: 40px; height: 40px; flex: 0 0 40px; border-radius: 10px; object-fit: cover; }

/* 输入区 */
.sheet-input {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px calc(14px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--fwl-surface, #eef4f9);
  background: #fff;
}

.sheet-input input[type="text"] {
  flex: 1;
  min-width: 0;
  height: 42px;
  padding: 0 14px;
  box-sizing: border-box;
  border: 1px solid var(--fwl-border, #dbe9f4);
  border-radius: 21px;
  background: var(--fwl-surface, #f7fbff);
  color: var(--fwl-brand-strong, #17324f);
  font-size: 13px;
  outline: none;
}

.sheet-input input[type="text"]:focus { border-color: var(--fwl-brand-soft, #85c2ff); background: #fff; }

.icon-button {
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  border: 0;
  border-radius: 50%;
  background: var(--fwl-surface, #f1f7fe);
  color: var(--fwl-brand, #1677ff);
  font-size: 16px;
  cursor: pointer;
}

.icon-button.active { background: #ffeaea; color: #f4483b; }
.icon-button:disabled { color: var(--fwl-subtle, #b8cbd7); cursor: not-allowed; }

.send-button {
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  border: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--fwl-brand-soft, #4da3ff), var(--fwl-brand, #1677ff));
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  box-shadow: 0 6px 14px rgba(var(--fwl-brand-rgb, 22, 119, 255), 0.24);
}

.send-button:disabled { background: var(--fwl-subtle, #c3d6e4); box-shadow: none; cursor: not-allowed; }

.hidden-input { display: none; }

.ai-sheet-enter-active, .ai-sheet-leave-active { transition: opacity 240ms ease; }
.ai-sheet-enter-active .ai-sheet, .ai-sheet-leave-active .ai-sheet { transition: transform 320ms cubic-bezier(.22, 1, .36, 1); }
.ai-sheet-enter-from, .ai-sheet-leave-to { opacity: 0; }
.ai-sheet-enter-from .ai-sheet, .ai-sheet-leave-to .ai-sheet { transform: translateY(100%); }

@media (prefers-reduced-motion: reduce) {
  .ai-sheet-enter-active, .ai-sheet-leave-active,
  .ai-sheet-enter-active .ai-sheet, .ai-sheet-leave-active .ai-sheet { transition: none; }
  .recording-dot { animation: none; }
}
</style>
