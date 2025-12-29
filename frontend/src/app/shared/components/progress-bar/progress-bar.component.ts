import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

export type ProgressBarVariant = 'primary' | 'success' | 'warning' | 'error' | 'info';
export type ProgressBarSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-progress-bar',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="progress-bar-container" [class]="containerClasses">
      <div 
        *ngIf="showLabel && label" 
        class="progress-label"
        [id]="labelId"
      >
        {{ label }}
      </div>
      
      <div 
        class="progress-bar"
        [class]="progressBarClasses"
        role="progressbar"
        [attr.aria-valuenow]="value"
        [attr.aria-valuemin]="min"
        [attr.aria-valuemax]="max"
        [attr.aria-labelledby]="showLabel && label ? labelId : null"
        [attr.aria-label]="!showLabel || !label ? ariaLabel : null"
      >
        <div 
          class="progress-fill"
          [style.width.%]="progressPercentage"
          [class]="fillClasses"
        >
          <div *ngIf="animated" class="progress-animation"></div>
        </div>
      </div>
      
      <div 
        *ngIf="showPercentage" 
        class="progress-percentage"
        [attr.aria-hidden]="true"
      >
        {{ progressPercentage | number:'1.0-1' }}%
      </div>
    </div>
  `,
  styleUrls: ['./progress-bar.component.scss']
})
export class ProgressBarComponent {
  @Input() value = 0;
  @Input() min = 0;
  @Input() max = 100;
  @Input() variant: ProgressBarVariant = 'primary';
  @Input() size: ProgressBarSize = 'md';
  @Input() label = '';
  @Input() showLabel = true;
  @Input() showPercentage = false;
  @Input() animated = false;
  @Input() striped = false;
  @Input() ariaLabel = 'Progression';

  get progressPercentage(): number {
    const range = this.max - this.min;
    const adjustedValue = Math.max(this.min, Math.min(this.max, this.value));
    return range > 0 ? ((adjustedValue - this.min) / range) * 100 : 0;
  }

  get labelId(): string {
    return `progress-label-${Math.random().toString(36).substr(2, 9)}`;
  }

  get containerClasses(): string {
    return [
      'progress-container',
      `progress-${this.size}`,
      this.showPercentage ? 'with-percentage' : ''
    ].filter(Boolean).join(' ');
  }

  get progressBarClasses(): string {
    return [
      'progress-track',
      `progress-${this.variant}`,
      `progress-${this.size}`
    ].filter(Boolean).join(' ');
  }

  get fillClasses(): string {
    return [
      'progress-fill',
      `fill-${this.variant}`,
      this.animated ? 'animated' : '',
      this.striped ? 'striped' : ''
    ].filter(Boolean).join(' ');
  }
}