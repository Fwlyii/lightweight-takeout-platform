export const assistantText = value => typeof value === 'string' ? value : '';
export const validAssistantId = value => Number.isSafeInteger(Number(value)) && Number(value) > 0;
export function assistantCandidates(value) {
  if (!Array.isArray(value)) return [];
  const seen = new Set();
  return value.filter(item => {
    if (!item || typeof item !== 'object' || !validAssistantId(item.foodId) || seen.has(String(item.foodId))) return false;
    seen.add(String(item.foodId));
    return true;
  }).map(item => ({ ...item, foodName: assistantText(item.foodName) || '商品',
    businessName: assistantText(item.businessName), reason: assistantText(item.reason), foodImg: assistantText(item.foodImg) }));
}
export const assistantKeywords = value => Array.isArray(value)
  ? value.filter(item => typeof item === 'string' && item.trim()).map(item => item.trim()).slice(0, 10) : [];
export const assistantBudget = value => value === null || value === undefined || value === '' ? null : Number(value);
export const validAssistantBudget = value => value === null || (Number.isFinite(value) && value >= .01 && value <= 9999.99);
