import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService, UserResponse } from '../../../core/services/auth.service';
import { AdminMenuComponent } from '../admin-menu/admin-menu.component';

@Component({
  selector: 'app-admin-container',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    AdminMenuComponent
  ],
  template: `
    <div class="admin-container">
      <div class="admin-header">
        <div class="header-content">
          <h1 class="admin-title">
            <i class="fas fa-shield-alt"></i>
            Administration
          </h1>
          <div class="admin-user-info">
            <span class="welcome-text">Bienvenue, {{ currentUser?.FirstName }} {{ currentUser?.LastName }}</span>
            <button class="btn btn-outline-secondary btn-sm" (click)="goToDashboard()">
              <i class="fas fa-tachometer-alt"></i>
              Tableau de bord
            </button>
          </div>
        </div>
      </div>

      <div class="admin-layout">
        <aside class="admin-sidebar">
          <app-admin-menu></app-admin-menu>
        </aside>

        <main class="admin-content">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styleUrls: ['./admin-container.component.scss']
})
export class AdminContainerComponent implements OnInit, OnDestroy {
  currentUser: UserResponse | null = null;
  private subscription = new Subscription();

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.subscription.add(
      this.authService.getCurrentUser().subscribe(user => {
        this.currentUser = user;
        console.log('✅ AdminContainer chargé pour:', this.currentUser?.FirstName);
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard/admin']);
  }
}