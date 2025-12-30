import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { Observable, BehaviorSubject } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

import { InscriptionService } from '../../../core/services/inscription.service';
import { Campagne, CampagneRequest, TypeInscription } from '../../../core/models/inscription.model';
import { AlertComponent } from '../../../shared/components/alert/alert.component';

interface CampagneFormData {
  nom: string;
  anneeUniversitaire: string;
  dateOuverture: string;
  dateFermeture: string;
  typeInscription: 'PREMIERE' | 'REINSCRIPTION';
  active: boolean;
  description?: string;
}

@Component({
  selector: 'app-campagne-management',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule,
    AlertComponent
  ],
  template: `
    <div class="campagne-management">
      <div class="page-header">
        <div class="header-content">
          <h1 class="page-title">
            <i class="fas fa-calendar-alt"></i>
            Gestion des Campagnes
          </h1>
          <button 
            class="btn btn-primary"
            (click)="showCreateForm = true"
            [disabled]="showCreateForm"
          >
            <i class="fas fa-plus"></i>
            Nouvelle Campagne
          </button>
        </div>
      </div>

      <!-- Formulaire de création/modification -->
      <div class="form-section" *ngIf="showCreateForm || editingCampagne">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">
              {{ editingCampagne ? 'Modifier la Campagne' : 'Créer une Nouvelle Campagne' }}
            </h3>
            <button 
              class="btn btn-outline-secondary btn-sm"
              (click)="cancelForm()"
            >
              <i class="fas fa-times"></i>
              Annuler
            </button>
          </div>
          <div class="card-body">
            <form [formGroup]="campagneForm" (ngSubmit)="onSubmit()">
              <div class="row">
                <div class="col-md-6">
                  <div class="form-group">
                    <label for="nom" class="form-label required">Nom de la campagne</label>
                    <input
                      type="text"
                      id="nom"
                      class="form-control"
                      formControlName="nom"
                      placeholder="Ex: Inscription Doctorat 2024-2025"
                    >
                    <div class="invalid-feedback" *ngIf="campagneForm.get('nom')?.invalid && campagneForm.get('nom')?.touched">
                      Le nom de la campagne est obligatoire
                    </div>
                  </div>
                </div>
                <div class="col-md-6">
                  <div class="form-group">
                    <label for="anneeUniversitaire" class="form-label required">Année universitaire</label>
                    <input
                      type="text"
                      id="anneeUniversitaire"
                      class="form-control"
                      formControlName="anneeUniversitaire"
                      placeholder="Ex: 2024-2025"
                    >
                    <div class="invalid-feedback" *ngIf="campagneForm.get('anneeUniversitaire')?.invalid && campagneForm.get('anneeUniversitaire')?.touched">
                      L'année universitaire est obligatoire
                    </div>
                  </div>
                </div>
              </div>

              <div class="row">
                <div class="col-md-6">
                  <div class="form-group">
                    <label for="dateOuverture" class="form-label required">Date d'ouverture</label>
                    <input
                      type="datetime-local"
                      id="dateOuverture"
                      class="form-control"
                      formControlName="dateOuverture"
                    >
                    <div class="invalid-feedback" *ngIf="campagneForm.get('dateOuverture')?.invalid && campagneForm.get('dateOuverture')?.touched">
                      La date d'ouverture est obligatoire
                    </div>
                  </div>
                </div>
                <div class="col-md-6">
                  <div class="form-group">
                    <label for="dateFermeture" class="form-label required">Date de fermeture</label>
                    <input
                      type="datetime-local"
                      id="dateFermeture"
                      class="form-control"
                      formControlName="dateFermeture"
                    >
                    <div class="invalid-feedback" *ngIf="campagneForm.get('dateFermeture')?.invalid && campagneForm.get('dateFermeture')?.touched">
                      La date de fermeture est obligatoire
                    </div>
                    <div class="invalid-feedback" *ngIf="campagneForm.hasError('dateOrder')">
                      La date de fermeture doit être postérieure à la date d'ouverture
                    </div>
                  </div>
                </div>
              </div>

              <div class="row">
                <div class="col-md-6">
                  <div class="form-group">
                    <label for="typeInscription" class="form-label required">Type d'inscription</label>
                    <select
                      id="typeInscription"
                      class="form-control"
                      formControlName="typeInscription"
                    >
                      <option value="">Sélectionner un type</option>
                      <option value="PREMIERE">Première inscription</option>
                      <option value="REINSCRIPTION">Réinscription</option>
                    </select>
                    <div class="invalid-feedback" *ngIf="campagneForm.get('typeInscription')?.invalid && campagneForm.get('typeInscription')?.touched">
                      Le type d'inscription est obligatoire
                    </div>
                  </div>
                </div>
                <div class="col-md-6">
                  <div class="form-group">
                    <div class="form-check">
                      <input
                        type="checkbox"
                        id="active"
                        class="form-check-input"
                        formControlName="active"
                      >
                      <label for="active" class="form-check-label">
                        Campagne active
                      </label>
                    </div>
                    <small class="form-text text-muted">
                      Une campagne active permet aux utilisateurs de s'inscrire
                    </small>
                  </div>
                </div>
              </div>

              <div class="form-group">
                <label for="description" class="form-label">Description (optionnelle)</label>
                <textarea
                  id="description"
                  class="form-control"
                  formControlName="description"
                  rows="3"
                  placeholder="Description de la campagne..."
                ></textarea>
              </div>

              <div class="form-actions">
                <button 
                  type="submit" 
                  class="btn btn-primary"
                  [disabled]="campagneForm.invalid || isSubmitting"
                >
                  <i class="fas fa-save"></i>
                  {{ editingCampagne ? 'Modifier' : 'Créer' }}
                  <span *ngIf="isSubmitting" class="spinner-border spinner-border-sm ms-2"></span>
                </button>
                <button 
                  type="button" 
                  class="btn btn-outline-secondary"
                  (click)="cancelForm()"
                >
                  Annuler
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>

      <!-- Liste des campagnes -->
      <div class="campaigns-section">
        <div class="section-header">
          <h2 class="section-title">Campagnes Existantes</h2>
          <div class="filters">
            <select class="form-control form-control-sm" [(ngModel)]="selectedFilter" (change)="applyFilter()">
              <option value="">Toutes les campagnes</option>
              <option value="active">Campagnes actives</option>
              <option value="inactive">Campagnes inactives</option>
              <option value="PREMIERE">Premières inscriptions</option>
              <option value="REINSCRIPTION">Réinscriptions</option>
            </select>
          </div>
        </div>

        <div class="campaigns-grid" *ngIf="filteredCampagnes$ | async as campagnes">
          <div class="campaign-card" *ngFor="let campagne of campagnes">
            <div class="card">
              <div class="card-header">
                <div class="campaign-title">
                  <h4>{{ campagne.nom }}</h4>
                  <div class="campaign-badges">
                    <span 
                      class="badge"
                      [class]="campagne.active ? 'badge-success' : 'badge-secondary'"
                    >
                      {{ campagne.active ? 'Active' : 'Inactive' }}
                    </span>
                    <span 
                      class="badge badge-info"
                    >
                      {{ campagne.typeInscription === 'PREMIERE' ? 'Première inscription' : 'Réinscription' }}
                    </span>
                  </div>
                </div>
                <div class="campaign-actions">
                  <button 
                    class="btn btn-outline-primary btn-sm"
                    (click)="editCampagne(campagne)"
                    title="Modifier"
                  >
                    <i class="fas fa-edit"></i>
                  </button>
                  <button 
                    class="btn btn-outline-warning btn-sm"
                    (click)="toggleCampagneStatus(campagne)"
                    [title]="campagne.active ? 'Désactiver' : 'Activer'"
                  >
                    <i [class]="campagne.active ? 'fas fa-pause' : 'fas fa-play'"></i>
                  </button>
                  <button 
                    class="btn btn-outline-danger btn-sm"
                    (click)="deleteCampagne(campagne)"
                    title="Supprimer"
                  >
                    <i class="fas fa-trash"></i>
                  </button>
                </div>
              </div>
              <div class="card-body">
                <div class="campaign-info">
                  <div class="info-item">
                    <i class="fas fa-calendar"></i>
                    <span>{{ campagne.anneeUniversitaire }}</span>
                  </div>
                  <div class="info-item">
                    <i class="fas fa-clock"></i>
                    <span>
                      {{ formatDate(campagne.dateOuverture) }} - {{ formatDate(campagne.dateFermeture) }}
                    </span>
                  </div>
                </div>
                <p class="campaign-description" *ngIf="campagne.description">
                  {{ campagne.description }}
                </p>
              </div>
            </div>
          </div>
        </div>

        <div class="empty-state" *ngIf="(filteredCampagnes$ | async)?.length === 0">
          <i class="fas fa-calendar-times"></i>
          <h3>Aucune campagne trouvée</h3>
          <p>{{ selectedFilter ? 'Aucune campagne ne correspond aux filtres sélectionnés.' : 'Créez votre première campagne d\'inscription.' }}</p>
        </div>
      </div>

      <!-- Messages d'alerte -->
      <app-alert 
        *ngIf="alertMessage"
        [type]="alertType"
        [message]="alertMessage"
        [dismissible]="true"
        (dismissed)="clearAlert()"
      ></app-alert>
    </div>
  `,
  styleUrls: ['./campagne-management.component.scss']
})
export class CampagneManagementComponent implements OnInit {
  campagnes$ = new BehaviorSubject<Campagne[]>([]);
  filteredCampagnes$!: Observable<Campagne[]>;
  
