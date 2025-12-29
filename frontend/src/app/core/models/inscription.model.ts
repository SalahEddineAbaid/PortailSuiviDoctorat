import { User } from './user.model';
import { Document } from './document.model';

/**
 * Inscription models aligned with backend DTOs
 */

export interface Inscription {
  id: number;
  doctorant: User;
  directeur: User;
  campagne: Campagne;
  sujetThese: string;
  laboratoire: string;
  specialite: string;
  dateInscription: Date;
  statut: InscriptionStatus;
  documents: Document[];
  avisDirecteur?: AvisDirecteur;
  validationAdmin?: ValidationAdmin;
}

export interface Campagne {
  id: number;
  nom: string;
  anneeUniversitaire: string;
  dateOuverture: Date;
  dateFermeture: Date;
  active: boolean;
  typeInscription: TypeInscription;
  description?: string;
}

export enum TypeInscription {
  PREMIERE = 'PREMIERE',
  REINSCRIPTION = 'REINSCRIPTION'
}

export interface AvisDirecteur {
  id: number;
  avis: string;
  dateAvis: Date;
  valide: boolean;
  commentaire?: string;
}

export interface ValidationAdmin {
  id: number;
  commentaire: string;
  dateValidation: Date;
  valide: boolean;
  validateur: User;
}

export enum InscriptionStatus {
  BROUILLON = 'BROUILLON',
  SOUMISE = 'SOUMISE',
  EN_COURS_VALIDATION = 'EN_COURS_VALIDATION',
  VALIDEE = 'VALIDEE',
  REJETEE = 'REJETEE'
}

export interface InscriptionRequest {
  directeurId: number;
  campagneId: number;
  sujetThese: string;
  laboratoire: string;
  specialite: string;
}

export interface InscriptionResponse {
  id: number;
  doctorant: User;
  directeur: User;
  campagne: Campagne;
  sujetThese: string;
  laboratoire: string;
  specialite: string;
  dateInscription: Date;
  statut: InscriptionStatus;
  documents?: Document[];
  avisDirecteur?: AvisDirecteur;
  validationAdmin?: ValidationAdmin;
}

export interface CampagneRequest {
  nom: string;
  anneeUniversitaire: string;
  dateOuverture: Date;
  dateFermeture: Date;
  typeInscription: TypeInscription;
  active?: boolean;
  description?: string;
}

export interface CampagneResponse {
  id: number;
  nom: string;
  anneeUniversitaire: string;
  dateOuverture: Date;
  dateFermeture: Date;
  active: boolean;
  typeInscription: TypeInscription;
  dateCreation: Date;
  description?: string;
}

export interface ValidationRequest {
  commentaire: string;
  valide: boolean;
}