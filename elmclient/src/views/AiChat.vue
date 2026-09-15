<template>
  <div class="assistant-page">
    <header class="ai-topbar">
      <div class="topbar-row">
        <button class="topbar-icon" type="button" title="返回" @click="goBack"><i class="fa fa-arrow-left"></i></button>
        <div class="topbar-title">
          <h1>AI智能助手</h1>
          <span v-if="activeTool !== 'chat'">{{ currentToolLabel }}</span>
        </div>
        <div class="topbar-actions">
          <span class="service-status" :class="aiStatus.type"><i></i><span>{{ aiStatus.text }}</span></span>
          <button class="topbar-icon cart-button" type="button" title="购物车" @click="router.push('/cart')"><i class="fa fa-shopping-cart"></i></button>
        </div>
      </div>
    </header>

    <div class="page-content">
      <nav v-if="activeTool !== 'chat'" class="feature-switcher" aria-label="切换AI功能">
        <button type="button" @click="router.replace('/ai-chat')"><i class="fa fa-comments-o"></i><span>问答客服</span></button>
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
          <div class="ai-hero">
            <div class="ai-hero-copy">
              <h2>今天想吃什么？</h2>
              <span class="hero-underline"></span>
              <p>告诉我预算、口味和场景，我帮你从附近美食里挑</p>
            </div>
            <div class="ai-robot" aria-hidden="true">
              <span class="robot-antenna"></span>
              <span class="robot-head"><i class="robot-eye robot-eye-left"></i><i class="robot-eye robot-eye-right"></i><b></b></span>
              <span class="robot-ear robot-ear-left"></span><span class="robot-ear robot-ear-right"></span>
              <span class="robot-body"><i class="fa fa-cutlery"></i></span>
              <span class="robot-ray ray-one"></span><span class="robot-ray ray-two"></span><span class="robot-ray ray-three"></span>
            </div>
          </div>
          <div class="quick-grid" aria-label="快捷点餐需求">
            <button v-for="action in quickActions" :key="action.label" type="button" :class="`quick-action quick-action-${action.tone}`" @click="runQuickAction(action)">
              <span class="quick-action-icon"><i :class="action.icon"></i></span><strong>{{ action.label }}</strong>
            </button>
          </div>
          <div class="assistant-welcome">
            <div class="mini-robot"><i class="fa fa-robot"></i></div>
            <p>Hi，我是你的美食助手！<br>我可以帮你推荐附近好吃的、查看菜品信息、对比价格，还能根据你的场景和口味做个性化推荐~</p>
          </div>
          <div class="sample-reply">
            <span>20元以内推荐点什么？</span><i class="fa fa-user"></i>
          </div>
          <div class="recommendation-preview">
            <div class="preview-message"><div class="mini-robot"><i class="fa fa-robot"></i></div><p>好的！我为你找到了一些高性价比美食，离你近、配送快、评价也很不错：</p></div>
            <div class="landing-food-grid">
              <article v-for="food in landingFoods" :key="food.foodId || food.foodName" class="landing-food-card">
                <div class="landing-food-image">
                  <img :src="food.foodImg" :alt="food.foodName" @error="handleLandingImageError">
                  <span><i class="fa fa-map-marker"></i> {{ food.distance }}</span>
                </div>
                <div class="landing-food-info">
                  <h3>{{ food.foodName }}</h3>
                  <strong class="landing-food-price"><small>¥</small>{{ Number(food.price).toFixed(1) }}</strong>
                  <div class="landing-food-meta"><span><i class="fa fa-star"></i> {{ food.rating }}</span><em>店铺销量 {{ food.sales }}</em></div>
                  <div class="landing-food-footer"><span>{{ food.businessName }}</span><button type="button" :aria-label="`加入${food.foodName}`" @click="addLandingFood(food)"><i class="fa fa-plus"></i></button></div>
                </div>
              </article>
            </div>
          </div>
          <div class="filter-rail" aria-label="推荐筛选">
            <button type="button" @click="runQuickAction({ mode: 'refresh', query: '推荐附近热门菜品' })"><i class="fa fa-refresh"></i>换一批</button>
            <button type="button" @click="runQuickAction({ mode: 'chat', query: '请按评分从高到低推荐附近菜品' })"><i class="fa fa-star"></i>按评分排序</button>
            <button type="button" @click="runQuickAction({ mode: 'chat', query: '只推荐品牌店铺' })"><i class="fa fa-shopping-bag"></i>只看品牌店</button>
            <button type="button" @click="runQuickAction({ mode: 'recommend', query: '有优惠的菜品' })"><i class="fa fa-tag"></i>有优惠的</button>
          </div>
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
        <div class="composer-main">
          <button class="composer-mic" type="button" title="语音点单" @click="router.replace('/ai-chat/voice')"><i class="fa fa-microphone"></i></button>
          <textarea ref="messageInput" v-model="inputMessage" rows="1" maxlength="500" placeholder="继续问问口味、预算或场景..." :disabled="isTyping" @keydown="handleKeyDown" @input="resizeComposer"></textarea>
          <button class="send-button" type="button" :disabled="!inputMessage.trim() || isTyping" @click="sendMessage"><i class="fa fa-paper-plane"></i><span>发送</span></button>
        </div>
        <div class="composer-actions">
          <span>{{ inputMessage.length }}/500</span>
          <button class="icon-button" type="button" title="历史记录" @click="openHistory"><i class="fa fa-history"></i></button>
          <button class="icon-button image-shortcut" type="button" title="图片识别" @click="router.replace('/ai-chat/image')"><i class="fa fa-image"></i></button>
          <button class="icon-button" type="button" title="清空当前对话" @click="clearChat"><i class="fa fa-trash"></i></button>
        </div>
      </footer>
      </main>
    </div>

    <nav class="ai-bottom-nav" aria-label="底部导航">
      <button type="button" @click="router.push('/index')"><i class="fa fa-home"></i><span>首页</span></button>
      <button type="button" @click="router.push('/orderList')"><i class="fa fa-file-text-o"></i><span>订单</span></button>
      <button class="active" type="button" @click="router.replace('/ai-chat')"><i class="fa fa-robot"></i><span>AI助手</span></button>
      <button type="button" @click="router.push('/myInformation')"><i class="fa fa-user"></i><span>我的</span></button>
    </nav>

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
import { navigateAssistantBack } from '../utils/backNavigation'

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

