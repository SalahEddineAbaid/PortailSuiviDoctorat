import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { BehaviorSubject, Subject, Observable } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';

import { DocumentService } from '../../../core/services/document.service';
import { SoutenanceService } from '../../../core/services/soutenance.service';
import { SoutenanceResponse, SoutenanceStatus, JuryMember } from '../../../core/models/soutenance.model';

export interface ProcesVerbalRequest {
  soutenanceId: number;
  dateSoutenance: Date;
  heureSoutenance: string;
  lieuSoutenance: string;
  resultat: ResultatSoutenance;
  mention?: MentionSoutenance;
  observations?: string;
  recommandations?: string;
  jury: JuryMember[];
}

export enum ResultatSoutenance {
  ADMIS = 'ADMIS',
  AJOURNE = 'AJOURNE',
  REFUSE = 'REFUSE'
}

export enum MentionSoutenance {
  TRES_HONORABLE = 'TRES_HONORABLE',
  TRES_HONORABLE_FELICITATIONS = 'TRES_HONORABLE_FELICITATIONS',
  HONORABLE = 'HONORABLE',
  PASSABLE = 'PASSABLE'
}

export interface ProcesVerbalResponse {
  id: number;
  soutenanceId: number;
  numeroProcesVerbal: string;
  dateGeneration: Date;
  documentUrl: string;
  fileName: string;
  signe: boolean;
}

