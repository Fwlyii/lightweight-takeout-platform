// Injectable transport keeps endpoint contracts independently testable.
export function createAdminAccountApi(request) {
  const requireSuccess = response => {
    if (!response?.success) throw new Error(response?.message || '用户管理请求失败');
    return response.data;
  };
  return {
    async list({ status = 0, keyword = '' } = {}) {
      return requireSuccess(await request.get('/api/admin/users', { params: { status, keyword } }));
    },
    async setActivated(userId, activated) {
      requireSuccess(await request.put(`/api/admin/users/${userId}/status`, null, { params: { activated } }));
    }
  };
}
