<template>
  <div class="container information-page" :class="{ 'information-page-ready': pageReady }">
    <div class="fixed-top">
      <div class="top-background">
        <div class="hero-copy">
          <p class="hero-eyebrow">轻松点餐 · 快乐生活</p>
          <h1>{{ riderMode ? '骑手中心' : '个人信息' }}</h1>
          <p class="hero-subtitle">美好生活，从一份美食开始</p>
        </div>
        <div class="hero-art" aria-hidden="true">
          <span class="hero-sun"></span>
          <span class="hero-cloud hero-cloud-one"></span>
          <span class="hero-cloud hero-cloud-two"></span>
          <i class="fas fa-motorcycle hero-bike"></i>
          <strong>好吃 · 更美好</strong>
        </div>
      </div>
    </div>

    <div class="user-card">
      <div class="profile-head">
        <div class="avatar-wrap">
          <div class="avatar" @click="triggerFileInput">
            <img :src="userInfo?.photo || defaultUserAvatar" alt="用户头像" @error="handleImageError">
            <div class="avatar-overlay">
              <i class="fas fa-camera"></i>
              <span>更换头像</span>
            </div>
          </div>
          <button class="avatar-camera" type="button" aria-label="更换头像" @click="triggerFileInput">
            <i class="fas fa-camera"></i>
          </button>
        </div>
        <div class="profile-heading">
          <div class="user-name">{{ userInfo?.username || 'demo_user' }}</div>
          <div class="profile-caption">校园外卖用户</div>
        </div>
        <button class="edit-button" type="button" @click="openEditModal">
          <i class="fas fa-pen"></i>
          <span>编辑</span>
        </button>
      </div>

      <div class="profile-info-list">
        <div class="info-row">
          <span class="info-row-icon icon-identity"><i class="fas fa-user-friends"></i></span>
          <span class="info-label">身份角色</span>
          <strong class="info-value">{{ riderMode ? '骑手用户' : '顾客演示' }}</strong>
        </div>
        <div class="info-row">
          <span class="info-row-icon icon-phone"><i class="fas fa-phone-alt"></i></span>
          <span class="info-label">手机号</span>
          <strong class="info-value">{{ userInfo?.phone || '未绑定手机' }}</strong>
        </div>
        <div class="info-row">
          <span class="info-row-icon icon-email"><i class="fas fa-envelope"></i></span>
          <span class="info-label">邮箱</span>
          <strong class="info-value">{{ userInfo?.email || '未设置邮箱' }}</strong>
        </div>
      </div>
      <div class="card-button-section">
        <button v-if="riderMode" class="switch-btn rider-entry-btn" @click="backToRiderDashboard">
          <i class="fas fa-route"></i> 返回配送工作台
        </button>
        <button class="logout-btn" @click="logout">
          <i class="fas fa-sign-out-alt"></i><span>退出登录</span>
        </button>
      </div>
    </div>

    <!-- 隐藏的文件输入框 -->
    <input type="file" ref="fileInput" style="display: none" accept="image/*" @change="handleFileUpload">

    <div v-if="!riderMode" class="menu-section">
      <div class="section-heading">
        <div class="section-title"><span></span><h2>常用功能</h2></div>
        <p>让外卖生活更简单</p>
      </div>
      <div class="menu-list">
        <div class="menu-item" @click="showAddressSection = !showAddressSection">
          <div class="menu-icon menu-icon-address">
            <i class="fas fa-map-marker-alt"></i>
          </div>
          <span class="menu-text">收货地址</span>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
        <AddressManager v-if="showAddressSection" :id="userInfo?.id" class="address-manager" />
        <div class="menu-item" @click="myfavorite">
          <div class="menu-icon menu-icon-favorite">
            <i class="fas fa-heart"></i>
          </div>
          <span class="menu-text">我的收藏</span>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
        <div class="menu-item message-item" @click="navigateTo('notifications')">
          <div class="menu-icon menu-icon-notification">
            <i class="fas fa-bell"></i>
          </div>
          <span class="menu-text">消息与通知</span>
          <div class="notification-badge" v-if="unreadMessageCount > 0">
            {{ unreadMessageCount }}
          </div>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
        <div class="menu-item" @click="navigateTo('assets')">
          <div class="menu-icon menu-icon-wallet"><i class="fas fa-wallet"></i></div>
          <span class="menu-text">钱包与优惠</span>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
        <div class="menu-item" @click="navigateTo('preferences')">
          <div class="menu-icon menu-icon-preferences"><i class="fas fa-palette"></i></div>
          <span class="menu-text">偏好与外观</span>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
      </div>
    </div>

    <div v-else class="menu-section rider-menu-section">
      <div class="section-heading">
        <div class="section-title"><span></span><h2>骑手工具</h2></div>
        <p>高效完成每一次配送</p>
      </div>
      <div class="menu-list">
        <div class="menu-item" @click="goRiderTab('active')">
          <div class="menu-icon"><i class="fas fa-route"></i></div>
          <span class="menu-text">配送中的订单</span>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
        <div class="menu-item" @click="goRiderTab('history')">
          <div class="menu-icon"><i class="fas fa-history"></i></div>
          <span class="menu-text">历史配送</span>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
        <div class="menu-item message-item" @click="navigateTo('notifications')">
          <div class="menu-icon"><i class="fas fa-bell"></i></div>
          <span class="menu-text">消息与通知</span>
          <div class="notification-badge" v-if="unreadMessageCount > 0">{{ unreadMessageCount }}</div>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
        <div class="menu-item" @click="openEditModal">
          <div class="menu-icon"><i class="fas fa-id-card"></i></div>
          <span class="menu-text">个人资料</span>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="uploading" class="upload-loading">
      <i class="fas fa-spinner fa-spin"></i> 上传中...
    </div>

    <div class="loading" v-if="loading">
      <i class="fas fa-spinner fa-spin"></i> 加载中...
    </div>

    <div class="error-message" v-if="errorMessage">
      <i class="fas fa-exclamation-circle"></i> {{ errorMessage }}
    </div>

    <div v-if="showEditModal" class="modal-overlay">
      <div class="modal-content">
        <h3>编辑个人信息</h3>
        <div class="modal-item">
          <label>姓氏</label>
          <input v-model="editFormData.lastName" placeholder="输入姓氏" />
        </div>
        <div class="modal-item">
          <label>名字</label>
          <input v-model="editFormData.firstName" placeholder="输入名字" />
        </div>
        <div class="modal-item">
          <label>手机号</label>
          <input v-model="editFormData.phone" placeholder="输入手机号" />
        </div>
        <div class="modal-item">
          <label>邮箱</label>
          <input v-model="editFormData.email" placeholder="输入邮箱" type="email" />
        </div>
        <div class="modal-buttons">
          <button @click="submitEdits">提交</button>
          <button @click="closeEditModal">取消</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import AddressManager from '../components/AddressManager.vue';
