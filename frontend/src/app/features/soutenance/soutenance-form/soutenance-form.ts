import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { SoutenanceService } from '../../../core/services/soutenance.service';
import { AuthService } from '../../../core/services/auth.service';
import { SoutenanceRequest, SoutenanceResponse, PrerequisStatus, JuryMemberRequest, JuryMember } from '../../../core/models/soutenance.model';
import { DocumentUpload, DocumentUploadConfig } from '../../inscription/document-upload/document-upload';
import { DocumentType, DocumentResponse } from '../../../core/models/document.model';
import { CustomValidators } from '../../../core/validators/custom-validators';
import { PrerequisCheckComponent } from '../../../shared/components/prerequis-check/prerequis-check.component';
import { JuryProposalComponent } from '../jury-proposal/jury-proposal.component';

@Component({
  selector: 'app-soutenance-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DocumentUpload, PrerequisCheckComponent, JuryProposalComponent],
  template: `
    <div class="soutenance-form">
      <header class="form-header">
        <h1>{{ isEditMode ? 'Modifier' : 'Nouvelle' }} demande de soutenance</h1>
        <p class="subtitle">Remplissez les informations pour votre demande de soutenance</p>
      </header>

      <!-- Vérification des prérequis -->
      <div class="prerequis-section">
        <app-prerequis-check
          [doctorantId]="currentUserId"
          [autoCheck]="true"
          [showTitle]="true"
          [compact]="false"
          (prerequisStatusChange)="onPrerequisStatusChange($event)"
          (canSubmitChange)="onCanSubmitChange($event)">
        </app-prerequis-check>
      </div>

      <!-- Formulaire -->
      <form [formGroup]="soutenanceForm" (ngSubmit)="onSubmit()" class="form-content">
        <div class="form-section">
          <h3>Informations générales</h3>
          
          <div class="form-group">
            <label for="titrethese">Titre de la thèse *</label>
            <textarea 
              id="titrethese"
              formControlName="titrethese"
              rows="3"
              placeholder="Saisissez le titre complet de votre thèse"
              [class.error]="soutenanceForm.get('titrethese')?.invalid && soutenanceForm.get('titrethese')?.touched">
            </textarea>
            <div class="error-message" *ngIf="soutenanceForm.get('titrethese')?.invalid && soutenanceForm.get('titrethese')?.touched">
              <span *ngIf="soutenanceForm.get('titrethese')?.errors?.['required']">Le titre de la thèse est obligatoire</span>
              <span *ngIf="soutenanceForm.get('titrethese')?.errors?.['minlength']">Le titre doit contenir au moins 10 caractères</span>
              <span *ngIf="soutenanceForm.get('titrethese')?.errors?.['maxlength']">Le titre ne peut pas dépasser 500 caractères</span>
            </div>
          </div>

          <div class="form-group">
            <label for="resumeThese">Résumé de la thèse *</label>
            <textarea 
              id="resumeThese"
              formControlName="resumeThese"
              rows="6"
              placeholder="Rédigez un résumé de votre thèse (maximum 2000 caractères)"
              [class.error]="soutenanceForm.get('resumeThese')?.invalid && soutenanceForm.get('resumeThese')?.touched">
            </textarea>
            <div class="char-counter">
              {{ soutenanceForm.get('resumeThese')?.value?.length || 0 }} / 2000 caractères
            </div>
            <div class="error-message" *ngIf="soutenanceForm.get('resumeThese')?.invalid && soutenanceForm.get('resumeThese')?.touched">
              <span *ngIf="soutenanceForm.get('resumeThese')?.errors?.['required']">Le résumé de la thèse est obligatoire</span>
              <span *ngIf="soutenanceForm.get('resumeThese')?.errors?.['minlength']">Le résumé doit contenir au moins 50 caractères</span>
              <span *ngIf="soutenanceForm.get('resumeThese')?.errors?.['maxlength']">Le résumé ne peut pas dépasser 2000 caractères</span>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="dateSoutenance">Date de soutenance souhaitée</label>
              <input 
                type="date"
                id="dateSoutenance"
                formControlName="dateSoutenance"
                [min]="minDate"
                [class.error]="soutenanceForm.get('dateSoutenance')?.invalid && soutenanceForm.get('dateSoutenance')?.touched">
              <div class="error-message" *ngIf="soutenanceForm.get('dateSoutenance')?.invalid && soutenanceForm.get('dateSoutenance')?.touched">
                <span *ngIf="soutenanceForm.get('dateSoutenance')?.errors?.['futureDate']">La date de soutenance doit être dans le futur</span>
              </div>
            </div>

            <div class="form-group">
              <label for="lieuSoutenance">Lieu de soutenance</label>
              <input 
                type="text"
                id="lieuSoutenance"
                formControlName="lieuSoutenance"
                placeholder="Ex: Amphithéâtre A, Université..."
                [class.error]="soutenanceForm.get('lieuSoutenance')?.invalid && soutenanceForm.get('lieuSoutenance')?.touched">
              <div class="error-message" *ngIf="soutenanceForm.get('lieuSoutenance')?.invalid && soutenanceForm.get('lieuSoutenance')?.touched">
                <span *ngIf="soutenanceForm.get('lieuSoutenance')?.errors?.['minlength']">Le lieu doit contenir au moins 3 caractères</span>
              </div>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="specialite">Spécialité *</label>
              <input 
                type="text"
                id="specialite"
                formControlName="specialite"
                placeholder="Ex: Informatique, Mathématiques..."
                [class.error]="soutenanceForm.get('specialite')?.invalid && soutenanceForm.get('specialite')?.touched">
              <div class="error-message" *ngIf="soutenanceForm.get('specialite')?.invalid && soutenanceForm.get('specialite')?.touched">
                <span *ngIf="soutenanceForm.get('specialite')?.errors?.['required']">La spécialité est obligatoire</span>
              </div>
            </div>

            <div class="form-group">
              <label for="laboratoire">Laboratoire *</label>
              <input 
                type="text"
                id="laboratoire"
                formControlName="laboratoire"
                placeholder="Ex: LIRMM, IRIT..."
                [class.error]="soutenanceForm.get('laboratoire')?.invalid && soutenanceForm.get('laboratoire')?.touched">
              <div class="error-message" *ngIf="soutenanceForm.get('laboratoire')?.invalid && soutenanceForm.get('laboratoire')?.touched">
                <span *ngIf="soutenanceForm.get('laboratoire')?.errors?.['required']">Le laboratoire est obligatoire</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Section Documents obligatoires -->
        <div class="form-section">
          <h3>Documents obligatoires</h3>
          <p class="section-description">
            Veuillez télécharger tous les documents requis pour votre demande de soutenance.
          </p>

          <div class="documents-grid">
            <div class="document-upload-container" *ngFor="let docConfig of documentConfigs">
              <app-document-upload
                [config]="docConfig"
                [inscriptionId]="soutenanceId"
                [disabled]="isSubmitting"
                (documentUploaded)="onDocumentUploaded($event)"
                (documentDeleted)="onDocumentDeleted($event)"
                (uploadError)="onDocumentError($event)">
              </app-document-upload>
            </div>
          </div>

          <div class="documents-summary" *ngIf="uploadedDocuments.length > 0">
            <h4>Documents téléchargés ({{ uploadedDocuments.length }}/{{ requiredDocumentsCount }})</h4>
            <div class="document-list">
              <div class="document-item" *ngFor="let doc of uploadedDocuments">
                <i class="icon-file-text"></i>
                <span class="document-name">{{ getDocumentTypeLabel(doc.type) }}</span>
                <span class="document-size">{{ formatFileSize(doc.taille) }}</span>
                <span class="document-status" [class]="doc.valide ? 'valid' : 'pending'">
                  {{ doc.valide ? 'Validé' : 'En attente' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Section Composition du jury -->
        <div class="form-section" *ngIf="showJurySection">
          <app-jury-proposal
            [soutenanceId]="soutenanceId"
            [existingJury]="existingJury"
            [directeurInfo]="directeurInfo"
            (juryUpdated)="onJuryUpdated($event)"
            (cancel)="onJuryCancel()">
          </app-jury-proposal>
        </div>

        <div class="form-actions">
          <button type="button" class="btn btn-secondary" (click)="onCancel()">
            Annuler
          </button>
          <button type="button" class="btn btn-outline" (click)="onSaveDraft()" [disabled]="isSubmitting">
            {{ isSubmitting ? 'Sauvegarde...' : 'Sauvegarder en brouillon' }}
          </button>
          <button 
            type="submit" 
            class="btn btn-primary" 
            [disabled]="!canSubmit() || isSubmitting"
            [title]="getSubmitButtonTooltip()">
            {{ isSubmitting ? 'Soumission...' : 'Soumettre la demande' }}
          </button>
        </div>
      </form>

      <!-- Messages d'erreur -->
      <div class="error-alert" *ngIf="errorMessage">
        <i class="icon-alert-circle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <!-- Message de succès -->
      <div class="success-alert" *ngIf="successMessage">
        <i class="icon-check-circle"></i>
        <span>{{ successMessage }}</span>
      </div>
    </div>
  `,
  styleUrls: ['./soutenance-form.scss']
})
export class SoutenanceForm implements OnInit {
  soutenanceForm: FormGroup;
  isEditMode = false;
  soutenanceId?: number;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';
  minDate: string;
  currentUserId: number = 0;
  prerequisStatus: PrerequisStatus | null = null;
  canSubmitBasedOnPrerequis = false;
  
