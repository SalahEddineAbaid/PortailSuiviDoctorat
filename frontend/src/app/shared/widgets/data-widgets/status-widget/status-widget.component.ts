import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface StatusData {
    status: string;
    label: string;
    color: 'success' | 'warning' | 'error' | 'info' | 'default';
    icon?: string;
    description?: string;
    lastUpdate?: Date;
    nextAction?: string;
}

/**
 * Status Widget Component
 * Displays status with visual indicators
 * 
 * @example
 * <app-status-widget 
 *   [data]="statusData" 
 *   variant="card"
 *   [showTimestamp]="true"
 *   (actionClick)="handleAction()">
 * </app-status-widget>
 */
@Component({
    selector: 'app-status-widget',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="status-widget" [class]="'variant-' + variant">
      <!-- Badge Variant -->
      <div *ngIf="variant === 'badge'" class="status-badge" [class]="'badge-' + data.color">
        <i *ngIf="showIcon && data.icon" class="icon-{{data.icon}}"></i>
        <span>{{ data.label }}</span>
      </div>

      <!-- Card Variant -->
      <div *ngIf="variant === 'card'" class="status-card" [class]="'card-' + data.color">
        <div class="card-header">
          <div class="status-icon" *ngIf="showIcon && data.icon">
            <i class="icon-{{data.icon}}"></i>
          </div>
          <div class="status-content">
            <h4 class="status-label">{{ data.label }}</h4>
            <p *ngIf="data.description" class="status-description">{{ data.description }}</p>
          </div>
        </div>
        
        <div class="card-footer" *ngIf="data.nextAction || data.lastUpdate">
          <div *ngIf="data.nextAction" class="next-action">
            <small>Prochaine étape:</small>
            <button (click)="onActionClick()" class="action-btn">
              {{ data.nextAction }}
            </button>
          </div>
          <div *ngIf="showTimestamp && data.lastUpdate" class="timestamp">
            <small>Mis à jour: {{ formatDate(data.lastUpdate) }}</small>
          </div>
        </div>
      </div>

      <!-- Banner Variant -->
      <div *ngIf="variant === 'banner'" class="status-banner" [class]="'banner-' + data.color">
        <div class="banner-icon" *ngIf="showIcon && data.icon">
          <i class="icon-{{data.icon}}"></i>
        </div>
        <div class="banner-content">
          <strong>{{ data.label }}</strong>
          <p *ngIf="data.description">{{ data.description }}</p>
        </div>
        <button 
          *ngIf="data.nextAction" 
          (click)="onActionClick()" 
          class="banner-action">
          {{ data.nextAction }}
        </button>
      </div>
    </div>
  `,
    styles: [`
    .status-widget {
      width: 100%;
    }

    /* Badge Variant */
    .status-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.375rem;
      padding: 0.25rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.875rem;
      font-weight: 500;
    }

    .status-badge i {
      font-size: 1rem;
    }

    .badge-success {
      background-color: #d1fae5;
      color: #065f46;
    }

    .badge-warning {
      background-color: #fef3c7;
      color: #92400e;
    }

    .badge-error {
      background-color: #fee2e2;
      color: #991b1b;
    }

    .badge-info {
      background-color: #dbeafe;
      color: #1e40af;
    }

    .badge-default {
      background-color: #f3f4f6;
      color: #374151;
    }

    /* Card Variant */
    .status-card {
      background-color: white;
      border: 1px solid;
      border-radius: 0.5rem;
      overflow: hidden;
    }

    .card-success {
      border-color: #10b981;
    }

    .card-warning {
      border-color: #f59e0b;
    }

    .card-error {
      border-color: #ef4444;
    }

    .card-info {
      border-color: #3b82f6;
    }

    .card-default {
      border-color: #e5e7eb;
    }

    .card-header {
      display: flex;
      gap: 1rem;
      padding: 1.25rem;
    }

    .status-icon {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.5rem;
      flex-shrink: 0;
    }

    .card-success .status-icon {
      background-color: #d1fae5;
      color: #059669;
    }

    .card-warning .status-icon {
      background-color: #fef3c7;
      color: #d97706;
    }

    .card-error .status-icon {
      background-color: #fee2e2;
      color: #dc2626;
    }

    .card-info .status-icon {
      background-color: #dbeafe;
      color: #2563eb;
    }

    .card-default .status-icon {
      background-color: #f3f4f6;
      color: #6b7280;
    }

    .status-content {
      flex: 1;
    }

    .status-label {
      margin: 0 0 0.25rem 0;
      font-size: 1.125rem;
      font-weight: 600;
      color: #111827;
    }

    .status-description {
      margin: 0;
      font-size: 0.875rem;
      color: #6b7280;
    }

    .card-footer {
      padding: 1rem 1.25rem;
      background-color: #f9fafb;
      border-top: 1px solid #e5e7eb;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .next-action {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .next-action small {
      color: #6b7280;
      font-size: 0.75rem;
    }

    .action-btn {
      background: none;
      border: none;
      color: #3b82f6;
      font-weight: 500;
      cursor: pointer;
      text-align: left;
      padding: 0;
      font-size: 0.875rem;
    }

    .action-btn:hover {
      text-decoration: underline;
    }

    .timestamp {
      font-size: 0.75rem;
      color: #9ca3af;
    }

    /* Banner Variant */
    .status-banner {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem 1.25rem;
      border-radius: 0.5rem;
      border-left: 4px solid;
    }

    .banner-success {
      background-color: #d1fae5;
      border-color: #059669;
    }

    .banner-warning {
      background-color: #fef3c7;
      border-color: #d97706;
    }

    .banner-error {
      background-color: #fee2e2;
      border-color: #dc2626;
    }

    .banner-info {
      background-color: #dbeafe;
      border-color: #2563eb;
    }

    .banner-default {
      background-color: #f3f4f6;
      border-color: #6b7280;
    }

    .banner-icon {
      font-size: 1.5rem;
      flex-shrink: 0;
    }

    .banner-success .banner-icon { color: #059669; }
    .banner-warning .banner-icon { color: #d97706; }
    .banner-error .banner-icon { color: #dc2626; }
    .banner-info .banner-icon { color: #2563eb; }
    .banner-default .banner-icon { color: #6b7280; }

    .banner-content {
      flex: 1;
    }

    .banner-content strong {
      display: block;
      margin-bottom: 0.25rem;
      color: #111827;
    }

    .banner-content p {
      margin: 0;
      font-size: 0.875rem;
      color: #374151;
    }

    .banner-action {
      padding: 0.5rem 1rem;
      background-color: white;
      border: 1px solid #e5e7eb;
      border-radius: 0.375rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;
    }

    .banner-action:hover {
      background-color: #f9fafb;
    }

    .banner-success .banner-action { color: #059669; }
    .banner-warning .banner-action { color: #d97706; }
    .banner-error .banner-action { color: #dc2626; }
    .banner-info .banner-action { color: #2563eb; }
  `]
})
export class StatusWidgetComponent {
    @Input() data!: StatusData;
    @Input() variant: 'badge' | 'card' | 'banner' = 'badge';
    @Input() showIcon: boolean = true;
    @Input() showTimestamp: boolean = false;

    @Output() actionClick = new EventEmitter<void>();

    onActionClick(): void {
        this.actionClick.emit();
    }

    formatDate(date: Date): string {
        const now = new Date();
        const diff = now.getTime() - new Date(date).getTime();
        const days = Math.floor(diff / (1000 * 60 * 60 * 24));

        if (days === 0) {
            return 'Aujourd\'hui';
        } else if (days === 1) {
            return 'Hier';
        } else if (days < 7) {
            return `Il y a ${days} jours`;
        } else {
            return new Date(date).toLocaleDateString('fr-FR');
        }
    }
}
