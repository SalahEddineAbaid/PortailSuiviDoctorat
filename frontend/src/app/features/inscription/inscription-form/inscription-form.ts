import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Observable, combineLatest } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

import { InscriptionService } from '../../../core/services/inscription.service';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { CustomValidators } from '../../../core/validators/custom-validators';

import { 
  InscriptionRequest, 
  CampagneResponse, 
  InscriptionResponse 
} from '../../../core/models/inscription.model';
import { UserResponse } from '../../../core/models/user.model';

@Component({
  selector: 'app-inscription-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './inscription-form.html',
  styleUrls: ['./inscription-form.scss']
})
export class InscriptionForm implements OnInit {
  inscriptionForm!: FormGroup;
  campagneActive$!: Observable<CampagneResponse | null>;
  directeurs$!: Observable<UserResponse[]>;
  filteredDirecteurs$!: Observable<UserResponse[]>;
  
  loading = false;
  error: string | null = null;
  success: string | null = null;
  isEditMode = false;
  inscriptionId: number | null = null;

  // Predefined options
  laboratoires = [
    'Laboratoire d\'Informatique',
    'Laboratoire de Mathématiques',
    'Laboratoire de Physique',
    'Laboratoire de Chimie',
    'Laboratoire de Biologie',
    'Laboratoire d\'Électronique',
    'Laboratoire de Mécanique',
    'Laboratoire de Génie Civil',
    'Autre'
  ];

  specialites = [
    'Informatique',
    'Mathématiques',
    'Physique',
    'Chimie',
    'Biologie',
    'Électronique',
    'Mécanique',
    'Génie Civil',
    'Sciences de l\'Ingénieur',
    'Autre'
  ];

  constructor(
    private fb: FormBuilder,
    private inscriptionService: InscriptionService,
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.initializeForm();
  }

  ngOnInit(): void {
    this.loadFormData();
    this.setupDirecteurFilter();
    this.checkEditMode();
  }

  private initializeForm(): void {
    this.inscriptionForm = this.fb.group({
      directeurId: ['', [Validators.required]],
      directeurSearch: [''], // For search functionality
      campagneId: ['', [Validators.required]],
      sujetThese: ['', [
        Validators.required,
        Validators.minLength(10),
        Validators.maxLength(500)
      ]],
      laboratoire: ['', [Validators.required]],
      laboratoireAutre: [''], // For custom laboratory
      specialite: ['', [Validators.required]],
      specialiteAutre: [''] // For custom specialty
    });

    // Add conditional validators
    this.inscriptionForm.get('laboratoire')?.valueChanges.subscribe(value => {
      const laboratoireAutreControl = this.inscriptionForm.get('laboratoireAutre');
      if (value === 'Autre') {
        laboratoireAutreControl?.setValidators([Validators.required]);
      } else {
        laboratoireAutreControl?.clearValidators();
      }
      laboratoireAutreControl?.updateValueAndValidity();
    });

    this.inscriptionForm.get('specialite')?.valueChanges.subscribe(value => {
      const specialiteAutreControl = this.inscriptionForm.get('specialiteAutre');
      if (value === 'Autre') {
        specialiteAutreControl?.setValidators([Validators.required]);
      } else {
        specialiteAutreControl?.clearValidators();
      }
      specialiteAutreControl?.updateValueAndValidity();
    });
  }

  private loadFormData(): void {
    this.campagneActive$ = this.inscriptionService.getCampagneActive();
    this.directeurs$ = this.userService.getDirecteurs();

    // Auto-select active campaign
    this.campagneActive$.subscribe(campagne => {
      if (campagne) {
        this.inscriptionForm.patchValue({ campagneId: campagne.id });
      }
    });
  }

