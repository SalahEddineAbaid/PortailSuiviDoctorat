import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';

import { UserResponse } from '../../../../core/models/user.model';
import { RoleName } from '../../../../core/models/role.model';
import { CustomValidators } from '../../../../core/validators/custom-validators';
import { CreateUserRequest, UpdateUserRequest } from '../../../../core/services/user.service';

export interface UserFormData {
  mode: 'create' | 'edit';
  user?: UserResponse;
}

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatCheckboxModule
  ],
  templateUrl: './user-form.component.html',
  styleUrls: ['./user-form.component.scss']
})
export class UserFormComponent implements OnInit {
  userForm: FormGroup;
  isEditMode: boolean;
  
  readonly roleNames = Object.values(RoleName);
  readonly roleLabels = {
    [RoleName.ADMIN]: 'Administrateur',
    [RoleName.DIRECTEUR]: 'Directeur de thèse',
    [RoleName.DOCTORANT]: 'Doctorant'
  };

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<UserFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: UserFormData
  ) {
    this.isEditMode = data.mode === 'edit';
    this.userForm = this.createForm();
  }

  ngOnInit(): void {
    if (this.isEditMode && this.data.user) {
      this.populateForm(this.data.user);
    }
  }

  private createForm(): FormGroup {
    const formConfig: any = {
      FirstName: ['', [Validators.required, Validators.minLength(2)]],
      LastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, CustomValidators.email]],
      phoneNumber: ['', [Validators.required, CustomValidators.phoneNumber]],
      adresse: ['', [Validators.required]],
      ville: ['', [Validators.required]],
      pays: ['', [Validators.required]],
      roles: [[], [Validators.required]]
    };

    // Ajouter le champ mot de passe uniquement en mode création
    if (!this.isEditMode) {
      formConfig.password = ['', [
        Validators.required,
        Validators.minLength(8),
        CustomValidators.strongPassword
      ]];
      formConfig.confirmPassword = ['', [Validators.required]];
    } else {
      // Ajouter le champ enabled uniquement en mode édition
      formConfig.enabled = [true];
    }

    const form = this.fb.group(formConfig);

    // Ajouter le validator pour vérifier que les mots de passe correspondent
    if (!this.isEditMode) {
      form.addValidators(this.passwordMatchValidator);
    }

    return form;
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const confirmPassword = control.get('confirmPassword');
    
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    
    return null;
  }

  private populateForm(user: UserResponse): void {
    this.userForm.patchValue({
      FirstName: user.FirstName,
      LastName: user.LastName,
      email: user.email,
      phoneNumber: user.phoneNumber,
      adresse: user.adresse,
      ville: user.ville,
      pays: user.pays,
      roles: user.roles,  // ✅ Les rôles sont déjà des strings
      enabled: user.enabled
    });
  }

  onSubmit(): void {
    if (this.userForm.valid) {
      const formValue = this.userForm.value;
      
      if (this.isEditMode) {
        const updateData: UpdateUserRequest = {
          FirstName: formValue.FirstName,
          LastName: formValue.LastName,
          email: formValue.email,
          phoneNumber: formValue.phoneNumber,
          adresse: formValue.adresse,
          ville: formValue.ville,
          pays: formValue.pays,
          roles: formValue.roles,
          enabled: formValue.enabled
        };
        this.dialogRef.close(updateData);
      } else {
        const createData: CreateUserRequest = {
          FirstName: formValue.FirstName,
          LastName: formValue.LastName,
          email: formValue.email,
          phoneNumber: formValue.phoneNumber,
          adresse: formValue.adresse,
          ville: formValue.ville,
          pays: formValue.pays,
          password: formValue.password,
          roles: formValue.roles
        };
        this.dialogRef.close(createData);
      }
    } else {
      this.markFormGroupTouched();
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  private markFormGroupTouched(): void {
    Object.keys(this.userForm.controls).forEach(key => {
      const control = this.userForm.get(key);
      control?.markAsTouched();
    });
  }

  getFieldError(fieldName: string): string {
    const control = this.userForm.get(fieldName);
    
    if (control?.errors && control.touched) {
      if (control.errors['required']) {
        return 'Ce champ est obligatoire';
      }
      if (control.errors['minlength']) {
        return `Minimum ${control.errors['minlength'].requiredLength} caractères`;
      }
      if (control.errors['email']) {
        return 'Format d\'email invalide';
      }
      if (control.errors['phoneNumber']) {
        return 'Format de téléphone invalide';
      }
      if (control.errors['strongPassword']) {
        return 'Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial';
      }
      if (control.errors['passwordMismatch']) {
        return 'Les mots de passe ne correspondent pas';
      }
    }
    
    return '';
  }

  getRoleLabel(roleName: RoleName): string {
    return this.roleLabels[roleName] || roleName;
  }

  get title(): string {
    return this.isEditMode ? 'Modifier l\'utilisateur' : 'Créer un utilisateur';
  }

  get submitButtonText(): string {
    return this.isEditMode ? 'Mettre à jour' : 'Créer';
  }
}