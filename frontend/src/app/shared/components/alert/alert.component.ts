import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DashboardAlert } from '../../../core/models/dashboard.model';

@Component({
  selector: 'app-alert',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './alert.component.html',
  styleUrl: './alert.component.scss'
})
export class AlertComponent {
  @Input() alert?: DashboardAlert;
  @Input() type: string = 'info';
  @Input() message: string = '';
  @Input() dismissible: boolean = true;
  @Output() dismiss = new EventEmitter<string>();
  @Output() dismissed = new EventEmitter<void>();

  get alertData(): DashboardAlert {
    return this.alert || {
      id: 'inline-alert',
      type: this.type as any,
      title: '',
      message: this.message,
      dismissible: this.dismissible
    };
  }

  getAlertIcon(type: string): string {
    switch (type) {
      case 'warning':
        return 'fas fa-exclamation-triangle';
      case 'error':
        return 'fas fa-times-circle';
      case 'info':
        return 'fas fa-info-circle';
      case 'success':
        return 'fas fa-check-circle';
      default:
        return 'fas fa-info-circle';
    }
  }

  onDismiss(): void {
    if (this.alertData.dismissible) {
      this.dismiss.emit(this.alertData.id);
      this.dismissed.emit();
    }
  }

  onAction(): void {
    // Action will be handled by router navigation
  }
}