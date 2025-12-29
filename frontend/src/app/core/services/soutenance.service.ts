import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Soutenance,
  SoutenanceRequest,
  SoutenanceResponse,
  DefenseScheduleDTO,
  DefenseResponseDTO,
  PrerequisStatus,
  JuryMember,
  JuryMemberRequest,
  SoutenanceStatus
} from '../models/soutenance.model';

@Injectable({
  providedIn: 'root'
})
export class SoutenanceService {
  private readonly API_URL = `${environment.apiUrl}/defense-service/defenses`;
  private readonly SOUTENANCE_API_URL = `${environment.apiUrl}/soutenances`;

  constructor(private http: HttpClient) {}

  // ===== SOUTENANCE ENDPOINTS =====

  /**
   * 🔹 Créer une demande de soutenance (DOCTORANT)
   */
  createDemandeSoutenance(data: SoutenanceRequest): Observable<SoutenanceResponse> {
    console.log('📤 [SOUTENANCE SERVICE] Création demande soutenance:', data);
    return this.http.post<SoutenanceResponse>(this.SOUTENANCE_API_URL, data);
  }

  /**
   * 🔹 Planifier une soutenance avec validation (via defense-service)
   */
  scheduleDefense(data: DefenseScheduleDTO): Observable<DefenseResponseDTO> {
    console.log('📤 [SOUTENANCE SERVICE] Planification soutenance:', data);
    return this.http.post<DefenseResponseDTO>(this.API_URL, data);
  }

  /**
   * 🔹 Récupérer la soutenance d'une demande
   */
  getDefenseByRequestId(requestId: number): Observable<DefenseResponseDTO> {
    console.log('📤 [SOUTENANCE SERVICE] Soutenance par demande:', requestId);
    return this.http.get<DefenseResponseDTO>(`${this.API_URL}/defense-request/${requestId}`);
  }

  /**
   * 🔹 Mettre à jour une demande de soutenance (DOCTORANT)
   */
  updateDemandeSoutenance(id: number, data: SoutenanceRequest): Observable<SoutenanceResponse> {
    console.log('📤 [SOUTENANCE SERVICE] Mise à jour soutenance:', id, data);
    return this.http.put<SoutenanceResponse>(`${this.SOUTENANCE_API_URL}/${id}`, data);
  }

  /**
   * 🔹 Récupérer mes soutenances (utilisateur connecté)
   */
  getMySoutenances(): Observable<SoutenanceResponse[]> {
    console.log('📤 [SOUTENANCE SERVICE] Mes soutenances');
    return this.http.get<SoutenanceResponse[]>(`${this.SOUTENANCE_API_URL}/me`);
  }

  /**
   * 🔹 Récupérer les soutenances d'un doctorant
   */
  getSoutenancesDoctorant(doctorantId: number): Observable<SoutenanceResponse[]> {
    console.log('📤 [SOUTENANCE SERVICE] Soutenances doctorant:', doctorantId);
    return this.http.get<SoutenanceResponse[]>(`${this.SOUTENANCE_API_URL}/doctorant/${doctorantId}`);
  }

  /**
   * 🔹 Récupérer les soutenances à valider pour un directeur
   */
  getSoutenancesEnAttenteDirecteur(directeurId: number): Observable<SoutenanceResponse[]> {
    console.log('📤 [SOUTENANCE SERVICE] Soutenances en attente directeur:', directeurId);
    return this.http.get<SoutenanceResponse[]>(`${this.SOUTENANCE_API_URL}/directeur/${directeurId}/en-attente`);
  }

  /**
   * 🔹 Récupérer toutes les soutenances d'un directeur
   */
  getSoutenancesByDirecteur(): Observable<SoutenanceResponse[]> {
    console.log('📤 [SOUTENANCE SERVICE] Soutenances par directeur');
    return this.http.get<SoutenanceResponse[]>(`${this.SOUTENANCE_API_URL}/directeur/soutenances`);
  }

  /**
   * 🔹 Récupérer les soutenances en attente admin
   */
  getSoutenancesEnAttenteAdmin(): Observable<SoutenanceResponse[]> {
    console.log('📤 [SOUTENANCE SERVICE] Soutenances en attente admin');
    return this.http.get<SoutenanceResponse[]>(`${this.SOUTENANCE_API_URL}/admin/en-attente`);
  }

  /**
   * 🔹 Récupérer une soutenance par ID
   */
  getSoutenance(id: number): Observable<SoutenanceResponse> {
    console.log('📤 [SOUTENANCE SERVICE] Soutenance:', id);
    return this.http.get<SoutenanceResponse>(`${this.SOUTENANCE_API_URL}/${id}`);
  }

  // ===== PREREQUIS ENDPOINTS =====

  /**
   * 🔹 Vérifier les prérequis pour une soutenance
   */
  checkPrerequis(doctorantId: number): Observable<PrerequisStatus> {
    console.log('📤 [SOUTENANCE SERVICE] Vérification prérequis:', doctorantId);
    return this.http.get<PrerequisStatus>(`${this.SOUTENANCE_API_URL}/prerequis/${doctorantId}`);
  }

  /**
   * 🔹 Valider les prérequis d'une soutenance
   */
  validatePrerequis(soutenanceId: number): Observable<any> {
    console.log('📤 [SOUTENANCE SERVICE] Validation prérequis:', soutenanceId);
    return this.http.post(`${this.SOUTENANCE_API_URL}/${soutenanceId}/valider-prerequis`, {});
  }

