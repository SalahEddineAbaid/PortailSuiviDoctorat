import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';

import { InscriptionService } from '../../../core/services/inscription.service';
import { CampagneResponse, TypeInscription } from '../../../core/models/inscription.model';

@Component({
  selector: 'app-campagne-info',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './campagne-info.html',
  styleUrls: ['./campagne-info.scss']
})
export class CampagneInfo implements OnInit {
  @Input() showTitle = true;
  @Input() showActions = true;
  @Input() compact = false;
  @Input() typeFilter?: TypeInscription;

  campagneActive$!: Observable<CampagneResponse | null>;
  campagnes$!: Observable<CampagneResponse[]>;
  loading = false;
  error: string | null = null;

  constructor(private inscriptionService: InscriptionService) {}

  ngOnInit(): void {
    this.loadCampagnes();
  }

  private loadCampagnes(): void {
    this.loading = true;
    this.error = null;

    if (this.compact) {
      // Load only active campaign for compact view
      this.campagneActive$ = this.inscriptionService.getCampagneActive();
      this.campagneActive$.subscribe({
        next: () => {
          this.loading = false;
        },
        error: (error) => {
          this.error = 'Erreur lors du chargement de la campagne active';
          this.loading = false;
          console.error('Erreur chargement campagne active:', error);
        }
      });
    } else {
      // Load all campaigns for full view
      this.campagnes$ = this.inscriptionService.getAllCampagnes();
      this.campagnes$.subscribe({
        next: () => {
          this.loading = false;
        },
        error: (error) => {
          this.error = 'Erreur lors du chargement des campagnes';
          this.loading = false;
          console.error('Erreur chargement campagnes:', error);
        }
      });
    }
  }

  onRefresh(): void {
    this.loadCampagnes();
  }

  isCampagneOpen(campagne: CampagneResponse): boolean {
    return this.inscriptionService.isCampagneOuverte(campagne);
  }

  getCampagneStatusClass(campagne: CampagneResponse): string {
    if (!campagne.active) return 'status-inactive';
    if (this.isCampagneOpen(campagne)) return 'status-open';
    return 'status-closed';
  }

  getCampagneStatusLabel(campagne: CampagneResponse): string {
    if (!campagne.active) return 'Inactive';
    if (this.isCampagneOpen(campagne)) return 'Ouverte';
    return 'Fermée';
  }

  getCampagneStatusIcon(campagne: CampagneResponse): string {
    if (!campagne.active) return 'block';
    if (this.isCampagneOpen(campagne)) return 'check_circle';
    return 'schedule';
  }

  getTypeLabel(type: TypeInscription): string {
    const labels = {
      [TypeInscription.PREMIERE]: 'Première inscription',
      [TypeInscription.REINSCRIPTION]: 'Réinscription'
    };
    return labels[type] || type;
  }

  getTypeIcon(type: TypeInscription): string {
    const icons = {
      [TypeInscription.PREMIERE]: 'person_add',
      [TypeInscription.REINSCRIPTION]: 'refresh'
    };
    return icons[type] || 'help';
  }

  getDaysRemaining(campagne: CampagneResponse): number {
    const now = new Date();
    const fermeture = new Date(campagne.dateFermeture);
    const diffTime = fermeture.getTime() - now.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  }

  getDaysRemainingLabel(campagne: CampagneResponse): string {
    const days = this.getDaysRemaining(campagne);
    
    if (days < 0) return 'Fermée';
    if (days === 0) return 'Ferme aujourd\'hui';
    if (days === 1) return '1 jour restant';
    if (days <= 7) return `${days} jours restants`;
    if (days <= 30) return `${days} jours restants`;
    return `${Math.ceil(days / 7)} semaines restantes`;
  }

  getUrgencyClass(campagne: CampagneResponse): string {
    const days = this.getDaysRemaining(campagne);
    
    if (days < 0) return 'urgency-closed';
    if (days <= 3) return 'urgency-critical';
    if (days <= 7) return 'urgency-high';
    if (days <= 14) return 'urgency-medium';
    return 'urgency-low';
  }
}