import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { InscriptionService } from '../../../core/services/inscription.service';
import { CampagneService } from '../../../core/services/campagne.service';
import { DocumentService } from '../../../core/services/document.service';
import { AuthService, UserInfo } from '../../../core/services/auth.service';
import { DerogationService } from '../../../core/services/derogation.service';

import { 
  InscriptionRequest, 
  InscriptionResponse,
  TypeInscription,
  StatutInscription
} from '../../../core/models/inscription.model';
import { CampagneResponse } from '../../../core/models/campagne.model';
import { TypeDocument, DOCUMENT_CONFIGS } from '../../../core/models/document.model';
import { DerogationRequestDTO } from '../../../core/models/derogation.model';

@Component({
  selector: 'app-reinscription-form',
  standalone: true,
  imports: [
    CommonModule, 
    ReactiveFormsModule,
    MatStepperModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatCardModule,
    MatCheckboxModule,
    MatProgressBarModule,
    MatDividerModule,
    MatChipsModule
  ],
  templateUrl: './reinscription-form.html',
  styleUrls: ['./reinscription-form.scss']
})
export class ReinscriptionForm implements OnInit, OnDestroy {
  @ViewChild('stepper') stepper!: MatStepper;
  
  private destroy$ = new Subject<void>();
  
  // Forms
  verificationForm!: FormGroup;
  modificationsForm!: FormGroup;
  documentsForm!: FormGroup;
  derogationForm!: FormGroup;
  
  // Data
  previousInscription: InscriptionResponse | null = null;
  campagneActive: CampagneResponse | null = null;
  currentUser: UserInfo | null = null;
  
  // State
  loading = false;
  submitting = false;
  error: string | null = null;
  requiresDerogation = false;
  dureeDoctoratAnnees = 0;
  
  // Upload state
  uploadedDocuments: Map<TypeDocument, any> = new Map();
  uploadProgress: Map<TypeDocument, number> = new Map();
  requiredDocuments = DOCUMENT_CONFIGS.filter(d => d.obligatoire);

  constructor(
    private fb: FormBuilder,
    private inscriptionService: InscriptionService,
    private campagneService: CampagneService,
    private documentService: DocumentService,
    private derogationService: DerogationService,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.initializeForms();
  }

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      if (user) {
        this.loadData();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForms(): void {
    // Étape 1: Vérification des données
    this.verificationForm = this.fb.group({
      confirmed: [false, Validators.requiredTrue]
    });

    // Étape 2: Modifications
    this.modificationsForm = this.fb.group({
      telephone: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
      adresse: ['', Validators.required],
      ville: ['', Validators.required],
      titreThese: ['', [Validators.required, Validators.minLength(10)]],
      laboratoire: ['', Validators.required],
      etablissementAccueil: ['', Validators.required]
    });

    // Étape 3: Documents
    this.documentsForm = this.fb.group({});

    // Étape 4: Dérogation (si nécessaire)
    this.derogationForm = this.fb.group({
      motif: ['', Validators.required],
      justification: ['', [Validators.required, Validators.minLength(50)]]
    });
  }

  private loadData(): void {
    if (!this.currentUser) return;
    
    this.loading = true;

    // Load active campaign
    this.campagneService.getCampagneActive().subscribe({
      next: (campagne) => {
        this.campagneActive = campagne;
      },
      error: (error) => {
        this.showError('Aucune campagne active');
      }
    });

    // Load previous inscription
    this.inscriptionService.getInscriptionsDoctorant(this.currentUser.id).subscribe({
      next: (inscriptions) => {
        // Get the most recent validated inscription
        const validated = inscriptions.filter(i => i.statut === StatutInscription.VALIDE);
        if (validated.length > 0) {
          this.previousInscription = validated.reduce((latest, current) => {
            return new Date(current.dateCreation) > new Date(latest.dateCreation) ? current : latest;
          });
          
          this.prefillForms();
          this.checkDerogationRequirement();
        } else {
          this.error = 'Aucune inscription validée trouvée';
        }
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Erreur lors du chargement des données';
        this.loading = false;
      }
    });
  }

  private prefillForms(): void {
    if (!this.previousInscription) return;

    const infos = this.previousInscription.infosDoctorant;
    const these = this.previousInscription.infosThese;

    this.modificationsForm.patchValue({
      telephone: infos.telephone,
      adresse: infos.adresse,
      ville: infos.ville,
      titreThese: these.titreThese,
      laboratoire: these.laboratoire,
      etablissementAccueil: these.etablissementAccueil
    });
  }

  private checkDerogationRequirement(): void {
    if (!this.previousInscription) return;

    // Calculate doctorate duration
    const dateDebut = new Date(this.previousInscription.infosThese.dateDebutEffective || 
                                this.previousInscription.infosThese.dateDebutPrevue);
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - dateDebut.getTime());
    const diffYears = diffTime / (1000 * 60 * 60 * 24 * 365.25);
    
    this.dureeDoctoratAnnees = Math.floor(diffYears);
    this.requiresDerogation = this.dureeDoctoratAnnees >= 3;

    if (this.requiresDerogation) {
      this.derogationForm.get('motif')?.setValidators([Validators.required]);
      this.derogationForm.get('justification')?.setValidators([Validators.required, Validators.minLength(50)]);
    } else {
      this.derogationForm.get('motif')?.clearValidators();
      this.derogationForm.get('justification')?.clearValidators();
    }
    this.derogationForm.updateValueAndValidity();
  }

