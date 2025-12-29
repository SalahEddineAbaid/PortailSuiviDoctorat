import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { BehaviorSubject, Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';

import { DocumentService } from '../../../core/services/document.service';
import { SoutenanceService } from '../../../core/services/soutenance.service';
import { SoutenanceResponse, SoutenanceStatus } from '../../../core/models/soutenance.model';

export interface AutorisationRequest {
  soutenanceId: number;
  dateProposee: Date;
  lieuPropose: string;
  heureProposee: string;
  commentaires?: string;
}

export interface AutorisationResponse {
  id: number;
  soutenanceId: number;
  numeroAutorisation: string;
  dateGeneration: Date;
  documentUrl: string;
  fileName: string;
  valide: boolean;
}

@Component({
  selector: 'app-autorisation-soutenance',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './autorisation-soutenance.component.html',
  styleUrls: ['./autorisation-soutenance.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AutorisationSoutenanceComponent implements OnInit {
  @Input() soutenanceId?: number;
  @Input() showSoutenanceSelector = true;
  
  @Output() autorisationGenerated = new EventEmitter<AutorisationResponse>();
  @Output() generationError = new EventEmitter<string>();

  private destroy$ = new Subject<void>();
  private generatingSubject = new BehaviorSubject<boolean>(false);
  private loadingSubject = new BehaviorSubject<boolean>(false);
  private errorSubject = new BehaviorSubject<string | null>(null);
  private successSubject = new BehaviorSubject<string | null>(null);
  
  generating$ = this.generatingSubject.asObservable();
  loading$ = this.loadingSubject.asObservable();
  error$ = this.errorSubject.asObservable();
  success$ = this.successSubject.asObservable();

  autorisationForm: FormGroup;
  soutenances: SoutenanceResponse[] = [];
  selectedSoutenance: SoutenanceResponse | null = null;
  
  // Minimum date (today)
  minDate = new Date().toISOString().split('T')[0];

  constructor(
    private fb: FormBuilder,
    private documentService: DocumentService,
    private soutenanceService: SoutenanceService
  ) {
    this.autorisationForm = this.createForm();
  }

  ngOnInit(): void {
    this.loadSoutenances();
    this.setupFormSubscriptions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private createForm(): FormGroup {
    return this.fb.group({
      soutenanceId: [this.soutenanceId || '', Validators.required],
      dateProposee: ['', Validators.required],
      heureProposee: ['', Validators.required],
      lieuPropose: ['', [Validators.required, Validators.minLength(5)]],
      commentaires: ['']
    });
  }

  private setupFormSubscriptions(): void {
    // Watch for soutenance selection changes
    this.autorisationForm.get('soutenanceId')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(soutenanceId => {
        this.selectedSoutenance = this.soutenances.find(s => s.id === soutenanceId) || null;
        this.clearMessages();
      });
  }

  private loadSoutenances(): void {
    if (!this.showSoutenanceSelector && this.soutenanceId) {
      this.loadSingleSoutenance(this.soutenanceId);
      return;
    }

    this.loadingSubject.next(true);
    
    this.soutenanceService.getMySoutenances()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loadingSubject.next(false))
      )
      .subscribe({
        next: (soutenances) => {
          // Filter soutenances that can have autorisation generated
          this.soutenances = soutenances.filter(s => 
            s.statut === SoutenanceStatus.EN_COURS_VALIDATION ||
            s.statut === SoutenanceStatus.AUTORISEE
          );
          
          // Auto-select if only one soutenance
          if (this.soutenances.length === 1) {
            this.autorisationForm.patchValue({ soutenanceId: this.soutenances[0].id });
          }
        },
        error: (error) => {
          console.error('Erreur chargement soutenances:', error);
          this.errorSubject.next('Impossible de charger les soutenances');
        }
      });
  }

  private loadSingleSoutenance(id: number): void {
    this.loadingSubject.next(true);
    
    this.soutenanceService.getSoutenance(id)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loadingSubject.next(false))
      )
      .subscribe({
        next: (soutenance) => {
          this.soutenances = [soutenance];
          this.selectedSoutenance = soutenance;
          this.autorisationForm.patchValue({ soutenanceId: soutenance.id });
        },
        error: (error) => {
          console.error('Erreur chargement soutenance:', error);
          this.errorSubject.next('Impossible de charger la soutenance');
        }
      });
  }

  onGenerate(): void {
    if (this.autorisationForm.invalid || this.generatingSubject.value) {
      this.markFormGroupTouched();
      return;
    }

    if (!this.selectedSoutenance) {
      this.errorSubject.next('Aucune soutenance sélectionnée');
      return;
    }

    this.generatingSubject.next(true);
    this.clearMessages();

    const request: AutorisationRequest = {
      ...this.autorisationForm.value,
      dateProposee: new Date(this.autorisationForm.value.dateProposee + 'T' + this.autorisationForm.value.heureProposee)
    };

    // Call backend API to generate autorisation
    this.generateAutorisation(request)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.generatingSubject.next(false))
      )
      .subscribe({
        next: (response) => {
          this.successSubject.next('Autorisation de soutenance générée avec succès');
          this.autorisationGenerated.emit(response);
          this.downloadAutorisation(response);
        },
        error: (error) => {
          const errorMessage = this.getErrorMessage(error);
          this.errorSubject.next(errorMessage);
          this.generationError.emit(errorMessage);
          console.error('Erreur génération autorisation:', error);
        }
      });
  }

  private generateAutorisation(request: AutorisationRequest) {
    // This would be a call to the backend API
    // For now, we'll simulate the API call
    return this.documentService.generateAutorisationSoutenance(request);
  }

  private downloadAutorisation(autorisation: AutorisationResponse): void {
    // Auto-download the generated autorisation
    const link = document.createElement('a');
    link.href = autorisation.documentUrl;
    link.download = autorisation.fileName;
    link.style.display = 'none';
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  private markFormGroupTouched(): void {
    Object.keys(this.autorisationForm.controls).forEach(key => {
      const control = this.autorisationForm.get(key);
      control?.markAsTouched();
    });
  }

  private clearMessages(): void {
    this.errorSubject.next(null);
    this.successSubject.next(null);
  }

  private getErrorMessage(error: any): string {
    if (error.error?.message) {
      return error.error.message;
    }
    
    switch (error.status) {
      case 400:
        return 'Données invalides pour la génération';
      case 404:
        return 'Soutenance non trouvée';
      case 403:
        return 'Accès non autorisé';
      case 409:
        return 'La soutenance n\'est pas dans un état valide pour générer l\'autorisation';
      case 500:
        return 'Erreur serveur lors de la génération';
      default:
        return 'Erreur lors de la génération de l\'autorisation';
    }
  }

  onReset(): void {
    this.autorisationForm.reset();
    this.autorisationForm.patchValue({
      soutenanceId: this.soutenanceId || ''
    });
    this.clearMessages();
  }

  // Helper methods for template
  get canGenerate(): boolean {
    return this.autorisationForm.valid && 
           !this.generatingSubject.value && 
           !this.loadingSubject.value &&
           !!this.selectedSoutenance;
  }

  get soutenanceStatusLabel(): string {
    if (!this.selectedSoutenance) return '';
    
    const labels = {
      [SoutenanceStatus.BROUILLON]: 'Brouillon',
      [SoutenanceStatus.SOUMISE]: 'Soumise',
      [SoutenanceStatus.EN_COURS_VALIDATION]: 'En cours de validation',
      [SoutenanceStatus.AUTORISEE]: 'Autorisée',
      [SoutenanceStatus.REJETEE]: 'Rejetée',
      [SoutenanceStatus.SOUTENUE]: 'Soutenue'
    };
    
    return labels[this.selectedSoutenance.statut] || this.selectedSoutenance.statut;
  }

  get isValidForAutorisation(): boolean {
    if (!this.selectedSoutenance) return false;
    
    return this.selectedSoutenance.statut === SoutenanceStatus.EN_COURS_VALIDATION ||
           this.selectedSoutenance.statut === SoutenanceStatus.AUTORISEE;
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.autorisationForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  getFieldError(fieldName: string): string {
    const field = this.autorisationForm.get(fieldName);
    if (field?.errors) {
      if (field.errors['required']) {
        return 'Ce champ est obligatoire';
      }
      if (field.errors['minlength']) {
        return `Minimum ${field.errors['minlength'].requiredLength} caractères`;
      }
    }
    return '';
  }
}