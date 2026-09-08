<template>
  <div class="notifications-container">
    <div class="header">
      <button class="header-back" type="button" aria-label="返回" @click="router.back()">
        <i class="fa fa-chevron-left" aria-hidden="true"></i>
      </button>
      <h1 class="title">消息与通知</h1>
      <div v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }}</div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载消息中...</p>
    </div>

    <!-- 历史接口真正不可用时才显示错误；实时通道断开不遮住消息列表。 -->
    <div v-else-if="historyError && messages.length === 0" class="error-state">
      <i class="fa fa-exclamation-circle" aria-hidden="true"></i>
      <p>{{ historyError }}</p>
      <button class="retry-btn" @click="retryAll">重新加载</button>
    </div>

    <!-- 消息列表 -->
    <div v-else class="notification-list">
      <div v-if="realtimeNotice" class="sync-notice">
        <i class="fa fa-refresh" aria-hidden="true"></i>
        <span>{{ realtimeNotice }}</span>
        <button type="button" @click="retryRealtime">重试</button>
      </div>
      <div v-for="message in messages" :key="message.id" class="notification-item" @click="markAsRead(message)">
        <div class="icon-wrapper" :class="{ 'unread': message.unread }">
          <svg class="icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
            <path d="M22 6.5C22 7.32843 21.3284 8 20.5 8H3.5C2.67157 8 2 7.32843 2 6.5V3.5C2 2.67157 2.67157 2 3.5 2H20.5C21.3284 2 22 2.67157 22 3.5V6.5ZM22 17.5C22 18.3284 21.3284 19 20.5 19H3.5C2.67157 19 2 18.3284 2 17.5V14.5C2 13.6716 2.67157 13 3.5 13H20.5C21.3284 13 22 13.6716 22 14.5V17.5ZM2 10.5C2 9.67157 2.67157 9 3.5 9H20.5C21.3284 9 22 9.67157 22 10.5V11.5C22 12.3284 21.3284 13 20.5 13H3.5C2.67157 13 2 12.3284 2 11.5V10.5Z"></path>
          </svg>
        </div>
        <div class="content">
          <div class="message-text" :class="{ 'bold': message.unread }">{{ message.notificationContent }}</div>
          <div class="timestamp">{{ formatTime(message.createTime) }}</div>
        </div>
        <div v-if="message.unread" class="dot"></div>
      </div>

      <!-- 空状态 -->
      <div v-if="messages.length === 0" class="empty-state">
        <i class="fa fa-envelope-open" aria-hidden="true"></i>
        <p>暂无消息通知</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import request from '@/utils/request';
import { toast } from '@/utils/toast';
import { getStoredUser } from '@/utils/auth';
import { createRealtimeConnection, REALTIME_STATE } from '@/services/realtimeService';

// 状态管理
const messages = ref([]);
const router = useRouter();
const loading = ref(true);
const historyError = ref('');
const realtimeState = ref(REALTIME_STATE.IDLE);
const currentUserId = getStoredUser()?.id;
let realtimeConnection = null;
let disposed = false;
let historyRequest = 0;

// 计算未读消息数量
const unreadCount = computed(() => {
  return messages.value.filter(msg => msg.unread).length;
});

const realtimeNotice = computed(() => {
  if ([REALTIME_STATE.RECONNECTING, REALTIME_STATE.OFFLINE].includes(realtimeState.value)) {
    return '实时同步恢复中，消息列表仍会定时刷新';
  }
  if (realtimeState.value === REALTIME_STATE.UNAUTHORIZED) return '登录状态已失效，请重新登录';
  return '';
});

// 初始化：历史消息优先展示，实时通道作为增强能力独立连接。
onMounted(async () => {
  await fetchHistoryMessages();
  if (disposed) return;
  realtimeConnection = createRealtimeConnection({
    onMessage: handleNewMessage,
    onStatusChange: ({ state }) => { realtimeState.value = state; },
    onFallbackRefresh: () => fetchHistoryMessages({ silent: true })
  });
  realtimeConnection.start();
});

// 组件卸载时关闭WebSocket连接
onUnmounted(() => {
  disposed = true;
  historyRequest += 1;
  realtimeConnection?.stop();
});

/**
 * 加载历史消息
 */
