import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';

export interface UserProfile {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    roles: string[];
}

/**
 * Navbar Component
 * Main navigation bar with logo, user profile, and notifications
 * 
 * @example
 * <app-navbar 
 *   appName="Portail Suivi Doctorat"
 *   [showNotifications]="true"
 *   (menuToggle)="toggleSidebar()">
 * </app-navbar>
 */
@Component({
    selector: 'app-navbar',
    standalone: true,
    imports: [CommonModule, RouterModule],
    template: `
    <nav class="navbar">
      <div class="navbar-container">
        <!-- Logo & Menu Toggle -->
        <div class="navbar-left">
          <button 
            class="menu-toggle"
            (click)="menuToggle.emit()"
            type="button"
            aria-label="Toggle menu">
            <i class="icon-menu"></i>
          </button>
          
          <a routerLink="/" class="navbar-brand">
            <img *ngIf="logoSrc" [src]="logoSrc" [alt]="appName" class="navbar-logo">
            <span class="navbar-title">{{ appName }}</span>
          </a>
        </div>

        <!-- Right Section -->
        <div class="navbar-right">
          <!-- Notifications -->
          <button 
            *ngIf="showNotifications"
            class="navbar-icon-btn"
            (click)="notificationClick.emit()"
            type="button"
            [attr.aria-label]="'Notifications' + (unreadCount > 0 ? ' (' + unreadCount + ' non lues)' : '')">
            <i class="icon-bell"></i>
            <span *ngIf="unreadCount > 0" class="badge">{{ unreadCount }}</span>
          </button>

          <!-- User Profile Dropdown -->
          <div *ngIf="showProfile && currentUser" class="user-dropdown" [class.open]="isDropdownOpen">
            <button 
              class="user-button"
              (click)="toggleDropdown()"
              type="button"
              aria-haspopup="true"
              [attr.aria-expanded]="isDropdownOpen">
              <div class="user-avatar">
                <i class="icon-user"></i>
              </div>
              <span class="user-name">{{ currentUser.firstName }} {{ currentUser.lastName }}</span>
              <i class="icon-chevron-down"></i>
            </button>

            <div class="dropdown-menu" *ngIf="isDropdownOpen">
              <div class="dropdown-header">
                <div class="user-info">
                  <strong>{{ currentUser.firstName }} {{ currentUser.lastName }}</strong>
                  <small>{{ currentUser.email }}</small>
                </div>
              </div>
              
              <div class="dropdown-divider"></div>
              
              <a routerLink="/profile" class="dropdown-item" (click)="closeDropdown()">
                <i class="icon-user"></i>
                <span>Mon Profil</span>
              </a>
              
              <a routerLink="/settings" class="dropdown-item" (click)="closeDropdown()">
                <i class="icon-settings"></i>
                <span>Paramètres</span>
              </a>
              
              <div class="dropdown-divider"></div>
              
              <button class="dropdown-item" (click)="logout()" type="button">
                <i class="icon-log-out"></i>
                <span>Déconnexion</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </nav>
  `,
    styles: [`
    .navbar {
      background-color: white;
      border-bottom: 1px solid #e5e7eb;
      height: 64px;
      position: sticky;
      top: 0;
      z-index: 1000;
      box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
    }

    .navbar-container {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 100%;
      padding: 0 1.5rem;
      max-width: 1920px;
      margin: 0 auto;
    }

    .navbar-left,
    .navbar-right {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .menu-toggle {
      display: none;
      background: none;
      border: none;
      cursor: pointer;
      padding: 0.5rem;
      color: #6b7280;
      font-size: 1.5rem;
    }

    .menu-toggle:hover {
      color: #111827;
    }

    .navbar-brand {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      text-decoration: none;
      color: #111827;
    }

    .navbar-logo {
      height: 32px;
      width: auto;
    }

    .navbar-title {
      font-size: 1.25rem;
      font-weight: 600;
    }

    .navbar-icon-btn {
      position: relative;
      background: none;
      border: none;
      cursor: pointer;
      padding: 0.5rem;
      color: #6b7280;
      font-size: 1.25rem;
      border-radius: 0.375rem;
      transition: all 0.2s;
    }

    .navbar-icon-btn:hover {
      background-color: #f3f4f6;
      color: #111827;
    }

    .badge {
      position: absolute;
      top: 0.25rem;
      right: 0.25rem;
      background-color: #ef4444;
      color: white;
      font-size: 0.75rem;
      font-weight: 600;
      padding: 0.125rem 0.375rem;
      border-radius: 9999px;
      min-width: 1.25rem;
      text-align: center;
    }

    .user-dropdown {
      position: relative;
    }

    .user-button {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      background: none;
      border: none;
      cursor: pointer;
      padding: 0.375rem 0.75rem;
      border-radius: 0.5rem;
      transition: background-color 0.2s;
    }

    .user-button:hover {
      background-color: #f3f4f6;
    }

    .user-avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background-color: #3b82f6;
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1rem;
    }

    .user-name {
      font-weight: 500;
      color: #111827;
    }

    .dropdown-menu {
      position: absolute;
      top: calc(100% + 0.5rem);
      right: 0;
      min-width: 240px;
      background-color: white;
      border: 1px solid #e5e7eb;
      border-radius: 0.5rem;
      box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
      overflow: hidden;
    }

    .dropdown-header {
      padding: 1rem;
      background-color: #f9fafb;
    }

    .user-info strong {
      display: block;
      font-size: 0.875rem;
      color: #111827;
    }

    .user-info small {
      display: block;
      font-size: 0.75rem;
      color: #6b7280;
      margin-top: 0.25rem;
    }

    .dropdown-divider {
      height: 1px;
      background-color: #e5e7eb;
      margin: 0.25rem 0;
    }

    .dropdown-item {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      width: 100%;
      padding: 0.75rem 1rem;
      background: none;
      border: none;
      cursor: pointer;
      text-decoration: none;
      color: #374151;
      font-size: 0.875rem;
      transition: background-color 0.2s;
    }

    .dropdown-item:hover {
      background-color: #f3f4f6;
    }

    .dropdown-item i {
      font-size: 1rem;
      color: #6b7280;
    }

    @media (max-width: 768px) {
      .menu-toggle {
        display: block;
      }

      .navbar-title {
        font-size: 1rem;
      }

      .user-name {
        display: none;
      }
    }
  `]
})
export class NavbarComponent implements OnInit {
    @Input() appName: string = 'Portail Suivi Doctorat';
    @Input() logoSrc?: string;
    @Input() showNotifications: boolean = true;
    @Input() showProfile: boolean = true;
    @Input() unreadCount: number = 0;

    @Output() menuToggle = new EventEmitter<void>();
    @Output() notificationClick = new EventEmitter<void>();

    currentUser: UserProfile | null = null;
    isDropdownOpen: boolean = false;

    constructor(
        private authService: AuthService,
        private router: Router
    ) { }

    ngOnInit(): void {
        // Load current user from auth service
        this.authService.currentUser$.subscribe(user => {
            if (user) {
                this.currentUser = {
                    id: user.id,
                    firstName: user.firstName,
                    lastName: user.lastName,
                    email: user.email,
                    roles: user.roles.map(r => r.name)
                };
            }
        });
    }

    toggleDropdown(): void {
        this.isDropdownOpen = !this.isDropdownOpen;
    }

    closeDropdown(): void {
        this.isDropdownOpen = false;
    }

    logout(): void {
        this.closeDropdown();
        this.authService.logout();
        this.router.navigate(['/login']);
    }
}
