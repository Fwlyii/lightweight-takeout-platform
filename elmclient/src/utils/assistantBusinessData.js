import { ref } from 'vue';
import { useLocationPicker } from '../composables/useLocationPicker.js';
import { withTianjinDelivery } from './businessDistance.js';

// Recommendation payloads identify the shop but omit its address. Join the same
// public catalog used by the home page instead of inventing a per-food distance.
export function createAssistantBusinessLookup(request, options) {
  const shops = ref(new Map());
  const { selectedLocation, restoreSavedLocation } = useLocationPicker(options);
  let pending = null, disposed = false;
  function load() {
    restoreSavedLocation();
    if (disposed) return Promise.resolve();
    if (pending) return pending;
    pending = Promise.resolve().then(async () => {
      try {
        const response = await request.get('/api/businesses/search', { params: { keyword: '', isScore: 0, isSales: 0 } });
        if (disposed) return;
        const items = response?.success && Array.isArray(response.data) ? response.data : Array.isArray(response) ? response : null;
        if (!items) return;
        shops.value = new Map(items.filter(item => item && Number(item.id) > 0).map(item => [String(item.id), item]));
      } catch { /* Recommendations stay usable if optional shop metadata fails. */ }
    }).finally(() => { pending = null; });
    return pending;
  }
  function enrich(food) {
    const shop = shops.value.get(String(food.businessId));
    return {
      ...withTianjinDelivery({ ...food,
        businessAddress: shop?.businessAddress ?? food.businessAddress,
        latitude: shop?.latitude ?? food.latitude, longitude: shop?.longitude ?? food.longitude
      }, selectedLocation.value),
      businessScore: shop?.score ?? food.businessScore,
      businessSalesCount: shop?.salesCount ?? food.businessSalesCount
    };
  }
  return { load, enrich, dispose: () => { disposed = true; } };
}
