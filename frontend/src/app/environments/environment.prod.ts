export const environment = {
  production: true,
  apiUrl: 'https://api.doctorat.ma/api',  // ← URL de production
  
  
  // Configuration JWT
  tokenKey: 'accessToken',
  refreshTokenKey: 'refreshToken',
  
  // Configuration API
  apiTimeout: 30000,
  
  // Configuration features
  features: {
    registration: true,
    forgotPassword: true,
    emailVerification: true  // Activé en production
  },
  
  // Debug désactivé en production
  debug: false,
  logLevel: 'error'
};