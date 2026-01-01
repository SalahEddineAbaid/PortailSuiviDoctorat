import { Injectable } from '@angular/core';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ApiIntegrationService } from './api-integration.service';
import { AuthService } from './auth.service';

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
    private authService: AuthService
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
   * Compile test results
   */
  private compileResults(tests: EndpointTest[]): BackendTestResults {
    const passed = tests.filter(t => t.success).length;
    const failed = tests.length - passed;
    const totalResponseTime = tests.reduce((sum, t) => sum + t.responseTime, 0);
    const averageResponseTime = tests.length > 0 ? totalResponseTime / tests.length : 0;

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
    
    return report;
  }
}
