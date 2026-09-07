<template>
  <ProfilePage title="测试商家">
    <p class="notice">本地功能验收：商家数据和收藏状态均来自测试后端。</p>
    <router-link class="action-link" to="/myInformation">个人信息</router-link>
    <router-link class="action-link" to="/favorites">我的收藏</router-link>
    <p v-if="error" role="alert">{{error}}</p>
    <article v-for="store in stores" :key="store.id" class="panel">
      <h2>{{store.businessName}}</h2>
      <p>{{store.score == null ? '暂无评分' : store.score+'分'}} · 月售{{store.salesCount}}单</p>
      <button type="button" :disabled="busy" @click="collect(store.id)">收藏{{store.businessName}}</button>
    </article>
    <p v-if="message" role="status">{{message}}</p>
  </ProfilePage>
</template>
<script setup>
import {onMounted,ref} from 'vue';
import ProfilePage from '../components/ProfilePage.vue';
import {searchBusinesses} from '../services/businessService';
import {updateMyInteraction} from '../services/merchantInteractionService';
const stores=ref([]),message=ref(''),error=ref(''),busy=ref(false);
onMounted(async()=>{try{stores.value=await searchBusinesses('');}catch(e){error.value=e.message;}});
async function collect(id){busy.value=true;try{await updateMyInteraction({merchantId:id,collected:true});message.value='已收藏';}catch(e){error.value=e.message;}finally{busy.value=false;}}
</script>
