// ============================================
// Enums
// ============================================

export enum StatutInscription {
  BROUILLON = 'BROUILLON',
  SOUMIS = 'SOUMIS',
  EN_ATTENTE_DIRECTEUR = 'EN_ATTENTE_DIRECTEUR',
  APPROUVE_DIRECTEUR = 'APPROUVE_DIRECTEUR',
  EN_ATTENTE_ADMIN = 'EN_ATTENTE_ADMIN',
  VALIDE = 'VALIDE',
  REJETE = 'REJETE'
}

export enum TypeInscription {
  PREMIERE_INSCRIPTION = 'PREMIERE_INSCRIPTION',
  REINSCRIPTION = 'REINSCRIPTION'
}

export enum StatutValidation {
  EN_ATTENTE = 'EN_ATTENTE',
  APPROUVE = 'APPROUVE',
  REJETE = 'REJETE'
}

export enum TypeValidateur {
  DIRECTEUR = 'DIRECTEUR',
  ADMIN = 'ADMIN'
}

// ============================================
// Request DTOs
// ============================================

export interface InscriptionRequest {
  doctorantId: number;
  directeurTheseId: number;
  campagneId: number;
  sujetThese: string;
  type: TypeInscription;
  anneeInscription: number;
  
  // Informations doctorant
  cin: string;
  cne?: string;
  telephone: string;
  adresse: string;
  ville: string;
  pays: string;
  dateNaissance: string; // ISO date string
  lieuNaissance: string;
  nationalite: string;
  
  // Informations thèse
  titreThese: string;
  discipline: string;
  laboratoire: string;
  etablissementAccueil: string;
  cotutelle?: boolean;
  universitePartenaire?: string;
  paysPartenaire?: string;
  dateDebutPrevue: string; // ISO date string
}

export interface ValidationRequest {
  approuve: boolean;
  commentaire?: string;
}

// ============================================
// Response DTOs
// ============================================

export interface InfosDoctorantResponse {
  id: number;
  cin: string;
  cne?: string;
  telephone: string;
  adresse: string;
  ville: string;
  pays: string;
  dateNaissance: string;
  lieuNaissance: string;
  nationalite: string;
}

export interface InfosTheseResponse {
  id: number;
  titreThese: string;
  discipline: string;
  laboratoire: string;
  etablissementAccueil: string;
  cotutelle: boolean;
  universitePartenaire?: string;
  paysPartenaire?: string;
  dateDebutPrevue: string;
  dateDebutEffective?: string;
}

export interface ValidationResponse {
  id: number;
  typeValidateur: TypeValidateur;
  validateurId: number;
  validateurNom: string;
  statut: StatutValidation;
  commentaire?: string;
  dateValidation?: string;
}

export interface DocumentResponse {
  id: number;
  typeDocument: string;
  nomFichier: string;
  tailleFichier: number;
  mimeType: string;
  dateUpload: string;
  valide: boolean;
  commentaire?: string;
}

export interface InscriptionResponse {
  id: number;
  doctorantId: number;
  directeurTheseId: number;
  sujetThese: string;
  type: TypeInscription;
  anneeInscription: number;
  statut: StatutInscription;
  dateCreation: string;
  dateValidation?: string;
  dureeDoctorat?: number;
  derogation: boolean;
  motifDerogation?: string;
  commentaireDirecteur?: string;
  commentaireAdmin?: string;
  
  infosDoctorant: InfosDoctorantResponse;
  infosThese: InfosTheseResponse;
  documents: DocumentResponse[];
  validations: ValidationResponse[];
}

// ============================================
// Dashboard DTOs
// ============================================

export interface InscriptionCourante {
  id: number;
  anneeInscription: number;
  statut: StatutInscription;
  dateCreation: string;
  dateValidation?: string;
  campagneLibelle: string;
}

export interface InscriptionHistorique {
  id: number;
  anneeInscription: number;
  statut: StatutInscription;
  dateCreation: string;
  dateValidation?: string;
}

export interface DocumentManquant {
  typeDocument: string;
  libelle: string;
  obligatoire: boolean;
}

export interface Milestone {
  libelle: string;
  dateEcheance: string;
  statut: 'COMPLETE' | 'EN_COURS' | 'EN_RETARD' | 'A_VENIR';
  description?: string;
}

export interface AlerteInfo {
  type: string;
  niveau: 'INFO' | 'WARNING' | 'DANGER';
  message: string;
  dateCreation: string;
}

export interface DoctorantInfo {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  cin: string;
  cne?: string;
}

