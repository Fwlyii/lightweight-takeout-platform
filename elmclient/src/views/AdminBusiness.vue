<template>
    <div class="wrapper">
        <AdminPageHeader title="商铺管理" />
        <div class="content">
            <ul class="business-list">
                <li v-for="b in businessList" :key="b.userId" class="business-item">
                    <div class="info">
                        <img :src="b.photo || defaultImg" alt="logo" class="logo" @error="onImgError">
                        <div class="meta">
                            <p class="name">{{ b.username }}集团</p>
                            <p class="addr">联系方式：{{ b.phone || '手机号未填写' }}</p>
                        </div>
                    </div>
                    <div class="actions">
                        <button class="toggle" @click.stop="enterBusiness(b)">查看商铺</button>
                    </div>
                </li>
            </ul>
        </div>
    </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';
import { DEFAULT_AVATAR_URL } from '../utils/profileDefaults';
import AdminPageHeader from '../components/AdminPageHeader.vue';
export default {
    name: 'ManageBusiness',
    components: { AdminPageHeader },
    setup() {
        const businessList = ref([]);
        const defaultImg = DEFAULT_AVATAR_URL;
        const router = useRouter();

        const onImgError = (e) => {
            e.target.src = defaultImg;
        };

        const loadBusinesses = async () => {
            try {
                const response = await request.get('/api/businesses/active');
                businessList.value = response;
            } catch (error) {
                console.error('获取商家列表失败:', error);
            }
        };

        const enterBusiness = (biz) => {
            router.push({
                path: '/admin/shop',
                query: {
                    ownerId: biz.userId,
                    merchantName: biz.username + '集团'
                }
            });
        };

        onMounted(() => {
            loadBusinesses();
        });

        return {
            businessList,
            defaultImg,
            onImgError,
            enterBusiness
        };
    }
};
</script>

<style scoped>
.wrapper { width: 100%; max-width: 960px; margin: 0 auto; min-height: 100%; padding-bottom: calc(84px + env(safe-area-inset-bottom)); background: #f5f8fb; color: #253f54; }
.content { padding: 16px; }
.business-list { display: grid; gap: 12px; }
.business-item { display: flex; align-items: center; gap: 14px; padding: 16px; background: white; border: 1px solid #dfe8ef; border-radius: 12px; }
.info { display: flex; align-items: center; flex: 1; min-width: 0; gap: 14px; }
.logo { width: 64px; height: 64px; flex: 0 0 64px; border-radius: 12px; object-fit: cover; }
.meta { min-width: 0; overflow-wrap: anywhere; }
.name { font-size: 17px; font-weight: 650; }
.addr { margin-top: 8px; font-size: 13px; color: #6d8192; }
.toggle { min-height: 44px; border: 1px solid #c6e0f1; color: #087ecc; background: #eef8ff; border-radius: 8px; padding: 8px 12px; font-size: 14px; cursor: pointer; }
@media (max-width: 420px) { .business-item { padding: 12px; flex-wrap: wrap; } .logo { width: 48px; height: 48px; flex-basis: 48px; } .actions { margin-left: auto; } }
</style>