  campagneForm!: FormGroup;
  showCreateForm = false;
  editingCampagne: Campagne | null = null;
  isSubmitting = false;
  selectedFilter = '';

  alertMessage = '';
  alertType: 'success' | 'error' | 'warning' | 'info' = 'info';

  constructor(
    private fb: FormBuilder,
    private inscriptionService: InscriptionService,
    private route: ActivatedRoute
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadCampagnes();
    this.setupFilters();
    this.checkQueryParams();
  }

  private initForm(): void {
    this.campagneForm = this.fb.group({
      nom: ['', [Validators.required, Validators.minLength(3)]],
      anneeUniversitaire: ['', [Validators.required, Validators.pattern(/^\d{4}-\d{4}$/)]],
      dateOuverture: ['', Validators.required],
      dateFermeture: ['', Validators.required],
      typeInscription: ['', Validators.required],
      active: [true],
      description: ['']
    }, { validators: this.dateOrderValidator });
  }

  private dateOrderValidator(form: FormGroup) {
    const dateOuverture = form.get('dateOuverture')?.value;
    const dateFermeture = form.get('dateFermeture')?.value;
    
    if (dateOuverture && dateFermeture && new Date(dateOuverture) >= new Date(dateFermeture)) {
      return { dateOrder: true };
    }
    return null;
  }

