import { getAccountRole, getRoleDefinition } from './roles.js';

/** Login is one operation: only publish a session after both authentication and profile succeed. */
export function createAuthenticationClient(request, session) {
  return {
    async login(credentials) {
      const role = credentials.role;
      try {
        const auth = await request.post('/api/auth', credentials, { skipAuthRedirect: true });
        if (!auth?.id_token || auth.role !== role) throw new Error('登录信息不完整，请重新登录');
        const user = await request.get('/api/user', {
          headers: { Authorization: `Bearer ${auth.id_token}` }, skipAuthRedirect: true
        });
        const applicationOnly = auth.application_only === true;
        const accountRole = getAccountRole(user);
        const valid = applicationOnly
          ? accountRole === 'user' && ['merchant', 'rider'].includes(role)
          : accountRole === role;
        if (!valid) throw new Error('账号权限已发生变化，请重新登录');
        session.saveAuth(auth.id_token, user, credentials.rememberMe === true, role);
        return { user, role, applicationOnly };
      } catch (error) {
        session.clearAuth();
        throw error;
      }
    }
  };
}

export function selectLoginDestination(session, redirect, resolveRoute) {
  const definition = getRoleDefinition(session.role);
  if (session.applicationOnly) return definition.applyTarget || '/login';
  if (session.role !== 'user') return definition.target;
  if (typeof redirect !== 'string' || !redirect.startsWith('/') || redirect.startsWith('//')
      || /[\\\x00-\x1f]/.test(redirect)) return definition.target;
  const resolved = resolveRoute(redirect);
  if (!resolved?.name || ['Login', 'Register', 'Unknown'].includes(resolved.name)) return definition.target;
  return resolved.meta.role === 'user' || resolved.meta.public ? redirect : definition.target;
}
