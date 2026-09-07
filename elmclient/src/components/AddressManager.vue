<template>
  <p v-if="loading" role="status">正在加载地址…</p>
  <p v-if="error" class="notice error" role="alert">
    {{ error }}
    <button type="button" :disabled="busy" @click="load">重试</button>
  </p>
  <p v-if="!loading && !error && !addresses.length" class="panel muted">
    还没有收货地址，添加一个吧。
  </p>
  <article v-for="address in addresses" :key="address.id" class="panel">
    <h2>
      {{ address.contactName }}
      <span class="muted">{{ address.contactTel }}</span>
    </h2>
    <p>{{ address.address }}</p>
    <p v-if="address.isDefault" class="default-tag">默认地址</p>
    <div class="actions">
      <button
        v-if="selectable"
        type="button"
        :disabled="busy"
        @click="$emit('select', address)"
      >
        使用此地址
      </button>
      <button
        v-if="!address.isDefault"
        type="button"
        :disabled="busy"
        @click="makeDefault(address.id)"
      >
        设为默认
      </button>
      <router-link
        class="action-link"
        :to="{ path: '/editUserAddress', query: { id: address.id } }"
        >编辑</router-link
      >
      <button
        type="button"
        :disabled="busy"
        @click="pendingDelete = address.id"
      >
        删除
      </button>
    </div>
    <div v-if="pendingDelete === address.id" class="notice">
      <p>确定删除这条地址？</p>
      <div class="actions">
        <button type="button" :disabled="busy" @click="remove(address.id)">
          确认删除
        </button>
        <button type="button" :disabled="busy" @click="pendingDelete = null">
          取消
        </button>
      </div>
    </div>
  </article>
  <router-link class="action-link primary wide" to="/addUserAddress"
    >新增收货地址</router-link
  >
</template>
<script setup>
import { onMounted, ref } from "vue";
import {
  listMyAddresses,
  setMyDefaultAddress,
  removeMyAddress,
} from "../services/addressService";
defineProps({ selectable: Boolean });
defineEmits(["select"]);
const addresses = ref([]),
  loading = ref(true),
  busy = ref(false),
  error = ref(""),
  pendingDelete = ref(null);
async function load() {
  loading.value = true;
  error.value = "";
  try {
    addresses.value = await listMyAddresses();
  } catch (e) {
    error.value = e.response?.data?.message || "地址加载失败";
  } finally {
    loading.value = false;
  }
}
async function change(operation) {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    await operation();
    pendingDelete.value = null;
    await load();
  } catch (e) {
    error.value = e.response?.data?.message || "操作失败，请重试";
  } finally {
    busy.value = false;
  }
}
const makeDefault = (id) => change(() => setMyDefaultAddress(id));
const remove = (id) => change(() => removeMyAddress(id));
onMounted(load);
</script>
<style scoped>
.default-tag {
  display: inline-block;
  border-radius: 4px;
  padding: 2px 7px;
  background: #e7f4ff;
  color: #0879c6;
  font-size: 13px;
}
h2 {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
</style>
