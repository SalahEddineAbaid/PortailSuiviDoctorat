import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { map, tap, catchError, shareReplay } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  InscriptionRequest,
  InscriptionResponse,
  ValidationRequest,
  DashboardResponse,
  StatistiquesDossier,
  AlerteVerificationSummary,
  StatutInscription,
  InscriptionListFilter,
  InscriptionListSort
} from '../models/inscription.model';

@Injectable({
  providedIn: 'root'
})
export class InscriptionService {
  private readonly apiUrl = `${environment.apiUrl}/inscriptions`;
  
  // Cache for inscriptions list
  private inscriptionsCache$ = new BehaviorSubject<InscriptionResponse[]>([]);
  private cacheTimestamp: number = 0;
  private readonly CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

  constructor(private http: HttpClient) {}

  // ============================================
  // CRUD Operations
  // ============================================

  /**
   * Créer une nouvelle inscription
   */
  createInscription(request: InscriptionRequest): Observable<InscriptionResponse> {
    return this.http.post<InscriptionResponse>(this.apiUrl, request).pipe(
      tap(() => this.invalidateCache()),
      catchError(this.handleError)
    );
  }

  /**
   * Récupérer une inscription par ID
   */
  getInscription(id: number): Observable<InscriptionResponse> {
    return this.http.get<InscriptionResponse>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Récupérer les inscriptions d'un doctorant
   */
  getInscriptionsDoctorant(doctorantId: number): Observable<InscriptionResponse[]> {
    return this.http.get<InscriptionResponse[]>(`${this.apiUrl}/doctorant/${doctorantId}`).pipe(
      tap(inscriptions => this.updateCache(inscriptions)),
      catchError(this.handleError)
    );
  }

  /**
   * Récupérer les inscriptions en attente pour un directeur
   */
  getInscriptionsEnAttenteDirecteur(directeurId: number): Observable<InscriptionResponse[]> {
    return this.http.get<InscriptionResponse[]>(`${this.apiUrl}/directeur/${directeurId}/en-attente`).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Récupérer les inscriptions en attente pour l'administration
   */
  getInscriptionsEnAttenteAdmin(): Observable<InscriptionResponse[]> {
    return this.http.get<InscriptionResponse[]>(`${this.apiUrl}/admin/en-attente`).pipe(
      catchError(this.handleError)
    );
  }

  // ============================================
  // Workflow Operations
  // ============================================

  /**
   * Soumettre une inscription pour validation
   */
  soumettre(id: number, doctorantId: number): Observable<InscriptionResponse> {
    const params = new HttpParams().set('doctorantId', doctorantId.toString());
    return this.http.post<InscriptionResponse>(`${this.apiUrl}/${id}/soumettre`, null, { params }).pipe(
      tap(() => this.invalidateCache()),
      catchError(this.handleError)
    );
  }

  /**
   * Valider une inscription par le directeur
   */
  validerParDirecteur(id: number, request: ValidationRequest, directeurId: number): Observable<InscriptionResponse> {
    const params = new HttpParams().set('directeurId', directeurId.toString());
    return this.http.post<InscriptionResponse>(`${this.apiUrl}/${id}/valider-directeur`, request, { params }).pipe(
      tap(() => this.invalidateCache()),
      catchError(this.handleError)
    );
  }

  /**
   * Valider une inscription par l'administration
   */
  validerParAdmin(id: number, request: ValidationRequest): Observable<InscriptionResponse> {
    return this.http.post<InscriptionResponse>(`${this.apiUrl}/${id}/valider-admin`, request).pipe(
      tap(() => this.invalidateCache()),
      catchError(this.handleError)
    );
  }

  // ============================================
  // Dashboard & Statistics
  // ============================================

  /**
   * Récupérer le dashboard d'un doctorant
   */
  getDashboardDoctorant(doctorantId: number, userId: number, role: string): Observable<DashboardResponse> {
    const params = new HttpParams()
      .set('userId', userId.toString())
      .set('role', role);
    
    return this.http.get<DashboardResponse>(`${this.apiUrl}/doctorant/${doctorantId}/dashboard`, { params }).pipe(
      shareReplay(1),
      catchError(this.handleError)
    );
  }

  /**
   * Récupérer les statistiques des dossiers
   */
  getStatistiquesDossier(): Observable<StatistiquesDossier> {
    return this.http.get<StatistiquesDossier>(`${this.apiUrl}/statistiques`).pipe(
      catchError(this.handleError)
    );
  }

  // ============================================
  // Documents
  // ============================================

  /**
   * Télécharger l'attestation d'inscription
   */
  downloadAttestation(id: number, userId: number, role: string): Observable<Blob> {
    const params = new HttpParams()
      .set('userId', userId.toString())
      .set('role', role);
    
    return this.http.get(`${this.apiUrl}/${id}/attestation`, {
      params,
      responseType: 'blob'
    }).pipe(
      catchError(this.handleError)
    );
  }

  // ============================================
  // Alerts
  // ============================================

  /**
   * Vérifier les alertes de durée (Admin only)
   */
  verifierAlertes(): Observable<AlerteVerificationSummary> {
    return this.http.get<AlerteVerificationSummary>(`${this.apiUrl}/verifier-alertes`).pipe(
      catchError(this.handleError)
    );
  }

  // ============================================
  // Filtering & Sorting
  // ============================================

  /**
   * Filtrer les inscriptions
   */
  filterInscriptions(
    inscriptions: InscriptionResponse[],
    filter: InscriptionListFilter
  ): InscriptionResponse[] {
    let filtered = [...inscriptions];

    if (filter.statut) {
      filtered = filtered.filter(i => i.statut === filter.statut);
    }

    if (filter.annee) {
      filtered = filtered.filter(i => i.anneeInscription === filter.annee);
    }

    if (filter.type) {
      filtered = filtered.filter(i => i.type === filter.type);
    }

    if (filter.searchTerm) {
      const term = filter.searchTerm.toLowerCase();
      filtered = filtered.filter(i =>
        i.sujetThese.toLowerCase().includes(term) ||
        i.infosDoctorant.cin.toLowerCase().includes(term) ||
        i.infosThese.titreThese.toLowerCase().includes(term)
      );
    }

    return filtered;
  }

  /**
   * Trier les inscriptions
   */
  sortInscriptions(
    inscriptions: InscriptionResponse[],
    sort: InscriptionListSort
  ): InscriptionResponse[] {
    const sorted = [...inscriptions];

    sorted.sort((a, b) => {
      let comparison = 0;

      switch (sort.field) {
        case 'dateCreation':
          comparison = new Date(a.dateCreation).getTime() - new Date(b.dateCreation).getTime();
          break;
        case 'anneeInscription':
          comparison = a.anneeInscription - b.anneeInscription;
          break;
        case 'statut':
          comparison = a.statut.localeCompare(b.statut);
          break;
        default:
          comparison = 0;
      }

      return sort.direction === 'asc' ? comparison : -comparison;
    });

    return sorted;
  }

  // ============================================
  // Cache Management
  // ============================================

  /**
   * Obtenir les inscriptions depuis le cache
   */
  getCachedInscriptions(): Observable<InscriptionResponse[]> {
    return this.inscriptionsCache$.asObservable();
  }

  /**
   * Mettre à jour le cache
   */
  private updateCache(inscriptions: InscriptionResponse[]): void {
    this.inscriptionsCache$.next(inscriptions);
    this.cacheTimestamp = Date.now();
  }

  /**
   * Invalider le cache
   */
  private invalidateCache(): void {
    this.cacheTimestamp = 0;
  }

  /**
   * Vérifier si le cache est valide
   */
  private isCacheValid(): boolean {
    return Date.now() - this.cacheTimestamp < this.CACHE_DURATION;
  }

  // ============================================
  // Helper Methods
  // ============================================

  /**
   * Vérifier si un doctorant peut créer une nouvelle inscription
   */
  canCreateInscription(inscriptions: InscriptionResponse[]): boolean {
    // Vérifier s'il n'y a pas d'inscription en cours (brouillon ou en attente)
    const activeStatuts = [
      StatutInscription.BROUILLON,
      StatutInscription.SOUMIS,
      StatutInscription.EN_ATTENTE_DIRECTEUR,
      StatutInscription.APPROUVE_DIRECTEUR,
      StatutInscription.EN_ATTENTE_ADMIN
    ];

    return !inscriptions.some(i => activeStatuts.includes(i.statut));
  }

  /**
   * Obtenir l'inscription courante (la plus récente non rejetée)
   */
  getCurrentInscription(inscriptions: InscriptionResponse[]): InscriptionResponse | null {
    const nonRejected = inscriptions.filter(i => i.statut !== StatutInscription.REJETE);
    
    if (nonRejected.length === 0) {
      return null;
    }

    return nonRejected.reduce((latest, current) => {
      const latestDate = new Date(latest.dateCreation);
      const currentDate = new Date(current.dateCreation);
      return currentDate > latestDate ? current : latest;
    });
  }

  /**
   * Obtenir les années d'inscription disponibles
   */
  getAvailableYears(inscriptions: InscriptionResponse[]): number[] {
    const years = inscriptions.map(i => i.anneeInscription);
    return Array.from(new Set(years)).sort((a, b) => b - a);
  }

  /**
   * Compter les inscriptions par statut
   */
  countByStatut(inscriptions: InscriptionResponse[]): Map<StatutInscription, number> {
    const counts = new Map<StatutInscription, number>();
    
    Object.values(StatutInscription).forEach(statut => {
      counts.set(statut, 0);
    });

    inscriptions.forEach(inscription => {
      const current = counts.get(inscription.statut) || 0;
      counts.set(inscription.statut, current + 1);
    });

    return counts;
  }

  // ============================================
  // Error Handling
  // ============================================

  private handleError(error: any): Observable<never> {
    console.error('InscriptionService Error:', error);
    
    let errorMessage = 'Une erreur est survenue';
    
    if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.status === 0) {
      errorMessage = 'Impossible de contacter le serveur';
    } else if (error.status === 401) {
      errorMessage = 'Non autorisé';
    } else if (error.status === 403) {
      errorMessage = 'Accès refusé';
    } else if (error.status === 404) {
      errorMessage = 'Ressource non trouvée';
    } else if (error.status === 409) {
      errorMessage = 'Conflit - Une inscription existe déjà';
    } else if (error.status >= 500) {
      errorMessage = 'Erreur serveur';
    }

    return throwError(() => ({ message: errorMessage, originalError: error }));
  }
}