  private setupDirecteurFilter(): void {
    const searchControl = this.inscriptionForm.get('directeurSearch');
    if (searchControl) {
      this.filteredDirecteurs$ = combineLatest([
        this.directeurs$,
        searchControl.valueChanges.pipe(startWith(''))
      ]).pipe(
        map(([directeurs, searchTerm]) => {
          if (!searchTerm) return directeurs;
          const term = searchTerm.toLowerCase();
          return directeurs.filter(directeur => 
            `${directeur.FirstName} ${directeur.LastName}`.toLowerCase().includes(term) ||
            directeur.email.toLowerCase().includes(term)
          );
        })
      );
    } else {
      this.filteredDirecteurs$ = this.directeurs$;
    }
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
        this.populateFormForEdit(inscription);
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Erreur lors du chargement de l\'inscription';
        this.loading = false;
        console.error('Erreur chargement inscription:', error);
      }
    });
  }

  private populateFormForEdit(inscription: InscriptionResponse): void {
    this.inscriptionForm.patchValue({
      directeurId: inscription.directeur.id,
      directeurSearch: `${inscription.directeur.FirstName} ${inscription.directeur.LastName}`,
      campagneId: inscription.campagne.id,
      sujetThese: inscription.sujetThese,
      laboratoire: this.laboratoires.includes(inscription.laboratoire) ? inscription.laboratoire : 'Autre',
      laboratoireAutre: this.laboratoires.includes(inscription.laboratoire) ? '' : inscription.laboratoire,
      specialite: this.specialites.includes(inscription.specialite) ? inscription.specialite : 'Autre',
      specialiteAutre: this.specialites.includes(inscription.specialite) ? '' : inscription.specialite
    });
  }

  onDirecteurSelect(directeur: UserResponse): void {
    this.inscriptionForm.patchValue({
      directeurId: directeur.id,
      directeurSearch: `${directeur.FirstName} ${directeur.LastName}`
    });
  }

  onSubmit(): void {
    if (this.inscriptionForm.valid) {
      this.loading = true;
      this.error = null;
      this.success = null;

      const formData = this.prepareFormData();

      if (this.isEditMode && this.inscriptionId) {
        // Update existing inscription
        this.updateInscription(formData);
      } else {
        // Create new inscription
        this.createInscription(formData);
      }
    } else {
      this.markFormGroupTouched();
    }
  }

  private prepareFormData(): InscriptionRequest {
    const formValue = this.inscriptionForm.value;
    
    return {
      directeurId: formValue.directeurId,
      campagneId: formValue.campagneId,
      sujetThese: formValue.sujetThese.trim(),
      laboratoire: formValue.laboratoire === 'Autre' ? formValue.laboratoireAutre.trim() : formValue.laboratoire,
      specialite: formValue.specialite === 'Autre' ? formValue.specialiteAutre.trim() : formValue.specialite
    };
  }

  private createInscription(data: InscriptionRequest): void {
    this.inscriptionService.createInscription(data).subscribe({
      next: (response) => {
        this.success = 'Inscription créée avec succès !';
        this.loading = false;
        setTimeout(() => {
          this.router.navigate(['/inscription', response.id]);
        }, 2000);
      },
      error: (error) => {
        this.error = this.getErrorMessage(error);
        this.loading = false;
        console.error('Erreur création inscription:', error);
      }
    });
  }

  private updateInscription(data: InscriptionRequest): void {
    // Note: Update method would need to be added to the service
    // For now, we'll show a message that update is not implemented
    this.error = 'La modification d\'inscription n\'est pas encore implémentée côté backend';
    this.loading = false;
  }

  private getErrorMessage(error: any): string {
    if (error.error?.message) {
      return error.error.message;
    }
    if (error.status === 400) {
      return 'Données invalides. Veuillez vérifier votre saisie.';
    }
    if (error.status === 409) {
      return 'Une inscription existe déjà pour cette campagne.';
    }
    return 'Une erreur est survenue lors de l\'enregistrement.';
  }

  private markFormGroupTouched(): void {
    Object.keys(this.inscriptionForm.controls).forEach(key => {
      const control = this.inscriptionForm.get(key);
      control?.markAsTouched();
    });
  }

  onCancel(): void {
    this.router.navigate(['/inscription']);
  }

  // Helper methods for template
  isFieldInvalid(fieldName: string): boolean {
    const field = this.inscriptionForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getFieldError(fieldName: string): string {
    const field = this.inscriptionForm.get(fieldName);
    if (field && field.errors) {
      if (field.errors['required']) return 'Ce champ est obligatoire';
      if (field.errors['minlength']) return `Minimum ${field.errors['minlength'].requiredLength} caractères`;
      if (field.errors['maxlength']) return `Maximum ${field.errors['maxlength'].requiredLength} caractères`;
    }
    return '';
  }
}