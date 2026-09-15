<template>
  <main class="manage-user-container" aria-labelledby="user-management-title">
    <div class="container">
      <header class="user-management-hero">
        <button class="hero-back" type="button" aria-label="返回管理员首页" @click="router.push('/admin/home')">
          <i class="fa fa-chevron-left" aria-hidden="true"></i>
          <span>返回</span>
        </button>
        <div class="hero-copy">
          <h1 id="user-management-title">用户管理</h1>
          <p>管理平台用户，保障系统安全运行</p>
        </div>
        <i class="hero-art fa fa-motorcycle" aria-hidden="true"></i>
      </header>

      <section class="search-section" aria-label="用户搜索">
        <label class="search-box">
          <i class="fa fa-search" aria-hidden="true"></i>
          <span class="sr-only">搜索用户名、手机号或邮箱</span>
          <input v-model="searchKeyword" type="search" placeholder="搜索用户名、手机号或邮箱" @input="handleSearch">
        </label>
      </section>

      <nav class="filter-section" aria-label="用户状态筛选">
        <div class="filter-tabs" role="tablist">
          <button type="button" role="tab" :aria-selected="activeFilter === 'all'" :class="{ active: activeFilter === 'all' }" @click="setFilter('all')">全部用户</button>
          <button type="button" role="tab" :aria-selected="activeFilter === 'enabled'" :class="{ active: activeFilter === 'enabled' }" @click="setFilter('enabled')">已启用</button>
          <button type="button" role="tab" :aria-selected="activeFilter === 'disabled'" :class="{ active: activeFilter === 'disabled' }" @click="setFilter('disabled')">已禁用</button>
        </div>
      </nav>

      <p v-if="loading" class="user-list-state" role="status" aria-live="polite">正在加载用户…</p>
      <section v-else class="user-list" aria-label="用户列表">
        <article v-for="user in users" :key="user.userId" class="user-item">
          <div class="user-avatar" :class="avatarTone(user)">
            <img v-if="user.photo" :src="user.photo" alt="" @error="hideBrokenAvatar">
            <i class="fa fa-user" aria-hidden="true"></i>
          </div>
          <div class="user-info">
            <h2 class="user-name">{{ user.username }}</h2>
            <p class="contact-row"><i class="fa fa-phone" aria-hidden="true"></i><span>{{ user.phone }}</span></p>
            <p class="contact-row"><i class="fa fa-envelope" aria-hidden="true"></i><span>{{ user.email }}</span></p>
          </div>
          <div class="user-actions">
            <button class="action-btn" :class="user.disabled ? 'enable-btn' : 'disable-btn'" :disabled="updating" type="button" @click="toggleUserStatus(user)">
              {{ user.disabled ? '启用' : '禁用' }}
            </button>
          </div>
          <div class="user-status">
            <span class="status-badge" :class="user.disabled ? 'status-disabled' : 'status-enabled'"><i aria-hidden="true"></i>{{ user.disabled ? '已禁用' : '已启用' }}</span>
            <span class="status-divider" aria-hidden="true"></span>
            <time class="register-date"><i class="fa fa-clock-o" aria-hidden="true"></i>注册时间：{{ user.registerDate }}</time>
          </div>
        </article>
      </section>

      <div v-if="!loading && users.length === 0" class="empty-state">
        <i class="fa fa-users" aria-hidden="true"></i>
        <p>暂无用户数据</p>
      </div>

      <div v-if="showConfirmModal" class="modal-overlay" role="presentation" @click.self="showConfirmModal = false">
        <section class="modal-content" role="dialog" aria-modal="true" aria-labelledby="user-status-dialog-title">
          <header class="modal-header">
            <h2 id="user-status-dialog-title">确认操作</h2>
            <button class="close-btn" type="button" aria-label="关闭" @click="showConfirmModal = false"><i class="fa fa-times" aria-hidden="true"></i></button>
          </header>
          <div class="modal-body">
            <p>确定要{{ selectedUser?.disabled ? '启用' : '禁用' }}用户“{{ selectedUser?.username }}”吗？</p>
          </div>
          <footer class="modal-footer">
            <button class="modal-btn confirm-btn" type="button" :disabled="updating" @click="confirmToggle">{{ updating ? '处理中…' : '确认' }}</button>
            <button class="modal-btn cancel-btn" type="button" @click="showConfirmModal = false">取消</button>
          </footer>
        </section>
      </div>
    </div>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { useAdminAccounts } from '../composables/useAdminAccounts';
import adminAccounts from '../services/adminAccountService';
import { toast } from '../utils/toast';

const router = useRouter();
const { searchKeyword, activeFilter, users, selectedUser, showConfirmModal, loading, updating,
  handleSearch, setFilter, toggleUserStatus, confirmToggle } = useAdminAccounts(adminAccounts, toast);

const avatarTone = user => `avatar-${Math.abs(Number(user.userId) || 0) % 3}`;
const hideBrokenAvatar = event => {
  event.target.style.display = 'none';
  if (event.target.nextElementSibling) event.target.nextElementSibling.style.display = 'block';
};
</script>

