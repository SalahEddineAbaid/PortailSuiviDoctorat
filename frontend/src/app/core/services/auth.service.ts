import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject, tap, catchError, throwError, switchMap, of } from 'rxjs';
import { environment } from '../../environments/environment';

// ✅ Interfaces correspondant EXACTEMENT au backend
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;  // ✅ minuscule comme dans le backend
  lastName: string;   // ✅ minuscule comme dans le backend
  phoneNumber: string;
  adresse: string;
  ville: string;
  pays: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface TokenRefreshRequest {
  refreshToken: string;
}

export interface UpdateProfileRequest {
  FirstName: string;
  LastName: string;
  phoneNumber: string;
  adresse: string;
  ville: string;
  pays: string;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

// ✅ DTOs Response - SANS tokenType
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}

export interface UserInfo {
  id: number;
  FirstName: string;  // ✅ Majuscule
  LastName: string;   // ✅ Majuscule
  email: string;
  phoneNumber: string;
  adresse: string;
  ville: string;
  pays: string;
  roles: string[];  // ✅ Backend renvoie un tableau de strings ['ROLE_DOCTORANT']
  enabled?: boolean;
}

export interface UserResponse {
  id: number;
  FirstName: string;  // ✅ Majuscule
  LastName: string;   // ✅ Majuscule
  email: string;
  phoneNumber: string;
  roles: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/auth`;
  private readonly USER_API_URL = `${environment.apiUrl}/users`;
  
  private currentUserSubject = new BehaviorSubject<UserInfo | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.loadCurrentUser();
  }

  /**
   * 📝 Inscription
   */
  register(data: RegisterRequest): Observable<any> {
    console.log('📤 [AUTH SERVICE] Envoi de la requête d\'inscription:', data.email);
    
    return this.http.post(`${this.API_URL}/register`, data, {
      headers: new HttpHeaders({ 'Content-Type': 'application/json' })
    }).pipe(
      tap((response: any) => console.log('✅ [AUTH SERVICE] Inscription réussie:', response)),
      catchError((error: any) => {
        console.error('❌ [AUTH SERVICE] Erreur inscription:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * 🔐 Connexion - retourne l'utilisateur une fois chargé
   */
  login(credentials: LoginRequest): Observable<UserInfo> {
    console.log('📤 [AUTH SERVICE] Tentative de connexion pour:', credentials.email);
    
    return this.http.post<TokenResponse>(`${this.API_URL}/login`, credentials).pipe(
      tap((response: TokenResponse) => {
        console.log('✅ [AUTH SERVICE] Tokens reçus');
        console.log('🔑 Access Token:', response.accessToken.substring(0, 50) + '...');
        console.log('🔄 Refresh Token:', response.refreshToken.substring(0, 20) + '...');
        
        // ✅ Stocker les tokens de manière synchrone
        localStorage.setItem(environment.tokenKey, response.accessToken);
        localStorage.setItem(environment.refreshTokenKey, response.refreshToken);
        
        // Vérifier que le token est bien stocké
        const storedToken = localStorage.getItem(environment.tokenKey);
        console.log('💾 [AUTH SERVICE] Token stocké:', !!storedToken);
        console.log('💾 [AUTH SERVICE] Token vérifié:', storedToken?.substring(0, 50) + '...');
      }),
      // ✅ Après stockage des tokens, charger l'utilisateur
      switchMap(() => {
        console.log('👤 [AUTH SERVICE] Chargement des infos utilisateur...');
        const token = localStorage.getItem(environment.tokenKey);
        console.log('🔑 [AUTH SERVICE] Token pour requête profile:', token?.substring(0, 50) + '...');
        
        // Faire la requête avec le token explicitement dans les headers
        return this.http.get<UserInfo>(`${this.USER_API_URL}/profile`, {
          headers: new HttpHeaders({
            'Authorization': `Bearer ${token}`
          })
        });
      }),
      tap((user: UserInfo) => {
        console.log('✅ [AUTH SERVICE] Utilisateur chargé:', user);
        console.log('👤 Nom:', user.FirstName, user.LastName);
        console.log('📧 Email:', user.email);
        console.log('🎭 Rôles:', user.roles);
        this.currentUserSubject.next(user);
      }),
      catchError((error: any) => {
        console.error('❌ [AUTH SERVICE] Erreur connexion:', error);
        console.error('Status:', error.status);
        console.error('Message:', error.error);
        return throwError(() => error);
      })
    );
  }

  /**
   * 🔄 Rafraîchir le token
   */
  refreshToken(): Observable<TokenResponse> {
    const refreshToken = this.getRefreshToken();
    
    console.log('🔄 [AUTH SERVICE] Tentative de rafraîchissement du token...');
    
    if (!refreshToken) {
      console.error('❌ [AUTH SERVICE] Aucun refresh token disponible');
      return throwError(() => new Error('Aucun refresh token disponible'));
    }

    return this.http.post<TokenResponse>(`${this.API_URL}/refresh`, {
      refreshToken
    }).pipe(
      tap((response: TokenResponse) => {
        console.log('✅ [AUTH SERVICE] Token rafraîchi avec succès');
        this.setTokens(response.accessToken, response.refreshToken);
      }),
      catchError((error: any) => {
        console.error('❌ [AUTH SERVICE] Erreur rafraîchissement token:', error);
        this.logout();
        return throwError(() => error);
      })
    );
  }

  /**
   * 🚪 Déconnexion
   */
  logout(): void {
    console.log('🚪 [AUTH SERVICE] Déconnexion de l\'utilisateur');
    localStorage.removeItem(environment.tokenKey);
    localStorage.removeItem(environment.refreshTokenKey);
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  /**
   * 👤 Charger les infos de l'utilisateur connecté
   */
  private loadCurrentUser(): void {
    const token = this.getToken();
    
    console.log('🔍 [AUTH SERVICE] Vérification du token...');
    console.log('🔑 Token présent:', !!token);
    
    if (!token) {
      console.warn('⚠️ [AUTH SERVICE] Aucun token trouvé');
      this.currentUserSubject.next(null);
      return;
    }

    console.log('📤 [AUTH SERVICE] Requête GET /users/profile');
    console.log('🌐 URL:', `${this.USER_API_URL}/profile`);

    this.http.get<UserInfo>(`${this.USER_API_URL}/profile`).pipe(
      catchError((error: any) => {
        console.error('❌ [AUTH SERVICE] Erreur chargement utilisateur:', error);
        console.error('Status:', error.status);
        console.error('Message:', error.error);
        this.currentUserSubject.next(null);
        return throwError(() => error);
      })
    ).subscribe((user: UserInfo) => {
      console.log('✅ [AUTH SERVICE] Utilisateur chargé:', user);
      console.log('👤 Nom:', user.FirstName, user.LastName);
      console.log('📧 Email:', user.email);
      console.log('🎭 Rôles:', user.roles);
      this.currentUserSubject.next(user);
    });
  }

  /**
   * 📦 Gestion des tokens
   */
  private setTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem(environment.tokenKey, accessToken);
    localStorage.setItem(environment.refreshTokenKey, refreshToken);
  }

  getToken(): string | null {
    return localStorage.getItem(environment.tokenKey);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(environment.refreshTokenKey);
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    const isAuth = !!token;
    console.log('🔐 [AUTH SERVICE] isAuthenticated:', isAuth);
    return isAuth;
  }

  getCurrentUser(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.USER_API_URL}/profile`);
  }

  /**
   * 🎯 Obtenir la route du dashboard selon le rôle
   */
  getDashboardRoute(): string {
    const user = this.currentUserSubject.value;
    
    console.log('🎯 [AUTH SERVICE] Détermination de la route du dashboard...');
    console.log('👤 Utilisateur:', user);
    
    if (!user || !user.roles || user.roles.length === 0) {
      console.warn('⚠️ [AUTH SERVICE] Aucun rôle trouvé, redirection vers /login');
      return '/login';
    }

    // ✅ Le backend renvoie les rôles comme strings directement
    const role = user.roles[0];
    console.log('🎭 [AUTH SERVICE] Rôle détecté:', role);

    switch (role) {
      case 'ROLE_DOCTORANT':
        console.log('✅ [AUTH SERVICE] Route: /dashboard/doctorant');
        return '/dashboard/doctorant';
      case 'ROLE_DIRECTEUR':
        console.log('✅ [AUTH SERVICE] Route: /dashboard/directeur');
        return '/dashboard/directeur';
      case 'ROLE_ADMIN':
        console.log('✅ [AUTH SERVICE] Route: /dashboard/admin');
        return '/dashboard/admin';
      default:
        console.warn('⚠️ [AUTH SERVICE] Rôle inconnu:', role);
        return '/login';
    }
  }

  /**
   * ✅ Vérifier si l'utilisateur a un rôle spécifique
   */
  hasRole(roleName: string): boolean {
    const user = this.currentUserSubject.value;
    // ✅ Les rôles sont des strings directement
    const hasRole = user?.roles?.includes(roleName) || false;
    console.log(`🔍 [AUTH SERVICE] hasRole(${roleName}):`, hasRole);
    return hasRole;
  }

  /**
   * 🎯 Obtenir le rôle principal de l'utilisateur
   */
  getUserRole(): string | null {
    const user = this.currentUserSubject.value;
    
    if (!user || !user.roles || user.roles.length === 0) {
      return null;
    }

    // ✅ Les rôles sont des strings directement
    return user.roles[0];
  }

  /**
   * ✅ Vérifier si l'utilisateur est admin
   */
  isAdmin(): boolean {
    return this.hasRole('ROLE_ADMIN');
  }

  /**
   * ✅ Vérifier si l'utilisateur est directeur
   */
  isDirecteur(): boolean {
    return this.hasRole('ROLE_DIRECTEUR');
  }

  /**
   * ✅ Vérifier si l'utilisateur est doctorant
   */
  isDoctorant(): boolean {
    return this.hasRole('ROLE_DOCTORANT');
  }

  /**
   * 🔍 Check if token is expired
   */
  isTokenExpired(): boolean {
    const token = this.getToken();
    if (!token) return true;

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const now = Math.floor(Date.now() / 1000);
      return payload.exp <= now;
    } catch {
      return true;
    }
  }

  /**
   * 🔍 Check if token expires soon (within 5 minutes)
   */
  isTokenExpiringSoon(): boolean {
    const token = this.getToken();
    if (!token) return true;

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const now = Math.floor(Date.now() / 1000);
      const fiveMinutes = 5 * 60;
      return payload.exp <= (now + fiveMinutes);
    } catch {
      return true;
    }
  }

  /**
   * 👤 Mettre à jour le profil utilisateur
   */
  updateProfile(data: Partial<UserInfo>): Observable<UserResponse> {
    console.log('📤 [AUTH SERVICE] Mise à jour du profil utilisateur');
    
    return this.http.put<UserResponse>(`${this.USER_API_URL}/profile`, data).pipe(
      tap((response: UserResponse) => {
        console.log('✅ [AUTH SERVICE] Profil mis à jour:', response);
        // Recharger les informations utilisateur
        this.loadCurrentUser();
      }),
      catchError((error: any) => {
        console.error('❌ [AUTH SERVICE] Erreur mise à jour profil:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * 🔐 Changer le mot de passe
   */
  changePassword(data: ChangePasswordRequest): Observable<any> {
    console.log('📤 [AUTH SERVICE] Changement de mot de passe');
    
    return this.http.post(`${this.USER_API_URL}/change-password`, data).pipe(
      tap(() => {
        console.log('✅ [AUTH SERVICE] Mot de passe changé avec succès');
      }),
      catchError((error: any) => {
        console.error('❌ [AUTH SERVICE] Erreur changement mot de passe:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * 📧 Demander la réinitialisation du mot de passe
   */
  forgotPassword(data: ForgotPasswordRequest): Observable<any> {
    console.log('📤 [AUTH SERVICE] Demande de réinitialisation mot de passe:', data.email);
    
    return this.http.post(`${this.USER_API_URL}/forgot-password`, data).pipe(
      tap((response: any) => {
        console.log('✅ [AUTH SERVICE] Email de réinitialisation envoyé');
      }),
      catchError((error: any) => {
        console.error('❌ [AUTH SERVICE] Erreur demande réinitialisation:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * 🔄 Réinitialiser le mot de passe avec token
   */
  resetPassword(data: ResetPasswordRequest): Observable<any> {
    console.log('📤 [AUTH SERVICE] Réinitialisation mot de passe avec token');
    
    return this.http.post(`${this.USER_API_URL}/reset-password`, data).pipe(
      tap((response: any) => {
        console.log('✅ [AUTH SERVICE] Mot de passe réinitialisé');
      }),
      catchError((error: any) => {
        console.error('❌ [AUTH SERVICE] Erreur réinitialisation mot de passe:', error);
        return throwError(() => error);
      })
    );
  }
}