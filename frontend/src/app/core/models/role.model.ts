/**
 * Enum des rôles (aligné avec RoleName.java du backend)
 */
export enum RoleName {
  DOCTORANT = 'ROLE_DOCTORANT',
  DIRECTEUR = 'ROLE_DIRECTEUR',
  ADMIN = 'ROLE_ADMIN'
}

/**
 * Interface Role (alignée avec Role.java)
 */
export interface Role {
  id: number;
  name: RoleName;
}