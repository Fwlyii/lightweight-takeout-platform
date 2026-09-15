<template>
  <CheckoutPage v-if="route.query.businessId !== undefined" :key="route.fullPath" />
  <ProfilePage v-else title="收货地址"><AddressManager /></ProfilePage>
</template>
<script setup>
import ProfilePage from "../components/ProfilePage.vue";
import AddressManager from "../components/AddressManager.vue";
import CheckoutPage from "../components/CheckoutPage.vue";
import { useRoute } from 'vue-router';
const route = useRoute();
</script>
