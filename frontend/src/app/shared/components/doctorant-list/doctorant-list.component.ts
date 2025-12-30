import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { UserResponse } from '../../../core/models/user.model';
import { Role } from '../../../core/models/role.model';

export interface DoctorantListItem {
  id: number;
  FirstName: string;
  LastName: string;
  email: string;
  phoneNumber: string;
  adresse: string;
  ville: string;
  pays: string;
  roles: Role[];
  enabled: boolean;
  inscriptionStatus?: string;
  soutenanceStatus?: string;
  lastActivity?: Date;
  pendingActions?: number;
}

@Component({
  selector: 'app-doctorant-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './doctorant-list.component.html',
  styleUrl: './doctorant-list.component.scss'
})
export class DoctorantListComponent {
  @Input() doctorants: DoctorantListItem[] = [];
  @Input() title: string = 'Liste des doctorants';
  @Input() showActions: boolean = true;
  @Output() viewDetails = new EventEmitter<DoctorantListItem>();
  @Output() viewDossier = new EventEmitter<DoctorantListItem>();

  onViewDetails(doctorant: DoctorantListItem): void {
    this.viewDetails.emit(doctorant);
  }

  onViewDossier(doctorant: DoctorantListItem): void {
    this.viewDossier.emit(doctorant);
  }

  getStatusColor(status?: string): string {
    switch (status) {
      case 'VALIDEE':
      case 'AUTORISEE':
        return 'green';
      case 'EN_COURS_VALIDATION':
        return 'orange';
      case 'REJETEE':
        return 'red';
      case 'SOUMISE':
        return 'blue';
      default:
        return 'gray';
    }
  }

  getStatusLabel(status?: string): string {
    switch (status) {
      case 'VALIDEE':
        return 'Validée';
      case 'EN_COURS_VALIDATION':
        return 'En validation';
      case 'REJETEE':
        return 'Rejetée';
      case 'SOUMISE':
        return 'Soumise';
      case 'AUTORISEE':
        return 'Autorisée';
      case 'SOUTENUE':
        return 'Soutenue';
      default:
        return 'Aucune';
    }
  }

  getInitials(firstName: string, lastName: string): string {
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  }

  formatLastActivity(date?: Date): string {
    if (!date) return 'Aucune activité';
    
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - date.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 1) return 'Hier';
    if (diffDays < 7) return `Il y a ${diffDays} jours`;
    if (diffDays < 30) return `Il y a ${Math.ceil(diffDays / 7)} semaines`;
    return `Il y a ${Math.ceil(diffDays / 30)} mois`;
  }

  trackByDoctorantId(index: number, doctorant: DoctorantListItem): number {
    return doctorant.id;
  }
}