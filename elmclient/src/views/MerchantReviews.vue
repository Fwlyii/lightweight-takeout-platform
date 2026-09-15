<template>
  <div class="reviews-page"><PageHeader title="顾客评价" /><main>
    <label v-if="shops.length" class="shop-select">店铺 <select :value="businessId" @change="selectShop(Number($event.target.value))"><option v-for="shop in shops" :key="shop.merchantId" :value="shop.merchantId">{{ shop.merchantName || `店铺 ${shop.merchantId}` }}</option></select></label>
    <div v-if="loading" class="empty">加载中...</div><div v-else-if="error" class="empty" role="alert">{{ error }} <button @click="load">重试</button></div><div v-else-if="!reviews.length" class="empty">暂时还没有顾客评价</div>
    <article v-for="review in reviews" :key="review.id" class="review-card"><div class="review-head"><strong>{{ review.customerName || '匿名顾客' }}</strong><span class="stars">{{ '★'.repeat(review.rating) }}{{ '☆'.repeat(5-review.rating) }}</span><time>{{ formatDate(review.createTime) }}</time></div><p>{{ review.content || '用户未填写文字评价' }}</p><div v-if="review.merchantReply" class="reply">已回复：{{ review.merchantReply }}</div><div v-else class="reply-form"><input v-model="replyDraft[review.id]" maxlength="500" placeholder="回复这条评价"><button :disabled="replying[review.id]" @click="reply(review)">回复</button></div></article>
  </main></div>
</template>
<script setup>
import PageHeader from '../components/PageHeader.vue';
import { ref, onMounted, onUnmounted } from 'vue'; import request from '../utils/request'; import { toast } from '../utils/toast';
const reviews=ref([]),loading=ref(true),replyDraft=ref({}),businessId=ref(null),shops=ref([]),error=ref(''),replying=ref({});
let requestVersion=0, disposed=false;
const selectShop=async(id)=>{
  if(!shops.value.some(shop=>shop.merchantId===id))return;
  businessId.value=id; reviews.value=[]; error.value=''; loading.value=true;
  const version=++requestVersion;
  try {
    const res=await request.get(`/api/v1/reviews/business/${id}`);
    if(disposed || version!==requestVersion)return;
    if(!res.success)throw Error(res.message || '评价加载失败');
    reviews.value=res.data || [];
  } catch(e) { if(!disposed && version===requestVersion)error.value=e.message || '评价加载失败'; }
  finally { if(!disposed && version===requestVersion)loading.value=false; }
};
const load=async()=>{
  loading.value=true;error.value='';
  try {
    const stores=await request.get('/api/businesses/id_list');
    if(disposed)return;
    if(!stores.success)throw Error(stores.message || '店铺加载失败');
    shops.value=(stores.data || []).map(shop=>({...shop,merchantId:Number(shop.merchantId || shop.businessId || shop.id)}));
    const selected=shops.value.find(shop=>shop.merchantId===businessId.value) || shops.value[0];
    if(selected)await selectShop(selected.merchantId);else reviews.value=[];
  } catch(e) {error.value=e.message || '店铺加载失败';reviews.value=[];}
  finally {loading.value=false;}
};
const reply=async(review)=>{
  if(replying.value[review.id])return;
  const text=replyDraft.value[review.id]?.trim();if(!text)return toast.warning('请输入回复内容');
  replying.value[review.id]=true;
  try {const res=await request.put(`/api/v1/reviews/${review.id}/reply`,{reply:text});if(!res.success)throw Error(res.message || '回复失败');review.merchantReply=text;toast.success('回复已提交');}
  catch(e){toast.error(e.response?.data?.message || e.message || '回复失败');}
  finally{replying.value[review.id]=false;}
};
onUnmounted(()=>{disposed=true;requestVersion++;});
const formatDate=(v)=>v?new Date(v).toLocaleDateString('zh-CN'):''; onMounted(load);
</script>
<style scoped>
.shop-select{display:flex;align-items:center;gap:12px;margin-bottom:16px}.shop-select select{min-width:0;flex:1;padding:10px;border:1px solid var(--fwl-border, #deebf4);border-radius:6px;background:#fff;color:inherit}
.reviews-page{min-height:100vh;background:var(--fwl-surface, #f5f8fc);color:var(--fwl-ink, #29445d)}header{position:relative;height:56px;background:var(--fwl-brand, #168bd1);color:#fff;display:flex;align-items:center;gap:12px;padding:0 16px}header button{border:0;background:none;color:#fff;font-size:30px}h1{font-size:18px;margin:0}main{max-width:680px;margin:auto;padding:16px}.empty{text-align:center;color:var(--fwl-muted, #8aa0b2);padding:60px 0}.review-card{background:#fff;border:1px solid var(--fwl-border, #deebf4);border-radius:10px;padding:15px;margin-bottom:12px}.review-head{display:flex;align-items:center;gap:10px}.review-head strong{color:var(--fwl-ink, #385b75)}.review-head time{margin-left:auto;color:var(--fwl-subtle, #9aacb9);font-size:12px}.stars{color:#efb33a;letter-spacing:1px}.review-card p{color:var(--fwl-muted, #526d82);line-height:1.6;margin:12px 0}.reply{background:var(--fwl-surface, #f3f8fc);border-left:3px solid var(--fwl-brand, #168bd1);padding:8px 10px;color:var(--fwl-muted, #557186);font-size:13px}.reply-form{display:flex;gap:8px}.reply-form input{min-width:0;flex:1;border:1px solid var(--fwl-border, #d6e4ef);border-radius:6px;padding:8px}.reply-form button{border:0;background:var(--fwl-brand, #168bd1);color:#fff;border-radius:6px;padding:0 14px}
</style>
