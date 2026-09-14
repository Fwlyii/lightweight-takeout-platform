const isAssistant = path => path === '/ai-chat' || path.startsWith('/ai-chat/');
const PORTAL_HOME = Object.freeze({ admin: '/admin/home', merchant: '/merchant/business', rider: '/rider/dashboard' });
const CUSTOMER_PARENT = Object.freeze({
  '/listDetail': '/orderList',
  '/assets': '/myInformation',
  '/notifications': '/myInformation',
  '/preferences': '/myInformation',
  '/addUserAddress': '/userAddress',
  '/editUserAddress': '/userAddress'
});
const NON_RETURNABLE = new Set(['/login', '/register', '/payment', '/successfulPayment']);
const portal = route => route.path.startsWith('/admin') ? 'admin'
  : route.path.startsWith('/merchant') ? 'merchant'
    : route.path.startsWith('/rider') || (['/myInformation', '/notifications'].includes(route.path) && route.query.role === 'rider')
      ? 'rider' : 'user';

function fallbackFor(route) {
  if (route.path === '/merchant/apply') return '/index';
  return PORTAL_HOME[portal(route)] || CUSTOMER_PARENT[route.path] || '/index';
}

function usablePrevious(router, route, previous, excludeAssistant) {
  if (typeof previous !== 'string' || !previous.startsWith('/') || previous.startsWith('//')
    || /[\\\u0000-\u0020]/.test(previous)) return false;
  try {
    const target = router.resolve(previous);
    if (!target.matched.length || target.matched.some(record => record.redirect)
      || target.fullPath === route.fullPath
      || NON_RETURNABLE.has(target.path)
      || (excludeAssistant && isAssistant(target.path))) return false;
    const role = target.meta.role || (target.query.role === 'rider' ? 'rider' : null);
    return !role || role === portal(route);
  } catch (_) {
    return false;
  }
}

export function navigateBack(router, route, { excludeAssistant = false } = {}) {
  const previous = router.options?.history?.state?.back;
  if (usablePrevious(router, route, previous, excludeAssistant)) return router.back();
  return router.replace(fallbackFor(route));
}

export function navigateAssistantBack(router, route) {
  // A second click from the leaving component must not navigate the new page.
  if (!isAssistant(route.path)) return;
  if (route.path !== '/ai-chat') return router.replace('/ai-chat');
  return navigateBack(router, route, { excludeAssistant: true });
}

export function returnToMerchant(router, route) {
  const value = route.query.businessId;
  const id = typeof value === 'string' && /^\d+$/.test(value) ? Number(value) : NaN;
  return router.replace(Number.isSafeInteger(id) && id > 0
    ? { path: '/businessInfo', query: { businessId: String(id) } } : '/index');
}
