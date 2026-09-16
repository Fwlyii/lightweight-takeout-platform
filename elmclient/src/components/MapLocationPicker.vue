<template>
  <section class="map-picker" aria-label="高德地图选址">
    <div class="map-search"><input v-model.trim="keyword" aria-label="搜索地点" maxlength="100" placeholder="输入学校、商场或完整地址" @keydown.enter.prevent="search" /><button type="button" :disabled="searching || keyword.length < 2" @click="search">搜索</button></div>
    <div class="map-tools"><button type="button" :disabled="locating" @click="locate">{{ locating ? '正在定位…' : '使用我的位置' }}</button><button v-if="mapError" type="button" @click="init">重试地图</button></div>
    <p v-if="error" role="alert" class="map-error">{{ error }}</p>
    <p v-if="mapError" role="status" class="map-error">{{ mapError }}</p>
    <p v-if="searching" role="status">搜索中…</p>
    <ul v-if="results.length" class="map-results"><li v-for="(item,index) in results" :key="item.poiId || index"><button type="button" @click="choose(item)"><b>{{ item.name }}</b><small>{{ item.formattedAddress }}</small></button></li></ul>
    <div ref="canvas" class="map-canvas" aria-label="点击地图选择位置"></div>
    <p class="map-selection">{{ resolving ? '正在读取地点…' : selected ? selected.formattedAddress : '搜索地点或点击地图，确认准确位置' }}</p>
    <small v-if="selected?.accuracy">设备定位精度约 {{ Math.round(selected.accuracy) }} 米，请核对标记位置</small>
    <button class="map-confirm" type="button" :disabled="!selected || resolving || locating" @click="$emit('select', { ...selected })">确认此位置</button>
  </section>
</template>
<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { loadMapSdk, mapSearch, mapReverse, locateDevice, poiLocation } from '../services/mapService';
import { validCoordinates } from '../utils/businessDistance';
const props = defineProps({ initial: { type: Object, default: null } });
defineEmits(['select']);
const canvas = ref(null), keyword = ref(''), results = ref([]), error = ref(''), mapError = ref('');
const searching = ref(false), locating = ref(false), resolving = ref(false), selected = ref(validCoordinates(props.initial) ? { ...props.initial } : null);
let map, marker, alive = true, revision = 0, searchRevision = 0;
function moveMarker() {
  if (!map || !selected.value) return;
  const position = [Number(selected.value.longitude), Number(selected.value.latitude)];
  if (!marker) marker = new window.AMap.Marker({ map, position }); else marker.setPosition(position);
  map.setZoomAndCenter(16, position);
}
async function init() {
  mapError.value = '';
  try {
    const AMap = await loadMapSdk(); if (!alive || map) return;
    // Default is a viewport, never a saved/user location.
    map = new AMap.Map(canvas.value, { zoom: 12, center: [117.30586, 38.99688], resizeEnable: true });
    map.on('click', event => resolvePoint({ longitude: Number(event.lnglat.getLng().toFixed(6)), latitude: Number(event.lnglat.getLat().toFixed(6)), source: 'map' }));
    moveMarker();
  } catch (e) { if (alive) mapError.value = e.message; }
}
function choose(item) { revision++; resolving.value = false; selected.value = { ...item }; results.value = []; error.value = ''; moveMarker(); }
async function search() {
  if (keyword.value.length < 2) return;
  const current = ++searchRevision; searching.value = true; error.value = '';
  try { const data = await mapSearch(keyword.value); if (!alive || current !== searchRevision) return;
    results.value = (data.pois || []).map(poiLocation).filter(Boolean);
    if (!results.value.length) error.value = '没有找到地点，请加上城市或更具体的名称';
  } catch(e) { if (alive) error.value = e.response?.data?.message || '搜索失败，请重试'; }
  finally { if(alive && current === searchRevision) searching.value = false; }
}
async function resolvePoint(point) {
  const current = ++revision; resolving.value = true; error.value = '';
  try { const data = await mapReverse(`${point.longitude},${point.latitude}`); if (!alive || current !== revision) return;
    const address = data.regeocode?.formatted_address;
    if (typeof address !== 'string' || !address) throw new Error('此位置无可用地址，请选择附近地点');
    selected.value = { ...point, formattedAddress: address, name: address, poiId: null, adcode: data.regeocode?.addressComponent?.adcode || null };
    moveMarker();
  } catch(e) { if(alive && current === revision) { selected.value = null; error.value = e.response?.data?.message || e.message || '读取地点失败'; } }
  finally { if(alive && current === revision) resolving.value = false; }
}
async function locate() {
  locating.value = true; error.value = '';
  try { const point = await locateDevice(); if(alive) await resolvePoint(point); }
  catch(e) { if(alive) error.value = e.response?.data?.message || e.message; }
  finally { if(alive) locating.value = false; }
}
onMounted(init);
onBeforeUnmount(() => { alive = false; revision++; searchRevision++; map?.destroy(); });
</script>
<style scoped>
.map-picker{color:var(--fwl-ink, #29445d);font-size:14px;text-align:left}.map-search{display:flex;gap:8px}.map-search input{min-width:0;flex:1;border:1px solid var(--fwl-border, #cbdde9);border-radius:8px;padding:10px}.map-picker button{cursor:pointer;border:1px solid var(--fwl-border, #cbdde9);border-radius:8px;background:var(--fwl-surface, #f3f9fd);color:var(--fwl-brand-strong, #167eb8);padding:9px 12px;font:inherit}.map-picker button:disabled{opacity:.5;cursor:not-allowed}.map-tools{display:flex;gap:8px;margin:10px 0}.map-canvas{height:250px;border-radius:10px;background:var(--fwl-surface, #edf4f8)}.map-results{list-style:none;padding:0;margin:8px 0;max-height:190px;overflow:auto}.map-results button{display:block;width:100%;text-align:left;margin-bottom:5px;background:white}.map-results small{display:block;color:var(--fwl-muted, #657d90);margin-top:4px}.map-error{color:#ad4d28;margin:8px 0}.map-selection{line-height:1.6;margin:10px 0}.map-confirm{width:100%;margin-top:10px;background:var(--fwl-brand, #168bd1)!important;color:white!important}
</style>
