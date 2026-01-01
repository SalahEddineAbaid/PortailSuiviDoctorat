import { Component, OnInit } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

/**
 * 🏠 Dashboard Container
 * 
 * Composant conteneur qui gère le routing dynamique vers le bon dashboard
 * en fonction du rôle de l'utilisateur connecté.
 */
@Component({
  selector: 'app-dashboard-container',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet></router-outlet>`,
  styles: [':host { display: block; min-height: 100vh; }']
})
export class DashboardContainer implements OnInit {
  
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Rediriger automatiquement vers le bon dashboard si on est sur la route racine
    if (this.router.url === '/dashboard' || this.router.url === '/dashboard/') {
      this.redirectToDashboard();
    }
  }

  /**
   * 🎯 Rediriger vers le dashboard approprié selon le rôle
   */
  private redirectToDashboard(): void {
    const dashboardRoute = this.authService.getDashboardRoute();
    console.log('🔀 [DASHBOARD CONTAINER] Redirection vers:', dashboardRoute);
    this.router.navigate([dashboardRoute]);
  }
}