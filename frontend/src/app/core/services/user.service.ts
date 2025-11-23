import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User, UserResponse } from '../models/user.model';
import { environment } from '../../environments/environment';

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
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
}