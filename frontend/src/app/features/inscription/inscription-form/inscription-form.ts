import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
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
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Observable, Subject, interval } from 'rxjs';
import { map, startWith, takeUntil, debounceTime } from 'rxjs/operators';

import { InscriptionService } from '../../../core/services/inscription.service';
import { CampagneService } from '../../../core/services/campagne.service';
import { DocumentService } from '../../../core/services/document.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';

import { 
  InscriptionRequest, 
  TypeInscription,
  StatutInscription
} from '../../../core/models/inscription.model';
import { CampagneResponse } from '../../../core/models/campagne.model';
import { TypeDocument, DOCUMENT_CONFIGS } from '../../../core/models/document.model';
import { UserResponse } from '../../../core/models/user.model';

@Component({
  selector: 'app-inscription-form',
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
    MatAutocompleteModule,
    MatChipsModule,
    MatProgressBarModule
  ],
  templateUrl: './inscription-form.html',
  styleUrls: ['./inscription-form.scss']
})
export class InscriptionForm implements OnInit, OnDestroy {
  @ViewChild('stepper') stepper!: MatStepper;
  
  private destroy$ = new Subject<void>();
  private autoSaveInterval = 30000; // 30 secondes
  
  // Stepper forms
  infoGeneralesForm!: FormGroup;
  infosPersonnellesForm!: FormGroup;
  infosTheseForm!: FormGroup;
  documentsForm!: FormGroup;
  
  // Data
  campagneActive$!: Observable<CampagneResponse | null>;
  directeurs: UserResponse[] = [];
  filteredDirecteurs$!: Observable<UserResponse[]>;
  currentUser: any;
  
  // State
  loading = false;
  saving = false;
  submitting = false;
  error: string | null = null;
  isEditMode = false;
  inscriptionId: number | null = null;
  draftId: number | null = null;
  lastSaved: Date | null = null;
  
  // Upload state
  uploadedDocuments: Map<TypeDocument, any> = new Map();
  uploadProgress: Map<TypeDocument, number> = new Map();
  requiredDocuments = DOCUMENT_CONFIGS.filter(d => d.obligatoire);
  
  // Options
  laboratoires = [
    'Laboratoire d\'Informatique',
    'Laboratoire de Mathématiques',
    'Laboratoire de Physique',
    'Laboratoire de Chimie',
    'Laboratoire de Biologie',
    'Autre'
  ];

  disciplines = [
    'Informatique',
    'Mathématiques',
    'Physique',
    'Chimie',
    'Biologie',
    'Sciences de l\'Ingénieur',
    'Autre'
  ];
  
  etablissements = [
    'EMSI',
    'Université Mohammed V',
    'Université Hassan II',
    'Autre'
  ];
  
  pays = [
    'Maroc',
    'France',
    'Espagne',
    'Belgique',
    'Canada',
    'Autre'
  ];

  constructor(
    private fb: FormBuilder,
    private inscriptionService: InscriptionService,
    private campagneService: CampagneService,
    private documentService: DocumentService,
    private authService: AuthService,
    private userService: UserService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {
    this.initializeForms();
  }

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadData();
    this.setupAutoSave();
    this.checkEditMode();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForms(): void {
    // Étape 1: Informations générales
    this.infoGeneralesForm = this.fb.group({
      campagneId: ['', Validators.required],
      directeurTheseId: ['', Validators.required],
      directeurSearch: [''],
      sujetThese: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(500)]]
    });

    // Étape 2: Informations personnelles
    this.infosPersonnellesForm = this.fb.group({
      cin: ['', [Validators.required, Validators.pattern(/^[A-Z]{1,2}[0-9]{1,6}$/)]],
      cne: ['', Validators.pattern(/^[A-Z0-9]{10}$/)],
      telephone: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
      adresse: ['', Validators.required],
      ville: ['', Validators.required],
      pays: ['Maroc', Validators.required],
      dateNaissance: ['', Validators.required],
      lieuNaissance: ['', Validators.required],
      nationalite: ['Marocaine', Validators.required]
    });

