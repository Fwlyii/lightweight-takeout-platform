<template>
  <main class="dispatch-page" aria-labelledby="dispatch-title">
    <header class="dispatch-hero">
      <button class="hero-back" type="button" aria-label="返回管理员首页" @click="router.push('/admin/home')">
        <i class="fa fa-chevron-left" aria-hidden="true"></i>
      </button>
      <div class="hero-copy">
        <h1 id="dispatch-title">骑手与配送调度</h1>
        <p>高效调度&nbsp;&nbsp;·&nbsp;&nbsp;安全配送&nbsp;&nbsp;·&nbsp;&nbsp;优质服务</p>
      </div>
      <div class="hero-art" aria-hidden="true">
        <img src="/images/admin-dispatch-reference.png" alt="">
      </div>
    </header>

    <section class="dispatch-content">
      <section class="platform-card" aria-label="平台管理">
        <div class="platform-copy">
          <span class="section-mark"></span>
          <div><h2>平台管理</h2><p>审核骑手资质，处理配送异常，查看实时调度</p></div>
        </div>
        <button class="refresh-button" type="button" :disabled="loading" @click="loadAll">
          <i class="fa fa-refresh" :class="{ spin: loading }" aria-hidden="true"></i><span>刷新数据</span>
        </button>
      </section>

      <section class="dispatch-stats" aria-label="配送统计">
        <article class="dispatch-stat stat-pending">
          <span class="stat-icon"><i class="fa fa-user-clock" aria-hidden="true"></i></span>
          <span class="stat-label">待审核申请</span><strong>{{ pendingApplications.length }}</strong>
        </article>
        <article class="dispatch-stat stat-approved">
          <span class="stat-icon"><i class="fa fa-motorcycle" aria-hidden="true"></i></span>
          <span class="stat-label">已认证骑手</span><strong>{{ approvedCount }}</strong>
        </article>
        <article class="dispatch-stat stat-delivering">
          <span class="stat-icon"><i class="fa fa-clipboard" aria-hidden="true"></i></span>
          <span class="stat-label">配送中订单</span><strong>{{ deliveringCount == null ? '--' : deliveringCount }}</strong>
        </article>
        <article class="dispatch-stat stat-exception">
          <span class="stat-icon"><i class="fa fa-exclamation-triangle" aria-hidden="true"></i></span>
          <span class="stat-label">待处理异常</span><strong>{{ openExceptions.length }}</strong>
        </article>
      </section>

      <section class="dispatch-workspace">
        <nav class="workspace-tabs" aria-label="配送管理模块" role="tablist">
          <button type="button" role="tab" :aria-selected="tab === 'applications'"
            :class="{ active: tab === 'applications' }" @click="tab = 'applications'">
            <span>骑手资质审核</span><b>{{ pendingApplications.length }}</b>
          </button>
          <button type="button" role="tab" :aria-selected="tab === 'exceptions'"
            :class="{ active: tab === 'exceptions' }" @click="tab = 'exceptions'">
            <span>配送异常工单</span><b>{{ openExceptions.length }}</b>
          </button>
        </nav>

        <div v-if="loading" class="state-message" role="status" aria-live="polite">
          <i class="fa fa-refresh spin" aria-hidden="true"></i><p>正在同步调度数据...</p>
        </div>

        <template v-else-if="tab === 'applications'">
          <div class="list-heading">
            <div><span class="section-mark"></span><h2>申请列表</h2></div>
            <label class="status-select"><span class="sr-only">申请状态</span>
              <select v-model="applicationFilter" aria-label="申请状态">
                <option value="all">全部状态</option><option value="0">待审核</option>
                <option value="1">已通过</option><option value="2">已拒绝</option>
              </select><i class="fa fa-chevron-down" aria-hidden="true"></i>
            </label>
          </div>
          <div v-if="filteredApplications.length === 0" class="empty-state">
            <i class="fa fa-clipboard" aria-hidden="true"></i><p>当前没有匹配的骑手申请</p>
          </div>
          <div v-else class="application-list">
            <article v-for="item in filteredApplications" :key="item.id" class="application-card">
              <div class="application-head">
                <div class="rider-identity">
                  <span class="rider-avatar">{{ item.realName?.slice(0, 1) || '?' }}</span>
                  <div><h3>{{ item.realName || '未填写姓名' }}</h3><p>@{{ item.username || '未设置账号' }}</p></div>
                </div>
                <span class="audit-badge" :class="auditClass(item.auditStatus)">
                  <i :class="item.auditStatus === 1 ? 'fa fa-check-circle' : item.auditStatus === 0 ? 'fa fa-clock-o' : 'fa fa-times-circle'" aria-hidden="true"></i>
                  {{ auditName(item.auditStatus) }}
                </span>
              </div>
              <div class="application-info">
                <div><i class="fa fa-phone" aria-hidden="true"></i><span>联系电话</span><strong>{{ item.phone || '未填写' }}</strong></div>
                <div><i class="fa fa-motorcycle" aria-hidden="true"></i><span>配送方式</span><strong>{{ vehicleName(item.vehicleType) }}</strong></div>
                <div><i class="fa fa-clock-o" aria-hidden="true"></i><span>申请时间</span><strong>{{ formatTime(item.createTime) }}</strong></div>
              </div>
              <div v-if="item.auditStatus === 0" class="application-actions">
                <button type="button" class="approve-button" @click="audit(item, true)">通过</button>
                <button type="button" class="reject-button" @click="openReject(item)">拒绝</button>
              </div>
              <p v-else class="completed-note">{{ item.rejectReason || '已完成审核' }}</p>
            </article>
          </div>
        </template>

        <template v-else>
          <div class="list-heading">
            <div><span class="section-mark"></span><h2>异常工单</h2></div>
            <label class="status-select"><span class="sr-only">异常状态</span>
              <select v-model="exceptionFilter" aria-label="异常状态">
                <option value="all">全部状态</option><option value="0">待处理</option><option value="1">已闭环</option>
              </select><i class="fa fa-chevron-down" aria-hidden="true"></i>
            </label>
          </div>
          <div v-if="filteredExceptions.length === 0" class="empty-state">
            <i class="fa fa-shield" aria-hidden="true"></i><p>当前没有配送异常</p>
          </div>
          <div v-else class="exception-list">
            <article v-for="item in filteredExceptions" :key="item.id" class="exception-card">
              <div class="exception-head">
                <div class="exception-title"><span class="exception-icon"><i class="fa fa-exclamation" aria-hidden="true"></i></span>
                  <div><small>订单 #{{ item.orderId }}</small><h3>{{ exceptionName(item.exceptionType) }}</h3></div>
                </div>
                <span class="audit-badge" :class="item.status === 0 ? 'badge-pending' : 'badge-approved'">
                  <i :class="item.status === 0 ? 'fa fa-clock-o' : 'fa fa-check-circle'" aria-hidden="true"></i>{{ item.status === 0 ? '待响应' : '已闭环' }}
                </span>
              </div>
              <p class="exception-description">{{ item.description || '未填写异常说明' }}</p>
              <div class="exception-meta">
                <span><i class="fa fa-motorcycle" aria-hidden="true"></i>{{ item.riderName || '未知骑手' }}</span>
                <span><i class="fa fa-store" aria-hidden="true"></i>{{ item.businessName || '未知商铺' }}</span>
                <span><i class="fa fa-clock-o" aria-hidden="true"></i>{{ formatTime(item.createTime) }}</span>
              </div>
              <div v-if="item.status === 0" class="resolution-actions">
                <button type="button" class="resume-button" @click="openResolution(item, 'RESUME')"><i class="fa fa-play" aria-hidden="true"></i>恢复配送</button>
                <button type="button" @click="openResolution(item, 'REASSIGN')"><i class="fa fa-random" aria-hidden="true"></i>重新派单</button>
                <button type="button" class="cancel-button" @click="openResolution(item, 'CANCEL')"><i class="fa fa-ban" aria-hidden="true"></i>取消订单</button>
              </div>
              <p v-else class="completed-note"><strong>{{ actionName(item.resolutionAction) }}</strong>{{ item.resolutionNote || '异常已处理' }}</p>
            </article>
          </div>
        </template>
      </section>
    </section>

    <div v-if="modal" class="modal-mask" @click.self="modal = null">
      <form class="modal" @submit.prevent="submitModal">
        <span class="modal-icon"><i :class="modal.type === 'reject' ? 'fa fa-user-times' : 'fa fa-headphones'" aria-hidden="true"></i></span>
        <h2>{{ modalTitle }}</h2><p>{{ modalDescription }}</p>
        <textarea v-model.trim="modal.note" maxlength="500" :required="modal.type === 'reject'"
          :placeholder="modal.type === 'reject' ? '请填写拒绝原因（必填）' : '填写调度处理说明'"></textarea>
        <div class="modal-actions"><button type="button" class="modal-secondary" @click="modal = null">返回</button><button type="submit" class="modal-confirm">确认处理</button></div>
      </form>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import request from '@/utils/request';
