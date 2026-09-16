<template>
    <main class="rider-page" :class="{ 'dispatch-page': activeTab === 'available', 'detail-page': selectedTaskId }">
        <header class="topbar">
            <div class="brand"><span class="logo"><i class="fas fa-motorcycle"></i></span>
                <div><b>配送工作台</b><small>骑手服务中心</small></div>
            </div>
            <div class="top-actions">
                <button v-if="activeTab === 'available' && profile?.auditStatus === 1" class="online-pill" :disabled="switching" @click="toggleOnline"><span :class="{ online: profile.online }"></span>{{ profile.online ? '在线接单' : '休息中' }}<i class="fas fa-chevron-down"></i></button>
                <button class="avatar" @click="$router.push('/myInformation?role=rider')">{{ profile?.realName?.slice(0,
                    1) ||
                    '骑' }}</button>
            </div>
        </header>

        <section v-if="loading" class="loading-card"><span class="spinner"></span>正在同步配送网络…</section>
        <section v-else-if="!profile || profile.auditStatus !== 1" class="loading-card">
            <i class="fas fa-id-card"></i>
            <h2>骑手资质尚未生效</h2>
            <p>请先完成申请与审核。</p>
            <button class="primary" @click="$router.push('/rider/apply')">查看申请状态</button>
        </section>

        <template v-else>
            <section class="hero-card">
                <img class="hero-illustration" :src="activeTab === 'available' ? '/images/rider/dispatch-hero.jpg' : '/images/rider/orders-hero.jpg'" alt="" />
                <div>
                    <p class="date">{{ todayText }}</p>
                    <h1>{{ greeting }}，{{ profile.realName }}</h1>
                    <p class="hero-note">{{ profile.online ? '已上线，可以查看和接取附近订单。' : '当前为休息状态，上线后可接取新任务。' }}</p>
                </div>
                <button v-if="activeTab !== 'available'" class="online-switch" :class="{ online: profile.online }" :disabled="switching"
                    @click="toggleOnline">
                    <span class="switch-dot"></span><span><b>{{ profile.online ? '接单中' : '已休息' }}</b><small>{{
                        profile.online ?
                            '点击下线' : '点击上线' }}</small></span>
                </button>
            </section>

            <section class="stat-grid" :class="{ 'rider-stats-ready': statsEntered }" @animationend="finishStats">
                <article><span class="stat-icon green"><i class="fas fa-route"></i></span>
                    <div><small>累计里程</small><strong>{{ number(profile.totalDistance, 1) }}<em> km</em></strong></div>
                </article>
                <article><span class="stat-icon blue"><i class="fas fa-check-circle"></i></span>
                    <div><small>完成配送</small><strong>{{ profile.completedOrders || 0 }}<em> 单</em></strong></div>
                </article>
                <article><span class="stat-icon orange"><i class="fas fa-wallet"></i></span>
                    <div><small>累计收入</small><strong><em>¥</em>{{ number(profile.totalIncome, 2) }}</strong></div>
                </article>
                <article><span class="stat-icon purple"><i class="fas fa-layer-group"></i></span>
                    <div><small>当前任务</small><strong>{{ activeTasks.length }}<em> 单</em></strong></div>
                </article>
            </section>

            <aside v-if="activeTab === 'available'" class="rider-notice"><i class="fas fa-bell"></i><b>骑行提醒</b><span>请注意行车安全，合理安排配送时间。</span></aside>
            <section class="workspace">
                <nav ref="tabBar" class="tabs">
                    <span class="rider-sliding-indicator" aria-hidden="true"></span>
                    <button v-for="tab in visibleTabs" :key="tab.key" :class="{ active: activeTab === tab.key }"
                        @click="selectTab(tab.key)">
                        <i :class="tab.icon"></i>{{ tab.label }}<b v-if="countFor(tab.key)">{{ countFor(tab.key) }}</b>
                    </button>
                    <button class="refresh" :disabled="refreshing" :aria-busy="refreshing" @click="refreshAll"><i class="fas fa-sync-alt"
                            :class="{ spin: refreshing }"></i>
                        刷新</button>
                </nav>

                <div v-if="selectedTaskId" class="detail-toolbar"><button class="text-button" @click="selectedTaskId = null; successNotice = false"><i class="fas fa-chevron-left"></i> 返回订单列表</button><span>配送详情</span></div>
                <aside v-if="successNotice && selectedTaskId" class="success-notice" role="status"><i class="fas fa-check-circle"></i><div><b>抢单成功，请安全前往商家</b><p>请尽快到达商家取餐，注意交通安全</p></div><button aria-label="关闭提示" @click="successNotice = false">×</button></aside>
                <Transition name="rider-feedback">
                    <aside v-if="actionFeedback?.phase === 'success' && actionFeedback.action !== 'accept'" :key="actionFeedback.sequence" class="rider-state-feedback" :class="{ 'rider-completion': actionFeedback.action === 'deliver' }" role="status">
                        <svg class="rider-success-check" viewBox="0 0 40 40" aria-hidden="true"><circle cx="20" cy="20" r="18"/><path d="m11 20 6 6 12-13" pathLength="1"/></svg>
                        <div><b>{{ actionSuccessText[actionFeedback.action] }}</b><small v-if="actionFeedback.action === 'deliver'">已送达，等待顾客确认 · 配送费 <strong>¥{{ number(actionFeedback.fee, 2) }}</strong></small></div>
                    </aside>
                </Transition>
                <Transition name="rider-feedback"><p v-if="actionError" class="rider-action-error" role="alert">{{ actionError }}</p></Transition>
                <div v-if="activeTab === 'available' && !profile.online" class="empty">
                    <span><i class="fas fa-power-off"></i></span>
                    <h3>上线后接收附近订单</h3>
                    <p>休息状态不会为你分配新任务。</p><button class="primary small" @click="toggleOnline">立即上线</button>
                </div>
                <div v-else-if="visibleTasks.length === 0" class="empty">
                    <span><i class="fas fa-mug-hot"></i></span>
                    <h3>{{ emptyTitle }}</h3>
                    <p>配送网络会自动同步最新状态。</p>
                </div>

                <TransitionGroup name="rider-list" type="transition" tag="div" class="task-grid" :class="{ 'rider-grid-empty': !displayedTasks.length }" appear @before-leave="prepareCardLeave" @after-leave="resetCardLeave" @leave-cancelled="resetCardLeave">
                    <article v-for="task in (activeTab === 'available' && !profile.online ? [] : displayedTasks)" :key="task.id" class="task-card"
                        :class="[task.taskStatus.toLowerCase(), { expanded: selectedTaskId === task.id, 'rider-new-order': newOrderIds.has(task.id) }]" @animationend.self="finishNewOrder(task.id, $event)">
                        <div class="task-head">
                            <div><span class="task-id">{{ activeTab === 'available' ? '#' : 'DELIVERY #' }}{{ task.id }}</span>
                                <h3><button class="task-title" :aria-expanded="selectedTaskId === task.id" @click="selectedTaskId = selectedTaskId === task.id ? null : task.id; successNotice = false">{{ task.businessName }}<i v-if="!selectedTaskId" class="fas fa-chevron-right" aria-hidden="true"></i></button></h3>
                            </div>
                            <span class="status" :class="task.taskStatus.toLowerCase()">{{ taskStatus(task.taskStatus)
                            }}</span>
                            <strong v-if="activeTab === 'available'" class="available-fee"><small>¥</small>{{ number(task.riderFee, 2) }}</strong>
                        </div>
                        <div class="route">
                            <div class="route-mark"><i class="fas fa-store"></i><span></span><i
                                    class="fas fa-map-marker-alt"></i>
                            </div>
                            <div class="route-copy">
                                <div><small>取餐地址</small>
                                    <p>{{ task.businessAddress }}</p>
                                </div>
                                <div><small>送达地址</small>
                                    <p>{{ task.deliveryAddress }}</p><em
                                        v-if="task.contactName && task.contactName !== '接单后可见'">{{
                                            task.contactName }} · {{ task.contactTel }}</em>
                                </div>
                            </div>
                        </div>
                        <div class="task-meta">
                            <span><i class="fas fa-road"></i><div><b>{{ task.distanceKm == null ? '暂无' : number(task.distanceKm, 1) + ' km' }}</b><small>直线距离</small></div></span>
                            <span><i class="fas fa-coins"></i><div><b>¥{{ number(task.riderFee, 2) }}</b><small>{{ activeTab === 'history' ? '配送费' : '预计收入' }}</small></div></span>
                            <span><i class="fas fa-receipt"></i><div><b>¥{{ number(task.orderTotal, 2) }}</b><small>订单金额</small></div></span>
                        </div>
                        <ol v-if="selectedTaskId && ['ACCEPTED', 'ARRIVED_STORE', 'DELIVERING', 'DELIVERED', 'COMPLETED'].includes(task.taskStatus)" class="delivery-progress" aria-label="配送进度">
                            <li v-for="(step, index) in deliverySteps" :key="step.title" :class="{ reached: progressIndex(task) >= index, current: progressIndex(task) === index, done: progressIndex(task) > index || ['DELIVERED', 'COMPLETED'].includes(task.taskStatus) }" :aria-current="progressIndex(task) === index ? 'step' : undefined"><span><i v-if="progressIndex(task) > index || ['DELIVERED', 'COMPLETED'].includes(task.taskStatus)" class="fa fa-check" aria-hidden="true"></i></span><b>{{ step.title }}</b><small>{{ step.note }}</small></li>
                        </ol>
                        <div class="task-actions">
                            <button v-if="task.taskStatus === 'WAITING_RIDER'" class="accept"
                                :disabled="actingId !== null" :aria-busy="isActionPending(task)" @click="act(task, 'accept')"><RiderActionFeedback label="接取订单" pending="正在接单" success-label="接单成功" :busy="isActionPending(task)" :success="isActionSuccess(task, 'accept')" /></button>
                            <template v-else-if="task.taskStatus === 'ACCEPTED'">
                                <button class="ghost" @click="navigate(task)"><i
                                        class="fas fa-location-arrow"></i>
                                    导航去商家</button><button class="accept"
                                    :disabled="actingId !== null" :aria-busy="isActionPending(task)" @click="act(task, 'arrive-store')"><RiderActionFeedback label="我已到店" pending="正在确认" :busy="isActionPending(task)" :success="isActionSuccess(task, 'arrive-store')" /></button>
                            </template>
                            <template v-else-if="task.taskStatus === 'ARRIVED_STORE'">
                                <button class="ghost danger" @click="openException(task)">上报异常</button><button
                                    class="accept" :disabled="actingId !== null" :aria-busy="isActionPending(task)" @click="act(task, 'pickup')"><RiderActionFeedback label="确认取餐" pending="正在取餐" :busy="isActionPending(task)" :success="isActionSuccess(task, 'pickup')" /></button>
                            </template>
                            <template v-else-if="task.taskStatus === 'DELIVERING'">
                                <button class="ghost" @click="navigate(task)"><i
                                        class="fas fa-location-arrow"></i>
                                    导航去顾客</button><button class="accept" :disabled="actingId !== null" :aria-busy="isActionPending(task)" @click="act(task, 'deliver')"><RiderActionFeedback label="确认送达" pending="正在送达" :busy="isActionPending(task)" :success="isActionSuccess(task, 'deliver')" /></button>
                                <button class="exception-link" @click="openException(task)">遇到配送问题？</button>
                            </template>
                            <div v-else-if="task.taskStatus === 'DELIVERED'" class="waiting-confirm"><i
                                    class="fas fa-clock"></i>
                                已送达，等待顾客确认</div>
                            <div v-else-if="task.taskStatus === 'EXCEPTION'" class="waiting-confirm warning"><i
                                    class="fas fa-headset"></i>
                                管理员正在处理异常</div>
                            <div v-else class="complete-note"><i class="fas fa-check"></i>
                                {{ task.completedTime ? '完成于 ' + formatTime(task.completedTime) : '任务已结束' }}
                            </div>
                        </div>
                    </article>
                </TransitionGroup>
            </section>
            <aside class="safety-card"><span><i class="fas fa-shield-alt"></i><i class="fas fa-check safety-check"></i></span><div><b>安全第一　平安配送</b><p>遵守交通规则 · 佩戴头盔 · 安全送达每一单</p></div></aside>
        </template>

        <div v-if="exceptionModal" class="modal-mask" @click.self="exceptionModal = null">
            <form class="modal" @submit.prevent="submitException">
                <span class="modal-icon"><i class="fas fa-exclamation-triangle"></i></span>
                <h2>上报配送异常</h2>
                <p>任务 #{{ exceptionModal.id }} 将暂停履约，并立即通知调度管理员。</p>
                <label>异常类型<select v-model="exceptionForm.exceptionType">
                        <option v-for="item in exceptionTypes" :key="item.value" :value="item.value">{{ item.label }}
                        </option>
                    </select></label>
                <label>情况说明<textarea v-model.trim="exceptionForm.description" maxlength="500"
                        placeholder="说明现场情况，便于调度快速处理" required></textarea></label>
                <div class="modal-actions"><button type="button" class="ghost"
                        @click="exceptionModal = null">取消</button><button class="accept" type="submit">提交异常</button>
                </div>
            </form>
        </div>
    </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import { toast } from '@/utils/toast';