  private loadCampagnes(): void {
    this.inscriptionService.getCampagnes().subscribe({
      next: (campagnes) => {
        this.campagnes$.next(campagnes);
      },
      error: (error) => {
        console.error('Erreur lors du chargement des campagnes:', error);
        this.showAlert('Erreur lors du chargement des campagnes', 'error');
      }
    });
  }

  private setupFilters(): void {
    this.filteredCampagnes$ = this.campagnes$.pipe(
      map(campagnes => this.filterCampagnes(campagnes))
    );
  }

  private filterCampagnes(campagnes: Campagne[]): Campagne[] {
    if (!this.selectedFilter) return campagnes;

    return campagnes.filter(campagne => {
      switch (this.selectedFilter) {
        case 'active':
          return campagne.active;
        case 'inactive':
          return !campagne.active;
        case 'PREMIERE':
        case 'REINSCRIPTION':
          return campagne.typeInscription === this.selectedFilter;
        default:
          return true;
      }
    });
  }

  private checkQueryParams(): void {
    this.route.queryParams.subscribe(params => {
      if (params['action'] === 'create') {
        this.showCreateForm = true;
      }
    });
  }

  applyFilter(): void {
    this.filteredCampagnes$ = this.campagnes$.pipe(
      map(campagnes => this.filterCampagnes(campagnes))
    );
  }

