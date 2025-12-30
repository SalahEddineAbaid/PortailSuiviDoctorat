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
  roles: string[];  // ✅ Backend renvoie Set<String> pas Role[]
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
  roles: string[];  // ✅ Backend renvoie Set<String> pas Role[]
  enabled?: boolean;
}