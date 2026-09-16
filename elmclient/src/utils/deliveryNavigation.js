export async function openDeliveryNavigation(taskId, { request, openWindow, notify }) {
  const target = openWindow();
  if (!target) {
    notify('请允许弹出窗口后重试导航。');
    return;
  }
  target.opener = null;
  try {
    const response = await request.get(`/api/v1/delivery-tasks/${taskId}/navigation`);
    if (!response.success) throw new Error(response.message || '暂时无法获取导航地址');
    const url = new URL(response.data.navigationUrl);
    if (url.origin !== 'https://uri.amap.com' || !['/search', '/navigation'].includes(url.pathname) || url.username || url.password) {
      throw new Error('导航地址无效');
    }
    target.location.replace(url.href);
  } catch (error) {
    target.close();
    notify(error.response?.data?.message || error.message || '获取导航失败');
  }
}
