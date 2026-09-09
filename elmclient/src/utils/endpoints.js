import { getToken } from './auth';
import { resolveApiBaseUrl } from './resolveApiBaseUrl';

export const apiBaseUrl = resolveApiBaseUrl(
  process.env.VUE_APP_API_BASE_URL, window.location, process.env.NODE_ENV
);

export const getWebSocketUrl = (path) => {
  const url = new URL(path, `${apiBaseUrl}/`);
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  const token = getToken();
  if (token) url.searchParams.set('access_token', token);
  return url.toString();
};
