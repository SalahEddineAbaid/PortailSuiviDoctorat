/**
 * Structure du JWT décodé (alignée avec JwtProvider.java du backend)
 */
export interface JwtPayload {
  sub: string;           // Email (subject)
  userId: number;        // ID de l'utilisateur
  email: string;         // Email
  roles: string[];       // Rôles : ["ROLE_DOCTORANT", "ROLE_DIRECTEUR", "ROLE_ADMIN"]
  iat: number;           // Issued At (timestamp de création)
  exp: number;           // Expiration (timestamp)
}