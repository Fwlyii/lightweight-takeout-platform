<template>
  <ProfilePage
    :title="id ? '编辑收货地址' : '新增收货地址'"
    back-to="/userAddress"
  >
    <p v-if="loading" role="status">正在加载地址…</p>
    <p v-if="error" class="notice error" role="alert">{{ error }}</p>
    <form v-if="ready" class="panel" @submit.prevent="save">
      <fieldset :disabled="busy">
        <label
          >联系人<input
            v-model.trim="form.contactName"
            required
            maxlength="40"
            autocomplete="name"
            placeholder="姓名"
        /></label>
        <label
          >称呼<select v-model="form.contactSex">
            <option :value="null">不填写</option>
            <option :value="1">先生</option>
            <option :value="0">女士</option>
          </select></label
        >
        <label
          >联系电话<input
            v-model.trim="form.contactTel"
            required
            pattern="1[3-9][0-9]{9}"
            maxlength="11"
            inputmode="tel"
            autocomplete="tel"
            placeholder="收餐人的手机号"
        /></label>
        <label
          >详细地址<input
            v-model.trim="form.address"
            required
            maxlength="255"
            autocomplete="street-address"
            placeholder="学校、楼栋及门牌号"
        /></label>
        <button class="primary wide" type="submit">
          {{ busy ? "保存中…" : "保存地址" }}
        </button>
      </fieldset>
    </form>
    <router-link v-if="!loading && !ready" class="action-link" to="/userAddress"
      >返回地址列表</router-link
    >
  </ProfilePage>
</template>
<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import ProfilePage from "./ProfilePage.vue";
import {
  getMyAddress,
  createMyAddress,
  updateMyAddress,
} from "../services/addressService";
const props = defineProps({ id: { type: String, default: "" } }),
  router = useRouter();
const form = reactive({
  contactName: "",
  contactSex: null,
  contactTel: "",
  address: "",
});
const loading = ref(false),
  busy = ref(false),
  error = ref(""),
  ready = ref(!props.id);
onMounted(async () => {
  if (!props.id) return;
  loading.value = true;
  try {
    const data = await getMyAddress(props.id);
    for (const key of Object.keys(form))
      form[key] = data[key] ?? (key === "contactSex" ? null : "");
    ready.value = true;
  } catch (e) {
    error.value = e.response?.data?.message || "地址不存在或无法访问";
  } finally {
    loading.value = false;
  }
});
async function save() {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    if (props.id) await updateMyAddress(props.id, { ...form });
    else await createMyAddress({ ...form });
    await router.replace("/userAddress");
  } catch (e) {
    error.value = e.response?.data?.message || "保存失败，请重试";
  } finally {
    busy.value = false;
  }
}
</script>
