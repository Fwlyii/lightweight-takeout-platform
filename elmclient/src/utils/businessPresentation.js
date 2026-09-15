const tagTone = label => {
  if (!label) return 'neutral';
  if (label.includes('满') || label.includes('购买') || label.includes('爱不释手')) return 'orange';
  if (label.includes('好评')) return 'gold';
  if (label.includes('新店') || label.includes('堂食')) return 'green';
  if (label.includes('买过') || label.includes('配送')) return 'blue';
  return 'neutral';
};

/** 前端不再推断标签资格，只展示后端规则引擎的结果。 */
export const getBusinessTags = business =>
  (Array.isArray(business?.recommendationTags) ? business.recommendationTags : [])
    .map(label => ({ label, tone: tagTone(label) }));

export const getRecommendationScore = business => Number(business?.recommendationScore) || 0;

export const hasConfiguredPromotion = business => {
  const threshold = Number(business?.promotionThreshold);
  const discount = Number(business?.promotionDiscount);
  return Number.isFinite(threshold) && Number.isFinite(discount)
    && threshold >= 1 && discount > 0 && discount < threshold;
};

export const supportsDineIn = business => [true, 1, '1', 'true'].includes(business?.dineInAvailable);

const nonNegative = value => value === null || value === undefined || value === ''
  || !Number.isFinite(Number(value)) || Number(value) < 0 ? null : Number(value);
export const getBusinessDistanceKm = business => nonNegative(business?.distanceKm ?? business?.distance);
export const getBusinessDeliveryMinutes = business => nonNegative(business?.deliveryMinutes);
export const compareBusinessDistance = (a, b) =>
  (getBusinessDistanceKm(a) ?? Infinity) - (getBusinessDistanceKm(b) ?? Infinity) || 0;

export const getBusinessAveragePrice = business =>
  Number(business?.averagePrice || business?.startPrice || 0);

export const isBusinessOpen = business => business?.operatingStatus !== false;

/** 后端规则引擎给出的展示理由，用于首页“猜你想吃”卡片角标。 */
const reasonTone = label => {
  if (!label) return 'neutral';
  if (label.includes('人气') || label.includes('深夜')) return 'hot';
  if (label.includes('早餐')) return 'morning';
  if (label.includes('新店')) return 'fresh';
  return 'good';
};

export const getGuessBadge = business => {
  const label = business?.recommendationReason;
  return label ? { label, tone: reasonTone(label) } : null;
};
