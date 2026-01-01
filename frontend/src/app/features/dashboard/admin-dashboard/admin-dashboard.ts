import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { DashboardService } from '../services/dashboard.service';
import { AdminDashboard as AdminDashboardModel } from '../models/dashboard.model';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { Sidebar, MenuItem } from '../../../shared/components/sidebar/sidebar';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    Navbar,
    Sidebar
  ],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminDashboard implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  dashboard: AdminDashboardModel | null = null;
  isLoading = false;
  error: string | null = null;

  menuItems: MenuItem[] = [
    { icon: 'fas fa-home', label: 'Accueil', route: '/dashboard/admin' },
    { icon: 'fas fa-users', label: 'Utilisateurs', route: '/admin/users' },
    { icon: 'fas fa-calendar-alt', label: 'Campagnes', route: '/admin/campagnes' },
    { icon: 'fas fa-check-circle', label: 'Validations', route: '/admin/validations' },
    { icon: 'fas fa-cogs', label: 'Paramétrage', route: '/admin/parametrage' },
    { icon: 'fas fa-bell', label: 'Notifications', route: '/notifications' },
    { icon: 'fas fa-user', label: 'Mon profil', route: '/profile' }
  ];

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
      route: '/admin/users',
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
    private route: ActivatedRoute,
    private dashboardService: DashboardService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // ✅ Récupérer les données préchargées par le resolver
    this.route.data.pipe(takeUntil(this.destroy$)).subscribe(data => {
      console.log('📊 [ADMIN DASHBOARD] Données reçues du resolver:', data);
      this.dashboard = data['dashboard'];
      
      if (!this.dashboard) {
        this.error = 'Impossible de charger les données du dashboard';
      }
      
      this.cdr.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * 🔄 Rafraîchir manuellement le dashboard
   */
  refreshDashboard(): void {
    this.isLoading = true;
    this.error = null;

    this.dashboardService.getAdminDashboard()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.dashboard = data;
          this.isLoading = false;
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('❌ Erreur rafraîchissement dashboard:', error);
          this.error = 'Erreur lors du rafraîchissement';
          this.isLoading = false;
          this.cdr.markForCheck();
        }
      });
  }

  /**
   * 🎯 Getters pour le template
   */
  getWelcomeMessage(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Bonjour';
    if (hour < 18) return 'Bon après-midi';
    return 'Bonsoir';
  }

  getSystemHealthClass(): string {
    if (!this.dashboard) return 'unknown';
    return this.dashboard.statistics.systemHealth;
  }

  getSystemHealthIcon(): string {
    const health = this.dashboard?.statistics.systemHealth;
    switch (health) {
      case 'healthy': return 'fa-check-circle';
      case 'warning': return 'fa-exclamation-triangle';
      case 'critical': return 'fa-times-circle';
      default: return 'fa-question-circle';
    }
  }

  hasCampagnes(): boolean {
    return (this.dashboard?.campagnes?.length || 0) > 0;
  }

  hasActiveUsers(): boolean {
    return (this.dashboard?.activeUsers?.length || 0) > 0;
  }

  hasAuditLogs(): boolean {
    return (this.dashboard?.recentAudits?.length || 0) > 0;
  }

  hasSystemAlerts(): boolean {
    return (this.dashboard?.systemAlerts?.length || 0) > 0;
  }
}