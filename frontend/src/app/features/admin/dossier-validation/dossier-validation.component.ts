import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { Observable, BehaviorSubject, combineLatest } from 'rxjs';
import { map, startWith, debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { InscriptionService } from '../../../core/services/inscription.service';
import { SoutenanceService } from '../../../core/services/soutenance.service';
import { InscriptionResponse, ValidationRequest } from '../../../core/models/inscription.model';
import { SoutenanceResponse } from '../../../core/models/soutenance.model';
import { AlertComponent } from '../../../shared/components/alert/alert.component';

interface DossierValidation {
  id: number;
  type: 'inscription' | 'soutenance';
  doctorant: {
    nom: string;
    prenom: string;
    email: string;
  };
  directeur: {
    nom: string;
    prenom: string;
  };
  titre: string;
  statut: string;
  dateCreation: Date;
  dateSoumission?: Date;
  priorite: 'haute' | 'normale' | 'basse';
  documentsManquants?: string[];
}

@Component({
  selector: 'app-dossier-validation',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule,
    AlertComponent
  ],
  template: `
    <div class="dossier-validation">
      <div class="page-header">
        <div class="header-content">
          <h1 class="page-title">
            <i class="fas fa-check-circle"></i>
            Validation des Dossiers
          </h1>
          <div class="header-stats">
            <div class="stat-item">
              <span class="stat-number">{{ (filteredDossiers$ | async)?.length || 0 }}</span>
              <span class="stat-label">Dossiers</span>
            </div>
            <div class="stat-item urgent">
              <span class="stat-number">{{ getUrgentCount() }}</span>
              <span class="stat-label">Urgents</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Filtres et recherche -->
      <div class="filters-section">
        <div class="card">
          <div class="card-body">
            <div class="filters-grid">
              <div class="filter-group">
                <label class="filter-label">Type de dossier</label>
                <select 
                  class="form-control form-control-sm"
                  [(ngModel)]="filters.type"
                  (change)="applyFilters()"
                >
                  <option value="">Tous les types</option>
                  <option value="inscription">Inscriptions</option>
                  <option value="soutenance">Soutenances</option>
                </select>
              </div>

              <div class="filter-group">
                <label class="filter-label">Priorité</label>
                <select 
                  class="form-control form-control-sm"
                  [(ngModel)]="filters.priorite"
                  (change)="applyFilters()"
                >
                  <option value="">Toutes les priorités</option>
                  <option value="haute">Haute priorité</option>
                  <option value="normale">Priorité normale</option>
                  <option value="basse">Basse priorité</option>
                </select>
              </div>

              <div class="filter-group">
                <label class="filter-label">Statut</label>
                <select 
                  class="form-control form-control-sm"
                  [(ngModel)]="filters.statut"
                  (change)="applyFilters()"
                >
                  <option value="">Tous les statuts</option>
                  <option value="SOUMISE">Soumis</option>
                  <option value="EN_COURS_VALIDATION">En cours</option>
                  <option value="DOCUMENTS_MANQUANTS">Documents manquants</option>
                </select>
              </div>

              <div class="filter-group search-group">
                <label class="filter-label">Recherche</label>
                <div class="search-input">
                  <input
                    type="text"
                    class="form-control form-control-sm"
                    placeholder="Nom, prénom, sujet..."
                    [(ngModel)]="filters.search"
                    (input)="onSearchChange($event)"
                  >
                  <i class="fas fa-search search-icon"></i>
                </div>
              </div>

              <div class="filter-actions">
                <button 
                  class="btn btn-outline-secondary btn-sm"
                  (click)="clearFilters()"
                >
                  <i class="fas fa-times"></i>
                  Effacer
                </button>
                <button 
                  class="btn btn-primary btn-sm"
                  (click)="exportDossiers()"
                >
                  <i class="fas fa-download"></i>
                  Exporter
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Liste des dossiers -->
      <div class="dossiers-section">
        <div class="dossiers-list" *ngIf="filteredDossiers$ | async as dossiers">
          <div 
            class="dossier-card"
            *ngFor="let dossier of dossiers; trackBy: trackByDossier"
            [class.urgent]="dossier.priorite === 'haute'"
          >
            <div class="card">
              <div class="card-header">
                <div class="dossier-info">
                  <div class="dossier-title">
                    <h4>{{ dossier.titre }}</h4>
                    <div class="dossier-badges">
                      <span 
                        class="badge"
                        [class]="'badge-' + getTypeBadgeClass(dossier.type)"
                      >
                        {{ dossier.type === 'inscription' ? 'Inscription' : 'Soutenance' }}
                      </span>
                      <span 
                        class="badge"
                        [class]="'badge-' + getPrioriteBadgeClass(dossier.priorite)"
                      >
                        {{ getPrioriteLabel(dossier.priorite) }}
                      </span>
                      <span 
                        class="badge"
                        [class]="'badge-' + getStatutBadgeClass(dossier.statut)"
                      >
                        {{ getStatutLabel(dossier.statut) }}
                      </span>
                    </div>
                  </div>
                  <div class="dossier-meta">
                    <div class="meta-item">
                      <i class="fas fa-user"></i>
                      <span>{{ dossier.doctorant.prenom }} {{ dossier.doctorant.nom }}</span>
                    </div>
                    <div class="meta-item">
                      <i class="fas fa-user-tie"></i>
                      <span>Dir. {{ dossier.directeur.prenom }} {{ dossier.directeur.nom }}</span>
                    </div>
                    <div class="meta-item">
                      <i class="fas fa-calendar"></i>
                      <span>{{ formatDate(dossier.dateCreation) }}</span>
                    </div>
                  </div>
                </div>
                <div class="dossier-actions">
                  <button 
                    class="btn btn-outline-primary btn-sm"
                    (click)="consulterDossier(dossier)"
                    title="Consulter le dossier"
                  >
                    <i class="fas fa-eye"></i>
                    Consulter
                  </button>
                  <button 
                    class="btn btn-outline-success btn-sm"
                    (click)="openValidationModal(dossier, true)"
                    title="Valider le dossier"
                  >
                    <i class="fas fa-check"></i>
                    Valider
                  </button>
                  <button 
                    class="btn btn-outline-danger btn-sm"
                    (click)="openValidationModal(dossier, false)"
                    title="Rejeter le dossier"
                  >
                    <i class="fas fa-times"></i>
                    Rejeter
                  </button>
                </div>
              </div>
              
              <div class="card-body" *ngIf="dossier.documentsManquants && dossier.documentsManquants.length > 0">
                <div class="documents-manquants">
                  <h6 class="documents-title">
                    <i class="fas fa-exclamation-triangle text-warning"></i>
                    Documents manquants
                  </h6>
                  <ul class="documents-list">
                    <li *ngFor="let doc of dossier.documentsManquants">{{ doc }}</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="empty-state" *ngIf="(filteredDossiers$ | async)?.length === 0">
          <i class="fas fa-inbox"></i>
          <h3>Aucun dossier à valider</h3>
          <p>{{ hasActiveFilters() ? 'Aucun dossier ne correspond aux filtres sélectionnés.' : 'Tous les dossiers ont été traités.' }}</p>
        </div>
      </div>

      <!-- Modal de validation -->
      <div class="modal fade" [class.show]="showValidationModal" [style.display]="showValidationModal ? 'block' : 'none'">
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">
                {{ validationForm.get('valide')?.value ? 'Valider' : 'Rejeter' }} le dossier
              </h5>
              <button 
                type="button" 
                class="btn-close"
                (click)="closeValidationModal()"
              ></button>
            </div>
            <div class="modal-body">
              <form [formGroup]="validationForm">
                <div class="dossier-summary" *ngIf="selectedDossier">
                  <h6>{{ selectedDossier.titre }}</h6>
                  <p class="text-muted">
                    {{ selectedDossier.doctorant.prenom }} {{ selectedDossier.doctorant.nom }}
                    - {{ selectedDossier.type === 'inscription' ? 'Inscription' : 'Soutenance' }}
                  </p>
                </div>

                <div class="form-group">
                  <label for="commentaire" class="form-label required">
                    {{ validationForm.get('valide')?.value ? 'Commentaire de validation' : 'Motif de rejet' }}
                  </label>
                  <textarea
                    id="commentaire"
                    class="form-control"
                    formControlName="commentaire"
                    rows="4"
                    [placeholder]="validationForm.get('valide')?.value ? 'Commentaire sur la validation...' : 'Précisez les raisons du rejet...'"
                  ></textarea>
                  <div class="invalid-feedback" *ngIf="validationForm.get('commentaire')?.invalid && validationForm.get('commentaire')?.touched">
                    Le commentaire est obligatoire
                  </div>
                </div>

                <div class="form-check" *ngIf="!validationForm.get('valide')?.value">
                  <input
                    type="checkbox"
                    id="notifierDoctorant"
                    class="form-check-input"
                    [(ngModel)]="notifierDoctorant"
                    [ngModelOptions]="{standalone: true}"
                  >
                  <label for="notifierDoctorant" class="form-check-label">
                    Notifier le doctorant par email
                  </label>
                </div>
              </form>
            </div>
            <div class="modal-footer">
              <button 
                type="button" 
                class="btn btn-outline-secondary"
                (click)="closeValidationModal()"
              >
                Annuler
              </button>
              <button 
                type="button" 
                class="btn"
                [class]="validationForm.get('valide')?.value ? 'btn-success' : 'btn-danger'"
                (click)="submitValidation()"
                [disabled]="validationForm.invalid || isSubmitting"
              >
                <i [class]="validationForm.get('valide')?.value ? 'fas fa-check' : 'fas fa-times'"></i>
                {{ validationForm.get('valide')?.value ? 'Valider' : 'Rejeter' }}
                <span *ngIf="isSubmitting" class="spinner-border spinner-border-sm ms-2"></span>
              </button>
            </div>
          </div>
        </div>
      </div>
      <div class="modal-backdrop fade" [class.show]="showValidationModal" *ngIf="showValidationModal"></div>

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
  styleUrls: ['./dossier-validation.component.scss']
})
export class DossierValidationComponent implements OnInit {
  dossiers$ = new BehaviorSubject<DossierValidation[]>([]);
  filteredDossiers$!: Observable<DossierValidation[]>;
  
  validationForm!: FormGroup;
  showValidationModal = false;
  selectedDossier: DossierValidation | null = null;
  isSubmitting = false;
  notifierDoctorant = true;

  filters = {
    type: '',
    priorite: '',
    statut: '',
    search: ''
  };

  alertMessage = '';
  alertType: 'success' | 'error' | 'warning' | 'info' = 'info';

  constructor(
    private fb: FormBuilder,
    private inscriptionService: InscriptionService,
    private soutenanceService: SoutenanceService,
    private router: Router
  ) {
    this.initValidationForm();
  }

  ngOnInit(): void {
    this.loadDossiers();
    this.setupFilters();
  }

  private initValidationForm(): void {
    this.validationForm = this.fb.group({
      commentaire: ['', [Validators.required, Validators.minLength(10)]],
      valide: [true]
    });
  }

  private loadDossiers(): void {
    // Charger les inscriptions en attente
    const inscriptions$ = this.inscriptionService.getInscriptionsEnAttenteAdmin();
    
    // Charger les soutenances en attente
    const soutenances$ = this.soutenanceService.getSoutenancesEnAttenteAdmin();

    combineLatest([inscriptions$, soutenances$]).subscribe({
      next: ([inscriptions, soutenances]) => {
        const dossiers: DossierValidation[] = [
          ...this.mapInscriptionsToDossiers(inscriptions),
          ...this.mapSoutenancesToDossiers(soutenances)
        ];
        
        // Trier par priorité et date
        dossiers.sort((a, b) => {
          const priorityOrder = { 'haute': 0, 'normale': 1, 'basse': 2 };
          const priorityDiff = priorityOrder[a.priorite] - priorityOrder[b.priorite];
          if (priorityDiff !== 0) return priorityDiff;
          
          return new Date(b.dateCreation).getTime() - new Date(a.dateCreation).getTime();
        });

        this.dossiers$.next(dossiers);
      },
      error: (error) => {
        console.error('Erreur lors du chargement des dossiers:', error);
        this.showAlert('Erreur lors du chargement des dossiers', 'error');
      }
    });
  }

  private mapInscriptionsToDossiers(inscriptions: InscriptionResponse[]): DossierValidation[] {
    return inscriptions.map(inscription => ({
      id: inscription.id,
      type: 'inscription' as const,
      doctorant: {
        nom: inscription.doctorant.LastName,
        prenom: inscription.doctorant.FirstName,
        email: inscription.doctorant.email
      },
      directeur: {
        nom: inscription.directeur.LastName,
        prenom: inscription.directeur.FirstName
      },
      titre: inscription.sujetThese,
      statut: inscription.statut,
      dateCreation: inscription.dateInscription,
      priorite: this.calculatePriority(inscription.dateInscription, 'inscription'),
      documentsManquants: this.getDocumentsManquants(inscription.documents || [])
    }));
  }

  private mapSoutenancesToDossiers(soutenances: SoutenanceResponse[]): DossierValidation[] {
    return soutenances.map(soutenance => {
      // Use current date as fallback for dateCreation since it's not in SoutenanceResponse
      const dateCreation = new Date();
      return {
        id: soutenance.id,
        type: 'soutenance' as const,
        doctorant: {
          nom: soutenance.doctorant.LastName,
          prenom: soutenance.doctorant.FirstName,
          email: soutenance.doctorant.email
        },
        directeur: {
          nom: soutenance.directeur.LastName,
          prenom: soutenance.directeur.FirstName
        },
        titre: soutenance.titrethese,
        statut: soutenance.statut,
        dateCreation: dateCreation,
        priorite: this.calculatePriority(dateCreation, 'soutenance'),
        documentsManquants: this.getDocumentsManquants(soutenance.documents || [])
      };
    });
  }

  private calculatePriority(dateCreation: Date, type: 'inscription' | 'soutenance'): 'haute' | 'normale' | 'basse' {
    const now = new Date();
    const daysDiff = Math.floor((now.getTime() - new Date(dateCreation).getTime()) / (1000 * 60 * 60 * 24));
    
    if (type === 'soutenance') {
      if (daysDiff > 14) return 'haute';
      if (daysDiff > 7) return 'normale';
      return 'basse';
    } else {
      if (daysDiff > 30) return 'haute';
      if (daysDiff > 15) return 'normale';
      return 'basse';
    }
  }

  private getDocumentsManquants(documents: any[]): string[] {
    // Logique pour déterminer les documents manquants
    // Cette logique devrait être basée sur les règles métier
    const documentsManquants: string[] = [];
    
    // Exemple de vérification (à adapter selon les besoins)
    const requiredDocs = ['CV', 'Diplôme Master', 'Carte d\'identité'];
    const presentDocs = documents.map(doc => doc.type);
    
    requiredDocs.forEach(docType => {
      if (!presentDocs.includes(docType)) {
        documentsManquants.push(docType);
      }
    });
    
    return documentsManquants;
  }

  private setupFilters(): void {
    this.filteredDossiers$ = combineLatest([
      this.dossiers$,
      this.getSearchObservable()
    ]).pipe(
      map(([dossiers, searchTerm]) => this.filterDossiers(dossiers, searchTerm))
    );
  }

  private getSearchObservable(): Observable<string> {
    return new BehaviorSubject(this.filters.search).pipe(
      debounceTime(300),
      distinctUntilChanged()
    );
  }

  private filterDossiers(dossiers: DossierValidation[], searchTerm: string): DossierValidation[] {
    return dossiers.filter(dossier => {
      // Filtre par type
      if (this.filters.type && dossier.type !== this.filters.type) {
        return false;
      }

      // Filtre par priorité
      if (this.filters.priorite && dossier.priorite !== this.filters.priorite) {
        return false;
      }

      // Filtre par statut
      if (this.filters.statut && dossier.statut !== this.filters.statut) {
        return false;
      }

      // Filtre par recherche
      if (searchTerm) {
        const searchLower = searchTerm.toLowerCase();
        const matchesSearch = 
          dossier.doctorant.nom.toLowerCase().includes(searchLower) ||
          dossier.doctorant.prenom.toLowerCase().includes(searchLower) ||
          dossier.directeur.nom.toLowerCase().includes(searchLower) ||
          dossier.directeur.prenom.toLowerCase().includes(searchLower) ||
          dossier.titre.toLowerCase().includes(searchLower);
        
        if (!matchesSearch) return false;
      }

      return true;
    });
  }

  applyFilters(): void {
    this.setupFilters();
  }

  onSearchChange(event: any): void {
    this.filters.search = event.target.value;
    this.setupFilters();
  }

  clearFilters(): void {
    this.filters = {
      type: '',
      priorite: '',
      statut: '',
      search: ''
    };
    this.setupFilters();
  }

  hasActiveFilters(): boolean {
    return !!(this.filters.type || this.filters.priorite || this.filters.statut || this.filters.search);
  }

  getUrgentCount(): number {
    return this.dossiers$.value.filter(d => d.priorite === 'haute').length;
  }

  consulterDossier(dossier: DossierValidation): void {
    const route = dossier.type === 'inscription' 
      ? `/inscription/${dossier.id}`
      : `/soutenance/${dossier.id}`;
    
    this.router.navigate([route]);
  }

  openValidationModal(dossier: DossierValidation, valide: boolean): void {
    this.selectedDossier = dossier;
    this.validationForm.patchValue({
      valide: valide,
      commentaire: ''
    });
    this.showValidationModal = true;
  }

  closeValidationModal(): void {
    this.showValidationModal = false;
    this.selectedDossier = null;
    this.validationForm.reset();
    this.notifierDoctorant = true;
  }

  submitValidation(): void {
    if (this.validationForm.valid && this.selectedDossier) {
      this.isSubmitting = true;
      
      const validationRequest: ValidationRequest = {
        commentaire: this.validationForm.get('commentaire')?.value,
        valide: this.validationForm.get('valide')?.value
      };

      if (this.selectedDossier.type === 'inscription') {
        this.inscriptionService.validerParAdmin(this.selectedDossier.id, validationRequest).subscribe({
          next: (response) => {
            const action = validationRequest.valide ? 'validé' : 'rejeté';
            this.showAlert(`Dossier ${action} avec succès`, 'success');
            this.closeValidationModal();
            this.loadDossiers();
          },
          error: (error: any) => {
            console.error('Erreur lors de la validation:', error);
            this.showAlert('Erreur lors de la validation du dossier', 'error');
          },
          complete: () => {
            this.isSubmitting = false;
          }
        });
      } else {
        this.soutenanceService.validerParAdmin(this.selectedDossier.id, validationRequest).subscribe({
          next: (response) => {
            const action = validationRequest.valide ? 'validé' : 'rejeté';
            this.showAlert(`Dossier ${action} avec succès`, 'success');
            this.closeValidationModal();
            this.loadDossiers();
          },
          error: (error: any) => {
            console.error('Erreur lors de la validation:', error);
            this.showAlert('Erreur lors de la validation du dossier', 'error');
          },
          complete: () => {
            this.isSubmitting = false;
          }
        });
      }
    }
  }

  exportDossiers(): void {
    // Logique d'export des dossiers
    console.log('Export des dossiers en cours...');
    this.showAlert('Export en cours de préparation', 'info');
  }

  trackByDossier(index: number, dossier: DossierValidation): number {
    return dossier.id;
  }

  // Méthodes utilitaires pour l'affichage
  getTypeBadgeClass(type: string): string {
    return type === 'inscription' ? 'primary' : 'info';
  }

  getPrioriteBadgeClass(priorite: string): string {
    const classes = {
      'haute': 'danger',
      'normale': 'warning',
      'basse': 'secondary'
    };
    return classes[priorite as keyof typeof classes] || 'secondary';
  }

  getStatutBadgeClass(statut: string): string {
    const classes = {
      'SOUMISE': 'info',
      'EN_COURS_VALIDATION': 'warning',
      'VALIDEE': 'success',
      'REJETEE': 'danger'
    };
    return classes[statut as keyof typeof classes] || 'secondary';
  }

  getPrioriteLabel(priorite: string): string {
    const labels = {
      'haute': 'Urgent',
      'normale': 'Normal',
      'basse': 'Faible'
    };
    return labels[priorite as keyof typeof labels] || priorite;
  }

  getStatutLabel(statut: string): string {
    const labels = {
      'SOUMISE': 'Soumis',
      'EN_COURS_VALIDATION': 'En cours',
      'VALIDEE': 'Validé',
      'REJETEE': 'Rejeté'
    };
    return labels[statut as keyof typeof labels] || statut;
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  private showAlert(message: string, type: 'success' | 'error' | 'warning' | 'info'): void {
    this.alertMessage = message;
    this.alertType = type;
    
    if (type === 'success') {
      setTimeout(() => this.clearAlert(), 5000);
    }
  }

  clearAlert(): void {
    this.alertMessage = '';
  }
}