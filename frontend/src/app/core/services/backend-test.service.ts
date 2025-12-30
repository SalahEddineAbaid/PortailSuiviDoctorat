import { Injectable } from '@angular/core';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ApiIntegrationService } from './api-integration.service';
import { AuthService } from './auth.service';
import { InscriptionService } from './inscription.service';
import { SoutenanceService } from './soutenance.service';
import { NotificationService } from './notification.service';
import { DocumentService } from './document.service';

export interface EndpointTest {
  name: string;
  endpoint: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  success: boolean;
  responseTime: number;
  error?: string;
  statusCode?: number;
}

export interface BackendTestResults {
  overall: boolean;
  timestamp: Date;
  tests: EndpointTest[];
  summary: {
    total: number;
    passed: number;
    failed: number;
    averageResponseTime: number;
  };
}

@Injectable({
  providedIn: 'root'
})
export class BackendTestService {
  constructor(
    private apiService: ApiIntegrationService,
    private authService: AuthService,
    private inscriptionService: InscriptionService,
    private soutenanceService: SoutenanceService,
    private notificationService: NotificationService,
    private documentService: DocumentService
  ) {}

  /**
   * Test all critical backend endpoints
   */
  testAllEndpoints(): Observable<BackendTestResults> {
    const tests: Observable<EndpointTest>[] = [
      // Health check
      this.testEndpoint('Health Check', '/health', 'GET', () => 
        this.apiService.testConnection()
      ),

      // Authentication
      this.testEndpoint('Authentication Test', '/auth/me', 'GET', () => 
        this.authService.getCurrentUser()
      ),

      // Inscriptions
      this.testEndpoint('My Inscriptions', '/inscriptions/me', 'GET', () => 
        this.inscriptionService.getMyInscriptions()
      ),

      this.testEndpoint('Active Campaign', '/campagnes/active', 'GET', () => 
        this.inscriptionService.getCampagneActive()
      ),

      this.testEndpoint('All Campaigns', '/campagnes', 'GET', () => 
        this.inscriptionService.getAllCampagnes()
      ),

      // Soutenances
      this.testEndpoint('My Soutenances', '/soutenances/me', 'GET', () => 
        this.soutenanceService.getMySoutenances()
      ),

      // Notifications
      this.testEndpoint('My Notifications', '/notifications/me', 'GET', () => 
        this.notificationService.getMyNotifications()
      ),

      // Documents (if user has documents)
      this.testEndpoint('Document List', '/documents/me', 'GET', () => 
        this.documentService.getMyDocuments()
      )
    ];

    return forkJoin(tests).pipe(
      map(results => this.compileResults(results))
    );
  }

  /**
   * Test specific endpoint
   */
  private testEndpoint(
    name: string, 
    endpoint: string, 
    method: 'GET' | 'POST' | 'PUT' | 'DELETE',
    testFn: () => Observable<any>
  ): Observable<EndpointTest> {
    const startTime = performance.now();

    return testFn().pipe(
      map(() => ({
        name,
        endpoint,
        method,
        success: true,
        responseTime: performance.now() - startTime
      })),
      catchError(error => {
        const responseTime = performance.now() - startTime;
        return of({
          name,
          endpoint,
          method,
          success: false,
          responseTime,
          error: error.userMessage || error.message || 'Unknown error',
          statusCode: error.status
        });
      })
    );
  }

  /**
   * Test JWT token functionality
   */
  testJWTToken(): Observable<{
    valid: boolean;
    expiration: Date | null;
    expiringSoon: boolean;
    details: string;
  }> {
    return new Observable(observer => {
      const token = this.authService.getToken();
      
      if (!token) {
        observer.next({
          valid: false,
          expiration: null,
          expiringSoon: false,
          details: 'No token found'
        });
        observer.complete();
        return;
      }

      const valid = this.apiService.validateToken(token);
      const expiration = this.apiService.getTokenExpiration(token);
      const expiringSoon = this.apiService.isTokenExpiringSoon(token);

      let details = 'Token is valid';
      if (!valid) {
        details = 'Token is invalid or expired';
      } else if (expiringSoon) {
        details = 'Token expires soon (within 5 minutes)';
      }

      observer.next({
        valid,
        expiration,
        expiringSoon,
        details
      });
      observer.complete();
    });
  }

