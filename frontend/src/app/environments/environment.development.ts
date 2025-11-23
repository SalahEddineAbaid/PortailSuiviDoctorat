export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081/api',
  
  // Configuration JWT
  tokenKey: 'accessToken',
  refreshTokenKey: 'refreshToken',
  
  // Configuration API
  apiTimeout: 30000,
  
  // Configuration features
  features: {
    registration: true,
    forgotPassword: true,
    emailVerification: false
  },
  
  // Debug mode (uniquement en dev)
  debug: true,
  logLevel: 'debug'
};