  // ===== JURY ENDPOINTS =====

  /**
   * 🔹 Proposer un jury pour une soutenance
   */
  proposeJury(soutenanceId: number, jury: JuryMemberRequest[]): Observable<any> {
    console.log('📤 [SOUTENANCE SERVICE] Proposition jury:', soutenanceId, jury);
    return this.http.post(`${this.SOUTENANCE_API_URL}/${soutenanceId}/jury`, { jury });
  }

  /**
   * 🔹 Valider un jury (DIRECTEUR/ADMIN)
   */
  validerJury(soutenanceId: number, validation: { valide: boolean; commentaire?: string }): Observable<any> {
    console.log('📤 [SOUTENANCE SERVICE] Validation jury:', soutenanceId, validation);
    return this.http.post(`${this.SOUTENANCE_API_URL}/${soutenanceId}/valider-jury`, validation);
  }

  // ===== PLANNING ENDPOINTS =====

  /**
   * 🔹 Planifier une soutenance (date, lieu)
   */
  planifierSoutenance(soutenanceId: number, planning: { dateSoutenance: Date; lieuSoutenance: string }): Observable<any> {
    console.log('📤 [SOUTENANCE SERVICE] Planification:', soutenanceId, planning);
    return this.http.post(`${this.SOUTENANCE_API_URL}/${soutenanceId}/planifier`, planning);
  }

  /**
   * 🔹 Autoriser une soutenance (ADMIN)
   */
  autoriserSoutenance(soutenanceId: number): Observable<SoutenanceResponse> {
    console.log('📤 [SOUTENANCE SERVICE] Autorisation soutenance:', soutenanceId);
    return this.http.post<SoutenanceResponse>(`${this.SOUTENANCE_API_URL}/${soutenanceId}/autoriser`, {});
  }

  /**
   * 🔹 Rejeter une soutenance (ADMIN)
   */
  rejeterSoutenance(soutenanceId: number, motif: string): Observable<SoutenanceResponse> {
    console.log('📤 [SOUTENANCE SERVICE] Rejet soutenance:', soutenanceId, motif);
    return this.http.post<SoutenanceResponse>(`${this.SOUTENANCE_API_URL}/${soutenanceId}/rejeter`, { motif });
  }

  /**
   * 🔹 Valider une soutenance par l'administration (ADMIN)
   */
  validerParAdmin(id: number, validation: { valide: boolean; commentaire: string }): Observable<SoutenanceResponse> {
    console.log('📤 [SOUTENANCE SERVICE] Validation admin:', id, validation);
    return this.http.post<SoutenanceResponse>(`${this.SOUTENANCE_API_URL}/${id}/valider-admin`, validation);
  }

  // ===== UTILITY METHODS =====

  /**
   * 🔹 Obtenir le libellé du statut
   */
  getStatusLabel(status: SoutenanceStatus): string {
    const labels = {
      [SoutenanceStatus.BROUILLON]: 'Brouillon',
      [SoutenanceStatus.SOUMISE]: 'Soumise',
      [SoutenanceStatus.EN_COURS_VALIDATION]: 'En cours de validation',
      [SoutenanceStatus.AUTORISEE]: 'Autorisée',
      [SoutenanceStatus.REJETEE]: 'Rejetée',
      [SoutenanceStatus.SOUTENUE]: 'Soutenue'
    };
    return labels[status] || status;
  }

  /**
   * 🔹 Obtenir la couleur du statut
   */
  getStatusColor(status: SoutenanceStatus): string {
    const colors = {
      [SoutenanceStatus.BROUILLON]: 'gray',
      [SoutenanceStatus.SOUMISE]: 'blue',
      [SoutenanceStatus.EN_COURS_VALIDATION]: 'orange',
      [SoutenanceStatus.AUTORISEE]: 'green',
      [SoutenanceStatus.REJETEE]: 'red',
      [SoutenanceStatus.SOUTENUE]: 'purple'
    };
    return colors[status] || 'gray';
  }

  /**
   * 🔹 Vérifier si tous les prérequis sont remplis
   */
  arePrerequisMet(prerequis: PrerequisStatus): boolean {
    return prerequis.prerequisRemplis;
  }

  /**
   * 🔹 Obtenir les prérequis manquants
   */
  getMissingPrerequis(prerequis: PrerequisStatus): string[] {
    return prerequis.details
      .filter(detail => !detail.valide)
      .map(detail => detail.critere);
  }

  /**
   * 🔹 Valider la composition du jury
   */
  validateJuryComposition(jury: JuryMember[]): { valid: boolean; errors: string[] } {
    const errors: string[] = [];
    
    // Vérifier qu'il y a au moins 3 membres
    if (jury.length < 3) {
      errors.push('Le jury doit comporter au moins 3 membres');
    }
    
    // Vérifier qu'il y a un président
    const presidents = jury.filter(member => member.role === 'PRESIDENT');
    if (presidents.length === 0) {
      errors.push('Le jury doit avoir un président');
    } else if (presidents.length > 1) {
      errors.push('Le jury ne peut avoir qu\'un seul président');
    }
    
    // Vérifier qu'il y a au moins un rapporteur
    const rapporteurs = jury.filter(member => member.role === 'RAPPORTEUR');
    if (rapporteurs.length === 0) {
      errors.push('Le jury doit avoir au moins un rapporteur');
    }
    
    return {
      valid: errors.length === 0,
      errors
    };
  }
}