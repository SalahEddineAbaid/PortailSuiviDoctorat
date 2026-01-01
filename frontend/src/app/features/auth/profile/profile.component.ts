import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService, ChangePasswordRequest, UserInfo } from '../../../core/services/auth.service';
import { CustomValidators } from '../../../core/validators/custom-validators';
import { Router } from '@angular/router';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  profileForm: FormGroup;
  passwordForm: FormGroup;
  currentUser: UserInfo | null = null;

  isLoadingProfile = false;
  isLoadingPassword = false;
  profileSuccess = false;
  passwordSuccess = false;
  profileError: string | null = null;
  passwordError: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.profileForm = this.fb.group({
      FirstName: ['', [Validators.required, Validators.minLength(2), CustomValidators.name]],
      LastName: ['', [Validators.required, Validators.minLength(2), CustomValidators.name]],
      phoneNumber: ['', [Validators.required, CustomValidators.phoneNumber]],
      adresse: ['', [Validators.required]],
      ville: ['', [Validators.required]],
      pays: ['', [Validators.required]]
    });

    this.passwordForm = this.fb.group({
      oldPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(12), CustomValidators.strongPassword]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: CustomValidators.matchFields('newPassword', 'confirmPassword') });
  }

  ngOnInit(): void {
    this.loadUserProfile();
  }

  private loadUserProfile(): void {
    this.authService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser = user;
        if (this.currentUser) {
          this.profileForm.patchValue({
            FirstName: this.currentUser.FirstName,
            LastName: this.currentUser.LastName,
            phoneNumber: this.currentUser.phoneNumber,
            adresse: this.currentUser.adresse,
            ville: this.currentUser.ville,
            pays: this.currentUser.pays
          });
        }
      },
      error: (error) => {
        console.error('❌ Erreur chargement profil:', error);
        this.profileError = 'Impossible de charger le profil';
      }
    });
  }

  onUpdateProfile(): void {
    if (this.profileForm.valid) {
      this.isLoadingProfile = true;
      this.profileError = null;
      this.profileSuccess = false;

      const updateData = this.profileForm.value;

      this.authService.updateProfile(updateData).subscribe({
        next: (response) => {
          console.log('✅ Profil mis à jour:', response);
          this.profileSuccess = true;
          this.isLoadingProfile = false;

          // Masquer le message de succès après 3 secondes
          setTimeout(() => {
            this.profileSuccess = false;
          }, 3000);
        },
        error: (error) => {
          console.error('❌ Erreur mise à jour profil:', error);
          this.profileError = this.getErrorMessage(error);
          this.isLoadingProfile = false;
        }
      });
    } else {
      this.markFormGroupTouched(this.profileForm);
    }
  }

  onChangePassword(): void {
    if (this.passwordForm.valid) {
      this.isLoadingPassword = true;
      this.passwordError = null;
      this.passwordSuccess = false;

      const passwordData: ChangePasswordRequest = {
        oldPassword: this.passwordForm.value.oldPassword,
        newPassword: this.passwordForm.value.newPassword
      };

      this.authService.changePassword(passwordData).subscribe({
        next: () => {
          console.log('✅ Mot de passe changé');
          this.passwordSuccess = true;
          this.isLoadingPassword = false;
          this.passwordForm.reset();

          // Masquer le message de succès après 3 secondes
          setTimeout(() => {
            this.passwordSuccess = false;
          }, 3000);
        },
        error: (error) => {
          console.error('❌ Erreur changement mot de passe:', error);
          this.passwordError = this.getErrorMessage(error);
          this.isLoadingPassword = false;
        }
      });
    } else {
      this.markFormGroupTouched(this.passwordForm);
    }
  }

  private getErrorMessage(error: any): string {
    if (error.error?.message) {
      return error.error.message;
    }

    switch (error.status) {
      case 400:
        return 'Données invalides. Veuillez vérifier votre saisie.';
      case 401:
        return 'Mot de passe actuel incorrect.';
      case 422:
        return 'Données non valides. Veuillez corriger les erreurs.';
      case 500:
        return 'Erreur serveur. Veuillez réessayer plus tard.';
      default:
        return 'Une erreur inattendue s\'est produite.';
    }
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  isFieldInvalid(form: FormGroup, fieldName: string): boolean {
    const field = form.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getFieldError(form: FormGroup, fieldName: string): string {
    const field = form.get(fieldName);
    if (field && field.errors) {
      return CustomValidators.getErrorMessage(field.errors, fieldName);
    }
    return '';
  }

  onCancel(): void {
    this.router.navigate([this.authService.getDashboardRoute()]);
  }
}
