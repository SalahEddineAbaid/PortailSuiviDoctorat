import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  DoctorantDashboard,
  DirecteurDashboard,
  AdminDashboard,
  InscriptionSummary,
  NotificationSummary
} from '../models/dashboard.model';
import { AuthService, UserInfo } from '../../../core/services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly API_URL = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  /**
   * 🎓 Récupérer le dashboard complet du doctorant
   */
  getDoctorantDashboard(userId: number): Observable<DoctorantDashboard> {
    console.log('📊 [DASHBOARD SERVICE] Chargement dashboard doctorant:', userId);

    return forkJoin({
      // Endpoint principal qui retourne toutes les données
      dashboardData: this.http.get<any>(`${this.API_URL}/inscriptions/doctorant/${userId}/dashboard`).pipe(
        catchError(error => {
          console.warn('⚠️ [DASHBOARD SERVICE] Erreur endpoint dashboard, utilisation données par défaut:', error);
          return of({
            totalInscriptions: 0,
            inscriptionsEnCours: 0,
            inscriptionsValidees: 0,
            inscriptionsEnAttente: 0,
            documentsManquants: 0,
            progressionThese: 0,
            anneesEcoulees: 0,
            anneesRestantes: 3,
            inscriptions: []
          });
        })
      ),
      // Notifications non lues
      notifications: this.http.get<NotificationSummary[]>(`${this.API_URL}/notifications/user/${userId}/unread`).pipe(
        catchError(() => of([]))
      ),
      // Profil utilisateur
      user: this.authService.getCurrentUser().pipe(
        catchError(() => of(null))
      )
    }).pipe(
      map(({ dashboardData, notifications, user }) => {
        console.log('✅ [DASHBOARD SERVICE] Données chargées:', dashboardData);

        return {
          user: (user || {}) as UserInfo,
          statistics: {
            totalInscriptions: dashboardData.totalInscriptions || 0,
            inscriptionsEnCours: dashboardData.inscriptionsEnCours || 0,
            inscriptionsValidees: dashboardData.inscriptionsValidees || 0,
            inscriptionsEnAttente: dashboardData.inscriptionsEnAttente || 0,
            documentsManquants: dashboardData.documentsManquants || 0,
            progressionThese: dashboardData.progressionThese || 0,
            anneesEcoulees: dashboardData.anneesEcoulees || 0,
            anneesRestantes: dashboardData.anneesRestantes || 0
          },
          inscriptions: dashboardData.inscriptions || [],
          notifications: notifications.slice(0, 5), // 5 dernières
          quickActions: this.getDoctorantQuickActions(),
          progression: {
            etapeActuelle: dashboardData.etapeActuelle || 'Inscription',
            etapesCompletees: dashboardData.etapesCompletees || 0,
            etapesTotales: dashboardData.etapesTotales || 6,
            pourcentage: dashboardData.progressionThese || 0,
            prochaineMilestone: dashboardData.prochaineMilestone || 'Inscription',
            dateDebutThese: dashboardData.dateDebutThese,
            dureeEcoulee: dashboardData.anneesEcoulees || 0,
            dureeRestante: dashboardData.anneesRestantes || 3
          }
        } as DoctorantDashboard;
      }),
      catchError(error => {
        console.error('❌ [DASHBOARD SERVICE] Erreur chargement dashboard doctorant:', error);
        throw error;
      })
    );
  }

  /**
   * 👨‍🏫 Récupérer le dashboard complet du directeur
   */
  getDirecteurDashboard(userId: number): Observable<DirecteurDashboard> {
    console.log('📊 [DASHBOARD SERVICE] Chargement dashboard directeur:', userId);

    return forkJoin({
      // Inscriptions en attente de validation
      demandesEnAttente: this.http.get<any[]>(`${this.API_URL}/inscriptions/directeur/${userId}/en-attente`).pipe(
        catchError(() => of([]))
      ),
      // Notifications
      notifications: this.http.get<NotificationSummary[]>(`${this.API_URL}/notifications/user/${userId}/unread`).pipe(
        catchError(() => of([]))
      ),
      // Profil utilisateur
      user: this.authService.getCurrentUser().pipe(
        catchError(() => of(null))
      )
    }).pipe(
      map(({ demandesEnAttente, notifications, user }) => {
        console.log('✅ [DASHBOARD SERVICE] Données directeur chargées');

        // Extraire les doctorants uniques des demandes
        const doctorantsMap = new Map();
        demandesEnAttente.forEach((demande: any) => {
          if (!doctorantsMap.has(demande.doctorantId)) {
            doctorantsMap.set(demande.doctorantId, {
              id: demande.doctorantId,
              nom: demande.doctorantNom || 'N/A',
              prenom: demande.doctorantPrenom || 'N/A',
              email: demande.doctorantEmail || '',
              sujetThese: demande.sujetThese || '',
              dateDebutThese: demande.dateDebutThese,
              anneesEcoulees: demande.anneesEcoulees || 0,
              statut: 'actif',
              dernierInscriptionStatut: demande.statut,
              alertes: []
            });
          }
        });

        const doctorants = Array.from(doctorantsMap.values());

        return {
          user: (user || {}) as UserInfo,
          statistics: {
            totalDoctorants: doctorants.length,
            doctorantsActifs: doctorants.length,
            demandesEnAttente: demandesEnAttente.length,
            demandesValidees: 0,
            demandesRejetees: 0,
            tauxValidation: 0,
            moyenneDelaiValidation: 0
          },
          doctorants: doctorants,
          demandesEnAttente: demandesEnAttente.map((demande: any) => ({
            id: demande.id,
            type: 'inscription',
            doctorantNom: demande.doctorantNom || 'N/A',
            doctorantPrenom: demande.doctorantPrenom || 'N/A',
            dateCreation: demande.dateCreation,
            priorite: 'normale',
            description: `Inscription ${demande.anneeUniversitaire}`,
            actions: [
              { id: 'view', label: 'Voir', type: 'view', icon: 'visibility' },
              { id: 'approve', label: 'Valider', type: 'approve', icon: 'check_circle' },
              { id: 'reject', label: 'Rejeter', type: 'reject', icon: 'cancel' }
            ]
          })),
          notifications: notifications.slice(0, 5)
        } as DirecteurDashboard;
      }),
      catchError(error => {
        console.error('❌ [DASHBOARD SERVICE] Erreur chargement dashboard directeur:', error);
        throw error;
      })
    );
  }

  /**
   * 🛠️ Récupérer le dashboard complet de l'admin
   */
  getAdminDashboard(): Observable<AdminDashboard> {
    console.log('📊 [DASHBOARD SERVICE] Chargement dashboard admin');

    return forkJoin({
      // Statistiques utilisateurs
      userStats: this.http.get<any>(`${this.API_URL}/admin/statistics/users`).pipe(
        catchError(() => of(null))
      ),
      // Statistiques connexions
      connectionStats: this.http.get<any>(`${this.API_URL}/admin/statistics/connections`).pipe(
        catchError(() => of(null))
      ),
      // Inscriptions en attente
      inscriptionsEnAttente: this.http.get<any[]>(`${this.API_URL}/inscriptions/admin/en-attente`).pipe(
        catchError(() => of([]))
      ),
      // Campagnes
      campagnes: this.http.get<any[]>(`${this.API_URL}/campagnes`).pipe(
        catchError(() => of([]))
      ),
      // Utilisateurs actifs
      users: this.http.get<any[]>(`${this.API_URL}/users`).pipe(
        catchError(() => of([]))
      ),
      // Logs récents
      recentAudits: this.http.get<any[]>(`${this.API_URL}/admin/audit/recent`).pipe(
        catchError(() => of([]))
      ),
      // Alertes système
      systemAlerts: this.http.get<any>(`${this.API_URL}/inscriptions/verifier-alertes`).pipe(
        catchError(() => of({ alertes: [] }))
      )
    }).pipe(
      map(({ userStats, connectionStats, inscriptionsEnAttente, campagnes, users, recentAudits, systemAlerts }) => {
        console.log('✅ [DASHBOARD SERVICE] Données admin chargées');

        const activeCampagnes = campagnes.filter((c: any) => c.statut === 'active');

        return {
          statistics: {
            totalUsers: userStats?.totalUsers || users.length,
            activeUsers: userStats?.activeUsers || users.filter((u: any) => u.enabled).length,
            disabledUsers: userStats?.disabledUsers || users.filter((u: any) => !u.enabled).length,
            totalInscriptions: inscriptionsEnAttente.length,
            inscriptionsEnAttente: inscriptionsEnAttente.length,
            activeCampagnes: activeCampagnes.length,
            pendingValidations: inscriptionsEnAttente.length,
            systemHealth: 'healthy'
          },
          userStatistics: userStats || {
            totalUsers: users.length,
            activeUsers: users.filter((u: any) => u.enabled).length,
            inactiveUsers: 0,
            disabledUsers: users.filter((u: any) => !u.enabled).length,
            usersByRole: [],
            newUsersLastMonth: 0,
            newUsersLastWeek: 0
          },
          connectionStatistics: connectionStats || {
            totalConnections: 0,
            uniqueUsers: 0,
            averageSessionDuration: 0,
            peakHours: [],
            connectionsByDay: [],
            failedLoginAttempts: 0
          },
          campagnes: campagnes.map((c: any) => ({
            id: c.id,
            nom: c.nom,
            anneeUniversitaire: c.anneeUniversitaire,
            dateDebut: c.dateDebut,
            dateFin: c.dateFin,
            statut: c.statut,
            totalInscriptions: c.totalInscriptions || 0,
            inscriptionsValidees: c.inscriptionsValidees || 0,
            inscriptionsEnAttente: c.inscriptionsEnAttente || 0
          })),
          recentAudits: recentAudits.map((audit: any) => ({
            id: audit.id,
            userId: audit.userId,
            userName: audit.userName || 'N/A',
            action: audit.action,
            entityType: audit.entityType,
            entityId: audit.entityId,
            timestamp: audit.timestamp,
            ipAddress: audit.ipAddress || 'N/A',
            details: audit.details
          })),
          systemAlerts: (systemAlerts.alertes || []).map((alert: any, index: number) => ({
            id: index,
            type: alert.type || 'info',
            message: alert.message,
            timestamp: new Date(),
            severity: alert.severity || 'low'
          })),
          activeUsers: users.slice(0, 10).map((u: any) => ({
            id: u.id,
            firstName: u.FirstName || u.firstName,
            lastName: u.LastName || u.lastName,
            email: u.email,
            role: u.roles?.[0] || 'N/A',
            lastConnection: new Date(),
            status: 'offline',
            enabled: u.enabled !== false
          }))
        } as AdminDashboard;
      }),
      catchError(error => {
        console.error('❌ [DASHBOARD SERVICE] Erreur chargement dashboard admin:', error);
        throw error;
      })
    );
  }

  /**
   * 🔄 Rafraîchir les données du dashboard
   */
  refreshDashboard(role: string, userId?: number): Observable<any> {
    console.log('🔄 [DASHBOARD SERVICE] Rafraîchissement dashboard:', role);

    switch (role) {
      case 'ROLE_DOCTORANT':
        return userId ? this.getDoctorantDashboard(userId) : of(null);
      case 'ROLE_DIRECTEUR':
        return userId ? this.getDirecteurDashboard(userId) : of(null);
      case 'ROLE_ADMIN':
        return this.getAdminDashboard();
      default:
        return of(null);
    }
  }

  /**
   * 🎯 Actions rapides pour le doctorant
   */
  private getDoctorantQuickActions() {
    return [
      {
        id: 'nouvelle-inscription',
        label: 'Nouvelle inscription',
        icon: 'add_circle',
        route: '/inscription/new',
        color: 'primary'
      },
      {
        id: 'mes-inscriptions',
        label: 'Mes inscriptions',
        icon: 'list_alt',
        route: '/inscription/list',
        color: 'accent'
      },
      {
        id: 'upload-document',
        label: 'Téléverser un document',
        icon: 'upload_file',
        route: '/inscription/documents',
        color: 'warn'
      },
      {
        id: 'attestations',
        label: 'Mes attestations',
        icon: 'description',
        route: '/inscription/attestations',
        color: 'primary'
      }
    ];
  }
}
