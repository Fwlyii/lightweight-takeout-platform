import { ORDER_STATUS } from './orderPresentation.js';
import { positiveId } from './checkout.js';

export async function loadPaymentReceipt(request, id) {
  const orderId = positiveId(id);
  if (!orderId) throw new Error('缺少有效订单编号，请从订单列表重新进入');
  const response = await request.get('/api/orders/detail', { params: { orderId } });
  if (!response?.success || !response.data) throw new Error(response?.message || '订单信息加载失败');
  const order = response.data;
  const state = order.orderState;
  if (!Object.values(ORDER_STATUS).includes(state)) throw new Error('订单状态异常，请查看订单详情');
  return { order, state: state === ORDER_STATUS.WAITING_PAYMENT ? 'unpaid'
    : state === ORDER_STATUS.CANCELLED ? 'cancelled' : 'paid' };
}
