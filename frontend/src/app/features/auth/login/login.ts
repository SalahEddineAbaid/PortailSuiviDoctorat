import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService, LoginRequest } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  formData: LoginRequest = {
    email: '',
    password: ''
  };

  errorMessage: string = '';
  isLoading: boolean = false;
  showPassword: boolean = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  /**
   * Soumettre le formulaire de connexion
   */
  onSubmit(): void {
    this.errorMessage = '';

    // Validation basique
    if (!this.formData.email || !this.formData.password) {
      this.errorMessage = 'Veuillez remplir tous les champs';
      return;
    }

    // Validation format email
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.formData.email)) {
      this.errorMessage = 'Veuillez entrer un email valide';
      return;
    }

    this.isLoading = true;

    this.authService.login(this.formData).subscribe({
      next: (response) => {
        console.log('✅ Connexion réussie !', response);
        
        // ✅ Redirection automatique selon le rôle
        const dashboardRoute = this.authService.getDashboardRoute();
        console.log('🔀 Redirection vers:', dashboardRoute);
        
        this.router.navigate([dashboardRoute]);
      },
      error: (error) => {
        console.error('❌ Erreur de connexion', error);
        
        if (error.status === 401) {
          this.errorMessage = 'Email ou mot de passe incorrect';
        } else if (error.status === 0) {
          this.errorMessage = 'Impossible de contacter le serveur. Vérifiez que le backend est démarré sur le port 8081.';
        } else {
          this.errorMessage = error.error?.message || 'Erreur lors de la connexion. Veuillez réessayer.';
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
}