import { computed, ref } from 'vue';
import { areaList } from '@vant/area-data';
import { validCoordinates } from '../utils/businessDistance.js';

const fields = ['province', 'city', 'district'];
const emptyLocation = () => ({ province: '', city: '', district: '' });
const entries = list => Object.entries(list).map(([adcode, name]) => ({ adcode, name }));
const provinces = entries(areaList.province_list);
const cities = entries(areaList.city_list);
const districts = entries(areaList.county_list);
const children = (items, code, length) => items.filter(item => item.adcode.startsWith(code.slice(0, length)));
function browserStorage() {
    try { return globalThis.localStorage; } catch { return undefined; }
}
function getDisplayText(value) {
    const { province, city, district } = value || emptyLocation();
    const genericCity = /^(省直辖县|省直辖县级行政区划|自治区直辖县级行政区划|市辖区|县)$/;
    return [province, city === province || genericCity.test(city) ? '' : city, district].filter(Boolean).join('');
}
function validLocation(value) {
    if (!value || !fields.every(field => typeof value[field] === 'string')) return false;
    const province = provinces.find(item => item.name === value.province);
    const city = province && children(cities, province.adcode, 2).find(item => item.name === value.city);
    return !!city && children(districts, city.adcode, 4).some(item => item.name === value.district);
}

// 行政区划随应用打包，不依赖地图 Key、网络或定位权限。
export function useLocationPicker({ storage = browserStorage() } = {}) {
    const showPicker = ref(false);
    const loading = ref(false);
    const error = ref('');
    const locationData = ref([]);
    const currentLevel = ref(0);
    const locationLevels = ['省份', '城市', '区 / 县'];
    const selectedLocation = ref(emptyLocation());
    const pendingLocation = ref(emptyLocation());
    const displayLocation = computed(() => selectedLocation.value.name || selectedLocation.value.formattedAddress || getDisplayText(selectedLocation.value) || '选择送达位置');
    function acceptMapLocation(point) {
        if (!validCoordinates(point)) return;
        selectedLocation.value = { ...point };
        try {
            storage?.setItem('userLocation', JSON.stringify(point));
            storage?.setItem('userLocationSource', point.source || 'map');
            storage?.setItem('userLocationVersion', '3');
        } catch { /* The current choice still works without storage. */ }
        hideLocationPicker();
    }
    function showLocationPicker() {
        showPicker.value = true;
        error.value = '';
        currentLevel.value = 0;
        pendingLocation.value = { ...selectedLocation.value };
        locationData.value = provinces;
    }
    function hideLocationPicker() {
        showPicker.value = false;
        error.value = '';
        locationData.value = [];
        pendingLocation.value = { ...selectedLocation.value };
    }
    function selectLocation(item) {
        if (!showPicker.value || !item) return;
        const option = locationData.value.find(value => value.adcode === item.adcode && value.name === item.name);
        if (!option) return;
        error.value = '';
        if (currentLevel.value === 0) {
            pendingLocation.value = { province: option.name, city: '', district: '' };
            locationData.value = children(cities, option.adcode, 2);
            currentLevel.value = 1;
        } else if (currentLevel.value === 1) {
            pendingLocation.value = { ...pendingLocation.value, city: option.name, district: '' };
            locationData.value = children(districts, option.adcode, 4);
            currentLevel.value = 2;
        } else {
            pendingLocation.value.district = option.name;
        }
    }
    function switchLevel(level) {
        if (!showPicker.value || !Number.isInteger(level) || level < 0 || level >= currentLevel.value) return;
        error.value = '';
        if (level === 0) {
            pendingLocation.value = emptyLocation();
            locationData.value = provinces;
        } else {
            const province = provinces.find(item => item.name === pendingLocation.value.province);
            if (!province) return;
            pendingLocation.value = { province: province.name, city: '', district: '' };
            locationData.value = children(cities, province.adcode, 2);
        }
        currentLevel.value = level;
    }
    function isSelected(item) {
        return pendingLocation.value[fields[currentLevel.value]] === item?.name;
    }
    function confirmLocation() {
        if (!showPicker.value) return false;
        const labels = ['省份', '城市', '区域'];
        for (let index = 0; index < fields.length; index += 1) {
            if (!pendingLocation.value[fields[index]]) {
                error.value = `请先选择${labels[index]}`;
                return false;
            }
        }
        if (!validLocation(pendingLocation.value)) {
            error.value = '请选择有效的省市区';
            return false;
        }
        selectedLocation.value = { ...pendingLocation.value };
        try {
            storage?.setItem('userLocation', JSON.stringify(selectedLocation.value));
            storage?.setItem('userLocationDisplay', getDisplayText(selectedLocation.value));
            storage?.setItem('userLocationSource', 'manual');
            storage?.setItem('userLocationVersion', '2');
        } catch {
            // 禁用存储时，本次选择仍可使用。
        }
        hideLocationPicker();
        return true;
    }
    function restoreSavedLocation() {
        try {
            if (storage?.getItem('userLocationVersion') === '3') {
                const saved = JSON.parse(storage.getItem('userLocation'));
                selectedLocation.value = validCoordinates(saved) ? saved : emptyLocation();
                return;
            }
            if (storage?.getItem('userLocationSource') !== 'manual'
                || storage?.getItem('userLocationVersion') !== '2') return;
            const saved = JSON.parse(storage.getItem('userLocation'));
            if (!validLocation(saved)) return;
            selectedLocation.value = Object.fromEntries(fields.map(field => [field, saved[field]]));
            pendingLocation.value = { ...selectedLocation.value };
        } catch {
            // 无效或不可访问的记录不应阻止首页加载。
        }
    }
    return {
        displayLocation, showPicker, loading, error, locationData, currentLevel, locationLevels,
        selectedLocation, pendingLocation, showLocationPicker, hideLocationPicker, switchLevel, selectLocation,
        isSelected, confirmLocation, getDisplayText, restoreSavedLocation, acceptMapLocation
    };
}
