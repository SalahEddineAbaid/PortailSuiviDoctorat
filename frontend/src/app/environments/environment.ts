export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  
  // Configuration JWT
  tokenKey: 'accessToken',
  refreshTokenKey: 'refreshToken',
  
  // Configuration API
  apiTimeout: 30000, // 30 secondes
  
  // Configuration features (pour activer/désactiver des fonctionnalités)
  features: {
    registration: true,
    forgotPassword: true,
    emailVerification: false
  },

  debug: true,           
  logLevel: 'debug'      
};