@Component({
  selector: 'app-proces-verbal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule, MatProgressSpinnerModule, MatButtonModule],
  templateUrl: './proces-verbal.component.html',
  styleUrls: ['./proces-verbal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProcesVerbalComponent implements OnInit {
  @Input() soutenanceId?: number;
  @Input() showSoutenanceSelector = true;
  @Input() readOnly = false;
  
  @Output() procesVerbalGenerated = new EventEmitter<ProcesVerbalResponse>();
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

  procesVerbalForm: FormGroup;
  soutenances: SoutenanceResponse[] = [];
  selectedSoutenance: SoutenanceResponse | null = null;
  
  // Enums for template
  resultats = ResultatSoutenance;
  mentions = MentionSoutenance;
  
  // Labels for display
  resultatLabels: { [key: string]: string } = {
    [ResultatSoutenance.ADMIS]: 'Admis',
    [ResultatSoutenance.AJOURNE]: 'Ajourné',
    [ResultatSoutenance.REFUSE]: 'Refusé'
  };
  
  mentionLabels: { [key: string]: string } = {
    [MentionSoutenance.TRES_HONORABLE]: 'Très honorable',
    [MentionSoutenance.TRES_HONORABLE_FELICITATIONS]: 'Très honorable avec félicitations du jury',
    [MentionSoutenance.HONORABLE]: 'Honorable',
    [MentionSoutenance.PASSABLE]: 'Passable'
  };

  constructor(
    private fb: FormBuilder,
    private documentService: DocumentService,
    private soutenanceService: SoutenanceService
  ) {
    this.procesVerbalForm = this.createForm();
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
      soutenanceId: [{ value: this.soutenanceId || '', disabled: this.readOnly }, Validators.required],
      dateSoutenance: [{ value: '', disabled: this.readOnly }, Validators.required],
      heureSoutenance: [{ value: '', disabled: this.readOnly }, Validators.required],
      lieuSoutenance: [{ value: '', disabled: this.readOnly }, [Validators.required, Validators.minLength(5)]],
      resultat: [{ value: '', disabled: this.readOnly }, Validators.required],
      mention: [{ value: '', disabled: this.readOnly }],
      observations: [{ value: '', disabled: this.readOnly }],
      recommandations: [{ value: '', disabled: this.readOnly }]
    });
  }

  private setupFormSubscriptions(): void {
    // Watch for soutenance selection changes
    this.procesVerbalForm.get('soutenanceId')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(soutenanceId => {
        this.selectedSoutenance = this.soutenances.find(s => s.id === soutenanceId) || null;
        this.prefillFormFromSoutenance();
        this.clearMessages();
      });

    // Watch for resultat changes to manage mention field
    this.procesVerbalForm.get('resultat')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(resultat => {
        const mentionControl = this.procesVerbalForm.get('mention');
        
        if (resultat === ResultatSoutenance.ADMIS) {
          mentionControl?.setValidators([Validators.required]);
        } else {
          mentionControl?.clearValidators();
          mentionControl?.setValue('');
        }
        
        mentionControl?.updateValueAndValidity();
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
          // Filter soutenances that can have proces-verbal generated
          this.soutenances = soutenances.filter(s => 
            s.statut === SoutenanceStatus.AUTORISEE ||
            s.statut === SoutenanceStatus.SOUTENUE
          );
          
          // Auto-select if only one soutenance
          if (this.soutenances.length === 1) {
            this.procesVerbalForm.patchValue({ soutenanceId: this.soutenances[0].id });
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
          this.procesVerbalForm.patchValue({ soutenanceId: soutenance.id });
        },
        error: (error) => {
          console.error('Erreur chargement soutenance:', error);
          this.errorSubject.next('Impossible de charger la soutenance');
        }
      });
  }

  private prefillFormFromSoutenance(): void {
    if (!this.selectedSoutenance) return;

    const soutenance = this.selectedSoutenance;
    
    // Prefill with existing soutenance data
    const updates: any = {};
    
    if (soutenance.dateSoutenance) {
      updates.dateSoutenance = new Date(soutenance.dateSoutenance).toISOString().split('T')[0];
    }
    
    if (soutenance.lieuSoutenance) {
      updates.lieuSoutenance = soutenance.lieuSoutenance;
    }

    this.procesVerbalForm.patchValue(updates);
  }

  onGenerate(): void {
    if (this.procesVerbalForm.invalid || this.generatingSubject.value) {
      this.markFormGroupTouched();
      return;
    }

    if (!this.selectedSoutenance) {
      this.errorSubject.next('Aucune soutenance sélectionnée');
      return;
    }

    this.generatingSubject.next(true);
    this.clearMessages();

    const formValue = this.procesVerbalForm.value;
    const request: ProcesVerbalRequest = {
      ...formValue,
      dateSoutenance: new Date(formValue.dateSoutenance + 'T' + formValue.heureSoutenance),
      jury: this.selectedSoutenance.jury || []
    };

    // Call backend API to generate proces-verbal
    this.generateProcesVerbal(request)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.generatingSubject.next(false))
      )
      .subscribe({
        next: (response) => {
          this.successSubject.next('Procès-verbal généré avec succès');
          this.procesVerbalGenerated.emit(response);
          this.downloadProcesVerbal(response);
        },
        error: (error) => {
          const errorMessage = this.getErrorMessage(error);
          this.errorSubject.next(errorMessage);
          this.generationError.emit(errorMessage);
          console.error('Erreur génération procès-verbal:', error);
        }
      });
  }

  private generateProcesVerbal(request: ProcesVerbalRequest) {
    // TODO: Implement proces-verbal generation via backend API
    return new Observable<ProcesVerbalResponse>(observer => {
      observer.error({ status: 501, message: 'Fonctionnalité non implémentée' });
    });
  }

  private downloadProcesVerbal(procesVerbal: ProcesVerbalResponse): void {
    // Auto-download the generated proces-verbal
    const link = document.createElement('a');
    link.href = procesVerbal.documentUrl;
    link.download = procesVerbal.fileName;
    link.style.display = 'none';
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  private markFormGroupTouched(): void {
    Object.keys(this.procesVerbalForm.controls).forEach(key => {
      const control = this.procesVerbalForm.get(key);
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
        return 'La soutenance n\'est pas dans un état valide pour générer le procès-verbal';
      case 500:
        return 'Erreur serveur lors de la génération';
      default:
        return 'Erreur lors de la génération du procès-verbal';
    }
  }

  onReset(): void {
    this.procesVerbalForm.reset();
    this.procesVerbalForm.patchValue({
      soutenanceId: this.soutenanceId || ''
    });
    this.clearMessages();
  }

  // Helper methods for template
  get canGenerate(): boolean {
    return this.procesVerbalForm.valid && 
           !this.generatingSubject.value && 
           !this.loadingSubject.value &&
           !!this.selectedSoutenance &&
           !this.readOnly;
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

  get isValidForProcesVerbal(): boolean {
    if (!this.selectedSoutenance) return false;
    
    return this.selectedSoutenance.statut === SoutenanceStatus.AUTORISEE ||
           this.selectedSoutenance.statut === SoutenanceStatus.SOUTENUE;
  }

  get requiresMention(): boolean {
    return this.procesVerbalForm.get('resultat')?.value === ResultatSoutenance.ADMIS;
  }

  get juryMembers(): JuryMember[] {
    return this.selectedSoutenance?.jury || [];
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.procesVerbalForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  getFieldError(fieldName: string): string {
    const field = this.procesVerbalForm.get(fieldName);
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