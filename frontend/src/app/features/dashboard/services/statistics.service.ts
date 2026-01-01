import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserStatistics, ConnectionStatistics } from '../models/dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class StatisticsService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /**
   * 📊 Récupérer les statistiques utilisateurs (Admin)
   */
  getUserStatistics(): Observable<UserStatistics> {
    console.log('📊 [STATISTICS SERVICE] Récupération statistiques utilisateurs');
    
    return this.http.get<UserStatistics>(`${this.API_URL}/admin/statistics/users`).pipe(
      catchError(error => {
        console.error('❌ [STATISTICS SERVICE] Erreur statistiques utilisateurs:', error);
        return of({
          totalUsers: 0,
          activeUsers: 0,
          inactiveUsers: 0,
          disabledUsers: 0,
          usersByRole: [],
          newUsersLastMonth: 0,
          newUsersLastWeek: 0
        });
      })
    );
  }

  /**
   * 🔌 Récupérer les statistiques de connexion (Admin)
   */
  getConnectionStatistics(): Observable<ConnectionStatistics> {
    console.log('📊 [STATISTICS SERVICE] Récupération statistiques connexions');
    
    return this.http.get<ConnectionStatistics>(`${this.API_URL}/admin/statistics/connections`).pipe(
      catchError(error => {
        console.error('❌ [STATISTICS SERVICE] Erreur statistiques connexions:', error);
        return of({
          totalConnections: 0,
          uniqueUsers: 0,
          averageSessionDuration: 0,
          peakHours: [],
          connectionsByDay: [],
          failedLoginAttempts: 0
        });
      })
    );
  }

  /**
   * 📈 Récupérer les statistiques d'inscriptions
   */
  getInscriptionStatistics(): Observable<any> {
    console.log('📊 [STATISTICS SERVICE] Récupération statistiques inscriptions');
    
    return this.http.get<any>(`${this.API_URL}/inscriptions/statistics`).pipe(
      catchError(error => {
        console.error('❌ [STATISTICS SERVICE] Erreur statistiques inscriptions:', error);
        return of({
          total: 0,
          enCours: 0,
          validees: 0,
          rejetees: 0
        });
      })
    );
  }

  /**
   * 🎓 Récupérer les statistiques d'un doctorant
   */
  getDoctorantStatistics(doctorantId: number): Observable<any> {
    console.log('📊 [STATISTICS SERVICE] Récupération statistiques doctorant:', doctorantId);
    
    return this.http.get<any>(`${this.API_URL}/inscriptions/doctorant/${doctorantId}/statistics`).pipe(
      catchError(error => {
        console.error('❌ [STATISTICS SERVICE] Erreur statistiques doctorant:', error);
        return of({
          totalInscriptions: 0,
          inscriptionsValidees: 0,
          documentsManquants: 0,
          progressionThese: 0
        });
      })
    );
  }

  /**
   * 👨‍🏫 Récupérer les statistiques d'un directeur
   */
  getDirecteurStatistics(directeurId: number): Observable<any> {
    console.log('📊 [STATISTICS SERVICE] Récupération statistiques directeur:', directeurId);
    
    return this.http.get<any>(`${this.API_URL}/inscriptions/directeur/${directeurId}/statistics`).pipe(
      catchError(error => {
        console.error('❌ [STATISTICS SERVICE] Erreur statistiques directeur:', error);
        return of({
          totalDoctorants: 0,
          demandesEnAttente: 0,
          tauxValidation: 0
        });
      })
    );
  }
}
