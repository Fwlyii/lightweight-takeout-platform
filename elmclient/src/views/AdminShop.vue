<template>
    <div class="wrapper">
        <AdminPageHeader :title="businessName || '商铺管理'" back-to="/admin/business" />
        <div class="content">
            <ul class="store-list">
                <li v-for="s in storeList" :key="s.id" class="store-item">
                    <div class="store-info">
                        <img :src="s.businessImg || defaultImg" class="logo" @error="onImgError" />
                        <div class="meta">
                            <p class="name">{{ s.businessName || '未命名商铺' }}</p>
                            <p class="addr">{{ s.businessAddress || '暂无地址' }}</p>
                            <p class="desc">{{ s.businessExplain || '暂无简介' }}</p>
                            <p class="type">类型: {{shopTypes.find(t => t.id === s.orderTypeId)?.name || '未设置'}}</p>
                        </div>
                    </div>
                    <div class="actions">
                        <button class="edit" @click="startEdit(s)">编辑</button>
                        <button class="del" @click="removeStore(s)">删除</button>
                    </div>
                </li>
            </ul>

            <!-- 底部新增按钮 -->
            <div class="bottom-bar">
                <button class="add" @click="startCreate">新增商铺</button>
            </div>

            <!-- 编辑/新增弹出层 -->
            <div v-if="editor.visible" class="editor">
                <div class="card">
                    <h3>{{ editor.mode === 'create' ? '新增商铺' : '编辑商铺' }}</h3>
                    <div class="form">
                        <label>商铺名称 <span class="limit">(不超过10字)</span></label>
                        <input v-model="editor.form.businessName" placeholder="请输入商铺名称" @input="validateName" />
                        <p class="error" v-if="nameError">{{ nameError }}</p>

                        <label>商铺图片</label>
                        <div class="upload-area">
                            <input type="file" ref="fileInput" @change="handleFileChange" accept="image/*"
                                style="display: none" />
                            <button class="upload-btn" @click="triggerFileInput">选择图片</button>
                            <span class="file-name">{{ uploadFileName || '未选择文件' }}</span>
                            <button class="upload-submit" @click="uploadImage" :disabled="!selectedFile">
                                上传
                            </button>
                        </div>
                        <div class="image-preview" v-if="editor.form.businessImg">
                            <img :src="editor.form.businessImg" alt="预览" />
                        </div>

                        <label>商铺地址 <span class="limit">(不超过15字)</span></label>
                        <input v-model="editor.form.businessAddress" placeholder="请输入地址" @input="validateAddress" />
                        <p class="error" v-if="addressError">{{ addressError }}</p>

                        <label>商铺简介 <span class="limit">(不超过15字)</span></label>
                        <textarea v-model="editor.form.businessExplain" placeholder="请输入商铺简介"
                            @input="validateExplain"></textarea>
                        <p class="error" v-if="explainError">{{ explainError }}</p>

                        <label>商铺类型</label>
                        <select v-model="editor.form.orderTypeId" class="swal2-input" @change="validateType"
                            style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
                            <option value="" disabled>请选择商铺类型</option>
                            <option v-for="type in shopTypes" :key="type.id" :value="type.id">
                                {{ type.name }}
                            </option>
                        </select>
                        <p class="error" v-if="typeError">{{ typeError }}</p>
                    </div>
                    <div class="editor-actions">
                        <button class="cancel" @click="closeEditor">取消</button>
                        <button class="save" @click="saveStore">保存</button>
                    </div>
                </div>
            </div>

            <!-- 删除确认弹窗 -->
            <div v-if="showConfirmModal" class="modal-overlay" @click.self="closeModal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>确认操作</h3>
                        <span class="close-btn" @click="closeModal">&times;</span>
                    </div>
                    <div class="modal-body">
                        <p>确认要删除此商铺吗？</p>
                    </div>
                    <div class="modal-footer">
                        <button class="modal-btn confirm-btn" @click="confirmDelete">确认</button>
                        <button class="modal-btn cancel-btn" @click="closeModal">取消</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import request from '@/utils/request';
import { toast } from '@/utils/toast';
import AdminPageHeader from '../components/AdminPageHeader.vue';

