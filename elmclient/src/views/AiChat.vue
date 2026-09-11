<template>
  <div class="assistant-page">
    <header class="ai-topbar">
      <button class="topbar-icon" type="button" title="返回" @click="goBack"><i class="fa fa-arrow-left"></i></button>
      <div class="topbar-title">
        <h1>AI问答客服</h1>
        <span v-if="activeTool !== 'chat'">{{ currentToolLabel }}</span>
      </div>
      <div class="topbar-actions">
        <span class="service-status" :class="aiStatus.type"><i></i>{{ aiStatus.text }}</span>
        <button class="topbar-icon" type="button" title="购物车" @click="router.push('/cart')"><i class="fa fa-shopping-cart"></i></button>
      </div>
    </header>

    <div class="page-content">
      <nav v-if="activeTool === 'chat'" class="feature-bubbles" aria-label="AI功能入口">
        <button v-for="tool in tools" :key="tool.key" type="button" @click="openTool(tool)">
          <span class="feature-icon"><i :class="tool.icon"></i></span>
          <span class="feature-copy"><b>{{ tool.label }}</b><small>{{ tool.description }}</small></span>
          <i class="fa fa-chevron-right feature-arrow"></i>
        </button>
      </nav>

      <nav v-else class="feature-switcher" aria-label="切换AI功能">
        <button type="button" @click="router.push('/ai-chat')"><i class="fa fa-comments-o"></i><span>问答客服</span></button>
        <button v-for="tool in tools" :key="tool.key" type="button" :class="{ active: activeTool === tool.key }" @click="openTool(tool)">
          <i :class="tool.icon"></i><span>{{ tool.shortLabel }}</span>
        </button>
      </nav>

      <section v-if="activeTool !== 'chat'" class="tool-panel" aria-live="polite">
      <div v-if="activeTool === 'recommend'" class="tool-content">
        <div class="tool-heading"><div><h2>按需求推荐</h2><p>结果来自当前上架且有库存的真实商品</p></div></div>
        <div class="query-row">
          <label><span>想吃什么</span><input v-model.trim="smartQuery" maxlength="60" placeholder="例如：清淡牛肉面" @keyup.enter="findSmartFoods"></label>
          <label class="budget-field"><span>预算</span><input v-model.number="smartBudget" type="number" min="1" max="999" placeholder="¥"></label>
          <button class="primary-button" type="button" :disabled="smartLoading" @click="findSmartFoods">
            <i :class="smartLoading ? 'fa fa-spinner fa-spin' : 'fa fa-search'"></i>{{ smartLoading ? '匹配中' : '查找' }}
          </button>
        </div>
        <FoodCandidates :foods="smartFoods" :empty-text="smartEmptyText" @add="addFoodToCart" />
      </div>

      <div v-else-if="activeTool === 'image'" class="tool-content media-layout">
        <div class="media-control">
          <div class="tool-heading"><div><h2>图片识菜</h2><p>支持 JPG、PNG、WebP，单张不超过 5 MB</p></div></div>
          <input ref="imageInput" class="hidden-input" type="file" accept="image/jpeg,image/png,image/webp" @change="handleImageChange">
          <button class="upload-zone" type="button" :disabled="imageLoading || capabilitiesState !== 'ready' || !capabilities.imageRecognition" @click="imageInput?.click()">
            <img v-if="imagePreview" :src="imagePreview" alt="待识别菜品预览">
            <template v-else><i class="fa fa-camera"></i><b>选择或拍摄菜品图片</b><span>识别后可修改关键词再搜索</span></template>
          </button>
          <p v-if="capabilitiesState === 'loading'" class="processing-note"><i class="fa fa-spinner fa-spin"></i>正在读取图片识别能力</p>
          <p v-else-if="capabilitiesState === 'error'" class="capability-note capability-error"><i class="fa fa-exclamation-circle"></i>能力状态读取失败，请检查连接后重试<button type="button" @click="loadCapabilities">重试</button></p>
          <p v-else-if="!capabilities.imageRecognition" class="capability-note"><i class="fa fa-info-circle"></i>服务端尚未配置图片识别模型</p>
          <p v-if="imageLoading" class="processing-note"><i class="fa fa-spinner fa-spin"></i>正在识别图片并匹配在售商品</p>
          <div v-if="imageResult" class="recognition-summary">
            <div><strong>{{ imageResult.summary }}</strong><span>置信度 {{ confidenceText(imageResult.confidence) }}</span></div>
            <div class="keyword-list">
              <button v-for="keyword in imageResult.keywords" :key="keyword" type="button" @click="searchKeyword(keyword)">{{ keyword }}</button>
            </div>
          </div>
        </div>
        <FoodCandidates :foods="imageResult?.candidates || []" empty-text="识别出的真实商品会显示在这里" @add="addFoodToCart" />
      </div>

      <div v-else class="tool-content media-layout">
        <div class="media-control">
          <div class="tool-heading"><div><h2>语音点单</h2><p>录音只用于本次转写，单段不超过 7 MB</p></div></div>
          <input ref="audioInput" class="hidden-input" type="file" accept="audio/*,video/webm,video/mp4" @change="handleAudioFile">
          <div class="voice-actions">
            <button class="record-button" :class="{ recording: isRecording }" type="button" :disabled="voiceLoading || capabilitiesState !== 'ready' || !capabilities.speechRecognition" @click="toggleRecording">
              <i :class="isRecording ? 'fa fa-stop' : 'fa fa-microphone'"></i>
              <span>{{ isRecording ? `停止录音 ${recordingSeconds}s` : '开始录音' }}</span>
            </button>
            <button class="secondary-button" type="button" :disabled="voiceLoading || capabilitiesState !== 'ready' || !capabilities.speechRecognition" @click="audioInput?.click()"><i class="fa fa-folder-open"></i>选择音频</button>
          </div>
          <p v-if="capabilitiesState === 'loading'" class="processing-note"><i class="fa fa-spinner fa-spin"></i>正在读取语音识别能力</p>
          <p v-else-if="capabilitiesState === 'error'" class="capability-note capability-error"><i class="fa fa-exclamation-circle"></i>能力状态读取失败，请检查连接后重试<button type="button" @click="loadCapabilities">重试</button></p>
          <p v-else-if="!capabilities.speechRecognition" class="capability-note"><i class="fa fa-info-circle"></i>服务端尚未配置语音识别模型</p>
          <p v-if="voiceLoading" class="processing-note"><i class="fa fa-spinner fa-spin"></i>正在转写并生成可编辑点餐草稿</p>
          <div v-if="voiceDraft" class="voice-draft">
            <label><span>识别文本</span><textarea v-model.trim="voiceDraft.transcript" maxlength="300"></textarea></label>
            <div class="draft-row">
              <label><span>商品关键词</span><input v-model.trim="voiceDraft.query" maxlength="60" @keyup.enter="refreshVoiceCandidates"></label>
              <label class="quantity-field"><span>数量</span><input v-model.number="voiceDraft.quantity" type="number" min="1" max="99"></label>
              <label><span>规格</span><input v-model.trim="voiceDraft.specification" maxlength="80" placeholder="如：大杯、少冰"></label>
              <label class="budget-field"><span>预算</span><input v-model.number="voiceDraft.budget" type="number" min="1" max="9999" placeholder="¥"></label>
              <button class="secondary-button" type="button" @click="refreshVoiceCandidates"><i class="fa fa-refresh"></i>重新匹配</button>
            </div>
          </div>
        </div>
        <FoodCandidates :foods="voiceDraft?.candidates || []" :quantity="voiceDraft?.quantity || 1" empty-text="语音转写后可编辑草稿并确认商品" @add="addFoodToCart" />
      </div>
    </section>

      <main v-if="activeTool === 'chat'" class="chat-shell">
      <section ref="messagesContainer" class="message-list" aria-label="对话记录">
        <div v-if="messages.length === 0" class="empty-chat">
          <i class="fa fa-commenting-o"></i><h2>文字助手</h2><p>可查询本人订单、平台规则和在售菜品</p>
          <div class="quick-list"><button v-for="question in quickQuestions" :key="question" type="button" @click="askQuickQuestion(question)">{{ question }}</button></div>
        </div>
        <article v-for="(message, index) in messages" :key="index" class="message" :class="message.type">
          <div class="message-avatar"><i :class="message.type === 'user' ? 'fa fa-user' : 'fa fa-robot'"></i></div>
          <div class="message-body">
            <div class="message-bubble" v-html="formatMessage(message.content)"></div>
            <FoodCandidates v-if="message.candidates?.length" :foods="message.candidates" @add="addFoodToCart" />
            <time>{{ formatTime(message.timestamp) }}<span v-if="message.processingTime"> · {{ message.processingTime }}ms</span></time>
          </div>
        </article>
        <article v-if="isTyping" class="message ai"><div class="message-avatar"><i class="fa fa-robot"></i></div><div class="message-body"><div class="message-bubble typing"><i></i><i></i><i></i></div></div></article>
      </section>

      <footer class="chat-composer">
        <textarea ref="messageInput" v-model="inputMessage" rows="1" maxlength="500" placeholder="输入问题，Enter 发送，Shift+Enter 换行" :disabled="isTyping" @keydown="handleKeyDown" @input="resizeComposer"></textarea>
        <div class="composer-actions">
          <span>{{ inputMessage.length }}/500</span>
          <button class="icon-button" type="button" title="历史记录" @click="openHistory"><i class="fa fa-history"></i></button>
          <button class="icon-button" type="button" title="清空当前对话" @click="clearChat"><i class="fa fa-trash"></i></button>
          <button class="send-button" type="button" :disabled="!inputMessage.trim() || isTyping" @click="sendMessage"><i class="fa fa-paper-plane"></i><span>发送</span></button>
        </div>
      </footer>
      </main>
    </div>

    <div v-if="showHistory" class="drawer-mask" @click.self="showHistory = false">
      <aside class="history-drawer">
        <header><h2>对话历史</h2><button class="icon-button" type="button" title="关闭" @click="showHistory = false"><i class="fa fa-times"></i></button></header>
        <button class="secondary-button history-refresh" type="button" :disabled="loadingHistory" @click="loadChatHistory"><i :class="loadingHistory ? 'fa fa-spinner fa-spin' : 'fa fa-refresh'"></i>刷新</button>
        <div v-if="chatHistory.length" class="history-list">
          <button v-for="history in chatHistory" :key="history.id" type="button" @click="loadHistorySession(history.sessionId)">
            <span><b>{{ truncateText(history.userMessage, 42) }}</b><time>{{ formatTime(history.createTime) }}</time></span><i class="fa fa-chevron-right"></i>
          </button>
        </div>
        <p v-else class="history-empty">暂无对话历史</p>
      </aside>
    </div>
  </div>
