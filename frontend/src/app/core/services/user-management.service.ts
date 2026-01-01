import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface UserStatisticsResponse {
  totalUsers: number;
  activeUsers: number;
  disabledUsers?: number;
  usersByRole: { [key: string]: number };
}

export interface ConnectionStatisticsResponse {
  totalConnections: number;
  activeConnections: number;
  connectionsToday: number;
  todayLogins?: number;
  weekLogins?: number;
  monthLogins?: number;
}

export interface UserResponse {
  id: number;
  FirstName: string;
  LastName: string;
  firstName?: string;
  lastName?: string;
  email: string;
  phoneNumber?: string;
  tel?: string;
  roles: string[];
  enabled: boolean;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class UserManagementService {
  private readonly apiUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) { }

  getUserStatistics(): Observable<UserStatisticsResponse> {
    return this.http.get<UserStatisticsResponse>(`${this.apiUrl}/statistics`);
  }

  getConnectionStatistics(): Observable<ConnectionStatisticsResponse> {
    return this.http.get<ConnectionStatisticsResponse>(`${this.apiUrl}/connections/statistics`);
  }

  createUser(userData: {
    email: string;
    firstName: string;
    lastName: string;
    tel?: string;
    password: string;
    role: string;
  }): Observable<UserResponse> {
    // Backend expects /api/admin/users with specific field names
    const adminApiUrl = `${environment.apiUrl}/admin/users`;
    return this.http.post<UserResponse>(adminApiUrl, {
      email: userData.email,
      firstName: userData.firstName,
      lastName: userData.lastName,
      phoneNumber: userData.tel || '',
      password: userData.password,
      role: userData.role  // singular string, not array
    });
  }

  getAllUsers(page: number = 0, size: number = 20): Observable<UserResponse[]> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<UserResponse[]>(this.apiUrl, { params });
  }

  getUserById(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/${id}`);
  }

  updateUser(id: number, data: Partial<UserResponse>): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/${id}`, data);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  enableUser(id: number): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/${id}/enable`, {});
  }

  disableUser(id: number, reason?: string): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/${id}/disable`, { reason });
  }

  assignRole(userId: number, roleName: string): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.apiUrl}/${userId}/roles/${roleName}`, {});
  }

  removeRole(userId: number, roleName: string): Observable<UserResponse> {
    return this.http.delete<UserResponse>(`${this.apiUrl}/${userId}/roles/${roleName}`);
  }

  // Helper methods
  getFullName(user: UserResponse): string {
    return `${user.FirstName || user.firstName || ''} ${user.LastName || user.lastName || ''}`.trim();
  }

  hasRole(user: UserResponse, role: string): boolean {
    return user.roles?.some(r => r.includes(role)) || false;
  }

  isUserEnabled(user: UserResponse): boolean {
    return user.enabled;
  }

  getUserStatus(user: UserResponse): string {
    return user.enabled ? 'Actif' : 'Désactivé';
  }

  getStatusColor(user: UserResponse): string {
    return user.enabled ? 'green' : 'red';
  }

  getUserRoles(user: UserResponse): string[] {
    return user.roles || [];
  }
}
