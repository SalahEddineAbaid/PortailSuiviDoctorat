import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';

import { CampagneService } from '../../../core/services/campagne.service';
import { CampagneResponse, TypeCampagne, isCampagneActive } from '../../../core/models/campagne.model';
import { TypeInscription } from '../../../core/models/inscription.model';

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

  constructor(private campagneService: CampagneService) {}

  ngOnInit(): void {
    this.loadCampagnes();
  }

  private loadCampagnes(): void {
    this.loading = true;
    this.error = null;

    if (this.compact) {
      // Load only active campaign for compact view
      this.campagneActive$ = this.campagneService.getCampagneActive();
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
      this.campagnes$ = this.campagneService.getAllCampagnes();
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
    return isCampagneActive(campagne);
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

  getTypeLabel(type: TypeCampagne | string): string {
    const labels: { [key: string]: string } = {
      'INSCRIPTION': 'Première inscription',
      'REINSCRIPTION': 'Réinscription',
      'MIXTE': 'Mixte'
    };
    return labels[type] || type;
  }

  getTypeIcon(type: TypeCampagne | string): string {
    const icons: { [key: string]: string } = {
      'INSCRIPTION': 'person_add',
      'REINSCRIPTION': 'refresh',
      'MIXTE': 'swap_horiz'
    };
    return icons[type] || 'help';
  }

  getDaysRemaining(campagne: CampagneResponse): number {
    return this.campagneService.getDaysRemaining(campagne);
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