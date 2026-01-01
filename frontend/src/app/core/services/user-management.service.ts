import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * User Response Interface
 */
export interface UserResponse {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
    tel?: string;
    roles: RoleResponse[];
    enabled: boolean;
    accountNonLocked: boolean;
    createdAt: Date;
}

/**
 * Role Response Interface
 */
export interface RoleResponse {
    id: number;
    name: string;
    description?: string;
}

/**
 * User Detailed Response Interface
 */
export interface UserDetailedResponse extends UserResponse {
    profileComplete: boolean;
    dateOfBirth?: Date;
    address?: string;
    city?: string;
    postalCode?: string;
    country?: string;
    nationality?: string;
    // Additional fields from backend
}

/**
 * User Statistics Response
 */
export interface UserStatisticsResponse {
    totalUsers: number;
    activeUsers: number;
    disabledUsers: number;
    usersByRole: { [key: string]: number };
    recentRegistrations: number;
}

/**
 * Connection Statistics Response
 */
export interface ConnectionStatisticsResponse {
    todayLogins: number;
    weekLogins: number;
    monthLogins: number;
    averageSessionDuration: number;
    peakHour: number;
}

/**
 * Disable Account Request
 */
export interface DisableAccountRequest {
    reason: string;
}

/**
 * Page Response (Spring Pagination)
 */
export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

/**
 * Service for User Management (Admin)
 * Integrates with user-service microservice
 * Base URL: http://localhost:8082/api
 */
@Injectable({
    providedIn: 'root'
})
export class UserManagementService {
    private readonly USER_API = `${environment.apiUrl}/api/users`;
    private readonly ADMIN_API = `${environment.apiUrl}/api/admin`;

    constructor(private http: HttpClient) { }

    // ===== USER CRUD OPERATIONS =====

    /**
     * Get all users (ADMIN only)
     * GET /api/users
     */
    getAllUsers(): Observable<UserResponse[]> {
        console.log('📤 [USER MANAGEMENT] Fetching all users');
        return this.http.get<UserResponse[]>(this.USER_API);
    }

    /**
     * Get user by ID
     * GET /api/users/{id}
     */
    getUserById(id: number): Observable<UserResponse> {
        console.log('📤 [USER MANAGEMENT] Fetching user:', id);
        return this.http.get<UserResponse>(`${this.USER_API}/${id}`);
    }

    /**
     * Get detailed user profile
     * GET /api/users/{id}/profile-complete
     */
    getDetailedProfile(id: number): Observable<UserDetailedResponse> {
        console.log('📤 [USER MANAGEMENT] Fetching detailed profile:', id);
        return this.http.get<UserDetailedResponse>(`${this.USER_API}/${id}/profile-complete`);
    }

    /**
     * Delete user (ADMIN only)
     * DELETE /api/users/{id}
     */
    deleteUser(id: number): Observable<{ message: string }> {
        console.log('📤 [USER MANAGEMENT] Deleting user:', id);
        return this.http.delete<{ message: string }>(`${this.USER_API}/${id}`);
    }

    // ===== ADMIN OPERATIONS =====

    /**
     * Disable user account (ADMIN only)
     * POST /api/admin/users/{userId}/disable
     */
    disableUser(userId: number, reason: string): Observable<void> {
        console.log('📤 [USER MANAGEMENT] Disabling user:', userId, 'Reason:', reason);
        const request: DisableAccountRequest = { reason };
        return this.http.post<void>(`${this.ADMIN_API}/users/${userId}/disable`, request);
    }

    /**
     * Enable user account (ADMIN only)
     * POST /api/admin/users/{userId}/enable
     */
    enableUser(userId: number): Observable<void> {
        console.log('📤 [USER MANAGEMENT] Enabling user:', userId);
        return this.http.post<void>(`${this.ADMIN_API}/users/${userId}/enable`, {});
    }

    /**
     * Get all disabled users (paginated)
     * GET /api/admin/users/disabled?page&size
     */
    getDisabledUsers(page: number = 0, size: number = 20): Observable<Page<UserResponse>> {
        console.log('📤 [USER MANAGEMENT] Fetching disabled users - Page:', page, 'Size:', size);
        const params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        return this.http.get<Page<UserResponse>>(`${this.ADMIN_API}/users/disabled`, { params });
    }

    // ===== STATISTICS =====

    /**
     * Get user statistics (ADMIN only)
     * GET /api/admin/statistics/users
     */
    getUserStatistics(): Observable<UserStatisticsResponse> {
        console.log('📤 [USER MANAGEMENT] Fetching user statistics');
        return this.http.get<UserStatisticsResponse>(`${this.ADMIN_API}/statistics/users`);
    }

    /**
     * Get connection statistics (ADMIN only)
     * GET /api/admin/statistics/connections
     */
    getConnectionStatistics(): Observable<ConnectionStatisticsResponse> {
        console.log('📤 [USER MANAGEMENT] Fetching connection statistics');
        return this.http.get<ConnectionStatisticsResponse>(`${this.ADMIN_API}/statistics/connections`);
    }

    // ===== HELPER METHODS =====

    /**
     * Check if user is enabled
     */
    isUserEnabled(user: UserResponse): boolean {
        return user.enabled && user.accountNonLocked;
    }

    /**
     * Get user's role names as array
     */
    getUserRoles(user: UserResponse): string[] {
        return user.roles.map(role => role.name);
    }

    /**
     * Check if user has specific role
     */
    hasRole(user: UserResponse, roleName: string): boolean {
        return user.roles.some(role => role.name === roleName || role.name === `ROLE_${roleName}`);
    }

    /**
     * Get user full name
     */
    getFullName(user: UserResponse): string {
        return `${user.firstName} ${user.lastName}`;
    }

    /**
     * Format user status for display
     */
    getUserStatus(user: UserResponse): string {
        if (!user.enabled) return 'Désactivé';
        if (!user.accountNonLocked) return 'Verrouillé';
        return 'Actif';
    }

    /**
     * Get status color for UI badges
     */
    getStatusColor(user: UserResponse): string {
        if (!user.enabled) return 'red';
        if (!user.accountNonLocked) return 'orange';
        return 'green';
    }
}