export interface DashboardResponse {
  doctorant: DoctorantInfo;
  inscriptionCourante?: InscriptionCourante;
  historiqueInscriptions: InscriptionHistorique[];
  documentsManquants: DocumentManquant[];
  milestones: Milestone[];
  alertes: AlerteInfo[];
  dureeDoctoratMois: number;
  progressionDoctorat: number;
  prochaineDateLimite?: string;
}

// ============================================
// Statistics DTOs
// ============================================

export interface StatistiquesDossier {
  totalInscriptions: number;
  inscriptionsValidees: number;
  inscriptionsEnAttente: number;
  inscriptionsRejetees: number;
  tauxValidation: number;
  documentsManquants: number;
}

// ============================================
// Alert DTOs
// ============================================

export interface AlerteVerificationSummary {
  totalInscriptionsVerifiees: number;
  totalAlertesGenerees: number;
  alertesParType: { [key: string]: number };
  inscriptionsBloqueees: number;
  dateVerification: string;
  dureeTraitementMs: number;
  message?: string;
}

// ============================================
// UI Models (Frontend only)
// ============================================

export interface InscriptionFormData {
  // Step 1: Informations générales
  directeurTheseId?: number;
  campagneId?: number;
  sujetThese?: string;
  
  // Step 2: Informations personnelles
  cin?: string;
  cne?: string;
  telephone?: string;
  adresse?: string;
  ville?: string;
  pays?: string;
  dateNaissance?: string;
  lieuNaissance?: string;
  nationalite?: string;
  
  // Step 3: Informations thèse
  titreThese?: string;
  discipline?: string;
  laboratoire?: string;
  etablissementAccueil?: string;
  cotutelle?: boolean;
  universitePartenaire?: string;
  paysPartenaire?: string;
  dateDebutPrevue?: string;
  
  // Step 4: Documents (handled separately)
}

export interface InscriptionListFilter {
  statut?: StatutInscription;
  annee?: number;
  campagneId?: number;
  type?: TypeInscription;
  searchTerm?: string;
}

export interface InscriptionListSort {
  field: 'dateCreation' | 'anneeInscription' | 'statut' | 'doctorantNom';
  direction: 'asc' | 'desc';
}

// ============================================
// Helper Functions
// ============================================

export function getStatutLabel(statut: StatutInscription): string {
  const labels: { [key in StatutInscription]: string } = {
    [StatutInscription.BROUILLON]: 'Brouillon',
    [StatutInscription.SOUMIS]: 'Soumis',
    [StatutInscription.EN_ATTENTE_DIRECTEUR]: 'En attente directeur',
    [StatutInscription.APPROUVE_DIRECTEUR]: 'Approuvé par directeur',
    [StatutInscription.EN_ATTENTE_ADMIN]: 'En attente administration',
    [StatutInscription.VALIDE]: 'Validé',
    [StatutInscription.REJETE]: 'Rejeté'
  };
  return labels[statut];
}

export function getStatutColor(statut: StatutInscription): string {
  const colors: { [key in StatutInscription]: string } = {
    [StatutInscription.BROUILLON]: 'gray',
    [StatutInscription.SOUMIS]: 'blue',
    [StatutInscription.EN_ATTENTE_DIRECTEUR]: 'orange',
    [StatutInscription.APPROUVE_DIRECTEUR]: 'cyan',
    [StatutInscription.EN_ATTENTE_ADMIN]: 'orange',
    [StatutInscription.VALIDE]: 'green',
    [StatutInscription.REJETE]: 'red'
  };
  return colors[statut];
}

export function getTypeInscriptionLabel(type: TypeInscription): string {
  const labels: { [key in TypeInscription]: string } = {
    [TypeInscription.PREMIERE_INSCRIPTION]: 'Première inscription',
    [TypeInscription.REINSCRIPTION]: 'Réinscription'
  };
  return labels[type];
}

export function canEditInscription(statut: StatutInscription): boolean {
  return statut === StatutInscription.BROUILLON;
}

export function canSubmitInscription(statut: StatutInscription): boolean {
  return statut === StatutInscription.BROUILLON;
}

export function canDeleteInscription(statut: StatutInscription): boolean {
  return statut === StatutInscription.BROUILLON;
}

export function isInscriptionValidated(statut: StatutInscription): boolean {
  return statut === StatutInscription.VALIDE;
}

export function isInscriptionRejected(statut: StatutInscription): boolean {
  return statut === StatutInscription.REJETE;
}

export function isInscriptionPending(statut: StatutInscription): boolean {
  return [
    StatutInscription.SOUMIS,
    StatutInscription.EN_ATTENTE_DIRECTEUR,
    StatutInscription.APPROUVE_DIRECTEUR,
    StatutInscription.EN_ATTENTE_ADMIN
  ].includes(statut);
}