const defaultLandingFoods = []

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
      router.replace(tool.path)
    }
    const goBack = () => navigateAssistantBack(router, route)
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
    const quickActions = [
      { label: '20元内吃什么', tone: 'budget', icon: 'fa fa-jpy', mode: 'recommend', query: '20元以内推荐附近高性价比美食' },
      { label: '附近评分高', tone: 'rating', icon: 'fa fa-thumbs-up', mode: 'chat', query: '请推荐附近评分高的美食' },
      { label: '减脂轻食', tone: 'healthy', icon: 'fa fa-leaf', mode: 'recommend', query: '推荐减脂轻食' },
      { label: '宿舍夜宵', tone: 'night', icon: 'fa fa-moon-o', mode: 'chat', query: '推荐适合宿舍夜宵的美食' },
      { label: '帮我点奶茶', tone: 'drink', icon: 'fa fa-glass', mode: 'chat', query: '帮我推荐一杯附近好喝的奶茶' },
      { label: '查看最近订单', tone: 'orders', icon: 'fa fa-file-text-o', mode: 'chat', query: '查看我的最近订单' }
    ]
    const landingFoods = ref(defaultLandingFoods.map(food => ({ ...food })))
    let mediaRecorder = null
    let mediaStream = null
    let recordingTimer = null
    let audioChunks = []
    let disposed = false
    let requestingMicrophone = false

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

    const normalizeLandingFood = food => {
      const score = Number(food.businessScore ?? food.rating ?? food.score)
      return {
        ...food,
        foodName: food.foodName || '商品',
        price: Number(food.price ?? food.foodPrice ?? 0),
        rating: score > 0 ? score.toFixed(1) : '暂无评分',
        sales: String(food.businessSalesCount ?? food.salesCount ?? '—'),
        businessName: food.businessName || '商家',
        distance: food.distanceText || '校园周边',
        foodImg: food.foodImg || '/images/foods/04-noodles.jpg'
      }
    }

    const loadLandingFoods = async () => {
      try {
        const result = await request.post('/api/v1/recommendations', { query: '附近高性价比美食', budget: 25, usePreferences: true })
        if (result.success && Array.isArray(result.data) && result.data.length) {
          landingFoods.value = result.data.slice(0, 3).map(normalizeLandingFood)
        }
      } catch (_) {
        landingFoods.value = [];
      }
    }

    const runQuickAction = async action => {
      const query = action.query || action.label || '推荐一些附近美食'
      if (action.mode === 'recommend') {
        smartQuery.value = query
        smartBudget.value = action.tone === 'budget' ? 20 : null
        activeTool.value = 'recommend'
        await router.replace('/ai-chat/recommend')
        await findSmartFoods()
        return
      }
      if (action.mode === 'refresh') {
        await runQuickAction({ mode: 'recommend', query: '推荐附近热门菜品' })
        return
      }
      inputMessage.value = query
      await sendMessage()
    }

    const addLandingFood = async food => {
      if (food.foodId) {
        await addFoodToCart(food)
        return
      }
      await runQuickAction({ mode: 'chat', query: `我想了解${food.foodName}，请告诉我附近有没有类似的在售菜品` })
    }

    const handleLandingImageError = event => {
      const image = event?.target
      if (!image || image.dataset.fallbackApplied === 'true') return
      image.dataset.fallbackApplied = 'true'
      image.src = '/images/foods/04-noodles.jpg'
    }

    const addFoodToCart = async (food, quantity = 1) => {
      const safeQuantity = Number(quantity)
      if (!Number.isInteger(safeQuantity) || safeQuantity < 1 || safeQuantity > 99) {
        appendAssistantMessage('数量必须是 1 到 99 之间的整数。')
        return
      }
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
      router.replace('/ai-chat/recommend')
      findSmartFoods()
    }

    const preferredAudioType = () => {
      if (!window.MediaRecorder) return ''
      return ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4'].find(type => MediaRecorder.isTypeSupported(type)) || ''
    }

    const toggleRecording = async () => {
      if (disposed || requestingMicrophone || voiceLoading.value) return
      if (isRecording.value) return stopRecording()
      if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
        appendAssistantMessage('当前浏览器不支持录音，请选择已有音频文件。')
        return
      }
      requestingMicrophone = true
      try {
        mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })
        if (disposed) {
          stopMediaStream()
          return
        }
        const mimeType = preferredAudioType()
        const recorder = mimeType ? new MediaRecorder(mediaStream, { mimeType }) : new MediaRecorder(mediaStream)
        mediaRecorder = recorder
        audioChunks = []
        recorder.ondataavailable = event => { if (event.data.size) audioChunks.push(event.data) }
        recorder.onstop = async () => {
          const blob = new Blob(audioChunks, { type: recorder.mimeType || 'audio/webm' })
          stopMediaStream()
          if (disposed) return
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
        if (!disposed) appendAssistantMessage(error?.name === 'NotAllowedError' ? '没有麦克风权限，请在浏览器设置中允许录音。' : '无法启动录音，请选择音频文件。')
      } finally {
        requestingMicrophone = false
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
      if (disposed) return
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
      const quantity = Number(voiceDraft.value.quantity)
      if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99) {
        appendAssistantMessage('数量必须是 1 到 99 之间的整数。')
        return
      }
      try {
        const result = await request.post('/api/v1/recommendations', { query: voiceDraft.value.query, quantity, budget: voiceDraft.value.budget || null, usePreferences: false })
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
      await Promise.all([loadCapabilities(), checkAiStatus(), loadLandingFoods()])
      messageInput.value?.focus()
    })
    onBeforeUnmount(() => {
      disposed = true
      stopRecording()
      stopMediaStream()
      if (imagePreview.value) URL.revokeObjectURL(imagePreview.value)
    })

    return {
      router, route, tools, activeTool, currentToolLabel, openTool, goBack, capabilities, capabilitiesState, aiStatus, smartQuery, smartBudget, smartFoods, smartEmptyText, smartLoading,
      imageInput, imagePreview, imageResult, imageLoading, audioInput, voiceDraft, voiceLoading, isRecording,
      recordingSeconds, messages, inputMessage, isTyping, messagesContainer, messageInput, showHistory,
      chatHistory, loadingHistory, quickQuestions, quickActions, landingFoods, findSmartFoods, runQuickAction, addLandingFood, handleLandingImageError, addFoodToCart, handleImageChange, searchKeyword,
      toggleRecording, handleAudioFile, refreshVoiceCandidates, sendMessage, askQuickQuestion, handleKeyDown,
      resizeComposer, clearChat, openHistory, loadChatHistory, loadHistorySession, formatMessage, formatTime,
      truncateText, confidenceText, loadCapabilities
    }
  }
})
</script>