  onSubmit(): void {
    if (this.campagneForm.valid) {
      this.isSubmitting = true;
      const formData: CampagneFormData = this.campagneForm.value;
      
      const campagneRequest: CampagneRequest = {
        nom: formData.nom,
        anneeUniversitaire: formData.anneeUniversitaire,
        dateOuverture: new Date(formData.dateOuverture),
        dateFermeture: new Date(formData.dateFermeture),
        typeInscription: formData.typeInscription as TypeInscription,
        active: formData.active,
        description: formData.description
      };

      const operation = this.editingCampagne 
        ? this.inscriptionService.updateCampagne(this.editingCampagne.id, campagneRequest)
        : this.inscriptionService.createCampagne(campagneRequest);

      operation.subscribe({
        next: (campagne) => {
          const message = this.editingCampagne 
            ? 'Campagne modifiée avec succès'
            : 'Campagne créée avec succès';
          
          this.showAlert(message, 'success');
          this.cancelForm();
          this.loadCampagnes();
        },
        error: (error) => {
          console.error('Erreur lors de la sauvegarde:', error);
          this.showAlert('Erreur lors de la sauvegarde de la campagne', 'error');
        },
        complete: () => {
          this.isSubmitting = false;
        }
      });
    }
  }

  editCampagne(campagne: Campagne): void {
    this.editingCampagne = campagne;
    this.showCreateForm = true;
    
    // Pré-remplir le formulaire
    this.campagneForm.patchValue({
      nom: campagne.nom,
      anneeUniversitaire: campagne.anneeUniversitaire,
      dateOuverture: this.formatDateForInput(campagne.dateOuverture),
      dateFermeture: this.formatDateForInput(campagne.dateFermeture),
      typeInscription: campagne.typeInscription,
      active: campagne.active,
      description: campagne.description || ''
    });
  }

  toggleCampagneStatus(campagne: Campagne): void {
    const newStatus = !campagne.active;
    const updateRequest: Partial<CampagneRequest> = { active: newStatus };

    this.inscriptionService.updateCampagne(campagne.id, updateRequest).subscribe({
      next: () => {
        const message = newStatus 
          ? `Campagne "${campagne.nom}" activée`
          : `Campagne "${campagne.nom}" désactivée`;
        
        this.showAlert(message, 'success');
        this.loadCampagnes();
      },
      error: (error) => {
        console.error('Erreur lors de la modification du statut:', error);
        this.showAlert('Erreur lors de la modification du statut', 'error');
      }
    });
  }

  deleteCampagne(campagne: Campagne): void {
    if (confirm(`Êtes-vous sûr de vouloir supprimer la campagne "${campagne.nom}" ?`)) {
      this.inscriptionService.deleteCampagne(campagne.id).subscribe({
        next: () => {
          this.showAlert(`Campagne "${campagne.nom}" supprimée`, 'success');
          this.loadCampagnes();
        },
        error: (error) => {
          console.error('Erreur lors de la suppression:', error);
          this.showAlert('Erreur lors de la suppression de la campagne', 'error');
        }
      });
    }
  }

  cancelForm(): void {
    this.showCreateForm = false;
    this.editingCampagne = null;
    this.campagneForm.reset();
    this.initForm();
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  private formatDateForInput(date: Date): string {
    const d = new Date(date);
    return d.toISOString().slice(0, 16);
  }

  private showAlert(message: string, type: 'success' | 'error' | 'warning' | 'info'): void {
    this.alertMessage = message;
    this.alertType = type;
    
    // Auto-dismiss success messages
    if (type === 'success') {
      setTimeout(() => this.clearAlert(), 5000);
    }
  }

  clearAlert(): void {
    this.alertMessage = '';
  }
}