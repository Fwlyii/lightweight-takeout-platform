<template><AddressEditor :key="addressId" :id="addressId" /></template>
<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import AddressEditor from "../components/AddressEditor.vue";
const route = useRoute(),
  addressId = computed(() =>
    String(route.query.id || route.query.addressId || "invalid")
  );
</script>