<style scoped>
* { box-sizing: border-box; }
.assistant-page { min-height: 100vh; background: var(--fwl-surface, #f4f7fa); color: var(--fwl-ink, #243b53); }
button, input, textarea { font: inherit; }
button { cursor: pointer; }
button:disabled { cursor: not-allowed; opacity: .55; }
.page-header { min-height: 64px; padding: 10px max(14px, calc((100% - 1080px) / 2)); display: flex; align-items: center; gap: 12px; background: #fff; border-bottom: 1px solid var(--fwl-border, #dce6ee); position: sticky; top: 0; z-index: 20; }
.header-copy { flex: 1; min-width: 0; }.header-copy h1 { margin: 0; font-size: 18px; }.header-copy p { margin: 3px 0 0; color: var(--fwl-muted, #70879a); font-size: 12px; }
.header-actions { display: flex; align-items: center; gap: 8px; }.icon-button { width: 38px; height: 38px; border: 1px solid var(--fwl-border, #d8e3eb); border-radius: 7px; background: #fff; color: var(--fwl-muted, #42647e); display: grid; place-items: center; flex: none; }
.service-status { display: inline-flex; align-items: center; gap: 6px; color: var(--fwl-muted, #667d8f); font-size: 12px; white-space: nowrap; }.service-status i { width: 7px; height: 7px; border-radius: 50%; background: #d7a137; }.service-status.online i { background: #21986f; }.service-status.offline i { background: #d85d55; }
.tool-tabs { max-width: 1080px; margin: 16px auto 0; padding: 0 14px; display: flex; gap: 2px; border-bottom: 1px solid var(--fwl-border, #d8e3eb); }.tool-tabs button { min-height: 42px; padding: 0 18px; border: 0; border-bottom: 3px solid transparent; background: transparent; color: var(--fwl-muted, #6a8091); display: flex; align-items: center; gap: 8px; }.tool-tabs button.active { color: var(--fwl-brand, #0097ff); border-bottom-color: var(--fwl-brand, #0097ff); font-weight: 700; }
.tool-panel { max-width: 1052px; margin: 0 auto; background: #fff; border: 1px solid var(--fwl-border, #d8e3eb); border-top: 0; }.tool-content { padding: 18px; }.tool-heading { display: flex; justify-content: space-between; align-items: start; gap: 16px; margin-bottom: 14px; }.tool-heading h2 { margin: 0; font-size: 16px; }.tool-heading p { margin: 4px 0 0; color: var(--fwl-muted, #71889a); font-size: 12px; }
.query-row, .draft-row { display: flex; align-items: end; gap: 10px; } label { min-width: 0; flex: 1; display: grid; gap: 6px; color: var(--fwl-muted, #60788a); font-size: 12px; } input, textarea { width: 100%; border: 1px solid var(--fwl-border, #cddae4); border-radius: 6px; background: #fff; color: var(--fwl-ink, #243b53); padding: 9px 10px; outline: none; } input:focus, textarea:focus { border-color: var(--fwl-brand, #0097ff); box-shadow: 0 0 0 2px rgba(var(--fwl-brand-rgb, 0, 151, 255), 0.12); }.budget-field, .quantity-field { max-width: 105px; }
.primary-button, .secondary-button, .send-button { min-height: 38px; border-radius: 6px; padding: 0 14px; display: inline-flex; justify-content: center; align-items: center; gap: 7px; white-space: nowrap; }.primary-button, .send-button { border: 1px solid var(--fwl-brand, #0097ff); background: var(--fwl-brand, #0097ff); color: #fff; }.secondary-button { border: 1px solid var(--fwl-border, #cbd9e3); background: #fff; color: var(--fwl-muted, #42647e); }
.assistant-page :deep(.candidate-list) { display: grid; gap: 8px; margin-top: 14px; }
.assistant-page :deep(.candidate-item) { min-height: 68px; padding: 8px; display: grid; grid-template-columns: 52px minmax(0, 1fr) auto; align-items: center; gap: 10px; border: 1px solid var(--fwl-border, #e0e8ee); border-radius: 7px; background: var(--fwl-surface, #fbfcfd); }
.assistant-page :deep(.candidate-item img), .assistant-page :deep(.food-placeholder) { width: 52px; height: 52px; border-radius: 6px; object-fit: cover; background: var(--fwl-border, #e9f3f5); color: var(--fwl-brand, #0097ff); display: grid; place-items: center; }
.assistant-page :deep(.candidate-copy) { min-width: 0; }
.assistant-page :deep(.candidate-copy b), .assistant-page :deep(.candidate-copy span), .assistant-page :deep(.candidate-copy small) { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.assistant-page :deep(.candidate-copy b) { font-size: 14px; }
.assistant-page :deep(.candidate-copy span) { margin-top: 3px; color: var(--fwl-muted, #60788a); font-size: 12px; }
.assistant-page :deep(.candidate-copy small) { margin-top: 3px; color: var(--fwl-muted, #8597a5); font-size: 11px; }
.assistant-page :deep(.candidate-item > button) { height: 34px; padding: 0 10px; border: 1px solid var(--fwl-brand, #0097ff); border-radius: 6px; background: #fff; color: var(--fwl-brand, #0097ff); display: flex; align-items: center; gap: 6px; }
.assistant-page :deep(.candidate-empty) { min-height: 66px; border: 1px dashed var(--fwl-border, #cbd8e2); color: var(--fwl-muted, #8599a8); display: flex; justify-content: center; align-items: center; gap: 8px; font-size: 12px; }
.media-layout { display: grid; grid-template-columns: minmax(280px, .8fr) minmax(360px, 1.2fr); gap: 18px; }.hidden-input { display: none; }.upload-zone { width: 100%; min-height: 150px; padding: 14px; border: 1px dashed var(--fwl-subtle, #9cb8c9); border-radius: 7px; background: var(--fwl-surface, #f8fbfc); color: var(--fwl-muted, #4d6c80); display: grid; place-items: center; gap: 6px; overflow: hidden; }.upload-zone > i { color: var(--fwl-brand, #0097ff); font-size: 28px; }.upload-zone b { font-size: 14px; }.upload-zone span { color: var(--fwl-muted, #8397a5); font-size: 12px; }.upload-zone img { width: 100%; height: 180px; object-fit: contain; }.capability-note, .processing-note { margin: 10px 0 0; padding: 9px 10px; border-radius: 6px; font-size: 12px; }.capability-note { color: #9b6a1b; background: #fff7e7; border: 1px solid #f0d7a7; }.processing-note { color: var(--fwl-brand, #0097ff); background: var(--fwl-surface, #edf7ff); border: 1px solid var(--fwl-border, #c9e7fb); }.recognition-summary { margin-top: 10px; padding: 10px; border-left: 3px solid var(--fwl-brand, #0097ff); background: var(--fwl-surface, #f2f9ff); }.recognition-summary strong, .recognition-summary span { display: block; }.recognition-summary span { margin-top: 3px; color: var(--fwl-muted, #6c8394); font-size: 11px; }.keyword-list { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }.keyword-list button { border: 1px solid var(--fwl-border, #b9ddf5); border-radius: 999px; background: #fff; color: var(--fwl-brand, #0097ff); padding: 4px 8px; font-size: 11px; }
.capability-error { display: flex; align-items: center; gap: 7px; color: #a13d36; background: #fff1ef; border-color: #efc5c1; }.capability-error button { margin-left: auto; border: 0; background: transparent; color: var(--fwl-brand, #0097ff); font-weight: 700; }
.voice-actions { display: flex; gap: 8px; }.record-button { min-height: 44px; flex: 1; border: 1px solid var(--fwl-brand, #0097ff); border-radius: 6px; background: var(--fwl-surface, #edf7ff); color: var(--fwl-brand, #0097ff); display: flex; justify-content: center; align-items: center; gap: 8px; }.record-button.recording { border-color: #d85d55; background: #fff0ef; color: #c84740; }.voice-draft { margin-top: 12px; display: grid; gap: 10px; }.voice-draft textarea { min-height: 70px; resize: vertical; }
.chat-shell { max-width: 1052px; height: min(560px, calc(100vh - 330px)); min-height: 420px; margin: 16px auto 28px; display: flex; flex-direction: column; background: #fff; border: 1px solid var(--fwl-border, #d8e3eb); }.message-list { flex: 1; min-height: 0; overflow-y: auto; padding: 18px; }.empty-chat { min-height: 100%; display: grid; align-content: center; justify-items: center; color: var(--fwl-muted, #71889a); text-align: center; }.empty-chat > i { font-size: 32px; color: var(--fwl-brand, #0097ff); }.empty-chat h2 { margin: 10px 0 3px; color: var(--fwl-ink, #314d62); font-size: 17px; }.empty-chat p { margin: 0; font-size: 12px; }.quick-list { margin-top: 16px; display: flex; flex-wrap: wrap; justify-content: center; gap: 7px; }.quick-list button { border: 1px solid var(--fwl-border, #d7e2ea); border-radius: 999px; background: #fff; color: var(--fwl-muted, #526d80); padding: 7px 10px; font-size: 12px; }
.message { display: flex; gap: 9px; margin-bottom: 14px; }.message.user { flex-direction: row-reverse; }.message-avatar { width: 32px; height: 32px; border-radius: 7px; display: grid; place-items: center; flex: none; background: var(--fwl-surface, #e8f4fb); color: var(--fwl-brand, #0097ff); }.message.user .message-avatar { background: #e9f5f2; color: var(--fwl-brand, #0097ff); }.message-body { max-width: min(76%, 720px); }.message.user .message-body { text-align: right; }.message-bubble { padding: 10px 12px; border-radius: 7px; background: var(--fwl-surface, #f2f6f9); color: var(--fwl-ink, #314d62); line-height: 1.6; text-align: left; font-size: 13px; overflow-wrap: anywhere; }.message.user .message-bubble { background: var(--fwl-brand, #0097ff); color: #fff; }.message-body time { display: block; margin-top: 4px; color: var(--fwl-muted, #94a4af); font-size: 10px; }.typing { display: flex; gap: 4px; }.typing i { width: 6px; height: 6px; border-radius: 50%; background: var(--fwl-muted, #8ba1af); animation: pulse 1s infinite alternate; }.typing i:nth-child(2) { animation-delay: .2s; }.typing i:nth-child(3) { animation-delay: .4s; }
.chat-composer { padding: 10px; border-top: 1px solid var(--fwl-border, #e0e8ee); }.chat-composer textarea { max-height: 120px; resize: none; }.composer-actions { min-height: 42px; padding-top: 7px; display: flex; justify-content: flex-end; align-items: center; gap: 7px; }.composer-actions > span { margin-right: auto; color: var(--fwl-muted, #93a4af); font-size: 11px; }.send-button { min-width: 86px; }
.drawer-mask { position: fixed; inset: 0; z-index: 50; background: rgba(var(--fwl-ink-rgb, 20, 38, 52), 0.45); display: flex; justify-content: flex-end; }.history-drawer { width: min(380px, 92vw); height: 100%; padding: 16px; background: #fff; box-shadow: -6px 0 24px rgba(var(--fwl-ink-rgb, 20, 38, 52), 0.15); }.history-drawer > header { display: flex; justify-content: space-between; align-items: center; }.history-drawer h2 { margin: 0; font-size: 17px; }.history-refresh { width: 100%; margin: 14px 0; }.history-list { display: grid; gap: 7px; }.history-list > button { width: 100%; padding: 11px; border: 1px solid var(--fwl-border, #e0e8ee); border-radius: 7px; background: var(--fwl-surface, #fbfcfd); color: var(--fwl-ink, #405f75); display: flex; align-items: center; text-align: left; }.history-list span { min-width: 0; flex: 1; }.history-list b, .history-list time { display: block; }.history-list b { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }.history-list time { margin-top: 4px; color: var(--fwl-muted, #91a2ad); font-size: 10px; }.history-empty { color: var(--fwl-muted, #8ba0ae); text-align: center; padding: 50px 0; }
@keyframes pulse { to { opacity: .3; transform: translateY(-2px); } }
@media (max-width: 760px) {
  .page-header { padding: 9px 12px; }.header-copy p, .service-status { display: none; }.tool-tabs { margin-top: 10px; padding: 0 8px; }.tool-tabs button { flex: 1; justify-content: center; padding: 0 6px; font-size: 12px; }.tool-panel { margin: 0 8px; }.tool-content { padding: 14px; }.media-layout { grid-template-columns: 1fr; }.query-row, .draft-row { flex-wrap: wrap; }.query-row label:first-child, .draft-row label:first-child { flex-basis: 100%; }.budget-field, .quantity-field { max-width: none; }.query-row .primary-button, .draft-row .secondary-button { flex: 1; }.assistant-page :deep(.candidate-item) { grid-template-columns: 44px minmax(0, 1fr) auto; }.assistant-page :deep(.candidate-item img), .assistant-page :deep(.food-placeholder) { width: 44px; height: 44px; }.assistant-page :deep(.candidate-item > button span) { display: none; }.chat-shell { height: 520px; min-height: 0; margin: 10px 8px 18px; }.message-list { padding: 12px; }.message-body { max-width: 84%; }
}

/* AI customer service follows the customer-facing delivery-blue palette. */
.ai-topbar { min-height: 64px; padding: 10px max(14px, calc((100% - 1080px) / 2)); display: flex; align-items: center; gap: 12px; position: sticky; top: 0; z-index: 30; color: #fff; background: var(--fwl-brand, #0097ff); box-shadow: 0 3px 12px rgba(var(--fwl-brand-rgb, 0, 112, 204), 0.2); }
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
.feature-bubbles button { min-width: 0; min-height: 82px; padding: 13px 14px; display: grid; grid-template-columns: 42px minmax(0, 1fr) 14px; align-items: center; gap: 10px; border: 1px solid var(--fwl-border, #cae7fb); border-radius: 8px; color: var(--fwl-ink, #294b66); background: #fff; box-shadow: 0 7px 20px rgba(var(--fwl-brand-strong-rgb, 22, 91, 139), 0.11); text-align: left; transition: transform .2s ease, border-color .2s ease, box-shadow .2s ease; }
.feature-bubbles button:hover { border-color: var(--fwl-brand-soft, #76c8f8); transform: translateY(-2px); box-shadow: 0 9px 24px rgba(var(--fwl-brand-rgb, 0, 126, 214), 0.16); }
.feature-icon { width: 42px; height: 42px; display: grid; place-items: center; border-radius: 8px; color: var(--fwl-brand, #0097ff); background: var(--fwl-surface, #e9f6ff); font-size: 18px; }
.feature-copy { min-width: 0; }
.feature-copy b, .feature-copy small { display: block; letter-spacing: 0; }
.feature-copy b { color: var(--fwl-brand-strong, #1f425e); font-size: 14px; line-height: 1.35; }
.feature-copy small { margin-top: 4px; color: var(--fwl-muted, #7690a4); font-size: 11px; line-height: 1.35; }
.feature-arrow { color: var(--fwl-brand-soft, #9ec9e5); font-size: 11px; }
.feature-switcher { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; margin-bottom: 14px; }
.feature-switcher button { min-height: 46px; padding: 0 10px; display: flex; align-items: center; justify-content: center; gap: 7px; border: 1px solid var(--fwl-border, #d5e5f0); border-radius: 7px; color: var(--fwl-muted, #55748c); background: #fff; }
.feature-switcher button:hover, .feature-switcher button.active { border-color: var(--fwl-brand, #0097ff); color: var(--fwl-brand, #0097ff); background: var(--fwl-surface, #edf7ff); }
.tool-panel { border: 1px solid var(--fwl-border, #d8e8f3); border-radius: 8px; box-shadow: 0 7px 24px rgba(var(--fwl-brand-strong-rgb, 31, 82, 118), 0.08); overflow: hidden; }
.tool-content { min-height: 360px; padding: 24px; }
.chat-shell { width: min(100%, 932px); height: min(680px, calc(100vh - 190px)); min-height: 500px; margin: 0 auto; padding-top: 18px; border-color: var(--fwl-border, #d8e8f3); border-radius: 8px; box-shadow: 0 8px 26px rgba(var(--fwl-brand-strong-rgb, 31, 82, 118), 0.09); overflow: hidden; }
.empty-chat > i { width: 54px; height: 54px; display: grid; place-items: center; border-radius: 8px; background: var(--fwl-surface, #e9f6ff); }
.empty-chat h2 { color: var(--fwl-brand-strong, #1f425e); }
.quick-list button:hover { border-color: var(--fwl-brand-soft, #83ccf7); color: var(--fwl-brand, #0088e6); background: var(--fwl-surface, #f3faff); }
.message.user .message-avatar { background: var(--fwl-surface, #e9f6ff); color: var(--fwl-brand, #0097ff); }
.message.user .message-bubble { background: var(--fwl-brand, #0097ff); }
.chat-composer { background: #fff; box-shadow: 0 -5px 16px rgba(var(--fwl-brand-strong-rgb, 37, 85, 118), 0.04); }
.chat-composer textarea { min-height: 42px; border-color: var(--fwl-border, #cbdfea); background: var(--fwl-surface, #fafdff); }
.send-button, .primary-button { border-color: var(--fwl-brand, #0097ff); background: var(--fwl-brand, #0097ff); }
.send-button:hover, .primary-button:hover { background: var(--fwl-brand, #0086e2); }
:global(html[data-theme="dark"]) .assistant-page { background: var(--fwl-ink, #101b28); }
:global(html[data-theme="dark"]) .feature-bubbles button, :global(html[data-theme="dark"]) .feature-switcher button, :global(html[data-theme="dark"]) .tool-panel, :global(html[data-theme="dark"]) .chat-shell, :global(html[data-theme="dark"]) .chat-composer { background: var(--fwl-ink, #17283a); border-color: var(--fwl-ink, #29445a); }
:global(html[data-theme="dark"]) .feature-copy b { color: var(--fwl-border, #e7f0f7); }
@media (max-width: 760px) {
  .ai-topbar { min-height: 58px; padding: 8px 10px; }
  .topbar-title h1 { font-size: 18px; }
  .topbar-title span, .ai-topbar .service-status { display: none; }
  .page-content { width: 100%; padding: 15px 8px 20px; }
  .feature-bubbles { grid-template-columns: 1fr; gap: 8px; width: 100%; margin-bottom: 10px; padding: 0; }
  .feature-bubbles button { min-height: 66px; padding: 9px 11px; grid-template-columns: 38px minmax(0, 1fr) 12px; box-shadow: 0 4px 13px rgba(var(--fwl-brand-strong-rgb, 22, 91, 139), 0.09); }
  .feature-icon { width: 38px; height: 38px; }
  .feature-switcher { grid-template-columns: repeat(4, minmax(64px, 1fr)); gap: 5px; overflow-x: auto; }
  .feature-switcher button { min-height: 44px; padding: 5px 4px; flex-direction: column; gap: 3px; font-size: 10px; }
  .tool-panel { margin: 0; }
  .tool-content { min-height: 0; padding: 15px; }
  .chat-shell { width: 100%; height: 560px; min-height: 0; margin: 0; padding-top: 0; }
}
</style>

<style scoped>
.assistant-page {
  --ai-ink: var(--fwl-brand-strong, #122d50);
  --ai-muted: var(--fwl-muted, #6f849d);
  --ai-blue: var(--fwl-brand, #168fe9);
  width: min(100%, 600px);
  min-height: 100dvh;
  margin: 0 auto;
  padding-bottom: calc(78px + env(safe-area-inset-bottom));
  overflow-x: hidden;
  color: var(--ai-ink);
  background: linear-gradient(180deg, var(--fwl-surface, #f5fbff) 0%, var(--fwl-surface, #eef8fe) 55%, var(--fwl-surface, #f9fcff) 100%) !important;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}

.ai-topbar {
  position: relative !important;
  min-height: 109px !important;
  padding: max(8px, env(safe-area-inset-top)) 18px 9px !important;
  color: var(--ai-ink) !important;
  background: linear-gradient(180deg, var(--fwl-surface, #e9f6ff) 0%, var(--fwl-surface, #f3faff) 100%) !important;
  border: 0 !important;
  box-shadow: none !important;
}

.topbar-row { display: flex; align-items: center; min-height: 48px; gap: 12px; }
.topbar-icon { width: 36px !important; height: 36px !important; color: var(--fwl-brand-strong, #1d5c91) !important; background: transparent !important; border: 0 !important; border-radius: 50% !important; font-size: 23px; }
.topbar-icon:hover { background: rgba(var(--fwl-brand-rgb, 22, 143, 233), 0.08) !important; }
.topbar-title { flex: 1; min-width: 0; text-align: left; }
.topbar-title h1,
.assistant-page .topbar-title h1 { min-height: 0 !important; margin: 0 !important; color: var(--ai-ink) !important; font-size: 22px !important; line-height: 1.2 !important; font-weight: 800 !important; letter-spacing: 0 !important; }
.assistant-page .topbar-title h1::after { content: none !important; display: none !important; }
.topbar-title span { color: var(--fwl-muted, #7890a8) !important; font-size: 11px !important; }
.topbar-actions { margin-left: auto; gap: 4px !important; }
.service-status { min-height: 34px !important; padding: 0 11px !important; gap: 7px !important; color: var(--fwl-brand-strong, #246896) !important; background: rgba(255, 255, 255, .72) !important; border: 1px solid var(--fwl-border, #d5eafb) !important; border-radius: 999px !important; font-size: 12px !important; font-weight: 700; box-shadow: 0 3px 9px rgba(var(--fwl-brand-rgb, 67, 139, 190), 0.06); }
.service-status::after { content: none !important; }
.service-status i { width: 8px !important; height: 8px !important; background: #17ba88 !important; box-shadow: 0 0 0 4px rgba(23, 186, 136, .12); }
.cart-button { display: none !important; }

.page-content { width: 100% !important; margin: 0 !important; padding: 0 16px 24px !important; }
.page-content:has(.empty-chat) { display: block !important; min-height: 0 !important; }
.chat-shell,
.page-content:has(.empty-chat) .chat-shell { width: 100% !important; height: auto !important; min-height: 0 !important; margin: 0 !important; padding: 0 !important; overflow: visible !important; border: 0 !important; border-radius: 0 !important; background: transparent !important; box-shadow: none !important; }
.message-list,
.page-content:has(.empty-chat) .message-list { display: block !important; min-height: 0 !important; overflow: visible !important; padding: 0 !important; }
.empty-chat,
.page-content:has(.empty-chat) .empty-chat { width: 100% !important; min-height: 0 !important; margin: 0 !important; padding: 0 !important; display: block !important; color: var(--ai-muted) !important; text-align: left !important; }
.assistant-page .page-content:has(.empty-chat) .empty-chat { display: block !important; align-content: initial !important; justify-items: initial !important; }
.assistant-page .page-content:has(.empty-chat) .empty-chat .ai-hero h2 { margin: 0 !important; color: var(--ai-ink) !important; font-size: 36px !important; line-height: 1.2 !important; }
.assistant-page .page-content:has(.empty-chat) .empty-chat .ai-hero h2::after { content: none !important; display: none !important; }
.assistant-page .page-content:has(.empty-chat) .empty-chat .ai-hero p { margin: 0 !important; color: var(--fwl-muted, #6e819b) !important; font-size: 15px !important; line-height: 1.6 !important; }
.assistant-page .page-content:has(.empty-chat) .empty-chat .ai-hero p::after { content: none !important; display: none !important; }

.ai-hero { position: relative; min-height: 238px; padding: 39px 5px 0; box-sizing: border-box; overflow: hidden; }
.ai-hero-copy { position: relative; z-index: 2; max-width: 410px; }
.ai-hero h2 { margin: 0; color: var(--ai-ink); font-size: 36px; line-height: 1.2; font-weight: 850; letter-spacing: 0; }
.hero-underline { display: block; width: 198px; height: 8px; margin: 5px 0 13px 10px; border-top: 5px solid var(--fwl-brand, #1599f1); border-radius: 50%; transform: rotate(-2deg); }
.ai-hero p { margin: 0; color: var(--fwl-muted, #6e819b); font-size: 15px; line-height: 1.6; font-weight: 500; }

.ai-robot { position: absolute; right: 2px; bottom: 8px; width: 180px; height: 166px; z-index: 1; }
.robot-antenna { position: absolute; left: 88px; top: 0; width: 4px; height: 32px; border-radius: 3px; background: var(--fwl-brand-soft, #75c9fa); transform: rotate(13deg); }
.robot-antenna::after { content: ""; position: absolute; top: -6px; left: -4px; width: 12px; height: 12px; border-radius: 50%; background: var(--fwl-brand, #51b8f3); box-shadow: 0 0 0 5px rgba(var(--fwl-brand-rgb, 81, 184, 243), 0.13); }
.robot-head { position: absolute; left: 27px; top: 28px; width: 127px; height: 91px; border: 9px solid var(--fwl-border, #d9f4ff); border-radius: 47% 47% 43% 43%; background: linear-gradient(145deg, #fff 12%, var(--fwl-border, #d5f3ff) 100%); box-shadow: inset 0 -8px 11px rgba(var(--fwl-brand-rgb, 40, 156, 224), 0.2), 0 12px 20px rgba(var(--fwl-brand-rgb, 66, 159, 215), 0.18); }
.robot-head::before { content: ""; position: absolute; left: 17px; top: 25px; width: 92px; height: 48px; border-radius: 24px; background: var(--fwl-brand-strong, #092b52); box-shadow: inset 0 4px 10px rgba(var(--fwl-brand-rgb, 40, 154, 222), 0.5); }
.robot-eye { position: absolute; z-index: 2; top: 42px; width: 11px; height: 9px; border-top: 3px solid var(--fwl-brand-soft, #72e7ff); border-radius: 50%; }
.robot-eye-left { left: 40px; transform: rotate(8deg); }.robot-eye-right { right: 40px; transform: rotate(-8deg); }
.robot-head b { position: absolute; z-index: 2; left: 52px; top: 53px; width: 22px; height: 9px; border-bottom: 3px solid var(--fwl-brand-soft, #72e7ff); border-radius: 50%; }
.robot-ear { position: absolute; z-index: 3; top: 57px; width: 23px; height: 53px; border-radius: 13px; background: linear-gradient(180deg, var(--fwl-brand, #1bb5fa), var(--fwl-brand-soft, #65d8ff)); box-shadow: 0 7px 11px rgba(var(--fwl-brand-rgb, 40, 156, 224), 0.16); }
.robot-ear-left { left: 8px; transform: rotate(5deg); }.robot-ear-right { right: 8px; transform: rotate(-5deg); }
.robot-body { position: absolute; left: 61px; bottom: 0; width: 64px; height: 46px; display: grid; place-items: center; color: #fff; border-radius: 25px 25px 10px 10px; background: linear-gradient(180deg, var(--fwl-border, #bdeeff), var(--fwl-brand, #45bff4)); box-shadow: 0 8px 14px rgba(var(--fwl-brand-rgb, 35, 147, 208), 0.16); }
.robot-body i { width: 34px; height: 26px; display: grid; place-items: center; color: #ff9a10; border-radius: 50%; background: #fff7d5; font-size: 15px; }
.robot-ray { position: absolute; width: 7px; height: 30px; border-radius: 6px; background: #ffd51a; transform-origin: bottom center; }
.ray-one { left: 13px; top: 22px; transform: rotate(-38deg); }.ray-two { left: 42px; top: 7px; height: 25px; transform: rotate(-18deg); }.ray-three { left: 3px; top: 51px; height: 22px; transform: rotate(-67deg); }

.quick-grid { position: relative; z-index: 3; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 9px; margin: -2px 0 20px; padding: 13px 10px; border: 1px solid rgba(255, 255, 255, .84); border-radius: 25px; background: rgba(255, 255, 255, .9); box-shadow: 0 12px 28px rgba(var(--fwl-brand-rgb, 61, 126, 170), 0.09); }
.quick-action { min-width: 0; min-height: 67px; padding: 8px 5px; display: flex; align-items: center; justify-content: center; gap: 8px; border: 1px solid rgba(var(--fwl-border-rgb, 220, 234, 244), 0.76); border-radius: 17px; color: var(--fwl-brand-strong, #183657); background: var(--fwl-surface, #f8fcff); font-size: 14px; font-weight: 700; text-align: left; transition: transform .2s ease, box-shadow .2s ease; }
.quick-action:hover { transform: translateY(-2px); box-shadow: 0 7px 14px rgba(var(--fwl-brand-rgb, 62, 131, 178), 0.1); }
.quick-action-icon { width: 33px; height: 33px; flex: 0 0 33px; display: grid; place-items: center; border-radius: 50%; font-size: 17px; }
.quick-action-budget .quick-action-icon { color: #fff; background: #fdb21e; }.quick-action-rating .quick-action-icon { color: #fff; background: #f45f5d; }.quick-action-healthy .quick-action-icon { color: #fff; background: #2fc995; }.quick-action-night .quick-action-icon { color: #fff; background: var(--fwl-brand-soft, #8b70eb); }.quick-action-drink .quick-action-icon { color: #fff; background: var(--fwl-brand-soft, #8969e9); }.quick-action-orders .quick-action-icon { color: #fff; background: var(--fwl-brand, #28a4ee); }
.quick-action-budget { background: #fffafb; }.quick-action-rating { background: #fffafa; }.quick-action-healthy { background: #f7fffb; }.quick-action-night, .quick-action-drink { background: var(--fwl-surface, #faf9ff); }.quick-action-orders { background: var(--fwl-surface, #f5fbff); }

.assistant-welcome { display: flex; align-items: flex-start; gap: 10px; margin: 0 4px 16px; }
.mini-robot { width: 54px; height: 54px; flex: 0 0 54px; display: grid; place-items: center; color: var(--fwl-brand, #159cf1); border: 5px solid var(--fwl-border, #d5f2ff); border-radius: 50%; background: var(--fwl-surface, #fafdff); box-shadow: 0 4px 12px rgba(var(--fwl-brand-rgb, 43, 155, 218), 0.12); }
.mini-robot i { font-size: 25px; }
.assistant-welcome p { margin: 0; padding: 15px 17px; color: var(--fwl-brand-strong, #1d3b5e); background: rgba(255, 255, 255, .94); border-radius: 22px; box-shadow: 0 7px 18px rgba(var(--fwl-brand-rgb, 57, 119, 161), 0.07); font-size: 15px; line-height: 1.55; }
.sample-reply { display: flex; align-items: center; justify-content: flex-end; gap: 10px; margin: 0 3px 18px; }
.sample-reply span { padding: 14px 19px; color: #fff; background: linear-gradient(135deg, var(--fwl-brand, #12a0fa), var(--fwl-brand, #188dea)); border-radius: 21px 6px 21px 21px; box-shadow: 0 8px 18px rgba(var(--fwl-brand-rgb, 17, 141, 228), 0.15); font-size: 15px; font-weight: 700; }
.sample-reply i { width: 47px; height: 47px; display: grid; place-items: center; color: #fff; background: #f09aa9; border-radius: 50%; font-size: 21px; }
.recommendation-preview { margin: 0 0 16px; }
.preview-message { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 13px; }
.preview-message .mini-robot { width: 48px; height: 48px; flex-basis: 48px; border-width: 4px; }.preview-message .mini-robot i { font-size: 21px; }
.preview-message p { margin: 0; padding: 13px 16px; color: var(--fwl-brand-strong, #1d3b5e); background: rgba(255, 255, 255, .94); border-radius: 21px; box-shadow: 0 7px 18px rgba(var(--fwl-brand-rgb, 57, 119, 161), 0.06); font-size: 14px; line-height: 1.55; }

.landing-food-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 9px; }
.landing-food-card { min-width: 0; overflow: hidden; border-radius: 18px; background: #fff; box-shadow: 0 7px 18px rgba(var(--fwl-brand-strong-rgb, 48, 102, 142), 0.1); }
.landing-food-image { position: relative; height: 117px; overflow: hidden; }
.landing-food-image img { display: block; width: 100%; height: 100%; object-fit: cover; }
.landing-food-image > span { position: absolute; left: 7px; bottom: 7px; padding: 3px 7px; color: #fff; background: rgba(53, 47, 40, .56); border-radius: 10px; font-size: 11px; }
.landing-food-info { padding: 9px 9px 8px; }
.landing-food-info h3 { overflow: hidden; margin: 0 0 4px; color: var(--fwl-brand-strong, #17385d); font-size: 15px; line-height: 1.25; text-overflow: ellipsis; white-space: nowrap; }
.landing-food-price { display: block; color: #f03d39; font-size: 22px; line-height: 1.1; }.landing-food-price small { margin-right: 2px; font-size: 12px; }
.landing-food-meta { display: flex; align-items: center; gap: 7px; margin-top: 4px; font-size: 12px; }.landing-food-meta span { color: #f39a16; }.landing-food-meta em { overflow: hidden; color: var(--fwl-subtle, #9aaabd); font-style: normal; text-overflow: ellipsis; white-space: nowrap; }
.landing-food-footer { display: flex; align-items: center; gap: 4px; margin-top: 6px; }.landing-food-footer span { min-width: 0; overflow: hidden; flex: 1; color: var(--fwl-muted, #90a2b2); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.landing-food-footer button { width: 29px; height: 29px; flex: 0 0 29px; display: grid; place-items: center; color: #fff; background: var(--fwl-brand, #1497ef); border: 0; border-radius: 50%; font-size: 13px; cursor: pointer; }

.filter-rail { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 7px; margin: 0 0 18px; }
.filter-rail button { min-height: 40px; padding: 0 5px; display: inline-flex; align-items: center; justify-content: center; gap: 5px; color: var(--fwl-brand, #137bc7); background: rgba(255, 255, 255, .84); border: 1px solid var(--fwl-border, #d9ecfa); border-radius: 999px; font-size: 12px; font-weight: 700; white-space: nowrap; cursor: pointer; }
.filter-rail button:hover { background: #fff; border-color: var(--fwl-brand-soft, #a9dafa); }

.chat-composer { position: relative; z-index: 4; margin: 0 0 12px; padding: 8px !important; border: 1px solid var(--fwl-border, #d7e9f4) !important; border-radius: 23px !important; background: rgba(255, 255, 255, .97) !important; box-shadow: 0 11px 28px rgba(var(--fwl-brand-rgb, 53, 112, 151), 0.1) !important; }
.composer-main { display: grid; grid-template-columns: 34px minmax(0, 1fr) 48px; align-items: center; min-height: 53px; gap: 5px; }
.composer-mic { width: 32px; height: 32px; color: var(--fwl-brand-strong, #174b7b); background: transparent; border: 0; font-size: 21px; cursor: pointer; }
.chat-composer textarea { min-height: 45px !important; max-height: 105px !important; padding: 10px 5px !important; color: var(--fwl-brand-strong, #1a3a5c) !important; border: 0 !important; background: transparent !important; box-shadow: none !important; font-size: 15px !important; }
.chat-composer textarea:focus { box-shadow: none !important; }
.composer-main .send-button { width: 48px !important; min-width: 48px !important; height: 48px !important; padding: 0 !important; border-radius: 50% !important; font-size: 19px; }
.composer-main .send-button span { display: none; }
.composer-actions { min-height: 0 !important; padding: 0 !important; }
.composer-actions > span, .composer-actions button[title="历史记录"], .composer-actions button[title="清空当前对话"] { display: none !important; }
.composer-actions .image-shortcut { position: absolute; right: 62px; top: 17px; width: 34px !important; height: 34px !important; color: var(--fwl-brand-strong, #1d5c91) !important; background: transparent !important; border: 0 !important; font-size: 20px; }

.ai-bottom-nav { position: fixed; left: 50%; bottom: 0; z-index: 40; width: min(100%, 600px); min-height: 70px; padding: 7px 10px max(7px, env(safe-area-inset-bottom)); display: grid; grid-template-columns: repeat(4, 1fr); box-sizing: border-box; transform: translateX(-50%); background: rgba(255, 255, 255, .96); border-top: 1px solid rgba(var(--fwl-border-rgb, 213, 229, 240), 0.76); box-shadow: 0 -7px 22px rgba(var(--fwl-brand-strong-rgb, 46, 105, 145), 0.08); backdrop-filter: blur(14px); }
.ai-bottom-nav button { position: relative; min-width: 0; padding: 3px 0 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 3px; color: var(--fwl-subtle, #a5b3c0); background: transparent; border: 0; font-size: 12px; font-weight: 650; cursor: pointer; }
.ai-bottom-nav button i { font-size: 23px; line-height: 1; }.ai-bottom-nav button.active { color: var(--fwl-brand, #168fe9); }.ai-bottom-nav button.active::after { content: ""; width: 21px; height: 4px; margin-top: 2px; border-radius: 4px; background: var(--fwl-brand, #168fe9); }

.page-content:not(:has(.empty-chat)) .chat-shell { min-height: calc(100dvh - 220px) !important; height: calc(100dvh - 220px) !important; padding-bottom: 0 !important; border: 1px solid var(--fwl-border, #d7e9f4) !important; border-radius: 22px !important; background: rgba(255, 255, 255, .9) !important; box-shadow: 0 12px 30px rgba(var(--fwl-brand-rgb, 53, 112, 151), 0.08) !important; overflow: hidden !important; }
.page-content:not(:has(.empty-chat)) .message-list { overflow-y: auto !important; padding: 16px !important; }
.feature-switcher { margin: 0 0 12px !important; }

@media (min-width: 741px) {
  .assistant-page { margin: 16px auto 0; border-radius: 29px 29px 0 0; box-shadow: 0 9px 32px rgba(var(--fwl-brand-strong-rgb, 42, 106, 150), 0.1); }
  .ai-topbar { border-radius: 29px 29px 0 0; }
}

@media (max-width: 430px) {
  /* 去掉模拟状态栏后，顶栏只保留一行标题，不再为其预留 27px 高度 */
  .ai-topbar { min-height: 64px !important; padding-left: 13px !important; padding-right: 13px !important; }
  .page-content { padding-left: 12px !important; padding-right: 12px !important; }
  .ai-hero { min-height: 223px; padding-top: 33px; }
  .ai-hero h2 { font-size: 31px; }
  .ai-hero p { max-width: 260px; font-size: 13px; }
  .hero-underline { width: 167px; margin-bottom: 11px; }
  .ai-robot { right: -12px; bottom: 2px; transform: scale(.86); transform-origin: right bottom; }
  .quick-grid { gap: 7px; margin-bottom: 16px; padding: 10px 8px; border-radius: 21px; }
  .quick-action { min-height: 61px; gap: 5px; border-radius: 14px; font-size: 12px; }
  .quick-action-icon { width: 29px; height: 29px; flex-basis: 29px; font-size: 14px; }
  .assistant-welcome p { padding: 12px 13px; font-size: 13px; }
  .mini-robot { width: 47px; height: 47px; flex-basis: 47px; }.mini-robot i { font-size: 20px; }
  .sample-reply span { padding: 12px 15px; font-size: 13px; }
  .sample-reply i { width: 42px; height: 42px; font-size: 18px; }
  .preview-message p { padding: 11px 13px; font-size: 12px; }
  .landing-food-grid { gap: 6px; }
  .landing-food-image { height: 94px; }
  .landing-food-image > span { left: 4px; bottom: 4px; padding: 2px 5px; font-size: 9px; }
  .landing-food-info { padding: 7px 6px 6px; }
  .landing-food-info h3 { font-size: 12px; }.landing-food-price { font-size: 18px; }.landing-food-meta { gap: 3px; font-size: 10px; }.landing-food-footer span { font-size: 9px; }.landing-food-footer button { width: 25px; height: 25px; flex-basis: 25px; font-size: 11px; }
  .filter-rail { gap: 4px; }.filter-rail button { min-height: 35px; font-size: 10px; }
  .chat-composer { border-radius: 20px !important; }.composer-main { grid-template-columns: 29px minmax(0, 1fr) 44px; }.composer-main .send-button { width: 44px !important; min-width: 44px !important; height: 44px !important; }.composer-actions .image-shortcut { right: 56px; top: 15px; }
}

@media (max-width: 360px) {
  .ai-hero h2 { font-size: 28px; }.ai-hero p { font-size: 12px; }.quick-action { font-size: 11px; }.quick-action-icon { width: 26px; height: 26px; flex-basis: 26px; }.landing-food-image { height: 82px; }.landing-food-meta em { display: none; }.filter-rail button { font-size: 9px; }
}

@media (prefers-reduced-motion: reduce) {
  .assistant-page *, .assistant-page *::before, .assistant-page *::after { animation: none !important; transition: none !important; }
}
</style>
