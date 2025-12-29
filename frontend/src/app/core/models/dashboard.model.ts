/**
 * Dashboard-related models
 */

export interface DashboardStats {
  totalInscriptions: number;
  inscriptionsEnAttente: number;
  inscriptionsValidees: number;
  inscriptionsRejetees: number;
  totalSoutenances: number;
  soutenancesEnAttente: number;
  soutenancesAutorisees: number;
  // Directeur-specific fields
  totalDoctorants?: number;
  soutenancesValidees?: number;
}

export interface DoctorantDashboardData {
  inscriptionActuelle?: any; // Will be typed with Inscription
  prochaineSoutenance?: any; // Will be typed with Soutenance
  notifications: any[]; // Will be typed with Notification[]
  documentsManquants: string[];
  alertes: DashboardAlert[];
  timeline: TimelineEvent[];
}

export interface DirecteurDashboardData {
  doctorants: any[]; // Will be typed with User[]
  dossiersEnAttente: any[]; // Will be typed with Inscription[]
  soutenancesAPlanifier: any[]; // Will be typed with Soutenance[]
  statistiques: DashboardStats;
}

export interface AdminDashboardData {
  statistiques: DashboardStats;
  campagneActive?: any; // Will be typed with Campagne
  dossiersEnAttente: any[]; // Will be typed with Inscription[]
  utilisateursRecents: any[]; // Will be typed with User[]
}

export interface DashboardAlert {
  id: string;
  type: 'warning' | 'error' | 'info' | 'success';
  title: string;
  message: string;
  actionLabel?: string;
  actionRoute?: string;
  dismissible: boolean;
}

export interface TimelineEvent {
  id: string;
  date: Date;
  title: string;
  description: string;
  status: 'completed' | 'current' | 'upcoming' | 'overdue';
  type: 'inscription' | 'soutenance' | 'document' | 'validation';
}

export interface ProgressIndicator {
  current: number;
  total: number;
  label: string;
  percentage: number;
}

export interface AlertData {
  type: 'warning' | 'error' | 'info' | 'success';
  title: string;
  message: string;
}