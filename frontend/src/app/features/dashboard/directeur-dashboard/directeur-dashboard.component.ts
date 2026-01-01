import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { DashboardService } from '../services/dashboard.service';
import { DirecteurDashboard as DirecteurDashboardModel } from '../models/dashboard.model';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { Sidebar, MenuItem } from '../../../shared/components/sidebar/sidebar';

@Component({
  selector: 'app-directeur-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    Navbar,
    Sidebar
  ],
  templateUrl: './directeur-dashboard.component.html',
  styleUrls: ['./directeur-dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DirecteurDashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  dashboard: DirecteurDashboardModel | null = null;
  isLoading = false;
  error: string | null = null;

  menuItems: MenuItem[] = [
    { icon: 'fas fa-home', label: 'Accueil', route: '/dashboard/directeur' },
    { icon: 'fas fa-users', label: 'Mes doctorants', route: '/directeur/doctorants' },
    { icon: 'fas fa-tasks', label: 'Validations', route: '/directeur/validations' },
    { icon: 'fas fa-bell', label: 'Notifications', route: '/notifications' },
    { icon: 'fas fa-user', label: 'Mon profil', route: '/profile' }
  ];

  constructor(
    private route: ActivatedRoute,
    private dashboardService: DashboardService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // ✅ Récupérer les données préchargées par le resolver
    this.route.data.pipe(takeUntil(this.destroy$)).subscribe(data => {
      console.log('📊 [DIRECTEUR DASHBOARD] Données reçues du resolver:', data);
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
    if (!this.dashboard?.user?.id) return;

    this.isLoading = true;
    this.error = null;

    this.dashboardService.getDirecteurDashboard(this.dashboard.user.id)
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

  getUserName(): string {
    return this.dashboard?.user?.FirstName || 'Directeur';
  }

  hasDoctorants(): boolean {
    return (this.dashboard?.doctorants?.length || 0) > 0;
  }

  hasDemandesEnAttente(): boolean {
    return (this.dashboard?.demandesEnAttente?.length || 0) > 0;
  }

  hasNotifications(): boolean {
    return (this.dashboard?.notifications?.length || 0) > 0;
  }

  getNotificationCount(): number {
    return this.dashboard?.notifications?.filter(n => !n.lu).length || 0;
  }

  /**
   * 🎯 Actions sur les demandes
   */
  onApproveRequest(demandeId: number): void {
    console.log('✅ Approuver demande:', demandeId);
    // TODO: Implémenter l'approbation
  }

  onRejectRequest(demandeId: number): void {
    console.log('❌ Rejeter demande:', demandeId);
    // TODO: Implémenter le rejet
  }

  onViewRequest(demandeId: number): void {
    console.log('👁️ Voir demande:', demandeId);
    // TODO: Navigation vers détail
  }
}