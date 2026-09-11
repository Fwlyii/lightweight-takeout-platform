import request from '../utils/request'

class AiChatService {
  async sendMessage(message, chatType = 'general', sessionId = null, options = {}) {
    try {
      const response = await request.post('/api/v1/assistant/messages', {
        message: message.trim(),
        sessionId,
        budget: options.budget || null,
        usePreferences: options.usePreferences !== false
      })
      if (!response?.success || !response.data) throw new Error(response?.message || '智能助手响应异常')
      return { success: true, data: response.data }
    } catch (error) {
      return {
        success: false,
        error: error?.response?.data?.message || error.message || '智能助手暂时不可用',
        data: {
          message: '智能助手暂时不可用，请稍后重试。',
          sessionId,
          candidates: [],
          processingTime: 0
        }
      }
    }
  }

  async getChatHistory(page = 1, size = 20) {
    try {
      const response = await request.get('/api/ai/chat/history', { params: { page, size } })
      if (!response?.success) throw new Error(response?.message || '获取对话历史失败')
      return { success: true, data: response.data || [] }
    } catch (error) {
      return { success: false, error: error?.response?.data?.message || error.message, data: [] }
    }
  }

  async getChatHistoryBySession(sessionId) {
    try {
      const response = await request.get(`/api/ai/chat/history/session/${encodeURIComponent(sessionId)}`)
      if (!response?.success) throw new Error(response?.message || '获取会话历史失败')
      return { success: true, data: response.data || [] }
    } catch (error) {
      return { success: false, error: error?.response?.data?.message || error.message, data: [] }
    }
  }

  async deleteChatHistory(historyId) {
    try {
      const response = await request.delete(`/api/ai/chat/history/${historyId}`)
      if (!response?.success) throw new Error(response?.message || '删除对话历史失败')
      return { success: true, data: Boolean(response.data) }
    } catch (error) {
      return { success: false, error: error?.response?.data?.message || error.message }
    }
  }

  async cleanOldChatHistory(keepCount = 50) {
    try {
      const response = await request.post('/api/ai/chat/history/clean', null, { params: { keepCount } })
      if (!response?.success) throw new Error(response?.message || '清理对话历史失败')
      return { success: true, data: Boolean(response.data) }
    } catch (error) {
      return { success: false, error: error?.response?.data?.message || error.message }
    }
  }

  async healthCheck() {
    try {
      const response = await request.get('/api/ai/chat/health')
      return response?.success
        ? { success: true, status: 'healthy', message: response.data }
        : { success: false, status: 'unhealthy', error: response?.message }
    } catch (error) {
      return { success: false, status: 'unhealthy', error: error.message }
    }
  }

  formatTime(timestamp) {
    const date = new Date(timestamp)
    const now = new Date()
    const diff = now - date
    if (diff < 24 * 60 * 60 * 1000 && now.getDate() === date.getDate()) {
      return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    }
    if (diff < 48 * 60 * 60 * 1000) {
      return `昨天 ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
    }
    return `${date.toLocaleDateString('zh-CN')} ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
  }

  detectChatType(message) {
    const normalized = message.toLowerCase()
    if (this.containsKeywords(normalized, ['订单', '催单', '配送状态', '物流'])) return 'order'
    if (this.containsKeywords(normalized, ['推荐', '商家', '餐厅'])) return 'business'
    if (this.containsKeywords(normalized, ['菜', '食物', '美食', '吃', '点餐'])) return 'food'
    return 'general'
  }

  containsKeywords(message, keywords) {
    return keywords.some(keyword => message.includes(keyword))
  }
}

export default new AiChatService()