import request from '../utils/request';
import { useRoute, useRouter } from 'vue-router';
import { toast } from '../utils/toast';
import { createRealtimeConnection } from '../services/realtimeService';
import { hasAuthority } from '../utils/roles';
import { clearAuth, getToken, updateStoredUser } from '../utils/auth';
import { DEFAULT_USER_AVATAR } from '../utils/profileDefaults';

export default {
  name: 'MyInformation',
  components: {
    AddressManager
  },
  setup() {
    const route = useRoute();
    const router = useRouter();
    const userInfo = ref({});
    const defaultUserAvatar = DEFAULT_USER_AVATAR;
    const loading = ref(false);
    const errorMessage = ref('');
    const showEditModal = ref(false);
    const showAddressSection = ref(false);
    const unreadMessageCount = ref(0);
    const pageReady = ref(false);
    const uploading = ref(false);
    const fileInput = ref(null);
    let realtimeConnection = null;
    const riderMode = computed(() => route.query.role === 'rider');

    const editFormData = ref({
      firstName: '',
      lastName: '',
      phone: '',
      email: ''
    });

    const formattedPhone = computed(() => {
      if (!userInfo.value.phone) return '未绑定手机';
      return userInfo.value.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
    });

    onMounted(async () => {
      requestAnimationFrame(() => { pageReady.value = true; });
      const token = getToken();
      if (!token) {
        toast.warning('用户未登录，请先登录！');
        router.push({ path: '/login', query: { role: riderMode.value ? 'rider' : 'user' } });
        return;
      }
      await loadUserData();
      await checkNewMessages();
      realtimeConnection = createRealtimeConnection({
        onMessage: handleNewMessage,
        onFallbackRefresh: checkNewMessages
      });
      realtimeConnection.start();
    });
    onUnmounted(() => {
      realtimeConnection?.stop();
    });
    const handleNewMessage = (message) => {
      const content = message.notificationContent || message.content || '您有一条新消息';
      toast.info(`新消息：${content}`);
      if (content.includes('您的成为商家申请已通过审核')) {
        if (userInfo.value.authorities && Array.isArray(userInfo.value.authorities)) {
          const hasBusinessAuth = hasAuthority(userInfo.value, 'BUSINESS');
          if (!hasBusinessAuth) {
            userInfo.value.authorities.push({ name: 'BUSINESS' });
            updateStoredUser(userInfo.value);
          }
        }
      }
      setTimeout(() => {
        checkNewMessages().catch(err => {
          console.error('新消息触发重新检查失败:', err);
          toast.error('新消息已收到，但加载失败');
        });
      }, 300);
    };
    const checkNewMessages = async () => {
      try {
        const token = getToken();
        if (!token) return;
        const response = await request.get('/api/notifications', {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });
        if (response && response.success && response.data) {
          unreadMessageCount.value = response.data.filter(
            item => item.isDeleted === 0 && item.isRead === 0
          ).length;
        } else {
          unreadMessageCount.value = 0;
        }
      } catch (error) {
        console.error('检查未读消息失败:', error);
        unreadMessageCount.value = 0;
      }
    };
    const triggerFileInput = () => {
      fileInput.value.click();
    };
    const handleFileUpload = async (event) => {
      const file = event.target.files[0];
      if (!file) return;
      if (!file.type.startsWith('image/')) {
        toast.error('请选择图片文件！');
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        toast.error('图片大小不能超过5MB！');
        return;
      }
      uploading.value = true;
      try {
        const token = getToken();
        const formData = new FormData();
        formData.append('file', file);
        const uploadResponse = await request.post('/upload', formData, {
          headers: {
            'Authorization': `Bearer ${token}`,
          }
        });
        if (uploadResponse && uploadResponse.data) {
          const updateResponse = await request.put('/api/person/info', {
            id: userInfo.value.id,
            photo: uploadResponse.data
          }, {
            headers: {
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json'
            }
          });
          if (updateResponse && updateResponse.success) {
            userInfo.value.photo = uploadResponse.data;
            updateStoredUser(userInfo.value);
            toast.success('头像更新成功！');
          } else {
            toast.error('头像更新失败！');
          }
        } else {
          toast.error('图片上传失败！');
        }
      } catch (error) {
        console.error('头像上传失败:', error);
        toast.error('头像上传失败，请重试！');
      } finally {
        uploading.value = false;
        event.target.value = '';
      }
    };
    const loadUserData = async () => {
      loading.value = true;
      errorMessage.value = '';
      try {
        const token = getToken();
        if (!token) {
          toast.warning('用户未登录，请先登录！');
          router.push({ path: '/login', query: { role: riderMode.value ? 'rider' : 'user' } });
          return;
        }
        const response = await request.get('/api/person', {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });
        if (response) {
          userInfo.value = response;
          updateStoredUser(userInfo.value);
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
        if (error.response && error.response.status === 401) {
          toast.error('登录已过期，请重新登录！');
          clearAuth();
          router.push({ path: '/login', query: { role: riderMode.value ? 'rider' : 'user' } });
        } else {
          errorMessage.value = '获取用户信息失败，请重试！';
          toast.error('获取用户信息失败，请重试！');
        }
      } finally {
        loading.value = false;
      }
    };
    const logout = () => {
      clearAuth();
      router.replace({ path: '/login', query: { role: riderMode.value ? 'rider' : 'user' } });
    };
    const backToRiderDashboard = () => {
      router.push({ path: '/rider/dashboard', query: { tab: 'available' } });
    };
    const goRiderTab = (tab) => {
      router.push({ path: '/rider/dashboard', query: { tab } });
    };
    const openEditModal = () => {
      if (userInfo.value) {
        editFormData.value.firstName = userInfo.value.firstName || '';
        editFormData.value.lastName = userInfo.value.lastName || '';
        editFormData.value.phone = userInfo.value.phone || '';
        editFormData.value.email = userInfo.value.email || '';
      }
      showEditModal.value = true;
    };
    const closeEditModal = () => {
      showEditModal.value = false;
    };
    const submitEdits = async () => {
      if (!editFormData.value.phone) {
        toast.warning('手机号不能为空！');
        return;
      }
      try {
        const token = getToken();
        const response = await request.put('/api/person/info', {
          id: userInfo.value.id,
          firstName: editFormData.value.firstName,
          lastName: editFormData.value.lastName,
          email: editFormData.value.email,
          phone: editFormData.value.phone,
          photo: userInfo.value.photo
        }, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });
        if (response.success) {
          userInfo.value.firstName = editFormData.value.firstName;
          userInfo.value.lastName = editFormData.value.lastName;
          userInfo.value.email = editFormData.value.email;
          userInfo.value.phone = editFormData.value.phone;
          updateStoredUser(userInfo.value);
          toast.success('个人信息修改成功！');
          closeEditModal();
        } else {
          toast.error('个人信息修改失败！');
        }
      } catch (error) {
        console.error(error);
        toast.error('个人信息修改失败！');
      }
    };
    const myfavorite = () => {
      router.push({ path: '/favorites' });
    };
    const handleImageError = (event) => {
      const image = event?.target;
      if (!image || image.dataset.fallbackApplied === 'true') return;
      image.dataset.fallbackApplied = 'true';
      image.src = defaultUserAvatar;
    };
    const navigateTo = (page) => {
      const pageRoutes = {
        'notifications': '/notifications',
        'assets': '/assets',
        'preferences': '/preferences'
      };
      if (pageRoutes[page]) {
        router.push({
          path: pageRoutes[page],
          ...(riderMode.value && page === 'notifications' ? { query: { role: 'rider' } } : {})
        });
        if (page === 'notifications') {
          unreadMessageCount.value = 0;
        }
      } else {
        toast.warning('功能待开发');
      }
    };
    return {
      userInfo,
      defaultUserAvatar,
      formattedPhone,
      loading,
      uploading,
      errorMessage,
      showEditModal,
      editFormData,
      fileInput,
      logout,
      openEditModal,
      closeEditModal,
      submitEdits,
      myfavorite,
      handleImageError,
      navigateTo,
      backToRiderDashboard,
      goRiderTab,
      riderMode,
      showAddressSection,
      triggerFileInput,
      handleFileUpload,
      unreadMessageCount
      ,pageReady
    };
  },
};
</script>

