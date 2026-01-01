import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Loading Spinner Component
 * Displays a loading spinner with optional overlay and message
 * 
 * @example
 * <!-- Inline spinner -->
 * <app-loading-spinner size="md"></app-loading-spinner>
 * 
 * <!-- Full screen overlay -->
 * <app-loading-spinner [overlay]="true" message="Chargement..."></app-loading-spinner>
 */
@Component({
    selector: 'app-loading-spinner',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="loading-spinner" [class.overlay]="overlay" [class.size-{{size}}]>
      <div class="spinner-wrapper">
        <div class="spinner"></div>
        <p *ngIf="message" class="loading-message">{{message}}</p>
      </div>
    </div>
  `,
    styles: [`
    .loading-spinner {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem;
    }
    
    .loading-spinner.overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: rgba(255, 255, 255, 0.9);
      z-index: 9998;
      padding: 0;
    }
    
    .spinner-wrapper {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
    }
    
    .spinner {
      border: 3px solid #f3f4f6;
      border-top-color: #3b82f6;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    
    .size-sm .spinner {
      width: 24px;
      height: 24px;
      border-width: 2px;
    }
    
    .size-md .spinner {
      width: 40px;
      height: 40px;
      border-width: 3px;
    }
    
    .size-lg .spinner {
      width: 64px;
      height: 64px;
      border-width: 4px;
    }
    
    .loading-message {
      margin: 0;
      color: #6b7280;
      font-size: 0.875rem;
      font-weight: 500;
    }
    
    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }
  `]
})
export class LoadingSpinnerComponent {
    @Input() size: 'sm' | 'md' | 'lg' = 'md';
    @Input() overlay: boolean = false;
    @Input() message?: string;
}
