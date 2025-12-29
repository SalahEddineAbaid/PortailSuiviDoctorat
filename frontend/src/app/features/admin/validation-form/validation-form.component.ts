import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { ValidationRequest } from '../../../core/models/inscription.model';

interface DossierInfo {
  id: number;
  type: 'inscription' | 'soutenance';
  titre: string;
  doctorant: {
    nom: string;
    prenom: string;
    email: string;
  };
  directeur: {
    nom: string;
    prenom: string;
  };
  statut: string;
  dateCreation: Date;
  documentsManquants?: string[];
}

@Component({
  selector: 'app-validation-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule
  ],
  template: `
    <div class="validation-form">
      <div class="dossier-summary" *ngIf="dossier">
        <div class="summary-header">
          <h5 class="dossier-title">{{ dossier.titre }}</h5>
          <div class="dossier-badges">
            <span 
              class="badge"
              [class]="'badge-' + getTypeBadgeClass(dossier.type)"
            >
              {{ dossier.type === 'inscription' ? 'Inscription' : 'Soutenance' }}
            </span>
            <span 
              class="badge"
              [class]="'badge-' + getStatutBadgeClass(dossier.statut)"
            >
              {{ getStatutLabel(dossier.statut) }}
            </span>
          </div>
        </div>
        
        <div class="summary-details">
          <div class="detail-row">
            <span class="detail-label">Doctorant:</span>
            <span class="detail-value">{{ dossier.doctorant.prenom }} {{ dossier.doctorant.nom }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Directeur:</span>
            <span class="detail-value">{{ dossier.directeur.prenom }} {{ dossier.directeur.nom }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Date de création:</span>
            <span class="detail-value">{{ formatDate(dossier.dateCreation) }}</span>
          </div>
        </div>

        <div class="documents-manquants" *ngIf="dossier.documentsManquants && dossier.documentsManquants.length > 0">
          <h6 class="documents-title">
            <i class="fas fa-exclamation-triangle text-warning"></i>
            Documents manquants
          </h6>
          <ul class="documents-list">
            <li *ngFor="let doc of dossier.documentsManquants">{{ doc }}</li>
          </ul>
        </div>
      </div>

      <form [formGroup]="validationForm" (ngSubmit)="onSubmit()">
        <!-- Action de validation -->
        <div class="form-group">
          <label class="form-label">Action</label>
          <div class="validation-actions">
            <div class="form-check">
              <input
                type="radio"
                id="valider"
                class="form-check-input"
                formControlName="valide"
                [value]="true"
              >
              <label for="valider" class="form-check-label">
                <i class="fas fa-check text-success"></i>
                Valider le dossier
              </label>
            </div>
            <div class="form-check">
              <input
                type="radio"
                id="rejeter"
                class="form-check-input"
                formControlName="valide"
                [value]="false"
              >
              <label for="rejeter" class="form-check-label">
                <i class="fas fa-times text-danger"></i>
                Rejeter le dossier
              </label>
            </div>
          </div>
        </div>

        <!-- Commentaire -->
        <div class="form-group">
          <label for="commentaire" class="form-label required">
            {{ validationForm.get('valide')?.value ? 'Commentaire de validation' : 'Motif de rejet' }}
          </label>
          <textarea
            id="commentaire"
            class="form-control"
            formControlName="commentaire"
            rows="4"
            [placeholder]="getCommentairePlaceholder()"
          ></textarea>
          <div class="form-text">
            {{ validationForm.get('valide')?.value 
              ? 'Ajoutez un commentaire sur la validation (optionnel mais recommandé)' 
              : 'Précisez les raisons du rejet (obligatoire)' }}
          </div>
          <div class="invalid-feedback" *ngIf="validationForm.get('commentaire')?.invalid && validationForm.get('commentaire')?.touched">
            <div *ngIf="validationForm.get('commentaire')?.errors?.['required']">
              Le commentaire est obligatoire pour un rejet
            </div>
            <div *ngIf="validationForm.get('commentaire')?.errors?.['minlength']">
              Le commentaire doit contenir au moins 10 caractères
            </div>
          </div>
        </div>

        <!-- Options supplémentaires pour rejet -->
        <div class="rejection-options" *ngIf="validationForm.get('valide')?.value === false">
          <div class="form-check">
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
          
          <div class="form-check">
            <input
              type="checkbox"
              id="notifierDirecteur"
              class="form-check-input"
              [(ngModel)]="notifierDirecteur"
              [ngModelOptions]="{standalone: true}"
            >
            <label for="notifierDirecteur" class="form-check-label">
              Notifier le directeur de thèse
            </label>
          </div>
        </div>

        <!-- Actions du formulaire -->
        <div class="form-actions">
          <button 
            type="button" 
            class="btn btn-outline-secondary"
            (click)="onCancel()"
            [disabled]="isSubmitting"
          >
            Annuler
          </button>
          <button 
            type="submit" 
            class="btn"
            [class]="validationForm.get('valide')?.value ? 'btn-success' : 'btn-danger'"
            [disabled]="validationForm.invalid || isSubmitting"
          >
            <i [class]="validationForm.get('valide')?.value ? 'fas fa-check' : 'fas fa-times'"></i>
            {{ validationForm.get('valide')?.value ? 'Valider' : 'Rejeter' }}
            <span *ngIf="isSubmitting" class="spinner-border spinner-border-sm ms-2"></span>
          </button>
        </div>
      </form>
    </div>
  `,
  styleUrls: ['./validation-form.component.scss']
})
export class ValidationFormComponent implements OnInit {
  @Input() dossier: DossierInfo | null = null;
  @Input() isSubmitting = false;
  @Output() validationSubmitted = new EventEmitter<{
    validation: ValidationRequest;
    options: {
      notifierDoctorant: boolean;
      notifierDirecteur: boolean;
    };
  }>();
  @Output() cancelled = new EventEmitter<void>();

