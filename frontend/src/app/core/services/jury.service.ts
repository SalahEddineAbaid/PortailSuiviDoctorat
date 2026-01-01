import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { JuryMember, JuryMemberRequest, JuryRole } from '../models/soutenance.model';

/**
 * Interface for Jury Response from backend
 */
export interface JuryResponseDTO {
    id: number;
    defenseRequestId: number;
    members: JuryMember[];
    status: JuryStatus;
    createdAt?: Date;
    updatedAt?: Date;
}

export enum JuryStatus {
    PROPOSED = 'PROPOSED',
    VALIDATED = 'VALIDATED',
    REJECTED = 'REJECTED'
}

/**
 * Service for managing jury operations
 * Aligned with JuryController in defense-service
 */
@Injectable({
    providedIn: 'root'
})
export class JuryService {
    private readonly API_URL = `${environment.apiUrl}/api/defense-service/juries`;

    constructor(private http: HttpClient) { }

    /**
     * 🔹 Créer/Proposer un jury pour une demande de soutenance
     */
    createJury(defenseRequestId: number, members: JuryMemberRequest[]): Observable<JuryResponseDTO> {
        console.log('📤 [JURY SERVICE] Création du jury pour la demande:', defenseRequestId);
        const payload = {
            defenseRequestId,
            members
        };
        return this.http.post<JuryResponseDTO>(this.API_URL, payload);
    }

    /**
     * 🔹 Récupérer le jury d'une demande de soutenance
     */
    getJuryByDefenseRequest(defenseRequestId: number): Observable<JuryResponseDTO> {
        console.log('📤 [JURY SERVICE] Récupération du jury pour la demande:', defenseRequestId);
        return this.http.get<JuryResponseDTO>(`${this.API_URL}/defense-request/${defenseRequestId}`);
    }

    /**
     * 🔹 Mettre à jour le statut du jury
     */
    updateJuryStatus(juryId: number, status: JuryStatus): Observable<JuryResponseDTO> {
        console.log('📤 [JURY SERVICE] Mise à jour du statut du jury:', juryId, status);
        return this.http.patch<JuryResponseDTO>(`${this.API_URL}/${juryId}/status`, null, {
            params: { status }
        });
    }

    /**
      * 🔹 Valider un jury (DIRECTEUR/ADMIN)
      */
    validateJury(juryId: number): Observable<JuryResponseDTO> {
        console.log('📤 [JURY SERVICE] Validation du jury:', juryId);
        return this.updateJuryStatus(juryId, JuryStatus.VALIDATED);
    }

    /**
     * 🔹 Rejeter un jury (DIRECTEUR/ADMIN)
     */
    rejectJury(juryId: number): Observable<JuryResponseDTO> {
        console.log('📤 [JURY SERVICE] Rejet du jury:', juryId);
        return this.updateJuryStatus(juryId, JuryStatus.REJECTED);
    }

    /**
     * 🔹 Valider la composition du jury côté client
     */
    validateJuryComposition(members: JuryMember[]): { valid: boolean; errors: string[] } {
        const errors: string[] = [];

        if (members.length < 3) {
            errors.push('Le jury doit comporter au moins 3 membres');
        }

        const presidents = members.filter(member => member.role === JuryRole.PRESIDENT);
        if (presidents.length === 0) {
            errors.push('Le jury doit avoir un président');
        } else if (presidents.length > 1) {
            errors.push('Le jury ne peut avoir qu\'un seul président');
        }

        const rapporteurs = members.filter(member => member.role === JuryRole.RAPPORTEUR);
        if (rapporteurs.length === 0) {
            errors.push('Le jury doit avoir au moins un rapporteur');
        }

        const examinateurs = members.filter(member => member.role === JuryRole.EXAMINATEUR);
        if (examinateurs.length === 0) {
            errors.push('Le jury doit avoir au moins un examinateur');
        }

        return {
            valid: errors.length === 0,
            errors
        };
    }

    /**
     * 🔹 Obtenir le libellé du rôle
     */
    getRoleLabel(role: JuryRole): string {
        const labels = {
            [JuryRole.PRESIDENT]: 'Président',
            [JuryRole.RAPPORTEUR]: 'Rapporteur',
            [JuryRole.EXAMINATEUR]: 'Examinateur',
            [JuryRole.DIRECTEUR]: 'Directeur de thèse',
            [JuryRole.CO_DIRECTEUR]: 'Co-directeur de thèse'
        };
        return labels[role] || role;
    }

    /**
     * 🔹 Obtenir la couleur du statut
     */
    getStatusColor(status: JuryStatus): string {
        const colors = {
            [JuryStatus.PROPOSED]: 'orange',
            [JuryStatus.VALIDATED]: 'green',
            [JuryStatus.REJECTED]: 'red'
        };
        return colors[status] || 'gray';
    }

    /**
     * 🔹 Obtenir le libellé du statut
     */
    getStatusLabel(status: JuryStatus): string {
        const labels = {
            [JuryStatus.PROPOSED]: 'Proposé',
            [JuryStatus.VALIDATED]: 'Validé',
            [JuryStatus.REJECTED]: 'Rejeté'
        };
        return labels[status] || status;
    }
}