export default {
    name: 'ManageShop',
    components: { AdminPageHeader },
    setup() {
        const route = useRoute();
        const ownerId = ref(null);
        const businessName = ref('');
        const storeList = ref([]);
        const defaultImg = require('@/assets/business-default.png');
        const fileInput = ref(null);
        const selectedFile = ref(null);
        const uploadFileName = ref('');

        // 删除确认弹窗相关
        const showConfirmModal = ref(false);
        const storeDeleteSelectId = ref(null);

        // 商铺类型选项
        const shopTypes = ref([
            { id: 1, name: '美食' },
            { id: 2, name: '早餐' },
            { id: 3, name: '跑腿代购' },
            { id: 4, name: '汉堡披萨' },
            { id: 5, name: '甜品饮品' },
            { id: 6, name: '速食简食' },
            { id: 7, name: '地方小吃' },
            { id: 8, name: '米粉面馆' },
            { id: 9, name: '包子粥铺' },
            { id: 10, name: '炸鸡炸串' }
        ]);

        // 验证错误信息
        const nameError = ref('');
        const addressError = ref('');
        const explainError = ref('');
        const typeError = ref('');

        const editor = reactive({
            visible: false,
            mode: 'edit',
            form: {
                id: null,
                businessName: '',
                businessImg: '',
                businessAddress: '',
                businessExplain: '',
                orderTypeId: null
            }
        });

        const onImgError = (e) => { e.target.src = defaultImg; };

        const loadStores = async () => {
            try {
                const response = await request.get('/api/businesses/merchant', {
                    params: {
                        userId: ownerId.value,
                        status: 1
                    }
                });
                if (response.success) {
                    storeList.value = response.data || [];
                }
            } catch (error) {
                console.error('获取商铺列表失败:', error);
                storeList.value = [];
                toast.error('商铺列表加载失败');
            }
        };

        // 验证商铺名称
        const validateName = () => {
            if (editor.form.businessName.length > 10) {
                nameError.value = '商铺名称不能超过10个字';
            } else {
                nameError.value = '';
            }
        };

        // 验证商铺地址
        const validateAddress = () => {
            if (editor.form.businessAddress.length > 15) {
                addressError.value = '商铺地址不能超过15个字';
            } else {
                addressError.value = '';
            }
        };

        // 验证商铺简介
        const validateExplain = () => {
            if (editor.form.businessExplain.length > 15) {
                explainError.value = '商铺简介不能超过15个字';
            } else {
                explainError.value = '';
            }
        };

        // 验证商铺类型
        const validateType = () => {
            if (!editor.form.orderTypeId) {
                typeError.value = '请选择商铺类型';
            } else {
                typeError.value = '';
            }
        };

        // 验证所有字段
        const validateAll = () => {
            validateName();
            validateAddress();
            validateExplain();
            validateType();
            return !nameError.value && !addressError.value && !explainError.value && !typeError.value;
        };

        const triggerFileInput = () => {
            fileInput.value.click();
        };

        const handleFileChange = (e) => {
            const file = e.target.files[0];
            if (file) {
                selectedFile.value = file;
                uploadFileName.value = file.name;
            }
        };

        const uploadImage = async () => {
            if (!selectedFile.value) return;

            try {
                const formData = new FormData();
                formData.append('file', selectedFile.value);

                const response = await request.post('/upload', formData, {
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                });

                if (response.success) {
                    editor.form.businessImg = response.data;
                    selectedFile.value = null;
                    uploadFileName.value = '上传成功';
                } else {
                    console.error('上传失败:', response.message);
                    uploadFileName.value = '上传失败';
                }
            } catch (error) {
                console.error('上传图片出错:', error);
                uploadFileName.value = '上传出错';
            }
        };

        const startEdit = (store) => {
            editor.mode = 'edit';
            editor.form = { ...store };
            editor.visible = true;
            selectedFile.value = null;
            uploadFileName.value = '';
            nameError.value = '';
            addressError.value = '';
            explainError.value = '';
            typeError.value = '';
        };

        const startCreate = () => {
            editor.mode = 'create';
            editor.form = {
                id: null,
                businessName: '',
                businessImg: '',
                businessAddress: '',
                businessExplain: '',
                orderTypeId: null
            };
            editor.visible = true;
            selectedFile.value = null;
            uploadFileName.value = '';
            nameError.value = '';
            addressError.value = '';
            explainError.value = '';
            typeError.value = '';
        };

        const closeEditor = () => {
            editor.visible = false;
            selectedFile.value = null;
            uploadFileName.value = '';
            nameError.value = '';
            addressError.value = '';
            explainError.value = '';
            typeError.value = '';
        };

        // 删除商铺相关方法
        const removeStore = (store) => {
            storeDeleteSelectId.value = store.id;
            showConfirmModal.value = true;
        };

        const closeModal = () => {
            showConfirmModal.value = false;
            storeDeleteSelectId.value = null;
        };

        const confirmDelete = async () => {
            if (!storeDeleteSelectId.value) return;

            try {
                await request.delete(`/api/businesses/${storeDeleteSelectId.value}`);
                storeList.value = storeList.value.filter(s => s.id !== storeDeleteSelectId.value);
                toast.success('商铺删除成功');
            } catch (error) {
                console.error('删除商铺失败:', error);
                toast.error(error?.response?.data?.message || '商铺删除失败');
            } finally {
                closeModal();
            }
        };

        const saveStore = async () => {
            if (!validateAll()) {
                toast.warning('请检查输入内容是否符合要求');
                return;
            }

            try {
                if (editor.mode === 'create') {
                    const businessData = {
                        businessName: editor.form.businessName,
                        businessAddress: editor.form.businessAddress,
                        businessExplain: editor.form.businessExplain,
                        businessImg: editor.form.businessImg,
                        orderTypeId: editor.form.orderTypeId,
                        userId: ownerId.value,
                    };

                    const response = await request.post('/api/businesses/apply', businessData);

                    if (response.success) {
                        await loadStores();
                        toast.success('商铺申请已提交');
                    } else {
                        throw new Error(response.message || '商铺申请失败');
                    }
                } else {
                    const updateData = {
                        businessName: editor.form.businessName,
                        businessAddress: editor.form.businessAddress,
                        businessExplain: editor.form.businessExplain,
                        businessImg: editor.form.businessImg,
                        orderTypeId: editor.form.orderTypeId
                    };

                    await request.patch(`/api/businesses/own/${editor.form.id}`, updateData);
                    await loadStores();
                    toast.success('商铺更新成功');
                }
                editor.visible = false;
            } catch (error) {
                console.error('保存商铺失败:', error);
                toast.error(error?.response?.data?.message || error.message || '商铺保存失败');
            }
        };

        onMounted(() => {
            ownerId.value = route.query.ownerId;
            businessName.value = route.query.merchantName || '';
            loadStores();
        });

        return {
            businessName,
            storeList,
            defaultImg,
            shopTypes,
            onImgError,
            startEdit,
            startCreate,
            closeEditor,
            saveStore,
            removeStore,
            editor,
            fileInput,
            selectedFile,
            uploadFileName,
            nameError,
            addressError,
            explainError,
            typeError,
            triggerFileInput,
            handleFileChange,
            uploadImage,
            validateName,
            validateAddress,
            validateExplain,
            validateType,
            showConfirmModal,
            closeModal,
            confirmDelete
        };
    }
};
</script>

