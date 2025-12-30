import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, timer, retry, catchError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

export interface ApiResponse<T> {
  data: T;
  message?: string;
  success: boolean;
  timestamp: string;
}

export interface FileUploadResponse {
  id: number;
  filename: string;
  originalName: string;
  size: number;
  mimeType: string;
  url: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiIntegrationService {
  private readonly baseUrl = environment.apiUrl;
  private readonly maxRetries = 3;
  private readonly retryDelay = 1000;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  /**
   * Get HTTP headers with authentication
   */
  private getHeaders(): HttpHeaders {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });

    const token = this.authService.getToken();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    return headers;
  }

  /**
   * Get HTTP headers for file upload
   */
  private getFileUploadHeaders(): HttpHeaders {
    let headers = new HttpHeaders({
      'Accept': 'application/json'
    });

    const token = this.authService.getToken();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    return headers;
  }

  /**
   * Handle HTTP errors with retry logic
   */
  private handleError = (error: HttpErrorResponse) => {
    console.error('🔥 API Error:', error);
    
    let errorMessage = 'Une erreur inattendue s\'est produite';
    
    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Erreur client: ${error.error.message}`;
    } else {
      // Server-side error
      switch (error.status) {
        case 400:
          errorMessage = error.error?.message || 'Données invalides';
          break;
        case 401:
          errorMessage = 'Session expirée. Veuillez vous reconnecter';
          this.authService.logout();
          break;
        case 403:
          errorMessage = 'Accès non autorisé';
          break;
        case 404:
          errorMessage = 'Ressource non trouvée';
          break;
        case 409:
          errorMessage = error.error?.message || 'Conflit de données';
          break;
        case 422:
          errorMessage = error.error?.message || 'Données non valides';
          break;
        case 500:
          errorMessage = 'Erreur serveur. Veuillez réessayer plus tard';
          break;
        case 503:
          errorMessage = 'Service temporairement indisponible';
          break;
        default:
          errorMessage = error.error?.message || `Erreur ${error.status}`;
      }
    }

    return throwError(() => ({ ...error, userMessage: errorMessage }));
  };

  /**
   * Generic GET request with retry logic
   */
  get<T>(endpoint: string, params?: HttpParams): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}${endpoint}`, {
      headers: this.getHeaders(),
      params
    }).pipe(
      retry({
        count: this.maxRetries,
        delay: (error, retryCount) => {
          if (error.status === 401 || error.status === 403) {
            return throwError(() => error);
          }
          return timer(this.retryDelay * retryCount);
        }
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Generic POST request with retry logic
   */
  post<T>(endpoint: string, data: any): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}${endpoint}`, data, {
      headers: this.getHeaders()
    }).pipe(
      retry({
        count: this.maxRetries,
        delay: (error, retryCount) => {
          if (error.status === 401 || error.status === 403 || error.status === 400) {
            return throwError(() => error);
          }
          return timer(this.retryDelay * retryCount);
        }
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Generic PUT request with retry logic
   */
  put<T>(endpoint: string, data: any): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}${endpoint}`, data, {
      headers: this.getHeaders()
    }).pipe(
      retry({
        count: this.maxRetries,
        delay: (error, retryCount) => {
          if (error.status === 401 || error.status === 403 || error.status === 400) {
            return throwError(() => error);
          }
          return timer(this.retryDelay * retryCount);
        }
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Generic DELETE request with retry logic
   */
  delete<T>(endpoint: string): Observable<T> {
    return this.http.delete<T>(`${this.baseUrl}${endpoint}`, {
      headers: this.getHeaders()
    }).pipe(
      retry({
        count: this.maxRetries,
        delay: (error, retryCount) => {
          if (error.status === 401 || error.status === 403) {
            return throwError(() => error);
          }
          return timer(this.retryDelay * retryCount);
        }
      }),
      catchError(this.handleError)
    );
  }

  /**
   * File upload with progress tracking
   */
  uploadFile(endpoint: string, file: File, additionalData?: any): Observable<FileUploadResponse> {
    const formData = new FormData();
    formData.append('file', file, file.name);

    if (additionalData) {
      Object.keys(additionalData).forEach(key => {
        formData.append(key, additionalData[key]);
      });
    }

    return this.http.post<FileUploadResponse>(`${this.baseUrl}${endpoint}`, formData, {
      headers: this.getFileUploadHeaders(),
      reportProgress: true
    }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * File download
   */
  downloadFile(endpoint: string, filename?: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}${endpoint}`, {
      headers: this.getHeaders(),
      responseType: 'blob'
    }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Test API connectivity
   */
  testConnection(): Observable<any> {
    return this.http.get(`${this.baseUrl}/health`, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Test authentication endpoint
   */
  testAuth(): Observable<any> {
    return this.http.get(`${this.baseUrl}/auth/me`, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Test all critical endpoints
   */
  testAllEndpoints(): Observable<any> {
    const endpoints = [
      '/health',
      '/auth/me',
      '/inscriptions/me',
      '/campagnes/active',
      '/notifications/me'
    ];

    const tests = endpoints.map(endpoint => 
      this.get(endpoint).pipe(
        catchError(error => throwError(() => ({ endpoint, error })))
      )
    );

    return new Observable(observer => {
      const results: any[] = [];
      let completed = 0;

      tests.forEach((test, index) => {
        test.subscribe({
          next: (data) => {
            results[index] = { endpoint: endpoints[index], success: true, data };
            completed++;
            if (completed === tests.length) {
              observer.next(results);
              observer.complete();
            }
          },
          error: (error) => {
            results[index] = { endpoint: endpoints[index], success: false, error };
            completed++;
            if (completed === tests.length) {
              observer.next(results);
              observer.complete();
            }
          }
        });
      });
    });
  }

  /**
   * Validate JWT token format and expiration
   */
  validateToken(token: string): boolean {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        return false;
      }

      const payload = JSON.parse(atob(parts[1]));
      const now = Math.floor(Date.now() / 1000);
      
      return payload.exp > now;
    } catch (error) {
      console.error('Token validation error:', error);
      return false;
    }
  }

  /**
   * Get token expiration time
   */
  getTokenExpiration(token: string): Date | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        return null;
      }

      const payload = JSON.parse(atob(parts[1]));
      return new Date(payload.exp * 1000);
    } catch (error) {
      console.error('Token expiration parsing error:', error);
      return null;
    }
  }

  /**
   * Check if token expires soon (within 5 minutes)
   */
  isTokenExpiringSoon(token: string): boolean {
    const expiration = this.getTokenExpiration(token);
    if (!expiration) {
      return true;
    }

    const fiveMinutesFromNow = new Date(Date.now() + 5 * 60 * 1000);
    return expiration <= fiveMinutesFromNow;
  }
}