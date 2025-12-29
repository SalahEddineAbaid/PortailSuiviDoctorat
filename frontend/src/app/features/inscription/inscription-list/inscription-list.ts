import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Observable } from 'rxjs';

import { InscriptionService } from '../../../core/services/inscription.service';
import { InscriptionResponse, InscriptionStatus } from '../../../core/models/inscription.model';

@Component({
  selector: 'app-inscription-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './inscription-list.html',
  styleUrls: ['./inscription-list.scss']
})
export class InscriptionList implements OnInit {
  @Input() showTitle = true;
  @Input() showActions = true;
  @Input() maxItems?: number;
  @Input() filterStatus?: InscriptionStatus;

  inscriptions$!: Observable<InscriptionResponse[]>;
  loading = false;
  error: string | null = null;

  constructor(private inscriptionService: InscriptionService) {}

  ngOnInit(): void {
    this.loadInscriptions();
  }

  private loadInscriptions(): void {
    this.loading = true;
    this.error = null;

    this.inscriptions$ = this.inscriptionService.getMyInscriptions();
    
    this.inscriptions$.subscribe({
      next: () => {
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Erreur lors du chargement des inscriptions';
        this.loading = false;
        console.error('Erreur chargement inscriptions:', error);
      }
    });
  }

  onRefresh(): void {
    this.loadInscriptions();
  }

  getStatusClass(status: InscriptionStatus): string {
    const statusClasses: { [key: string]: string } = {
      [InscriptionStatus.BROUILLON]: 'status-draft',
      [InscriptionStatus.SOUMISE]: 'status-submitted',
      [InscriptionStatus.EN_COURS_VALIDATION]: 'status-pending',
      [InscriptionStatus.VALIDEE]: 'status-approved',
      [InscriptionStatus.REJETEE]: 'status-rejected'
    };
    return statusClasses[status] || 'status-default';
  }

  getStatusLabel(status: InscriptionStatus): string {
    return this.inscriptionService.getStatusLabel(status);
  }

  getStatusIcon(status: InscriptionStatus): string {
    const statusIcons: { [key: string]: string } = {
      [InscriptionStatus.BROUILLON]: 'edit',
      [InscriptionStatus.SOUMISE]: 'send',
      [InscriptionStatus.EN_COURS_VALIDATION]: 'hourglass_empty',
      [InscriptionStatus.VALIDEE]: 'check_circle',
      [InscriptionStatus.REJETEE]: 'cancel'
    };
    return statusIcons[status] || 'help';
  }

  canEdit(inscription: InscriptionResponse): boolean {
    return inscription.statut === InscriptionStatus.BROUILLON;
  }

  canSubmit(inscription: InscriptionResponse): boolean {
    return inscription.statut === InscriptionStatus.BROUILLON;
  }

  onSubmitInscription(inscription: InscriptionResponse): void {
    if (!this.canSubmit(inscription)) return;

    if (confirm('Êtes-vous sûr de vouloir soumettre cette inscription ? Vous ne pourrez plus la modifier après soumission.')) {
      this.inscriptionService.submitInscription(inscription.id).subscribe({
        next: () => {
          this.loadInscriptions(); // Refresh the list
        },
        error: (error) => {
          this.error = 'Erreur lors de la soumission de l\'inscription';
          console.error('Erreur soumission inscription:', error);
        }
      });
    }
  }

  getProgressPercentage(inscription: InscriptionResponse): number {
    let progress = 0;
    
    // Basic info (40%)
    if (inscription.sujetThese && inscription.laboratoire && inscription.specialite) {
      progress += 40;
    }
    
    // Director approval (30%)
    if (inscription.avisDirecteur) {
      progress += 30;
    }
    
    // Admin validation (30%)
    if (inscription.validationAdmin) {
      progress += 30;
    }
    
    return Math.min(progress, 100);
  }

  getProgressLabel(inscription: InscriptionResponse): string {
    const percentage = this.getProgressPercentage(inscription);
    
    if (percentage === 100) return 'Complète';
    if (percentage >= 70) return 'Presque terminée';
    if (percentage >= 40) return 'En cours';
    return 'Incomplète';
  }
}