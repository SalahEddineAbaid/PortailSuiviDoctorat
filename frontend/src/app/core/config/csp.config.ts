/**
 * Content Security Policy Configuration
 * This configuration helps prevent XSS attacks and other code injection attacks
 */

export interface CSPDirectives {
  'default-src': string[];
  'script-src': string[];
  'style-src': string[];
  'img-src': string[];
  'connect-src': string[];
  'font-src': string[];
  'object-src': string[];
  'media-src': string[];
  'frame-src': string[];
  'child-src': string[];
  'worker-src': string[];
  'manifest-src': string[];
  'base-uri': string[];
  'form-action': string[];
  'frame-ancestors': string[];
  'upgrade-insecure-requests'?: boolean;
  'block-all-mixed-content'?: boolean;
}

export const CSP_CONFIG: CSPDirectives = {
  // Default fallback for all resource types
  'default-src': ["'self'"],

  // Scripts - Allow self and inline scripts for Angular
  'script-src': [
    "'self'",
    "'unsafe-inline'", // Required for Angular in development
    "'unsafe-eval'",   // Required for Angular in development
    "https://cdn.jsdelivr.net", // For external libraries if needed
  ],

  // Styles - Allow self and inline styles
  'style-src': [
    "'self'",
    "'unsafe-inline'", // Required for Angular component styles
    "https://fonts.googleapis.com", // Google Fonts
    "https://cdn.jsdelivr.net", // External CSS libraries
  ],

  // Images - Allow self, data URLs, and external image sources
  'img-src': [
    "'self'",
    "data:",
    "https:",
    "blob:", // For generated images/previews
  ],

  // AJAX, WebSocket, and EventSource connections
  'connect-src': [
    "'self'",
    "http://localhost:8081", // Backend API
    "ws://localhost:8081",   // WebSocket connection
    "https://api.example.com", // External APIs if needed
  ],

  // Fonts
  'font-src': [
    "'self'",
    "https://fonts.gstatic.com", // Google Fonts
    "data:", // Data URLs for fonts
  ],

  // Plugins and objects (Flash, etc.) - Block all
  'object-src': ["'none'"],

  // Audio and video
  'media-src': ["'self'"],

  // Frames and iframes - Restrict to self
  'frame-src': [
    "'self'",
    // Add trusted domains for embedded content if needed
  ],

  // Child contexts (workers, frames)
  'child-src': ["'self'"],

  // Web Workers
  'worker-src': ["'self'", "blob:"],

  // Web App Manifest
  'manifest-src': ["'self'"],

  // Base URI restriction
  'base-uri': ["'self'"],

  // Form submission targets
  'form-action': ["'self'"],

  // Embedding restrictions
  'frame-ancestors': ["'none'"], // Prevent clickjacking

  // Force HTTPS in production
  'upgrade-insecure-requests': true,

  // Block mixed content
  'block-all-mixed-content': true,
};

/**
 * Generate CSP header string from configuration
 */
export function generateCSPHeader(config: CSPDirectives): string {
  const directives: string[] = [];

  Object.entries(config).forEach(([directive, value]) => {
    if (typeof value === 'boolean') {
      if (value) {
        directives.push(directive);
      }
    } else if (Array.isArray(value)) {
      directives.push(`${directive} ${value.join(' ')}`);
    }
  });

  return directives.join('; ');
}

/**
 * Development CSP configuration (more permissive)
 */
export const CSP_CONFIG_DEV: CSPDirectives = {
  ...CSP_CONFIG,
  'script-src': [
    "'self'",
    "'unsafe-inline'",
    "'unsafe-eval'",
    "http://localhost:*", // Allow local development servers
    "https://cdn.jsdelivr.net",
  ],
  'connect-src': [
    "'self'",
    "http://localhost:*", // Allow all localhost connections
    "ws://localhost:*",   // Allow all localhost WebSocket connections
    "https://api.example.com",
  ],
  'upgrade-insecure-requests': false, // Allow HTTP in development
  'block-all-mixed-content': false,   // Allow mixed content in development
};

/**
 * Production CSP configuration (more restrictive)
 */
export const CSP_CONFIG_PROD: CSPDirectives = {
  ...CSP_CONFIG,
  'script-src': [
    "'self'",
    // Remove unsafe-inline and unsafe-eval in production
    "https://cdn.jsdelivr.net",
  ],
  'style-src': [
    "'self'",
    "https://fonts.googleapis.com",
    "https://cdn.jsdelivr.net",
  ],
  'connect-src': [
    "'self'",
    "https://your-api-domain.com", // Replace with actual production API domain
    "wss://your-api-domain.com",   // Replace with actual production WebSocket domain
  ],
  'upgrade-insecure-requests': true,
  'block-all-mixed-content': true,
};

/**
 * Apply CSP to the document
 */
export function applyCSP(config: CSPDirectives): void {
  const meta = document.createElement('meta');
  meta.httpEquiv = 'Content-Security-Policy';
  meta.content = generateCSPHeader(config);
  document.head.appendChild(meta);
}

/**
 * CSP violation reporting
 */
export function setupCSPReporting(): void {
  document.addEventListener('securitypolicyviolation', (event) => {
    console.error('🚨 CSP Violation:', {
      blockedURI: event.blockedURI,
      violatedDirective: event.violatedDirective,
      originalPolicy: event.originalPolicy,
      sourceFile: event.sourceFile,
      lineNumber: event.lineNumber,
      columnNumber: event.columnNumber,
    });

    // In production, send violation reports to your security monitoring service
    // sendCSPViolationReport(event);
  });
}

/**
 * Nonce generator for inline scripts/styles
 */
export function generateNonce(): string {
  const array = new Uint8Array(16);
  crypto.getRandomValues(array);
  return btoa(String.fromCharCode(...array));
}