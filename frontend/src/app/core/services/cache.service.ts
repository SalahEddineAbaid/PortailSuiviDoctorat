import { Injectable } from '@angular/core';
import { Observable, of, tap } from 'rxjs';

export interface CacheEntry<T> {
  data: T;
  timestamp: number;
  expiry: number;
}

@Injectable({
  providedIn: 'root'
})
export class CacheService {
  private cache = new Map<string, CacheEntry<any>>();
  private readonly DEFAULT_CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

  /**
   * Get cached data if available and not expired
   */
  get<T>(key: string): T | null {
    const cached = this.cache.get(key);
    if (!cached) {
      return null;
    }

    if (Date.now() > cached.expiry) {
      this.cache.delete(key);
      return null;
    }

    return cached.data;
  }

  /**
   * Set data in cache with optional custom duration
   */
  set<T>(key: string, data: T, durationMs?: number): void {
    const duration = durationMs || this.DEFAULT_CACHE_DURATION;
    const entry: CacheEntry<T> = {
      data,
      timestamp: Date.now(),
      expiry: Date.now() + duration
    };
    this.cache.set(key, entry);
  }

  /**
   * Remove specific cache entry
   */
  remove(key: string): void {
    this.cache.delete(key);
  }

  /**
   * Clear all cache entries
   */
  clear(): void {
    this.cache.clear();
  }

  /**
   * Clear expired cache entries
   */
  clearExpired(): void {
    const now = Date.now();
    for (const [key, entry] of this.cache.entries()) {
      if (now > entry.expiry) {
        this.cache.delete(key);
      }
    }
  }

  /**
   * Get cache size
   */
  size(): number {
    return this.cache.size;
  }

  /**
   * Cache an Observable result
   */
  cacheObservable<T>(
    key: string,
    source: Observable<T>,
    durationMs?: number
  ): Observable<T> {
    const cached = this.get<T>(key);
    if (cached !== null) {
      return of(cached);
    }

    return source.pipe(
      tap(data => this.set(key, data, durationMs))
    );
  }

  /**
   * Get cache statistics
   */
  getStats(): { size: number; keys: string[] } {
    return {
      size: this.cache.size,
      keys: Array.from(this.cache.keys())
    };
  }
}