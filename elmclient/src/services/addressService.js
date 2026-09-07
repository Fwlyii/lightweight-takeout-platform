import request from '../utils/request';

const unwrap = response => {
  if (!response?.success) throw new Error(response?.message || '地址操作失败');
  return response.data;
};
export const listMyAddresses = async () => unwrap(await request.get('/api/addresses/me'));
export const getMyAddress = async id => unwrap(await request.get('/api/addresses/' + id));
export const createMyAddress = async address => unwrap(await request.post('/api/addresses/me', address));
export const updateMyAddress = async (id, address) => unwrap(await request.put('/api/addresses/' + id, address));
export const removeMyAddress = async id => unwrap(await request.delete('/api/addresses/' + id));
export const setMyDefaultAddress = async id => unwrap(await request.put('/api/addresses/' + id + '/default'));
