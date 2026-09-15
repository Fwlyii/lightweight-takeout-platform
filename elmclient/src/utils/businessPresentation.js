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

/**
 * 商家展示用的距离（km）与预计送达时长（分钟）。
 * 后端一旦下发 distanceKm / deliveryMinutes 就直接使用；否则按商家 id 稳定推导，
 * 保证首页与商家列表对同一家店展示的数值完全一致（口径集中在这里，页面不再各写一份）。
 */
export const getBusinessDistanceKm = (business, fallbackIndex = 0) =>
  Number(business?.distanceKm || business?.distance
    || (0.6 + ((Number(business?.id || fallbackIndex) % 4) * 0.2)));

export const getBusinessDeliveryMinutes = (business, fallbackIndex = 0) =>
  Number(business?.deliveryMinutes || (15 + ((Number(business?.id || fallbackIndex) % 4) * 3)));

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
