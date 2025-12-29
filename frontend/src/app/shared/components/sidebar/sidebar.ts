import { Component, Input, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { AuthService, UserInfo } from '../../../core/services/auth.service';
import { Subject, takeUntil, filter } from 'rxjs';

export interface MenuItem {
  icon: string;
  label: string;
  route: string;
  badge?: string | number;
  children?: MenuItem[];
  roles?: string[];
  divider?: boolean;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Sidebar implements OnInit, OnDestroy {
  @Input() title: string = 'Menu';
  @Input() customMenuItems: MenuItem[] = [];
  
  currentUser: UserInfo | null = null;
  menuItems: MenuItem[] = [];
  isCollapsed = false;
  currentRoute = '';
  expandedItems: Set<string> = new Set();
  
  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Subscribe to user changes
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe(user => {
        this.currentUser = user;
        this.updateMenuItems();
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
        this.autoExpandActiveItems();
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateMenuItems(): void {
    if (this.customMenuItems.length > 0) {
      this.menuItems = this.filterMenuItemsByRole(this.customMenuItems);
      return;
    }

    if (!this.currentUser) {
      this.menuItems = [];
      return;
    }

    const userRole = this.getUserRole();
    
    switch (userRole) {
      case 'ROLE_DOCTORANT':
        this.menuItems = this.getDoctorantMenuItems();
        break;
      case 'ROLE_DIRECTEUR':
        this.menuItems = this.getDirecteurMenuItems();
        break;
      case 'ROLE_ADMIN':
        this.menuItems = this.getAdminMenuItems();
        break;
      default:
        this.menuItems = this.getDefaultMenuItems();
    }
  }

  private filterMenuItemsByRole(items: MenuItem[]): MenuItem[] {
    if (!this.currentUser) return [];
    
    const userRole = this.getUserRole();
    
    return items.filter(item => {
      if (!item.roles || item.roles.length === 0) return true;
      return item.roles.includes(userRole);
    }).map(item => ({
      ...item,
      children: item.children ? this.filterMenuItemsByRole(item.children) : undefined
    }));
  }

  private getDoctorantMenuItems(): MenuItem[] {
    return [
      {
        icon: 'dashboard',
        label: 'Tableau de bord',
        route: '/dashboard'
      },
      {
        icon: 'school',
        label: 'Inscription',
        route: '/inscription',
        children: [
          { icon: 'add', label: 'Nouvelle inscription', route: '/inscription/nouvelle' },
          { icon: 'refresh', label: 'Réinscription', route: '/inscription/reinscription' },
          { icon: 'list', label: 'Mes inscriptions', route: '/inscription/list' }
        ]
      },
      {
        icon: 'event',
        label: 'Soutenance',
        route: '/soutenance',
        children: [
          { icon: 'add', label: 'Demande de soutenance', route: '/soutenance/nouvelle' },
          { icon: 'list', label: 'Mes soutenances', route: '/soutenance/list' }
        ]
      },
      { divider: true, icon: '', label: '', route: '' },
      {
        icon: 'folder',
        label: 'Documents',
        route: '/documents'
      },
      {
        icon: 'notifications',
        label: 'Notifications',
        route: '/notifications'
      },
      {
        icon: 'help',
        label: 'Aide',
        route: '/help'
      }
    ];
  }

  private getDirecteurMenuItems(): MenuItem[] {
    return [
      {
        icon: 'dashboard',
        label: 'Tableau de bord',
        route: '/dashboard'
      },
      {
        icon: 'people',
        label: 'Mes doctorants',
        route: '/directeur/doctorants'
      },
      {
        icon: 'assignment_turned_in',
        label: 'Dossiers à valider',
        route: '/directeur/validations',
        badge: '3'
      },
      {
        icon: 'event',
        label: 'Soutenances',
        route: '/directeur/soutenances'
      },
      { divider: true, icon: '', label: '', route: '' },
      {
        icon: 'notifications',
        label: 'Notifications',
        route: '/notifications'
      },
      {
        icon: 'help',
        label: 'Aide',
        route: '/help'
      }
    ];
  }

  private getAdminMenuItems(): MenuItem[] {
    return [
      {
        icon: 'dashboard',
        label: 'Tableau de bord',
        route: '/dashboard'
      },
      {
        icon: 'admin_panel_settings',
        label: 'Administration',
        route: '/admin',
        children: [
          { icon: 'campaign', label: 'Gestion des campagnes', route: '/admin/campagnes' },
          { icon: 'assignment_turned_in', label: 'Validation des dossiers', route: '/admin/validations' },
          { icon: 'people', label: 'Gestion des utilisateurs', route: '/admin/users' },
          { icon: 'settings', label: 'Paramétrage', route: '/admin/parametrage' }
        ]
      },
      {
        icon: 'analytics',
        label: 'Statistiques',
        route: '/admin/statistics'
      },
      { divider: true, icon: '', label: '', route: '' },
      {
        icon: 'notifications',
        label: 'Notifications',
        route: '/notifications'
      },
      {
        icon: 'help',
        label: 'Aide',
        route: '/help'
      }
    ];
  }

  private getDefaultMenuItems(): MenuItem[] {
    return [
      {
        icon: 'dashboard',
        label: 'Tableau de bord',
        route: '/dashboard'
      }
    ];
  }

  private getUserRole(): string {
    if (!this.currentUser?.roles?.length) {
      return '';
    }
    return this.currentUser.roles[0].name;
  }

  private autoExpandActiveItems(): void {
    this.menuItems.forEach(item => {
      if (item.children) {
        const hasActiveChild = item.children.some(child => 
          this.currentRoute.startsWith(child.route)
        );
        if (hasActiveChild) {
          this.expandedItems.add(item.route);
        }
      }
    });
  }

  toggleSidebar(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  toggleMenuItem(item: MenuItem, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    
    if (!item.children || item.children.length === 0) {
      return;
    }

    if (this.expandedItems.has(item.route)) {
      this.expandedItems.delete(item.route);
    } else {
      this.expandedItems.add(item.route);
    }
  }

  isActiveRoute(route: string): boolean {
    return this.currentRoute === route || this.currentRoute.startsWith(route + '/');
  }

  isExpanded(item: MenuItem): boolean {
    return this.expandedItems.has(item.route);
  }

  hasChildren(item: MenuItem): boolean {
    return !!(item.children && item.children.length > 0);
  }

  trackByRoute(index: number, item: MenuItem): string {
    return item.route + item.label;
  }
}