  // Document upload configuration
  documentConfigs: DocumentUploadConfig[] = [];
  uploadedDocuments: DocumentResponse[] = [];
  requiredDocumentsCount = 0;

  // Jury management
  showJurySection = false;
  existingJury: JuryMember[] = [];
  directeurInfo?: { nom: string; prenom: string; etablissement: string; grade: string };
  juryData: JuryMemberRequest[] = [];

  constructor(
    private fb: FormBuilder,
    private soutenanceService: SoutenanceService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.soutenanceForm = this.createForm();
    this.minDate = new Date().toISOString().split('T')[0];
    this.initializeDocumentConfigs();
    
    // Get current user ID
    this.authService.getCurrentUser().subscribe(currentUser => {
      if (currentUser) {
        this.currentUserId = currentUser.id;
      }
    });
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.soutenanceId = +params['id'];
        this.loadSoutenance();
      }
    });

    // Initialize director info for jury
    this.initializeDirecteurInfo();
  }

  private createForm(): FormGroup {
    const form = this.fb.group({
      titrethese: ['', [
        Validators.required, 
        Validators.minLength(10),
        Validators.maxLength(500)
      ]],
      resumeThese: ['', [
        Validators.required,
        Validators.minLength(50),
        Validators.maxLength(2000)
      ]],
      dateSoutenance: ['', [CustomValidators.futureDate]],
      lieuSoutenance: ['', [Validators.minLength(3)]],
      specialite: ['', [Validators.required]],
      laboratoire: ['', [Validators.required]]
    });

    // Listen to form changes to show/hide jury section
    form.valueChanges.subscribe(() => {
      this.updateJurySectionVisibility();
    });

    return form;
  }

  private loadSoutenance(): void {
    if (!this.soutenanceId) return;

    this.soutenanceService.getSoutenance(this.soutenanceId).subscribe({
      next: (soutenance) => {
        this.soutenanceForm.patchValue({
          titrethese: soutenance.titrethese,
          resumeThese: soutenance.resumeThese || '',
          dateSoutenance: soutenance.dateSoutenance ? 
            new Date(soutenance.dateSoutenance).toISOString().split('T')[0] : '',
          lieuSoutenance: soutenance.lieuSoutenance || '',
          specialite: soutenance.specialite || '',
          laboratoire: soutenance.laboratoire || ''
        });
        
        // Load existing documents
        if (soutenance.documents) {
          this.uploadedDocuments = soutenance.documents;
        }

        // Load existing jury
        if (soutenance.jury) {
          this.existingJury = soutenance.jury;
        }

        // Show jury section if we have basic info filled
        this.showJurySection = !!(soutenance.titrethese && soutenance.specialite && soutenance.laboratoire);
      },
      error: (error) => {
        console.error('Erreur lors du chargement de la soutenance:', error);
        this.errorMessage = 'Erreur lors du chargement des données';
      }
    });
  }

  private initializeDocumentConfigs(): void {
    this.documentConfigs = [
      {
        type: DocumentType.MANUSCRIT_THESE,
        label: 'Manuscrit de thèse',
        required: true,
        maxSizeMB: 50,
        allowedTypes: ['application/pdf'],
        description: 'Version finale du manuscrit de thèse au format PDF'
      },
      {
        type: DocumentType.RESUME_THESE,
        label: 'Résumé de thèse',
        required: true,
        maxSizeMB: 5,
        allowedTypes: ['application/pdf'],
        description: 'Résumé détaillé de la thèse (2-4 pages)'
      },
      {
        type: DocumentType.PUBLICATIONS,
        label: 'Liste des publications',
        required: true,
        maxSizeMB: 10,
        allowedTypes: ['application/pdf'],
        description: 'Liste complète des publications scientifiques'
      },
      {
        type: DocumentType.ATTESTATION_FORMATION,
        label: 'Attestation de formation',
        required: true,
        maxSizeMB: 5,
        allowedTypes: ['application/pdf'],
        description: 'Attestation des heures de formation doctorales'
      }
    ];
    
    this.requiredDocumentsCount = this.documentConfigs.filter(config => config.required).length;
  }

  canSubmit(): boolean {
    const formValid = this.soutenanceForm.valid;
    const documentsComplete = this.uploadedDocuments.length >= this.requiredDocumentsCount;
    const prerequisMet = this.canSubmitBasedOnPrerequis;
    return formValid && documentsComplete && prerequisMet && !this.isSubmitting;
  }

  /**
   * Handler pour les changements de statut des prérequis
   */
  onPrerequisStatusChange(status: PrerequisStatus): void {
    console.log('📋 [SOUTENANCE FORM] Statut prérequis mis à jour:', status);
    this.prerequisStatus = status;
  }

  /**
   * Handler pour les changements de possibilité de soumission
   */
  onCanSubmitChange(canSubmit: boolean): void {
    console.log('📋 [SOUTENANCE FORM] Possibilité de soumission:', canSubmit);
    this.canSubmitBasedOnPrerequis = canSubmit;
    
    if (!canSubmit && this.prerequisStatus) {
      const missingPrerequis = this.soutenanceService.getMissingPrerequis(this.prerequisStatus);
      if (missingPrerequis.length > 0) {
        this.errorMessage = `Prérequis manquants: ${missingPrerequis.join(', ')}`;
      }
    } else {
      // Clear error message if prerequis are met
      if (this.errorMessage.includes('Prérequis manquants')) {
        this.errorMessage = '';
      }
    }
  }

  onSubmit(): void {
    if (!this.canSubmit()) return;

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    const formData = this.soutenanceForm.value;
    const request: SoutenanceRequest = {
      titrethese: formData.titrethese,
      resumeThese: formData.resumeThese,
      dateSoutenance: formData.dateSoutenance ? new Date(formData.dateSoutenance) : undefined,
      lieuSoutenance: formData.lieuSoutenance || undefined,
      specialite: formData.specialite,
      laboratoire: formData.laboratoire,
      jury: this.juryData // Include jury data
    };

    const operation = this.isEditMode && this.soutenanceId ?
      this.soutenanceService.updateDemandeSoutenance(this.soutenanceId, request) :
      this.soutenanceService.createDemandeSoutenance(request);

    operation.subscribe({
      next: (response) => {
        this.successMessage = this.isEditMode ? 
          'Demande de soutenance mise à jour avec succès' : 
          'Demande de soutenance créée avec succès';
        
        setTimeout(() => {
          this.router.navigate(['../'], { relativeTo: this.route });
        }, 2000);
      },
      error: (error) => {
        console.error('Erreur lors de la soumission:', error);
        this.errorMessage = error.error?.message || 'Erreur lors de la soumission de la demande';
        this.isSubmitting = false;
      }
    });
  }

  onSaveDraft(): void {
    if (!this.soutenanceForm.valid) return;

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    const formData = this.soutenanceForm.value;
    const request: SoutenanceRequest = {
      titrethese: formData.titrethese,
      resumeThese: formData.resumeThese,
      dateSoutenance: formData.dateSoutenance ? new Date(formData.dateSoutenance) : undefined,
      lieuSoutenance: formData.lieuSoutenance || undefined,
      specialite: formData.specialite,
      laboratoire: formData.laboratoire,
      jury: this.juryData // Include jury data for draft as well
    };

    const operation = this.isEditMode && this.soutenanceId ?
      this.soutenanceService.updateDemandeSoutenance(this.soutenanceId, request) :
      this.soutenanceService.createDemandeSoutenance(request);

    operation.subscribe({
      next: (response) => {
        this.successMessage = 'Brouillon sauvegardé avec succès';
        this.isSubmitting = false;
        
        if (!this.isEditMode) {
          // Redirect to edit mode for the newly created soutenance
          this.router.navigate(['../', response.id, 'edit'], { relativeTo: this.route });
        }
      },
      error: (error) => {
        console.error('Erreur lors de la sauvegarde:', error);
        this.errorMessage = error.error?.message || 'Erreur lors de la sauvegarde';
        this.isSubmitting = false;
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['../'], { relativeTo: this.route });
  }

  // Document handling methods
  onDocumentUploaded(document: DocumentResponse): void {
    const existingIndex = this.uploadedDocuments.findIndex(doc => doc.type === document.type);
    if (existingIndex >= 0) {
      this.uploadedDocuments[existingIndex] = document;
    } else {
      this.uploadedDocuments.push(document);
    }
    this.successMessage = 'Document téléchargé avec succès';
    setTimeout(() => this.successMessage = '', 3000);
  }

  onDocumentDeleted(documentId: number): void {
    this.uploadedDocuments = this.uploadedDocuments.filter(doc => doc.id !== documentId);
    this.successMessage = 'Document supprimé avec succès';
    setTimeout(() => this.successMessage = '', 3000);
  }

  onDocumentError(error: string): void {
    this.errorMessage = error;
    setTimeout(() => this.errorMessage = '', 5000);
  }

  // Helper methods for template
  getDocumentTypeLabel(type: DocumentType): string {
    const config = this.documentConfigs.find(c => c.type === type);
    return config?.label || type;
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  /**
   * Obtenir le tooltip pour le bouton de soumission
   */
  getSubmitButtonTooltip(): string {
    if (!this.soutenanceForm.valid) {
      return 'Veuillez remplir tous les champs obligatoires';
    }
    if (this.uploadedDocuments.length < this.requiredDocumentsCount) {
      return 'Veuillez télécharger tous les documents obligatoires';
    }
    if (!this.canSubmitBasedOnPrerequis) {
      return 'Tous les prérequis doivent être remplis avant la soumission';
    }
    return 'Soumettre la demande de soutenance';
  }

  /**
   * Initialize director information for jury management
   */
  private initializeDirecteurInfo(): void {
    const currentUser = this.authService.getCurrentUser();
    if (currentUser) {
      // In a real application, you would fetch the director info from the backend
      // For now, we'll use placeholder data
      this.directeurInfo = {
        nom: 'Directeur', // This should come from the backend
        prenom: 'Nom', // This should come from the backend
        etablissement: 'Université', // This should come from the backend
        grade: 'Professeur' // This should come from the backend
      };
    }
  }

  /**
   * Handle jury composition updates
   */
  onJuryUpdated(juryData: JuryMemberRequest[]): void {
    console.log('📋 [SOUTENANCE FORM] Jury mis à jour:', juryData);
    this.juryData = juryData;
    this.successMessage = 'Composition du jury sauvegardée avec succès';
    setTimeout(() => this.successMessage = '', 3000);
  }

  /**
   * Handle jury management cancellation
   */
  onJuryCancel(): void {
    console.log('📋 [SOUTENANCE FORM] Gestion du jury annulée');
    // Could hide the jury section or reset jury data if needed
  }

  /**
   * Check if jury section should be shown
   */
  private updateJurySectionVisibility(): void {
    const formData = this.soutenanceForm.value;
    this.showJurySection = !!(formData.titrethese && formData.specialite && formData.laboratoire);
  }
}