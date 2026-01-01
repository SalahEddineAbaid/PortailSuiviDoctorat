import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { DashboardService } from '../services/dashboard.service';
import { DoctorantDashboard as DoctorantDashboardModel } from '../models/dashboard.model';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { Sidebar, MenuItem } from '../../../shared/components/sidebar/sidebar';
import { TimelineComponent } from '../../../shared/components/timeline/timeline.component';
import { AlertComponent } from '../../../shared/components/alert/alert.component';
import { ProgressWidgetComponent } from '../../../shared/components/progress-widget/progress-widget.component';
import { StatusWidgetComponent } from '../../../shared/components/status-widget/status-widget.component';

@Component({
  selector: 'app-doctorant-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, 
    RouterModule,
    Navbar, 
    Sidebar, 
    TimelineComponent, 
    AlertComponent, 
    ProgressWidgetComponent, 
    StatusWidgetComponent
  ],
  templateUrl: './doctorant-dashboard.html',
  styleUrl: './doctorant-dashboard.scss'
})
export class DoctorantDashboard implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  dashboard: DoctorantDashboardModel | null = null;
  isLoading = false;
  error: string | null = null;

  menuItems: MenuItem[] = [
    { icon: 'fas fa-home', label: 'Accueil', route: '/dashboard/doctorant' },
    { icon: 'fas fa-user-plus', label: 'Inscription', route: '/inscription', badge: '1' },
    { icon: 'fas fa-graduation-cap', label: 'Soutenance', route: '/soutenance' },
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
      console.log('📊 [DOCTORANT DASHBOARD] Données reçues du resolver:', data);
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

    this.dashboardService.getDoctorantDashboard(this.dashboard.user.id)
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
    return this.dashboard?.user?.FirstName || 'Doctorant';
  }

  hasNotifications(): boolean {
    return (this.dashboard?.notifications?.length || 0) > 0;
  }

  getNotificationCount(): number {
    return this.dashboard?.notifications?.filter(n => !n.lu).length || 0;
  }

  hasInscriptions(): boolean {
    return (this.dashboard?.inscriptions?.length || 0) > 0;
  }

  getProgressPercentage(): number {
    return this.dashboard?.progression?.pourcentage || 0;
  }
}