import { toast } from '@/utils/toast';

const router = useRouter();
const applications = ref([]);
const exceptions = ref([]);
const deliveringCount = ref(null);
const loading = ref(false);
const tab = ref('applications');
const applicationFilter = ref('all');
const exceptionFilter = ref('all');
const modal = ref(null);

const pendingApplications = computed(() => applications.value.filter(item => Number(item.auditStatus) === 0));
const approvedCount = computed(() => applications.value.filter(item => Number(item.auditStatus) === 1).length);
const openExceptions = computed(() => exceptions.value.filter(item => Number(item.status) === 0));
const filteredApplications = computed(() => applicationFilter.value === 'all' ? applications.value : applications.value.filter(item => Number(item.auditStatus) === Number(applicationFilter.value)));
const filteredExceptions = computed(() => exceptionFilter.value === 'all' ? exceptions.value : exceptions.value.filter(item => Number(item.status) === Number(exceptionFilter.value)));

const loadAll = async () => {
  loading.value = true;
  try {
    const [applicationResponse, exceptionResponse, summaryResponse] = await Promise.all([
      request.get('/api/v1/admin/rider-applications'),
      request.get('/api/v1/admin/delivery-exceptions'),
      request.get('/api/v1/admin/delivery-tasks/summary').catch(() => null)
    ]);
    applications.value = Array.isArray(applicationResponse?.data) ? applicationResponse.data : [];
    exceptions.value = Array.isArray(exceptionResponse?.data) ? exceptionResponse.data : [];
    deliveringCount.value = summaryResponse?.success ? Number(summaryResponse.data || 0) : null;
  } catch (error) {
    toast.error(error?.response?.data?.message || '调度数据加载失败');
  } finally {
    loading.value = false;
  }
};