<style scoped>
.wrapper { width: 100%; max-width: 960px; margin: 0 auto; min-height: 100%; background: var(--skin-surface, #f5f8fb); color: var(--skin-ink, #253f54); }
.content { padding: 16px 16px calc(90px + env(safe-area-inset-bottom)); }
.store-list { display: grid; gap: 12px; }
.store-item { display: flex; align-items: center; gap: 16px; padding: 16px; background: #fff; border: 1px solid var(--skin-border, #dfe8ef); border-radius: 12px; }
.store-info { display: flex; align-items: center; gap: 14px; min-width: 0; flex: 1; }
.logo { width: 88px; height: 88px; flex: 0 0 88px; object-fit: cover; border-radius: 10px; }
.meta { min-width: 0; overflow-wrap: anywhere; }
.name { font-size: 17px; font-weight: 650; margin-bottom: 8px; }
.addr, .desc, .type { font-size: 13px; color: var(--skin-muted, #6d8192); line-height: 1.6; }
.actions { display: flex; flex-wrap: wrap; gap: 8px; }
button { min-height: 40px; padding: 8px 14px; border: 1px solid var(--skin-border, #cbdde9); border-radius: 8px; color: var(--skin-brand, #087ecc); background: #fff; font: inherit; cursor: pointer; }
button:disabled { opacity: .5; cursor: wait; }
.edit, .save, .add, .confirm-btn, .upload-submit { background: var(--skin-brand, #087ecc); border-color: var(--skin-brand, #087ecc); color: #fff; }
.del { color: #b63b4d; border-color: #efc8cf; }
.bottom-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
.editor, .modal-overlay { position: fixed; inset: 0; z-index: 2000; display: grid; place-items: center; padding: 16px; background: rgba(var(--skin-brand-strong-rgb, 16, 40, 57), 0.501961); }
.editor .card, .modal-content { width: 100%; max-width: 540px; max-height: 85dvh; overflow-y: auto; border-radius: 14px; padding: 22px; background: white; }
.form { display: grid; gap: 8px; margin: 18px 0; }
.form label { font-size: 14px; color: var(--skin-ink, #455d70); margin-top: 8px; }
.form input, .form textarea, .form select { width: 100%; min-width: 0; min-height: 44px; padding: 10px; border: 1px solid var(--skin-border, #cbdde9); border-radius: 8px; font: inherit; margin: 0; }
.form textarea { min-height: 90px; resize: vertical; }
.editor-actions, .modal-footer, .modal-header { display: flex; align-items: center; justify-content: flex-end; gap: 10px; }
.modal-header { justify-content: space-between; }
.modal-body { padding: 20px 0; line-height: 1.6; }
.close-btn { min-width: 40px; font-size: 26px; cursor: pointer; }
.upload-area { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.file-name, .limit { font-size: 12px; color: var(--skin-muted, #748799); overflow-wrap: anywhere; }
.image-preview img { display: block; max-width: 160px; max-height: 120px; object-fit: contain; }
.error { color: #b63b4d; font-size: 13px; }
@media (max-width: 480px) { .store-item { align-items: flex-start; flex-wrap: wrap; padding: 12px; } .logo { width: 64px; height: 64px; flex-basis: 64px; } .actions { width: 100%; justify-content: flex-end; } .editor .card { padding: 16px; } }
</style>
