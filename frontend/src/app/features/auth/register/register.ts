import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService, RegisterRequest } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.html',
  styleUrl: './register.scss'
})
export class Register {
  formData: RegisterRequest = {
    email: '',
    password: '',
    FirstName: '',
    LastName: '',
    phoneNumber: '',
    adresse: '',
    ville: '',
    pays: ''
  };

  confirmPassword: string = '';
  errorMessage: string = '';
  successMessage: string = '';
  isLoading: boolean = false;
  showPassword: boolean = false;
  showConfirmPassword: boolean = false;

  // Messages de validation
  passwordErrors: string[] = [];

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  /**
   * 🔹 Vérifier la longueur du mot de passe
   */
  hasValidLength(): boolean {
    return this.formData.password.length >= 12 && this.formData.password.length <= 64;
  }

  /**
   * 🔹 Vérifier si le mot de passe contient une minuscule
   */
  hasLowerCase(): boolean {
    return /[a-z]/.test(this.formData.password);
  }

  /**
   * 🔹 Vérifier si le mot de passe contient une majuscule
   */
  hasUpperCase(): boolean {
    return /[A-Z]/.test(this.formData.password);
  }

  /**
   * 🔹 Vérifier si le mot de passe contient un chiffre
   */
  hasDigit(): boolean {
    return /\d/.test(this.formData.password);
  }

  /**
   * 🔹 Vérifier si le mot de passe contient un caractère spécial
   */
  hasSpecialChar(): boolean {
    return /[@$!%*?&.]/.test(this.formData.password);
  }

  /**
   * Valider le mot de passe selon les critères du backend
   */
  validatePassword(): void {
    this.passwordErrors = [];
    const password = this.formData.password;

    if (!this.hasValidLength()) {
      this.passwordErrors.push('Le mot de passe doit contenir entre 12 et 64 caractères');
    }
    if (!this.hasLowerCase()) {
      this.passwordErrors.push('Au moins une lettre minuscule');
    }
    if (!this.hasUpperCase()) {
      this.passwordErrors.push('Au moins une lettre majuscule');
    }
    if (!this.hasDigit()) {
      this.passwordErrors.push('Au moins un chiffre');
    }
    if (!this.hasSpecialChar()) {
      this.passwordErrors.push('Au moins un caractère spécial (@$!%*?&.)');
    }
  }

  /**
   * Soumettre le formulaire
   */
  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    // Validation basique
    if (!this.formData.email || !this.formData.password || !this.formData.FirstName || 
        !this.formData.LastName || !this.formData.phoneNumber || !this.formData.adresse || 
        !this.formData.ville || !this.formData.pays) {
      this.errorMessage = 'Veuillez remplir tous les champs obligatoires';
      return;
    }

    // Vérifier que les mots de passe correspondent
    if (this.formData.password !== this.confirmPassword) {
      this.errorMessage = 'Les mots de passe ne correspondent pas';
      return;
    }

    // Valider le mot de passe
    this.validatePassword();
    if (this.passwordErrors.length > 0) {
      this.errorMessage = 'Le mot de passe ne respecte pas les critères de sécurité';
      return;
    }

    this.isLoading = true;

    this.authService.register(this.formData).subscribe({
      next: (response) => {
        console.log('✅ Inscription réussie !', response);
        this.successMessage = 'Inscription réussie ! Redirection vers la page de connexion...';
        
        // Redirection vers login après 2 secondes
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (error) => {
        console.error('❌ Erreur d\'inscription', error);
        
        if (error.status === 409) {
          this.errorMessage = 'Cet email est déjà utilisé';
        } else if (error.status === 400) {
          this.errorMessage = error.error?.message || 'Données invalides. Veuillez vérifier vos informations.';
        } else if (error.status === 0) {
          this.errorMessage = 'Impossible de contacter le serveur. Vérifiez que le backend est démarré.';
        } else {
          this.errorMessage = error.error?.message || 'Erreur lors de l\'inscription. Veuillez réessayer.';
        }
        this.isLoading = false;
      },
      complete: () => {
        this.isLoading = false;
      }
    });
  }

  /**
   * Basculer la visibilité du mot de passe
   */
  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }
}