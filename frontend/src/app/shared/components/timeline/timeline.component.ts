import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TimelineEvent } from '../../../core/models/dashboard.model';

@Component({
  selector: 'app-timeline',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './timeline.component.html',
  styleUrl: './timeline.component.scss'
})
export class TimelineComponent {
  @Input() events: TimelineEvent[] = [];
  @Input() title: string = 'Timeline';

  getStatusIcon(status: string): string {
    switch (status) {
      case 'completed':
        return 'fas fa-check-circle';
      case 'current':
        return 'fas fa-clock';
      case 'upcoming':
        return 'fas fa-circle';
      case 'overdue':
        return 'fas fa-exclamation-triangle';
      default:
        return 'fas fa-circle';
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'completed':
        return 'completed';
      case 'current':
        return 'current';
      case 'upcoming':
        return 'upcoming';
      case 'overdue':
        return 'overdue';
      default:
        return 'upcoming';
    }
  }

  getTypeIcon(type: string): string {
    switch (type) {
      case 'inscription':
        return 'fas fa-user-plus';
      case 'soutenance':
        return 'fas fa-graduation-cap';
      case 'document':
        return 'fas fa-file-alt';
      case 'validation':
        return 'fas fa-check';
      default:
        return 'fas fa-info-circle';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'completed':
        return 'Terminé';
      case 'current':
        return 'En cours';
      case 'upcoming':
        return 'À venir';
      case 'overdue':
        return 'En retard';
      default:
        return 'À venir';
    }
  }

  trackByEventId(index: number, event: TimelineEvent): string {
    return event.id;
  }
}