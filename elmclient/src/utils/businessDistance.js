// Offline Tianjin presentation estimates, not GPS fixes or routing-service promises.
// District centers: https://geo.datav.aliyun.com/areas_v3/bound/120000_full.json
// Campus points: OSM way/476603051 (Peiyang), way/48165383 (Weijin).
const point = (latitude, longitude) => Object.freeze({ latitude, longitude });
export const PEIYANG_CAMPUS = point(38.99688, 117.30586);
// The Jinnan showcase is centered around Peiyang campus, not the distant district office.
export const JINNAN_REFERENCE = point(39.001, 117.315);
// Stable showcase storefront points within each original campus/address area.
// Match the complete demo identity, never assign coordinates to an unrelated new shop by ID alone.
const DEMO_SHOP_POINTS = [
  [1, '北洋食堂·现炒', '天津大学北洋园校区', 39.0000, 117.3120],
  [2, '海棠早餐铺', '天津大学卫津路校区', 39.1100, 117.1660],
  [4, '津南麻辣香锅', '天津大学北洋园校区', 38.9970, 117.3060],
  [5, '北洋咖啡实验室', '天津大学卫津路校区', 39.1063, 117.1629],
  [6, '清真兰州牛肉面', '天津大学北洋园校区', 38.9915, 117.3030],
  [7, '轻食研究所', '天津大学津南校区', 39.0050, 117.3080],
  [8, '泰合炸鸡·夜宵', '天津大学北洋园校区', 38.9920, 117.2990],
  [9, '肯德基', '天津大学', 39.0020, 117.3132]
];
export const TIANJIN_DISTRICTS = Object.freeze({
  '和平区': point(39.118327, 117.195907), '河东区': point(39.122125, 117.226568),
  '河西区': point(39.101897, 117.217536), '南开区': point(39.120474, 117.164143),
  '河北区': point(39.156632, 117.201569), '红桥区': point(39.175066, 117.163301),
  '东丽区': point(39.087764, 117.313967), '西青区': point(39.139446, 117.012247),
  '津南区': JINNAN_REFERENCE, '北辰区': point(39.225555, 117.13482),
  '武清区': point(39.376925, 117.057959), '宝坻区': point(39.716965, 117.308094),
  '滨海新区': point(39.032846, 117.654173), '宁河区': point(39.328886, 117.82828),
  '静海区': point(38.935671, 116.925304), '蓟州区': point(40.045342, 117.407449)
});
export function tianjinOrigin(location) {
  if (!location?.province) return JINNAN_REFERENCE;
  if (location.province !== '天津市') return null;
  return TIANJIN_DISTRICTS[location.district] || null;
}
export function businessLocation(business) {
  if (validCoordinates(business)) return business;
  const address = typeof business?.businessAddress === 'string' ? business.businessAddress.trim() : '';
  const demoPoint = DEMO_SHOP_POINTS.find(([id, name, expectedAddress]) => Number(business.id ?? business.businessId) === id
    && business.businessName === name && address === expectedAddress);
  if (demoPoint) return point(demoPoint[3], demoPoint[4]);
  if (/天津大学.*卫津路/.test(address)) return point(39.10912, 117.16467);
  if (/天津大学.*(北洋园|津南校区)/.test(address) || address === '天津大学') return PEIYANG_CAMPUS;
  // Require a Tianjin address; e.g. Shenyang also has a Heping district.
  if (!address.startsWith('天津')) return null;
  const district = Object.keys(TIANJIN_DISTRICTS).find(name => address.includes(name));
  return district ? TIANJIN_DISTRICTS[district] : null;
}
const coordinate = (value, limit) => (typeof value === 'number' || typeof value === 'string')
  && String(value).trim() !== '' && Number.isFinite(Number(value)) && Math.abs(Number(value)) <= limit;
export const validCoordinates = point => !!point && coordinate(point.latitude, 90) && coordinate(point.longitude, 180);
export function distanceKm(from, to) {
  if (!validCoordinates(from) || !validCoordinates(to)) return null;
  const radians = degrees => Number(degrees) * Math.PI / 180;
  const dLat = radians(to.latitude - from.latitude), dLon = radians(to.longitude - from.longitude);
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(radians(from.latitude)) * Math.cos(radians(to.latitude)) * Math.sin(dLon / 2) ** 2;
  return 6371.0088 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, a))));
}
export function estimatedDeliveryMinutes(km) {
  if (typeof km !== 'number' || !Number.isFinite(km) || km < 0) return null;
  // 15 min preparation + 5 min dispatch + 1.3 road factor / 18 km/h riding speed.
  // Round up to 5 minutes; never force remote shops into a fake short-delivery window.
  return Math.max(25, Math.ceil((20 + km * 1.3 / 18 * 60) / 5) * 5);
}
export function withTianjinDelivery(business, location) {
  const km = distanceKm(tianjinOrigin(location), businessLocation(business));
  // Switching province also clears any previously calculated/API distance or ETA.
  return { ...business, distanceKm: km, distance: null, deliveryMinutes: estimatedDeliveryMinutes(km) };
}
export function businessDistanceText(business) {
  const value = business?.distanceKm;
  if (value === null || value === undefined || !Number.isFinite(value) || value < 0) return '距离暂无';
  const formatted = `${value.toFixed(1)}km`;
  return `约${formatted}`;
}
