import api from "./profileApi";
export const listMyAddresses = api.addresses.list;
export const getMyAddress = api.addresses.get;
export const createMyAddress = api.addresses.create;
export const updateMyAddress = api.addresses.update;
export const removeMyAddress = api.addresses.remove;
export const setMyDefaultAddress = api.addresses.setDefault;
