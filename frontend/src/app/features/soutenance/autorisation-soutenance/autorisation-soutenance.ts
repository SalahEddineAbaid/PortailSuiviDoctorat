import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { Router } from '@angular/router';

import { SoutenanceService } from '../../../core/services/soutenance.service';

@Component({
  selector: 'app-autorisation-soutenance',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatRadioModule,
    MatCardModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatCheckboxModule
  ],
  templateUrl: './autorisation-soutenance.html',
  styleUrls: ['./autorisation-soutenance.scss']
})
export class AutorisationSoutenanceComponent implements OnInit {
  @Input() defenseRequestId!: number;
  
  autorisationForm!: FormGroup;
  loading = false;
  submitting = false;
  defenseRequest: any = null;

  constructor(
    private fb: FormBuilder,
    private soutenanceService: SoutenanceService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.loadDefenseRequest();
  }

  private initializeForm(): void {
    this.autorisationForm = this.fb.group({
      statut: ['', Validators.required],
      prerequisValides: [false],
      juryComplet: [false],
      rapportsFavorables: [false],
      documentsComplets: [false],
      commentaireAdmin: [''],
      motifRefus: [''],
      dateSoutenance: [''],
      lieuSoutenance: [''],
      salleSoutenance: ['']
    });

    // Conditional validators
    this.autorisationForm.get('statut')?.valueChanges.subscribe(value => {
      const motifControl = this.autorisationForm.get('motifRefus');
      const dateControl = this.autorisationForm.get('dateSoutenance');
      const lieuControl = this.autorisationForm.get('lieuSoutenance');
      const salleControl = this.autorisationForm.get('salleSoutenance');

      if (value === 'REFUSE') {
        motifControl?.setValidators([Validators.required]);
        dateControl?.clearValidators();
        lieuControl?.clearValidators();
        salleControl?.clearValidators();
      } else if (value === 'AUTORISE') {
        motifControl?.clearValidators();
        dateControl?.setValidators([Validators.required]);
        lieuControl?.setValidators([Validators.required]);
        salleControl?.setValidators([Validators.required]);
      } else {
        motifControl?.clearValidators();
        dateControl?.clearValidators();
        lieuControl?.clearValidators();
        salleControl?.clearValidators();
      }

      motifControl?.updateValueAndValidity();
      dateControl?.updateValueAndValidity();
      lieuControl?.updateValueAndValidity();
      salleControl?.updateValueAndValidity();
    });
  }

  private loadDefenseRequest(): void {
    if (!this.defenseRequestId) return;

    this.loading = true;
    this.soutenanceService.getDefenseRequestById(this.defenseRequestId).subscribe({
      next: (data: any) => {
        this.defenseRequest = data;
        this.checkPrerequisites();
        this.loading = false;
      },
      error: () => {
        this.showError('Erreur lors du chargement de la demande');
        this.loading = false;
      }
    });
  }

  private checkPrerequisites(): void {
    if (!this.defenseRequest) return;

    const checks = {
      prerequisValides: this.defenseRequest.prerequisites?.isValid || false,
      juryComplet: this.defenseRequest.jury?.members?.length >= 3 || false,
      rapportsFavorables: this.checkRapportsFavorables(),
      documentsComplets: this.checkDocumentsComplets()
    };

    this.autorisationForm.patchValue(checks);
  }

  private checkRapportsFavorables(): boolean {
    if (!this.defenseRequest?.rapports) return false;
    return this.defenseRequest.rapports.every((r: any) => r.favorable);
  }

  private checkDocumentsComplets(): boolean {
    if (!this.defenseRequest?.prerequisites) return false;
    const prereq = this.defenseRequest.prerequisites;
    return prereq.manuscriptUploaded && 
           prereq.antiPlagiarismUploaded && 
           prereq.publicationsReportUploaded &&
           prereq.trainingCertsUploaded &&
           prereq.authorizationLetterUploaded;
  }

  get canAuthorize(): boolean {
    const form = this.autorisationForm.value;
    return form.prerequisValides && 
           form.juryComplet && 
           form.rapportsFavorables && 
           form.documentsComplets;
  }

  onSubmit(): void {
    if (!this.autorisationForm.valid) {
      this.showError('Veuillez remplir tous les champs obligatoires');
      return;
    }

    const statut = this.autorisationForm.value.statut;
    const message = statut === 'AUTORISE' 
      ? 'Êtes-vous sûr de vouloir autoriser cette soutenance ?'
      : 'Êtes-vous sûr de vouloir refuser cette soutenance ?';

    if (confirm(message)) {
      this.submitAutorisation();
    }
  }

  private submitAutorisation(): void {
    this.submitting = true;
    
    const statut = this.autorisationForm.value.statut;
    const status = statut === 'AUTORISE' ? 'AUTORISEE' : 'REJETEE';

    this.soutenanceService.updateDefenseRequestStatus(this.defenseRequestId, status).subscribe({
      next: () => {
        this.submitting = false;
        this.showSuccess('Autorisation enregistrée avec succès');
        setTimeout(() => {
          this.router.navigate(['/admin/soutenances']);
        }, 2000);
      },
      error: () => {
        this.submitting = false;
        this.showError('Erreur lors de l\'enregistrement');
      }
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.autorisationForm.get(fieldName);
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
