/**
 * 📊 Modèles de données pour les Dashboards
 */

import { UserInfo } from '../../../core/services/auth.service';

// ============================================
// Types communs
// ============================================

export interface QuickAction {
  id: string;
  label: string;
  icon: string;
  route: string;
  color?: string;
}

export interface SystemAlert {
  id: number;
  type: 'warning' | 'error' | 'info';
  message: string;
  timestamp: Date;
  severity: 'low' | 'medium' | 'high';
}

// ============================================
// Dashboard Doctorant
// ============================================

export interface DoctorantDashboard {
  user: UserInfo;
  statistics: DoctorantStatistics;
  inscriptions: InscriptionSummary[];
  notifications: NotificationSummary[];
  quickActions: QuickAction[];
  progression: ProgressionData;
}

export interface DoctorantStatistics {
  totalInscriptions: number;
  inscriptionsEnCours: number;
  inscriptionsValidees: number;
  inscriptionsEnAttente: number;
  documentsManquants: number;
  progressionThese: number;
  anneesEcoulees: number;
  anneesRestantes: number;
}

export interface InscriptionSummary {
  id: number;
  anneeUniversitaire: string;
  statut: InscriptionStatut;
  dateCreation: Date;
  dateValidation?: Date;
  campagneId: number;
  campagneNom: string;
  documentsManquants: number;
  canEdit: boolean;
  canSubmit: boolean;
}

export enum InscriptionStatut {
  BROUILLON = 'BROUILLON',
  SOUMISE = 'SOUMISE',
  EN_ATTENTE_DIRECTEUR = 'EN_ATTENTE_DIRECTEUR',
  VALIDEE_DIRECTEUR = 'VALIDEE_DIRECTEUR',
  EN_ATTENTE_ADMIN = 'EN_ATTENTE_ADMIN',
  VALIDEE = 'VALIDEE',
  REJETEE = 'REJETEE'
}

export interface ProgressionData {
  etapeActuelle: string;
  etapesCompletees: number;
  etapesTotales: number;
  pourcentage: number;
  prochaineMilestone: string;
  dateDebutThese?: Date;
  dureeEcoulee: number; // en mois
  dureeRestante: number; // en mois
}

// ============================================
// Dashboard Directeur
// ============================================

export interface DirecteurDashboard {
  user: UserInfo;
  statistics: DirecteurStatistics;
  doctorants: DoctorantSummary[];
  demandesEnAttente: DemandeSummary[];
  notifications: NotificationSummary[];
}

export interface DirecteurStatistics {
  totalDoctorants: number;
  doctorantsActifs: number;
  demandesEnAttente: number;
  demandesValidees: number;
  demandesRejetees: number;
  tauxValidation: number;
  moyenneDelaiValidation: number; // en jours
}

export interface DoctorantSummary {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  sujetThese: string;
  dateDebutThese: Date;
  anneesEcoulees: number;
  statut: 'actif' | 'suspendu' | 'termine';
  dernierInscriptionStatut: InscriptionStatut;
  alertes: string[];
}

export interface DemandeSummary {
  id: number;
  type: 'inscription' | 'derogation' | 'document';
  doctorantNom: string;
  doctorantPrenom: string;
  dateCreation: Date;
  priorite: 'basse' | 'normale' | 'haute';
  description: string;
  actions: DemandeAction[];
}

export interface DemandeAction {
  id: string;
  label: string;
  type: 'approve' | 'reject' | 'view';
  icon: string;
}

// ============================================
// Dashboard Admin
// ============================================

export interface AdminDashboard {
  statistics: AdminStatistics;
  userStatistics: UserStatistics;
  connectionStatistics: ConnectionStatistics;
  campagnes: CampagneSummary[];
  recentAudits: AuditRecord[];
  systemAlerts: SystemAlert[];
  activeUsers: ActiveUserSummary[];
}

export interface AdminStatistics {
  totalUsers: number;
  activeUsers: number;
  disabledUsers: number;
  totalInscriptions: number;
  inscriptionsEnAttente: number;
  activeCampagnes: number;
  pendingValidations: number;
  systemHealth: 'healthy' | 'warning' | 'critical';
}

export interface UserStatistics {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  disabledUsers: number;
  usersByRole: {
    role: string;
    count: number;
  }[];
  newUsersLastMonth: number;
  newUsersLastWeek: number;
}

export interface ConnectionStatistics {
  totalConnections: number;
  uniqueUsers: number;
  averageSessionDuration: number;
  peakHours: {
    hour: number;
    connections: number;
  }[];
  connectionsByDay: {
    date: string;
    connections: number;
  }[];
  failedLoginAttempts: number;
}

export interface CampagneSummary {
  id: number;
  nom: string;
  anneeUniversitaire: string;
  dateDebut: Date;
  dateFin: Date;
  statut: 'active' | 'terminee' | 'planifiee';
  totalInscriptions: number;
  inscriptionsValidees: number;
  inscriptionsEnAttente: number;
}

export interface AuditRecord {
  id: number;
  userId: number;
  userName: string;
  action: string;
  entityType: string;
  entityId: number;
  timestamp: Date;
  ipAddress: string;
  details?: string;
}

export interface ActiveUserSummary {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  lastConnection: Date;
  status: 'online' | 'offline';
  enabled: boolean;
}

// ============================================
// Notifications
// ============================================

export interface NotificationSummary {
  id: number;
  type: NotificationType;
  titre: string;
  message: string;
  dateCreation: Date;
  lu: boolean;
  priorite: 'basse' | 'normale' | 'haute';
  lien?: string;
}

export enum NotificationType {
  INSCRIPTION = 'INSCRIPTION',
  VALIDATION = 'VALIDATION',
  DOCUMENT = 'DOCUMENT',
  ALERTE = 'ALERTE',
  SYSTEME = 'SYSTEME',
  CAMPAGNE = 'CAMPAGNE'
}

// ============================================
// Réponses API
// ============================================

export interface DashboardResponse {
  success: boolean;
  data: any;
  message?: string;
  timestamp: Date;
}

export interface StatisticsResponse {
  statistics: any;
  generatedAt: Date;
}
