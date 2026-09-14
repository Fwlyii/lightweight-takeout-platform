export const pushWithViewTransition = (router, location) => {
  if (typeof document === 'undefined' || typeof document.startViewTransition !== 'function') {
    return router.push(location);
  }

  try {
    const transition = document.startViewTransition(() => router.push(location));
    return transition.finished.catch(() => undefined);
  } catch (_) {
    return router.push(location);
  }
};
