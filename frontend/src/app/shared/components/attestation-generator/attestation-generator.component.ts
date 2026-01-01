import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { BehaviorSubject, Subject, Observable } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';

import { DocumentService } from '../../../core/services/document.service';
import { InscriptionService } from '../../../core/services/inscription.service';
import { AuthService } from '../../../core/services/auth.service';
import { InscriptionResponse } from '../../../core/models/inscription.model';

export interface AttestationRequest {
  inscriptionId: number;
  type: AttestationType;
  langue?: 'FR' | 'EN';
  motif?: string;
  destinataire?: string;
}

export enum AttestationType {
  INSCRIPTION = 'INSCRIPTION',
  SCOLARITE = 'SCOLARITE',
  PRESENCE = 'PRESENCE',
  REUSSITE = 'REUSSITE'
}

export interface AttestationResponse {
  id: number;
  type: AttestationType;
  dateGeneration: Date;
  documentUrl: string;
  fileName: string;
}

@Component({
  selector: 'app-attestation-generator',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule, MatProgressSpinnerModule, MatButtonModule],
  templateUrl: './attestation-generator.component.html',
  styleUrls: ['./attestation-generator.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AttestationGeneratorComponent implements OnInit {
  @Input() inscriptionId?: number;
  @Input() showInscriptionSelector = true;
  
  @Output() attestationGenerated = new EventEmitter<AttestationResponse>();
  @Output() generationError = new EventEmitter<string>();

  private destroy$ = new Subject<void>();
  private generatingSubject = new BehaviorSubject<boolean>(false);
  private errorSubject = new BehaviorSubject<string | null>(null);
  private successSubject = new BehaviorSubject<string | null>(null);
  
  generating$ = this.generatingSubject.asObservable();
  error$ = this.errorSubject.asObservable();
  success$ = this.successSubject.asObservable();

  attestationForm: FormGroup;
  inscriptions: InscriptionResponse[] = [];
  attestationTypes = AttestationType;
  
  // Type labels for display
  typeLabels: { [key: string]: string } = {
    [AttestationType.INSCRIPTION]: 'Attestation d\'inscription',
    [AttestationType.SCOLARITE]: 'Certificat de scolarité',
    [AttestationType.PRESENCE]: 'Attestation de présence',
    [AttestationType.REUSSITE]: 'Attestation de réussite'
  };

  constructor(
    private fb: FormBuilder,
    private documentService: DocumentService,
    private inscriptionService: InscriptionService,
    private authService: AuthService
  ) {
    this.attestationForm = this.createForm();
  }

  ngOnInit(): void {
    this.loadInscriptions();
    this.setupFormValidation();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private createForm(): FormGroup {
    return this.fb.group({
      inscriptionId: [this.inscriptionId || '', Validators.required],
      type: [AttestationType.INSCRIPTION, Validators.required],
      langue: ['FR', Validators.required],
      motif: [''],
      destinataire: ['']
    });
  }

  private setupFormValidation(): void {
    // Add conditional validators based on attestation type
    this.attestationForm.get('type')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(type => {
        const motifControl = this.attestationForm.get('motif');
        const destinataireControl = this.attestationForm.get('destinataire');
        
        if (type === AttestationType.PRESENCE || type === AttestationType.REUSSITE) {
          motifControl?.setValidators([Validators.required]);
          destinataireControl?.setValidators([Validators.required]);
        } else {
          motifControl?.clearValidators();
          destinataireControl?.clearValidators();
        }
        
        motifControl?.updateValueAndValidity();
        destinataireControl?.updateValueAndValidity();
      });
  }

  private loadInscriptions(): void {
    if (!this.showInscriptionSelector && this.inscriptionId) {
      return;
    }

    // Use getInscriptionsDoctorant with current user ID
    // For now, we'll use a placeholder approach
    this.authService.currentUser$.subscribe(user => {
      if (user) {
        this.inscriptionService.getInscriptionsDoctorant(user.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (inscriptions: InscriptionResponse[]) => {
              this.inscriptions = inscriptions;
              
              // Auto-select if only one inscription
              if (inscriptions.length === 1) {
                this.attestationForm.patchValue({ inscriptionId: inscriptions[0].id });
              }
            },
            error: (error: any) => {
              console.error('Erreur chargement inscriptions:', error);
              this.errorSubject.next('Impossible de charger les inscriptions');
            }
          });
      }
    });
  }

  onGenerate(): void {
    if (this.attestationForm.invalid || this.generatingSubject.value) {
      this.markFormGroupTouched();
      return;
    }

    this.generatingSubject.next(true);
    this.clearMessages();

    const request: AttestationRequest = this.attestationForm.value;

    // Call backend API to generate attestation
    this.generateAttestation(request)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.generatingSubject.next(false))
      )
      .subscribe({
        next: (response) => {
          this.successSubject.next('Attestation générée avec succès');
          this.attestationGenerated.emit(response);
          this.downloadAttestation(response);
        },
        error: (error) => {
          const errorMessage = this.getErrorMessage(error);
          this.errorSubject.next(errorMessage);
          this.generationError.emit(errorMessage);
          console.error('Erreur génération attestation:', error);
        }
      });
  }

  private generateAttestation(request: AttestationRequest) {
    // TODO: Implement attestation generation via backend API
    // For now, return an observable that simulates the API call
    return new Observable<AttestationResponse>(observer => {
      observer.error({ status: 501, message: 'Fonctionnalité non implémentée' });
    });
  }

  private downloadAttestation(attestation: AttestationResponse): void {
    // Auto-download the generated attestation
    const link = document.createElement('a');
    link.href = attestation.documentUrl;
    link.download = attestation.fileName;
    link.style.display = 'none';
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  private markFormGroupTouched(): void {
    Object.keys(this.attestationForm.controls).forEach(key => {
      const control = this.attestationForm.get(key);
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
        return 'Inscription non trouvée';
      case 403:
        return 'Accès non autorisé';
      case 500:
        return 'Erreur serveur lors de la génération';
      default:
        return 'Erreur lors de la génération de l\'attestation';
    }
  }

  onReset(): void {
    this.attestationForm.reset();
    this.attestationForm.patchValue({
      inscriptionId: this.inscriptionId || '',
      type: AttestationType.INSCRIPTION,
      langue: 'FR'
    });
    this.clearMessages();
  }

  // Helper methods for template
  get selectedInscription(): InscriptionResponse | null {
    const inscriptionId = this.attestationForm.get('inscriptionId')?.value;
    return this.inscriptions.find(i => i.id === inscriptionId) || null;
  }

  get selectedType(): AttestationType {
    return this.attestationForm.get('type')?.value;
  }

  get typeLabel(): string {
    return this.typeLabels[this.selectedType] || '';
  }

  get requiresMotif(): boolean {
    const type = this.selectedType;
    return type === AttestationType.PRESENCE || type === AttestationType.REUSSITE;
  }

  get canGenerate(): boolean {
    return this.attestationForm.valid && !this.generatingSubject.value;
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.attestationForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  getFieldError(fieldName: string): string {
    const field = this.attestationForm.get(fieldName);
    if (field?.errors) {
      if (field.errors['required']) {
        return 'Ce champ est obligatoire';
      }
    }
    return '';
  }
}