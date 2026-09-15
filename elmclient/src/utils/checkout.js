export const positiveId = value => typeof value !== 'object' && /^[1-9][0-9]*$/.test(String(value))
  && Number.isSafeInteger(Number(value)) ? Number(value) : null;

// Preserve only checkout fields when moving through address editors, never arbitrary redirects.
export function checkoutContext(query = {}) {
  if (query.businessId === undefined) return null;
  const businessId = positiveId(query.businessId);
  if (!businessId) throw new Error('商家编号无效，请返回购物车重新结算');
  const serviceMode = query.serviceMode || 'delivery';
  if (!['delivery', 'pickup'].includes(serviceMode)) throw new Error('请选择外送或到店自取');
  let foodIds = null;
  if (query.foodIds !== undefined) {
    if (typeof query.foodIds !== 'string' || query.foodIds.length > 2000) throw new Error('商品选择无效');
    foodIds = [...new Set(query.foodIds.split(',').map(positiveId))];
    if (!foodIds.length || foodIds.includes(null) || foodIds.length > 100) throw new Error('商品选择无效');
  }
  return { businessId, serviceMode, foodIds };
}

export function checkoutQuery(context) {
  if (!context) return {};
  return { businessId: String(context.businessId), serviceMode: context.serviceMode,
    ...(context.foodIds ? { foodIds: context.foodIds.join(',') } : {}) };
}

export function addressReturnLocation(query) {
  try { return { path: '/userAddress', query: checkoutQuery(checkoutContext(query)) }; }
  catch { return { path: '/userAddress' }; }
}

export function checkoutItems(items, context) {
  const selected = context.foodIds && new Set(context.foodIds);
  const matching = items.filter(item => Number(item.businessId) === context.businessId
    && (!selected || selected.has(Number(item.foodId))));
  if (!matching.length) throw new Error('待结算商品已不在购物车，请返回重新选择');
  if (selected && new Set(matching.map(item => Number(item.foodId))).size !== selected.size) {
    throw new Error('部分待结算商品已变化，请返回购物车重新选择');
  }
  return matching;
}

export function createOrderSubmitter(request, makeKey = () => crypto.randomUUID()) {
  const requestId = makeKey();
  let pending = null;
  let orderId = null;
  return (context, addressId) => {
    if (orderId) return Promise.resolve(orderId);
    if (pending) return pending;
    const deliveryAddress = context.serviceMode === 'delivery' ? positiveId(addressId) : null;
    if (context.serviceMode === 'delivery' && !deliveryAddress) return Promise.reject(new Error('请选择收货地址'));
    pending = request.post('/api/orders/submit', null, {
      headers: { 'Idempotency-Key': requestId },
      params: { businessId: context.businessId, serviceMode: context.serviceMode,
        ...(deliveryAddress ? { addressId: deliveryAddress } : {}),
        ...(context.foodIds ? { foodIds: context.foodIds.join(',') } : {}) }
    }).then(response => {
      if (!response?.success || !positiveId(response.data)) throw new Error(response?.message || '下单失败，请重试');
      orderId = Number(response.data);
      return orderId;
    }).finally(() => { pending = null; });
    return pending;
  };
}
