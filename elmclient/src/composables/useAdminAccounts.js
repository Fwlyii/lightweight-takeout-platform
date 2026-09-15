import { computed, onMounted, ref } from 'vue';

const formatDate = value => {
  if (!value) return '未知时间';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '未知时间';
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};
const toListItem = item => ({
  userId: item.id, username: item.username, phone: item.phone || '未填写',
  email: item.email || '未填写', disabled: !item.activated,
  registerDate: formatDate(item.createTime), photo: item.photo || ''
});
const errorMessage = error => error.response?.data?.message || error.message || '请求失败，请重试';

export function useAdminAccounts(api, notifications) {
  const searchKeyword = ref(''), activeFilter = ref('all'), users = ref([]);
  const selectedUser = ref(null), showConfirmModal = ref(false);
  const loading = ref(false), updating = ref(false);
  const filterStatus = computed(() => ({ all: 0, enabled: 1, disabled: 2 }[activeFilter.value]));
  let loadSequence = 0;

  async function load() {
    const sequence = ++loadSequence;
    loading.value = true;
    try {
      const data = await api.list({ status: filterStatus.value, keyword: searchKeyword.value.trim() });
      // A slow earlier search must not replace the latest search results.
      if (sequence === loadSequence) users.value = data.map(toListItem);
    } catch (error) {
      if (sequence === loadSequence) notifications.error(errorMessage(error));
    } finally {
      if (sequence === loadSequence) loading.value = false;
    }
  }
  function setFilter(filter) { activeFilter.value = filter; return load(); }
  function toggleUserStatus(user) { selectedUser.value = user; showConfirmModal.value = true; }
  async function confirmToggle() {
    if (!selectedUser.value || updating.value) return;
    const user = selectedUser.value;
    const activated = user.disabled;
    updating.value = true;
    try {
      await api.setActivated(user.userId, activated);
      showConfirmModal.value = false;
      notifications.success(`用户已${activated ? '启用' : '禁用'}`);
      await load();
    } catch (error) { notifications.error(errorMessage(error)); }
    finally { updating.value = false; }
  }
  onMounted(load);
  return { searchKeyword, activeFilter, users, selectedUser, showConfirmModal, loading, updating,
    handleSearch: load, setFilter, toggleUserStatus, confirmToggle };
}
