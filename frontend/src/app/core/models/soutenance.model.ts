import { User } from './user.model';
import { Document } from './document.model';

/**
 * Soutenance (Defense) models aligned with backend DTOs
 */

export interface Soutenance {
  id: number;
  doctorant: User;
  directeur: User;
  titrethese: string;
  resumeThese?: string;
  specialite?: string;
  laboratoire?: string;
  dateSoutenance?: Date;
  lieuSoutenance?: string;
  statut: SoutenanceStatus;
  prerequis: PrerequisStatus;
  jury: JuryMember[];
  documents: Document[];
  rapporteurs: User[];
}

export enum SoutenanceStatus {
  BROUILLON = 'BROUILLON',
  SOUMISE = 'SOUMISE',
  EN_COURS_VALIDATION = 'EN_COURS_VALIDATION',
  AUTORISEE = 'AUTORISEE',
  REJETEE = 'REJETEE',
  SOUTENUE = 'SOUTENUE'
}

export interface PrerequisStatus {
  publicationsValides: boolean;
  heuresFormationValides: boolean;
  dureeDoctoratValide: boolean;
  documentsCompletsValides: boolean;
  prerequisRemplis: boolean;
  details: PrerequisDetail[];
}

export interface PrerequisDetail {
  critere: string;
  valide: boolean;
  commentaire?: string;
  valeurRequise?: string;
  valeurActuelle?: string;
}

export interface JuryMember {
  id: number;
  nom: string;
  prenom: string;
  etablissement: string;
  grade: string;
  role: JuryRole;
  externe: boolean;
}

export enum JuryRole {
  PRESIDENT = 'PRESIDENT',
  RAPPORTEUR = 'RAPPORTEUR',
  EXAMINATEUR = 'EXAMINATEUR',
  DIRECTEUR = 'DIRECTEUR',
  CO_DIRECTEUR = 'CO_DIRECTEUR'
}

export interface SoutenanceRequest {
  titrethese: string;
  resumeThese?: string;
  specialite?: string;
  laboratoire?: string;
  dateSoutenance?: Date;
  lieuSoutenance?: string;
  jury: JuryMemberRequest[];
}

export interface JuryMemberRequest {
  nom: string;
  prenom: string;
  etablissement: string;
  grade: string;
  role: JuryRole;
  externe: boolean;
}

export interface SoutenanceResponse {
  id: number;
  doctorant: User;
  directeur: User;
  titrethese: string;
  resumeThese?: string;
  specialite?: string;
  laboratoire?: string;
  dateSoutenance?: Date;
  lieuSoutenance?: string;
  statut: SoutenanceStatus;
  prerequis: PrerequisStatus;
  jury: JuryMember[];
  documents?: Document[];
}

export interface DefenseScheduleDTO {
  doctorantId: number;
  directeurId: number;
  titrethese: string;
  dateSoutenance: Date;
  lieuSoutenance: string;
  jury: JuryMemberRequest[];
}

export interface DefenseResponseDTO {
  id: number;
  doctorantId: number;
  directeurId: number;
  titrethese: string;
  dateSoutenance: Date;
  lieuSoutenance: string;
  statut: SoutenanceStatus;
  dateCreation: Date;
}