import { createRealtimeConnection } from '@/services/realtimeService';
import { taskStatusText } from '@/utils/orderPresentation';
import { openDeliveryNavigation } from '@/utils/deliveryNavigation';
import RiderActionFeedback from '@/components/RiderActionFeedback.vue';
import { useRiderIndicator } from '@/composables/useRiderIndicator';

const route = useRoute();
const router = useRouter();
const profile = ref(null), availableTasks = ref([]), activeTasks = ref([]), historyTasks = ref([]);
const loading = ref(true), refreshing = ref(false), switching = ref(false), actingId = ref(null), activeTab = ref('available');
const exceptionModal = ref(null);
const selectedTaskId = ref(null), successNotice = ref(false);
const tabBar = ref(null);
useRiderIndicator(tabBar);
const statsEntered = ref(false);
const finishStats = event => {
    if (event.animationName === 'rider-stat-in' && event.target === event.currentTarget.lastElementChild) statsEntered.value = true;
};
const actionFeedback = ref(null), actionError = ref('');
const actionSuccessText = { 'arrive-store': '到店成功，请核对餐品', pickup: '取餐成功，请安全送往顾客', deliver: '送达成功，辛苦了' };
let feedbackTimer, feedbackSequence = 0;
const isActionPending = task => actingId.value === task.id && actionFeedback.value?.phase === 'pending';
const isActionSuccess = (task, action) => actingId.value === task.id && actionFeedback.value?.phase === 'success' && actionFeedback.value.action === action;
const seenOrderIds = new Set(), newOrderIds = ref(new Set());
watch(availableTasks, tasks => {
    const fresh = new Set([...newOrderIds.value].filter(id => tasks.some(task => task.id === id)));
    for (const task of tasks) {
        if (!seenOrderIds.has(task.id)) fresh.add(task.id);
        seenOrderIds.add(task.id);
    }
    newOrderIds.value = fresh;
});
const finishNewOrder = (id, event) => {
    if (event.animationName !== 'rider-order-glow') return;
    const next = new Set(newOrderIds.value); next.delete(id); newOrderIds.value = next;
};
// Pin leaving cards to their measured position so TransitionGroup can move siblings.
const prepareCardLeave = element => {
    Object.assign(element.style, { left: `${element.offsetLeft}px`, top: `${element.offsetTop}px`, width: `${element.offsetWidth}px`, height: `${element.offsetHeight}px` });
};
const resetCardLeave = element => { for (const property of ['left', 'top', 'width', 'height']) element.style[property] = ''; };
const deliverySteps = [{ title: '前往商家', note: '请尽快到店取餐' }, { title: '到店取餐', note: '核对餐品并取餐' }, { title: '送达顾客', note: '完成配送' }];
const progressIndex = task => task.taskStatus === 'ACCEPTED' ? 0 : task.taskStatus === 'ARRIVED_STORE' ? 1 : 2;
let realtimeConnection = null;
const exceptionForm = reactive({ exceptionType: 'STORE_DELAY', description: '' });
const tabs = [
    { key: 'available', label: '接单广场', icon: 'fas fa-compass' },
    { key: 'active', label: '进行中', icon: 'fas fa-route' },
    { key: 'history', label: '历史配送', icon: 'fas fa-history' }
];
const exceptionTypes = [
    { value: 'STORE_DELAY', label: '商家出餐延迟' }, { value: 'CUSTOMER_UNREACHABLE', label: '无法联系顾客' },
    { value: 'ADDRESS_ERROR', label: '收货地址异常' }, { value: 'VEHICLE_FAILURE', label: '车辆故障' }, { value: 'OTHER', label: '其他问题' }
];

