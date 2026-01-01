import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface ProgressStep {
    id: string;
    label: string;
    status: 'completed' | 'current' | 'pending' | 'error';
    date?: Date;
    description?: string;
}

export interface ProgressData {
    current: number;      // Étape actuelle (1-indexed)
    total: number;        // Total étapes
    percentage?: number;  // Calculé auto si non fourni
    label?: string;
    steps?: ProgressStep[];
}

/**
 * Progress Widget Component
 * Displays progress in different visual formats
 * 
 * @example
 * <app-progress-widget 
 *   [data]="progressData" 
 *   variant="stepper"
 *   [showSteps]="true">
 * </app-progress-widget>
 */
@Component({
    selector: 'app-progress-widget',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="progress-widget" [class]="'variant-' + variant">
      <!-- Linear Progress -->
      <div *ngIf="variant === 'linear'" class="progress-linear">
        <div class="progress-header" *ngIf="data.label || showPercentage">
          <span class="progress-label">{{ data.label }}</span>
          <span class="progress-percentage" *ngIf="showPercentage">{{ getPercentage() }}%</span>
        </div>
        <div class="progress-bar-track">
          <div 
            class="progress-bar-fill"
            [class]="'color-' + color"
            [style.width.%]="getPercentage()">
          </div>
        </div>
        <div class="progress-info" *ngIf="data.steps">
          <small>Étape {{ data.current }} sur {{ data.total }}</small>
        </div>
      </div>

      <!-- Circular Progress -->
      <div *ngIf="variant === 'circular'" class="progress-circular">
        <svg class="circular-svg" viewBox="0 0 120 120">
          <circle
            class="circle-bg"
            cx="60"
            cy="60"
            r="54"
            fill="none"
            stroke="#e5e7eb"
            stroke-width="8">
          </circle>
          <circle
            class="circle-progress"
            [class]="'stroke-' + color"
            cx="60"
            cy="60"
            r="54"
            fill="none"
            stroke-width="8"
            [style.stroke-dasharray]="getCircumference()"
            [style.stroke-dashoffset]="getCircularOffset()">
          </circle>
        </svg>
        <div class="circular-content">
          <span class="circular-percentage">{{ getPercentage() }}%</span>
          <small *ngIf="data.label">{{ data.label }}</small>
        </div>
      </div>

      <!-- Stepper Progress -->
      <div *ngIf="variant === 'stepper' && data.steps" class="progress-stepper">
        <div 
          *ngFor="let step of data.steps; let i = index; let last = last"
          class="step-item"
          [class.completed]="step.status === 'completed'"
          [class.current]="step.status === 'current'"
          [class.error]="step.status === 'error'">
          
          <div class="step-marker">
            <div class="step-circle">
              <i 
                *ngIf="step.status === 'completed'" 
                class="icon-check">
              </i>
              <i 
                *ngIf="step.status === 'error'" 
                class="icon-x">
              </i>
              <span *ngIf="step.status === 'pending' || step.status === 'current'">
                {{ i + 1 }}
              </span>
            </div>
            <div *ngIf="!last" class="step-line"></div>
          </div>
          
          <div class="step-content">
            <div class="step-label">{{ step.label }}</div>
            <div *ngIf="step.description" class="step-description">{{ step.description }}</div>
            <div *ngIf="step.date" class="step-date">{{ formatDate(step.date) }}</div>
          </div>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .progress-widget {
      width: 100%;
    }

    /* Linear Progress */
    .progress-linear {
      width: 100%;
    }

    .progress-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.5rem;
    }

    .progress-label {
      font-weight: 500;
      color: #374151;
    }

    .progress-percentage {
      font-weight: 600;
      color: #6b7280;
    }

    .progress-bar-track {
      height: 8px;
      background-color: #e5e7eb;
      border-radius: 9999px;
      overflow: hidden;
    }

    .progress-bar-fill {
      height: 100%;
      border-radius: 9999px;
      transition: width 0.3s ease;
    }

    .progress-bar-fill.color-primary {
      background-color: #3b82f6;
    }

    .progress-bar-fill.color-success {
      background-color: #10b981;
    }

    .progress-bar-fill.color-warning {
      background-color: #f59e0b;
    }

    .progress-info {
      margin-top: 0.5rem;
      font-size: 0.875rem;
      color: #6b7280;
    }

    /* Circular Progress */
    .progress-circular {
      position: relative;
      width: 120px;
      height: 120px;
      margin: 0 auto;
    }

    .circular-svg {
      transform: rotate(-90deg);
    }

    .circle-progress {
      transition: stroke-dashoffset 0.3s ease;
    }

    .stroke-primary {
      stroke: #3b82f6;
    }

    .stroke-success {
      stroke: #10b981;
    }

    .stroke-warning {
      stroke: #f59e0b;
    }

    .circular-content {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      text-align: center;
    }

    .circular-percentage {
      display: block;
      font-size: 1.5rem;
      font-weight: 700;
      color: #111827;
    }

    .circular-content small {
      font-size: 0.75rem;
      color: #6b7280;
    }

    /* Stepper Progress */
    .progress-stepper {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .step-item {
      display: flex;
      gap: 1rem;
      position: relative;
    }

    .step-marker {
      display: flex;
      flex-direction: column;
      align-items: center;
      flex-shrink: 0;
    }

    .step-circle {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      border: 2px solid #e5e7eb;
      background-color: white;
      color: #9ca3af;
      transition: all 0.2s;
    }

    .step-item.completed .step-circle {
      background-color: #10b981;
      border-color: #10b981;
      color: white;
    }

    .step-item.current .step-circle {
      background-color: #3b82f6;
      border-color: #3b82f6;
      color: white;
    }

    .step-item.error .step-circle {
      background-color: #ef4444;
      border-color: #ef4444;
      color: white;
    }

    .step-line {
      width: 2px;
      flex: 1;
      background-color: #e5e7eb;
      margin: 0.25rem 0;
    }

    .step-item.completed .step-line {
      background-color: #10b981;
    }

    .step-content {
      flex: 1;
      padding-top: 0.5rem;
    }

    .step-label {
      font-weight: 500;
      color: #111827;
      margin-bottom: 0.25rem;
    }

    .step-description {
      font-size: 0.875rem;
      color: #6b7280;
      margin-bottom: 0.25rem;
    }

    .step-date {
      font-size: 0.75rem;
      color: #9ca3af;
    }

    .step-item.completed .step-label {
      color: #059669;
    }

    .step-item.current .step-label {
      color: #2563eb;
      font-weight: 600;
    }

    .step-item.error .step-label {
      color: #dc2626;
    }
  `]
})
export class ProgressWidgetComponent {
    @Input() data!: ProgressData;
    @Input() variant: 'linear' | 'circular' | 'stepper' = 'linear';
    @Input() color: 'primary' | 'success' | 'warning' = 'primary';
    @Input() showPercentage: boolean = true;
    @Input() showSteps: boolean = false;

    getPercentage(): number {
        if (this.data.percentage !== undefined) {
            return Math.min(100, Math.max(0, this.data.percentage));
        }

        if (this.data.total > 0) {
            return Math.round((this.data.current / this.data.total) * 100);
        }

        return 0;
    }

    getCircumference(): number {
        const radius = 54;
        return 2 * Math.PI * radius;
    }

    getCircularOffset(): number {
        const circumference = this.getCircumference();
        const percentage = this.getPercentage();
        return circumference - (percentage / 100) * circumference;
    }

    formatDate(date: Date): string {
        return new Date(date).toLocaleDateString('fr-FR', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }
}
