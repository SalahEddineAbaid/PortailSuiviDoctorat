import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { Router } from '@angular/router';

import { DefenseService } from '../../../core/services/defense.service';

@Component({
  selector: 'app-attestation-generator',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatCardModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  templateUrl: './attestation-generator.html',
  styleUrls: ['./attestation-generator.scss']
})
export class AttestationGeneratorComponent implements OnInit {
  @Input() defenseId!: number;
  
  attestationForm!: FormGroup;
  loading = false;
  generating = false;
  defense: any = null;

  attestationTypes = [
    { value: 'REUSSITE', label: 'Attestation de Réussite' },
    { value: 'INSCRIPTION', label: 'Attestation d\'Inscription' },
    { value: 'SOUTENANCE', label: 'Attestation de Soutenance' },
    { value: 'PRESENCE', label: 'Attestation de Présence' }
  ];

  constructor(
    private fb: FormBuilder,
    private defenseService: DefenseService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.loadDefense();
  }

  private initializeForm(): void {
    this.attestationForm = this.fb.group({
      type: ['REUSSITE', Validators.required],
      doctorantNom: ['', Validators.required],
      doctorantPrenom: ['', Validators.required],
      cin: ['', Validators.required],
      titreThese: ['', Validators.required],
      discipline: ['', Validators.required],
      dateSoutenance: ['', Validators.required],
      mention: [''],
      etablissement: ['EMSI', Validators.required],
      directeurThese: ['', Validators.required],
      numeroAttestation: ['', Validators.required],
      dateEmission: [new Date(), Validators.required],
      signataire: ['', Validators.required],
      qualiteSignataire: ['', Validators.required]
    });

    // Generate attestation number
    this.generateAttestationNumber();
  }

  private generateAttestationNumber(): void {
    const year = new Date().getFullYear();
    const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0');
    this.attestationForm.patchValue({
      numeroAttestation: `ATT-${year}-${random}`
    });
  }

  private loadDefense(): void {
    if (!this.defenseId) return;

    this.loading = true;
    this.defenseService.getDefense(this.defenseId).subscribe({
      next: (data) => {
        this.defense = data;
        this.populateForm();
        this.loading = false;
      },
      error: () => {
        this.showError('Erreur lors du chargement de la soutenance');
        this.loading = false;
      }
    });
  }

  private populateForm(): void {
    if (!this.defense) return;

    this.attestationForm.patchValue({
      doctorantNom: this.defense.doctorant?.lastName,
      doctorantPrenom: this.defense.doctorant?.firstName,
      cin: this.defense.doctorant?.cin,
      titreThese: this.defense.defenseRequest?.titreThese,
      discipline: this.defense.defenseRequest?.discipline,
      dateSoutenance: this.defense.defenseDate,
      mention: this.defense.mention,
      directeurThese: `${this.defense.directeur?.firstName} ${this.defense.directeur?.lastName}`
    });
  }

  generateAttestation(): void {
    if (!this.attestationForm.valid) {
      this.showError('Veuillez remplir tous les champs obligatoires');
      return;
    }

    this.generating = true;
    
    const attestationData = {
      defenseId: this.defenseId,
      ...this.attestationForm.value
    };

    this.defenseService.generateAttestation(attestationData).subscribe({
      next: (blob) => {
        this.generating = false;
        this.downloadPDF(blob);
        this.showSuccess('Attestation générée avec succès');
      },
      error: () => {
        this.generating = false;
        this.showError('Erreur lors de la génération de l\'attestation');
      }
    });
  }

  private downloadPDF(blob: Blob): void {
    const type = this.attestationForm.value.type;
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `attestation-${type.toLowerCase()}-${this.defenseId}.pdf`;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  previewAttestation(): void {
    if (!this.attestationForm.valid) {
      this.showError('Veuillez remplir tous les champs pour prévisualiser');
      return;
    }

    // Open preview in new window
    const attestationData = {
      defenseId: this.defenseId,
      ...this.attestationForm.value
    };

    this.defenseService.previewAttestation(attestationData).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: () => {
        this.showError('Erreur lors de la prévisualisation');
      }
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.attestationForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
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
