<template>
  <div class="shop-management-page merchant-ui">
    <div class="top-background">
      <div><h1>经营工作台</h1><p>用 心 经 营 · 美 味 传 递</p></div>
      <MerchantLogoutButton />
    </div>

    <div v-merchant-indicator class="status-tabs">
      <button v-for="tab in tabs" :key="tab.status" :class="{ active: activeTab === tab.status }"
        @click="changeTab(tab.status)">
        {{ tab.label }}
      </button>
    </div>

    <div class="workbench-summary">
      <button @click="changeTab(null)"><i class="fas fa-store stat-blue"></i><span>我的店铺<strong>{{ shops.length }}</strong></span><i class="fas fa-chevron-right"></i></button>
      <button @click="changeTab(1)"><i class="fas fa-check-circle stat-green"></i><span>已上线<strong>{{ approvedCount }}</strong></span><i class="fas fa-chevron-right"></i></button>
      <button @click="changeTab(0)"><i class="fas fa-clock stat-orange"></i><span>待审核<strong>{{ pendingCount }}</strong></span><i class="fas fa-chevron-right"></i></button>
    </div>

    <div class="container wrapper">
      <p v-if="loading" class="workbench-notice" role="status">正在加载店铺…</p>
      <p v-else-if="errorMessage" class="workbench-notice" role="alert">{{ errorMessage }} <button @click="loadShops()">重新加载</button></p>
      <section v-else-if="!filteredShops.length" class="empty-shop-card">
        <img src="/images/merchant/empty-shop.jpg" alt="">
        <h2>当前分类暂无店铺</h2><p>可在下方申请新店，完善资料后提交审核</p>
        <button class="apply-button" @click="applyNewShop"><i class="fas fa-plus-circle"></i> 申请新店</button>
        <button v-if="activeTab !== 0" class="progress-link" @click="changeTab(0)"><i class="fas fa-file-alt"></i> 查看审核中的店铺</button>
      </section>
      <TransitionGroup name="merchant-list" type="transition" tag="ul" class="business-list" appear @before-leave="pinMerchantCard" @after-leave="releaseMerchantCard" @leave-cancelled="releaseMerchantCard">
        <li v-for="(shop, index) in filteredShops" :key="shop?.id || index">
          <span class="shop-review-status" :class="getStatusClass(shop.status)">{{ getStatusText(shop.status) }}</span>
          <img :src="shop?.businessImg || require('@/assets/business-default.png')" :alt="shop?.businessName || '未命名商铺'"
            class="logo" @error="handleImageError" />
          <div class="business-info-detail">
            <h3>{{ shop?.businessName || '未命名商铺' }}</h3>
            <div class="delivery-info-container">
              <div class="business-info-delivery">
                <p>配送费{{ shop?.deliveryPrice?.toFixed(2) || '0.00' }}元</p>
              </div>
              <div class="business-info-delivery">
                <p>起送费{{ shop?.startPrice?.toFixed(2) || '0.00' }}元</p>
              </div>
            </div>
            <div class="business-info-delivery">
              <p>商家地址：{{ shop?.businessAddress || '暂无地址信息' }}</p>
            </div>
            <div class="shop-tags">
              <span v-if="shop?.status === 1" :class="shop?.operatingStatus === false ? 'shop-closed' : 'shop-open'">{{ shop?.operatingStatus === false ? '休息中' : '营业中' }}</span>
              <span v-if="shop?.dineInAvailable">堂食店</span>
              <span v-if="shop?.promotionThreshold && shop?.promotionDiscount">满{{ Number(shop.promotionThreshold).toFixed(0) }}减{{ Number(shop.promotionDiscount).toFixed(0) }}</span>
            </div>
          </div>
          <div class="action-buttons">
            <button class="edit-btn" @click="editShop(shop?.id || index)" :disabled="shop.status === 2">编辑</button>
            <button class="delete-btn" @click="deleteShop(shop?.id || index)">删除</button>
          </div>
        </li>
      </TransitionGroup>
    </div>

    <div v-if="filteredShops.length && !loading && !errorMessage" class="footer-button-container">
      <button class="apply-button" @click="applyNewShop">申请新店</button>
    </div>
    <section class="onboarding-card">
      <h2><span></span>入驻流程</h2>
      <ol><li><i class="fas fa-edit stat-blue"></i><b>填写资料</b><small>提交店铺经营信息</small></li><li><i class="fas fa-search stat-green"></i><b>平台审核</b><small>提交后等待审核结果</small></li><li><i class="fas fa-store stat-orange"></i><b>上线营业</b><small>审核通过后管理店铺</small></li></ol>
    </section>
  </div>
