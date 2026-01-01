import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { trigger, transition, style, animate } from '@angular/animations';

export type AlertType = 'info' | 'success' | 'warning' | 'error';

/**
 * Alert Component
 * Displays inline alert messages with different types and auto-dismiss
 * 
 * @example
 * <app-alert type="success" message="Operation completed!" [closeable]="true"></app-alert>
 */
@Component({
    selector: 'app-alert',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div 
      class="alert alert-{{type}}" 
      *ngIf="visible" 
      [@fadeInOut]
      role="alert"
      [attr.aria-live]="type === 'error' ? 'assertive' : 'polite'">
      <div class="alert-icon">
        <i class="icon icon-{{getIcon()}}"></i>
      </div>
      <div class="alert-content">
        <strong *ngIf="title" class="alert-title">{{title}}</strong>
        <p class="alert-message">{{message}}</p>
      </div>
      <button 
        *ngIf="closeable" 
        (click)="close()" 
        class="alert-close"
        type="button"
        aria-label="Fermer">
        <i class="icon-x"></i>
      </button>
    </div>
  `,
    styles: [`
    .alert {
      display: flex;
      align-items: flex-start;
      padding: 1rem;
      border-radius: 0.375rem;
      border: 1px solid;
      margin-bottom: 1rem;
    }
    
    .alert-icon {
      flex-shrink: 0;
      margin-right: 0.75rem;
      font-size: 1.25rem;
    }
    
    .alert-content {
      flex: 1;
    }
    
    .alert-title {
      display: block;
      font-weight: 600;
      margin-bottom: 0.25rem;
    }
    
    .alert-message {
      margin: 0;
    }
    
    .alert-close {
      flex-shrink: 0;
      background: none;
      border: none;
      cursor: pointer;
      padding: 0.25rem;
      margin-left: 0.75rem;
      opacity: 0.7;
      transition: opacity 0.2s;
    }
    
    .alert-close:hover {
      opacity: 1;
    }
    
    /* Type variants */
    .alert-info {
      background-color: #dbeafe;
      border-color: #3b82f6;
      color: #1e40af;
    }
    
    .alert-success {
      background-color: #d1fae5;
      border-color: #10b981;
      color: #065f46;
    }
    
    .alert-warning {
      background-color: #fef3c7;
      border-color: #f59e0b;
      color: #92400e;
    }
    
    .alert-error {
      background-color: #fee2e2;
      border-color: #ef4444;
      color: #991b1b;
    }
  `],
    animations: [
        trigger('fadeInOut', [
            transition(':enter', [
                style({ opacity: 0, transform: 'translateY(-10px)' }),
                animate('200ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
            ]),
            transition(':leave', [
                animate('200ms ease-in', style({ opacity: 0, transform: 'translateY(-10px)' }))
            ])
        ])
    ]
})
export class AlertComponent implements OnInit, OnDestroy {
    @Input() type: AlertType = 'info';
    @Input() message: string = '';
    @Input() title?: string;
    @Input() closeable: boolean = true;
    @Input() autoDismiss: number = 0; // 0 = no auto dismiss, else milliseconds

    @Output() closed = new EventEmitter<void>();

    visible: boolean = true;
    private dismissTimer?: any;

    ngOnInit(): void {
        if (this.autoDismiss > 0) {
            this.dismissTimer = setTimeout(() => this.close(), this.autoDismiss);
        }
    }

    ngOnDestroy(): void {
        if (this.dismissTimer) {
            clearTimeout(this.dismissTimer);
        }
    }

    close(): void {
        this.visible = false;
        this.closed.emit();
    }

    getIcon(): string {
        const icons: Record<AlertType, string> = {
            info: 'info-circle',
            success: 'check-circle',
            warning: 'alert-triangle',
            error: 'x-circle'
        };
        return icons[this.type];
    }
}
