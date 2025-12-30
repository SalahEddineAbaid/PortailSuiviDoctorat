import { Injectable } from '@angular/core';
import { DomSanitizer, SafeHtml, SafeUrl, SafeResourceUrl } from '@angular/platform-browser';

@Injectable({
  providedIn: 'root'
})
export class SecurityService {
  constructor(private sanitizer: DomSanitizer) {}

  /**
   * Sanitize HTML content to prevent XSS attacks
   */
  sanitizeHtml(html: string): SafeHtml {
    return this.sanitizer.sanitize(1, html) || '';
  }

  /**
   * Sanitize URL to prevent malicious redirects
   */
  sanitizeUrl(url: string): SafeUrl {
    return this.sanitizer.sanitize(4, url) || '';
  }

  /**
   * Sanitize resource URL for iframes, etc.
   */
  sanitizeResourceUrl(url: string): SafeResourceUrl {
    return this.sanitizer.sanitize(5, url) || '';
  }

  /**
   * Escape HTML characters to prevent XSS
   */
  escapeHtml(text: string): string {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  /**
   * Validate and sanitize user input
   */
  sanitizeInput(input: string): string {
    if (!input) return '';
    
    // Remove potentially dangerous characters
    return input
      .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
      .replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '')
      .replace(/javascript:/gi, '')
      .replace(/on\w+\s*=/gi, '')
      .trim();
  }

  /**
   * Validate email format
   */
  isValidEmail(email: string): boolean {
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return emailRegex.test(email);
  }

  /**
   * Validate phone number format (French)
   */
  isValidPhoneNumber(phone: string): boolean {
    const phoneRegex = /^(\+33|0)[1-9](\d{8})$/;
    return phoneRegex.test(phone.replace(/\s/g, ''));
  }

  /**
   * Validate password strength
   */
  validatePasswordStrength(password: string): {
    isValid: boolean;
    score: number;
    feedback: string[];
  } {
    const feedback: string[] = [];
    let score = 0;

    if (password.length < 8) {
      feedback.push('Le mot de passe doit contenir au moins 8 caractères');
    } else {
      score += 1;
    }

    if (!/[a-z]/.test(password)) {
      feedback.push('Le mot de passe doit contenir au moins une lettre minuscule');
    } else {
      score += 1;
    }

    if (!/[A-Z]/.test(password)) {
      feedback.push('Le mot de passe doit contenir au moins une lettre majuscule');
    } else {
      score += 1;
    }

    if (!/\d/.test(password)) {
      feedback.push('Le mot de passe doit contenir au moins un chiffre');
    } else {
      score += 1;
    }

    if (!/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
      feedback.push('Le mot de passe doit contenir au moins un caractère spécial');
    } else {
      score += 1;
    }

    return {
      isValid: score >= 4,
      score,
      feedback
    };
  }

  /**
   * Generate CSRF token
   */
  generateCSRFToken(): string {
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    return Array.from(array, byte => byte.toString(16).padStart(2, '0')).join('');
  }

  /**
   * Store CSRF token securely
   */
  storeCSRFToken(token: string): void {
    sessionStorage.setItem('csrf-token', token);
  }

  /**
   * Get CSRF token
   */
  getCSRFToken(): string | null {
    return sessionStorage.getItem('csrf-token');
  }

  /**
   * Validate file type for uploads
   */
  isValidFileType(file: File, allowedTypes: string[]): boolean {
    return allowedTypes.includes(file.type);
  }

  /**
   * Validate file size
   */
  isValidFileSize(file: File, maxSizeInMB: number): boolean {
    const maxSizeInBytes = maxSizeInMB * 1024 * 1024;
    return file.size <= maxSizeInBytes;
  }

  /**
   * Scan file for potential threats (basic check)
   */
  scanFileForThreats(file: File): Promise<boolean> {
    return new Promise((resolve) => {
      const reader = new FileReader();
      
      reader.onload = (e) => {
        const content = e.target?.result as string;
        
        // Basic threat detection patterns
        const threats = [
          /<script/i,
          /javascript:/i,
          /vbscript:/i,
          /onload=/i,
          /onerror=/i,
          /eval\(/i,
          /document\.write/i
        ];

        const hasThreat = threats.some(pattern => pattern.test(content));
        resolve(!hasThreat);
      };

      reader.onerror = () => resolve(false);
      reader.readAsText(file.slice(0, 1024)); // Read first 1KB
    });
  }

  /**
   * Secure token storage
   */
  secureStoreToken(key: string, token: string): void {
    // In production, consider using httpOnly cookies
    // For now, use localStorage with additional security measures
    const encryptedToken = btoa(token); // Basic encoding (use proper encryption in production)
    localStorage.setItem(key, encryptedToken);
  }

  /**
   * Secure token retrieval
   */
  secureGetToken(key: string): string | null {
    const encryptedToken = localStorage.getItem(key);
    if (!encryptedToken) return null;
    
    try {
      return atob(encryptedToken); // Basic decoding
    } catch {
      return null;
    }
  }

  /**
   * Clear sensitive data from memory
   */
  clearSensitiveData(): void {
    // Clear tokens
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    sessionStorage.removeItem('csrf-token');
    
    // Clear any cached sensitive data
    if ('caches' in window) {
      caches.keys().then(names => {
        names.forEach(name => {
          if (name.includes('sensitive') || name.includes('auth')) {
            caches.delete(name);
          }
        });
      });
    }
  }

  /**
   * Detect potential XSS attempts
   */
  detectXSS(input: string): boolean {
    const xssPatterns = [
      /<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi,
      /<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi,
      /javascript:/gi,
      /vbscript:/gi,
      /on\w+\s*=/gi,
      /<img[^>]+src[^>]*>/gi,
      /<object\b[^<]*(?:(?!<\/object>)<[^<]*)*<\/object>/gi,
      /<embed\b[^<]*(?:(?!<\/embed>)<[^<]*)*<\/embed>/gi
    ];

    return xssPatterns.some(pattern => pattern.test(input));
  }

  /**
   * Rate limiting for API calls
   */
  private rateLimitMap = new Map<string, { count: number; resetTime: number }>();

  checkRateLimit(key: string, maxRequests: number, windowMs: number): boolean {
    const now = Date.now();
    const record = this.rateLimitMap.get(key);

    if (!record || now > record.resetTime) {
      this.rateLimitMap.set(key, { count: 1, resetTime: now + windowMs });
      return true;
    }

    if (record.count >= maxRequests) {
      return false;
    }

    record.count++;
    return true;
  }

  /**
   * Generate secure random string
   */
  generateSecureRandomString(length: number): string {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    const array = new Uint8Array(length);
    crypto.getRandomValues(array);
    
    return Array.from(array, byte => chars[byte % chars.length]).join('');
  }

  /**
   * Validate session integrity
   */
  validateSessionIntegrity(): boolean {
    const token = localStorage.getItem('accessToken');
    const sessionId = sessionStorage.getItem('sessionId');
    
    if (!token || !sessionId) {
      return false;
    }

    // Additional integrity checks can be added here
    return true;
  }

  /**
   * Log security events
   */
  logSecurityEvent(event: string, details: any): void {
    const logEntry = {
      timestamp: new Date().toISOString(),
      event,
      details,
      userAgent: navigator.userAgent,
      url: window.location.href
    };

    console.warn('🔒 Security Event:', logEntry);
    
    // In production, send to security monitoring service
    // this.sendToSecurityService(logEntry);
  }
}