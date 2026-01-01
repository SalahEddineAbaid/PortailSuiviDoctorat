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
  // ✅ Aligned with actual backend controllers
  private readonly DEFENSE_REQUEST_API = `${environment.apiUrl}/api/defense-service/defense-requests`;
  private readonly JURY_API = `${environment.apiUrl}/api/defense-service/juries`;
  private readonly PREREQUISITES_API = `${environment.apiUrl}/api/defense-service/prerequisites`;
  private readonly DOCUMENTS_API = `${environment.apiUrl}/api/defense-service/documents`;

  constructor(private http: HttpClient) { }

  // ===== DEFENSE REQUEST ENDPOINTS =====

  /**
   * 🔹 Créer une demande de soutenance (DOCTORANT)
   */
  createDemandeSoutenance(data: SoutenanceRequest): Observable<SoutenanceResponse> {
    console.log('📤 [SOUTENANCE SERVICE] Création demande soutenance:', data);
    return this.http.post<SoutenanceResponse>(this.DEFENSE_REQUEST_API, data);
  }

  /**
   * 🔹 Récupérer une demande par ID
   */
  getDefenseRequestById(id: number): Observable<SoutenanceResponse> {
    console.log('📤 [SOUTENANCE SERVICE] Récupération demande:', id);
    return this.http.get<SoutenanceResponse>(`${this.DEFENSE_REQUEST_API}/${id}`);
  }

  /**
   * 🔹 Récupérer toutes les demandes
   */
  getAllDefenseRequests(): Observable<SoutenanceResponse[]> {
    console.log('📤 [SOUTENANCE SERVICE] Récupération toutes les demandes');
    return this.http.get<SoutenanceResponse[]>(this.DEFENSE_REQUEST_API);
  }

  /**
   * 🔹 Mettre à jour une demande de soutenance (DOCTORANT)
   */
  updateDemandeSoutenance(id: number, data: SoutenanceRequest): Observable<SoutenanceResponse> {
    console.log('📤 [SOUTENANCE SERVICE] Mise à jour soutenance:', id, data);
    return this.http.put<SoutenanceResponse>(`${this.DEFENSE_REQUEST_API}/${id}`, data);
  }

  /**
   * 🔹 Mettre à jour le statut d'une demande
   */
  updateDefenseRequestStatus(id: number, status: string): Observable<SoutenanceResponse> {
    console.log('📤 [SOUTENANCE SERVICE] Mise à jour statut:', id, status);
    return this.http.patch<SoutenanceResponse>(`${this.DEFENSE_REQUEST_API}/${id}/status`, null, {
      params: { status }
    });
  }

  /**
   * 🔹 Supprimer une demande
   */
  deleteDefenseRequest(id: number): Observable<void> {
    console.log('📤 [SOUTENANCE SERVICE] Suppression demande:', id);
    return this.http.delete<void>(`${this.DEFENSE_REQUEST_API}/${id}`);
  }

  // ===== QUERY METHODS (Role-based filtering) =====

  /**
   * 🔹 Récupérer mes soutenances (utilisateur connecté)
   * Note: This will use getAllDefenseRequests and filter client-side based on current user
   */
  getMySoutenances(): Observable<SoutenanceResponse[]> {
    console.log('📤 [SOUTENANCE SERVICE] Mes soutenances');
    // Backend doesn't have /me endpoint, use getAll and filter client-side
    return this.getAllDefenseRequests();
  }

  /**
   * 🔹 Récupérer les soutenances d'un doctorant
   */
  getSoutenancesDoctorant(doctorantId: number): Observable<SoutenanceResponse[]> {
    console.log('📤 [SOUTENANCE SERVICE] Soutenances doctorant:', doctorantId);
    // Backend doesn't have this specific endpoint, use getAll and filter client-side
    return this.getAllDefenseRequests();
  }

  /**
   * 🔹 Récupérer les soutenances à valider pour un directeur
   */
  getSoutenancesEnAttenteDirecteur(directeurId: number): Observable<SoutenanceResponse[]> {
    console.log('📤 [SOUTENANCE SERVICE] Soutenances en attente directeur:', directeurId);
    // Backend doesn't have this specific endpoint, use getAll and filter client-side
    return this.getAllDefenseRequests();
  }

  /**
   * 🔹 Récupérer toutes les soutenances d'un directeur
   */
  getSoutenancesByDirecteur(): Observable<SoutenanceResponse[]> {
    console.log('📤 [SOUTENANCE SERVICE] Soutenances par directeur');
    // Backend doesn't have this specific endpoint, use getAll and filter client-side
    return this.getAllDefenseRequests();
  }

  /**
   * 🔹 Récupérer les soutenances en attente admin
   */
  getSoutenancesEnAttenteAdmin(): Observable<SoutenanceResponse[]> {
    console.log('📤 [SOUTENANCE SERVICE] Soutenances en attente admin');
    // Backend doesn't have this specific endpoint, use getAll and filter client-side
    return this.getAllDefenseRequests();
  }

  /**
   * 🔹 Récupérer une soutenance par ID (alias for backward compatibility)
   */
  getSoutenance(id: number): Observable<SoutenanceResponse> {
    console.log('📤 [SOUTENANCE SERVICE] Soutenance:', id);
    return this.getDefenseRequestById(id);
  }

  // ===== PREREQUISITES ENDPOINTS =====

  /**
   * 🔹 Récupérer les prérequis d'un doctorant
   */
  getPrerequisitesByDoctorant(doctorantId: number): Observable<any[]> {
    console.log('📤 [SOUTENANCE SERVICE] Récupération prérequis doctorant:', doctorantId);
    return this.http.get<any[]>(`${this.PREREQUISITES_API}/doctorant/${doctorantId}`);
  }

  /**
   * 🔹 Récupérer les prérequis validés d'un doctorant
   */
  getValidatedPrerequisitesByDoctorant(doctorantId: number): Observable<any[]> {
    console.log('📤 [SOUTENANCE SERVICE] Prérequis validés doctorant:', doctorantId);
    return this.http.get<any[]>(`${this.PREREQUISITES_API}/doctorant/${doctorantId}/validated`);
  }

  /**
   * 🔹 Vérifier les prérequis pour une soutenance (legacy compatibility)
   */
  checkPrerequis(doctorantId: number): Observable<PrerequisStatus> {
    console.log('📤 [SOUTENANCE SERVICE] Vérification prérequis:', doctorantId);
    // This will need transformation from backend format to PrerequisStatus
    return this.getValidatedPrerequisitesByDoctorant(doctorantId) as any;
  }

  /**
   * 🔹 Valider ou rejeter les prérequis
   */
  validatePrerequis(prerequisitesId: number, valid: boolean): Observable<any> {
    console.log('📤 [SOUTENANCE SERVICE] Validation prérequis:', prerequisitesId, valid);
    return this.http.patch(`${this.PREREQUISITES_API}/${prerequisitesId}/validate`, null, {
      params: { valid: valid.toString() }
    });
  }

  // ===== JURY ENDPOINTS =====

  /**
   * 🔹 Créer/Proposer un jury pour une demande
   */
  createJury(defenseRequestId: number, juryData: { members: JuryMemberRequest[] }): Observable<any> {
    console.log('📤 [SOUTENANCE SERVICE] Création jury:', defenseRequestId, juryData);
    return this.http.post(`${this.JURY_API}`, { defenseRequestId, ...juryData });
  }

  /**
   * 🔹 Récupérer le jury d'une demande
   */
  getJuryByDefenseRequest(defenseRequestId: number): Observable<any> {
    console.log('📤 [SOUTENANCE SERVICE] Récupération jury:', defenseRequestId);
    return this.http.get(`${this.JURY_API}/defense-request/${defenseRequestId}`);
  }

  /**
   * 🔹 Mettre à jour le statut du jury
   */
  updateJuryStatus(juryId: number, status: string): Observable<any> {
    console.log('📤 [SOUTENANCE SERVICE] Mise à jour statut jury:', juryId, status);
    return this.http.patch(`${this.JURY_API}/${juryId}/status`, null, {
      params: { status }
    });
  }

  /**
   * 🔹 Proposer un jury (legacy compatibility)
   */
  proposeJury(defenseRequestId: number, jury: JuryMemberRequest[]): Observable<any> {
    console.log('📤 [SOUTENANCE SERVICE] Proposition jury:', defenseRequestId, jury);
    return this.createJury(defenseRequestId, { members: jury });
  }

  /**
   * 🔹 Valider un jury (DIRECTEUR/ADMIN) - legacy compatibility
   */
  validerJury(juryId: number, validation: { valide: boolean; commentaire?: string }): Observable<any> {
    console.log('📤 [SOUTENANCE SERVICE] Validation jury:', juryId, validation);
    const status = validation.valide ? 'VALIDATED' : 'REJECTED';
    return this.updateJuryStatus(juryId, status);
  }

  // ===== PLANNING AND AUTHORIZATION ENDPOINTS =====

  /**
   * 🔹 Planifier une soutenance (date, lieu) - via update
   */
  planifierSoutenance(defenseRequestId: number, planning: { dateSoutenance: Date; lieuSoutenance: string }): Observable<any> {
    console.log('📤 [SOUTENANCE SERVICE] Planification:', defenseRequestId, planning);
    // Use updateDemandeSoutenance to update planning details
    return this.updateDemandeSoutenance(defenseRequestId, planning as any);
  }

  /**
   * 🔹 Autoriser une soutenance (ADMIN) - via status update
   */
  autoriserSoutenance(defenseRequestId: number): Observable<SoutenanceResponse> {
    console.log('📤 [SOUTENANCE SERVICE] Autorisation soutenance:', defenseRequestId);
    return this.updateDefenseRequestStatus(defenseRequestId, 'AUTORISEE');
  }

  /**
   * 🔹 Rejeter une soutenance (ADMIN) - via status update
   */
  rejeterSoutenance(defenseRequestId: number, motif: string): Observable<SoutenanceResponse> {
    console.log('📤 [SOUTENANCE SERVICE] Rejet soutenance:', defenseRequestId, motif);
    // Note: Backend status update doesn't include motif parameter
    // This would need to be stored separately if tracking rejection reasons
    return this.updateDefenseRequestStatus(defenseRequestId, 'REJETEE');
  }

  /**
   * 🔹 Valider une soutenance par l'administration (ADMIN)
   */
  validerParAdmin(id: number, validation: { valide: boolean; commentaire: string }): Observable<SoutenanceResponse> {
    console.log('📤 [SOUTENANCE SERVICE] Validation admin:', id, validation);
    const status = validation.valide ? 'EN_COURS_VALIDATION' : 'REJETEE';
    return this.updateDefenseRequestStatus(id, status);
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