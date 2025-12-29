import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

export interface DossierValidationItem {
  id: number;
  type: 'inscription' | 'soutenance';
  doctorant: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
  };
  titre?: string; // Pour les soutenances
  sujetThese?: string; // Pour les inscriptions
  dateCreation: Date;
  statut: 'EN_COURS_VALIDATION' | 'VALIDEE' | 'REJETEE';
  priorite: 'haute' | 'normale' | 'basse';
  documentsCount: number;
  directeur?: {
    firstName: string;
    lastName: string;
  };
}

export interface ValidationAction {
  dossier: DossierValidationItem;
  action: 'valider' | 'rejeter' | 'consulter';
  commentaire?: string;
}

@Component({
  selector: 'app-dossier-validation-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './dossier-validation-list.component.html',
  styleUrl: './dossier-validation-list.component.scss'
})
export class DossierValidationListComponent {
  @Input() dossiers: DossierValidationItem[] = [];
  @Input() loading = false;
  @Input() title = 'Dossiers en attente de validation';
  @Input() showFilters = true;
  @Input() showActions = true;
  @Output() actionClicked = new EventEmitter<ValidationAction>();
  @Output() filterChanged = new EventEmitter<{type?: string, priorite?: string, search?: string}>();

  // Filtres
  selectedType = '';
  selectedPriorite = '';
  searchTerm = '';

  get filteredDossiers(): DossierValidationItem[] {
    let filtered = [...this.dossiers];

    if (this.selectedType) {
      filtered = filtered.filter(d => d.type === this.selectedType);
    }

    if (this.selectedPriorite) {
      filtered = filtered.filter(d => d.priorite === this.selectedPriorite);
    }

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase().trim();
      filtered = filtered.filter(d => 
        d.doctorant.firstName.toLowerCase().includes(term) ||
        d.doctorant.lastName.toLowerCase().includes(term) ||
        d.doctorant.email.toLowerCase().includes(term) ||
        (d.titre && d.titre.toLowerCase().includes(term)) ||
        (d.sujetThese && d.sujetThese.toLowerCase().includes(term))
      );
    }

    return filtered.sort((a, b) => {
      // Trier par priorité puis par date
      const prioriteOrder = { 'haute': 0, 'normale': 1, 'basse': 2 };
      const prioriteDiff = prioriteOrder[a.priorite] - prioriteOrder[b.priorite];
      if (prioriteDiff !== 0) return prioriteDiff;
      
      return new Date(b.dateCreation).getTime() - new Date(a.dateCreation).getTime();
    });
  }

  onFilterChange(): void {
    this.filterChanged.emit({
      type: this.selectedType || undefined,
      priorite: this.selectedPriorite || undefined,
      search: this.searchTerm || undefined
    });
  }

  onAction(dossier: DossierValidationItem, action: 'valider' | 'rejeter' | 'consulter'): void {
    this.actionClicked.emit({ dossier, action });
  }

  getTypeLabel(type: string): string {
    const labels = {
      'inscription': 'Inscription',
      'soutenance': 'Soutenance'
    };
    return labels[type as keyof typeof labels] || type;
  }

  getTypeIcon(type: string): string {
    const icons = {
      'inscription': 'fas fa-user-graduate',
      'soutenance': 'fas fa-graduation-cap'
    };
    return icons[type as keyof typeof icons] || 'fas fa-file';
  }

  getTypeColorClasses(type: string): string {
    const colors = {
      'inscription': 'bg-blue-100 text-blue-800',
      'soutenance': 'bg-purple-100 text-purple-800'
    };
    return colors[type as keyof typeof colors] || 'bg-gray-100 text-gray-800';
  }

  getPrioriteColorClasses(priorite: string): string {
    const colors = {
      'haute': 'bg-red-100 text-red-800',
      'normale': 'bg-yellow-100 text-yellow-800',
      'basse': 'bg-green-100 text-green-800'
    };
    return colors[priorite as keyof typeof colors] || 'bg-gray-100 text-gray-800';
  }

  getPrioriteLabel(priorite: string): string {
    const labels = {
      'haute': 'Haute',
      'normale': 'Normale',
      'basse': 'Basse'
    };
    return labels[priorite as keyof typeof labels] || priorite;
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  getDoctorantFullName(doctorant: { firstName: string; lastName: string }): string {
    return `${doctorant.firstName} ${doctorant.lastName}`;
  }

  getDirecteurFullName(directeur?: { firstName: string; lastName: string }): string {
    return directeur ? `${directeur.firstName} ${directeur.lastName}` : 'Non assigné';
  }

  getDossierTitle(dossier: DossierValidationItem): string {
    if (dossier.type === 'soutenance' && dossier.titre) {
      return dossier.titre;
    }
    if (dossier.type === 'inscription' && dossier.sujetThese) {
      return dossier.sujetThese;
    }
    return `${this.getTypeLabel(dossier.type)} - ${this.getDoctorantFullName(dossier.doctorant)}`;
  }

  trackByDossier(index: number, dossier: DossierValidationItem): number {
    return dossier.id;
  }
}