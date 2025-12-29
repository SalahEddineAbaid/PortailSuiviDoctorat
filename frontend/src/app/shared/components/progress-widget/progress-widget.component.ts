import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProgressIndicator } from '../../../core/models/dashboard.model';

@Component({
  selector: 'app-progress-widget',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './progress-widget.component.html',
  styleUrl: './progress-widget.component.scss'
})
export class ProgressWidgetComponent {
  @Input() progress!: ProgressIndicator;
  @Input() color: string = 'blue';
  @Input() size: 'small' | 'medium' | 'large' = 'medium';

  getProgressColor(): string {
    const colors = {
      blue: '#3b82f6',
      green: '#10b981',
      orange: '#f59e0b',
      red: '#ef4444',
      purple: '#8b5cf6'
    };
    return colors[this.color as keyof typeof colors] || colors.blue;
  }

  getProgressClass(): string {
    return `progress-${this.size}`;
  }
}