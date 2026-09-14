import { reactive } from 'vue';

export const routeMotion = reactive({
  active: false,
  settling: false,
  x: 0,
  y: 0
});

export const startRouteWipe = (element) => {
  const rect = element?.getBoundingClientRect?.();
  routeMotion.x = rect ? rect.left + rect.width / 2 : window.innerWidth / 2;
  routeMotion.y = rect ? rect.top + rect.height / 2 : window.innerHeight / 2;
  routeMotion.settling = false;
  routeMotion.active = true;
};

export const settleRouteWipe = () => {
  routeMotion.settling = true;
  window.setTimeout(() => {
    routeMotion.active = false;
    routeMotion.settling = false;
  }, 300);
};