  /**
   * Test file upload functionality
   */
  testFileUpload(): Observable<{
    success: boolean;
    error?: string;
    details: string;
  }> {
    // Create a small test file
    const testFile = new File(['test content'], 'test.txt', { type: 'text/plain' });

    return this.apiService.uploadFile('/documents/upload', testFile).pipe(
      map(response => ({
        success: true,
        details: `File uploaded successfully: ${response.filename}`
      })),
      catchError(error => of({
        success: false,
        error: error.userMessage || error.message,
        details: 'File upload failed'
      }))
    );
  }

  /**
   * Test WebSocket connection
   */
  testWebSocketConnection(): Observable<{
    success: boolean;
    error?: string;
    details: string;
  }> {
    return new Observable(observer => {
      try {
        const wsUrl = `${this.getWebSocketUrl()}?token=${this.authService.getToken()}`;
        const ws = new WebSocket(wsUrl);
        
        const timeout = setTimeout(() => {
          ws.close();
          observer.next({
            success: false,
            error: 'Connection timeout',
            details: 'WebSocket connection timed out after 5 seconds'
          });
          observer.complete();
        }, 5000);

        ws.onopen = () => {
          clearTimeout(timeout);
          ws.close();
          observer.next({
            success: true,
            details: 'WebSocket connection established successfully'
          });
          observer.complete();
        };

        ws.onerror = (error) => {
          clearTimeout(timeout);
          observer.next({
            success: false,
            error: 'Connection error',
            details: 'Failed to establish WebSocket connection'
          });
          observer.complete();
        };

      } catch (error) {
        observer.next({
          success: false,
          error: 'WebSocket not supported',
          details: 'WebSocket is not supported in this browser'
        });
        observer.complete();
      }
    });
  }

  /**
   * Test error handling
   */
  testErrorHandling(): Observable<{
    success: boolean;
    details: string;
  }> {
    // Test 404 error
    return this.apiService.get('/non-existent-endpoint').pipe(
      map(() => ({
        success: false,
        details: 'Error handling failed - should have thrown 404'
      })),
      catchError(error => of({
        success: error.status === 404,
        details: error.status === 404 
          ? 'Error handling working correctly (404 caught)'
          : `Unexpected error: ${error.status}`
      }))
    );
  }

  /**
   * Compile test results
   */
  private compileResults(tests: EndpointTest[]): BackendTestResults {
    const passed = tests.filter(t => t.success).length;
    const failed = tests.length - passed;
    const totalResponseTime = tests.reduce((sum, t) => sum + t.responseTime, 0);
    const averageResponseTime = totalResponseTime / tests.length;

    return {
      overall: failed === 0,
      timestamp: new Date(),
      tests,
      summary: {
        total: tests.length,
        passed,
        failed,
        averageResponseTime: Math.round(averageResponseTime * 100) / 100
      }
    };
  }

  /**
   * Get WebSocket URL from environment
   */
  private getWebSocketUrl(): string {
    // This should match your environment configuration
    return 'ws://localhost:8081/ws';
  }

  /**
   * Generate test report
   */
  generateTestReport(results: BackendTestResults): string {
    let report = `# Backend Integration Test Report\n\n`;
    report += `**Timestamp:** ${results.timestamp.toISOString()}\n`;
    report += `**Overall Status:** ${results.overall ? '✅ PASS' : '❌ FAIL'}\n\n`;
    
    report += `## Summary\n`;
    report += `- Total Tests: ${results.summary.total}\n`;
    report += `- Passed: ${results.summary.passed}\n`;
    report += `- Failed: ${results.summary.failed}\n`;
    report += `- Average Response Time: ${results.summary.averageResponseTime}ms\n\n`;
    
    report += `## Detailed Results\n\n`;
    
    results.tests.forEach(test => {
      const status = test.success ? '✅' : '❌';
      report += `### ${status} ${test.name}\n`;
      report += `- **Endpoint:** ${test.method} ${test.endpoint}\n`;
      report += `- **Response Time:** ${Math.round(test.responseTime * 100) / 100}ms\n`;
      
      if (!test.success) {
        report += `- **Error:** ${test.error}\n`;
        if (test.statusCode) {
          report += `- **Status Code:** ${test.statusCode}\n`;
        }
      }
      report += `\n`;
    });
    
    return report;
  }
}