<style scoped>
:global(*) { box-sizing: border-box; }
:global(body) { background: var(--skin-surface); }

.manage-user-container {
  min-height: 100%;
  padding-bottom: calc(76px + env(safe-area-inset-bottom));
  overflow-x: hidden;
  color: var(--skin-ink);
  background: var(--skin-surface);
}

.container {
  width: 100%;
  min-height: 100%;
  margin: 0;
  padding: 0 0 16px;
  background: var(--skin-surface);
}

.user-management-hero {
  position: relative;
  min-height: 132px;
  padding: 16px 16px 31px;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(145deg, var(--skin-brand-soft, #45b6ee), var(--skin-brand, #168bd1));
}
.user-management-hero::before,
.user-management-hero::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, .1);
  pointer-events: none;
}
.user-management-hero::before { width: 190px; height: 190px; top: -129px; right: -42px; }
.user-management-hero::after { width: 145px; height: 145px; top: -81px; left: 45%; }
.hero-back {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 16px 0 13px;
  border: 1px solid rgba(255, 255, 255, .32);
  border-radius: 999px;
  color: #fff;
  background: rgba(255, 255, 255, .11);
  font: inherit;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
}
.hero-back i { font-size: 18px; }
.hero-copy { position: relative; z-index: 1; margin-top: -2px; text-align: center; }
.hero-copy h1 { margin: 0; font-size: 27px; line-height: 1.2; letter-spacing: 0; }
.hero-copy p { margin: 7px 0 0; color: rgba(255, 255, 255, .86); font-size: 14px; }
.hero-art { position: absolute; z-index: 1; right: 19px; bottom: 17px; color: rgba(255, 255, 255, .25); font-size: 43px; transform: rotate(-4deg); }

.search-section { position: relative; z-index: 2; width: calc(100% - 24px); margin: -20px auto 0; }
.search-box { display: flex; align-items: center; min-height: 54px; padding: 0 18px; border: 1px solid rgba(255, 255, 255, .75); border-radius: 999px; background: rgba(255, 255, 255, .97); box-shadow: 0 9px 23px rgba(var(--skin-brand-strong-rgb), .14); }
.search-box i { flex: none; margin-right: 13px; color: var(--skin-brand-strong); font-size: 22px; }
.search-box input { width: 100%; min-width: 0; min-height: 48px; padding: 0; border: 0; outline: 0; color: var(--skin-ink); background: transparent; font: inherit; font-size: 16px; }
.search-box input::placeholder { color: var(--skin-muted); opacity: .9; }

