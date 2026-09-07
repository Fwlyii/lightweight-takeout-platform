import request from '../utils/request';

const unwrap = response => {
  if (!response?.success) throw new Error(response?.message || '收藏操作失败');
  return response.data;
};
export const listMyCollections = async () => unwrap(await request.get('/api/merchant/interaction/collections/me'));
export const getMyInteraction = async merchantId => unwrap(await request.get('/api/merchant/interaction/status/me', { params: { merchantId } }));
export const updateMyInteraction = async interaction => unwrap(await request.post('/api/merchant/interaction/update', interaction));
