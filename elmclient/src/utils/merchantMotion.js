const observers = new WeakMap();
function measure(element) {
  const active = element.querySelector('.active');
  if (!active) return;
  element.style.setProperty('--merchant-indicator-x', `${active.offsetLeft}px`);
  element.style.setProperty('--merchant-indicator-width', `${active.offsetWidth}px`);
}
export const merchantIndicator = {
  mounted(element) {
    measure(element);
    if (typeof ResizeObserver !== 'undefined') {
      const observer = new ResizeObserver(() => measure(element));
      observer.observe(element);
      observers.set(element, observer);
    }
  },
  updated: measure,
  unmounted(element) { observers.get(element)?.disconnect(); observers.delete(element); }
};
export function pinMerchantCard(element) {
  Object.assign(element.style, { left: `${element.offsetLeft}px`, top: `${element.offsetTop}px`, width: `${element.offsetWidth}px`, height: `${element.offsetHeight}px` });
}
export function releaseMerchantCard(element) {
  for (const property of ['left', 'top', 'width', 'height']) element.style[property] = '';
}
