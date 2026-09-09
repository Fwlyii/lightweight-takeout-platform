export function resolveApiBaseUrl(configuredUrl, location, mode) {
  if (configuredUrl) return configuredUrl.replace(/\/$/, '');
  const localDevelopment = mode === 'development'
    && ['localhost', '127.0.0.1'].includes(location.hostname);
  return localDevelopment ? 'http://localhost:18080' : location.origin;
}
