// ============================================
// Enums
// ============================================

export enum TypeCampagne {
  INSCRIPTION = 'INSCRIPTION',
  REINSCRIPTION = 'REINSCRIPTION',
  MIXTE = 'MIXTE'
}

// ============================================
// Request DTOs
// ============================================

export interface CampagneRequest {
  libelle: string;
  type: TypeCampagne;
  dateDebut: string; // ISO date string
  dateFin: string; // ISO date string
  anneeUniversitaire: number;
  active?: boolean;
}

export interface CloneCampagneRequest {
  dateDebut: string;
  dateFin: string;
}

// ============================================
// Response DTOs
// ============================================

export interface CampagneResponse {
  id: number;
  libelle: string;
  nom?: string; // Alias for libelle
  type: TypeCampagne;
  dateDebut: string;
  dateFin: string;
  active: boolean;
  anneeUniversitaire: number;
  ouverte: boolean;
  statut?: string;
  description?: string;
}

export interface StatistiquesCampagne {
  campagneId: number;
  campagneLibelle: string;
  totalInscriptions: number;
  inscriptionsValidees: number;
  inscriptionsEnAttente: number;
  inscriptionsRejetees: number;
  tauxValidation: number;
  inscriptionsParType: { [key: string]: number };
  inscriptionsParStatut: { [key: string]: number };
}

// ============================================
// UI Models (Frontend only)
// ============================================

export interface CampagneListFilter {
  type?: TypeCampagne;
  active?: boolean;
  anneeUniversitaire?: number;
  searchTerm?: string;
}

export interface CampagneFormData {
  libelle: string;
  type: TypeCampagne;
  dateDebut: Date;
  dateFin: Date;
  anneeUniversitaire: number;
  active: boolean;
}

// ============================================
// Helper Functions
// ============================================

export function getTypeCampagneLabel(type: TypeCampagne): string {
  const labels: { [key in TypeCampagne]: string } = {
    [TypeCampagne.INSCRIPTION]: 'Inscription',
    [TypeCampagne.REINSCRIPTION]: 'Réinscription',
    [TypeCampagne.MIXTE]: 'Mixte'
  };
  return labels[type];
}

export function getTypeCampagneColor(type: TypeCampagne): string {
  const colors: { [key in TypeCampagne]: string } = {
    [TypeCampagne.INSCRIPTION]: 'blue',
    [TypeCampagne.REINSCRIPTION]: 'green',
    [TypeCampagne.MIXTE]: 'purple'
  };
  return colors[type];
}

export function isCampagneActive(campagne: CampagneResponse): boolean {
  return campagne.active && campagne.ouverte;
}

export function isCampagneOuverte(campagne: CampagneResponse): boolean {
  const now = new Date();
  const dateDebut = new Date(campagne.dateDebut);
  const dateFin = new Date(campagne.dateFin);
  return now >= dateDebut && now <= dateFin;
}

export function getCampagneStatus(campagne: CampagneResponse): 'active' | 'future' | 'closed' | 'inactive' {
  if (!campagne.active) {
    return 'inactive';
  }
  
  const now = new Date();
  const dateDebut = new Date(campagne.dateDebut);
  const dateFin = new Date(campagne.dateFin);
  
  if (now < dateDebut) {
    return 'future';
  }
  
  if (now > dateFin) {
    return 'closed';
  }
  
  return 'active';
}

export function getCampagneStatusLabel(status: 'active' | 'future' | 'closed' | 'inactive'): string {
  const labels = {
    active: 'Active',
    future: 'À venir',
    closed: 'Fermée',
    inactive: 'Inactive'
  };
  return labels[status];
}

export function getCampagneStatusColor(status: 'active' | 'future' | 'closed' | 'inactive'): string {
  const colors = {
    active: 'green',
    future: 'blue',
    closed: 'gray',
    inactive: 'red'
  };
  return colors[status];
}

export function getDaysRemaining(campagne: CampagneResponse): number {
  const now = new Date();
  const dateFin = new Date(campagne.dateFin);
  const diff = dateFin.getTime() - now.getTime();
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
}

export function getDaysUntilStart(campagne: CampagneResponse): number {
  const now = new Date();
  const dateDebut = new Date(campagne.dateDebut);
  const diff = dateDebut.getTime() - now.getTime();
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
}

export function getCampagneDuration(campagne: CampagneResponse): number {
  const dateDebut = new Date(campagne.dateDebut);
  const dateFin = new Date(campagne.dateFin);
  const diff = dateFin.getTime() - dateDebut.getTime();
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
}

export function canModifyCampagne(campagne: CampagneResponse): boolean {
  const status = getCampagneStatus(campagne);
  return status === 'future' || status === 'inactive';
}

export function canCloseCampagne(campagne: CampagneResponse): boolean {
  return campagne.active && campagne.ouverte;
}

export function canCloneCampagne(campagne: CampagneResponse): boolean {
  return true; // Can always clone a campaign
}
