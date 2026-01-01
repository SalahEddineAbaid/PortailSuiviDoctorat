import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Progress Bar Component
 * Displays a horizontal progress bar with percentage
 * 
 * @example
 * <app-progress-bar [value]="75" color="success"></app-progress-bar>
 * <app-progress-bar [indeterminate]="true"></app-progress-bar>
 */
@Component({
    selector: 'app-progress-bar',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="progress-bar-container">
      <div class="progress-bar-track">
        <div 
          class="progress-bar-fill progress-{{color}}"
          [style.width.%]="indeterminate ? 100 : value"
          [class.indeterminate]="indeterminate"
          role="progressbar"
          [attr.aria-valuenow]="value"
          [attr.aria-valuemin]="0"
          [attr.aria-valuemax]="100">
        </div>
      </div>
      <span *ngIf="showPercentage && !indeterminate" class="progress-percentage">
        {{value}}%
      </span>
    </div>
  `,
    styles: [`
    .progress-bar-container {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    
    .progress-bar-track {
      flex: 1;
      height: 0.5rem;
      background-color: #e5e7eb;
      border-radius: 9999px;
      overflow: hidden;
    }
    
    .progress-bar-fill {
      height: 100%;
      border-radius: 9999px;
      transition: width 0.3s ease;
    }
    
    .progress-bar-fill.indeterminate {
      width: 30% !important;
      animation: indeterminate 1.5s ease-in-out infinite;
    }
    
    .progress-primary {
      background-color: #3b82f6;
    }
    
    .progress-success {
      background-color: #10b981;
    }
    
    .progress-warning {
      background-color: #f59e0b;
    }
    
    .progress-error {
      background-color: #ef4444;
    }
    
    .progress-percentage {
      font-size: 0.875rem;
      font-weight: 600;
      color: #6b7280;
      min-width: 3rem;
      text-align: right;
    }
    
    @keyframes indeterminate {
      0% {
        transform: translateX(-100%);
      }
      100% {
        transform: translateX(400%);
      }
    }
  `]
})
export class ProgressBarComponent {
    @Input() value: number = 0;  // 0-100
    @Input() color: 'primary' | 'success' | 'warning' | 'error' = 'primary';
    @Input() indeterminate: boolean = false;
    @Input() showPercentage: boolean = true;
}
