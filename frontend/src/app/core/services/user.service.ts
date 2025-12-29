import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User, UserResponse } from '../models/user.model';
import { Role, RoleName } from '../models/role.model';
import { environment } from '../../environments/environment';
import { ChangePasswordRequest } from './auth.service';

export interface CreateUserRequest {
  FirstName: string;
  LastName: string;
  email: string;
  phoneNumber: string;
  adresse: string;
  ville: string;
  pays: string;
  password: string;
  roles: RoleName[];
}

export interface UpdateUserRequest {
  FirstName: string;
  LastName: string;
  email: string;
  phoneNumber: string;
  adresse: string;
  ville: string;
  pays: string;
  roles: RoleName[];
  enabled: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly API_URL = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  /**
   * 🔹 Récupérer le profil de l'utilisateur connecté
   */
  getCurrentUser(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.API_URL}/profile`);
  }

  /**
   * 🔹 Mettre à jour le profil
   */
  updateProfile(data: Partial<UserResponse>): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.API_URL}/profile`, data);
  }

  /**
   * 🔹 Changer le mot de passe
   */
  changePassword(data: ChangePasswordRequest): Observable<any> {
    return this.http.post(`${this.API_URL}/change-password`, data);
  }

  /**
   * 🔹 Déconnexion (côté serveur)
   */
  logout(): Observable<any> {
    return this.http.post(`${this.API_URL}/logout`, {});
  }

  /**
   * 🔹 Récupérer tous les utilisateurs (ADMIN uniquement)
   */
  getAllUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.API_URL);
  }

  /**
   * 🔹 Supprimer un utilisateur (ADMIN uniquement)
   */
  deleteUser(userId: number): Observable<any> {
    return this.http.delete(`${this.API_URL}/${userId}`);
  }

  /**
   * 🔹 Récupérer tous les directeurs de thèse
   */
  getDirecteurs(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.API_URL}/directeurs`);
  }

  /**
   * 🔹 Récupérer un utilisateur par ID
   */
  getUserById(userId: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.API_URL}/${userId}`);
  }

  /**
   * 🔹 Créer un nouvel utilisateur (ADMIN uniquement)
   */
  createUser(data: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.API_URL, data);
  }

  /**
   * 🔹 Mettre à jour un utilisateur (ADMIN uniquement)
   */
  updateUser(userId: number, data: UpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.API_URL}/${userId}`, data);
  }

  /**
   * 🔹 Activer/Désactiver un utilisateur (ADMIN uniquement)
   */
  toggleUserStatus(userId: number, enabled: boolean): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.API_URL}/${userId}/status`, { enabled });
  }

  /**
   * 🔹 Récupérer tous les rôles disponibles
   */
  getAllRoles(): Observable<Role[]> {
    return this.http.get<Role[]>(`${environment.apiUrl}/roles`);
  }

  /**
   * 🔹 Rechercher des utilisateurs par critères
   */
  searchUsers(searchTerm: string, role?: RoleName): Observable<UserResponse[]> {
    let params = `?search=${encodeURIComponent(searchTerm)}`;
    if (role) {
      params += `&role=${role}`;
    }
    return this.http.get<UserResponse[]>(`${this.API_URL}/search${params}`);
  }
}