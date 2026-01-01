import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { InscriptionService } from '../../../core/services/inscription.service';
import { InscriptionResponse, StatutInscription } from '../../../core/models/inscription.model';

@Component({
  selector: 'app-dossier-validation',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="dossier-validation">
      <h1>Validation des Dossiers</h1>
      
      <div *ngIf="isLoading" class="loading">Chargement...</div>
      <div *ngIf="errorMessage" class="error">{{ errorMessage }}</div>
      <div *ngIf="successMessage" class="success">{{ successMessage }}</div>
      
      <div class="dossiers-list" *ngIf="!isLoading">
        <div *ngFor="let dossier of dossiers" class="dossier-item">
          <h3>Dossier #{{ dossier.id }}</h3>
          <p>Statut: {{ dossier.statut }}</p>
          <p>Sujet: {{ dossier.sujetThese }}</p>
        </div>
        <p *ngIf="dossiers.length === 0">Aucun dossier en attente de validation</p>
      </div>
    </div>
  `,
  styles: [`
    .dossier-validation { padding: 20px; }
    .dossier-item { border: 1px solid #ddd; padding: 15px; margin: 10px 0; border-radius: 8px; }
    .loading, .error, .success { padding: 10px; margin: 10px 0; }
    .error { background: #fee; color: #c00; }
    .success { background: #efe; color: #0a0; }
  `]
})
export class DossierValidationComponent implements OnInit {
  dossiers: InscriptionResponse[] = [];
  isLoading = true;
  errorMessage = '';
  successMessage = '';
  selectedDossier: InscriptionResponse | null = null;
  validationForm: FormGroup;

  constructor(
    private inscriptionService: InscriptionService,
    private fb: FormBuilder
  ) {
    this.validationForm = this.fb.group({
      commentaire: [''],
      approuve: [true]
    });
  }

  ngOnInit(): void {
    this.loadDossiers();
  }

  loadDossiers(): void {
    this.isLoading = true;
    this.inscriptionService.getInscriptionsEnAttenteAdmin().subscribe({
      next: (dossiers: InscriptionResponse[]) => {
        this.dossiers = dossiers;
        this.isLoading = false;
      },
      error: (error: any) => {
        this.errorMessage = 'Erreur lors du chargement des dossiers';
        this.isLoading = false;
      }
    });
  }
}
