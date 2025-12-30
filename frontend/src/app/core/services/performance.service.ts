import { Injectable } from '@angular/core';
import { CacheService } from './cache.service';

@Injectable({
  providedIn: 'root'
})
export class PerformanceService {
  private performanceObserver?: PerformanceObserver;
  private metrics: Map<string, number> = new Map();

  constructor(private cacheService: CacheService) {
    this.initPerformanceObserver();
  }

  /**
   * Initialize performance observer for monitoring
   */
  private initPerformanceObserver(): void {
    if ('PerformanceObserver' in window) {
      this.performanceObserver = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          this.recordMetric(entry.name, entry.duration);
        }
      });

      this.performanceObserver.observe({ 
        entryTypes: ['navigation', 'resource', 'measure'] 
      });
    }
  }

  /**
   * Record a performance metric
   */
  recordMetric(name: string, value: number): void {
    this.metrics.set(name, value);
    console.log(`📊 Performance metric: ${name} = ${value.toFixed(2)}ms`);
  }

  /**
   * Get all recorded metrics
   */
  getMetrics(): Map<string, number> {
    return new Map(this.metrics);
  }

  /**
   * Clear performance metrics
   */
  clearMetrics(): void {
    this.metrics.clear();
  }

  /**
   * Measure execution time of a function
   */
  measureExecution<T>(name: string, fn: () => T): T {
    const start = performance.now();
    const result = fn();
    const end = performance.now();
    this.recordMetric(name, end - start);
    return result;
  }

  /**
   * Measure async execution time
   */
  async measureAsyncExecution<T>(name: string, fn: () => Promise<T>): Promise<T> {
    const start = performance.now();
    const result = await fn();
    const end = performance.now();
    this.recordMetric(name, end - start);
    return result;
  }

  /**
   * Preload critical resources
   */
  preloadResource(url: string, type: 'script' | 'style' | 'image' = 'script'): void {
    const link = document.createElement('link');
    link.rel = 'preload';
    link.href = url;
    link.as = type;
    document.head.appendChild(link);
  }

  /**
   * Lazy load images with intersection observer
   */
  lazyLoadImages(): void {
    if ('IntersectionObserver' in window) {
      const imageObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            const img = entry.target as HTMLImageElement;
            if (img.dataset['src']) {
              img.src = img.dataset['src'];
              img.classList.remove('lazy');
              imageObserver.unobserve(img);
            }
          }
        });
      });

      document.querySelectorAll('img[data-src]').forEach(img => {
        imageObserver.observe(img);
      });
    }
  }

  /**
   * Optimize bundle loading with prefetch
   */
  prefetchRoute(routePath: string): void {
    const link = document.createElement('link');
    link.rel = 'prefetch';
    link.href = routePath;
    document.head.appendChild(link);
  }

  /**
   * Get cache statistics
   */
  getCacheStats(): { size: number; keys: string[] } {
    return this.cacheService.getStats();
  }

  /**
   * Clean up expired cache entries
   */
  cleanupCache(): void {
    this.cacheService.clearExpired();
  }

  /**
   * Get memory usage information
   */
  getMemoryUsage(): any {
    if ('memory' in performance) {
      return (performance as any).memory;
    }
    return null;
  }

  /**
   * Monitor Core Web Vitals
   */
  monitorWebVitals(): void {
    // Largest Contentful Paint (LCP)
    new PerformanceObserver((entryList) => {
      for (const entry of entryList.getEntries()) {
        this.recordMetric('LCP', entry.startTime);
      }
    }).observe({ type: 'largest-contentful-paint', buffered: true });

    // First Input Delay (FID)
    new PerformanceObserver((entryList) => {
      for (const entry of entryList.getEntries()) {
        this.recordMetric('FID', (entry as any).processingStart - entry.startTime);
      }
    }).observe({ type: 'first-input', buffered: true });

    // Cumulative Layout Shift (CLS)
    let clsValue = 0;
    new PerformanceObserver((entryList) => {
      for (const entry of entryList.getEntries()) {
        if (!(entry as any).hadRecentInput) {
          clsValue += (entry as any).value;
          this.recordMetric('CLS', clsValue);
        }
      }
    }).observe({ type: 'layout-shift', buffered: true });
  }

  /**
   * Dispose performance observer
   */
  dispose(): void {
    if (this.performanceObserver) {
      this.performanceObserver.disconnect();
    }
  }
}