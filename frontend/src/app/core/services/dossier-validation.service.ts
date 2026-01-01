import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * Statut Inscription Enum
 */
export enum StatutInscription {
    BROUILLON = 'BROUILLON',
    SOUMISE = 'SOUMISE',
    EN_ATTENTE_DIRECTEUR = 'EN_ATTENTE_DIRECTEUR',
    VALIDEE_DIRECTEUR = 'VALIDEE_DIRECTEUR',
    REJETEE_DIRECTEUR = 'REJETEE_DIRECTEUR',
    EN_ATTENTE_ADMIN = 'EN_ATTENTE_ADMIN',
    VALIDEE = 'VALIDEE',
    REJETEE = 'REJETEE'
}

/**
 * Inscription Response Interface
 */
export interface InscriptionResponse {
    id: number;
    doctorantId: number;
    campagneId: number;
    statut: StatutInscription;
    validationDirecteur?: ValidationInfo;
    validationAdmin?: ValidationInfo;
    createdAt: Date;
    updatedAt?: Date;
    // Additional fields from backend
    doctorant?: {
        id: number;
        nom: string;
        prenom: string;
        email: string;
    };
    campagne?: {
        id: number;
        nom: string;
    };
}

/**
 * Validation Info
 */
export interface ValidationInfo {
    date: Date;
    validateur: string;
    commentaire?: string;
}

/**
 * Validation Request
 */
export interface ValidationRequest {
    valide: boolean;
    commentaire: string;
}

/**
 * Derogation Request DTO
 */
export interface DerogationRequestDTO {
    motif: string;
    justification: string;
    anneesSupplementaires: number;
}

/**
 * Derogation Validation DTO
 */
export interface DerogationValidationDTO {
    valide: boolean;
    commentaire?: string;
}

/**
 * Derogation Response
 */
export interface DerogationResponse {
    id: number;
    inscriptionId: number;
    motif: string;
    justification: string;
    anneesSupplementaires: number;
    statut: string;
    validationDirecteur?: ValidationInfo;
    validationPED?: ValidationInfo;
    createdAt: Date;
}

/**
 * Dashboard Doctorant Response
 */
export interface DashboardDoctorantResponse {
    doctorant: {
        id: number;
        nom: string;
        prenom: string;
        email: string;
    };
    inscriptionCourante?: InscriptionResponse;
    inscriptionsHistorique: InscriptionResponse[];
    alertes: AlerteInscription[];
    statistiques: {
        totalInscriptions: number;
        inscriptionsValidees: number;
        inscriptionsEnAttente: number;
    };
}

/**
 * Alerte Inscription
 */
export interface AlerteInscription {
    type: 'DUREE_NORMALE' | 'DUREE_MAX' | 'DOCUMENT_MANQUANT';
    message: string;
    niveau: 'INFO' | 'WARNING' | 'DANGER';
    inscriptionId: number;
}

/**
 * Service for Dossier/Inscription Validation (Admin/Directeur)
 * Integrates with inscription-service microservice
 * Base URL: http://localhost:8083/api/inscriptions
 */
@Injectable({
    providedIn: 'root'
})
export class DossierValidationService {
    private readonly API_URL = `${environment.apiUrl}/api/inscriptions`;
    private readonly DEROGATION_API = `${environment.apiUrl}/api/derogation`;

    constructor(private http: HttpClient) { }

    // ===== CRUD OPERATIONS =====

    /**
     * Get inscription by ID
     * GET /api/inscriptions/{id}
     */
    getInscription(id: number): Observable<InscriptionResponse> {
        console.log('📤 [DOSSIER VALIDATION] Fetching inscription:', id);
        return this.http.get<InscriptionResponse>(`${this.API_URL}/${id}`);
    }

    /**
     * Get inscriptions by doctorant
     * GET /api/inscriptions/doctorant/{doctorantId}
     */
    getInscriptionsByDoctorant(doctorantId: number): Observable<InscriptionResponse[]> {
        console.log('📤 [DOSSIER VALIDATION] Fetching inscriptions for doctorant:', doctorantId);
        return this.http.get<InscriptionResponse[]>(`${this.API_URL}/doctorant/${doctorantId}`);
    }

    // ===== VALIDATION WORKFLOW =====

    /**
     * Get inscriptions pending for directeur
     * GET /api/inscriptions/directeur/{directeurId}/en-attente
     */
    getPendingForDirecteur(directeurId: number): Observable<InscriptionResponse[]> {
        console.log('📤 [DOSSIER VALIDATION] Fetching pending inscriptions for directeur:', directeurId);
        return this.http.get<InscriptionResponse[]>(`${this.API_URL}/directeur/${directeurId}/en-attente`);
    }

    /**
     * Validate inscription by directeur
     * PUT /api/inscriptions/{id}/valider-directeur?directeurId
     */
    validateByDirecteur(id: number, directeurId: number, request: ValidationRequest): Observable<InscriptionResponse> {
        console.log('📤 [DOSSIER VALIDATION] Directeur validating inscription:', id);
        const params = new HttpParams().set('directeurId', directeurId.toString());
        return this.http.put<InscriptionResponse>(`${this.API_URL}/${id}/valider-directeur`, request, { params });
    }

    /**
     * Get inscriptions pending for admin
     * GET /api/inscriptions/admin/en-attente
     */
    getPendingForAdmin(): Observable<InscriptionResponse[]> {
        console.log('📤 [DOSSIER VALIDATION] Fetching pending inscriptions for admin');
        return this.http.get<InscriptionResponse[]>(`${this.API_URL}/admin/en-attente`);
    }

