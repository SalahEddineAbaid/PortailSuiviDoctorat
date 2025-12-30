import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { environment } from './environments/environment';
import { CSP_CONFIG_DEV, CSP_CONFIG_PROD, applyCSP, setupCSPReporting } from './app/core/config/csp.config';

// Apply Content Security Policy
const cspConfig = environment.production ? CSP_CONFIG_PROD : CSP_CONFIG_DEV;
applyCSP(cspConfig);

// Setup CSP violation reporting
setupCSPReporting();

// Initialize security measures
if (environment.production) {
  // Disable console in production for security
  console.log = () => {};
  console.warn = () => {};
  console.error = () => {};
}

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
