import request from '../utils/request';
import { updateStoredUser } from '../utils/auth';

export const getMyProfile = async () => {
  const profile = await request.get('/api/user');
  updateStoredUser(profile);
  return profile;
};
export const updateMyProfile = async fields => {
  const profile = await request.put('/api/user', fields);
  updateStoredUser(profile);
  return profile;
};
