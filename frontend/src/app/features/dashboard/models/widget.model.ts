/**
 * 🧩 Modèles pour les Widgets des Dashboards
 */

export interface Widget {
  id: string;
  type: WidgetType;
  title: string;
  icon?: string;
  size: WidgetSize;
  order: number;
  visible: boolean;
  refreshInterval?: number; // en secondes
  lastRefresh?: Date;
}

export enum WidgetType {
  PROGRESSION = 'progression',
  INSCRIPTIONS = 'inscriptions',
  NOTIFICATIONS = 'notifications',
  QUICK_ACTIONS = 'quick_actions',
  DOCTORANTS = 'doctorants',
  PENDING_REQUESTS = 'pending_requests',
  STATISTICS = 'statistics',
  SYSTEM_OVERVIEW = 'system_overview',
  USER_STATISTICS = 'user_statistics',
  CAMPAGNES = 'campagnes',
  ACTIVE_USERS = 'active_users',
  AUDIT_LOGS = 'audit_logs'
}

export enum WidgetSize {
  SMALL = 'small',      // 1/4 largeur
  MEDIUM = 'medium',    // 1/2 largeur
  LARGE = 'large',      // 3/4 largeur
  FULL = 'full'         // pleine largeur
}

export interface WidgetPreferences {
  userId: number;
  dashboardType: 'doctorant' | 'directeur' | 'admin';
  widgets: Widget[];
  layout: WidgetLayout;
}

export interface WidgetLayout {
  columns: number;
  rows: WidgetRow[];
}

export interface WidgetRow {
  widgets: string[]; // IDs des widgets
  height?: number;
}

export interface WidgetData {
  widgetId: string;
  data: any;
  loading: boolean;
  error?: string;
  lastUpdate: Date;
}

export interface WidgetAction {
  id: string;
  label: string;
  icon: string;
  callback: () => void;
  disabled?: boolean;
}

// ============================================
// Widgets spécifiques
// ============================================

export interface ProgressionWidgetData {
  pourcentage: number;
  etapeActuelle: string;
  etapesCompletees: number;
  etapesTotales: number;
  prochaineMilestone: string;
  dateDebutThese?: Date;
  dureeEcoulee: number;
  dureeRestante: number;
  alertes: string[];
}

export interface InscriptionsWidgetData {
  inscriptions: {
    id: number;
    anneeUniversitaire: string;
    statut: string;
    dateCreation: Date;
    actions: WidgetAction[];
  }[];
  total: number;
  enCours: number;
  validees: number;
}

export interface NotificationsWidgetData {
  notifications: {
    id: number;
    titre: string;
    message: string;
    dateCreation: Date;
    lu: boolean;
    priorite: string;
  }[];
  totalNonLues: number;
}

export interface QuickActionsWidgetData {
  actions: {
    id: string;
    label: string;
    icon: string;
    route: string;
    color: string;
    badge?: number;
  }[];
}

export interface StatisticsWidgetData {
  metrics: {
    label: string;
    value: number | string;
    icon: string;
    color: string;
    trend?: {
      direction: 'up' | 'down' | 'stable';
      percentage: number;
    };
  }[];
}

export interface ChartData {
  labels: string[];
  datasets: {
    label: string;
    data: number[];
    backgroundColor?: string | string[];
    borderColor?: string | string[];
    borderWidth?: number;
  }[];
}
