import { Injectable } from '@angular/core';
import { Observable, of, map, catchError } from 'rxjs';
import { AuthService } from './auth.service';
import {
  DoctorantDashboardData,
  DirecteurDashboardData,
  AdminDashboardData,
  DashboardAlert,
  TimelineEvent,
  ProgressIndicator,
  DashboardStats
} from '../models/dashboard.model';
import { StatusWidgetData } from '../../shared/components/status-widget/status-widget.component';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  constructor(
    private authService: AuthService
  ) {}

  // ===== DOCTORANT DASHBOARD =====

  getDoctorantDashboardData(): Observable<DoctorantDashboardData> {
    return of({
      inscriptionActuelle: undefined,
      prochaineSoutenance: undefined,
      notifications: [],
      documentsManquants: [],
      alertes: [],
      timeline: []
    });
  }

  getDoctorantStatusWidgets(): Observable<StatusWidgetData[]> {
    return of([
      {
        title: 'Inscription',
        value: 'En cours',
        subtitle: 'Année 2024-2025',
        icon: 'fas fa-user-graduate',
        color: 'blue' as const,
        actionLabel: 'Voir détails',
        actionRoute: '/inscription'
      },
      {
        title: 'Soutenance',
        value: 'Non planifiée',
        icon: 'fas fa-graduation-cap',
        color: 'gray' as const,
        actionLabel: 'Voir détails',
        actionRoute: '/soutenance'
      },
      {
        title: 'Notifications',
        value: 0,
        subtitle: '0 au total',
        icon: 'fas fa-bell',
        color: 'gray' as const,
        actionLabel: 'Voir toutes',
        actionRoute: '/notifications'
      }
    ]);
  }

  getDoctorantProgressIndicators(): Observable<ProgressIndicator[]> {
    return of([
      {
        current: 1,
        total: 5,
        label: 'Progression générale',
        percentage: 20
      },
      {
        current: 0,
        total: 3,
        label: 'Documents validés',
        percentage: 0
      }
    ]);
  }

  // ===== DIRECTEUR DASHBOARD =====

  getDirecteurDashboardData(): Observable<DirecteurDashboardData> {
    return of({
      doctorants: [],
      dossiersEnAttente: [],
      soutenancesAPlanifier: [],
      statistiques: this.getEmptyStats()
    });
  }

  getDirecteurStats(): Observable<DashboardStats> {
    return of(this.getEmptyStats());
  }

  getDirecteurAlerts(): Observable<DashboardAlert[]> {
    return of([]);
  }

  // ===== ADMIN DASHBOARD =====

  getAdminDashboardData(): Observable<AdminDashboardData> {
    return of({
      statistiques: this.getEmptyStats(),
      campagneActive: undefined,
      dossiersEnAttente: [],
      utilisateursRecents: []
    });
  }

  // ===== UTILITY METHODS =====

  private getEmptyStats(): DashboardStats {
    return {
      totalInscriptions: 0,
      inscriptionsEnAttente: 0,
      inscriptionsValidees: 0,
      inscriptionsRejetees: 0,
      totalSoutenances: 0,
      soutenancesEnAttente: 0,
      soutenancesAutorisees: 0
    };
  }
}
