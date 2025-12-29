import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

export type SpinnerSize = 'sm' | 'md' | 'lg' | 'xl';
export type SpinnerVariant = 'primary' | 'secondary' | 'white';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div 
      class="loading-spinner"
      [class]="spinnerClasses"
      [attr.aria-label]="ariaLabel"
      [attr.aria-hidden]="!showLabel"
      role="status"
    >
      <div class="spinner-circle"></div>
      <div *ngIf="showLabel && label" class="spinner-label">
        {{ label }}
      </div>
    </div>
  `,
  styleUrls: ['./loading-spinner.component.scss']
})
export class LoadingSpinnerComponent {
  @Input() size: SpinnerSize = 'md';
  @Input() variant: SpinnerVariant = 'primary';
  @Input() label = 'Chargement...';
  @Input() showLabel = false;
  @Input() ariaLabel = 'Chargement en cours';
  @Input() centered = false;
  @Input() overlay = false;

  get spinnerClasses(): string {
    return [
      'spinner',
      `spinner-${this.size}`,
      `spinner-${this.variant}`,
      this.centered ? 'centered' : '',
      this.overlay ? 'overlay' : ''
    ].filter(Boolean).join(' ');
  }
}