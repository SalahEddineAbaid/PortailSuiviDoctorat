import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

interface AdminMenuItem {
  id: string;
  title: string;
  description: string;
  icon: string;
  route: string;
  color: string;
  badge?: {
    text: string;
    type: 'primary' | 'success' | 'warning' | 'danger';
  };
}

@Component({
  selector: 'app-admin-menu',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <nav class="admin-menu">
      <div class="menu-section">
        <h3 class="section-title">Gestion Administrative</h3>
        <ul class="menu-list">
          <li *ngFor="let item of menuItems" class="menu-item">
            <a 
              [routerLink]="item.route" 
              routerLinkActive="active"
              class="menu-link"
              [class]="'menu-link-' + item.color"
            >
              <div class="menu-icon">
                <i [class]="item.icon"></i>
              </div>
              <div class="menu-content">
                <div class="menu-title">
                  {{ item.title }}
                  <span 
                    *ngIf="item.badge" 
                    class="badge"
                    [class]="'badge-' + item.badge.type"
                  >
                    {{ item.badge.text }}
                  </span>
                </div>
                <div class="menu-description">{{ item.description }}</div>
              </div>
              <div class="menu-arrow">
                <i class="fas fa-chevron-right"></i>
              </div>
            </a>
          </li>
        </ul>
      </div>

      <div class="menu-section">
        <h3 class="section-title">Actions Rapides</h3>
        <div class="quick-actions">
          <button class="quick-action-btn" (click)="createCampagne()">
            <i class="fas fa-plus"></i>
            Nouvelle Campagne
          </button>
          <button class="quick-action-btn" (click)="exportData()">
            <i class="fas fa-download"></i>
            Exporter Données
          </button>
        </div>
      </div>
    </nav>
  `,
  styleUrls: ['./admin-menu.component.scss']
})
export class AdminMenuComponent implements OnInit {
  currentRoute = '';

  menuItems: AdminMenuItem[] = [
    {
      id: 'accueil',
      title: 'Accueil',
      description: 'Tableau de bord administrateur',
      icon: 'fas fa-home',
      route: '/dashboard/admin',
      color: 'gray'
    },
    {
      id: 'campagnes',
      title: 'Gestion des Campagnes',
      description: 'Créer et gérer les campagnes d\'inscription',
      icon: 'fas fa-calendar-alt',
      route: '/admin/campagnes',
      color: 'blue',
      badge: {
        text: '3 actives',
        type: 'primary'
      }
    },
    {
      id: 'validations',
      title: 'Validation des Dossiers',
      description: 'Valider les inscriptions et soutenances',
      icon: 'fas fa-check-circle',
      route: '/admin/validations',
      color: 'green',
      badge: {
        text: '12 en attente',
        type: 'warning'
      }
    },
    {
      id: 'utilisateurs',
      title: 'Gestion des Utilisateurs',
      description: 'Administrer les comptes utilisateurs',
      icon: 'fas fa-users',
      route: '/admin/users',
      color: 'purple'
    },
    {
      id: 'parametrage',
      title: 'Paramétrage Système',
      description: 'Configurer les règles et seuils',
      icon: 'fas fa-cogs',
      route: '/admin/parametrage',
      color: 'orange'
    },
    {
      id: 'notifications',
      title: 'Notifications',
      description: 'Gérer les notifications',
      icon: 'fas fa-bell',
      route: '/notifications',
      color: 'yellow'
    },
    {
      id: 'profil',
      title: 'Mon profil',
      description: 'Voir et modifier mon profil',
      icon: 'fas fa-user',
      route: '/profile',
      color: 'teal'
    }
  ];

  constructor(private router: Router) {}

  ngOnInit(): void {
    // Suivre les changements de route pour mettre à jour l'état actif
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.currentRoute = event.url;
      });
  }

  createCampagne(): void {
    this.router.navigate(['/admin/campagnes'], { 
      queryParams: { action: 'create' } 
    });
  }

  exportData(): void {
    // Logique d'export des données
    console.log('Export des données administratives');
    // Ici on pourrait ouvrir un modal ou déclencher un téléchargement
  }
}