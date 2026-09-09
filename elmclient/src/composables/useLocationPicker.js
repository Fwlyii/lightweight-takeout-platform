import { computed, ref } from 'vue';
import axios from 'axios';

const DEFAULT_LOCATION = '天津大学北洋园校区';
const STORAGE_VERSION = '2';
const emptyLocation = () => ({ province: '', city: '', district: '' });

function browserStorage() {
    try { return globalThis.localStorage; } catch { return undefined; }
}

function completeLocation(value) {
    return value && ['province', 'city', 'district'].every(field =>
        typeof value[field] === 'string' && value[field].trim().length > 0);
}

function getDisplayText(value) {
    const { province, city, district } = value || emptyLocation();
    return [province, city === province ? '' : city, district].filter(Boolean).join(' ');
}

// 页面使用真实地图服务；测试注入 HTTP 和存储，不依赖网络或浏览器环境。
export function useLocationPicker({
    http = axios,
    storage = browserStorage(),
    apiKey = process.env.VUE_APP_AMAP_KEY || ''
} = {}) {
    const showPicker = ref(false);
    const loading = ref(false);
    const error = ref('');
    const locationData = ref([]);
    const currentLevel = ref(0);
    const locationLevels = ['请选择省份', '请选择城市', '请选择区域'];
    const selectedLocation = ref(emptyLocation());
    const pendingLocation = ref(emptyLocation());
    const displayLocation = computed(() => getDisplayText(selectedLocation.value) || DEFAULT_LOCATION);
    let requestVersion = 0;

    async function loadDistricts(keyword, level, selection) {
        const version = ++requestVersion;
        error.value = '';
        locationData.value = [];
        if (!apiKey.trim()) {
            loading.value = false;
            error.value = '暂未配置地图服务，当前地点保持不变';
            return;
        }
        loading.value = true;
        try {
            const response = await http.get('https://restapi.amap.com/v3/config/district', {
                params: { key: apiKey, keywords: keyword, subdistrict: 1 },
                timeout: 10000
            });
            if (version !== requestVersion || !showPicker.value) return;
            const children = response?.data?.districts?.[0]?.districts;
            if (response?.data?.status !== '1' || !Array.isArray(children)) {
                error.value = '位置数据暂时不可用，请稍后重试';
                return;
            }
            locationData.value = children.filter(item => item && typeof item.name === 'string' && item.name.trim());
            currentLevel.value = level;
            pendingLocation.value = selection;
        } catch {
            if (version === requestVersion) error.value = '位置数据加载失败，请检查网络后重试';
        } finally {
            if (version === requestVersion) loading.value = false;
        }
    }

    function showLocationPicker() {
        showPicker.value = true;
        return loadDistricts('中国', 0, { ...selectedLocation.value });
    }

    function hideLocationPicker() {
        requestVersion += 1;
        showPicker.value = false;
        loading.value = false;
        locationData.value = [];
        pendingLocation.value = { ...selectedLocation.value };
    }

    function selectLocation(item) {
        if (loading.value || !showPicker.value || !item || typeof item.name !== 'string' || !item.name.trim()) return;
        const name = item.name.trim();
        if (currentLevel.value === 0) {
            return loadDistricts(item.adcode || name, 1, { province: name, city: '', district: '' });
        }
        if (currentLevel.value === 1) {
            return loadDistricts(item.adcode || name, 2, { ...pendingLocation.value, city: name, district: '' });
        }
        pendingLocation.value.district = name;
        error.value = '';
    }

    function switchLevel(level) {
        if (!Number.isInteger(level) || level < 0 || level >= currentLevel.value || !showPicker.value) return;
        if (level === 0) return loadDistricts('中国', 0, emptyLocation());
        return loadDistricts(pendingLocation.value.province, 1, {
            province: pendingLocation.value.province, city: '', district: ''
        });
    }

    function isSelected(item) {
        return pendingLocation.value[['province', 'city', 'district'][currentLevel.value]] === item?.name;
    }

    function confirmLocation() {
        const fields = ['province', 'city', 'district'];
        const labels = ['省份', '城市', '区域'];
        for (let index = 0; index < fields.length; index += 1) {
            if (!pendingLocation.value[fields[index]]) {
                error.value = `请先选择${labels[index]}`;
                return false;
            }
        }
        selectedLocation.value = { ...pendingLocation.value };
        try {
            storage?.setItem('userLocation', JSON.stringify(selectedLocation.value));
            storage?.setItem('userLocationDisplay', getDisplayText(selectedLocation.value));
            storage?.setItem('userLocationSource', 'manual');
            storage?.setItem('userLocationVersion', STORAGE_VERSION);
        } catch {
            // 浏览器禁用存储时，本次选择仍可使用，但不会跨会话保存。
        }
        hideLocationPicker();
        return true;
    }

    function restoreSavedLocation() {
        try {
            if (storage?.getItem('userLocationSource') !== 'manual'
                || storage?.getItem('userLocationVersion') !== STORAGE_VERSION) return;
            const saved = JSON.parse(storage.getItem('userLocation'));
            if (!completeLocation(saved)) return;
            selectedLocation.value = Object.fromEntries(['province', 'city', 'district'].map(field => [field, saved[field].trim()]));
            pendingLocation.value = { ...selectedLocation.value };
        } catch {
            // 无效或不可访问的本地记录不应阻止首页加载。
        }
    }

    return {
        displayLocation, showPicker, loading, error, locationData, currentLevel, locationLevels,
        selectedLocation, showLocationPicker, hideLocationPicker, switchLevel, selectLocation,
        isSelected, confirmLocation, getDisplayText, restoreSavedLocation
    };
}
