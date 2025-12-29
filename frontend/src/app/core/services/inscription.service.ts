import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Inscription,
  InscriptionRequest,
  InscriptionResponse,
  Campagne,
  CampagneRequest,
  CampagneResponse,
  ValidationRequest,
  InscriptionStatus
} from '../models/inscription.model';

@Injectable({
  providedIn: 'root'
})
export class InscriptionService {
  private readonly API_URL = `${environment.apiUrl}/inscriptions`;
  private readonly CAMPAGNE_API_URL = `${environment.apiUrl}/campagnes`;

  constructor(private http: HttpClient) {}

  // ===== INSCRIPTION ENDPOINTS =====

  /**
   * 🔹 Créer une nouvelle demande d'inscription (DOCTORANT)
   */
  createInscription(data: InscriptionRequest): Observable<InscriptionResponse> {
    console.log('📤 [INSCRIPTION SERVICE] Création inscription:', data);
    return this.http.post<InscriptionResponse>(this.API_URL, data);
  }

  /**
   * 🔹 Soumettre l'inscription pour validation (DOCTORANT)
   */
  submitInscription(id: number): Observable<InscriptionResponse> {
    console.log('📤 [INSCRIPTION SERVICE] Soumission inscription:', id);
    return this.http.post<InscriptionResponse>(`${this.API_URL}/${id}/soumettre`, {});
  }

  /**
   * 🔹 Récupérer une inscription par ID
   */
  getInscription(id: number): Observable<InscriptionResponse> {
    console.log('📤 [INSCRIPTION SERVICE] Récupération inscription:', id);
    return this.http.get<InscriptionResponse>(`${this.API_URL}/${id}`);
  }

  /**
   * 🔹 Récupérer les inscriptions d'un doctorant
   */
  getInscriptionsDoctorant(doctorantId: number): Observable<InscriptionResponse[]> {
    console.log('📤 [INSCRIPTION SERVICE] Inscriptions doctorant:', doctorantId);
    return this.http.get<InscriptionResponse[]>(`${this.API_URL}/doctorant/${doctorantId}`);
  }

  /**
   * 🔹 Récupérer mes inscriptions (utilisateur connecté)
   */
  getMyInscriptions(): Observable<InscriptionResponse[]> {
    console.log('📤 [INSCRIPTION SERVICE] Mes inscriptions');
    return this.http.get<InscriptionResponse[]>(`${this.API_URL}/me`);
  }

  /**
   * 🔹 Récupérer les inscriptions en attente pour un directeur (DIRECTEUR)
   */
  getInscriptionsEnAttenteDirecteur(directeurId: number): Observable<InscriptionResponse[]> {
    console.log('📤 [INSCRIPTION SERVICE] Inscriptions en attente directeur:', directeurId);
    return this.http.get<InscriptionResponse[]>(`${this.API_URL}/directeur/${directeurId}/en-attente`);
  }

  /**
   * 🔹 Récupérer tous les doctorants encadrés par un directeur (DIRECTEUR)
   */
  getDoctorantsByDirecteur(): Observable<any[]> {
    console.log('📤 [INSCRIPTION SERVICE] Doctorants par directeur');
    return this.http.get<any[]>(`${this.API_URL}/directeur/doctorants`);
  }

  /**
   * 🔹 Valider l'inscription par le directeur (DIRECTEUR)
   */
  validerParDirecteur(id: number, validation: ValidationRequest): Observable<InscriptionResponse> {
    console.log('📤 [INSCRIPTION SERVICE] Validation directeur:', id, validation);
    return this.http.post<InscriptionResponse>(`${this.API_URL}/${id}/valider-directeur`, validation);
  }

  /**
   * 🔹 Récupérer les inscriptions en attente admin (ADMIN)
   */
  getInscriptionsEnAttenteAdmin(): Observable<InscriptionResponse[]> {
    console.log('📤 [INSCRIPTION SERVICE] Inscriptions en attente admin');
    return this.http.get<InscriptionResponse[]>(`${this.API_URL}/admin/en-attente`);
  }

  /**
   * 🔹 Valider l'inscription par l'administration (ADMIN)
   */
  validerParAdmin(id: number, validation: ValidationRequest): Observable<InscriptionResponse> {
    console.log('📤 [INSCRIPTION SERVICE] Validation admin:', id, validation);
    return this.http.post<InscriptionResponse>(`${this.API_URL}/${id}/valider-admin`, validation);
  }

  // ===== CAMPAGNE ENDPOINTS =====

