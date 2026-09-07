import api from "./profileApi";
import { updateStoredUser } from "../utils/auth";

export const getMyProfile = async () => {
  const profile = await api.profile.get();
  updateStoredUser(profile);
  return profile;
};
export const updateMyProfile = async (fields) => {
  const profile = await api.profile.update(fields);
  updateStoredUser(profile);
  return profile;
};