<style scoped>
.container {
  width: 100%;
  max-width: 600px;
  min-height: 100vh;
  margin: 0 auto;
  padding: 0 0 calc(96px + env(safe-area-inset-bottom));
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
  color: #14213d;
  background: #f4faff;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}

.fixed-top {
  position: relative;
  width: 100%;
  z-index: 1;
}

.top-background {
  position: relative;
  width: 100%;
  height: 268px;
  overflow: hidden;
  box-sizing: border-box;
  padding: max(30px, env(safe-area-inset-top)) 28px 0;
  color: #fff;
  background: linear-gradient(145deg, #58c5fb 0%, #269af1 57%, #1985e5 100%);
  border-radius: 0 0 34px 34px;
  isolation: isolate;
}

.top-background::before,
.top-background::after {
  content: "";
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.top-background::before {
  width: 250px;
  height: 250px;
  right: -76px;
  top: -118px;
  background: rgba(255, 255, 255, .13);
}

.top-background::after {
  width: 230px;
  height: 116px;
  left: -72px;
  bottom: -68px;
  background: rgba(255, 255, 255, .11);
}

.hero-copy {
  position: relative;
  z-index: 3;
  max-width: 355px;
}

.hero-eyebrow {
  margin: 0 0 9px;
  color: rgba(255, 255, 255, .82);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
}

.top-background h1 {
  margin: 0;
  color: #fff;
  font-size: 38px;
  line-height: 1.15;
  font-weight: 800;
  letter-spacing: 0;
  text-shadow: 0 3px 10px rgba(18, 106, 180, .2);
}

.hero-subtitle {
  margin: 13px 0 0;
  color: rgba(255, 255, 255, .9);
  font-size: 17px;
  line-height: 1.5;
  font-weight: 500;
  letter-spacing: 1px;
}

.hero-art {
  position: absolute;
  z-index: 2;
  right: 9px;
  bottom: 16px;
  width: 190px;
  height: 126px;
  color: rgba(255, 255, 255, .72);
  pointer-events: none;
}

.hero-art strong {
  position: absolute;
  right: 2px;
  top: 6px;
  color: rgba(255, 255, 255, .86);
  font-family: "KaiTi", "STKaiti", cursive;
  font-size: 17px;
  font-weight: 600;
  transform: rotate(-8deg);
  white-space: nowrap;
}

.hero-art strong::after {
  content: "";
  position: absolute;
  width: 110px;
  height: 12px;
  right: -5px;
  bottom: -13px;
  border-top: 2px solid rgba(255, 255, 255, .6);
  border-radius: 50%;
  transform: rotate(-7deg);
}

.hero-sun {
  position: absolute;
  right: 107px;
  top: 35px;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: rgba(255, 255, 255, .28);
}

.hero-cloud {
  position: absolute;
  bottom: 7px;
  width: 62px;
  height: 25px;
  border-radius: 15px 15px 8px 8px;
  background: rgba(255, 255, 255, .3);
}

.hero-cloud::before,
.hero-cloud::after {
  content: "";
  position: absolute;
  bottom: 10px;
  border-radius: 50%;
  background: inherit;
}

.hero-cloud::before {
  left: 13px;
  width: 29px;
  height: 29px;
}

.hero-cloud::after {
  left: 34px;
  width: 22px;
  height: 22px;
}

.hero-cloud-one { right: 73px; opacity: .72; }
.hero-cloud-two { right: 10px; bottom: 1px; transform: scale(.7); opacity: .5; }

.hero-bike {
  position: absolute;
  right: 47px;
  bottom: 24px;
  color: rgba(255, 255, 255, .68);
  font-size: 59px;
  transform: rotate(-4deg);
}

.user-card {
  position: relative;
  z-index: 2;
  width: calc(100% - 32px);
  max-width: 552px;
  margin: -48px auto 0;
  padding: 23px 18px 19px;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, .96);
  border-radius: 25px;
  background: rgba(255, 255, 255, .97);
  box-shadow: 0 16px 38px rgba(36, 128, 198, .14);
}

.profile-head {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.avatar-wrap {
  position: relative;
  width: 86px;
  height: 86px;
}

.avatar {
  width: 86px;
  height: 86px;
  overflow: hidden;
  box-sizing: border-box;
  border: 4px solid #fff;
  border-radius: 50%;
  background: #eaf5ff;
  box-shadow: 0 5px 18px rgba(31, 143, 232, .2);
  cursor: pointer;
  transition: transform .24s ease, box-shadow .24s ease;
}

.avatar:hover,
.avatar:focus-within {
  transform: translateY(-2px);
  box-shadow: 0 8px 22px rgba(31, 143, 232, .28);
}

.avatar img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-overlay {
  position: absolute;
  inset: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  color: #fff;
  background: rgba(22, 127, 218, .7);
  border-radius: 50%;
  opacity: 0;
  transition: opacity .2s ease;
}

.avatar:hover .avatar-overlay { opacity: 1; }
.avatar-overlay i { font-size: 18px; }
.avatar-overlay span { font-size: 10px; }

.avatar-camera {
  position: absolute;
  right: -5px;
  bottom: -2px;
  width: 32px;
  height: 32px;
  padding: 0;
  color: #fff;
  background: #208eeb;
  border: 3px solid #fff;
  border-radius: 50%;
  box-shadow: 0 4px 10px rgba(20, 130, 220, .25);
  cursor: pointer;
  transition: transform .2s ease, background .2s ease;
}

.avatar-camera:hover { background: #0874cb; transform: scale(1.06); }
.avatar-camera i { font-size: 13px; }

.profile-heading { min-width: 0; }

.user-name {
  overflow: hidden;
  color: #14213d;
  font-size: 26px;
  line-height: 1.2;
  font-weight: 800;
  letter-spacing: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-caption {
  margin-top: 7px;
  color: #7f91aa;
  font-size: 13px;
  font-weight: 600;
}

.edit-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-width: 70px;
  height: 42px;
  padding: 0 12px;
  color: #168ce9;
  background: #edf7ff;
  border: 0;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: background .2s ease, transform .2s ease;
}

.edit-button:hover { background: #dcefff; transform: translateY(-1px); }
.edit-button i { font-size: 14px; }

.profile-info-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 20px;
}

.info-row {
  display: grid;
  grid-template-columns: 34px 68px minmax(0, 1fr);
  align-items: center;
  min-height: 52px;
  padding: 7px 12px;
  box-sizing: border-box;
  border-radius: 15px;
  background: #f4f9fd;
}

.info-row-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: #208fe9;
  font-size: 17px;
}

.info-label {
  color: #556981;
  font-size: 15px;
  font-weight: 600;
}

.info-value {
  min-width: 0;
  overflow: hidden;
  color: #14213d;
  font-size: 15px;
  font-weight: 700;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-button-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  margin-top: 20px;
}

.switch-btn,
.logout-btn {
  width: 100%;
  min-height: 54px;
  padding: 0 18px;
  box-sizing: border-box;
  border: 0;
  border-radius: 17px;
  color: #fff;
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 0;
  cursor: pointer;
  transition: transform .22s ease, box-shadow .22s ease, filter .22s ease;
}

.switch-btn {
  background: #159a78;
  box-shadow: 0 9px 18px rgba(21, 154, 120, .2);
}

.logout-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: linear-gradient(100deg, #43b7fa 0%, #1589ed 100%);
  box-shadow: 0 10px 20px rgba(24, 139, 231, .2);
}

.switch-btn:hover,
.logout-btn:hover { transform: translateY(-2px); filter: saturate(1.08); }
.logout-btn i { font-size: 20px; }

.menu-section {
  width: calc(100% - 32px);
  max-width: 552px;
  margin: 27px auto 0;
}

.section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 10px;
  margin: 0 8px 13px;
}

.section-title {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.section-title > span {
  display: block;
  width: 7px;
  height: 28px;
  flex: 0 0 auto;
  border-radius: 5px;
  background: #2399ef;
}

.section-title h2 {
  margin: 0;
  color: #14213d;
  font-size: 23px;
  line-height: 1.2;
  font-weight: 800;
}

.section-heading p {
  margin: 0 0 1px;
  color: #9aaabd;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.menu-list {
  overflow: hidden;
  padding: 7px 18px;
  border: 1px solid rgba(225, 237, 247, .92);
  border-radius: 23px;
  background: rgba(255, 255, 255, .97);
  box-shadow: 0 13px 30px rgba(48, 104, 145, .08);
}

.menu-item {
  display: flex;
  align-items: center;
  min-height: 72px;
  padding: 10px 0;
  box-sizing: border-box;
  border-bottom: 1px solid #edf2f6;
  cursor: pointer;
  transition: background .2s ease, padding .2s ease;
}

.menu-item:last-child { border-bottom: 0; }
.menu-item:hover { padding-left: 4px; background: #fbfdff; }

.menu-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  margin-right: 15px;
  border-radius: 13px;
  font-size: 20px;
}

.menu-icon-address { color: #297fe8; background: #edf4ff; }
.menu-icon-favorite { color: #ed4e7a; background: #fff0f5; }
.menu-icon-notification { color: #f3a42d; background: #fff7e9; }
.menu-icon-wallet { color: #1dae7a; background: #ecfbf4; }
.menu-icon-preferences { color: #7a67e8; background: #f2efff; }

.menu-text {
  min-width: 0;
  flex: 1;
  color: #172745;
  font-size: 17px;
  font-weight: 650;
}

.menu-arrow {
  margin-left: 12px;
  color: #aab6c4;
  font-size: 17px;
}

.menu-item.message-item { position: relative; }

.notification-badge {
  min-width: 20px;
  height: 20px;
  margin-right: 9px;
  padding: 0 5px;
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: #fa5870;
  border: 2px solid #fff;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 800;
  box-shadow: 0 3px 8px rgba(250, 88, 112, .2);
}

.rider-menu-section { margin-top: 27px; }
.rider-menu-section .menu-icon { color: #198fda; background: #edf7ff; }

.address-manager {
  width: 100%;
  max-width: none;
  margin: 0;
  transform: none;
  border-bottom: 1px solid #edf2f6;
}

.menu-list > .address-manager {
  padding: 12px 0 4px;
  border-top: 1px solid #edf2f6;
}

.menu-list > .address-manager .section-title {
  margin: 0 0 8px;
  padding: 0;
  border: 0;
  color: #718399;
  font-size: 13px;
}

.menu-list > .address-manager .menu-list {
  padding: 0;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.loading {
  padding: 22px 15px;
  color: #208fe9;
  font-size: 14px;
  text-align: center;
}

.error-message {
  margin: 15px 16px;
  padding: 12px 14px;
  color: #d64a4a;
  background: #fff0f0;
  border-radius: 12px;
  font-size: 13px;
  text-align: center;
}

.upload-loading {
  position: fixed;
  left: 50%;
  top: 50%;
  z-index: 1100;
  padding: 15px 18px;
  color: #fff;
  background: rgba(20, 42, 71, .86);
  border-radius: 13px;
  font-size: 14px;
  transform: translate(-50%, -50%);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  box-sizing: border-box;
  background: rgba(17, 43, 72, .42);
  backdrop-filter: blur(5px);
}

.modal-content {
  width: min(100%, 410px);
  max-height: min(690px, calc(100vh - 40px));
  overflow-y: auto;
  padding: 23px 20px 20px;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, .8);
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 18px 50px rgba(19, 67, 107, .24);
}

.modal-content h3 {
  margin: 0 0 20px;
  color: #14213d;
  font-size: 21px;
  font-weight: 800;
}

.modal-item { margin-bottom: 14px; }
.modal-item label {
  display: block;
  margin: 0 0 7px;
  color: #596d84;
  font-size: 13px;
  font-weight: 700;
}

.modal-content input,
.modal-content textarea {
  width: 100%;
  min-height: 44px;
  padding: 10px 12px;
  box-sizing: border-box;
  border: 1px solid #dbe8f2;
  border-radius: 11px;
  outline: none;
  color: #14213d;
  background: #f8fbfe;
  font: inherit;
  font-size: 15px;
  transition: border-color .2s ease, box-shadow .2s ease;
}

.modal-content input:focus,
.modal-content textarea:focus {
  border-color: #2b9bf0;
  box-shadow: 0 0 0 3px rgba(43, 155, 240, .12);
}

.modal-buttons {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 21px;
}

.modal-buttons button {
  min-height: 44px;
  border: 0;
  border-radius: 11px;
  font: inherit;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.modal-buttons button:first-child { color: #fff; background: #188fea; }
.modal-buttons button:last-child { color: #53667b; background: #edf3f7; }

@media (min-width: 601px) {
  .container {
    min-height: calc(100vh - 72px);
    margin: 16px auto 0;
    border-radius: 28px 28px 0 0;
    box-shadow: 0 8px 32px rgba(42, 106, 150, .1);
  }

  .top-background { border-radius: 28px 28px 34px 34px; }
}

@media (max-width: 420px) {
  .top-background { height: 250px; padding-left: 22px; padding-right: 22px; }
  .top-background h1 { font-size: 34px; }
  .hero-subtitle { font-size: 15px; }
  .hero-art { right: -12px; bottom: 7px; transform: scale(.88); transform-origin: right bottom; }
  .user-card { width: calc(100% - 24px); margin-top: -40px; padding: 19px 14px 16px; border-radius: 22px; }
  .menu-section { width: calc(100% - 24px); }
  .menu-list { padding-left: 15px; padding-right: 15px; border-radius: 21px; }
  .section-heading { margin-left: 3px; margin-right: 3px; }
  .section-title h2 { font-size: 21px; }
  .section-heading p { font-size: 12px; }
  .menu-item { min-height: 68px; }
}

@media (max-width: 360px) {
  .profile-head { grid-template-columns: 76px minmax(0, 1fr) auto; gap: 10px; }
  .avatar-wrap, .avatar { width: 76px; height: 76px; }
  .user-name { font-size: 22px; }
  .edit-button { min-width: 58px; padding: 0 9px; font-size: 13px; }
  .info-row { grid-template-columns: 31px 60px minmax(0, 1fr); padding-left: 9px; padding-right: 9px; }
  .info-label, .info-value { font-size: 13px; }
  .menu-text { font-size: 16px; }
}

@media (prefers-reduced-motion: reduce) {
  .avatar,
  .avatar-camera,
  .edit-button,
  .switch-btn,
  .logout-btn,
  .menu-item { transition: none; }
}
</style>