  onFileSelected(event: any, type: TypeDocument): void {
    const file = event.target.files[0];
    if (!file) return;

    // Validate file
    const validation = this.documentService.getValidationErrorMessage(file);
    if (validation) {
      this.showError(validation);
      return;
    }

    // For reinscription, we need to create a draft first
    if (!this.previousInscription) {
      this.showError('Erreur: inscription précédente non trouvée');
      return;
    }

    // Upload file (we'll use the previous inscription ID temporarily)
    this.uploadProgress.set(type, 0);
    
    this.documentService.uploadDocument(this.previousInscription.id, file, type).subscribe({
      next: (event) => {
        const progress = this.documentService.getUploadProgress(event);
        this.uploadProgress.set(type, progress.progress);
        
        if (progress.status === 'complete' && event.type === 4) {
          this.uploadedDocuments.set(type, event.body);
          this.showSuccess('Document uploadé avec succès');
        }
      },
      error: (error) => {
        this.uploadProgress.delete(type);
        this.showError('Erreur lors de l\'upload du document');
      }
    });
  }

  removeDocument(type: TypeDocument): void {
    const doc = this.uploadedDocuments.get(type);
    if (!doc) return;

    if (confirm('Êtes-vous sûr de vouloir supprimer ce document ?')) {
      this.documentService.deleteDocument(doc.id).subscribe({
        next: () => {
          this.uploadedDocuments.delete(type);
          this.uploadProgress.delete(type);
          this.showSuccess('Document supprimé');
        },
        error: (error) => {
          this.showError('Erreur lors de la suppression');
        }
      });
    }
  }

  private prepareReinscriptionData(): InscriptionRequest {
    if (!this.previousInscription || !this.campagneActive || !this.currentUser) {
      throw new Error('Données manquantes');
    }

    const modifications = this.modificationsForm.value;
    const infos = this.previousInscription.infosDoctorant;
    const these = this.previousInscription.infosThese;

    return {
      doctorantId: this.currentUser.id,
      directeurTheseId: this.previousInscription.directeurTheseId,
      campagneId: this.campagneActive.id,
      sujetThese: this.previousInscription.sujetThese,
      type: TypeInscription.REINSCRIPTION,
      anneeInscription: new Date().getFullYear(),
      
      // Informations doctorant (with modifications)
      cin: infos.cin,
      cne: infos.cne,
      telephone: modifications.telephone,
      adresse: modifications.adresse,
      ville: modifications.ville,
      pays: infos.pays,
      dateNaissance: infos.dateNaissance,
      lieuNaissance: infos.lieuNaissance,
      nationalite: infos.nationalite,
      
      // Informations thèse (with modifications)
      titreThese: modifications.titreThese,
      discipline: these.discipline,
      laboratoire: modifications.laboratoire,
      etablissementAccueil: modifications.etablissementAccueil,
      cotutelle: these.cotutelle,
      universitePartenaire: these.universitePartenaire,
      paysPartenaire: these.paysPartenaire,
      dateDebutPrevue: these.dateDebutEffective || these.dateDebutPrevue
    };
  }

  onSubmit(): void {
    // Validate all forms
    if (!this.verificationForm.valid || !this.modificationsForm.valid) {
      this.showError('Veuillez remplir tous les champs obligatoires');
      return;
    }

    if (this.requiresDerogation && !this.derogationForm.valid) {
      this.showError('Veuillez remplir la demande de dérogation');
      return;
    }

    // Check required documents
    const missingDocs = this.requiredDocuments.filter(
      doc => !this.uploadedDocuments.has(doc.type)
    );
    
    if (missingDocs.length > 0) {
      this.showError(`Documents manquants: ${missingDocs.map(d => d.label).join(', ')}`);
      return;
    }

    if (confirm('Êtes-vous sûr de vouloir soumettre votre réinscription ?')) {
      this.submitReinscription();
    }
  }

  private submitReinscription(): void {
    this.submitting = true;
    
    const data = this.prepareReinscriptionData();
    
    // Create reinscription
    this.inscriptionService.createInscription(data).subscribe({
      next: (inscription) => {
        // If derogation is required, create it
        if (this.requiresDerogation) {
          this.createDerogation(inscription.id);
        } else {
          this.finalizeSubmission(inscription.id);
        }
      },
      error: (error) => {
        this.submitting = false;
        this.showError('Erreur lors de la création de la réinscription');
      }
    });
  }

  private createDerogation(inscriptionId: number): void {
    this.derogationService.createDerogation(inscriptionId, this.derogationForm.value.motif, this.derogationForm.value.justification).subscribe({
      next: () => {
        this.finalizeSubmission(inscriptionId);
      },
      error: (error) => {
        this.submitting = false;
        this.showError('Erreur lors de la création de la dérogation');
      }
    });
  }

  private finalizeSubmission(inscriptionId: number): void {
    if (!this.currentUser) return;
    
    // Submit the inscription
    this.inscriptionService.soumettre(inscriptionId, this.currentUser.id).subscribe({
      next: (response) => {
        this.submitting = false;
        this.showSuccess('Réinscription soumise avec succès !');
        setTimeout(() => {
          this.router.navigate(['/inscription', response.id]);
        }, 2000);
      },
      error: (error) => {
        this.submitting = false;
        this.showError('Erreur lors de la soumission');
      }
    });
  }

  onCancel(): void {
    if (confirm('Voulez-vous vraiment annuler la réinscription ?')) {
      this.router.navigate(['/inscription/dashboard']);
    }
  }

  get allRequiredDocumentsUploaded(): boolean {
    return this.requiredDocuments.every(doc => this.uploadedDocuments.has(doc.type));
  }

  get uploadProgressPercentage(): number {
    const total = this.requiredDocuments.length;
    const uploaded = Array.from(this.uploadedDocuments.keys()).length;
    return Math.round((uploaded / total) * 100);
  }

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 3000,
      panelClass: ['success-snackbar']
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }
}