const audit = async (item, approved) => {
  try {
    await request.post(`/api/v1/admin/rider-applications/${item.id}/audit`, { approved, reason: null });
    toast.success(approved ? `已通过 ${item.realName} 的骑手申请` : '审核意见已发送');
    await loadAll();
  } catch (error) { toast.error(error?.response?.data?.message || '审核失败'); }
};

const openReject = item => { modal.value = { type: 'reject', item, note: '' }; };
const openResolution = (item, action) => { modal.value = { type: 'resolution', item, action, note: '' }; };
const modalTitle = computed(() => modal.value?.type === 'reject' ? '拒绝骑手申请' : actionName(modal.value?.action));
const modalDescription = computed(() => modal.value?.type === 'reject'
  ? `将退回 ${modal.value?.item?.realName || '该骑手'} 的申请，对方可根据意见重新提交。`
  : `正在处理订单 #${modal.value?.item?.orderId || '-'} 的配送异常，操作将写入履约轨迹。`);

const submitModal = async () => {
  if (modal.value.type === 'reject' && !modal.value.note) return toast.warning('请填写拒绝原因');
  try {
    if (modal.value.type === 'reject') {
      await request.post(`/api/v1/admin/rider-applications/${modal.value.item.id}/audit`, { approved: false, reason: modal.value.note });
      toast.success('审核意见已发送');
    } else {
      await request.post(`/api/v1/admin/delivery-exceptions/${modal.value.item.id}/resolve`, { action: modal.value.action, note: modal.value.note });
      toast.success('配送异常已处理并记录');
    }
    modal.value = null;
    await loadAll();
  } catch (error) { toast.error(error?.response?.data?.message || '处理失败'); }
};

const vehicleName = value => ({ E_BIKE: '电动车', BIKE: '自行车', WALK: '步行' }[value] || value || '未填写');
const auditName = value => ({ 0: '待审核', 1: '已通过', 2: '已拒绝' }[value] || '未知');
const auditClass = value => Number(value) === 0 ? 'badge-pending' : Number(value) === 1 ? 'badge-approved' : 'badge-rejected';
const exceptionName = value => ({ STORE_DELAY: '商家出餐延迟', CUSTOMER_UNREACHABLE: '无法联系顾客', ADDRESS_ERROR: '收货地址异常', VEHICLE_FAILURE: '骑手车辆故障', OTHER: '其他配送问题' }[value] || value || '配送异常');
const actionName = value => ({ RESUME: '恢复原配送', REASSIGN: '重新分配骑手', CANCEL: '取消订单' }[value] || '处理配送异常');
const formatTime = value => value ? new Date(value).toLocaleString('zh-CN') : '-';

