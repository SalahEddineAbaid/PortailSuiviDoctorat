import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, NavigationEnd, ActivatedRoute } from '@angular/router';
import { Subject, takeUntil, filter } from 'rxjs';

export interface BreadcrumbItem {
  label: string;
  route?: string;
  icon?: string;
  active?: boolean;
}

@Component({
  selector: 'app-breadcrumb',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="breadcrumb" *ngIf="breadcrumbs.length > 0" aria-label="Fil d'Ariane">
      <ol class="breadcrumb-list">
        <li 
          *ngFor="let item of breadcrumbs; let last = last; trackBy: trackByLabel"
          class="breadcrumb-item"
          [class.active]="item.active || last"
        >
          <a 
            *ngIf="item.route && !item.active && !last"
            [routerLink]="item.route"
            class="breadcrumb-link"
            [attr.aria-label]="'Naviguer vers ' + item.label"
          >
            <span *ngIf="item.icon" class="material-icons breadcrumb-icon" aria-hidden="true">{{ item.icon }}</span>
            <span class="breadcrumb-text">{{ item.label }}</span>
          </a>
          
          <span 
            *ngIf="!item.route || item.active || last"
            class="breadcrumb-current"
            [attr.aria-current]="last ? 'page' : null"
          >
            <span *ngIf="item.icon" class="material-icons breadcrumb-icon" aria-hidden="true">{{ item.icon }}</span>
            <span class="breadcrumb-text">{{ item.label }}</span>
          </span>
          
          <span 
            *ngIf="!last" 
            class="material-icons breadcrumb-separator" 
            aria-hidden="true"
          >
            chevron_right
          </span>
        </li>
      </ol>
    </nav>
  `,
  styleUrls: ['./breadcrumb.component.scss']
})
export class BreadcrumbComponent implements OnInit, OnDestroy {
  breadcrumbs: BreadcrumbItem[] = [];
  
  private destroy$ = new Subject<void>();
  
  // Route to breadcrumb mapping
  private routeMap: { [key: string]: BreadcrumbItem } = {
    '/dashboard': { label: 'Tableau de bord', icon: 'dashboard' },
    
    // Inscription routes
    '/inscription': { label: 'Inscription', icon: 'school' },
    '/inscription/nouvelle': { label: 'Nouvelle inscription' },
    '/inscription/reinscription': { label: 'Réinscription' },
    '/inscription/list': { label: 'Mes inscriptions' },
    
    // Soutenance routes
    '/soutenance': { label: 'Soutenance', icon: 'event' },
    '/soutenance/nouvelle': { label: 'Demande de soutenance' },
    '/soutenance/list': { label: 'Mes soutenances' },
    
    // Directeur routes
    '/directeur': { label: 'Espace Directeur', icon: 'people' },
    '/directeur/doctorants': { label: 'Mes doctorants' },
    '/directeur/validations': { label: 'Dossiers à valider' },
    '/directeur/soutenances': { label: 'Soutenances' },
    
    // Admin routes
    '/admin': { label: 'Administration', icon: 'admin_panel_settings' },
    '/admin/campagnes': { label: 'Gestion des campagnes' },
    '/admin/validations': { label: 'Validation des dossiers' },
    '/admin/users': { label: 'Gestion des utilisateurs' },
    '/admin/parametrage': { label: 'Paramétrage' },
    '/admin/statistics': { label: 'Statistiques' },
    
    // Other routes
    '/documents': { label: 'Documents', icon: 'folder' },
    '/notifications': { label: 'Notifications', icon: 'notifications' },
    '/profile': { label: 'Mon profil', icon: 'person' },
    '/settings': { label: 'Paramètres', icon: 'settings' },
    '/help': { label: 'Aide', icon: 'help' }
  };

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.buildBreadcrumbs();
        this.cdr.markForCheck();
      });

    // Build initial breadcrumbs
    this.buildBreadcrumbs();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private buildBreadcrumbs(): void {
    const url = this.router.url;
    const urlSegments = url.split('/').filter(segment => segment);
    
    this.breadcrumbs = [];
    
    // Always start with home/dashboard
    if (url !== '/dashboard') {
      this.breadcrumbs.push({
        label: 'Accueil',
        route: '/dashboard',
        icon: 'home'
      });
    }
    
    // Build breadcrumbs from URL segments
    let currentPath = '';
    
    for (let i = 0; i < urlSegments.length; i++) {
      currentPath += '/' + urlSegments[i];
      
      const breadcrumbItem = this.routeMap[currentPath];
      
      if (breadcrumbItem) {
        this.breadcrumbs.push({
          ...breadcrumbItem,
          route: currentPath,
          active: i === urlSegments.length - 1
        });
      } else {
        // Handle dynamic routes (e.g., /inscription/123)
        const dynamicItem = this.getDynamicBreadcrumb(currentPath, urlSegments[i]);
        if (dynamicItem) {
          this.breadcrumbs.push({
            ...dynamicItem,
            route: currentPath,
            active: i === urlSegments.length - 1
          });
        }
      }
    }
  }

  private getDynamicBreadcrumb(path: string, segment: string): BreadcrumbItem | null {
    // Handle ID-based routes
    if (/^\d+$/.test(segment)) {
      const parentPath = path.substring(0, path.lastIndexOf('/'));
      const parentItem = this.routeMap[parentPath];
      
      if (parentItem) {
        return {
          label: `Détails`,
          icon: 'info'
        };
      }
    }
    
    // Handle edit routes
    if (segment === 'edit') {
      return {
        label: 'Modifier',
        icon: 'edit'
      };
    }
    
    // Handle view routes
    if (segment === 'view') {
      return {
        label: 'Voir',
        icon: 'visibility'
      };
    }
    
    // Default fallback
    return {
      label: this.formatSegmentLabel(segment)
    };
  }

  private formatSegmentLabel(segment: string): string {
    return segment
      .split('-')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  trackByLabel(index: number, item: BreadcrumbItem): string {
    return item.label + (item.route || '');
  }
}