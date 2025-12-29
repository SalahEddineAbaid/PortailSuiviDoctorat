import { User } from './user.model';

/**
 * Notification models aligned with backend DTOs
 */

export interface Notification {
  id: number;
  destinataire: User;
  titre: string;
  message: string;
  type: NotificationType;
  dateCreation: Date;
  dateLecture?: Date;
  lue: boolean;
  actions?: NotificationAction[];
}

export enum NotificationType {
  INFO = 'INFO',
  WARNING = 'WARNING',
  SUCCESS = 'SUCCESS',
  ERROR = 'ERROR',
  REMINDER = 'REMINDER'
}

export interface NotificationAction {
  label: string;
  route: string;
  icon?: string;
}

export interface NotificationRequest {
  destinataireId: number;
  titre: string;
  message: string;
  type: NotificationType;
}

export interface NotificationResponse {
  id: number;
  destinataire: User;
  titre: string;
  message: string;
  type: NotificationType;
  dateCreation: Date;
  dateLecture?: Date;
  lue: boolean;
}

export interface NotificationSettings {
  emailEnabled: boolean;
  pushEnabled: boolean;
  inscriptionNotifications: boolean;
  soutenanceNotifications: boolean;
  adminNotifications: boolean;
}

export interface WebSocketMessage {
  type: 'NOTIFICATION' | 'SYSTEM' | 'ERROR';
  data: any;
  timestamp: Date;
}