.filter-section { width: calc(100% - 24px); margin: 18px auto 16px; }
.filter-tabs { display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px; padding: 5px; border-radius: 17px; background: rgba(255, 255, 255, .7); box-shadow: 0 5px 16px rgba(var(--skin-brand-strong-rgb), .07); }
.filter-tabs button { position: relative; min-height: 54px; padding: 0 4px; border: 0; border-radius: 13px; color: var(--skin-muted); background: transparent; font: inherit; font-size: 16px; font-weight: 600; cursor: pointer; transition: color 180ms ease, background 180ms ease, box-shadow 180ms ease; }
.filter-tabs button.active { color: #fff; background: var(--skin-brand); box-shadow: 0 6px 14px rgba(var(--skin-brand-rgb), .2); }
.filter-tabs button.active::after { content: ''; position: absolute; right: 38%; bottom: 7px; left: 38%; height: 3px; border-radius: 3px; background: rgba(255, 255, 255, .95); }

.user-list { display: grid; gap: 16px; width: calc(100% - 24px); margin: 0 auto; }
.user-item { display: grid; grid-template-columns: 52px minmax(0, 1fr) 76px; grid-template-rows: auto auto; column-gap: 13px; align-items: start; padding: 16px; border: 1px solid rgba(255, 255, 255, .88); border-radius: 16px; background: rgba(255, 255, 255, .97); box-shadow: 0 8px 24px rgba(var(--skin-brand-strong-rgb), .08); }
.user-avatar { display: grid; grid-row: 1; width: 52px; height: 52px; place-items: center; overflow: hidden; border-radius: 50%; }
.user-avatar img { display: block; width: 100%; height: 100%; object-fit: cover; }
.user-avatar i { display: block; color: currentColor; font-size: 27px; }
.user-avatar img + i { display: none; }
.avatar-0 { color: var(--admin-avatar-blue, #2196f3); background: color-mix(in srgb, var(--admin-avatar-blue, #2196f3) 14%, white); }
.avatar-1 { color: var(--admin-avatar-orange, #ff9f43); background: color-mix(in srgb, var(--admin-avatar-orange, #ff9f43) 15%, white); }
.avatar-2 { color: var(--admin-avatar-purple, #8e70e8); background: color-mix(in srgb, var(--admin-avatar-purple, #8e70e8) 14%, white); }
.user-info { min-width: 0; }
.user-name { margin: 0 0 7px; overflow: hidden; color: var(--skin-ink); font-size: 20px; line-height: 1.2; text-overflow: ellipsis; white-space: nowrap; }
.contact-row { display: flex; align-items: center; gap: 9px; min-width: 0; margin: 5px 0 0; color: var(--skin-muted); font-size: 14px; line-height: 1.25; }
.contact-row i { flex: none; width: 18px; color: var(--skin-muted); font-size: 17px; text-align: center; }
.contact-row span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.user-actions { display: flex; justify-content: flex-end; }
.action-btn { width: 76px; min-height: 42px; padding: 0 6px; border: 0; border-radius: 999px; color: #fff; font: inherit; font-size: 16px; font-weight: 700; cursor: pointer; transition: transform 160ms ease, filter 160ms ease; }
.action-btn:active { transform: scale(.96); }
.action-btn:disabled { cursor: wait; opacity: .6; }
.disable-btn { background: var(--admin-danger, #ef5b5b); box-shadow: 0 7px 14px rgba(239, 91, 91, .18); }
.enable-btn { background: var(--admin-success, #42ad61); box-shadow: 0 7px 14px rgba(66, 173, 97, .17); }
.user-status { display: flex; grid-column: 1 / -1; align-items: center; gap: 10px; min-width: 0; margin-top: 14px; }
.status-badge { display: inline-flex; align-items: center; gap: 8px; flex: none; min-height: 30px; padding: 0 13px; border-radius: 999px; font-size: 14px; font-weight: 700; }
.status-badge i { width: 9px; height: 9px; border-radius: 50%; background: currentColor; }
.status-enabled { color: #29994a; background: #e4f6e8; }
.status-disabled { color: #d74f4f; background: #ffebeb; }
.status-divider { width: 1px; height: 22px; flex: none; background: var(--skin-border); }
.register-date { display: flex; align-items: center; gap: 8px; min-width: 0; overflow: hidden; color: var(--skin-muted); font-size: 14px; white-space: nowrap; text-overflow: ellipsis; }
.register-date i { flex: none; color: var(--skin-muted); font-size: 18px; }

.user-list-state,
.empty-state { display: grid; place-items: center; min-height: 180px; margin: 0 12px; color: var(--skin-muted); text-align: center; }
.empty-state i { margin-bottom: 12px; color: var(--skin-border); font-size: 40px; }
.empty-state p { margin: 0; font-size: 16px; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }

.modal-overlay { position: fixed; z-index: 3000; inset: 0; display: grid; place-items: center; padding: 18px; background: rgba(var(--skin-brand-strong-rgb), .55); }
.modal-content { width: min(100%, 420px); overflow: hidden; border-radius: 16px; background: #fff; box-shadow: 0 18px 50px rgba(var(--skin-brand-strong-rgb), .24); animation: user-modal-in 220ms cubic-bezier(.22, .61, .36, 1) both; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 20px; border-bottom: 1px solid var(--skin-border); }
.modal-header h2 { margin: 0; color: var(--skin-ink); font-size: 18px; }
.close-btn { display: grid; width: 34px; height: 34px; place-items: center; border: 0; color: var(--skin-muted); background: transparent; font-size: 18px; cursor: pointer; }
.modal-body { padding: 20px; color: var(--skin-muted); font-size: 15px; line-height: 1.7; }
.modal-body p { margin: 0; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 20px 20px; }
.modal-btn { min-width: 78px; min-height: 40px; border: 0; border-radius: 9px; font: inherit; cursor: pointer; }
.modal-btn:disabled { cursor: wait; opacity: .6; }
.confirm-btn { color: #fff; background: var(--skin-brand); }
.cancel-btn { color: var(--skin-muted); background: var(--skin-surface); }
@keyframes user-modal-in { from { opacity: 0; transform: translateY(12px) scale(.98); } to { opacity: 1; transform: translateY(0) scale(1); } }

@media (max-width: 390px) {
  .user-management-hero { min-height: 126px; padding-inline: 12px; }
  .hero-copy h1 { font-size: 24px; }
  .hero-copy p { font-size: 13px; }
  .hero-back { min-height: 36px; padding-inline: 12px; font-size: 15px; }
  .hero-art { right: 12px; bottom: 15px; font-size: 36px; }
  .filter-tabs button { min-height: 50px; font-size: 14px; }
  .user-item { grid-template-columns: 48px minmax(0, 1fr) 70px; column-gap: 10px; padding: 14px; }
  .user-avatar { width: 48px; height: 48px; }
  .user-name { font-size: 18px; }
  .contact-row { gap: 7px; font-size: 13px; }
  .action-btn { width: 70px; min-height: 40px; font-size: 15px; }
  .status-badge { padding-inline: 10px; font-size: 13px; }
  .register-date { font-size: 12px; }
  .register-date i { font-size: 16px; }
}

@media (prefers-reduced-motion: reduce) {
  .filter-tabs button,
  .action-btn,
  .modal-content { transition: none; animation: none; }
}
</style>
