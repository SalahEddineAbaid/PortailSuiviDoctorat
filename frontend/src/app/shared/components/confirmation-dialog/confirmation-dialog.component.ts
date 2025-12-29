import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface ConfirmationConfig {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  confirmVariant?: 'primary' | 'danger' | 'warning';
  icon?: string;
  showIcon?: boolean;
}

@Component({
  selector: 'app-confirmation-dialog',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="dialog-overlay" (click)="onOverlayClick()" *ngIf="isOpen">
      <div 
        class="dialog-container"
        [class]="dialogClasses"
        (click)="$event.stopPropagation()"
        role="dialog"
        [attr.aria-labelledby]="titleId"
        [attr.aria-describedby]="messageId"
        aria-modal="true"
      >
        <div class="dialog-header">
          <div class="dialog-icon" *ngIf="config.showIcon !== false">
            <span class="material-icons" [class]="iconClasses" aria-hidden="true">
              {{ config.icon || getDefaultIcon() }}
            </span>
          </div>
          <h2 class="dialog-title" [id]="titleId">
            {{ config.title }}
          </h2>
        </div>
        
        <div class="dialog-body">
          <p class="dialog-message" [id]="messageId">
            {{ config.message }}
          </p>
        </div>
        
        <div class="dialog-actions">
          <button
            type="button"
            class="btn btn-secondary"
            (click)="onCancel()"
            [disabled]="loading"
          >
            {{ config.cancelText || 'Annuler' }}
          </button>
          <button
            type="button"
            class="btn"
            [class]="confirmButtonClasses"
            (click)="onConfirm()"
            [disabled]="loading"
          >
            <span *ngIf="loading" class="btn-spinner" aria-hidden="true"></span>
            {{ config.confirmText || 'Confirmer' }}
          </button>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./confirmation-dialog.component.scss']
})
export class ConfirmationDialogComponent {
  @Input() config: ConfirmationConfig = {
    title: 'Confirmation',
    message: 'Êtes-vous sûr de vouloir continuer ?'
  };
  @Input() isOpen = false;
  @Input() loading = false;
  @Input() closeOnOverlayClick = true;
  
  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  titleId = `dialog-title-${Math.random().toString(36).substr(2, 9)}`;
  messageId = `dialog-message-${Math.random().toString(36).substr(2, 9)}`;

  get dialogClasses(): string {
    return [
      'dialog',
      `dialog-${this.config.confirmVariant || 'primary'}`
    ].join(' ');
  }

  get iconClasses(): string {
    const variant = this.config.confirmVariant || 'primary';
    return `icon-${variant}`;
  }

  get confirmButtonClasses(): string {
    const variant = this.config.confirmVariant || 'primary';
    return `btn-${variant}`;
  }

  getDefaultIcon(): string {
    switch (this.config.confirmVariant) {
      case 'danger':
        return 'warning';
      case 'warning':
        return 'info';
      default:
        return 'help';
    }
  }

  onConfirm(): void {
    this.confirmed.emit();
  }

  onCancel(): void {
    this.cancelled.emit();
    this.closed.emit();
  }

  onOverlayClick(): void {
    if (this.closeOnOverlayClick && !this.loading) {
      this.onCancel();
    }
  }
}