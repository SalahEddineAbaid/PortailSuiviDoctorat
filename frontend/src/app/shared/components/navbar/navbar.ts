import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { AuthService, UserInfo } from '../../../core/services/auth.service';
import { Subject, takeUntil, filter } from 'rxjs';

export interface NavItem {
  label: string;
  route: string;
  icon?: string;
  roles?: string[];
  badge?: string | number;
  children?: NavItem[];
}

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Navbar implements OnInit, OnDestroy {
  currentUser: UserInfo | null = null;
  showUserMenu = false;
  showMobileMenu = false;
  isLoading = true;
  currentRoute = '';
  
  private destroy$ = new Subject<void>();

  // Navigation items based on user role
  navItems: NavItem[] = [];

  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Subscribe to user changes
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe(user => {
        this.currentUser = user;
        this.isLoading = false;
        this.updateNavItems();
        this.cdr.markForCheck();
      });

    // Subscribe to route changes
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((event: NavigationEnd) => {
        this.currentRoute = event.url;
        this.cdr.markForCheck();
      });

    // Close menus when clicking outside
    document.addEventListener('click', this.onDocumentClick.bind(this));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    document.removeEventListener('click', this.onDocumentClick.bind(this));
  }

  private updateNavItems(): void {
    if (!this.currentUser) {
      this.navItems = [];
      return;
    }

    const userRole = this.getUserRole();
    
    switch (userRole) {
      case 'ROLE_DOCTORANT':
        this.navItems = this.getDoctorantNavItems();
        break;
      case 'ROLE_DIRECTEUR':
        this.navItems = this.getDirecteurNavItems();
        break;
      case 'ROLE_ADMIN':
        this.navItems = this.getAdminNavItems();
        break;
      default:
        this.navItems = this.getDefaultNavItems();
    }
  }

  private getDoctorantNavItems(): NavItem[] {
    return [
      {
        label: 'Tableau de bord',
        route: '/dashboard',
        icon: 'dashboard'
      },
      {
        label: 'Inscription',
        route: '/inscription',
        icon: 'school',
        children: [
          { label: 'Nouvelle inscription', route: '/inscription/nouvelle' },
          { label: 'Réinscription', route: '/inscription/reinscription' },
          { label: 'Mes inscriptions', route: '/inscription/list' }
        ]
      },
      {
        label: 'Soutenance',
        route: '/soutenance',
        icon: 'event',
        children: [
          { label: 'Demande de soutenance', route: '/soutenance/nouvelle' },
          { label: 'Mes soutenances', route: '/soutenance/list' }
        ]
      },
      {
        label: 'Documents',
        route: '/documents',
        icon: 'folder'
      },
      {
        label: 'Notifications',
        route: '/notifications',
        icon: 'notifications'
      }
    ];
  }

  private getDirecteurNavItems(): NavItem[] {
    return [
      {
        label: 'Tableau de bord',
        route: '/dashboard',
        icon: 'dashboard'
      },
      {
        label: 'Mes doctorants',
        route: '/directeur/doctorants',
        icon: 'people'
      },
      {
        label: 'Dossiers à valider',
        route: '/directeur/validations',
        icon: 'assignment_turned_in',
        badge: '3' // This would come from a service
      },
      {
        label: 'Soutenances',
        route: '/directeur/soutenances',
        icon: 'event'
      },
      {
        label: 'Notifications',
        route: '/notifications',
        icon: 'notifications'
      }
    ];
  }

  private getAdminNavItems(): NavItem[] {
    return [
      {
        label: 'Tableau de bord',
        route: '/dashboard',
        icon: 'dashboard'
      },
      {
        label: 'Administration',
        route: '/admin',
        icon: 'admin_panel_settings',
        children: [
          { label: 'Gestion des campagnes', route: '/admin/campagnes' },
          { label: 'Validation des dossiers', route: '/admin/validations' },
          { label: 'Gestion des utilisateurs', route: '/admin/users' },
          { label: 'Paramétrage', route: '/admin/parametrage' }
        ]
      },
      {
        label: 'Statistiques',
        route: '/admin/statistics',
        icon: 'analytics'
      },
      {
        label: 'Notifications',
        route: '/notifications',
        icon: 'notifications'
      }
    ];
  }

  private getDefaultNavItems(): NavItem[] {
    return [
      {
        label: 'Tableau de bord',
        route: '/dashboard',
        icon: 'dashboard'
      }
    ];
  }

  toggleUserMenu(event: Event): void {
    event.stopPropagation();
    this.showUserMenu = !this.showUserMenu;
    this.showMobileMenu = false;
  }

  toggleMobileMenu(event: Event): void {
    event.stopPropagation();
    this.showMobileMenu = !this.showMobileMenu;
    this.showUserMenu = false;
  }

  private onDocumentClick(): void {
    this.showUserMenu = false;
    this.showMobileMenu = false;
    this.cdr.markForCheck();
  }

  getUserRole(): string {
    if (!this.currentUser?.roles?.length) {
      return '';
    }
    return this.currentUser.roles[0].name;
  }

  getUserRoleLabel(): string {
    const role = this.getUserRole();
    switch (role) {
      case 'ROLE_DOCTORANT':
        return 'Doctorant';
      case 'ROLE_DIRECTEUR':
        return 'Directeur de thèse';
      case 'ROLE_ADMIN':
        return 'Administrateur';
      default:
        return 'Utilisateur';
    }
  }

  getUserInitials(): string {
    if (!this.currentUser) return '';
    const firstName = this.currentUser.FirstName || '';
    const lastName = this.currentUser.LastName || '';
    return (firstName.charAt(0) + lastName.charAt(0)).toUpperCase();
  }

  isActiveRoute(route: string): boolean {
    return this.currentRoute.startsWith(route);
  }

  hasChildren(item: NavItem): boolean {
    return !!(item.children && item.children.length > 0);
  }

  goToProfile(): void {
    this.showUserMenu = false;
    this.router.navigate(['/profile']);
  }

  goToSettings(): void {
    this.showUserMenu = false;
    this.router.navigate(['/settings']);
  }

  logout(): void {
    this.showUserMenu = false;
    this.authService.logout();
  }

  trackByRoute(index: number, item: NavItem): string {
    return item.route;
  }
}