</template>

<script>
import { computed, defineComponent, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import aiChatService from '../services/aiChatService'
import { addCartItem } from '../services/cartService'
import request from '../utils/request'
import { formatSafeMessage } from '../utils/safeMessage'

const FoodCandidates = defineComponent({
  name: 'FoodCandidates',
  props: {
    foods: { type: Array, default: () => [] },
    quantity: { type: Number, default: 1 },
    emptyText: { type: String, default: '暂无候选' }
  },
  emits: ['add'],
  setup(props, { emit }) {
    return () => h('div', { class: 'candidate-list' }, props.foods.length
      ? props.foods.map(food => h('article', { class: 'candidate-item', key: food.foodId }, [
          food.foodImg
            ? h('img', { src: food.foodImg, alt: food.foodName })
            : h('span', { class: 'food-placeholder' }, [h('i', { class: 'fa fa-cutlery' })]),
          h('div', { class: 'candidate-copy' }, [
            h('b', food.foodName),
            h('span', `${food.businessName || '平台商家'} · ¥${Number(food.price || 0).toFixed(2)}`),
            h('small', food.reason || '真实在售商品')
          ]),
          h('button', { type: 'button', title: `加入${props.quantity}份`, onClick: () => emit('add', food, props.quantity) }, [
            h('i', { class: 'fa fa-cart-plus' }), h('span', props.quantity > 1 ? `加入 ${props.quantity} 份` : '加入')
          ])
        ]))
      : [h('div', { class: 'candidate-empty' }, [h('i', { class: 'fa fa-search' }), h('span', props.emptyText)])])
  }
})

export default defineComponent({
  name: 'AiChat',
  components: { FoodCandidates },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const tools = [
      { key: 'recommend', label: '智能筛选', shortLabel: '智能筛选', description: '按口味和预算匹配在售餐品', icon: 'fa fa-sliders', path: '/ai-chat/recommend' },
      { key: 'voice', label: '语音问答或者语音点菜', shortLabel: '语音点菜', description: '说出需求，自动生成点餐草稿', icon: 'fa fa-microphone', path: '/ai-chat/voice' },
      { key: 'image', label: '菜品识别', shortLabel: '菜品识别', description: '上传图片识别并查找同类菜品', icon: 'fa fa-camera', path: '/ai-chat/image' }
    ]
    const activeTool = ref('chat')
    const currentToolLabel = computed(() => tools.find(tool => tool.key === activeTool.value)?.label || '在线问答')
    const openTool = tool => {
      activeTool.value = tool.key
      router.push(tool.path)
    }
    const goBack = () => {
      if (activeTool.value !== 'chat') router.push('/ai-chat')
      else router.back()
    }
    watch(() => route.meta.aiTool, value => {
      activeTool.value = typeof value === 'string' ? value : 'chat'
    }, { immediate: true })
    const capabilities = ref({ textChat: true, imageRecognition: false, speechRecognition: false })
    const capabilitiesState = ref('loading')
    const aiStatus = ref({ type: 'checking', text: '检查中' })
    const smartQuery = ref('')
    const smartBudget = ref(null)
    const smartFoods = ref([])
    const smartEmptyText = ref('输入需求后获取商品候选')
    const smartLoading = ref(false)
    const imageInput = ref(null)
    const imagePreview = ref('')
    const imageResult = ref(null)
    const imageLoading = ref(false)
    const audioInput = ref(null)
    const voiceDraft = ref(null)
    const voiceLoading = ref(false)
    const isRecording = ref(false)
    const recordingSeconds = ref(0)
    const messages = ref([])
    const inputMessage = ref('')
    const isTyping = ref(false)
    const messagesContainer = ref(null)
    const messageInput = ref(null)
    const currentSessionId = ref(null)
    const showHistory = ref(false)
    const chatHistory = ref([])
    const loadingHistory = ref(false)
    const quickQuestions = ['推荐一些在售菜品', '查看我的最近订单', '配送一般需要多久', '会员有哪些权益']
    let mediaRecorder = null
    let mediaStream = null
    let recordingTimer = null
    let audioChunks = []

    const errorMessage = (error, fallback) => error?.response?.data?.message || error?.message || fallback
    const appendAssistantMessage = content => messages.value.push({ type: 'ai', content, timestamp: new Date() })

    const loadCapabilities = async () => {
      capabilitiesState.value = 'loading'
      try {
        const result = await request.get('/api/v1/assistant/capabilities')
        if (!result.success || !result.data) throw new Error(result.message || '能力接口返回异常')
        capabilities.value = result.data
        capabilitiesState.value = 'ready'
      } catch (_) {
        capabilitiesState.value = 'error'
      }
    }

    const checkAiStatus = async () => {
      try {
        const result = await aiChatService.healthCheck()
        aiStatus.value = result.success ? { type: 'online', text: '文字服务在线' } : { type: 'offline', text: '服务异常' }
      } catch (_) {
        aiStatus.value = { type: 'offline', text: '连接失败' }
      }
    }

    const findSmartFoods = async () => {
      if (smartLoading.value) return
      smartLoading.value = true
      smartEmptyText.value = '正在匹配在售商品...'
      try {
        const result = await request.post('/api/v1/recommendations', { query: smartQuery.value, budget: smartBudget.value || null, usePreferences: true })
        smartFoods.value = result.success ? result.data || [] : []
        smartEmptyText.value = smartFoods.value.length
          ? '输入需求后获取商品候选'
          : '暂无符合条件的在售商品，请调整关键词或预算'
      } catch (error) {
        smartFoods.value = []
        smartEmptyText.value = errorMessage(error, '智能推荐暂时不可用，请稍后重试')
      } finally {
        smartLoading.value = false
      }
    }

    const addFoodToCart = async (food, quantity = 1) => {
      const safeQuantity = Math.max(1, Math.min(99, Number(quantity) || 1))
      try {
        await addCartItem(food.foodId, safeQuantity)
        appendAssistantMessage(`已确认将 ${food.foodName} × ${safeQuantity} 加入购物车。`)
      } catch (error) {
        appendAssistantMessage(errorMessage(error, '加入购物车失败，请检查库存或登录状态。'))
      }
      await scrollToBottom()
    }

    const handleImageChange = async event => {
      const file = event.target.files?.[0]
      event.target.value = ''
      if (!file) return
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024) {
        appendAssistantMessage('请选择不超过 5 MB 的 JPG、PNG 或 WebP 图片。')
        return
      }
      if (imagePreview.value) URL.revokeObjectURL(imagePreview.value)
      imagePreview.value = URL.createObjectURL(file)
      imageResult.value = null
      imageLoading.value = true
      try {
        const form = new FormData()
        form.append('image', file)
        const result = await request.post('/api/v1/dish-recognitions', form, { timeout: 15000 })
        if (result.success) imageResult.value = result.data
      } catch (error) {
        appendAssistantMessage(errorMessage(error, '图片识别失败，可改用文字搜索。'))
      } finally {
        imageLoading.value = false
      }
    }

    const searchKeyword = keyword => {
      smartQuery.value = keyword
      activeTool.value = 'recommend'
      router.push('/ai-chat/recommend')
      findSmartFoods()
    }

    const preferredAudioType = () => {
      if (!window.MediaRecorder) return ''
      return ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4'].find(type => MediaRecorder.isTypeSupported(type)) || ''
    }

    const toggleRecording = async () => {
      if (isRecording.value) return stopRecording()
      if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
        appendAssistantMessage('当前浏览器不支持录音，请选择已有音频文件。')
        return
      }
      try {
        mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })
        const mimeType = preferredAudioType()
        const recorder = mimeType ? new MediaRecorder(mediaStream, { mimeType }) : new MediaRecorder(mediaStream)
        mediaRecorder = recorder
        audioChunks = []
        recorder.ondataavailable = event => { if (event.data.size) audioChunks.push(event.data) }
        recorder.onstop = async () => {
          const blob = new Blob(audioChunks, { type: recorder.mimeType || 'audio/webm' })
          stopMediaStream()
          await transcribeAudio(blob, `voice-${Date.now()}.${blob.type.includes('mp4') ? 'm4a' : 'webm'}`)
        }
        recorder.start()
        isRecording.value = true
        recordingSeconds.value = 0
        recordingTimer = window.setInterval(() => {
          recordingSeconds.value += 1
          if (recordingSeconds.value >= 60) stopRecording()
        }, 1000)
      } catch (error) {
        stopMediaStream()
        appendAssistantMessage(error?.name === 'NotAllowedError' ? '没有麦克风权限，请在浏览器设置中允许录音。' : '无法启动录音，请选择音频文件。')
      }
    }

    const stopRecording = () => {
      if (mediaRecorder?.state === 'recording') mediaRecorder.stop()
      isRecording.value = false
      if (recordingTimer) window.clearInterval(recordingTimer)
      recordingTimer = null
    }

    const stopMediaStream = () => {
      mediaStream?.getTracks().forEach(track => track.stop())
      mediaStream = null
    }

    const handleAudioFile = async event => {
      const file = event.target.files?.[0]
      event.target.value = ''
      if (file) await transcribeAudio(file, file.name)
    }

    const transcribeAudio = async (blob, filename) => {
      if (blob.size > 7 * 1024 * 1024) {
        appendAssistantMessage('音频不能超过 7 MB。')
        return
      }
      voiceLoading.value = true
      voiceDraft.value = null
      try {
        const form = new FormData()
        form.append('audio', blob, filename)
        const result = await request.post('/api/v1/voice-order-drafts', form, { timeout: 15000 })
        if (result.success) voiceDraft.value = result.data
      } catch (error) {
        appendAssistantMessage(errorMessage(error, '语音识别失败，可改用文字输入。'))
      } finally {
        voiceLoading.value = false
      }
    }

    const refreshVoiceCandidates = async () => {
      if (!voiceDraft.value?.query) return
      try {
        const result = await request.post('/api/v1/recommendations', { query: voiceDraft.value.query, budget: voiceDraft.value.budget || null, usePreferences: false })
        if (result.success) voiceDraft.value.candidates = result.data || []
      } catch (error) {
        appendAssistantMessage(errorMessage(error, '重新匹配失败。'))
      }
    }

    const sendMessage = async () => {
      const text = inputMessage.value.trim()
      if (!text || isTyping.value) return
      const chatType = aiChatService.detectChatType(text)
      messages.value.push({ type: 'user', content: text, timestamp: new Date() })
      inputMessage.value = ''
      resizeComposer()
      isTyping.value = true
      await scrollToBottom()
      try {
        const result = await aiChatService.sendMessage(text, chatType, currentSessionId.value)
        if (result.success) {
          currentSessionId.value = result.data.sessionId || currentSessionId.value
          messages.value.push({ type: 'ai', content: result.data.message || '没有收到有效回复', timestamp: new Date(), processingTime: result.data.processingTime, candidates: result.data.candidates || [] })
        } else appendAssistantMessage(result.error || '智能服务暂时不可用。')
      } catch (error) {
        appendAssistantMessage(errorMessage(error, '智能服务暂时不可用，您仍可使用普通搜索。'))
      } finally {
        isTyping.value = false
        await scrollToBottom()
      }
    }

    const askQuickQuestion = question => { inputMessage.value = question; sendMessage() }
    const handleKeyDown = event => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); sendMessage() } }
    const resizeComposer = () => nextTick(() => {
      if (messageInput.value) {
        messageInput.value.style.height = 'auto'
        messageInput.value.style.height = `${Math.min(messageInput.value.scrollHeight, 120)}px`
      }
    })
    const scrollToBottom = () => nextTick(() => { if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight })
    const clearChat = () => { if (!messages.value.length || window.confirm('清空当前对话？')) { messages.value = []; currentSessionId.value = null } }

    const loadChatHistory = async () => {
      loadingHistory.value = true
      try {
        const result = await aiChatService.getChatHistory(1, 30)
        if (result.success) chatHistory.value = result.data || []
      } finally {
        loadingHistory.value = false
      }
    }

    const openHistory = () => {
      showHistory.value = true
      loadChatHistory()
    }

    const loadHistorySession = async sessionId => {
      try {
        const result = await aiChatService.getChatHistoryBySession(sessionId)
        if (result.success) {
          messages.value = (result.data || []).flatMap(item => [
            { type: 'user', content: item.userMessage, timestamp: new Date(item.createTime) },
            { type: 'ai', content: item.aiResponse, timestamp: new Date(item.createTime), processingTime: item.processingTime }
          ])
          currentSessionId.value = sessionId
          showHistory.value = false
          await scrollToBottom()
        }
      } catch (error) {
        appendAssistantMessage(errorMessage(error, '历史记录加载失败。'))
      }
    }

    const formatMessage = content => formatSafeMessage(content)
    const formatTime = value => aiChatService.formatTime(value)
    const truncateText = (text, length) => String(text || '').length > length ? `${String(text).slice(0, length)}...` : String(text || '')
    const confidenceText = value => `${Math.round(Math.max(0, Math.min(1, Number(value) || 0)) * 100)}%`

    onMounted(async () => {
      await Promise.all([loadCapabilities(), checkAiStatus()])
      messageInput.value?.focus()
    })
    onBeforeUnmount(() => {
      stopRecording()
      stopMediaStream()
      if (imagePreview.value) URL.revokeObjectURL(imagePreview.value)
    })

    return {
      router, route, tools, activeTool, currentToolLabel, openTool, goBack, capabilities, capabilitiesState, aiStatus, smartQuery, smartBudget, smartFoods, smartEmptyText, smartLoading,
      imageInput, imagePreview, imageResult, imageLoading, audioInput, voiceDraft, voiceLoading, isRecording,
      recordingSeconds, messages, inputMessage, isTyping, messagesContainer, messageInput, showHistory,
      chatHistory, loadingHistory, quickQuestions, findSmartFoods, addFoodToCart, handleImageChange, searchKeyword,
      toggleRecording, handleAudioFile, refreshVoiceCandidates, sendMessage, askQuickQuestion, handleKeyDown,
      resizeComposer, clearChat, openHistory, loadChatHistory, loadHistorySession, formatMessage, formatTime,
      truncateText, confidenceText, loadCapabilities
    }
  }
})
</script>

