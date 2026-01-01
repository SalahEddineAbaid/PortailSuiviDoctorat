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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { Router } from '@angular/router';

import { DefenseService } from '../../../core/services/defense.service';

@Component({
  selector: 'app-proces-verbal',
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
    MatNativeDateModule,
    MatCheckboxModule
  ],
  templateUrl: './proces-verbal.html',
  styleUrls: ['./proces-verbal.scss']
})
export class ProcesVerbalComponent implements OnInit {
  @Input() defenseId!: number;
  
  procesVerbalForm!: FormGroup;
  loading = false;
  submitting = false;
  generating = false;
  defense: any = null;

  mentions = [
    { value: 'TRES_HONORABLE_AVEC_FELICITATIONS', label: 'Très Honorable avec Félicitations du Jury' },
    { value: 'TRES_HONORABLE', label: 'Très Honorable' },
    { value: 'HONORABLE', label: 'Honorable' },
    { value: 'PASSABLE', label: 'Passable' },
    { value: 'AJOURNE', label: 'Ajourné' }
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
    this.procesVerbalForm = this.fb.group({
      mention: ['', Validators.required],
      publicationRecommended: [false],
      juryComments: ['', [Validators.required, Validators.minLength(50)]],
      deliberationDate: [new Date(), Validators.required],
      presidentSignature: ['', Validators.required],
      rapporteurSignature: ['', Validators.required],
      examinateurSignatures: [''],
      observationsGenerales: [''],
      recommandationsPublication: [''],
      correctionsRequises: [''],
      delaiCorrections: ['']
    });

    // Conditional validators
    this.procesVerbalForm.get('publicationRecommended')?.valueChanges.subscribe(value => {
      const recommandationsControl = this.procesVerbalForm.get('recommandationsPublication');
      if (value) {
        recommandationsControl?.setValidators([Validators.required]);
      } else {
        recommandationsControl?.clearValidators();
      }
      recommandationsControl?.updateValueAndValidity();
    });

    this.procesVerbalForm.get('mention')?.valueChanges.subscribe(value => {
      const correctionsControl = this.procesVerbalForm.get('correctionsRequises');
      const delaiControl = this.procesVerbalForm.get('delaiCorrections');
      
      if (value === 'AJOURNE' || value === 'PASSABLE') {
        correctionsControl?.setValidators([Validators.required]);
        delaiControl?.setValidators([Validators.required]);
      } else {
        correctionsControl?.clearValidators();
        delaiControl?.clearValidators();
      }
      correctionsControl?.updateValueAndValidity();
      delaiControl?.updateValueAndValidity();
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

    if (this.defense.procesVerbal) {
      this.procesVerbalForm.patchValue(this.defense.procesVerbal);
    }
  }

  onSubmit(): void {
    if (!this.procesVerbalForm.valid) {
      this.showError('Veuillez remplir tous les champs obligatoires');
      return;
    }

    if (confirm('Êtes-vous sûr de vouloir enregistrer ce procès-verbal ? Cette action est définitive.')) {
      this.submitProcesVerbal();
    }
  }

  private submitProcesVerbal(): void {
    this.submitting = true;
    
    const procesVerbalData = {
      defenseId: this.defenseId,
      ...this.procesVerbalForm.value
    };

    this.defenseService.submitProcesVerbal(procesVerbalData).subscribe({
      next: () => {
        this.submitting = false;
        this.showSuccess('Procès-verbal enregistré avec succès');
        setTimeout(() => {
          this.router.navigate(['/soutenance', this.defenseId]);
        }, 2000);
      },
      error: () => {
        this.submitting = false;
        this.showError('Erreur lors de l\'enregistrement');
      }
    });
  }

  generatePDF(): void {
    if (!this.procesVerbalForm.valid) {
      this.showError('Veuillez remplir tous les champs avant de générer le PDF');
      return;
    }

    this.generating = true;
    
    const procesVerbalData = {
      defenseId: this.defenseId,
      ...this.procesVerbalForm.value
    };

    this.defenseService.generateProcesVerbalPDF(procesVerbalData).subscribe({
      next: (blob) => {
        this.generating = false;
        this.downloadPDF(blob);
        this.showSuccess('PDF généré avec succès');
      },
      error: () => {
        this.generating = false;
        this.showError('Erreur lors de la génération du PDF');
      }
    });
  }

  private downloadPDF(blob: Blob): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `proces-verbal-${this.defenseId}.pdf`;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.procesVerbalForm.get(fieldName);
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
