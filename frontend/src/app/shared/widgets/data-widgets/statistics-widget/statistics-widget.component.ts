import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

export interface StatisticTrend {
    value: number;         // +15, -3
    direction: 'up' | 'down' | 'stable';
}

export interface StatisticData {
    label: string;
    value: number | string;
    trend?: StatisticTrend;
    icon?: string;
    color?: 'primary' | 'success' | 'warning' | 'error' | 'info';
    description?: string;
    link?: string;
}

/**
 * Statistics Widget Component
 * Displays KPI statistics with optional trends and links
 * 
 * @example
 * <app-statistics-widget 
 *   [data]="stat" 
 *   variant="card"
 *   [showTrend]="true">
 * </app-statistics-widget>
 */
@Component({
    selector: 'app-statistics-widget',
    standalone: true,
    imports: [CommonModule, RouterModule],
    template: `
    <div class="statistics-widget" [class]="'variant-' + variant" [class]="'color-' + (data.color || 'default')">
      <!-- Card Variant -->
      <a 
        *ngIf="variant === 'card'"
        [routerLink]="data.link"
        [class.clickable]="data.link"
        class="stat-card">
        
        <div class="stat-icon" *ngIf="data.icon">
          <i class="icon-{{data.icon}}"></i>
        </div>
        
        <div class="stat-content">
          <div class="stat-header">
            <span class="stat-label">{{ data.label }}</span>
            <span *ngIf="showTrend && data.trend" class="stat-trend" [class]="'trend-' + data.trend.direction">
              <i class="icon-arrow-{{data.trend.direction === 'up' ? 'up' : 'down'}}" *ngIf="data.trend.direction !== 'stable'"></i>
              <i class="icon-minus" *ngIf="data.trend.direction === 'stable'"></i>
              {{ getTrendDisplay(data.trend) }}
            </span>
          </div>
          
          <div class="stat-value" *ngIf="!loading">{{ data.value }}</div>
          <div class="stat-loading" *ngIf="loading">
            <div class="skeleton"></div>
          </div>
          
          <p *ngIf="data.description" class="stat-description">{{ data.description }}</p>
        </div>
      </a>

      <!-- Compact Variant -->
      <div *ngIf="variant === 'compact'" class="stat-compact">
        <div class="compact-label">{{ data.label }}</div>
        <div class="compact-value">
          {{ data.value }}
          <span *ngIf="showTrend && data.trend" class="compact-trend" [class]="'trend-' + data.trend.direction">
            {{ getTrendDisplay(data.trend) }}
          </span>
        </div>
      </div>

      <!-- Detailed Variant -->
      <div *ngIf="variant === 'detailed'" class="stat-detailed">
        <div class="detailed-header">
          <div class="detailed-icon" *ngIf="data.icon">
            <i class="icon-{{data.icon}}"></i>
          </div>
          <h3>{{ data.label }}</h3>
        </div>
        
        <div class="detailed-body">
          <div class="detailed-value">{{ data.value }}</div>
          
          <div *ngIf="showTrend && data.trend" class="detailed-trend" [class]="'trend-' + data.trend.direction">
            <i class="icon-trending-{{data.trend.direction === 'up' ? 'up' : 'down'}}" *ngIf="data.trend.direction !== 'stable'"></i>
            <span>{{ getTrendDisplay(data.trend) }}</span>
            <small>vs période précédente</small>
          </div>
          
          <p *ngIf="data.description" class="detailed-description">{{ data.description }}</p>
        </div>
        
        <a *ngIf="data.link" [routerLink]="data.link" class="detailed-link">
          Voir détails →
        </a>
      </div>
    </div>
  `,
    styles: [`
    .statistics-widget {
      width: 100%;
    }

    /* Card Variant */
    .stat-card {
      display: block;
      background-color: white;
      border: 1px solid #e5e7eb;
      border-radius: 0.5rem;
      padding: 1.25rem;
      transition: all 0.2s;
      text-decoration: none;
      color: inherit;
    }

    .stat-card.clickable:hover {
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
      transform: translateY(-2px);
    }

    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: 0.5rem;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.5rem;
      margin-bottom: 1rem;
    }

    .color-primary .stat-icon {
      background-color: #dbeafe;
      color: #2563eb;
    }

    .color-success .stat-icon {
      background-color: #d1fae5;
      color: #059669;
    }

    .color-warning .stat-icon {
      background-color: #fef3c7;
      color: #d97706;
    }

    .color-error .stat-icon {
      background-color: #fee2e2;
      color: #dc2626;
    }

    .color-info .stat-icon {
      background-color: #e0e7ff;
      color: #4f46e5;
    }

    .color-default .stat-icon {
      background-color: #f3f4f6;
      color: #6b7280;
    }

    .stat-content {
      flex: 1;
    }

    .stat-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.5rem;
    }

    .stat-label {
      font-size: 0.875rem;
      color: #6b7280;
      font-weight: 500;
    }

    .stat-trend {
      display: flex;
      align-items: center;
      gap: 0.25rem;
      font-size: 0.75rem;
      font-weight: 600;
    }

    .stat-trend.trend-up {
      color: #059669;
    }

    .stat-trend.trend-down {
      color: #dc2626;
    }

    .stat-trend.trend-stable {
      color: #6b7280;
    }

    .stat-value {
      font-size: 2rem;
      font-weight: 700;
      color: #111827;
      line-height: 1;
      margin-bottom: 0.5rem;
    }

    .stat-loading .skeleton {
      height: 2rem;
      width: 60%;
      background: linear-gradient(90deg, #f3f4f6 25%, #e5e7eb 50%, #f3f4f6 75%);
      background-size: 200% 100%;
      animation: loading 1.5s ease-in-out infinite;
      border-radius: 0.25rem;
    }

    @keyframes loading {
      0% { background-position: 200% 0; }
      100% { background-position: -200% 0; }
    }

    .stat-description {
      font-size: 0.75rem;
      color: #9ca3af;
      margin: 0;
    }

    /* Compact Variant */
    .stat-compact {
      padding: 0.75rem;
      background-color: #f9fafb;
      border-radius: 0.375rem;
    }

    .compact-label {
      font-size: 0.75rem;
      color: #6b7280;
      margin-bottom: 0.25rem;
    }

    .compact-value {
      font-size: 1.5rem;
      font-weight: 700;
      color: #111827;
      display: flex;
      align-items: baseline;
      gap: 0.5rem;
    }

    .compact-trend {
      font-size: 0.875rem;
      font-weight: 500;
    }

    .compact-trend.trend-up { color: #059669; }
    .compact-trend.trend-down { color: #dc2626; }
    .compact-trend.trend-stable { color: #6b7280; }

    /* Detailed Variant */
    .stat-detailed {
      background-color: white;
      border: 1px solid #e5e7eb;
      border-radius: 0.5rem;
      overflow: hidden;
    }

    .detailed-header {
      padding: 1rem 1.25rem;
      background-color: #f9fafb;
      border-bottom: 1px solid #e5e7eb;
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .detailed-icon {
      width: 40px;
      height: 40px;
      border-radius: 0.375rem;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.25rem;
    }

    .color-primary .detailed-icon {
      background-color: #dbeafe;
      color: #2563eb;
    }

    .detailed-header h3 {
      margin: 0;
      font-size: 1rem;
      font-weight: 600;
      color: #111827;
    }

    .detailed-body {
      padding: 1.25rem;
    }

    .detailed-value {
      font-size: 2.5rem;
      font-weight: 700;
      color: #111827;
      margin-bottom: 0.75rem;
    }

    .detailed-trend {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-weight: 600;
      margin-bottom: 0.5rem;
    }

    .detailed-trend small {
      font-weight: 400;
      margin-left: auto;
    }

    .detailed-description {
      font-size: 0.875rem;
      color: #6b7280;
      margin: 0;
    }

    .detailed-link {
      display: block;
      padding: 0.75rem 1.25rem;
      background-color: #f9fafb;
      border-top: 1px solid #e5e7eb;
      color: #3b82f6;
      text-decoration: none;
      font-weight: 500;
      font-size: 0.875rem;
      transition: background-color 0.2s;
    }

    .detailed-link:hover {
      background-color: #f3f4f6;
    }
  `]
})
export class StatisticsWidgetComponent {
    @Input() data!: StatisticData;
    @Input() variant: 'card' | 'compact' | 'detailed' = 'card';
    @Input() showTrend: boolean = true;
    @Input() loading: boolean = false;

    getTrendDisplay(trend: StatisticTrend): string {
        const sign = trend.direction === 'up' ? '+' : (trend.direction === 'down' ? '-' : '');
        return `${sign}${Math.abs(trend.value)}%`;
    }
}
