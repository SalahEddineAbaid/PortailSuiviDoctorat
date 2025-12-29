import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface StepperStep {
  id: string;
  label: string;
  description?: string;
  completed: boolean;
  active: boolean;
  disabled?: boolean;
  icon?: string;
}

@Component({
  selector: 'app-stepper',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="stepper" [attr.aria-label]="ariaLabel">
      <div class="stepper-header">
        <div 
          *ngFor="let step of steps; let i = index; trackBy: trackByStepId"
          class="stepper-step"
          [class.completed]="step.completed"
          [class.active]="step.active"
          [class.disabled]="step.disabled"
          [attr.aria-current]="step.active ? 'step' : null"
          [attr.aria-disabled]="step.disabled"
          (click)="onStepClick(step, i)"
          [attr.tabindex]="step.disabled ? -1 : 0"
          (keydown.enter)="onStepClick(step, i)"
          (keydown.space)="onStepClick(step, i)"
          role="button"
        >
          <div class="step-icon">
            <span *ngIf="step.completed && !step.icon" class="material-icons" aria-hidden="true">check</span>
            <span *ngIf="step.icon && !step.completed" class="material-icons" aria-hidden="true">{{ step.icon }}</span>
            <span *ngIf="!step.completed && !step.icon" class="step-number" aria-hidden="true">{{ i + 1 }}</span>
          </div>
          <div class="step-content">
            <div class="step-label">{{ step.label }}</div>
            <div *ngIf="step.description" class="step-description">{{ step.description }}</div>
          </div>
          <div *ngIf="i < steps.length - 1" class="step-connector" aria-hidden="true"></div>
        </div>
      </div>
      
      <div class="stepper-content" *ngIf="showContent">
        <ng-content></ng-content>
      </div>
    </div>
  `,
  styleUrls: ['./stepper.component.scss']
})
export class StepperComponent {
  @Input() steps: StepperStep[] = [];
  @Input() showContent = true;
  @Input() ariaLabel = 'Étapes du processus';
  @Input() allowClickNavigation = true;
  
  @Output() stepChange = new EventEmitter<{ step: StepperStep; index: number }>();

  trackByStepId(index: number, step: StepperStep): string {
    return step.id;
  }

  onStepClick(step: StepperStep, index: number): void {
    if (step.disabled || !this.allowClickNavigation) {
      return;
    }
    
    this.stepChange.emit({ step, index });
  }
}