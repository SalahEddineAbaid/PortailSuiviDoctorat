/**
 * Document models aligned with backend DTOs
 */

export interface Document {
  id: number;
  nom: string;
  type: DocumentType;
  taille: number;
  dateUpload: Date;
  chemin: string;
  obligatoire: boolean;
  valide?: boolean;
}

export enum DocumentType {
  // Inscription documents
  CARTE_IDENTITE = 'CARTE_IDENTITE',
  DIPLOME_MASTER = 'DIPLOME_MASTER',
  RELEVES_NOTES = 'RELEVES_NOTES',
  CV = 'CV',
  LETTRE_MOTIVATION = 'LETTRE_MOTIVATION',
  
  // Soutenance documents
  MANUSCRIT_THESE = 'MANUSCRIT_THESE',
  RESUME_THESE = 'RESUME_THESE',
  PUBLICATIONS = 'PUBLICATIONS',
  ATTESTATION_FORMATION = 'ATTESTATION_FORMATION',
  AUTORISATION_SOUTENANCE = 'AUTORISATION_SOUTENANCE'
}

export interface DocumentRequest {
  inscriptionId: number;
  type: DocumentType;
  obligatoire: boolean;
}

export interface DocumentResponse {
  id: number;
  nom: string;
  type: DocumentType;
  taille: number;
  dateUpload: Date;
  obligatoire: boolean;
  valide?: boolean;
}

export interface DocumentUploadRequest {
  file: File;
  type: DocumentType;
  obligatoire: boolean;
}