  /**
   * 🔹 Créer une nouvelle campagne (ADMIN)
   */
  createCampagne(data: CampagneRequest): Observable<CampagneResponse> {
    console.log('📤 [INSCRIPTION SERVICE] Création campagne:', data);
    return this.http.post<CampagneResponse>(this.CAMPAGNE_API_URL, data);
  }

  /**
   * 🔹 Récupérer toutes les campagnes
   */
  getAllCampagnes(): Observable<CampagneResponse[]> {
    console.log('📤 [INSCRIPTION SERVICE] Toutes les campagnes');
    return this.http.get<CampagneResponse[]>(this.CAMPAGNE_API_URL);
  }

  /**
   * 🔹 Récupérer toutes les campagnes (alias pour compatibilité)
   */
  getCampagnes(): Observable<CampagneResponse[]> {
    return this.getAllCampagnes();
  }

  /**
   * 🔹 Récupérer les campagnes actives
   */
  getCampagnesActives(): Observable<CampagneResponse[]> {
    console.log('📤 [INSCRIPTION SERVICE] Campagnes actives');
    return this.http.get<CampagneResponse[]>(`${this.CAMPAGNE_API_URL}/actives`);
  }

  /**
   * 🔹 Récupérer la campagne active pour inscription
   */
  getCampagneActive(): Observable<CampagneResponse | null> {
    console.log('📤 [INSCRIPTION SERVICE] Campagne active');
    return this.http.get<CampagneResponse>(`${this.CAMPAGNE_API_URL}/active`);
  }

  /**
   * 🔹 Récupérer une campagne par ID
   */
  getCampagne(id: number): Observable<CampagneResponse> {
    console.log('📤 [INSCRIPTION SERVICE] Campagne:', id);
    return this.http.get<CampagneResponse>(`${this.CAMPAGNE_API_URL}/${id}`);
  }

  /**
   * 🔹 Fermer une campagne (ADMIN)
   */
  fermerCampagne(id: number): Observable<CampagneResponse> {
    console.log('📤 [INSCRIPTION SERVICE] Fermeture campagne:', id);
    return this.http.put<CampagneResponse>(`${this.CAMPAGNE_API_URL}/${id}/fermer`, {});
  }

  /**
   * 🔹 Modifier une campagne (ADMIN)
   */
  modifierCampagne(id: number, data: CampagneRequest): Observable<CampagneResponse> {
    console.log('📤 [INSCRIPTION SERVICE] Modification campagne:', id, data);
    return this.http.put<CampagneResponse>(`${this.CAMPAGNE_API_URL}/${id}`, data);
  }

  /**
   * 🔹 Modifier une campagne (alias pour compatibilité)
   */
  updateCampagne(id: number, data: Partial<CampagneRequest>): Observable<CampagneResponse> {
    return this.modifierCampagne(id, data as CampagneRequest);
  }

  /**
   * 🔹 Supprimer une campagne (ADMIN)
   */
  deleteCampagne(id: number): Observable<void> {
    console.log('📤 [INSCRIPTION SERVICE] Suppression campagne:', id);
    return this.http.delete<void>(`${this.CAMPAGNE_API_URL}/${id}`);
  }

  // ===== UTILITY METHODS =====

  /**
   * 🔹 Vérifier si une campagne est ouverte
   */
  isCampagneOuverte(campagne: CampagneResponse): boolean {
    const now = new Date();
    const ouverture = new Date(campagne.dateOuverture);
    const fermeture = new Date(campagne.dateFermeture);
    
    return campagne.active && now >= ouverture && now <= fermeture;
  }

  /**
   * 🔹 Obtenir le statut d'une inscription avec libellé
   */
  getStatusLabel(status: InscriptionStatus): string {
    const labels = {
      [InscriptionStatus.BROUILLON]: 'Brouillon',
      [InscriptionStatus.SOUMISE]: 'Soumise',
      [InscriptionStatus.EN_COURS_VALIDATION]: 'En cours de validation',
      [InscriptionStatus.VALIDEE]: 'Validée',
      [InscriptionStatus.REJETEE]: 'Rejetée'
    };
    return labels[status] || status;
  }

  /**
   * 🔹 Obtenir la couleur du statut pour l'affichage
   */
  getStatusColor(status: InscriptionStatus): string {
    const colors = {
      [InscriptionStatus.BROUILLON]: 'gray',
      [InscriptionStatus.SOUMISE]: 'blue',
      [InscriptionStatus.EN_COURS_VALIDATION]: 'orange',
      [InscriptionStatus.VALIDEE]: 'green',
      [InscriptionStatus.REJETEE]: 'red'
    };
    return colors[status] || 'gray';
  }
}