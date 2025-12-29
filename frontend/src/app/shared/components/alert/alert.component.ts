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
  @Input() alert!: DashboardAlert;
  @Output() dismiss = new EventEmitter<string>();

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
    if (this.alert.dismissible) {
      this.dismiss.emit(this.alert.id);
    }
  }

  onAction(): void {
    // Action will be handled by router navigation
  }
}