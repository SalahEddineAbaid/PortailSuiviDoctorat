/**
 * System configuration models for parametrage functionality
 */

export interface SystemConfiguration {
  id: number;
  category: ConfigurationCategory;
  key: string;
  value: string;
  description: string;
  type: ConfigurationType;
  updatedAt: Date;
  updatedBy: number;
}

export enum ConfigurationCategory {
  SEUILS = 'SEUILS',
  DOCUMENTS = 'DOCUMENTS',
  NOTIFICATIONS = 'NOTIFICATIONS',
  GENERAL = 'GENERAL'
}

export enum ConfigurationType {
  STRING = 'STRING',
  NUMBER = 'NUMBER',
  BOOLEAN = 'BOOLEAN',
  JSON = 'JSON'
}

export interface SeuilConfiguration {
  id: number;
  nom: string;
  valeur: number;
  unite: string;
  description: string;
  category: SeuilCategory;
}

export enum SeuilCategory {
  DUREE_DOCTORAT = 'DUREE_DOCTORAT',
  PUBLICATIONS_MINIMUM = 'PUBLICATIONS_MINIMUM',
  HEURES_FORMATION = 'HEURES_FORMATION',
  TAILLE_FICHIER = 'TAILLE_FICHIER'
}

export interface DocumentTypeConfiguration {
  id: number;
  type: string;
  nom: string;
  obligatoire: boolean;
  formatAutorise: string[];
  tailleMaxMo: number;
  description: string;
  category: DocumentCategory;
}

export enum DocumentCategory {
  INSCRIPTION = 'INSCRIPTION',
  SOUTENANCE = 'SOUTENANCE',
  ADMINISTRATIF = 'ADMINISTRATIF'
}

export interface NotificationConfiguration {
  id: number;
  type: string;
  nom: string;
  template: string;
  actif: boolean;
  delaiRappel?: number;
  destinataires: NotificationDestinataire[];
}

export enum NotificationDestinataire {
  DOCTORANT = 'DOCTORANT',
  DIRECTEUR = 'DIRECTEUR',
  ADMIN = 'ADMIN'
}

export interface ParametrageRequest {
  category: ConfigurationCategory;
  configurations: SystemConfiguration[];
}

export interface ParametrageResponse {
  success: boolean;
  message: string;
  updatedConfigurations: SystemConfiguration[];
}

export interface SeuilRequest {
  seuils: SeuilConfiguration[];
}

export interface DocumentTypeRequest {
  documentTypes: DocumentTypeConfiguration[];
}

export interface NotificationConfigRequest {
  notifications: NotificationConfiguration[];
}