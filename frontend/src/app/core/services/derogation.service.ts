import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  DerogationRequestDTO,
  DerogationValidationDTO,
  DerogationResponse,
  StatutDerogation,
  isDerogationRequired
} from '../models/derogation.model';

@Injectable({
  providedIn: 'root'
})
export class DerogationService {
  private readonly apiUrl = `${environment.apiUrl}/inscriptions`;

  constructor(private http: HttpClient) {}

  // ============================================
  // CRUD Operations
  // ============================================

  /**
   * Créer une demande de dérogation
   */
  createDerogation(
    inscriptionId: number,
    motif: string,
    documentsJustificatifs?: File
  ): Observable<DerogationResponse> {
    const formData = new FormData();
    formData.append('motif', motif);
    
    if (documentsJustificatifs) {
      formData.append('documents', documentsJustificatifs);
    }

    return this.http.post<DerogationResponse>(
      `${this.apiUrl}/${inscriptionId}/derogation`,
      formData
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Récupérer une demande de dérogation
   */
  getDerogation(inscriptionId: number): Observable<DerogationResponse> {
    return this.http.get<DerogationResponse>(
      `${this.apiUrl}/${inscriptionId}/derogation`
    ).pipe(
      catchError(this.handleError)
    );
  }

  // ============================================
  // Validation Operations
  // ============================================

  /**
   * Valider une dérogation par le directeur
   */
  validerParDirecteur(
    inscriptionId: number,
    validation: DerogationValidationDTO,
    directeurId: number
  ): Observable<DerogationResponse> {
    const params = new HttpParams().set('directeurId', directeurId.toString());
    
    return this.http.post<DerogationResponse>(
      `${this.apiUrl}/${inscriptionId}/derogation/valider-directeur`,
      validation,
      { params }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Valider une dérogation par le PED
   */
  validerParPED(
    inscriptionId: number,
    validation: DerogationValidationDTO
  ): Observable<DerogationResponse> {
    return this.http.post<DerogationResponse>(
      `${this.apiUrl}/${inscriptionId}/derogation/valider-ped`,
      validation
    ).pipe(
      catchError(this.handleError)
    );
  }

  // ============================================
  // Helper Methods
  // ============================================

  /**
   * Vérifier si une dérogation est requise
   */
  isDerogationRequired(dureeDoctoratMois: number): boolean {
    return isDerogationRequired(dureeDoctoratMois);
  }

  /**
   * Obtenir le message d'avertissement
   */
  getWarningMessage(dureeDoctoratMois: number): string | null {
    if (dureeDoctoratMois <= 36) {
      return null;
    }
    
    const annees = Math.floor(dureeDoctoratMois / 12);
    const mois = dureeDoctoratMois % 12;
    
    let dureeText = `${annees} an${annees > 1 ? 's' : ''}`;
    if (mois > 0) {
      dureeText += ` et ${mois} mois`;
    }
    
    return `Votre doctorat dure ${dureeText}. Une demande de dérogation est obligatoire pour continuer.`;
  }

  /**
   * Vérifier si le motif est valide
   */
  isMotifValid(motif: string): boolean {
    return motif.trim().length >= 50;
  }

  /**
   * Obtenir le statut label
   */
  getStatutLabel(statut: StatutDerogation): string {
    const labels: { [key in StatutDerogation]: string } = {
      [StatutDerogation.EN_ATTENTE_DIRECTEUR]: 'En attente directeur',
      [StatutDerogation.APPROUVE_DIRECTEUR]: 'Approuvé par directeur',
      [StatutDerogation.REJETE_DIRECTEUR]: 'Rejeté par directeur',
      [StatutDerogation.EN_ATTENTE_PED]: 'En attente PED',
      [StatutDerogation.APPROUVE]: 'Approuvé',
      [StatutDerogation.REJETE]: 'Rejeté'
    };
    return labels[statut];
  }

  /**
   * Obtenir la couleur du statut
   */
  getStatutColor(statut: StatutDerogation): string {
    const colors: { [key in StatutDerogation]: string } = {
      [StatutDerogation.EN_ATTENTE_DIRECTEUR]: 'orange',
      [StatutDerogation.APPROUVE_DIRECTEUR]: 'cyan',
      [StatutDerogation.REJETE_DIRECTEUR]: 'red',
      [StatutDerogation.EN_ATTENTE_PED]: 'orange',
      [StatutDerogation.APPROUVE]: 'green',
      [StatutDerogation.REJETE]: 'red'
    };
    return colors[statut];
  }

  /**
   * Vérifier si la dérogation est en attente
   */
  isPending(statut: StatutDerogation): boolean {
    return [
      StatutDerogation.EN_ATTENTE_DIRECTEUR,
      StatutDerogation.APPROUVE_DIRECTEUR,
      StatutDerogation.EN_ATTENTE_PED
    ].includes(statut);
  }

  /**
   * Vérifier si la dérogation est approuvée
   */
  isApproved(statut: StatutDerogation): boolean {
    return statut === StatutDerogation.APPROUVE;
  }

  /**
   * Vérifier si la dérogation est rejetée
   */
  isRejected(statut: StatutDerogation): boolean {
    return [
      StatutDerogation.REJETE_DIRECTEUR,
      StatutDerogation.REJETE
    ].includes(statut);
  }

  /**
   * Vérifier si la dérogation peut être modifiée
   */
  canEdit(statut: StatutDerogation): boolean {
    return statut === StatutDerogation.EN_ATTENTE_DIRECTEUR;
  }

  /**
   * Calculer la progression de la dérogation
   */
  getProgress(statut: StatutDerogation): number {
    const progressMap: { [key in StatutDerogation]: number } = {
      [StatutDerogation.EN_ATTENTE_DIRECTEUR]: 25,
      [StatutDerogation.APPROUVE_DIRECTEUR]: 50,
      [StatutDerogation.REJETE_DIRECTEUR]: 100,
      [StatutDerogation.EN_ATTENTE_PED]: 75,
      [StatutDerogation.APPROUVE]: 100,
      [StatutDerogation.REJETE]: 100
    };
    return progressMap[statut];
  }

  /**
   * Obtenir les étapes de validation
   */
  getValidationSteps(): Array<{ label: string; description: string }> {
    return [
      {
        label: 'Demande',
        description: 'Soumission de la demande de dérogation'
      },
      {
        label: 'Directeur',
        description: 'Validation par le directeur de thèse'
      },
      {
        label: 'PED',
        description: 'Validation par le Président d\'Établissement Doctoral'
      },
      {
        label: 'Décision',
        description: 'Décision finale'
      }
    ];
  }

  /**
   * Obtenir l'étape courante
   */
  getCurrentStep(statut: StatutDerogation): number {
    const stepMap: { [key in StatutDerogation]: number } = {
      [StatutDerogation.EN_ATTENTE_DIRECTEUR]: 1,
      [StatutDerogation.APPROUVE_DIRECTEUR]: 2,
      [StatutDerogation.REJETE_DIRECTEUR]: 3,
      [StatutDerogation.EN_ATTENTE_PED]: 2,
      [StatutDerogation.APPROUVE]: 3,
      [StatutDerogation.REJETE]: 3
    };
    return stepMap[statut];
  }

  // ============================================
  // Validation
  // ============================================

  /**
   * Valider les données de la demande
   */
  validateDerogationRequest(motif: string): string[] {
    const errors: string[] = [];

    if (!motif || motif.trim().length === 0) {
      errors.push('Le motif est obligatoire');
    } else if (motif.trim().length < 50) {
      errors.push('Le motif doit contenir au moins 50 caractères');
    } else if (motif.trim().length > 2000) {
      errors.push('Le motif ne doit pas dépasser 2000 caractères');
    }

    return errors;
  }

  /**
   * Valider les données de validation
   */
  validateDerogationValidation(validation: DerogationValidationDTO): string[] {
    const errors: string[] = [];

    if (validation.approuve === undefined || validation.approuve === null) {
      errors.push('La décision est obligatoire');
    }

    if (!validation.approuve && (!validation.commentaire || validation.commentaire.trim().length === 0)) {
      errors.push('Un commentaire est obligatoire en cas de rejet');
    }

    return errors;
  }

  // ============================================
  // Error Handling
  // ============================================

  private handleError(error: any): Observable<never> {
    console.error('DerogationService Error:', error);
    
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
      errorMessage = 'Dérogation non trouvée';
    } else if (error.status === 409) {
      errorMessage = 'Une dérogation existe déjà pour cette inscription';
    } else if (error.status >= 500) {
      errorMessage = 'Erreur serveur';
    }

    return throwError(() => ({ message: errorMessage, originalError: error }));
  }
}
