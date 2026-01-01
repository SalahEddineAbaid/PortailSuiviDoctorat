import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface StatusEvent {
    id: string;
    status: string;
    label: string;
    date: Date;
    user?: string;
    comment?: string;
    icon?: string;
    color?: 'success' | 'warning' | 'error' | 'info' | 'default';
}

/**
 * Status Tracking Component
 * Displays timeline of status events
 * 
 * @example
 * <app-status-tracking 
 *   [events]="statusEvents"
 *   variant="vertical">
 * </app-status-tracking>
 */
@Component({
    selector: 'app-status-tracking',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="status-tracking" [class]="'variant-' + variant">
      <!-- Vertical Timeline -->
      <div *ngIf="variant === 'vertical'" class="timeline-vertical">
        <div 
          *ngFor="let event of events; let first = first; let last = last"
          class="timeline-item"
          [class.first]="first"
          [class.last]="last"
          [class]="'item-' + (event.color || 'default')">
          
          <div class="timeline-marker">
            <div class="marker-circle">
              <i class="icon-{{event.icon || 'circle'}}" *ngIf="event.icon"></i>
              <span *ngIf="!event.icon" class="marker-dot"></span>
            </div>
            <div *ngIf="!last" class="marker-line"></div>
          </div>
          
          <div class="timeline-content">
            <div class="event-header">
              <strong class="event-label">{{event.label}}</strong>
              <span class="event-date">{{formatDate(event.date)}}</span>
            </div>
            <p *ngIf="event.comment" class="event-comment">{{event.comment}}</p>
            <small *ngIf="event.user" class="event-user">
              <i class="icon-user"></i>
              {{event.user}}
            </small>
          </div>
        </div>
      </div>

      <!-- Horizontal Timeline -->
      <div *ngIf="variant === 'horizontal'" class="timeline-horizontal">
        <div 
          *ngFor="let event of events; let last = last"
          class="h-timeline-item"
          [class]="'item-' + (event.color || 'default')">
          
          <div class="h-item-marker">
            <div class="h-marker-circle">
              <i class="icon-{{event.icon || 'check'}}"></i>
            </div>
            <div *ngIf="!last" class="h-marker-line"></div>
          </div>
          
          <div class="h-item-content">
            <div class="h-event-label">{{event.label}}</div>
            <div class="h-event-date">{{formatDateShort(event.date)}}</div>
          </div>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .status-tracking {
      width: 100%;
    }

    /* Vertical Timeline */
    .timeline-vertical {
      display: flex;
      flex-direction: column;
    }

    .timeline-item {
      display: flex;
      gap: 1rem;
      position: relative;
      padding-bottom: 1.5rem;
    }

    .timeline-item.last {
      padding-bottom: 0;
    }

    .timeline-marker {
      display: flex;
      flex-direction: column;
      align-items: center;
      flex-shrink: 0;
    }

    .marker-circle {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: white;
      border: 2px solid #e5e7eb;
      position: relative;
      z-index: 2;
      transition: all 0.2s;
    }

    .marker-circle i {
      font-size: 1rem;
      color: #6b7280;
    }

    .marker-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background-color: #9ca3af;
    }

    .item-success .marker-circle {
      background-color: #d1fae5;
      border-color: #10b981;
    }

    .item-success .marker-circle i {
      color: #059669;
    }

    .item-warning .marker-circle {
      background-color: #fef3c7;
      border-color: #f59e0b;
    }

    .item-warning .marker-circle i {
      color: #d97706;
    }

    .item-error .marker-circle {
      background-color: #fee2e2;
      border-color: #ef4444;
    }

    .item-error .marker-circle i {
      color: #dc2626;
    }

    .item-info .marker-circle {
      background-color: #dbeafe;
      border-color: #3b82f6;
    }

    .item-info .marker-circle i {
      color: #2563eb;
    }

    .marker-line {
      width: 2px;
      flex: 1;
      background-color: #e5e7eb;
      margin: 0.25rem 0;
      min-height: 20px;
    }

    .item-success .marker-line {
      background-color: #d1d5db;
    }

    .timeline-content {
      flex: 1;
      padding-top: 0.375rem;
    }

    .event-header {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      margin-bottom: 0.5rem;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .event-label {
      font-size: 1rem;
      color: #111827;
    }

    .event-date {
      font-size: 0.875rem;
      color: #6b7280;
    }

    .event-comment {
      margin: 0 0 0.5rem 0;
      font-size: 0.875rem;
      color: #374151;
      padding: 0.75rem;
      background-color: #f9fafb;
      border-radius: 0.375rem;
      border-left: 3px solid #e5e7eb;
    }

    .item-success .event-comment {
      border-left-color: #10b981;
    }

    .item-error .event-comment {
      border-left-color: #ef4444;
    }

    .event-user {
      display: flex;
      align-items: center;
      gap: 0.375rem;
      font-size: 0.75rem;
      color: #9ca3af;
    }

    .event-user i {
      font-size: 0.875rem;
    }

    /* Horizontal Timeline */
    .timeline-horizontal {
      display: flex;
      align-items: flex-start;
      overflow-x: auto;
      padding: 1rem 0;
    }

    .h-timeline-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      min-width: 120px;
      flex-shrink: 0;
    }

    .h-item-marker {
      display: flex;
      align-items: center;
      margin-bottom: 0.75rem;
      width: 100%;
    }

    .h-marker-circle {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: white;
      border: 2px solid #e5e7eb;
      flex-shrink: 0;
    }

    .h-marker-circle i {
      font-size: 0.875rem;
      color: #6b7280;
    }

    .item-success .h-marker-circle {
      background-color: #d1fae5;
      border-color: #10b981;
    }

    .item-success .h-marker-circle i {
      color: #059669;
    }

    .h-marker-line {
      height: 2px;
      flex: 1;
      background-color: #e5e7eb;
      margin: 0 0.5rem;
    }

    .item-success .h-marker-line {
      background-color: #10b981;
    }

    .h-item-content {
      text-align: center;
    }

    .h-event-label {
      font-size: 0.875rem;
      font-weight: 500;
      color: #111827;
      margin-bottom: 0.25rem;
    }

    .h-event-date {
      font-size: 0.75rem;
      color: #6b7280;
    }

    @media (max-width: 640px) {
      .timeline-item {
        gap: 0.75rem;
      }

      .marker-circle {
        width: 32px;
        height: 32px;
      }

      .marker-circle i {
        font-size: 0.875rem;
      }
    }
  `]
})
export class StatusTrackingComponent {
    @Input() events: StatusEvent[] = [];
    @Input() variant: 'vertical' | 'horizontal' = 'vertical';

    formatDate(date: Date): string {
        return new Date(date).toLocaleDateString('fr-FR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    formatDateShort(date: Date): string {
        return new Date(date).toLocaleDateString('fr-FR', {
            day: 'numeric',
            month: 'short'
        });
    }
}
