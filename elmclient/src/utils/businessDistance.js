// Only persisted / explicitly selected GCJ-02 coordinates are distance inputs.
export function tianjinOrigin(location) { return validCoordinates(location) ? location : null; }
export function businessLocation(business) { return validCoordinates(business) ? business : null; }
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
  return `直线${formatted}`;
}
