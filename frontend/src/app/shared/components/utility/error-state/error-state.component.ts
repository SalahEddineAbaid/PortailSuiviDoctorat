import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Error State Component
 * Displays error messages with retry option
 * 
 * @example
 * <app-error-state 
 *   message="Impossible de charger les données"
 *   (retry)="loadData()">
 * </app-error-state>
 */
@Component({
    selector: 'app-error-state',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="error-state">
      <div class="error-state-icon">
        <i class="icon-alert-circle"></i>
      </div>
      <h3 class="error-state-title">{{title}}</h3>
      <p class="error-state-message">{{message}}</p>
      <p *ngIf="details && showDetails" class="error-details">{{details}}</p>
      <button 
        class="btn btn-primary"
        (click)="retry.emit()"
        type="button">
        {{retryText}}
      </button>
    </div>
  `,
    styles: [`
    .error-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 3rem 1.5rem;
      text-align: center;
    }
    
    .error-state-icon {
      width: 4rem;
      height: 4rem;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: #fee2e2;
      border-radius: 50%;
      margin-bottom: 1.5rem;
      font-size: 2rem;
      color: #ef4444;
    }
    
    .error-state-title {
      font-size: 1.25rem;
      font-weight: 600;
      color: #111827;
      margin: 0 0 0.5rem 0;
    }
    
    .error-state-message {
      color: #6b7280;
      margin: 0 0 1rem 0;
      max-width: 28rem;
    }
    
    .error-details {
      font-size: 0.875rem;
      color: #9ca3af;
      font-family: monospace;
      margin: 0 0 1.5rem 0;
      padding: 0.75rem;
      background-color: #f9fafb;
      border-radius: 0.375rem;
      max-width: 28rem;
    }
    
    .btn {
      padding: 0.625rem 1.25rem;
      border-radius: 0.375rem;
      font-weight: 500;
      cursor: pointer;
      border: none;
      transition: all 0.2s;
    }
    
    .btn-primary {
      background-color: #3b82f6;
      color: white;
    }
    
    .btn-primary:hover {
      background-color: #2563eb;
    }
  `]
})
export class ErrorStateComponent {
    @Input() title: string = 'Une erreur est survenue';
    @Input() message: string = 'Impossible de charger les données';
    @Input() details?: string;
    @Input() showDetails: boolean = false;
    @Input() retryText: string = 'Réessayer';

    @Output() retry = new EventEmitter<void>();
}
