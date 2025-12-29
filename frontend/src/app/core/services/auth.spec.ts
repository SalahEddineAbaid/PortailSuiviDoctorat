import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService, LoginRequest, RegisterRequest, TokenResponse, UserInfo } from './auth.service';
import { environment } from '../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;
  const API_URL = `${environment.apiUrl}/auth`;
  const USER_API_URL = `${environment.apiUrl}/users`;

  beforeEach(() => {
    const spy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: spy }
      ]
    });
    
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    // Clear localStorage before each test
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('Authentication', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should login successfully', () => {
      const loginRequest: LoginRequest = {
        email: 'test@example.com',
        password: 'password123'
      };

      const tokenResponse: TokenResponse = {
        accessToken: 'mock-access-token',
        refreshToken: 'mock-refresh-token'
      };

      const mockUser: UserInfo = {
        id: 1,
        FirstName: 'John',
        LastName: 'Doe',
        email: 'test@example.com',
        phoneNumber: '0123456789',
        adresse: 'Test Address',
        ville: 'Test City',
        pays: 'France',
        roles: [{ id: 1, name: 'ROLE_DOCTORANT' }],
        enabled: true
      };

      service.login(loginRequest).subscribe(response => {
        expect(response).toEqual(tokenResponse);
        expect(localStorage.getItem(environment.tokenKey)).toBe('mock-access-token');
        expect(localStorage.getItem(environment.refreshTokenKey)).toBe('mock-refresh-token');
      });

      // Expect login request
      const loginReq = httpMock.expectOne(`${API_URL}/login`);
      expect(loginReq.request.method).toBe('POST');
      expect(loginReq.request.body).toEqual(loginRequest);
      loginReq.flush(tokenResponse);

      // Expect user profile request after login
      const profileReq = httpMock.expectOne(`${USER_API_URL}/profile`);
      expect(profileReq.request.method).toBe('GET');
      profileReq.flush(mockUser);
    });

    it('should register successfully', () => {
      const registerRequest: RegisterRequest = {
        email: 'newuser@example.com',
        password: 'password123',
        FirstName: 'Jane',
        LastName: 'Smith',
        phoneNumber: '0123456789',
        adresse: 'Test Address',
        ville: 'Test City',
        pays: 'France'
      };

      service.register(registerRequest).subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${API_URL}/register`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(registerRequest);
      expect(req.request.headers.get('Content-Type')).toBe('application/json');
      req.flush({ success: true });
    });

    it('should refresh token successfully', () => {
      // Set up initial refresh token
      localStorage.setItem(environment.refreshTokenKey, 'old-refresh-token');

      const tokenResponse: TokenResponse = {
        accessToken: 'new-access-token',
        refreshToken: 'new-refresh-token'
      };

      service.refreshToken().subscribe(response => {
        expect(response).toEqual(tokenResponse);
        expect(localStorage.getItem(environment.tokenKey)).toBe('new-access-token');
        expect(localStorage.getItem(environment.refreshTokenKey)).toBe('new-refresh-token');
      });

      const req = httpMock.expectOne(`${API_URL}/refresh`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken: 'old-refresh-token' });
      req.flush(tokenResponse);
    });

    it('should logout and clear tokens', () => {
      // Set up tokens
      localStorage.setItem(environment.tokenKey, 'access-token');
      localStorage.setItem(environment.refreshTokenKey, 'refresh-token');

      service.logout();

      expect(localStorage.getItem(environment.tokenKey)).toBeNull();
      expect(localStorage.getItem(environment.refreshTokenKey)).toBeNull();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('should check if user is authenticated', () => {
      expect(service.isAuthenticated()).toBe(false);

      localStorage.setItem(environment.tokenKey, 'mock-token');
      expect(service.isAuthenticated()).toBe(true);
    });
  });

  describe('User Management', () => {
    it('should load current user on service initialization', () => {
      // This test verifies the constructor behavior
      const mockUser: UserInfo = {
        id: 1,
        FirstName: 'John',
        LastName: 'Doe',
        email: 'test@example.com',
        phoneNumber: '0123456789',
        adresse: 'Test Address',
        ville: 'Test City',
        pays: 'France',
        roles: [{ id: 1, name: 'ROLE_DOCTORANT' }],
        enabled: true
      };

      // Set token to trigger user loading
      localStorage.setItem(environment.tokenKey, 'mock-token');

      // Create new service instance to trigger constructor
      const newService = TestBed.inject(AuthService);

      const req = httpMock.expectOne(`${USER_API_URL}/profile`);
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);

      newService.currentUser$.subscribe(user => {
        expect(user).toEqual(mockUser);
      });
    });

    it('should get current user', () => {
      const mockUser: UserInfo = {
        id: 1,
        FirstName: 'John',
        LastName: 'Doe',
        email: 'test@example.com',
        phoneNumber: '0123456789',
        adresse: 'Test Address',
        ville: 'Test City',
        pays: 'France',
        roles: [{ id: 1, name: 'ROLE_DOCTORANT' }],
        enabled: true
      };

      // Manually set the current user
      service['currentUserSubject'].next(mockUser);

      expect(service.getCurrentUser()).toEqual(mockUser);
    });

    it('should check user roles correctly', () => {
      const mockUser: UserInfo = {
        id: 1,
        FirstName: 'John',
        LastName: 'Doe',
        email: 'test@example.com',
        phoneNumber: '0123456789',
        adresse: 'Test Address',
        ville: 'Test City',
        pays: 'France',
        roles: [{ id: 1, name: 'ROLE_DOCTORANT' }],
        enabled: true
      };

      service['currentUserSubject'].next(mockUser);

      expect(service.hasRole('ROLE_DOCTORANT')).toBe(true);
      expect(service.hasRole('ROLE_ADMIN')).toBe(false);
      expect(service.isDoctorant()).toBe(true);
      expect(service.isAdmin()).toBe(false);
      expect(service.isDirecteur()).toBe(false);
    });

    it('should get user role', () => {
      const mockUser: UserInfo = {
        id: 1,
        FirstName: 'John',
        LastName: 'Doe',
        email: 'test@example.com',
        phoneNumber: '0123456789',
        adresse: 'Test Address',
        ville: 'Test City',
        pays: 'France',
        roles: [{ id: 1, name: 'ROLE_DIRECTEUR' }],
        enabled: true
      };

      service['currentUserSubject'].next(mockUser);

      expect(service.getUserRole()).toBe('ROLE_DIRECTEUR');
    });

    it('should return null for user role when no user', () => {
      service['currentUserSubject'].next(null);
      expect(service.getUserRole()).toBeNull();
    });
  });

  describe('Dashboard Routing', () => {
    it('should get correct dashboard route for doctorant', () => {
      const mockUser: UserInfo = {
        id: 1,
        FirstName: 'John',
        LastName: 'Doe',
        email: 'test@example.com',
        phoneNumber: '0123456789',
        adresse: 'Test Address',
        ville: 'Test City',
        pays: 'France',
        roles: [{ id: 1, name: 'ROLE_DOCTORANT' }],
        enabled: true
      };

      service['currentUserSubject'].next(mockUser);
      expect(service.getDashboardRoute()).toBe('/dashboard/doctorant');
    });

    it('should get correct dashboard route for directeur', () => {
      const mockUser: UserInfo = {
        id: 1,
        FirstName: 'Jane',
        LastName: 'Smith',
        email: 'jane@example.com',
        phoneNumber: '0123456789',
        adresse: 'Test Address',
        ville: 'Test City',
        pays: 'France',
        roles: [{ id: 2, name: 'ROLE_DIRECTEUR' }],
        enabled: true
      };

      service['currentUserSubject'].next(mockUser);
      expect(service.getDashboardRoute()).toBe('/dashboard/directeur');
    });

    it('should get correct dashboard route for admin', () => {
      const mockUser: UserInfo = {
        id: 1,
        FirstName: 'Admin',
        LastName: 'User',
        email: 'admin@example.com',
        phoneNumber: '0123456789',
        adresse: 'Test Address',
        ville: 'Test City',
        pays: 'France',
        roles: [{ id: 3, name: 'ROLE_ADMIN' }],
        enabled: true
      };

      service['currentUserSubject'].next(mockUser);
      expect(service.getDashboardRoute()).toBe('/dashboard/admin');
    });

    it('should return login route for unknown role', () => {
      const mockUser: UserInfo = {
        id: 1,
        FirstName: 'Unknown',
        LastName: 'User',
        email: 'unknown@example.com',
        phoneNumber: '0123456789',
        adresse: 'Test Address',
        ville: 'Test City',
        pays: 'France',
        roles: [{ id: 4, name: 'ROLE_UNKNOWN' }],
        enabled: true
      };

      service['currentUserSubject'].next(mockUser);
      expect(service.getDashboardRoute()).toBe('/login');
    });

    it('should return login route when no user', () => {
      service['currentUserSubject'].next(null);
      expect(service.getDashboardRoute()).toBe('/login');
    });
  });

  describe('Token Management', () => {
    it('should get and set tokens', () => {
      expect(service.getToken()).toBeNull();
      expect(service.getRefreshToken()).toBeNull();

      service['setTokens']('access-token', 'refresh-token');

      expect(service.getToken()).toBe('access-token');
      expect(service.getRefreshToken()).toBe('refresh-token');
    });

    it('should handle refresh token failure', () => {
      localStorage.setItem(environment.refreshTokenKey, 'invalid-token');

      service.refreshToken().subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(error).toBeTruthy();
          expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
        }
      });

      const req = httpMock.expectOne(`${API_URL}/refresh`);
      req.flush({ message: 'Invalid refresh token' }, { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle refresh token when no refresh token available', () => {
      service.refreshToken().subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(error.message).toBe('Aucun refresh token disponible');
        }
      });
    });
  });

  describe('Error Handling', () => {
    it('should handle login error', () => {
      const loginRequest: LoginRequest = {
        email: 'invalid@example.com',
        password: 'wrongpassword'
      };

      service.login(loginRequest).subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(error.status).toBe(401);
          expect(error.error.message).toBe('Invalid credentials');
        }
      });

      const req = httpMock.expectOne(`${API_URL}/login`);
      req.flush({ message: 'Invalid credentials' }, { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle register error', () => {
      const registerRequest: RegisterRequest = {
        email: 'existing@example.com',
        password: 'password123',
        FirstName: 'Jane',
        LastName: 'Smith',
        phoneNumber: '0123456789',
        adresse: 'Test Address',
        ville: 'Test City',
        pays: 'France'
      };

      service.register(registerRequest).subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(error.status).toBe(409);
          expect(error.error.message).toBe('Email already exists');
        }
      });

      const req = httpMock.expectOne(`${API_URL}/register`);
      req.flush({ message: 'Email already exists' }, { status: 409, statusText: 'Conflict' });
    });

    it('should handle user profile loading error', () => {
      localStorage.setItem(environment.tokenKey, 'invalid-token');

      // Create new service instance to trigger user loading
      const newService = TestBed.inject(AuthService);

      const req = httpMock.expectOne(`${USER_API_URL}/profile`);
      req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

      newService.currentUser$.subscribe(user => {
        expect(user).toBeNull();
      });
    });
  });
});