const todayText = new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date());
const greeting = computed(() => { const h = new Date().getHours(); return h < 11 ? '早上好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好'; });
const visibleTasks = computed(() => activeTab.value === 'available' ? availableTasks.value : activeTab.value === 'active' ? activeTasks.value : historyTasks.value);
const displayedTasks = computed(() => selectedTaskId.value ? visibleTasks.value.filter(task => task.id === selectedTaskId.value) : visibleTasks.value);
watch(visibleTasks, tasks => { if (selectedTaskId.value && !tasks.some(task => task.id === selectedTaskId.value)) selectedTaskId.value = null; });
const emptyTitle = computed(() => activeTab.value === 'available' ? '暂时没有新任务' : activeTab.value === 'active' ? '没有进行中的配送' : '还没有历史配送');
const countFor = key => key === 'available' ? availableTasks.value.length : key === 'active' ? activeTasks.value.length : historyTasks.value.length;
const visibleTabs = computed(() => activeTab.value === 'available'
    ? [{ key: 'available', label: '待接单', icon: 'fas fa-list-alt' }, ...tabs.filter(tab => tab.key !== 'available')]
    : tabs.filter(tab => tab.key !== 'available'));
const number = (value, digits) => Number(value || 0).toFixed(digits);
const taskStatus = taskStatusText;
const formatTime = value => new Date(value).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' });
const selectTab = tab => {
    selectedTaskId.value = null;
    successNotice.value = false;
    activeTab.value = tab;
    router.replace({ query: { ...route.query, tab } });
};
watch(() => route.query.tab, tab => {
    if (['available', 'active', 'history'].includes(tab)) activeTab.value = tab;
}, { immediate: true });