<style scoped>
* { box-sizing: border-box; }
.assistant-page { min-height: 100vh; background: #f4f7fa; color: #243b53; }
button, input, textarea { font: inherit; }
button { cursor: pointer; }
button:disabled { cursor: not-allowed; opacity: .55; }
.page-header { min-height: 64px; padding: 10px max(14px, calc((100% - 1080px) / 2)); display: flex; align-items: center; gap: 12px; background: #fff; border-bottom: 1px solid #dce6ee; position: sticky; top: 0; z-index: 20; }
.header-copy { flex: 1; min-width: 0; }.header-copy h1 { margin: 0; font-size: 18px; }.header-copy p { margin: 3px 0 0; color: #70879a; font-size: 12px; }
.header-actions { display: flex; align-items: center; gap: 8px; }.icon-button { width: 38px; height: 38px; border: 1px solid #d8e3eb; border-radius: 7px; background: #fff; color: #42647e; display: grid; place-items: center; flex: none; }
.service-status { display: inline-flex; align-items: center; gap: 6px; color: #667d8f; font-size: 12px; white-space: nowrap; }.service-status i { width: 7px; height: 7px; border-radius: 50%; background: #d7a137; }.service-status.online i { background: #21986f; }.service-status.offline i { background: #d85d55; }
.tool-tabs { max-width: 1080px; margin: 16px auto 0; padding: 0 14px; display: flex; gap: 2px; border-bottom: 1px solid #d8e3eb; }.tool-tabs button { min-height: 42px; padding: 0 18px; border: 0; border-bottom: 3px solid transparent; background: transparent; color: #6a8091; display: flex; align-items: center; gap: 8px; }.tool-tabs button.active { color: #0097ff; border-bottom-color: #0097ff; font-weight: 700; }
.tool-panel { max-width: 1052px; margin: 0 auto; background: #fff; border: 1px solid #d8e3eb; border-top: 0; }.tool-content { padding: 18px; }.tool-heading { display: flex; justify-content: space-between; align-items: start; gap: 16px; margin-bottom: 14px; }.tool-heading h2 { margin: 0; font-size: 16px; }.tool-heading p { margin: 4px 0 0; color: #71889a; font-size: 12px; }
.query-row, .draft-row { display: flex; align-items: end; gap: 10px; } label { min-width: 0; flex: 1; display: grid; gap: 6px; color: #60788a; font-size: 12px; } input, textarea { width: 100%; border: 1px solid #cddae4; border-radius: 6px; background: #fff; color: #243b53; padding: 9px 10px; outline: none; } input:focus, textarea:focus { border-color: #0097ff; box-shadow: 0 0 0 2px rgba(0, 151, 255, .12); }.budget-field, .quantity-field { max-width: 105px; }
.primary-button, .secondary-button, .send-button { min-height: 38px; border-radius: 6px; padding: 0 14px; display: inline-flex; justify-content: center; align-items: center; gap: 7px; white-space: nowrap; }.primary-button, .send-button { border: 1px solid #0097ff; background: #0097ff; color: #fff; }.secondary-button { border: 1px solid #cbd9e3; background: #fff; color: #42647e; }
.assistant-page :deep(.candidate-list) { display: grid; gap: 8px; margin-top: 14px; }
.assistant-page :deep(.candidate-item) { min-height: 68px; padding: 8px; display: grid; grid-template-columns: 52px minmax(0, 1fr) auto; align-items: center; gap: 10px; border: 1px solid #e0e8ee; border-radius: 7px; background: #fbfcfd; }
.assistant-page :deep(.candidate-item img), .assistant-page :deep(.food-placeholder) { width: 52px; height: 52px; border-radius: 6px; object-fit: cover; background: #e9f3f5; color: #0097ff; display: grid; place-items: center; }
.assistant-page :deep(.candidate-copy) { min-width: 0; }
.assistant-page :deep(.candidate-copy b), .assistant-page :deep(.candidate-copy span), .assistant-page :deep(.candidate-copy small) { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.assistant-page :deep(.candidate-copy b) { font-size: 14px; }
.assistant-page :deep(.candidate-copy span) { margin-top: 3px; color: #60788a; font-size: 12px; }
.assistant-page :deep(.candidate-copy small) { margin-top: 3px; color: #8597a5; font-size: 11px; }
.assistant-page :deep(.candidate-item > button) { height: 34px; padding: 0 10px; border: 1px solid #0097ff; border-radius: 6px; background: #fff; color: #0097ff; display: flex; align-items: center; gap: 6px; }
.assistant-page :deep(.candidate-empty) { min-height: 66px; border: 1px dashed #cbd8e2; color: #8599a8; display: flex; justify-content: center; align-items: center; gap: 8px; font-size: 12px; }
.media-layout { display: grid; grid-template-columns: minmax(280px, .8fr) minmax(360px, 1.2fr); gap: 18px; }.hidden-input { display: none; }.upload-zone { width: 100%; min-height: 150px; padding: 14px; border: 1px dashed #9cb8c9; border-radius: 7px; background: #f8fbfc; color: #4d6c80; display: grid; place-items: center; gap: 6px; overflow: hidden; }.upload-zone > i { color: #0097ff; font-size: 28px; }.upload-zone b { font-size: 14px; }.upload-zone span { color: #8397a5; font-size: 12px; }.upload-zone img { width: 100%; height: 180px; object-fit: contain; }.capability-note, .processing-note { margin: 10px 0 0; padding: 9px 10px; border-radius: 6px; font-size: 12px; }.capability-note { color: #9b6a1b; background: #fff7e7; border: 1px solid #f0d7a7; }.processing-note { color: #0097ff; background: #edf7ff; border: 1px solid #c9e7fb; }.recognition-summary { margin-top: 10px; padding: 10px; border-left: 3px solid #0097ff; background: #f2f9ff; }.recognition-summary strong, .recognition-summary span { display: block; }.recognition-summary span { margin-top: 3px; color: #6c8394; font-size: 11px; }.keyword-list { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }.keyword-list button { border: 1px solid #b9ddf5; border-radius: 999px; background: #fff; color: #0097ff; padding: 4px 8px; font-size: 11px; }
.capability-error { display: flex; align-items: center; gap: 7px; color: #a13d36; background: #fff1ef; border-color: #efc5c1; }.capability-error button { margin-left: auto; border: 0; background: transparent; color: #0097ff; font-weight: 700; }
.voice-actions { display: flex; gap: 8px; }.record-button { min-height: 44px; flex: 1; border: 1px solid #0097ff; border-radius: 6px; background: #edf7ff; color: #0097ff; display: flex; justify-content: center; align-items: center; gap: 8px; }.record-button.recording { border-color: #d85d55; background: #fff0ef; color: #c84740; }.voice-draft { margin-top: 12px; display: grid; gap: 10px; }.voice-draft textarea { min-height: 70px; resize: vertical; }
.chat-shell { max-width: 1052px; height: min(560px, calc(100vh - 330px)); min-height: 420px; margin: 16px auto 28px; display: flex; flex-direction: column; background: #fff; border: 1px solid #d8e3eb; }.message-list { flex: 1; min-height: 0; overflow-y: auto; padding: 18px; }.empty-chat { min-height: 100%; display: grid; align-content: center; justify-items: center; color: #71889a; text-align: center; }.empty-chat > i { font-size: 32px; color: #0097ff; }.empty-chat h2 { margin: 10px 0 3px; color: #314d62; font-size: 17px; }.empty-chat p { margin: 0; font-size: 12px; }.quick-list { margin-top: 16px; display: flex; flex-wrap: wrap; justify-content: center; gap: 7px; }.quick-list button { border: 1px solid #d7e2ea; border-radius: 999px; background: #fff; color: #526d80; padding: 7px 10px; font-size: 12px; }
.message { display: flex; gap: 9px; margin-bottom: 14px; }.message.user { flex-direction: row-reverse; }.message-avatar { width: 32px; height: 32px; border-radius: 7px; display: grid; place-items: center; flex: none; background: #e8f4fb; color: #0097ff; }.message.user .message-avatar { background: #e9f5f2; color: #0097ff; }.message-body { max-width: min(76%, 720px); }.message.user .message-body { text-align: right; }.message-bubble { padding: 10px 12px; border-radius: 7px; background: #f2f6f9; color: #314d62; line-height: 1.6; text-align: left; font-size: 13px; overflow-wrap: anywhere; }.message.user .message-bubble { background: #0097ff; color: #fff; }.message-body time { display: block; margin-top: 4px; color: #94a4af; font-size: 10px; }.typing { display: flex; gap: 4px; }.typing i { width: 6px; height: 6px; border-radius: 50%; background: #8ba1af; animation: pulse 1s infinite alternate; }.typing i:nth-child(2) { animation-delay: .2s; }.typing i:nth-child(3) { animation-delay: .4s; }
.chat-composer { padding: 10px; border-top: 1px solid #e0e8ee; }.chat-composer textarea { max-height: 120px; resize: none; }.composer-actions { min-height: 42px; padding-top: 7px; display: flex; justify-content: flex-end; align-items: center; gap: 7px; }.composer-actions > span { margin-right: auto; color: #93a4af; font-size: 11px; }.send-button { min-width: 86px; }
.drawer-mask { position: fixed; inset: 0; z-index: 50; background: rgba(20, 38, 52, .45); display: flex; justify-content: flex-end; }.history-drawer { width: min(380px, 92vw); height: 100%; padding: 16px; background: #fff; box-shadow: -6px 0 24px rgba(20, 38, 52, .15); }.history-drawer > header { display: flex; justify-content: space-between; align-items: center; }.history-drawer h2 { margin: 0; font-size: 17px; }.history-refresh { width: 100%; margin: 14px 0; }.history-list { display: grid; gap: 7px; }.history-list > button { width: 100%; padding: 11px; border: 1px solid #e0e8ee; border-radius: 7px; background: #fbfcfd; color: #405f75; display: flex; align-items: center; text-align: left; }.history-list span { min-width: 0; flex: 1; }.history-list b, .history-list time { display: block; }.history-list b { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }.history-list time { margin-top: 4px; color: #91a2ad; font-size: 10px; }.history-empty { color: #8ba0ae; text-align: center; padding: 50px 0; }
@keyframes pulse { to { opacity: .3; transform: translateY(-2px); } }
@media (max-width: 760px) {
  .page-header { padding: 9px 12px; }.header-copy p, .service-status { display: none; }.tool-tabs { margin-top: 10px; padding: 0 8px; }.tool-tabs button { flex: 1; justify-content: center; padding: 0 6px; font-size: 12px; }.tool-panel { margin: 0 8px; }.tool-content { padding: 14px; }.media-layout { grid-template-columns: 1fr; }.query-row, .draft-row { flex-wrap: wrap; }.query-row label:first-child, .draft-row label:first-child { flex-basis: 100%; }.budget-field, .quantity-field { max-width: none; }.query-row .primary-button, .draft-row .secondary-button { flex: 1; }.assistant-page :deep(.candidate-item) { grid-template-columns: 44px minmax(0, 1fr) auto; }.assistant-page :deep(.candidate-item img), .assistant-page :deep(.food-placeholder) { width: 44px; height: 44px; }.assistant-page :deep(.candidate-item > button span) { display: none; }.chat-shell { height: 520px; min-height: 0; margin: 10px 8px 18px; }.message-list { padding: 12px; }.message-body { max-width: 84%; }
}

/* AI customer service follows the customer-facing delivery-blue palette. */
.ai-topbar { min-height: 64px; padding: 10px max(14px, calc((100% - 1080px) / 2)); display: flex; align-items: center; gap: 12px; position: sticky; top: 0; z-index: 30; color: #fff; background: #0097ff; box-shadow: 0 3px 12px rgba(0, 112, 204, .2); }
.topbar-icon { width: 40px; height: 40px; flex: none; display: grid; place-items: center; border: 0; border-radius: 7px; color: #fff; background: rgba(255, 255, 255, .14); }
.topbar-icon:hover { background: rgba(255, 255, 255, .24); }
.topbar-title { flex: 1; min-width: 0; }
.topbar-title h1 { margin: 0; color: #fff; font-size: 20px; font-weight: 700; }
.topbar-title span { display: block; margin-top: 2px; color: rgba(255, 255, 255, .82); font-size: 12px; }
.topbar-actions { display: flex; align-items: center; gap: 10px; }
.ai-topbar .service-status { color: #fff; }
.ai-topbar .service-status i { background: #ffe28a; box-shadow: 0 0 0 3px rgba(255, 255, 255, .18); }
.ai-topbar .service-status.online i { background: #8ff0bf; }
.ai-topbar .service-status.offline i { background: #ffd0cd; }
.page-content { width: min(1080px, calc(100% - 28px)); margin: 0 auto; padding: 24px 0 30px; }
.feature-bubbles { position: relative; z-index: 2; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; width: min(100%, 960px); margin: 0 auto -13px; padding: 0 14px; }
.feature-bubbles button { min-width: 0; min-height: 82px; padding: 13px 14px; display: grid; grid-template-columns: 42px minmax(0, 1fr) 14px; align-items: center; gap: 10px; border: 1px solid #cae7fb; border-radius: 8px; color: #294b66; background: #fff; box-shadow: 0 7px 20px rgba(22, 91, 139, .11); text-align: left; transition: transform .2s ease, border-color .2s ease, box-shadow .2s ease; }
.feature-bubbles button:hover { border-color: #76c8f8; transform: translateY(-2px); box-shadow: 0 9px 24px rgba(0, 126, 214, .16); }
.feature-icon { width: 42px; height: 42px; display: grid; place-items: center; border-radius: 8px; color: #0097ff; background: #e9f6ff; font-size: 18px; }
.feature-copy { min-width: 0; }
.feature-copy b, .feature-copy small { display: block; letter-spacing: 0; }
.feature-copy b { color: #1f425e; font-size: 14px; line-height: 1.35; }
.feature-copy small { margin-top: 4px; color: #7690a4; font-size: 11px; line-height: 1.35; }
.feature-arrow { color: #9ec9e5; font-size: 11px; }
.feature-switcher { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; margin-bottom: 14px; }
.feature-switcher button { min-height: 46px; padding: 0 10px; display: flex; align-items: center; justify-content: center; gap: 7px; border: 1px solid #d5e5f0; border-radius: 7px; color: #55748c; background: #fff; }
.feature-switcher button:hover, .feature-switcher button.active { border-color: #0097ff; color: #0097ff; background: #edf7ff; }
.tool-panel { border: 1px solid #d8e8f3; border-radius: 8px; box-shadow: 0 7px 24px rgba(31, 82, 118, .08); overflow: hidden; }
.tool-content { min-height: 360px; padding: 24px; }
.chat-shell { width: min(100%, 932px); height: min(680px, calc(100vh - 190px)); min-height: 500px; margin: 0 auto; padding-top: 18px; border-color: #d8e8f3; border-radius: 8px; box-shadow: 0 8px 26px rgba(31, 82, 118, .09); overflow: hidden; }
.empty-chat > i { width: 54px; height: 54px; display: grid; place-items: center; border-radius: 8px; background: #e9f6ff; }
.empty-chat h2 { color: #1f425e; }
.quick-list button:hover { border-color: #83ccf7; color: #0088e6; background: #f3faff; }
.message.user .message-avatar { background: #e9f6ff; color: #0097ff; }
.message.user .message-bubble { background: #0097ff; }
.chat-composer { background: #fff; box-shadow: 0 -5px 16px rgba(37, 85, 118, .04); }
.chat-composer textarea { min-height: 42px; border-color: #cbdfea; background: #fafdff; }
.send-button, .primary-button { border-color: #0097ff; background: #0097ff; }
.send-button:hover, .primary-button:hover { background: #0086e2; }
:global(html[data-theme="dark"]) .assistant-page { background: #101b28; }
:global(html[data-theme="dark"]) .feature-bubbles button, :global(html[data-theme="dark"]) .feature-switcher button, :global(html[data-theme="dark"]) .tool-panel, :global(html[data-theme="dark"]) .chat-shell, :global(html[data-theme="dark"]) .chat-composer { background: #17283a; border-color: #29445a; }
:global(html[data-theme="dark"]) .feature-copy b { color: #e7f0f7; }
@media (max-width: 760px) {
  .ai-topbar { min-height: 58px; padding: 8px 10px; }
  .topbar-title h1 { font-size: 18px; }
  .topbar-title span, .ai-topbar .service-status { display: none; }
  .page-content { width: 100%; padding: 15px 8px 20px; }
  .feature-bubbles { grid-template-columns: 1fr; gap: 8px; width: 100%; margin-bottom: 10px; padding: 0; }
  .feature-bubbles button { min-height: 66px; padding: 9px 11px; grid-template-columns: 38px minmax(0, 1fr) 12px; box-shadow: 0 4px 13px rgba(22, 91, 139, .09); }
  .feature-icon { width: 38px; height: 38px; }
  .feature-switcher { grid-template-columns: repeat(4, minmax(64px, 1fr)); gap: 5px; overflow-x: auto; }
  .feature-switcher button { min-height: 44px; padding: 5px 4px; flex-direction: column; gap: 3px; font-size: 10px; }
  .tool-panel { margin: 0; }
  .tool-content { min-height: 0; padding: 15px; }
  .chat-shell { width: 100%; height: 560px; min-height: 0; margin: 0; padding-top: 0; }
}
</style>
