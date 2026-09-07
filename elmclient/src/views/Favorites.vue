<template>
  <ProfilePage title="我的收藏">
    <p v-if="loading" role="status">正在加载收藏…</p>
    <p v-if="error" class="notice error" role="alert">{{error}} <button type="button" @click="load">重试</button></p>
    <p v-if="!loading && !error && !businesses.length" class="panel muted">还没有收藏的店铺，去看看喜欢吃什么吧。</p>
    <article v-for="business in businesses" :key="business.id" class="panel">
      <router-link class="store-summary" :to="{path:'/businessInfo',query:{businessId:business.id}}">
        <img :src="business.businessImg || '/images/default-store.svg'" :alt="business.businessName" @error="fallback">
        <div><h2>{{business.businessName}}</h2><p class="muted">{{business.operatingStatus === false ? '休息中' : '营业中'}}</p>
          <p>{{business.score == null ? '暂无评分' : Number(business.score).toFixed(1)+'分'}} · 月售{{business.salesCount || 0}}单</p>
          <p class="muted">起送 ¥{{money(business.startPrice)}} · 配送 ¥{{money(business.deliveryPrice)}}</p>
        </div>
      </router-link>
      <div class="store-tags"><span v-for="tag in business.recommendationTags || []" :key="tag">{{tag}}</span></div>
      <div class="actions"><button type="button" :disabled="busy" @click="remove(business.id)">取消收藏</button></div>
    </article>
  </ProfilePage>
</template>
<script setup>
import {onMounted,ref} from 'vue';
import ProfilePage from '../components/ProfilePage.vue';
import {listMyCollections,updateMyInteraction} from '../services/merchantInteractionService';
const businesses=ref([]),loading=ref(true),busy=ref(false),error=ref('');
const money=value=>(Number(value)||0).toFixed(2);
function fallback(event){if(!event.target.dataset.fallback){event.target.dataset.fallback='true';event.target.src='/images/default-store.svg';}}
async function load(){loading.value=true;error.value='';try{businesses.value=await listMyCollections();}catch(e){error.value=e.response?.data?.message || '收藏加载失败';}finally{loading.value=false;}}
async function remove(id){if(busy.value)return;busy.value=true;error.value='';try{await updateMyInteraction({merchantId:id,collected:false});await load();}catch(e){error.value=e.response?.data?.message || '取消收藏失败';}finally{busy.value=false;}}
onMounted(load);
</script>
<style scoped>
.store-summary{display:flex;gap:12px;text-decoration:none;color:inherit}.store-summary img{width:72px;height:72px;border-radius:8px;object-fit:cover;flex-shrink:0}.store-summary div{min-width:0}.store-summary h2{margin:0;font-size:16px}.store-summary p{font-size:13px}.store-tags{display:flex;flex-wrap:wrap;gap:6px;margin-top:10px}.store-tags span{font-size:12px;background:#edf6ff;color:#0879c6;padding:2px 6px;border-radius:4px}
</style>