</template>

<script>
import Swal from 'sweetalert2';
import { pickMapLocation } from '../utils/pickMapLocation';
import { ref, onMounted, computed } from 'vue';
import request from '../utils/request';
import { useRouter } from 'vue-router';
import { toast } from '../utils/toast';
import { clearAuth, getToken } from '../utils/auth';
import { listMyBusinesses } from '../services/businessService';
import MerchantLogoutButton from '../components/MerchantLogoutButton.vue';
import { merchantIndicator, pinMerchantCard, releaseMerchantCard } from '../utils/merchantMotion';

export default {
  name: 'MyApplication',
  components: { MerchantLogoutButton },
  directives: { merchantIndicator },
  setup() {
    const router = useRouter();
    const shops = ref([]);
    const loading = ref(false);
    const errorMessage = ref('');
    const activeTab = ref(null);

    const tabs = [
      { status: null, label: '全部' },
      { status: 0, label: '审核中' },
      { status: 1, label: '已上线' },
      { status: 2, label: '未通过' }
    ];

    const getStatusText = (status) => {
      switch (status) {
        case 0: return '审核中';
        case 1: return '已上线';
        case 2: return '审核未通过';
        default: return '未知状态';
      }
    };

    const getStatusClass = (status) => {
      switch (status) {
        case 0: return 'status-pending';
        case 1: return 'status-approved';
        case 2: return 'status-rejected';
        default: return '';
      }
    };

    // 外部对象存储图片可能失效，工作台始终保留本地兜底图，避免出现破图图标和 alt 文本。
    const handleImageError = (event) => {
      const image = event?.target;
      if (!image || image.dataset.fallbackApplied === 'true') return;
      image.dataset.fallbackApplied = 'true';
      image.src = require('@/assets/business-default.png');
    };

    const filteredShops = computed(() => {
      if (activeTab.value === null) {
        return shops.value;
      }
      return shops.value.filter(shop => shop.status === activeTab.value);
    });
    const approvedCount = computed(() => shops.value.filter(shop => shop.status === 1).length);
    const pendingCount = computed(() => shops.value.filter(shop => shop.status === 0).length);

    const changeTab = (status) => {
      activeTab.value = status;
    };

    const loadShops = async (status = null) => {
      loading.value = true;
      errorMessage.value = '';
      try {
        const token = getToken();
        if (!token) {
          toast.warning('用户未登录，请先登录！');
          router.push({ path: '/login', query: { role: 'merchant' } });
          return;
        }
        shops.value = await listMyBusinesses(status);
      } catch (error) {
        console.error('获取商铺列表失败:', error);
        if (error.response && error.response.status === 401) {
          toast.error('登录已过期，请重新登录！');
          clearAuth();
          router.push({ path: '/login', query: { role: 'merchant' } });
        } else {
          errorMessage.value = '获取商铺列表失败，请重试！';
          toast.error('获取商铺列表失败，请重试！');
        }
      } finally {
        loading.value = false;
      }
    };

    const deleteShop = async (shopId) => {
      const result = await Swal.fire({
        title: '确定删除此店铺？',
        text: "删除后将无法恢复！",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      });
      if (result.isConfirmed) {
        try {
          const token = getToken();
          if (!token) {
            toast.warning('用户未登录，请先登录！');
            router.push({ path: '/login', query: { role: 'merchant' } });
            return;
          }
          await request.delete(`/api/businesses/${shopId}`, { headers: { 'Authorization': `Bearer ${token}` } });
          shops.value = shops.value.filter(shop => shop.id !== shopId);
          toast.success('店铺删除成功！');
        } catch (error) {
          console.error('删除店铺失败:', error);
          toast.error('删除店铺失败，请重试！');
        }
      }
    };

    const editShop = (shopId) => {
      router.push(`/merchant/businessinfo?businessId=${shopId}`);
    };

    const applyNewShop = async () => {
      try {
        const token = getToken();
        if (!token) {
          toast.warning('用户未登录，请先登录！');
          router.push({ path: '/login', query: { role: 'merchant' } });
          return;
        }

        const mapPoint = await pickMapLocation();
        if (!mapPoint) return;
        let uploadedFile = null;
        let imageUrl = '';
        let selectedOrderType = null;

        const { value: formValues } = await Swal.fire({
          title: '<span class="application-title-icon"><i class="fas fa-store"></i></span><span>申请新店<small>完善店铺信息，开启外卖经营之旅</small></span>',
          html: `<div class="shop-application-form">
              <section class="application-section">
                <h3><span>1</span>店铺基本信息<small>* 为必填项</small></h3>
                <div class="application-basics">
                  <div id="image-upload-area" class="image-upload-area">
                    <span id="upload-icon" class="upload-icon"><i class="fas fa-camera"></i></span>
                    <span id="upload-text" class="upload-text">上传店铺图片<small>选择一张店铺展示图</small></span>
                    <img id="image-preview" src="" class="image-preview" alt="店铺图片预览">
                    <input id="businessImg" type="file" accept="image/*" class="file-input" aria-label="上传店铺图片">
                  </div>
                  <div class="application-basic-fields">
                    <label for="businessName"><b>*</b> 店铺名称<input id="businessName" class="swal2-input modern-input" placeholder="请输入店铺名称（最多10字）" maxlength="10" required></label>
                    <label for="orderTypeInput"><b>*</b> 店铺类型<select id="orderTypeInput" class="swal2-input modern-input" aria-label="商铺类型" required>
                      <option value="">请选择店铺类型</option>
                      <option value="1">美食</option><option value="2">早餐</option><option value="3">跑腿代购</option><option value="4">汉堡披萨</option><option value="5">甜品饮品</option><option value="6">速食简食</option><option value="7">地方小吃</option><option value="8">米粉面馆</option><option value="9">包子粥铺</option><option value="10">炸鸡炸串</option>
                    </select></label>
                  </div>
                </div>
              </section>
              <section class="application-section"><h3><span>2</span>店铺地址</h3>
                <label for="businessAddress" class="address-field"><b>*</b> 详细地址<input id="businessAddress" class="swal2-input modern-input" placeholder="地图地址，可补充楼栋门牌（最多255字）" maxlength="255" required></label>
              </section>
              <section class="application-section"><h3><span>3</span>店铺介绍</h3>
                <label for="businessExplain" class="sr-only">店铺介绍</label><textarea id="businessExplain" class="swal2-textarea modern-textarea" placeholder="介绍店铺特色、主营菜品等（最多15字）" maxlength="15"></textarea>
              </section>
              <section class="application-section"><h3><span>4</span>配送设置</h3><div class="shop-amount-fields">
                <label for="deliveryPrice"><b>*</b> 配送费（元）<input id="deliveryPrice" class="swal2-input modern-input" aria-label="配送费（元）" placeholder="¥ 请输入" type="number" min="0" step="0.01" required></label>
                <label for="startPrice"><b>*</b> 起送价（元）<input id="startPrice" class="swal2-input modern-input" aria-label="起送价（元）" placeholder="¥ 请输入" type="number" min="0" step="0.01" required></label>
              </div></section>
              <section class="application-section"><h3><span>5</span>其他设置</h3><div class="shop-other-fields">
                <label class="shop-setting-row"><input id="dineInAvailable" type="checkbox"><span>支持堂食<small>在店铺页面展示“堂食店”标识</small></span></label>
                <select id="promotionPreset" class="swal2-input modern-input" aria-label="满减活动"><option value="">不设置满减活动</option><option value="20-3">满20减3</option><option value="30-5">满30减5</option><option value="50-10">满50减10</option></select>
              </div></section>
            </div>`,
          width: '660px',
          showCloseButton: true,
          reverseButtons: true,
          footer: '提交后将进入平台审核，请在工作台查看审核状态',
          padding: '1rem',
          focusConfirm: false,
          showCancelButton: true,
          confirmButtonText: '提交申请',
          confirmButtonColor: '#0097ff',
          cancelButtonText: '取消',
          customClass: {
            popup: 'merchant-shop-popup',
            content: 'compact-content'
          },
          didOpen: () => {
            document.getElementById("businessAddress").value = mapPoint.formattedAddress;
            const fileInput = document.getElementById('businessImg');
            const imagePreview = document.getElementById('image-preview');
            const uploadIcon = document.getElementById('upload-icon');
            const uploadText = document.getElementById('upload-text');

            const orderTypeInput = document.getElementById('orderTypeInput');
            orderTypeInput.addEventListener('change', () => {
              selectedOrderType = orderTypeInput.value ? { id: Number(orderTypeInput.value) } : null;
            });

            // 实时校验函数
            const validateField = (inputId, validationFn, errorMessage) => {
              const input = document.getElementById(inputId);
              if (input) {
                input.addEventListener('input', () => {
                  const isValid = validationFn(input.value.trim());
                  if (!isValid) {
                    input.style.borderColor = '#dc3545';
                    input.style.backgroundColor = '#fff5f5';
                    // 显示错误提示
                    showFieldError(input, errorMessage);
                  } else {
                    input.style.borderColor = '#28a745';
                    input.style.backgroundColor = '#f8fff8';
                    hideFieldError(input);
                  }
                });
              }
            };

            // 显示字段错误提示
            const showFieldError = (input, message) => {
              hideFieldError(input);
              const errorDiv = document.createElement('div');
              errorDiv.className = 'field-error-message';
              errorDiv.textContent = message;
              errorDiv.style.color = '#dc3545';
              errorDiv.style.fontSize = '12px';
              errorDiv.style.marginTop = '4px';
              input.parentNode.appendChild(errorDiv);
            };

            // 隐藏字段错误提示
            const hideFieldError = (input) => {
              const existingError = input.parentNode.querySelector('.field-error-message');
              if (existingError) {
                existingError.remove();
              }
            };

            // 商铺名称校验（最多10个字符）
            validateField('businessName', (value) => {
              return value.length <= 10;
            }, '商铺名称不能超过10个字符');

            // 商铺地址校验（最多15个字符）
            validateField('businessAddress', (value) => {
              return value.length <= 255;
            }, '商铺地址不能超过255个字符');

            // 商铺介绍校验（最多15个字符）
            validateField('businessExplain', (value) => {
              return value.length <= 15;
            }, '商铺介绍不能超过15个字符');

            // 配送费校验
            validateField('deliveryPrice', (value) => {
              if (!value) return false;
              const num = parseFloat(value);
              if (isNaN(num) || num < 0) return false;
              if (value.includes('.') && value.split('.')[1].length > 2) return false;
              return true;
            }, '配送费必须大于等于0，小数点最多保留两位');

            // 起送价校验
            validateField('startPrice', (value) => {
              if (!value) return false;
              const num = parseFloat(value);
              if (isNaN(num) || num < 0) return false;
              if (value.includes('.') && value.split('.')[1].length > 2) return false;
              return true;
            }, '起送价必须大于等于0，小数点最多保留两位');

            fileInput.addEventListener('change', (event) => {
              uploadedFile = event.target.files[0];
              if (uploadedFile) {
                const reader = new FileReader();
                reader.onload = (e) => {
                  imagePreview.src = e.target.result;
                  imagePreview.style.display = 'block';
                  uploadIcon.style.display = 'none';
                  uploadText.style.display = 'none';
                };
                reader.readAsDataURL(uploadedFile);
              } else {
                imagePreview.style.display = 'none';
                uploadIcon.style.display = 'block';
                uploadText.style.display = 'block';
              }
            });


          },
          preConfirm: async () => {
            const businessName = document.getElementById('businessName').value.trim();
            const businessAddress = document.getElementById('businessAddress').value.trim();
            if (!businessAddress.startsWith(mapPoint.formattedAddress)) {
              Swal.showValidationMessage('请保留地图地址，楼栋门牌可补充在后面；更换地点请重新选址');
              return false;
            }
            const businessExplain = document.getElementById('businessExplain').value.trim();
            const deliveryPriceStr = document.getElementById('deliveryPrice').value.trim();
            const startPriceStr = document.getElementById('startPrice').value.trim();
            const dineInAvailable = document.getElementById('dineInAvailable').checked;
            const promotionValue = document.getElementById('promotionPreset').value;

            if (businessName.length > 10 || businessAddress.length > 255 || businessExplain.length > 15) {
              Swal.showValidationMessage('名称最多10字，地址最多255字，介绍最多15字');
              return false;
            }
            if (![deliveryPriceStr, startPriceStr].every(value => /^\d+(\.\d{1,2})?$/.test(value) && Number.isFinite(Number(value)))) {
              Swal.showValidationMessage('配送费和起送价需填写非负金额，最多两位小数');
              return false;
            }

            // 基础必填项校验
            if (!businessName || !businessAddress || !selectedOrderType) {
              Swal.showValidationMessage('请填写所有必填项');
              return false;
            }

            // 检查是否有字段校验错误
            const errorMessages = document.querySelectorAll('.field-error-message');
            if (errorMessages.length > 0) {
              Swal.showValidationMessage('请修正表单中的错误');
              return false;
            }

            const deliveryPrice = parseFloat(deliveryPriceStr) || 0;
            const startPrice = parseFloat(startPriceStr) || 0;
            const [promotionThreshold, promotionDiscount] = promotionValue
              ? promotionValue.split('-').map(value => parseFloat(value))
              : [null, null];

            if (uploadedFile) {
              const formData = new FormData();
              formData.append('file', uploadedFile);
              try {
                const uploadResponse = await request.post('/upload', formData, {
                  headers: {
                    'Content-Type': 'multipart/form-data',
                    'Authorization': `Bearer ${token}`
                  }
                });
                if (uploadResponse && uploadResponse.success && uploadResponse.data) {
                  imageUrl = uploadResponse.data;
                } else {
                  Swal.showValidationMessage(uploadResponse?.message || '图片上传失败');
                  return false;
                }
              } catch (error) {
                console.error('图片上传出错:', error.response || error);
                Swal.showValidationMessage(error.response?.data?.message || error.message || '图片上传出错');
                return false;
              }
            }
            return {
              businessName,
              businessAddress,
              longitude: mapPoint.longitude, latitude: mapPoint.latitude, poiId: mapPoint.poiId, adcode: mapPoint.adcode, formattedAddress: mapPoint.formattedAddress,
              businessExplain,
              deliveryPrice,
              startPrice,
              dineInAvailable,
              promotionThreshold,
              promotionDiscount,
              businessImg: imageUrl,
              orderTypeId: selectedOrderType.id
            };
          }
        });

        if (formValues) {
          const response = await request.post('/api/permission/apply-shop', formValues, {
            headers: { 'Authorization': `Bearer ${token}` }
          });
          if (response && response.success) {
            toast.success('新店申请提交成功！');
            await loadShops();
          } else {
            toast.error(response?.message || '申请提交失败');
          }
        }
      } catch (error) {
        console.error('申请新店出错:', error);
        toast.error(error.response?.data?.message || '申请新店过程中出错，请重试');
      }
    };

    onMounted(() => {
      changeTab(null);
      loadShops();
    });

    return {
      shops,
      pinMerchantCard, releaseMerchantCard, loading,
      errorMessage,
      tabs,
      activeTab,
      filteredShops,
      approvedCount,
      pendingCount,
      deleteShop,
      editShop,
      applyNewShop,
      getStatusText,
      getStatusClass,
      handleImageError,
      changeTab,
      loadShops
    };
  }
};
</script>
