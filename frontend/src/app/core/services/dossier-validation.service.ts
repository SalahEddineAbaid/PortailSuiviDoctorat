import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from '../../environments/environment';

export enum StatutInscription {
  BROUILLON = 'BROUILLON',
  SOUMIS = 'SOUMIS',
  EN_ATTENTE_DIRECTEUR = 'EN_ATTENTE_DIRECTEUR',
  APPROUVE_DIRECTEUR = 'APPROUVE_DIRECTEUR',
  EN_ATTENTE_ADMIN = 'EN_ATTENTE_ADMIN',
  VALIDE = 'VALIDE',
  REJETE = 'REJETE'
}

export interface InscriptionResponse {
  id: number;
  doctorantId: number;
  directeurTheseId: number;
  campagneId?: number;
  statut: StatutInscription;
  createdAt?: string;
  doctorant?: {
    nom: string;
    prenom: string;
    email: string;
  };
  campagne?: {
    id: number;
    nom: string;
  };
}

@Injectable({
  providedIn: 'root'
})
export class DossierValidationService {
  private readonly apiUrl = `${environment.apiUrl}/inscriptions`;

  constructor(private http: HttpClient) {}

  getPendingForAdmin(): Observable<InscriptionResponse[]> {
    return this.http.get<InscriptionResponse[]>(`${this.apiUrl}/admin/en-attente`);
  }

  isPending(dossier: InscriptionResponse): boolean {
    return [
      StatutInscription.SOUMIS,
      StatutInscription.EN_ATTENTE_DIRECTEUR,
      StatutInscription.EN_ATTENTE_ADMIN
    ].includes(dossier.statut);
  }

  getStatusLabel(statut: StatutInscription): string {
    const labels: { [key in StatutInscription]: string } = {
      [StatutInscription.BROUILLON]: 'Brouillon',
      [StatutInscription.SOUMIS]: 'Soumis',
      [StatutInscription.EN_ATTENTE_DIRECTEUR]: 'En attente directeur',
      [StatutInscription.APPROUVE_DIRECTEUR]: 'Approuvé directeur',
      [StatutInscription.EN_ATTENTE_ADMIN]: 'En attente admin',
      [StatutInscription.VALIDE]: 'Validé',
      [StatutInscription.REJETE]: 'Rejeté'
    };
    return labels[statut] || statut;
  }

  getStatusColor(statut: StatutInscription): string {
    const colors: { [key in StatutInscription]: string } = {
      [StatutInscription.BROUILLON]: 'gray',
      [StatutInscription.SOUMIS]: 'blue',
      [StatutInscription.EN_ATTENTE_DIRECTEUR]: 'orange',
      [StatutInscription.APPROUVE_DIRECTEUR]: 'teal',
      [StatutInscription.EN_ATTENTE_ADMIN]: 'purple',
      [StatutInscription.VALIDE]: 'green',
      [StatutInscription.REJETE]: 'red'
    };
    return colors[statut] || 'gray';
  }
}
