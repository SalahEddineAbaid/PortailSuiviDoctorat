import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { UserService } from '../../../core/services/user.service';
import { DashboardService } from '../../../core/services/dashboard.service';
import { AdminDashboardData, DashboardStats } from '../../../core/models/dashboard.model';
import { AlertComponent } from '../../../shared/components/alert/alert.component';
import { StatisticsComponent } from '../../../shared/components/statistics/statistics.component';
import { DossierValidationListComponent, ValidationAction } from '../../../shared/components/dossier-validation-list/dossier-validation-list.component';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    AlertComponent,
    StatisticsComponent,
    DossierValidationListComponent
  ],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss'
})
export class AdminDashboard implements OnInit {
  dashboardData$!: Observable<AdminDashboardData>;
  loading = true;

  // Raccourcis de gestion
  managementShortcuts = [
    {
      title: 'Gestion des Campagnes',
      description: 'Créer et gérer les campagnes d\'inscription',
      icon: 'fas fa-calendar-alt',
      route: '/admin/campagnes',
      color: 'blue'
    },
    {
      title: 'Validation des Dossiers',
      description: 'Valider les inscriptions et soutenances',
      icon: 'fas fa-check-circle',
      route: '/admin/validations',
      color: 'green'
    },
    {
      title: 'Gestion des Utilisateurs',
      description: 'Administrer les comptes utilisateurs',
      icon: 'fas fa-users',
      route: '/admin/utilisateurs',
      color: 'purple'
    },
    {
      title: 'Paramétrage Système',
      description: 'Configurer les règles et seuils',
      icon: 'fas fa-cogs',
      route: '/admin/parametrage',
      color: 'orange'
    }
  ];

  constructor(
    private userService: UserService,
    private dashboardService: DashboardService
  ) {}

  ngOnInit(): void {
    console.log('✅ AdminDashboard chargé');
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    this.loading = true;
    this.dashboardData$ = this.dashboardService.getAdminDashboardData();
    
    // Simuler le chargement
    setTimeout(() => {
      this.loading = false;
    }, 1000);
  }

  onRefresh(): void {
    this.loadDashboardData();
  }

  onValidationAction(action: ValidationAction): void {
    console.log('Action de validation:', action);
    
    switch (action.action) {
      case 'consulter':
        this.consulterDossier(action.dossier);
        break;
      case 'valider':
        this.validerDossier(action.dossier);
        break;
      case 'rejeter':
        this.rejeterDossier(action.dossier);
        break;
    }
  }

  onFilterChanged(filters: {type?: string, priorite?: string, search?: string}): void {
    console.log('Filtres changés:', filters);
    // Ici on pourrait implémenter une logique de filtrage côté serveur
    // Pour l'instant, le filtrage se fait côté client dans le composant
  }

  private consulterDossier(dossier: any): void {
    // Navigation vers la page de consultation du dossier
    const route = dossier.type === 'inscription' 
      ? `/admin/inscriptions/${dossier.id}`
      : `/admin/soutenances/${dossier.id}`;
    
    console.log(`Navigation vers: ${route}`);
    // this.router.navigate([route]);
  }

  private validerDossier(dossier: any): void {
    // Logique de validation du dossier
    console.log(`Validation du dossier ${dossier.type} #${dossier.id}`);
    
    // Ici on appellerait le service approprié pour valider
    // Puis on rechargerait les données
    // this.loadDashboardData();
  }

  private rejeterDossier(dossier: any): void {
    // Logique de rejet du dossier
    console.log(`Rejet du dossier ${dossier.type} #${dossier.id}`);
    
    // Ici on appellerait le service approprié pour rejeter
    // Puis on rechargerait les données
    // this.loadDashboardData();
  }
}