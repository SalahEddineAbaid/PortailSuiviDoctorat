import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable, combineLatest, of } from 'rxjs';
import { map, switchMap, startWith } from 'rxjs/operators';

import { InscriptionService } from '../../../core/services/inscription.service';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';

import { 
  InscriptionRequest, 
  InscriptionResponse,
  CampagneResponse,
  TypeInscription 
} from '../../../core/models/inscription.model';
import { UserResponse } from '../../../core/models/user.model';

@Component({
  selector: 'app-reinscription-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './reinscription-form.html',
  styleUrls: ['./reinscription-form.scss']
})
export class ReinscriptionForm implements OnInit {
  reinscriptionForm!: FormGroup;
  campagneActive$!: Observable<CampagneResponse | null>;
  directeurs$!: Observable<UserResponse[]>;
  filteredDirecteurs$!: Observable<UserResponse[]>;
  previousInscription$!: Observable<InscriptionResponse | null>;
  
  loading = false;
  loadingPreviousData = false;
  error: string | null = null;
  success: string | null = null;

  // Predefined options (same as inscription form)
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
    private router: Router
  ) {
    this.initializeForm();
  }

  ngOnInit(): void {
    this.loadFormData();
    this.setupDirecteurFilter();
    this.loadPreviousInscription();
  }

  private initializeForm(): void {
    this.reinscriptionForm = this.fb.group({
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
      specialiteAutre: [''], // For custom specialty
      
      // Additional fields for reinscription
      changementsApportes: ['', [Validators.maxLength(1000)]],
      justificationChangements: ['', [Validators.maxLength(1000)]],
      avancementTravaux: ['', [
        Validators.required,
        Validators.minLength(50),
        Validators.maxLength(2000)
      ]],
      objectifsAnneeProchaine: ['', [
        Validators.required,
        Validators.minLength(50),
        Validators.maxLength(2000)
      ]]
    });

    // Add conditional validators
    this.setupConditionalValidators();
  }

  private setupConditionalValidators(): void {
    this.reinscriptionForm.get('laboratoire')?.valueChanges.subscribe(value => {
      const laboratoireAutreControl = this.reinscriptionForm.get('laboratoireAutre');
      if (value === 'Autre') {
        laboratoireAutreControl?.setValidators([Validators.required]);
      } else {
        laboratoireAutreControl?.clearValidators();
      }
      laboratoireAutreControl?.updateValueAndValidity();
    });

    this.reinscriptionForm.get('specialite')?.valueChanges.subscribe(value => {
      const specialiteAutreControl = this.reinscriptionForm.get('specialiteAutre');
      if (value === 'Autre') {
        specialiteAutreControl?.setValidators([Validators.required]);
      } else {
        specialiteAutreControl?.clearValidators();
      }
      specialiteAutreControl?.updateValueAndValidity();
    });

    // Add validator for changes justification
    this.reinscriptionForm.get('changementsApportes')?.valueChanges.subscribe(value => {
      const justificationControl = this.reinscriptionForm.get('justificationChangements');
      if (value && value.trim().length > 0) {
        justificationControl?.setValidators([Validators.required, Validators.minLength(20)]);
      } else {
        justificationControl?.clearValidators();
      }
      justificationControl?.updateValueAndValidity();
    });
  }

  private loadFormData(): void {
    // Load active campaign (should be REINSCRIPTION type)
    this.campagneActive$ = this.inscriptionService.getCampagneActive().pipe(
      map(campagne => {
        if (campagne && campagne.typeInscription !== TypeInscription.REINSCRIPTION) {
          this.error = 'Aucune campagne de réinscription active trouvée';
          return null;
        }
        return campagne;
      })
    );
    
    this.directeurs$ = this.userService.getDirecteurs();

    // Auto-select active campaign
    this.campagneActive$.subscribe(campagne => {
      if (campagne) {
        this.reinscriptionForm.patchValue({ campagneId: campagne.id });
      }
    });
  }

  private setupDirecteurFilter(): void {
    const searchControl = this.reinscriptionForm.get('directeurSearch');
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

  private loadPreviousInscription(): void {
    this.loadingPreviousData = true;
    
    // Get the most recent inscription of the current user
    this.previousInscription$ = this.inscriptionService.getMyInscriptions().pipe(
      map(inscriptions => {
        if (inscriptions.length === 0) {
          this.error = 'Aucune inscription précédente trouvée. Vous devez d\'abord créer une première inscription.';
          return null;
        }
        
        // Sort by date and get the most recent
        const sortedInscriptions = inscriptions.sort((a, b) => 
          new Date(b.dateInscription).getTime() - new Date(a.dateInscription).getTime()
        );
        
        return sortedInscriptions[0];
      })
    );

    this.previousInscription$.subscribe({
      next: (inscription) => {
        if (inscription) {
          this.prefillFormWithPreviousData(inscription);
        }
        this.loadingPreviousData = false;
      },
      error: (error) => {
        this.error = 'Erreur lors du chargement des données précédentes';
        this.loadingPreviousData = false;
        console.error('Erreur chargement inscription précédente:', error);
      }
    });
  }

  private prefillFormWithPreviousData(inscription: InscriptionResponse): void {
    this.reinscriptionForm.patchValue({
      directeurId: inscription.directeur.id,
      directeurSearch: `${inscription.directeur.FirstName} ${inscription.directeur.LastName}`,
      sujetThese: inscription.sujetThese,
      laboratoire: this.laboratoires.includes(inscription.laboratoire) ? inscription.laboratoire : 'Autre',
      laboratoireAutre: this.laboratoires.includes(inscription.laboratoire) ? '' : inscription.laboratoire,
      specialite: this.specialites.includes(inscription.specialite) ? inscription.specialite : 'Autre',
      specialiteAutre: this.specialites.includes(inscription.specialite) ? '' : inscription.specialite
    });
  }

  onDirecteurSelect(directeur: UserResponse): void {
    this.reinscriptionForm.patchValue({
      directeurId: directeur.id,
      directeurSearch: `${directeur.FirstName} ${directeur.LastName}`
    });
  }

  onSubmit(): void {
    if (this.reinscriptionForm.valid) {
      this.loading = true;
      this.error = null;
      this.success = null;

      const formData = this.prepareFormData();
      this.createReinscription(formData);
    } else {
      this.markFormGroupTouched();
    }
  }

  private prepareFormData(): InscriptionRequest {
    const formValue = this.reinscriptionForm.value;
    
    // For now, we'll use the same InscriptionRequest interface
    // In a real implementation, you might want a separate ReinscriptionRequest interface
    return {
      directeurId: formValue.directeurId,
      campagneId: formValue.campagneId,
      sujetThese: formValue.sujetThese.trim(),
      laboratoire: formValue.laboratoire === 'Autre' ? formValue.laboratoireAutre.trim() : formValue.laboratoire,
      specialite: formValue.specialite === 'Autre' ? formValue.specialiteAutre.trim() : formValue.specialite
    };
  }

  private createReinscription(data: InscriptionRequest): void {
    this.inscriptionService.createInscription(data).subscribe({
      next: (response) => {
        this.success = 'Réinscription créée avec succès !';
        this.loading = false;
        setTimeout(() => {
          this.router.navigate(['/inscription', response.id]);
        }, 2000);
      },
      error: (error) => {
        this.error = this.getErrorMessage(error);
        this.loading = false;
        console.error('Erreur création réinscription:', error);
      }
    });
  }

  private getErrorMessage(error: any): string {
    if (error.error?.message) {
      return error.error.message;
    }
    if (error.status === 400) {
      return 'Données invalides. Veuillez vérifier votre saisie.';
    }
    if (error.status === 409) {
      return 'Une réinscription existe déjà pour cette campagne.';
    }
    return 'Une erreur est survenue lors de l\'enregistrement.';
  }

  private markFormGroupTouched(): void {
    Object.keys(this.reinscriptionForm.controls).forEach(key => {
      const control = this.reinscriptionForm.get(key);
      control?.markAsTouched();
    });
  }

  onCancel(): void {
    this.router.navigate(['/inscription']);
  }

  onResetToOriginal(): void {
    this.previousInscription$.subscribe(inscription => {
      if (inscription) {
        this.prefillFormWithPreviousData(inscription);
        // Clear the additional reinscription fields
        this.reinscriptionForm.patchValue({
          changementsApportes: '',
          justificationChangements: '',
          avancementTravaux: '',
          objectifsAnneeProchaine: ''
        });
      }
    });
  }

  // Helper methods for template
  isFieldInvalid(fieldName: string): boolean {
    const field = this.reinscriptionForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getFieldError(fieldName: string): string {
    const field = this.reinscriptionForm.get(fieldName);
    if (field && field.errors) {
      if (field.errors['required']) return 'Ce champ est obligatoire';
      if (field.errors['minlength']) return `Minimum ${field.errors['minlength'].requiredLength} caractères`;
      if (field.errors['maxlength']) return `Maximum ${field.errors['maxlength'].requiredLength} caractères`;
    }
    return '';
  }

  hasChangesFromPrevious(): boolean {
    const changementsControl = this.reinscriptionForm.get('changementsApportes');
    return !!(changementsControl?.value && changementsControl.value.trim().length > 0);
  }
}