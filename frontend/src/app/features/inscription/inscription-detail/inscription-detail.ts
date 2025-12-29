import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';

import { InscriptionService } from '../../../core/services/inscription.service';
import { DocumentsManager } from '../documents-manager/documents-manager';
import { InscriptionResponse, InscriptionStatus } from '../../../core/models/inscription.model';

@Component({
  selector: 'app-inscription-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, DocumentsManager],
  templateUrl: './inscription-detail.html',
  styleUrls: ['./inscription-detail.scss']
})
export class InscriptionDetail implements OnInit {
  inscription$!: Observable<InscriptionResponse>;
  loading = false;
  error: string | null = null;
  inscriptionId!: number;

  constructor(
    private inscriptionService: InscriptionService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.inscriptionId = +params['id'];
      if (this.inscriptionId) {
        this.loadInscription();
      } else {
        this.error = 'ID d\'inscription invalide';
      }
    });
  }

  private loadInscription(): void {
    this.loading = true;
    this.error = null;

    this.inscription$ = this.inscriptionService.getInscription(this.inscriptionId);
    
    this.inscription$.subscribe({
      next: () => {
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Erreur lors du chargement de l\'inscription';
        this.loading = false;
        console.error('Erreur chargement inscription:', error);
      }
    });
  }

  onBack(): void {
    this.router.navigate(['/inscription']);
  }

  onEdit(inscription: InscriptionResponse): void {
    if (this.canEdit(inscription)) {
      this.router.navigate(['/inscription', inscription.id, 'edit']);
    }
  }

  onSubmit(inscription: InscriptionResponse): void {
    if (!this.canSubmit(inscription)) return;

    if (confirm('Êtes-vous sûr de vouloir soumettre cette inscription ? Vous ne pourrez plus la modifier après soumission.')) {
      this.inscriptionService.submitInscription(inscription.id).subscribe({
        next: () => {
          this.loadInscription(); // Refresh the data
        },
        error: (error) => {
          this.error = 'Erreur lors de la soumission de l\'inscription';
          console.error('Erreur soumission inscription:', error);
        }
      });
    }
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

  getProgressSteps(inscription: InscriptionResponse): Array<{label: string, completed: boolean, current: boolean}> {
    const steps = [
      { label: 'Création', completed: true, current: false },
      { label: 'Soumission', completed: false, current: false },
      { label: 'Avis Directeur', completed: false, current: false },
      { label: 'Validation Admin', completed: false, current: false },
      { label: 'Validée', completed: false, current: false }
    ];

    switch (inscription.statut) {
      case InscriptionStatus.BROUILLON:
        steps[0].current = true;
        break;
      case InscriptionStatus.SOUMISE:
        steps[1].completed = true;
        steps[1].current = true;
        break;
      case InscriptionStatus.EN_COURS_VALIDATION:
        steps[1].completed = true;
        if (inscription.avisDirecteur) {
          steps[2].completed = true;
          steps[3].current = true;
        } else {
          steps[2].current = true;
        }
        break;
      case InscriptionStatus.VALIDEE:
        steps[1].completed = true;
        steps[2].completed = true;
        steps[3].completed = true;
        steps[4].completed = true;
        steps[4].current = true;
        break;
      case InscriptionStatus.REJETEE:
        steps[1].completed = true;
        if (inscription.avisDirecteur) {
          steps[2].completed = true;
        }
        if (inscription.validationAdmin) {
          steps[3].completed = true;
        }
        break;
    }

    return steps;
  }
}