  validationForm!: FormGroup;
  notifierDoctorant = true;
  notifierDirecteur = false;

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.initForm();
    this.setupFormValidation();
  }

  private initForm(): void {
    this.validationForm = this.fb.group({
      valide: [true, Validators.required],
      commentaire: ['']
    });
  }

  private setupFormValidation(): void {
    // Écouter les changements de l'action de validation
    this.validationForm.get('valide')?.valueChanges.subscribe(valide => {
      const commentaireControl = this.validationForm.get('commentaire');
      
      if (valide === false) {
        // Pour un rejet, le commentaire est obligatoire
        commentaireControl?.setValidators([
          Validators.required,
          Validators.minLength(10)
        ]);
      } else {
        // Pour une validation, le commentaire est optionnel
        commentaireControl?.setValidators([Validators.minLength(5)]);
      }
      
      commentaireControl?.updateValueAndValidity();
    });
  }

  onSubmit(): void {
    if (this.validationForm.valid) {
      const validation: ValidationRequest = {
        valide: this.validationForm.get('valide')?.value,
        commentaire: this.validationForm.get('commentaire')?.value
      };

      const options = {
        notifierDoctorant: this.notifierDoctorant,
        notifierDirecteur: this.notifierDirecteur
      };

      this.validationSubmitted.emit({ validation, options });
    }
  }

  onCancel(): void {
    this.cancelled.emit();
  }

  getCommentairePlaceholder(): string {
    const valide = this.validationForm.get('valide')?.value;
    
    if (valide) {
      return 'Commentaire sur la validation (optionnel)...';
    } else {
      return 'Précisez les raisons du rejet, les documents manquants ou les corrections à apporter...';
    }
  }

  // Méthodes utilitaires pour l'affichage
  getTypeBadgeClass(type: string): string {
    return type === 'inscription' ? 'primary' : 'info';
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
}