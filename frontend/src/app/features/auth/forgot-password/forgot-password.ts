import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { AuthService, ForgotPasswordRequest, ResetPasswordRequest } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.scss'
})
export class ForgotPassword {
  // Mode: 'request' pour demander le reset, 'reset' pour réinitialiser avec token
  mode: 'request' | 'reset' = 'request';
  
  // Formulaire de demande
  email: string = '';
  
  // Formulaire de réinitialisation
  token: string = '';
  newPassword: string = '';
  confirmPassword: string = '';
  
  // États
  isLoading: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';
  showPassword: boolean = false;
  showConfirmPassword: boolean = false;
  
  // Validation du mot de passe
  passwordErrors: string[] = [];

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    // Vérifier si on a un token dans l'URL
    this.route.queryParams.subscribe(params => {
      if (params['token']) {
        this.mode = 'reset';
        this.token = params['token'];
      }
    });
  }

  /**
   * 📧 Demander la réinitialisation du mot de passe
   */
  onRequestReset(): void {
    this.errorMessage = '';
    this.successMessage = '';

    // Validation
    if (!this.email) {
      this.errorMessage = 'Veuillez saisir votre adresse email';
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.email)) {
      this.errorMessage = 'Veuillez saisir une adresse email valide';
      return;
    }

    this.isLoading = true;

    const request: ForgotPasswordRequest = { email: this.email };

    this.authService.forgotPassword(request).subscribe({
      next: (response) => {
        console.log('✅ Email de réinitialisation envoyé');
        this.successMessage = 'Si l\'email existe, un lien de réinitialisation a été envoyé. Veuillez vérifier votre boîte de réception.';
        this.email = '';
        this.isLoading = false;
      },
      error: (error) => {
        console.error('❌ Erreur demande réinitialisation', error);
        // Pour des raisons de sécurité, on affiche toujours le même message
        this.successMessage = 'Si l\'email existe, un lien de réinitialisation a été envoyé. Veuillez vérifier votre boîte de réception.';
        this.isLoading = false;
      }
    });
  }

  /**
   * 🔄 Réinitialiser le mot de passe avec le token
   */
  onResetPassword(): void {
    this.errorMessage = '';
    this.successMessage = '';

    // Validation
    if (!this.newPassword || !this.confirmPassword) {
      this.errorMessage = 'Veuillez remplir tous les champs';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
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

    const request: ResetPasswordRequest = {
      token: this.token,
      newPassword: this.newPassword
    };

    this.authService.resetPassword(request).subscribe({
      next: (response) => {
        console.log('✅ Mot de passe réinitialisé');
        this.successMessage = 'Votre mot de passe a été réinitialisé avec succès. Redirection vers la page de connexion...';
        
        // Redirection vers login après 3 secondes
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 3000);
        
        this.isLoading = false;
      },
      error: (error) => {
        console.error('❌ Erreur réinitialisation mot de passe', error);
        
        if (error.status === 400) {
          this.errorMessage = 'Le lien de réinitialisation est invalide ou a expiré. Veuillez faire une nouvelle demande.';
        } else if (error.status === 0) {
          this.errorMessage = 'Impossible de contacter le serveur. Vérifiez que le backend est démarré.';
        } else {
          this.errorMessage = error.error?.message || 'Erreur lors de la réinitialisation. Veuillez réessayer.';
        }
        
        this.isLoading = false;
      }
    });
  }

  /**
   * 🔹 Valider le mot de passe selon les critères du backend
   */
  validatePassword(): void {
    this.passwordErrors = [];
    const password = this.newPassword;

    if (password.length < 12 || password.length > 64) {
      this.passwordErrors.push('Le mot de passe doit contenir entre 12 et 64 caractères');
    }
    if (!/[a-z]/.test(password)) {
      this.passwordErrors.push('Au moins une lettre minuscule');
    }
    if (!/[A-Z]/.test(password)) {
      this.passwordErrors.push('Au moins une lettre majuscule');
    }
    if (!/\d/.test(password)) {
      this.passwordErrors.push('Au moins un chiffre');
    }
    if (!/[@$!%*?&.]/.test(password)) {
      this.passwordErrors.push('Au moins un caractère spécial (@$!%*?&.)');
    }
    if (!/^[A-Za-z\d@$!%*?&.]+$/.test(password)) {
      this.passwordErrors.push('Pas d\'espaces ni de caractères non autorisés');
    }
  }

  /**
   * 🔹 Vérifications individuelles pour l'affichage
   */
  hasValidLength(): boolean {
    return this.newPassword.length >= 12 && this.newPassword.length <= 64;
  }

  hasLowerCase(): boolean {
    return /[a-z]/.test(this.newPassword);
  }

  hasUpperCase(): boolean {
    return /[A-Z]/.test(this.newPassword);
  }

  hasDigit(): boolean {
    return /\d/.test(this.newPassword);
  }

  hasSpecialChar(): boolean {
    return /[@$!%*?&.]/.test(this.newPassword);
  }

  hasOnlyAllowedChars(): boolean {
    return /^[A-Za-z\d@$!%*?&.]+$/.test(this.newPassword);
  }

  /**
   * 👁️ Basculer la visibilité du mot de passe
   */
  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }
}