const loadProfile = async () => { const response = await request.get('/api/v1/riders/me'); profile.value = response.data; };
const loadTasks = async () => {
    const calls = [request.get('/api/v1/riders/me/tasks?active=true'), request.get('/api/v1/riders/me/tasks?active=false')];
    if (profile.value?.online) calls.push(request.get('/api/v1/riders/available-tasks'));
    const [active, history, available] = await Promise.all(calls);
    activeTasks.value = active.data || []; historyTasks.value = history.data || []; availableTasks.value = available?.data || [];
};
const refreshAll = async ({ silent = false } = {}) => { if (!silent) refreshing.value = true; try { await loadProfile(); if (profile.value?.auditStatus === 1) await loadTasks(); } catch (e) { if (!silent) toast.error(e.response?.data?.message || '数据同步失败'); } finally { if (!silent) refreshing.value = false; } };
const toggleOnline = async () => { switching.value = true; try { const res = await request.patch('/api/v1/riders/me/online', { online: !profile.value.online }); profile.value = res.data; await loadTasks(); toast.success(profile.value.online ? '已上线，可以接单了' : '已安全下线'); } catch (e) { toast.error(e.response?.data?.message || '状态更新失败'); } finally { switching.value = false; } };
const act = async (task, action) => {
    if (actingId.value !== null) return;
    actingId.value = task.id;
    clearTimeout(feedbackTimer);
    actionError.value = '';
    actionFeedback.value = { id: task.id, action, phase: 'pending', fee: task.riderFee, sequence: ++feedbackSequence };
    try {
        await request.post(`/api/v1/delivery-tasks/${task.id}/${action}`);
        actionFeedback.value = { ...actionFeedback.value, phase: 'success' };
        await refreshAll();
        activeTab.value = 'active'; selectedTaskId.value = task.id; successNotice.value = action === 'accept';
        await router.replace({ query: { ...route.query, tab: 'active' } });
    } catch (e) {
        actionFeedback.value = null;
        actionError.value = e.response?.data?.message || '操作失败，请重试';
    } finally {
        actingId.value = null;
        // Only dismiss presentation feedback; never delay a request or route change.
        feedbackTimer = setTimeout(() => { actionFeedback.value = null; actionError.value = ''; }, 2600);
    }
};
const navigate = task => openDeliveryNavigation(task.id, {
    request, openWindow: () => window.open('about:blank', '_blank'), notify: message => toast.error(message)
});
const openException = task => { exceptionModal.value = task; exceptionForm.exceptionType = 'STORE_DELAY'; exceptionForm.description = ''; };
const submitException = async () => { if (!exceptionForm.description) return toast.warning('请填写情况说明'); try { await request.post(`/api/v1/delivery-tasks/${exceptionModal.value.id}/exceptions`, exceptionForm); toast.success('异常已上报，调度员将尽快处理'); exceptionModal.value = null; await refreshAll(); } catch (e) { toast.error(e.response?.data?.message || '上报失败'); } };
onMounted(async () => { try { await loadProfile(); if (!profile.value) { router.replace('/rider/apply'); return; } if (profile.value.auditStatus === 1) { await loadTasks(); realtimeConnection = createRealtimeConnection({ onMessage: message => { if (message.type === 'delivery_update') refreshAll({ silent: true }); }, onFallbackRefresh: () => refreshAll({ silent: true }) }); realtimeConnection.start(); } } catch (e) { toast.error(e.response?.data?.message || '加载失败'); } finally { loading.value = false; } });
onUnmounted(() => { realtimeConnection?.stop(); clearTimeout(feedbackTimer); });
</script>

<style scoped src="@/assets/styles/rider-dashboard.css"></style>