const fetchHistoryMessages = async ({ silent = false } = {}) => {
  const requestId = ++historyRequest;
  if (!silent) loading.value = true;
  try {
    const res = await request.get('/api/notifications');
    if (disposed || requestId !== historyRequest) return;
    if (!res?.success || !Array.isArray(res.data)) {
      throw new Error(res?.message || '消息加载失败');
    }
    historyError.value = '';
    messages.value = res.data.map(msg => ({ ...msg, unread: Number(msg.isRead) !== 1 }));
  } catch (err) {
    if (!disposed && requestId === historyRequest) historyError.value = '消息加载失败，请稍后重试';
  } finally {
    if (!disposed && requestId === historyRequest) loading.value = false;
  }
};

const retryRealtime = () => {
  realtimeConnection?.retry();
};

const retryAll = async () => {
  await fetchHistoryMessages();
  retryRealtime();
};

/**
 * 处理新接收的消息
 */
const handleNewMessage = (message) => {
  // 只处理当前用户的消息
  if (message.userId && String(message.userId) !== String(currentUserId)) {
    return;
  }
  const content = message.notificationContent || message.content;
  if (!content) {
    console.warn('收到无效的WebSocket消息:', message);
    return;
  }

  toast.info(`新消息：${content}`);

  // 推送在事务提交后发送；历史接口始终是消息列表的数据来源。
  void fetchHistoryMessages({ silent: true });
};

/**
 * 标记消息为已读
 */
const markAsRead = async (message) => {
  if (!message.unread) return;

  try {
    // 调用接口标记为已读
    const response = await request.put(`/api/notifications/${message.id}/read`);
    if (!response?.success) throw new Error(response?.message || '更新消息状态失败');

    // 更新本地状态
    message.unread = false;
    await fetchHistoryMessages({ silent: true });
  } catch (err) {
    console.error('标记消息为已读失败:', err);
    toast.error('更新消息状态失败');
  }
};

/**
 * 格式化时间显示
 */
const formatTime = (timeStr) => {
  if (!timeStr) return '';

  const date = new Date(timeStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};
</script>

<style scoped>
.notifications-container {
  max-width: 600px;
  margin: 0 auto;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  color: #333;
  min-height: 100vh;
  background-color: #fff;
}

.header {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 50px;
  background-color: #0097ff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  z-index: 100;
}
/* .header {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid #e0e0e0;
  position: relative;
} */

.title {
 font-size: 1.1rem;
  color: #ffffff;
  font-weight: 600;
  margin: 0;
}

.header-back {
  position: absolute;
  left: 14px;
  width: 34px;
  height: 34px;
  border: 0;
  background: transparent;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
}

.unread-badge {
  position: absolute;
  top: 15px;
  right: 20px;
  background-color: #ff4d4f;
  color: white;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
}

.notification-list {
  padding: 70px 16px 24px;
  margin: 0 auto;
  max-width: 600px;
}

.sync-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  padding: 9px 12px;
  border: 1px solid #dcebf7;
  border-radius: 7px;
  background: #f4f9fd;
  color: #617b92;
  font-size: 12px;
}

.sync-notice button {
  margin-left: auto;
  border: 0;
  background: transparent;
  color: #168bd1;
  cursor: pointer;
}

.notification-item {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s ease;
  cursor: pointer;
}

.notification-item:hover {
  background-color: #f5f8ff; /* 浅蓝色背景 */
}

.notification-item:last-child {
  border-bottom: none;
}

.icon-wrapper {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 15px;
  background-color: #e6f0ff; /* 浅蓝色 */
}

.icon-wrapper.unread {
  background-color: #c9e2ff; /* 针对未读消息的更深的蓝色 */
}

.icon {
  width: 20px;
  height: 20px;
  color: #1a73e8; /* 图标蓝色 */
}

.content {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.message-text {
  font-size: 16px;
  line-height: 1.5;
  color: #555;
}

.message-text.bold {
  font-weight: 600;
  color: #1a73e8; /* 未读消息文本蓝色 */
}

.timestamp {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.dot {
  width: 8px;
  height: 8px;
  background-color: #ff4d4f; /* 未读小红点 */
  border-radius: 50%;
  margin-left: 10px;
  flex-shrink: 0;
}

/* 空状态样式 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state i {
  font-size: 3rem;
  margin-bottom: 15px;
  color: #ddd;
}

.empty-state p {
  font-size: 1.1rem;
}

/* 加载状态样式 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #1a73e8;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 错误状态样式 */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #f5222d;
  text-align: center;
}

.error-state i {
  font-size: 3rem;
  margin-bottom: 15px;
}

.error-state p {
  font-size: 1.1rem;
  margin-bottom: 20px;
}

.retry-btn {
  background-color: #1a73e8;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.retry-btn:hover {
  background-color: #0d66d0;
}
</style>
