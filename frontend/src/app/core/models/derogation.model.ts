// ============================================
// Enums
// ============================================

export enum StatutDerogation {
  EN_ATTENTE_DIRECTEUR = 'EN_ATTENTE_DIRECTEUR',
  APPROUVE_DIRECTEUR = 'APPROUVE_DIRECTEUR',
  REJETE_DIRECTEUR = 'REJETE_DIRECTEUR',
  EN_ATTENTE_PED = 'EN_ATTENTE_PED',
  APPROUVE = 'APPROUVE',
  REJETE = 'REJETE'
}

// ============================================
// Request DTOs
// ============================================

export interface DerogationRequestDTO {
  motif: string;
  documentsJustificatifs?: File;
}

export interface DerogationValidationDTO {
  approuve: boolean;
  commentaire?: string;
}

// ============================================
// Response DTOs
// ============================================

export interface DerogationResponse {
  id: number;
  inscriptionId: number;
  motif: string;
  statut: StatutDerogation;
  dateDemande: string;
  validateurId?: number;
  commentaireValidation?: string;
  dateValidation?: string;
  hasDocuments: boolean;
}

// ============================================
// UI Models (Frontend only)
// ============================================

export interface DerogationFormData {
  motif: string;
  documentsJustificatifs?: File;
}

// ============================================
// Helper Functions
// ============================================

export function getStatutDerogationLabel(statut: StatutDerogation): string {
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

export function getStatutDerogationColor(statut: StatutDerogation): string {
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

export function getStatutDerogationIcon(statut: StatutDerogation): string {
  const icons: { [key in StatutDerogation]: string } = {
    [StatutDerogation.EN_ATTENTE_DIRECTEUR]: 'pending',
    [StatutDerogation.APPROUVE_DIRECTEUR]: 'check_circle_outline',
    [StatutDerogation.REJETE_DIRECTEUR]: 'cancel',
    [StatutDerogation.EN_ATTENTE_PED]: 'pending',
    [StatutDerogation.APPROUVE]: 'check_circle',
    [StatutDerogation.REJETE]: 'cancel'
  };
  return icons[statut];
}

export function isDerogationPending(statut: StatutDerogation): boolean {
  return [
    StatutDerogation.EN_ATTENTE_DIRECTEUR,
    StatutDerogation.APPROUVE_DIRECTEUR,
    StatutDerogation.EN_ATTENTE_PED
  ].includes(statut);
}

export function isDerogationApproved(statut: StatutDerogation): boolean {
  return statut === StatutDerogation.APPROUVE;
}

export function isDerogationRejected(statut: StatutDerogation): boolean {
  return [
    StatutDerogation.REJETE_DIRECTEUR,
    StatutDerogation.REJETE
  ].includes(statut);
}

export function canEditDerogation(statut: StatutDerogation): boolean {
  return statut === StatutDerogation.EN_ATTENTE_DIRECTEUR;
}

export function needsDirecteurValidation(statut: StatutDerogation): boolean {
  return statut === StatutDerogation.EN_ATTENTE_DIRECTEUR;
}

export function needsPEDValidation(statut: StatutDerogation): boolean {
  return [
    StatutDerogation.APPROUVE_DIRECTEUR,
    StatutDerogation.EN_ATTENTE_PED
  ].includes(statut);
}

export function getDerogationProgress(statut: StatutDerogation): number {
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

export function getDerogationSteps(): Array<{ label: string; description: string }> {
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

export function getCurrentStep(statut: StatutDerogation): number {
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

export function isDerogationRequired(dureeDoctoratMois: number): boolean {
  return dureeDoctoratMois > 36; // Plus de 3 ans
}

export function getDerogationWarningMessage(dureeDoctoratMois: number): string | null {
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

export function canSubmitDerogation(motif: string): boolean {
  return motif.trim().length >= 50; // Minimum 50 caractères
}

export function getMotifMinLength(): number {
  return 50;
}

export function getMotifMaxLength(): number {
  return 2000;
}