    // Étape 3: Informations de thèse
    this.infosTheseForm = this.fb.group({
      titreThese: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]],
      discipline: ['', Validators.required],
      disciplineAutre: [''],
      laboratoire: ['', Validators.required],
      laboratoireAutre: [''],
      etablissementAccueil: ['EMSI', Validators.required],
      etablissementAutre: [''],
      cotutelle: [false],
      universitePartenaire: [''],
      paysPartenaire: [''],
      dateDebutPrevue: ['', Validators.required]
    });

    // Conditional validators
    this.setupConditionalValidators();

    // Étape 4: Documents
    this.documentsForm = this.fb.group({});
  }

  private setupConditionalValidators(): void {
    // Discipline autre
    this.infosTheseForm.get('discipline')?.valueChanges.subscribe(value => {
      const autreControl = this.infosTheseForm.get('disciplineAutre');
      if (value === 'Autre') {
        autreControl?.setValidators([Validators.required]);
      } else {
        autreControl?.clearValidators();
      }
      autreControl?.updateValueAndValidity();
    });

    // Laboratoire autre
    this.infosTheseForm.get('laboratoire')?.valueChanges.subscribe(value => {
      const autreControl = this.infosTheseForm.get('laboratoireAutre');
      if (value === 'Autre') {
        autreControl?.setValidators([Validators.required]);
      } else {
        autreControl?.clearValidators();
      }
      autreControl?.updateValueAndValidity();
    });

    // Établissement autre
    this.infosTheseForm.get('etablissementAccueil')?.valueChanges.subscribe(value => {
      const autreControl = this.infosTheseForm.get('etablissementAutre');
      if (value === 'Autre') {
        autreControl?.setValidators([Validators.required]);
      } else {
        autreControl?.clearValidators();
      }
      autreControl?.updateValueAndValidity();
    });

    // Cotutelle
    this.infosTheseForm.get('cotutelle')?.valueChanges.subscribe(value => {
      const universiteControl = this.infosTheseForm.get('universitePartenaire');
      const paysControl = this.infosTheseForm.get('paysPartenaire');
      
      if (value) {
        universiteControl?.setValidators([Validators.required]);
        paysControl?.setValidators([Validators.required]);
      } else {
        universiteControl?.clearValidators();
        paysControl?.clearValidators();
      }
      universiteControl?.updateValueAndValidity();
      paysControl?.updateValueAndValidity();
    });
  }

  private loadData(): void {
    this.loading = true;

    // Charger la campagne active
    this.campagneActive$ = this.campagneService.getCampagneActive();
    this.campagneActive$.subscribe(campagne => {
      if (campagne) {
        this.infoGeneralesForm.patchValue({ campagneId: campagne.id });
      }
    });

    // Charger les directeurs
    this.userService.getDirecteurs().subscribe({
      next: (directeurs) => {
        this.directeurs = directeurs;
        this.setupDirecteurFilter();
        this.loading = false;
      },
      error: (error) => {
        this.showError('Erreur lors du chargement des directeurs');
        this.loading = false;
      }
    });
  }

  private setupDirecteurFilter(): void {
    const searchControl = this.infoGeneralesForm.get('directeurSearch');
    if (searchControl) {
      this.filteredDirecteurs$ = searchControl.valueChanges.pipe(
        startWith(''),
        map(value => this.filterDirecteurs(value || ''))
      );
    }
  }

  private filterDirecteurs(value: string): UserResponse[] {
    const filterValue = value.toLowerCase();
    return this.directeurs.filter(directeur =>
      `${directeur.FirstName} ${directeur.LastName}`.toLowerCase().includes(filterValue) ||
      directeur.email.toLowerCase().includes(filterValue)
    );
  }

  onDirecteurSelect(directeur: UserResponse): void {
    this.infoGeneralesForm.patchValue({
      directeurTheseId: directeur.id,
      directeurSearch: `${directeur.FirstName} ${directeur.LastName}`
    });
  }

  private checkEditMode(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.inscriptionId = +id;
      this.loadInscriptionForEdit(this.inscriptionId);
    }
  }

  private loadInscriptionForEdit(id: number): void {
    this.loading = true;
    this.inscriptionService.getInscription(id).subscribe({
      next: (inscription) => {
        this.populateFormsForEdit(inscription);
        this.loading = false;
      },
      error: (error) => {
        this.showError('Erreur lors du chargement de l\'inscription');
        this.loading = false;
      }
    });
  }

  private populateFormsForEdit(inscription: any): void {
    // Étape 1
    this.infoGeneralesForm.patchValue({
      campagneId: inscription.campagneId,
      directeurTheseId: inscription.directeurTheseId,
      sujetThese: inscription.sujetThese
    });

    // Étape 2
    if (inscription.infosDoctorant) {
      this.infosPersonnellesForm.patchValue(inscription.infosDoctorant);
    }

    // Étape 3
    if (inscription.infosThese) {
      this.infosTheseForm.patchValue(inscription.infosThese);
    }

    // Documents
    if (inscription.documents) {
      inscription.documents.forEach((doc: any) => {
        this.uploadedDocuments.set(doc.typeDocument, doc);
      });
    }
  }

  private setupAutoSave(): void {
    // Auto-save every 30 seconds
    interval(this.autoSaveInterval)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.hasUnsavedChanges() && !this.saving) {
          this.saveDraft();
        }
      });

    // Save on form changes (debounced)
    this.infoGeneralesForm.valueChanges
      .pipe(
        debounceTime(5000),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        if (!this.saving) {
          this.saveDraft();
        }
      });
  }

  private hasUnsavedChanges(): boolean {
    return this.infoGeneralesForm.dirty || 
           this.infosPersonnellesForm.dirty || 
           this.infosTheseForm.dirty;
  }

  saveDraft(): void {
    if (!this.infoGeneralesForm.valid) {
      return;
    }

    this.saving = true;
    const draftData = this.prepareInscriptionData();

    // Si c'est un brouillon existant, on met à jour
    if (this.draftId) {
      // Update draft logic here
      this.saving = false;
      this.lastSaved = new Date();
      this.showSuccess('Brouillon sauvegardé');
    } else {
      // Create new draft
      this.inscriptionService.createInscription(draftData).subscribe({
        next: (response) => {
          this.draftId = response.id;
          this.lastSaved = new Date();
          this.saving = false;
          this.showSuccess('Brouillon sauvegardé');
        },
        error: (error) => {
          this.saving = false;
          console.error('Erreur sauvegarde brouillon:', error);
        }
      });
    }
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

    // Upload file
    this.uploadProgress.set(type, 0);
    
    if (!this.draftId) {
      this.showError('Veuillez d\'abord sauvegarder le formulaire');
      return;
    }

    this.documentService.uploadDocument(this.draftId, file, type).subscribe({
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

  private prepareInscriptionData(): InscriptionRequest {
    const formData = {
      ...this.infoGeneralesForm.value,
      ...this.infosPersonnellesForm.value,
      ...this.infosTheseForm.value
    };

    // Handle "Autre" fields
    if (formData.discipline === 'Autre') {
      formData.discipline = formData.disciplineAutre;
    }
    if (formData.laboratoire === 'Autre') {
      formData.laboratoire = formData.laboratoireAutre;
    }
    if (formData.etablissementAccueil === 'Autre') {
      formData.etablissementAccueil = formData.etablissementAutre;
    }

    return {
      doctorantId: this.currentUser.id,
      directeurTheseId: formData.directeurTheseId,
      campagneId: formData.campagneId,
      sujetThese: formData.sujetThese,
      type: TypeInscription.PREMIERE_INSCRIPTION,
      anneeInscription: new Date().getFullYear(),
      cin: formData.cin,
      cne: formData.cne,
      telephone: formData.telephone,
      adresse: formData.adresse,
      ville: formData.ville,
      pays: formData.pays,
      dateNaissance: formData.dateNaissance,
      lieuNaissance: formData.lieuNaissance,
      nationalite: formData.nationalite,
      titreThese: formData.titreThese,
      discipline: formData.discipline,
      laboratoire: formData.laboratoire,
      etablissementAccueil: formData.etablissementAccueil,
      cotutelle: formData.cotutelle,
      universitePartenaire: formData.universitePartenaire,
      paysPartenaire: formData.paysPartenaire,
      dateDebutPrevue: formData.dateDebutPrevue
    };
  }

  onSubmit(): void {
    // Validate all forms
    if (!this.infoGeneralesForm.valid || 
        !this.infosPersonnellesForm.valid || 
        !this.infosTheseForm.valid) {
      this.showError('Veuillez remplir tous les champs obligatoires');
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

    if (confirm('Êtes-vous sûr de vouloir soumettre votre inscription ? Vous ne pourrez plus la modifier.')) {
      this.submitInscription();
    }
  }

  private submitInscription(): void {
    this.submitting = true;
    
    if (this.draftId) {
      // Submit existing draft
      this.inscriptionService.soumettre(this.draftId, this.currentUser.id).subscribe({
        next: (response) => {
          this.submitting = false;
          this.showSuccess('Inscription soumise avec succès !');
          setTimeout(() => {
            this.router.navigate(['/inscription', response.id]);
          }, 2000);
        },
        error: (error) => {
          this.submitting = false;
          this.showError('Erreur lors de la soumission');
        }
      });
    } else {
      // Create and submit
      const data = this.prepareInscriptionData();
      this.inscriptionService.createInscription(data).subscribe({
        next: (response) => {
          this.inscriptionService.soumettre(response.id, this.currentUser.id).subscribe({
            next: (finalResponse) => {
              this.submitting = false;
              this.showSuccess('Inscription soumise avec succès !');
              setTimeout(() => {
                this.router.navigate(['/inscription', finalResponse.id]);
              }, 2000);
            },
            error: (error) => {
              this.submitting = false;
              this.showError('Erreur lors de la soumission');
            }
          });
        },
        error: (error) => {
          this.submitting = false;
          this.showError('Erreur lors de la création');
        }
      });
    }
  }

  onCancel(): void {
    if (this.hasUnsavedChanges()) {
      if (confirm('Vous avez des modifications non sauvegardées. Voulez-vous vraiment quitter ?')) {
        this.router.navigate(['/inscription']);
      }
    } else {
      this.router.navigate(['/inscription']);
    }
  }

  // Helper methods
  isFieldInvalid(form: FormGroup, fieldName: string): boolean {
    const field = form.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getFieldError(form: FormGroup, fieldName: string): string {
    const field = form.get(fieldName);
    if (field && field.errors) {
      if (field.errors['required']) return 'Ce champ est obligatoire';
      if (field.errors['minlength']) return `Minimum ${field.errors['minlength'].requiredLength} caractères`;
      if (field.errors['maxlength']) return `Maximum ${field.errors['maxlength'].requiredLength} caractères`;
      if (field.errors['pattern']) return 'Format invalide';
    }
    return '';
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
