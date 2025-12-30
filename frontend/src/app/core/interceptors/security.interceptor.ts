import { HttpRequest, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { inject } from '@angular/core';
import { SecurityService } from '../services/security.service';

/**
 * 🔒 Intercepteur de sécurité
 * ⚠️ N'injecte PAS AuthService pour éviter les dépendances circulaires
 */
export const securityInterceptor: HttpInterceptorFn = (req, next) => {
  const securityService = inject(SecurityService);

  let secureReq = req;

  // Add CSRF token for state-changing requests
  if (isStateChangingRequest(req.method)) {
    const csrfToken = securityService.getCSRFToken();
    if (csrfToken) {
      secureReq = req.clone({
        setHeaders: {
          'X-CSRF-Token': csrfToken
        }
      });
    }
  }

  // Add security headers
  secureReq = secureReq.clone({
    setHeaders: {
      'X-Requested-With': 'XMLHttpRequest',
      'X-Content-Type-Options': 'nosniff',
      'X-Frame-Options': 'DENY',
      'X-XSS-Protection': '1; mode=block'
    }
  });

  // Rate limiting check
  const rateLimitKey = `${req.method}:${req.url}`;
  if (!securityService.checkRateLimit(rateLimitKey, 100, 60000)) { // 100 requests per minute
    securityService.logSecurityEvent('RATE_LIMIT_EXCEEDED', {
      method: req.method,
      url: req.url,
      timestamp: new Date().toISOString()
    });
    
    return throwError(() => new HttpErrorResponse({
      status: 429,
      statusText: 'Too Many Requests',
      error: { message: 'Rate limit exceeded. Please try again later.' }
    }));
  }

  // Validate request data for potential XSS
  if (req.body && typeof req.body === 'object') {
    validateRequestBody(req.body, securityService);
  }

  return next(secureReq).pipe(
    tap(event => {
      // Log successful requests for monitoring
      if (event.type === 4) { // HttpEventType.Response
        logSecureRequest(req, true, securityService);
      }
    }),
    catchError((error: HttpErrorResponse) => {
      // Log failed requests
      logSecureRequest(req, false, securityService, error);
      
      // Handle specific security-related errors
      if (error.status === 403) {
        securityService.logSecurityEvent('FORBIDDEN_ACCESS', {
          url: req.url,
          method: req.method,
          error: error.message
        });
      }

      if (error.status === 401) {
        securityService.logSecurityEvent('UNAUTHORIZED_ACCESS', {
          url: req.url,
          method: req.method,
          error: error.message
        });
      }

      return throwError(() => error);
    })
  );
};

/**
 * Check if request method is state-changing
 */
function isStateChangingRequest(method: string): boolean {
  return ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method.toUpperCase());
}

/**
 * Validate request body for potential security threats
 */
function validateRequestBody(body: any, securityService: SecurityService): void {
  const jsonString = JSON.stringify(body);
  
  if (securityService.detectXSS(jsonString)) {
    securityService.logSecurityEvent('XSS_ATTEMPT_DETECTED', {
      body: jsonString.substring(0, 500), // Log first 500 chars
      timestamp: new Date().toISOString()
    });
    
    throw new Error('Potentially malicious content detected');
  }
}

/**
 * Log secure requests for monitoring
 */
function logSecureRequest(req: HttpRequest<any>, success: boolean, securityService: SecurityService, error?: HttpErrorResponse): void {
  const logData = {
    method: req.method,
    url: req.url,
    success,
    timestamp: new Date().toISOString(),
    userAgent: navigator.userAgent
  };

  if (error) {
    (logData as any).error = {
      status: error.status,
      message: error.message
    };
  }

  // In development, only log errors and security-relevant requests
  if (!success || isSecurityRelevantRequest(req)) {
    console.log('🔒 Security Request Log:', logData);
  }

  // In production, send to security monitoring service
  // this.sendToSecurityMonitoring(logData);
}

/**
 * Check if request is security-relevant
 */
function isSecurityRelevantRequest(req: HttpRequest<any>): boolean {
  const securityEndpoints = [
    '/auth/',
    '/login',
    '/logout',
    '/register',
    '/password',
    '/admin/',
    '/upload'
  ];

  return securityEndpoints.some(endpoint => req.url.includes(endpoint));
}