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
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatSelectModule } from '@angular/material/select';
import { Router } from '@angular/router';

import { SoutenanceService } from '../../../core/services/soutenance.service';

@Component({
  selector: 'app-avis-form',
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
    MatProgressBarModule,
    MatDividerModule,
    MatSelectModule
  ],
  templateUrl: './avis-form.html',
  styleUrls: ['./avis-form.scss']
})
export class AvisFormComponent implements OnInit {
  @Input() defenseRequestId!: number;
  @Input() juryMemberId!: number;
  
  avisForm!: FormGroup;
  loading = false;
  submitting = false;
  uploadProgress = 0;
  uploadedFile: any = null;

  constructor(
    private fb: FormBuilder,
    private soutenanceService: SoutenanceService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.loadExistingAvis();
  }

  private initializeForm(): void {
    this.avisForm = this.fb.group({
      favorable: ['', Validators.required],
      commentaires: ['', [Validators.required, Validators.minLength(50)]],
      recommandations: [''],
      pointsForts: ['', Validators.required],
      pointsAmeliorer: [''],
      qualiteRecherche: ['', Validators.required],
      originalite: ['', Validators.required],
      methodologie: ['', Validators.required],
      redaction: ['', Validators.required]
    });
  }

  private loadExistingAvis(): void {
    if (!this.defenseRequestId || !this.juryMemberId) return;

    // TODO: Implement rapport loading via backend API
    this.loading = false;
  }

  private populateForm(rapport: any): void {
    this.avisForm.patchValue({
      favorable: rapport.favorable,
      commentaires: rapport.commentaires,
      recommandations: rapport.recommandations,
      pointsForts: rapport.pointsForts,
      pointsAmeliorer: rapport.pointsAmeliorer,
      qualiteRecherche: rapport.qualiteRecherche,
      originalite: rapport.originalite,
      methodologie: rapport.methodologie,
      redaction: rapport.redaction
    });
    
    if (rapport.reportUrl) {
      this.uploadedFile = { url: rapport.reportUrl };
    }
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (!file) return;

    if (file.type !== 'application/pdf') {
      this.showError('Seuls les fichiers PDF sont acceptés');
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      this.showError('Le fichier ne doit pas dépasser 10 Mo');
      return;
    }

    this.uploadFile(file);
  }

  private uploadFile(file: File): void {
    this.uploadProgress = 0;
    
    // TODO: Implement file upload via backend API
    // For now, simulate upload
    this.uploadProgress = 100;
    this.uploadedFile = { url: URL.createObjectURL(file), name: file.name };
    this.showSuccess('Rapport uploadé avec succès');
  }

  removeFile(): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce fichier ?')) {
      this.uploadedFile = null;
      this.uploadProgress = 0;
    }
  }

  onSubmit(): void {
    if (!this.avisForm.valid) {
      this.showError('Veuillez remplir tous les champs obligatoires');
      return;
    }

    if (!this.uploadedFile) {
      this.showError('Veuillez uploader votre rapport PDF');
      return;
    }

    if (confirm('Êtes-vous sûr de vouloir soumettre votre avis ? Cette action est définitive.')) {
      this.submitAvis();
    }
  }

  private submitAvis(): void {
    this.submitting = true;
    
    // TODO: Implement rapport submission via backend API
    // For now, show success message
    this.submitting = false;
    this.showSuccess('Avis soumis avec succès');
    setTimeout(() => {
      this.router.navigate(['/soutenance']);
    }, 2000);
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.avisForm.get(fieldName);
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
