<template>
  <div class="container search-page">
    <!-- 顶部蓝色部分 -->
    <PageHeader title="搜索商家" />

    <!-- 输入框和按钮 -->
    <div class="search-box-container">
    <div class="search-box">
      <input v-model="searchQuery" type="text" placeholder="请输入商家名称" @keyup.enter="performSearch" />
      <button @click="performSearch">搜索</button>
    </div>
  </div>

    <!-- 搜索历史 -->
    <div v-if="searchHistory.length > 0" class="history">
      <h2>搜索历史</h2>
      <ul>
        <li v-for="( history, index) in searchHistory" :key="index" @click="handleHistoryClick(history)" class="history-item">
          {{ history }}
        </li>
      </ul>
    </div>

    <!-- 搜索结果 -->
    <p v-if="searching" class="search-state" role="status">正在查找商家…</p>
    <p v-else-if="searchError" class="search-state" role="alert">{{ searchError }}</p>
    <p v-else-if="searched && !searchResults.length" class="search-state">没有找到相关商家，换个关键词试试。</p>
    <div v-if="searchResults.length > 0" class="results">
      <h2>搜索结果</h2>
      <ul>
        <li v-for="item in searchResults" :key="item.businessId" @click="toBusinessInfo(item.businessId)">
          <div class="business-item">
            <div class="business-img">
              <img :src="item.businessImg || require('@/assets/business-default.png')" @error="handleImageError" alt="商家图片">
            </div>
            <div class="business-info">
              <h3>{{ item.businessName }}</h3>
              <p>&#165;{{ item.startPrice ?? item.starPrice ?? 0 }}起送 | &#165;{{ item.deliveryPrice ?? 0 }}配送</p>
              <p>{{ item.businessExplain }}</p>
            </div>
          </div>
        </li>

      </ul>
    </div>
  </div>
</template>

<script>
import PageHeader from '../components/PageHeader.vue';
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { searchBusinesses } from '@/services/businessService';
import { toast } from '@/utils/toast';

const HISTORY_KEY = 'businessSearchHistory';
const HISTORY_LIMIT = 6;

export default {
  components: { PageHeader },
  setup() {
    const searchQuery = ref('');
    const searchHistory = ref([]);
    const searchResults = ref([]);
    const searching = ref(false), searched = ref(false), searchError = ref('');
    let searchVersion = 0;
    const router = useRouter();

    onMounted(() => {
      try {
        const stored = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]');
        searchHistory.value = Array.isArray(stored) ? stored.slice(0, HISTORY_LIMIT) : [];
      } catch (_) {
        searchHistory.value = [];
      }
    });

    const rememberSearch = (keyword) => {
      searchHistory.value = [keyword, ...searchHistory.value.filter(item => item !== keyword)]
        .slice(0, HISTORY_LIMIT);
      try { localStorage.setItem(HISTORY_KEY, JSON.stringify(searchHistory.value)); } catch (_) { /* 搜索不依赖本地存储。 */ }
    };

    const toBusinessInfo = (businessId) => {
      router.push({ path: '/businessInfo', query: { businessId } });
    };

    const performSearch = async () => {
      const keyword = searchQuery.value.trim();
      if (!keyword) {
        toast.info('请输入商家名称');
        return;
      }
      const version = ++searchVersion;
      searching.value = true; searchError.value = '';
      try {
        const results = await searchBusinesses(keyword);
        if (version !== searchVersion) return;
        searchResults.value = results;
        rememberSearch(keyword);
      } catch (error) {
        if (version !== searchVersion) return;
        console.error('搜索失败:', error);
        searchResults.value = [];
        searchError.value = error?.message || '搜索失败，请稍后重试';
      } finally {
        if (version === searchVersion) { searching.value = false; searched.value = true; }
      }
    };

    const handleHistoryClick = (history) => {
      searchQuery.value = history;
      performSearch();
    };
    const handleImageError = (event) => {
      const image = event?.target;
      if (!image || image.dataset.fallbackApplied === 'true') return;
      image.dataset.fallbackApplied = 'true';
      image.src = require('@/assets/business-default.png');
    };
    return {
      searchQuery,
      searchHistory,
      searchResults,
      searching, searched, searchError,
      performSearch,
      toBusinessInfo,
      handleHistoryClick,
      handleImageError
    };
  }
};
</script>

<style scoped>
/* 容器样式 */
.container {
  max-width: 600px;
  margin: 0 auto;

}

/* 顶部蓝色部分 */
.header {
  background-color: var(--skin-brand, #0097FF);
  padding: 10px;
  color: white;
  text-align: center;
  margin-bottom: 20px;
}

/* 搜索框容器样式 */
.search-box-container {
  display: flex;
  justify-content: center; /* 水平居中 */
  margin-bottom: 20px;
}

/* 搜索框样式 */
.search-box {
  display: flex;
}

.search-box input {
  flex: 1;
  padding: 10px;
  margin-right: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.search-box button {

  padding: 10px;
  background-color: var(--skin-brand, #0097FF);
  color: white;
  border: none;
  border-radius: 0 4px 4px 0;
  cursor: pointer;
}

.search-box button:hover {
  background-color: var(--skin-brand, #0097FF);
}

/* 搜索历史 */
.history {
  margin: 20px 0;
}

.history h2 {
  font-size: 18px;
  margin-bottom: 10px;
}

.history ul {
  list-style-type: none;
  padding: 0;
}

.history li {
  background-color: #f1f1f1;
  padding: 10px;
  margin-bottom: 5px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.history li:hover {
  background-color: #e0e0e0;
}

/* 搜索结果 */
.results {
  margin: 20px 0;

}

.results h2 {
  font-size: 18px;
  margin-bottom: 10px;
}

.results ul {
  list-style-type: none;
  padding: 0;
}

.results li {
  background-color: var(--skin-surface, #e7f3ff);
  padding: 10px;
  margin-bottom: 5px;
  border-radius: 4px;
}

.business-item {
  display: flex;
  align-items: center;
  /* 垂直居中对齐 */
  background-color: var(--skin-surface, #e7f3ff);
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 5px;
}

.business-img {
  margin-right: 10px;
  /* 图片和文字之间的间距 */
}

.business-img img {
  width: 60px;
  /* 图片宽度 */
  height: 60px;
  /* 图片高度 */
  border-radius: 4px;
}

.business-info h3 {
  font-size: 16px;
  margin: 0 0 5px 0;
}

.business-info p {
  font-size: 14px;
  color: #666;
  margin: 0;
}
</style>
