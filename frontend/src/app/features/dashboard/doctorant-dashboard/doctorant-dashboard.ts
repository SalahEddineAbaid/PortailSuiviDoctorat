import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { UserService } from '../../../core/services/user.service';
import { DashboardService } from '../../../core/services/dashboard.service';
import { UserResponse } from '../../../core/models/user.model';
import { DoctorantDashboardData, DashboardAlert, TimelineEvent, ProgressIndicator } from '../../../core/models/dashboard.model';
import { StatusWidgetData } from '../../../shared/components/status-widget/status-widget.component';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { Sidebar, MenuItem } from '../../../shared/components/sidebar/sidebar';
import { TimelineComponent } from '../../../shared/components/timeline/timeline.component';
import { AlertComponent } from '../../../shared/components/alert/alert.component';
import { ProgressWidgetComponent } from '../../../shared/components/progress-widget/progress-widget.component';
import { StatusWidgetComponent } from '../../../shared/components/status-widget/status-widget.component';

@Component({
  selector: 'app-doctorant-dashboard',
  standalone: true,
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
  
  user: UserResponse | null = null;
  dashboardData: DoctorantDashboardData | null = null;
  statusWidgets: StatusWidgetData[] = [];
  progressIndicators: ProgressIndicator[] = [];
  isLoading = true;
  error: string | null = null;

  menuItems: MenuItem[] = [
    { icon: 'fas fa-home', label: 'Accueil', route: '/dashboard/doctorant' },
    { icon: 'fas fa-user-plus', label: 'Inscription', route: '/inscription', badge: '1' },
    { icon: 'fas fa-graduation-cap', label: 'Soutenance', route: '/soutenance' },
    { icon: 'fas fa-bell', label: 'Notifications', route: '/notifications' },
    { icon: 'fas fa-user', label: 'Mon profil', route: '/auth/profile' }
  ];

  constructor(
    private userService: UserService,
    private dashboardService: DashboardService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadDashboardData(): void {
    this.isLoading = true;
    this.error = null;

    // Charger les données utilisateur
    this.userService.getCurrentUser()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (user) => {
          this.user = user;
        },
        error: (error) => {
          console.error('❌ Erreur chargement utilisateur:', error);
          this.error = 'Erreur lors du chargement des informations utilisateur';
        }
      });

    // Charger les données du dashboard
    this.dashboardService.getDoctorantDashboardData()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.dashboardData = data;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('❌ Erreur chargement dashboard:', error);
          this.error = 'Erreur lors du chargement du dashboard';
          this.isLoading = false;
        }
      });

    // Charger les widgets de statut
    this.dashboardService.getDoctorantStatusWidgets()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (widgets) => {
          this.statusWidgets = widgets;
        },
        error: (error) => {
          console.error('❌ Erreur chargement widgets:', error);
        }
      });

    // Charger les indicateurs de progression
    this.dashboardService.getDoctorantProgressIndicators()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (indicators) => {
          this.progressIndicators = indicators;
        },
        error: (error) => {
          console.error('❌ Erreur chargement indicateurs:', error);
        }
      });
  }

  onAlertDismiss(alertId: string): void {
    if (this.dashboardData) {
      this.dashboardData.alertes = this.dashboardData.alertes.filter(alert => alert.id !== alertId);
    }
  }

  refreshDashboard(): void {
    this.loadDashboardData();
  }

  get userInfo(): UserResponse | null {
    return this.user;
  }

  getWelcomeMessage(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Bonjour';
    if (hour < 18) return 'Bon après-midi';
    return 'Bonsoir';
  }

  getUserName(): string {
    return this.user?.FirstName || 'Doctorant';
  }

  hasAlerts(): boolean {
    return !!(this.dashboardData?.alertes && this.dashboardData.alertes.length > 0);
  }

  hasTimeline(): boolean {
    return !!(this.dashboardData?.timeline && this.dashboardData.timeline.length > 0);
  }

  getNotificationCount(): number {
    return this.dashboardData?.notifications?.filter(n => !n.lue).length || 0;
  }
}