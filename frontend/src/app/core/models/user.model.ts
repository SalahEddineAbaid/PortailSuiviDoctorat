import { Role } from './role.model';

/**
 * 🔹 Représente un utilisateur complet (aligné avec User.java du backend)
 */
export interface User {
  id: number;
  FirstName: string;      // ✅ Majuscule comme dans le backend
  LastName: string;       // ✅ Majuscule comme dans le backend
  email: string;
  phoneNumber: string;
  adresse: string;
  ville: string;
  pays: string;
  enabled: boolean;
  roles: Role[];
}

/**
 * 🔹 Réponse du backend (UserResponse.java)
 */
export interface UserResponse {
  id: number;
  FirstName: string;
  LastName: string;
  email: string;
  phoneNumber: string;
  adresse: string;
  ville: string;
  pays: string;
  roles: Role[];
  enabled: boolean;
}