    /**
     * Validate inscription by admin
     * PUT /api/inscriptions/{id}/valider-admin
     */
    validateByAdmin(id: number, request: ValidationRequest): Observable<InscriptionResponse> {
        console.log('📤 [DOSSIER VALIDATION] Admin validating inscription:', id);
        return this.http.put<InscriptionResponse>(`${this.API_URL}/${id}/valider-admin`, request);
    }

    // ===== DEROGATION MANAGEMENT =====

    /**
     * Create derogation request
     * POST /api/inscriptions/{id}/derogation
     */
    createDerogation(id: number, request: DerogationRequestDTO): Observable<DerogationResponse> {
        console.log('📤 [DOSSIER VALIDATION] Creating derogation for inscription:', id);
        return this.http.post<DerogationResponse>(`${this.API_URL}/${id}/derogation`, request);
    }

    /**
     * Get derogation for inscription
     * GET /api/inscriptions/{id}/derogation
     */
    getDerogation(id: number): Observable<DerogationResponse> {
        console.log('📤 [DOSSIER VALIDATION] Fetching derogation for inscription:', id);
        return this.http.get<DerogationResponse>(`${this.API_URL}/${id}/derogation`);
    }

    /**
     * Validate derogation by directeur
     * PUT /derogation/{id}/valider-directeur?directeurId
     */
    validateDerogationByDirecteur(id: number, directeurId: number, request: DerogationValidationDTO): Observable<DerogationResponse> {
        console.log('📤 [DOSSIER VALIDATION] Directeur validating derogation:', id);
        const params = new HttpParams().set('directeurId', directeurId.toString());
        return this.http.put<DerogationResponse>(`${this.DEROGATION_API}/${id}/valider-directeur`, request, { params });
    }

    /**
     * Validate derogation by PED
     * PUT /derogation/{id}/valider-ped
     */
    validateDerogationByPED(id: number, request: DerogationValidationDTO): Observable<DerogationResponse> {
        console.log('📤 [DOSSIER VALIDATION] PED validating derogation:', id);
        return this.http.put<DerogationResponse>(`${this.DEROGATION_API}/${id}/valider-ped`, request);
    }

    // ===== DASHBOARD & REPORTING =====

    /**
     * Get dashboard for doctorant
     * GET /dashboard/doctorant/{id}?userId&role
     */
    getDashboardDoctorant(id: number, userId: number, role: string): Observable<DashboardDoctorantResponse> {
        console.log('📤 [DOSSIER VALIDATION] Fetching dashboard for doctorant:', id);
        const params = new HttpParams()
            .set('userId', userId.toString())
            .set('role', role);
        return this.http.get<DashboardDoctorantResponse>(`${environment.apiUrl}/api/dashboard/doctorant/${id}`, { params });
    }

    /**
     * Verify alerts for all active inscriptions (admin/batch)
     * POST /api/inscriptions/alertes/verifier
     */
    verifyAlerts(): Observable<any> {
        console.log('📤 [DOSSIER VALIDATION] Verifying alerts');
        return this.http.post(`${this.API_URL}/alertes/verifier`, {});
    }

    // ===== HELPER METHODS =====

    /**
     * Get status label
     */
    getStatusLabel(statut: StatutInscription): string {
        const labels = {
            [StatutInscription.BROUILLON]: 'Brouillon',
            [StatutInscription.SOUMISE]: 'Soumise',
            [StatutInscription.EN_ATTENTE_DIRECTEUR]: 'En attente directeur',
            [StatutInscription.VALIDEE_DIRECTEUR]: 'Validée par directeur',
            [StatutInscription.REJETEE_DIRECTEUR]: 'Rejetée par directeur',
            [StatutInscription.EN_ATTENTE_ADMIN]: 'En attente administration',
            [StatutInscription.VALIDEE]: 'Validée',
            [StatutInscription.REJETEE]: 'Rejetée'
        };
        return labels[statut] || statut;
    }

    /**
     * Get status color for UI
     */
    getStatusColor(statut: StatutInscription): string {
        const colors = {
            [StatutInscription.BROUILLON]: 'gray',
            [StatutInscription.SOUMISE]: 'blue',
            [StatutInscription.EN_ATTENTE_DIRECTEUR]: 'orange',
            [StatutInscription.VALIDEE_DIRECTEUR]: 'lightgreen',
            [StatutInscription.REJETEE_DIRECTEUR]: 'red',
            [StatutInscription.EN_ATTENTE_ADMIN]: 'orange',
            [StatutInscription.VALIDEE]: 'green',
            [StatutInscription.REJETEE]: 'red'
        };
        return colors[statut] || 'gray';
    }

    /**
     * Check if inscription is pending validation
     */
    isPending(inscription: InscriptionResponse): boolean {
        return [
            StatutInscription.EN_ATTENTE_DIRECTEUR,
            StatutInscription.EN_ATTENTE_ADMIN,
            StatutInscription.SOUMISE
        ].includes(inscription.statut);
    }

    /**
     * Check if inscription can be validated by directeur
     */
    canValidateByDirecteur(inscription: InscriptionResponse): boolean {
        return inscription.statut === StatutInscription.EN_ATTENTE_DIRECTEUR ||
            inscription.statut === StatutInscription.SOUMISE;
    }

    /**
     * Check if inscription can be validated by admin
     */
    canValidateByAdmin(inscription: InscriptionResponse): boolean {
        return inscription.statut === StatutInscription.EN_ATTENTE_ADMIN ||
            inscription.statut === StatutInscription.VALIDEE_DIRECTEUR;
    }

    /**
     * Get alerte level color
     */
    getAlerteLevelColor(niveau: 'INFO' | 'WARNING' | 'DANGER'): string {
        const colors = {
            'INFO': 'blue',
            'WARNING': 'orange',
            'DANGER': 'red'
        };
        return colors[niveau] || 'gray';
    }
}
