/**
 * 统一管理主题偏好。
 * 主题只负责视觉层，不改变业务数据；本地值用于首屏快速渲染，
 * 登录后再由 /api/v1/preferences/me 提供跨设备恢复。
 */
export const THEME_STORAGE_KEY = 'appTheme';

export const THEME_OPTIONS = [
  { value: 'light', label: '清爽蓝白', description: '默认的外卖平台配色' },
  { value: 'dark', label: '深海夜色', description: '夜间使用更柔和' },
  { value: 'mint', label: '薄荷青', description: '清新、低饱和的绿色调' },
  { value: 'warm', label: '暖阳橙', description: '温暖明亮的橙色调' }
];

const THEME_VALUES = new Set(THEME_OPTIONS.map(option => option.value));
export const normalizeTheme = (theme) => THEME_VALUES.has(theme) ? theme : 'light';

export const applyTheme = (theme) => {
  const normalized = normalizeTheme(theme);
  if (typeof document !== 'undefined') {
    document.documentElement.dataset.theme = normalized;
  }
  if (typeof localStorage !== 'undefined') {
    try {
      localStorage.setItem(THEME_STORAGE_KEY, normalized);
    } catch (_) {
      // 禁用本地存储时，仍允许切换主题并由服务端保存偏好。
    }
  }
  return normalized;
};

export const getStoredTheme = () => {
  if (typeof localStorage === 'undefined') return 'light';
  try {
    return normalizeTheme(localStorage.getItem(THEME_STORAGE_KEY));
  } catch (_) {
    return 'light';
  }
};
