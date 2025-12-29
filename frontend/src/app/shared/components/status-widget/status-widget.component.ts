import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

export interface StatusWidgetData {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: string;
  color: 'blue' | 'green' | 'orange' | 'red' | 'purple' | 'gray';
  trend?: {
    value: number;
    label: string;
    direction: 'up' | 'down' | 'neutral';
  };
  actionLabel?: string;
  actionRoute?: string;
}

@Component({
  selector: 'app-status-widget',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './status-widget.component.html',
  styleUrl: './status-widget.component.scss'
})
export class StatusWidgetComponent {
  @Input() data!: StatusWidgetData;
  @Input() size: 'small' | 'medium' | 'large' = 'medium';

  getColorClass(): string {
    return `widget-${this.data.color}`;
  }

  getSizeClass(): string {
    return `widget-${this.size}`;
  }

  getTrendIcon(): string {
    if (!this.data.trend) return '';
    
    switch (this.data.trend.direction) {
      case 'up':
        return 'fas fa-arrow-up';
      case 'down':
        return 'fas fa-arrow-down';
      case 'neutral':
        return 'fas fa-minus';
      default:
        return '';
    }
  }

  getTrendClass(): string {
    if (!this.data.trend) return '';
    
    switch (this.data.trend.direction) {
      case 'up':
        return 'trend-up';
      case 'down':
        return 'trend-down';
      case 'neutral':
        return 'trend-neutral';
      default:
        return '';
    }
  }
}