import request from '../utils/request';
import { apiBaseUrl } from '../utils/endpoints';

export const mapSearch = (keywords, city = '') => request.get('/api/maps/search', { params: { keywords, city } });
export const mapReverse = location => request.get('/api/maps/reverse', { params: { location } });
export const mapRoute = (from, to) => request.get('/api/maps/route', { params: { origin: `${from.longitude},${from.latitude}`, destination: `${to.longitude},${to.latitude}` } });
let sdkPromise;
export function loadMapSdk() {
  if (window.AMap?.Map) return Promise.resolve(window.AMap);
  if (sdkPromise) return sdkPromise;
  sdkPromise = request.get('/api/maps/config').then(config => new Promise((resolve, reject) => {
    if (!config.enabled) { reject(new Error('地图服务尚未配置，仍可填写地址')); return; }
    window._AMapSecurityConfig = { serviceHost: `${apiBaseUrl.replace(/\/$/, '')}/api/maps/_AMapService` };
    const script = document.createElement('script');
    const timeout = setTimeout(() => { script.remove(); reject(new Error('地图加载超时，可重试或搜索地点')); }, 12000);
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(config.jsKey)}`;
    script.onload = () => { clearTimeout(timeout); window.AMap?.Map ? resolve(window.AMap) : reject(new Error('地图加载失败')); };
    script.onerror = () => { clearTimeout(timeout); script.remove(); reject(new Error('地图网络连接失败，可搜索地点重试')); };
    document.head.appendChild(script);
  })).catch(error => { sdkPromise = null; throw error; });
  return sdkPromise;
}
export async function locateDevice() {
  if (!navigator.geolocation) throw new Error('当前浏览器不支持定位，请搜索选址');
  const position = await new Promise((resolve, reject) => navigator.geolocation.getCurrentPosition(resolve,
    () => reject(new Error('未取得定位权限或定位超时，请搜索选址')), { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }));
  const raw = `${position.coords.longitude.toFixed(6)},${position.coords.latitude.toFixed(6)}`;
  const converted = await request.get('/api/maps/convert', { params: { location: raw } });
  const [longitude, latitude] = String(converted.locations || '').split(',').map(Number);
  if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) throw new Error('定位坐标转换失败，请重试');
  return { longitude, latitude, accuracy: position.coords.accuracy, source: 'device' };
}
export function poiLocation(poi) {
  if (typeof poi.location !== 'string' || !poi.location.includes(',')) return null;
  const [longitude, latitude] = poi.location.split(',').map(Number);
  if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) return null;
  const text = v => typeof v === 'string' ? v : '';
  return { longitude, latitude, poiId: text(poi.id), adcode: text(poi.adcode) || null,
    name: text(poi.name), formattedAddress: [poi.pname, poi.cityname, poi.adname, poi.address, poi.name].map(text).filter(Boolean).filter((v,i,a)=>a.indexOf(v)===i).join(''), source: 'map' };
}
