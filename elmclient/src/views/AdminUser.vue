<template>
    <div class="manage-user-container">
        <div class="container">
            <AdminPageHeader title="用户管理" />

            <div class="search-section">
                <div class="search-box">
                    <i class="fas fa-search"></i>
                    <input v-model="searchKeyword" type="text" placeholder="搜索用户名、手机号或邮箱" @input="handleSearch" />
                </div>
            </div>

            <div class="filter-section">
                <div class="filter-tabs">
                    <div class="filter-tab" :class="{ active: activeFilter === 'all' }" @click="setFilter('all')">
                        全部用户
                    </div>
                    <div class="filter-tab" :class="{ active: activeFilter === 'enabled' }"
                        @click="setFilter('enabled')">
                        已启用
                    </div>
                    <div class="filter-tab" :class="{ active: activeFilter === 'disabled' }"
                        @click="setFilter('disabled')">
                        已禁用
                    </div>
                </div>
            </div>

            <p v-if="loading" role="status">正在加载用户…</p>
            <div class="user-list">
                <div v-for="user in users" :key="user.userId" class="user-item">
                    <div class="user-avatar">
                        <i class="fas fa-user"></i>
                    </div>
                    <div class="user-info">
                        <div class="user-name">{{ user.username }}</div>
                        <div class="user-details">
                            <span class="user-phone">{{ user.phone }}</span>
                            <span class="user-email">{{ user.email }}</span>
                        </div>
                        <div class="user-status">
                            <span class="status-badge" :class="{
                                'status-enabled': !user.disabled,
                                'status-disabled': user.disabled
                            }">
                                {{ user.disabled ? '已禁用' : '已启用' }}
                            </span>
                            <span class="register-date">注册时间：{{ user.registerDate }}</span>
                        </div>
                    </div>
                    <div class="user-actions">
                        <button class="action-btn" :class="{
                            'enable-btn': user.disabled,
                            'disable-btn': !user.disabled
                        }" :disabled="updating" @click="toggleUserStatus(user)">
                            {{ user.disabled ? '启用' : '禁用' }}
                        </button>
                    </div>
                </div>
            </div>

            <div v-if="!loading && users.length === 0" class="empty-state">
                <i class="fas fa-users"></i>
                <p>暂无用户数据</p>
            </div>

            <!-- 确认对话框 - 完全保留原始样式 -->
            <div v-if="showConfirmModal" class="modal-overlay" @click.self="showConfirmModal = false">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>确认操作</h3>
                        <span class="close-btn" @click="showConfirmModal = false">&times;</span>
                    </div>
                    <div class="modal-body">
                        <p>确定要{{ selectedUser?.disabled ? '启用' : '禁用' }}用户 "{{ selectedUser?.username }}" 吗？</p>
                    </div>
                    <div class="modal-footer">
                        <button class="modal-btn confirm-btn" :disabled="updating" @click="confirmToggle">{{ updating ? '处理中…' : '确认' }}</button>
                        <button class="modal-btn cancel-btn" @click="showConfirmModal = false">取消</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { useAdminAccounts } from '../composables/useAdminAccounts';
import adminAccounts from '../services/adminAccountService';
import { toast } from '../utils/toast';
import AdminPageHeader from '../components/AdminPageHeader.vue';
const { searchKeyword, activeFilter, users, selectedUser, showConfirmModal, loading, updating,
    handleSearch, setFilter, toggleUserStatus, confirmToggle } = useAdminAccounts(adminAccounts, toast);
</script>

<style scoped>
.manage-user-container { width: 100%; max-width: 960px; margin: 0 auto; min-height: 100%; background: #f5f8fb; color: #253f54; padding-bottom: calc(84px + env(safe-area-inset-bottom)); }
.container { width: 100%; }
.search-section, .filter-section, .user-list { margin: 16px; }
.search-box { display: flex; gap: 10px; align-items: center; padding: 12px 14px; background: #fff; border: 1px solid #dbe5ed; border-radius: 10px; }
.search-box input { width: 100%; min-width: 0; border: 0; background: transparent; color: inherit; font: inherit; outline-offset: 4px; }
.filter-tabs { display: flex; gap: 4px; background: #eaf0f5; padding: 4px; border-radius: 10px; }
.filter-tab { flex: 1; min-height: 44px; border: 0; padding: 10px 4px; border-radius: 7px; text-align: center; color: #526d82; background: transparent; font: inherit; cursor: pointer; }
.filter-tab.active { background: #fff; color: #087ecc; box-shadow: 0 2px 5px #204c6812; }
.user-list { display: grid; gap: 12px; }
.user-item { display: flex; align-items: center; gap: 14px; padding: 16px; background: #fff; border: 1px solid #e0e9f0; border-radius: 12px; }
.user-avatar { flex: 0 0 44px; width: 44px; height: 44px; display: grid; place-items: center; background: #e9f5ff; border-radius: 50%; color: #087ecc; }
.user-info { min-width: 0; flex: 1; overflow-wrap: anywhere; }
.user-name { font-weight: 650; font-size: 16px; margin-bottom: 6px; }
.user-details, .user-status { display: flex; flex-wrap: wrap; gap: 6px 12px; color: #647c8d; font-size: 13px; line-height: 1.6; }
.user-status { margin-top: 6px; }
.status-badge { border-radius: 5px; padding: 0 7px; }
.status-enabled { color: #187955; background: #e8f7ef; }
.status-disabled { color: #a83848; background: #fff0f2; }
.action-btn, .modal-btn { min-height: 40px; padding: 8px 14px; border-radius: 8px; border: 1px solid #cbdde9; background: #fff; color: #087ecc; font: inherit; cursor: pointer; }
.disable-btn { color: #b54050; border-color: #f0cbd1; }
button:disabled { opacity: .5; cursor: wait; }
.empty-state, [role="status"] { padding: 22px 16px; text-align: center; color: #718798; }
.modal-overlay { position: fixed; inset: 0; z-index: 2000; display: grid; place-items: center; padding: 16px; background: #10283980; }
.modal-content { width: 100%; max-width: 420px; max-height: 85dvh; overflow: auto; padding: 20px; border-radius: 14px; background: #fff; }
.modal-header, .modal-footer { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.modal-header h3 { font-size: 18px; }
.close-btn { font-size: 24px; border: 0; color: #597285; background: transparent; cursor: pointer; min-width: 40px; min-height: 40px; }
.modal-body { padding: 18px 0; overflow-wrap: anywhere; line-height: 1.6; }
.modal-footer { justify-content: flex-end; }
.confirm-btn { color: white; background: #087ecc; }
@media (max-width: 420px) { .user-item { gap: 10px; padding: 12px; } .user-avatar { display: none; } .action-btn { padding: 8px 10px; } }
</style>
