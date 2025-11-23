import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject, tap, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';  // ✅ Corrigé le chemin

// ✅ Interfaces correspondant EXACTEMENT au backend
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  FirstName: string;  // ✅ Majuscule comme dans le backend
  LastName: string;   // ✅ Majuscule comme dans le backend
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
  password: string;
}

export interface TokenRefreshRequest {
  refreshToken: string;
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
  roles: Array<{ id: number; name: string }>;
  enabled: boolean;
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
      tap(response => console.log('✅ [AUTH SERVICE] Inscription réussie:', response)),
      catchError(error => {
        console.error('❌ [AUTH SERVICE] Erreur inscription:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * 🔐 Connexion
   */
  login(credentials: LoginRequest): Observable<TokenResponse> {
    console.log('📤 [AUTH SERVICE] Tentative de connexion pour:', credentials.email);
    
    return this.http.post<TokenResponse>(`${this.API_URL}/login`, credentials).pipe(
      tap(response => {
        console.log('✅ [AUTH SERVICE] Tokens reçus');
        console.log('🔑 Access Token:', response.accessToken.substring(0, 20) + '...');
        console.log('🔄 Refresh Token:', response.refreshToken.substring(0, 20) + '...');
        
        // ✅ Stocker les tokens
        this.setTokens(response.accessToken, response.refreshToken);
        console.log('💾 [AUTH SERVICE] Tokens stockés dans localStorage');
        
        // ✅ Charger les infos utilisateur
        console.log('👤 [AUTH SERVICE] Chargement des infos utilisateur...');
        this.loadCurrentUser();
      }),
      catchError(error => {
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
      tap(response => {
        console.log('✅ [AUTH SERVICE] Token rafraîchi avec succès');
        this.setTokens(response.accessToken, response.refreshToken);
      }),
      catchError(error => {
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

    this.http.get<UserInfo>(`${this.USER_API_URL}/profile`).pipe(  // ✅ CHANGÉ DE /me À /profile
      catchError(error => {
        console.error('❌ [AUTH SERVICE] Erreur chargement utilisateur:', error);
        console.error('Status:', error.status);
        console.error('Message:', error.error);
        this.currentUserSubject.next(null);
        return throwError(() => error);
      })
    ).subscribe(user => {
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

  getCurrentUser(): UserInfo | null {
    return this.currentUserSubject.value;
  }

  /**
   * 🎯 Obtenir la route du dashboard selon le rôle
   */
  getDashboardRoute(): string {
    const user = this.getCurrentUser();
    
    console.log('🎯 [AUTH SERVICE] Détermination de la route du dashboard...');
    console.log('👤 Utilisateur:', user);
    
    if (!user || !user.roles || user.roles.length === 0) {
      console.warn('⚠️ [AUTH SERVICE] Aucun rôle trouvé, redirection vers /login');
      return '/login';
    }

    const role = user.roles[0].name;
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
    const user = this.getCurrentUser();
    const hasRole = user?.roles?.some(role => role.name === roleName) || false;
    console.log(`🔍 [AUTH SERVICE] hasRole(${roleName}):`, hasRole);
    return hasRole;
  }

  /**
   * 🎯 Obtenir le rôle principal de l'utilisateur
   */
  getUserRole(): string | null {
    const user = this.getCurrentUser();
    
    if (!user || !user.roles || user.roles.length === 0) {
      return null;
    }

    // Retourner le nom du premier rôle
    return user.roles[0].name;
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
}