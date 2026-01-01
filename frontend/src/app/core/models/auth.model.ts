/**
 * Modèles d'authentification basés sur l'API user-service
 */

// ============================================
// REQUEST MODELS
// ============================================

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  adresse: string;
  ville: string;
  pays: string;
  roles?: string[];
}

export interface TokenRefreshRequest {
  refreshToken: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

// ============================================
// RESPONSE MODELS
// ============================================

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}

export interface UserResponse {
  id: number;
  email: string;
  FirstName: string;
  LastName: string;
  phoneNumber: string;
  adresse: string;
  ville: string;
  pays: string;
  roles: string[];
}

export interface ErrorResponse {
  error?: string;
  message?: string;
  status?: number;
  timestamp?: string;
}

// ============================================
// JWT PAYLOAD
// ============================================

export interface JwtPayload {
  userId: number;
  email: string;
  roles: string[];
  sub: string;
  iat: number;
  exp: number;
}

// ============================================
// AUTH STATE
// ============================================

export interface AuthState {
  isAuthenticated: boolean;
  user: UserResponse | null;
  accessToken: string | null;
  refreshToken: string | null;
  loading: boolean;
  error: string | null;
}

// ============================================
// ROLE ENUM
// ============================================

export enum RoleName {
  ROLE_DOCTORANT = 'ROLE_DOCTORANT',
  ROLE_DIRECTEUR = 'ROLE_DIRECTEUR',
  ROLE_ADMIN = 'ROLE_ADMIN',
  ROLE_DOCTORANT_ACTIF = 'ROLE_DOCTORANT_ACTIF'
}

// ============================================
// HELPER TYPES
// ============================================

export type AuthRole = 'ROLE_DOCTORANT' | 'ROLE_DIRECTEUR' | 'ROLE_ADMIN' | 'ROLE_DOCTORANT_ACTIF';