onMounted(loadAll);
</script>

<style scoped>
:global(*) { box-sizing: border-box; }
:global(body) { background: var(--skin-surface, #f3f8fd); }

.dispatch-page {
  --dispatch-brand: var(--skin-brand, #168cf0);
  --dispatch-brand-rgb: var(--skin-brand-rgb, 22, 140, 240);
  --dispatch-ink: var(--skin-ink, #18365d);
  --dispatch-muted: var(--skin-muted, #778ba5);
  --dispatch-border: var(--skin-border, #dce9f4);
  width: 100%;
  max-width: 600px;
  min-width: 0;
  min-height: 100%;
  margin: 0 auto;
  padding-bottom: calc(68px + env(safe-area-inset-bottom));
  overflow-x: hidden;
  color: var(--dispatch-ink);
  background: var(--skin-surface, #f3f8fd);
}

.dispatch-page button,
.dispatch-page select,
.dispatch-page textarea { font-family: inherit; }

.dispatch-hero {
  position: relative;
  height: 78px;
  overflow: hidden;
  color: #fff;
  background:
    radial-gradient(circle at 8% 92%, rgba(255,255,255,.10) 0 23%, transparent 23.5%),
    linear-gradient(126deg, var(--skin-brand-soft, #48aaf6) 0%, var(--skin-brand, #168cf0) 58%, var(--skin-brand, #248ee9) 100%);
}

.dispatch-hero::after {
  content: '';
  position: absolute;
  right: 5%;
  bottom: -34px;
  left: -5%;
  height: 50px;
  border-radius: 50%;
  background: rgba(255,255,255,.06);
}

.hero-back {
  position: absolute;
  z-index: 3;
  top: 14px;
  left: 16px;
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  padding: 0;
  border: 1px solid rgba(255,255,255,.32);
  border-radius: 8px;
  color: #fff;
  background: rgba(255,255,255,.18);
  box-shadow: inset 0 1px 0 rgba(255,255,255,.16);
  font-size: 17px;
  cursor: pointer;
  backdrop-filter: blur(3px);
}

.hero-copy {
  position: absolute;
  z-index: 2;
  top: 15px;
  left: 50%;
  width: 242px;
  max-width: calc(100% - 112px);
  text-align: center;
  transform: translateX(-50%);
}

.hero-copy h1 {
  margin: 0;
  color: #fff;
  font-size: 22px;
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: 0;
  text-shadow: 0 2px 4px rgba(var(--skin-brand-strong-rgb, 0,91,176),.12);
  white-space: nowrap;
}

.hero-copy p {
  margin: 5px 0 0;
  color: rgba(255,255,255,.82);
  font-size: 11px;
  line-height: 1.35;
  white-space: nowrap;
}

.hero-art {
  position: absolute;
  z-index: 1;
  top: 0;
  right: 0;
  width: 102px;
  height: 78px;
  overflow: hidden;
  pointer-events: none;
  -webkit-mask-image: linear-gradient(90deg, transparent 0, #000 16%, #000 100%);
  mask-image: linear-gradient(90deg, transparent 0, #000 16%, #000 100%);
}

.hero-art img {
  position: absolute;
  top: 0;
  right: 0;
  display: block;
  width: 375px;
  max-width: none;
  height: auto;
}

.dispatch-content {
  position: relative;
  z-index: 2;
  padding: 0 12px 14px;
  background: var(--skin-surface, #f3f8fd);
}

.platform-card {
  display: flex;
  min-width: 0;
  min-height: 68px;
  align-items: center;
  justify-content: space-between;
  gap: 9px;
  margin-top: -1px;
  padding: 11px 12px 11px 16px;
  border: 1px solid rgba(255,255,255,.95);
  border-radius: 10px;
  background: rgba(255,255,255,.94);
  box-shadow: 0 5px 16px rgba(var(--skin-brand-strong-rgb, 40,104,157),.10);
}

.platform-copy {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  gap: 9px;
}

.platform-copy > div { min-width: 0; }

.section-mark {
  display: block;
  width: 5px;
  height: 17px;
  flex: 0 0 5px;
  margin-top: 1px;
  border-radius: 4px;
  background: var(--dispatch-brand);
}

.platform-copy h2 {
  margin: 0;
  color: var(--dispatch-ink);
  font-size: 16px;
  font-weight: 800;
  line-height: 1.2;
}

.platform-copy p {
  margin: 8px 0 0;
  overflow: hidden;
  color: var(--dispatch-muted);
  font-size: 11px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.refresh-button {
  display: inline-flex;
  width: 91px;
  min-width: 91px;
  height: 35px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 9px;
  border: 0;
  border-radius: 8px;
  color: #fff;
  background: linear-gradient(135deg, var(--skin-brand-soft, #32a6fb), var(--skin-brand, #087fe4));
  box-shadow: 0 5px 10px rgba(var(--dispatch-brand-rgb),.22);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
  transition: transform 160ms ease, opacity 160ms ease;
}

.refresh-button:active { transform: scale(.97); }
.refresh-button:disabled { cursor: wait; opacity: .72; }
.refresh-button i { font-size: 15px; }

.dispatch-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  margin: 12px 0 13px;
}

.dispatch-stat {
  display: grid;
  min-width: 0;
  height: 95px;
  align-content: center;
  justify-items: center;
  padding: 8px 2px;
  border: 1px solid rgba(var(--skin-border-rgb, 222,234,245),.9);
  border-radius: 9px;
  background: rgba(255,255,255,.96);
  box-shadow: 0 4px 13px rgba(var(--skin-brand-strong-rgb, 34,92,139),.06);
}

.stat-icon {
  display: grid;
  width: 33px;
  height: 33px;
  place-items: center;
  border-radius: 11px;
  font-size: 17px;
}

.stat-label {
  width: 100%;
  margin-top: 7px;
  overflow: hidden;
  color: var(--skin-muted, #5f7591);
  font-size: 11px;
  line-height: 1.2;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dispatch-stat strong {
  margin-top: 5px;
  color: var(--skin-ink, #142f55);
  font-size: 23px;
  font-weight: 800;
  line-height: 1;
}

.stat-pending .stat-icon,
.stat-approved .stat-icon { color: var(--skin-brand, #087ed9); background: rgba(var(--skin-brand-rgb, 8,126,217),.1); }
.stat-delivering .stat-icon { color: #0aad82; background: #ddf7ed; }
.stat-exception .stat-icon { color: #eb5538; background: #ffebe8; }

.dispatch-workspace {
  min-width: 0;
  overflow: hidden;
  border: 1px solid rgba(var(--skin-border-rgb, 222,234,245),.92);
  border-radius: 10px;
  background: rgba(255,255,255,.97);
  box-shadow: 0 5px 16px rgba(var(--skin-brand-strong-rgb, 37,91,135),.07);
}

.workspace-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: 0;
  border-bottom: 1px solid var(--dispatch-border);
}

.workspace-tabs button {
  position: relative;
  display: inline-flex;
  min-width: 0;
  height: 43px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 6px;
  border: 0;
  color: var(--skin-muted, #536b89);
  background: transparent;
  font-size: 14px;
  font-weight: 750;
  cursor: pointer;
}

.workspace-tabs button.active { color: var(--skin-brand, #087fdc); }

.workspace-tabs button.active::after {
  content: '';
  position: absolute;
  right: 10%;
  bottom: -1px;
  left: 10%;
  height: 3px;
  border-radius: 3px 3px 0 0;
  background: var(--dispatch-brand);
}

.workspace-tabs b {
  display: grid;
  min-width: 21px;
  height: 21px;
  place-items: center;
  padding: 0 5px;
  border-radius: 50%;
  color: var(--skin-brand, #087fdc);
  background: rgba(var(--skin-brand-rgb, 8,127,220),.1);
  font-size: 11px;
  line-height: 1;
}

.list-heading {
  display: flex;
  min-width: 0;
  height: 54px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 0 12px;
}

.list-heading > div {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 9px;
}

.list-heading .section-mark { height: 18px; }

.list-heading h2 {
  margin: 0;
  color: var(--dispatch-ink);
  font-size: 17px;
  font-weight: 800;
  white-space: nowrap;
}

.status-select {
  position: relative;
  display: inline-flex;
  width: 78px;
  min-width: 78px;
  height: 30px;
  align-items: center;
  border: 1px solid var(--skin-border, #ceddeb);
  border-radius: 9px;
  color: var(--dispatch-ink);
  background: #fff;
}

.status-select select {
  appearance: none;
  width: 100%;
  height: 100%;
  padding: 0 22px 0 10px;
  border: 0;
  outline: 0;
  color: inherit;
  background: transparent;
  font-size: 11px;
  cursor: pointer;
}

.status-select i {
  position: absolute;
  right: 8px;
  pointer-events: none;
  font-size: 9px;
}

.application-list,
.exception-list {
  display: grid;
  min-width: 0;
  gap: 8px;
  padding: 0 11px 11px;
}

.application-card,
.exception-card {
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--skin-border, #dce9f4);
  border-radius: 9px;
  background: #fff;
  box-shadow: 0 3px 10px rgba(var(--skin-brand-strong-rgb, 28,77,119),.045);
}

.application-head,
.exception-head {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.rider-identity,
.exception-title {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.rider-identity > div,
.exception-title > div { min-width: 0; }

.rider-avatar {
  display: grid;
  width: 37px;
  height: 37px;
  flex: 0 0 37px;
  place-items: center;
  border-radius: 50%;
  color: var(--skin-brand, #0876c8);
  background: rgba(var(--skin-brand-rgb, 8,118,200),.11);
  font-size: 18px;
  font-weight: 800;
}

.rider-identity h3 {
  margin: 0;
  overflow: hidden;
  color: var(--dispatch-ink);
  font-size: 16px;
  font-weight: 800;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rider-identity p {
  margin: 3px 0 0;
  overflow: hidden;
  color: var(--skin-muted, #7e90a8);
  font-size: 11px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-badge {
  display: inline-flex;
  min-width: 0;
  min-height: 27px;
  flex: 0 0 auto;
  align-items: center;
  gap: 5px;
  padding: 0 9px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 750;
  white-space: nowrap;
}

.audit-badge i { font-size: 12px; }
.badge-approved { color: #078967; background: #e0f7ee; }
.badge-pending { color: #bd6e00; background: #fff1dc; }
.badge-rejected { color: #d34f45; background: #ffebe8; }

.application-info {
  display: grid;
  grid-template-columns: 1.08fr .78fr 1.28fr;
  min-width: 0;
  margin-top: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--skin-border, #e1ebf3);
}

.application-info > div {
  display: grid;
  grid-template-columns: 17px minmax(0, 1fr);
  min-width: 0;
  align-items: center;
  column-gap: 5px;
  padding: 0 7px;
  border-right: 1px solid var(--skin-border, #e1ebf3);
}

.application-info > div:first-child { padding-left: 0; }
.application-info > div:last-child { padding-right: 0; border-right: 0; }

.application-info i {
  grid-row: span 2;
  color: var(--skin-muted, #8ca0b8);
  font-size: 14px;
  text-align: center;
}

.application-info span {
  min-width: 0;
  overflow: hidden;
  color: var(--skin-muted, #8396ae);
  font-size: 10px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.application-info strong {
  min-width: 0;
  margin-top: 2px;
  overflow: hidden;
  color: var(--skin-ink, #17355b);
  font-size: 11px;
  font-weight: 500;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.application-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
}

.application-actions button {
  min-width: 58px;
  height: 32px;
  padding: 0 13px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 150ms ease, filter 150ms ease;
}

.application-actions button:active,
.resolution-actions button:active { transform: scale(.97); }

.approve-button {
  border: 0;
  color: #fff;
  background: linear-gradient(135deg, var(--skin-brand-soft, #32a6fb), var(--skin-brand, #087fe4));
  box-shadow: 0 4px 9px rgba(var(--dispatch-brand-rgb),.19);
}

.reject-button { border: 1px solid #ff776c; color: #ec4f47; background: #fff; }

.completed-note {
  min-height: 23px;
  margin: 0;
  padding-top: 7px;
  overflow-wrap: anywhere;
  color: var(--skin-muted, #7b8ea6);
  font-size: 10px;
  line-height: 1.45;
  text-align: right;
}

.exception-icon {
  display: grid;
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  place-items: center;
  border-radius: 10px;
  color: #e85d45;
  background: #fff0ec;
  font-size: 15px;
}

.exception-title small {
  display: block;
  overflow: hidden;
  color: var(--skin-muted, #8a9bb0);
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.exception-title h3 {
  margin: 3px 0 0;
  overflow: hidden;
  color: var(--dispatch-ink);
  font-size: 14px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.exception-description {
  margin: 9px 0;
  padding: 8px 9px;
  overflow-wrap: anywhere;
  border-radius: 7px;
  color: var(--skin-muted, #667d97);
  background: var(--skin-surface, #f5f9fc);
  font-size: 11px;
  line-height: 1.5;
}

.exception-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 5px;
  min-width: 0;
  color: var(--skin-muted, #7c90a8);
  font-size: 9px;
}

.exception-meta span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.exception-meta i { margin-right: 4px; color: var(--dispatch-brand); }

.resolution-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  margin-top: 9px;
  padding-top: 8px;
  border-top: 1px dashed var(--skin-border, #dce8f2);
}

.resolution-actions button {
  min-width: 0;
  height: 31px;
  padding: 0 4px;
  overflow: hidden;
  border: 1px solid var(--skin-border, #d7e4ef);
  border-radius: 7px;
  color: var(--skin-muted, #607791);
  background: #fff;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.resolution-actions button i { margin-right: 4px; }
.resolution-actions .resume-button { border-color: var(--dispatch-brand); color: #fff; background: var(--dispatch-brand); }
.resolution-actions .cancel-button { border-color: #f0b7b1; color: #d95148; }
.completed-note strong { margin-right: 7px; color: var(--dispatch-brand); }

.state-message,
.empty-state {
  display: grid;
  min-height: 165px;
  place-content: center;
  padding: 25px 15px;
  color: var(--dispatch-muted);
  text-align: center;
}

.state-message > i,
.empty-state > i {
  display: block;
  margin-bottom: 9px;
  color: var(--skin-brand-soft, #8dc7f5);
  font-size: 27px;
}

.state-message p,
.empty-state p { margin: 0; font-size: 12px; }

.modal-mask {
  position: fixed;
  z-index: 2000;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 16px;
  background: rgba(var(--skin-ink-rgb, 13,57,96),.55);
}

.modal {
  width: min(100%, 370px);
  max-height: calc(100dvh - 32px);
  padding: 22px;
  overflow-y: auto;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 18px 50px rgba(var(--skin-ink-rgb, 12,55,91),.2);
}

.modal-icon {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 11px;
  color: #e75d47;
  background: #fff0ec;
  font-size: 19px;
}

.modal h2 { margin: 14px 0 7px; color: var(--dispatch-ink); font-size: 19px; }
.modal > p { margin: 0; color: var(--dispatch-muted); font-size: 12px; line-height: 1.6; }

.modal textarea {
  width: 100%;
  min-height: 96px;
  margin: 15px 0;
  padding: 11px;
  border: 1px solid var(--dispatch-border);
  border-radius: 9px;
  outline: 0;
  resize: vertical;
  color: var(--dispatch-ink);
  background: #fff;
  font-size: 13px;
}

.modal textarea:focus { border-color: var(--dispatch-brand); box-shadow: 0 0 0 2px rgba(var(--dispatch-brand-rgb),.12); }
.modal-actions { display: flex; gap: 9px; }

.modal-actions button {
  flex: 1;
  min-height: 40px;
  border-radius: 9px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.modal-secondary { border: 1px solid var(--dispatch-border); color: var(--dispatch-muted); background: #fff; }
.modal-confirm { border: 0; color: #fff; background: var(--dispatch-brand); }

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0,0,0,0);
  white-space: nowrap;
  border: 0;
}

.spin { animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 359px) {
  .hero-copy { left: 49%; max-width: calc(100% - 104px); }
  .hero-copy h1 { font-size: 20px; }
  .hero-copy p { font-size: 10px; }
  .platform-card { padding-left: 12px; }
  .platform-copy p { max-width: 150px; }
  .refresh-button { width: 84px; min-width: 84px; padding-inline: 6px; font-size: 12px; }
  .dispatch-stats { gap: 4px; }
  .stat-label { font-size: 10px; }
  .workspace-tabs button { gap: 5px; font-size: 13px; }
  .application-info > div { padding-inline: 4px; }
  .application-info strong { font-size: 10px; }
}

@media (prefers-reduced-motion: reduce) {
  .spin { animation: none; }
  .refresh-button,
  .application-actions button,
  .resolution-actions button { transition: none; }
}
</style>
