import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { map, catchError, shareReplay, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  CampagneRequest,
  CampagneResponse,
  CloneCampagneRequest,
  StatistiquesCampagne,
  TypeCampagne,
  CampagneListFilter,
  isCampagneActive
} from '../models/campagne.model';

@Injectable({
  providedIn: 'root'
})
export class CampagneService {
  private readonly apiUrl = `${environment.apiUrl}/campagnes`;
  
  // Cache for active campaign
  private activeCampagneCache$?: Observable<CampagneResponse | null>;
  private cacheTimestamp: number = 0;
  private readonly CACHE_DURATION = 10 * 60 * 1000; // 10 minutes

  constructor(private http: HttpClient) {}

  // ============================================
  // CRUD Operations
  // ============================================

  /**
   * Créer une nouvelle campagne (Admin only)
   */
  createCampagne(request: CampagneRequest): Observable<CampagneResponse> {
    return this.http.post<CampagneResponse>(this.apiUrl, request).pipe(
      tap(() => this.invalidateCache()),
      catchError(this.handleError)
    );
  }

  /**
   * Récupérer toutes les campagnes
   */
  getAllCampagnes(): Observable<CampagneResponse[]> {
    return this.http.get<CampagneResponse[]>(this.apiUrl).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Récupérer les campagnes actives
   */
  getCampagnesActives(): Observable<CampagneResponse[]> {
    return this.http.get<CampagneResponse[]>(`${this.apiUrl}/actives`).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Récupérer une campagne par ID
   */
  getCampagne(id: number): Observable<CampagneResponse> {
    return this.http.get<CampagneResponse>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Modifier une campagne (Admin only)
   */
  updateCampagne(id: number, request: CampagneRequest): Observable<CampagneResponse> {
    return this.http.put<CampagneResponse>(`${this.apiUrl}/${id}`, request).pipe(
      tap(() => this.invalidateCache()),
      catchError(this.handleError)
    );
  }

  /**
   * Fermer une campagne (Admin only)
   */
  fermerCampagne(id: number): Observable<CampagneResponse> {
    return this.http.put<CampagneResponse>(`${this.apiUrl}/${id}/fermer`, null).pipe(
      tap(() => this.invalidateCache()),
      catchError(this.handleError)
    );
  }

  /**
   * Cloner une campagne (Admin only)
   */
  clonerCampagne(id: number, request: CloneCampagneRequest): Observable<CampagneResponse> {
    return this.http.post<CampagneResponse>(`${this.apiUrl}/${id}/cloner`, request).pipe(
      tap(() => this.invalidateCache()),
      catchError(this.handleError)
    );
  }

  // ============================================
  // Statistics
  // ============================================

  /**
   * Récupérer les statistiques d'une campagne (Admin only)
   */
  getStatistiques(id: number): Observable<StatistiquesCampagne> {
    return this.http.get<StatistiquesCampagne>(`${this.apiUrl}/${id}/statistiques`).pipe(
      catchError(this.handleError)
    );
  }

  // ============================================
  // Helper Methods
  // ============================================

  /**
   * Récupérer la campagne active pour les inscriptions
   * Utilise le cache pour éviter les appels répétés
   */
  getCampagneActive(): Observable<CampagneResponse | null> {
    if (this.activeCampagneCache$ && this.isCacheValid()) {
      return this.activeCampagneCache$;
    }

    this.activeCampagneCache$ = this.getCampagnesActives().pipe(
      map(campagnes => {
        // Trouver la première campagne active et ouverte pour les inscriptions
        const activeCampagne = campagnes.find(c => 
          isCampagneActive(c) && 
          (c.type === TypeCampagne.INSCRIPTION || c.type === TypeCampagne.MIXTE)
        );
        return activeCampagne || null;
      }),
      tap(() => this.cacheTimestamp = Date.now()),
      shareReplay(1),
      catchError(error => {
        console.error('Error fetching active campaign:', error);
        return of(null);
      })
    );

    return this.activeCampagneCache$;
  }

  /**
   * Récupérer la campagne active pour les réinscriptions
   */
  getCampagneActiveReinscription(): Observable<CampagneResponse | null> {
    return this.getCampagnesActives().pipe(
      map(campagnes => {
        const activeCampagne = campagnes.find(c => 
          isCampagneActive(c) && 
          (c.type === TypeCampagne.REINSCRIPTION || c.type === TypeCampagne.MIXTE)
        );
        return activeCampagne || null;
      }),
      catchError(error => {
        console.error('Error fetching active reinscription campaign:', error);
        return of(null);
      })
    );
  }

  /**
   * Vérifier si une campagne d'inscription est active
   */
  hasActiveCampagne(): Observable<boolean> {
    return this.getCampagneActive().pipe(
      map(campagne => campagne !== null)
    );
  }

  /**
   * Vérifier si une campagne de réinscription est active
   */
  hasActiveCampagneReinscription(): Observable<boolean> {
    return this.getCampagneActiveReinscription().pipe(
      map(campagne => campagne !== null)
    );
  }

  // ============================================
  // Filtering
  // ============================================

  /**
   * Filtrer les campagnes
   */
  filterCampagnes(
    campagnes: CampagneResponse[],
    filter: CampagneListFilter
  ): CampagneResponse[] {
    let filtered = [...campagnes];

    if (filter.type) {
      filtered = filtered.filter(c => c.type === filter.type);
    }

    if (filter.active !== undefined) {
      filtered = filtered.filter(c => c.active === filter.active);
    }

    if (filter.anneeUniversitaire) {
      filtered = filtered.filter(c => c.anneeUniversitaire === filter.anneeUniversitaire);
    }

    if (filter.searchTerm) {
      const term = filter.searchTerm.toLowerCase();
      filtered = filtered.filter(c =>
        c.libelle.toLowerCase().includes(term)
      );
    }

    return filtered;
  }

  /**
   * Obtenir les années universitaires disponibles
   */
  getAvailableYears(campagnes: CampagneResponse[]): number[] {
    const years = campagnes.map(c => c.anneeUniversitaire);
    return Array.from(new Set(years)).sort((a, b) => b - a);
  }

  /**
   * Obtenir les campagnes par année universitaire
   */
  getCampagnesByYear(campagnes: CampagneResponse[], year: number): CampagneResponse[] {
    return campagnes.filter(c => c.anneeUniversitaire === year);
  }

  /**
   * Obtenir les campagnes par type
   */
  getCampagnesByType(campagnes: CampagneResponse[], type: TypeCampagne): CampagneResponse[] {
    return campagnes.filter(c => c.type === type);
  }

  /**
   * Obtenir la campagne la plus récente
   */
  getLatestCampagne(campagnes: CampagneResponse[]): CampagneResponse | null {
    if (campagnes.length === 0) {
      return null;
    }

    return campagnes.reduce((latest, current) => {
      const latestDate = new Date(latest.dateDebut);
      const currentDate = new Date(current.dateDebut);
      return currentDate > latestDate ? current : latest;
    });
  }

  /**
   * Vérifier si une campagne peut être modifiée
   */
  canModifyCampagne(campagne: CampagneResponse): boolean {
    const now = new Date();
    const dateDebut = new Date(campagne.dateDebut);
    
    // Peut être modifiée si elle n'a pas encore commencé ou si elle est inactive
    return !campagne.active || now < dateDebut;
  }

  /**
   * Vérifier si une campagne peut être fermée
   */
  canCloseCampagne(campagne: CampagneResponse): boolean {
    return campagne.active && campagne.ouverte;
  }

  /**
   * Calculer le nombre de jours restants
   */
  getDaysRemaining(campagne: CampagneResponse): number {
    const now = new Date();
    const dateFin = new Date(campagne.dateFin);
    const diff = dateFin.getTime() - now.getTime();
    return Math.ceil(diff / (1000 * 60 * 60 * 24));
  }

  /**
   * Calculer le pourcentage de progression
   */
  getProgress(campagne: CampagneResponse): number {
    const now = new Date();
    const dateDebut = new Date(campagne.dateDebut);
    const dateFin = new Date(campagne.dateFin);
    
    if (now < dateDebut) {
      return 0;
    }
    
    if (now > dateFin) {
      return 100;
    }
    
    const total = dateFin.getTime() - dateDebut.getTime();
    const elapsed = now.getTime() - dateDebut.getTime();
    
    return Math.round((elapsed / total) * 100);
  }

  // ============================================
  // Cache Management
  // ============================================

  /**
   * Invalider le cache
   */
  private invalidateCache(): void {
    this.activeCampagneCache$ = undefined;
    this.cacheTimestamp = 0;
  }

  /**
   * Vérifier si le cache est valide
   */
  private isCacheValid(): boolean {
    return Date.now() - this.cacheTimestamp < this.CACHE_DURATION;
  }

  /**
   * Forcer le rechargement du cache
   */
  refreshCache(): void {
    this.invalidateCache();
  }

  // ============================================
  // Error Handling
  // ============================================

  private handleError(error: any): Observable<never> {
    console.error('CampagneService Error:', error);
    
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
      errorMessage = 'Campagne non trouvée';
    } else if (error.status === 409) {
      errorMessage = 'Conflit - Une campagne existe déjà pour cette période';
    } else if (error.status >= 500) {
      errorMessage = 'Erreur serveur';
    }

    return throwError(() => ({ message: errorMessage, originalError: error }));
  }
}
