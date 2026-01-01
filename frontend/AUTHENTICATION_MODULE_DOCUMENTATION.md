# 🔐 Module d'Authentification Angular - Documentation Complète

## 📋 Table des matières
1. [Vue d'ensemble](#vue-densemble)
2. [Architecture](#architecture)
3. [Service d'authentification](#service-dauthentification)
4. [Composants](#composants)
5. [Guards et Intercepteurs](#guards-et-intercepteurs)
6. [Flux d'authentification](#flux-dauthentification)
7. [Gestion des tokens](#gestion-des-tokens)
8. [Validation des formulaires](#validation-des-formulaires)
9. [Gestion des erreurs](#gestion-des-erreurs)
10. [Tests](#tests)

---

## 🎯 Vue d'ensemble

Le module d'authentification fournit une solution complète pour gérer l'authentification et l'autorisation des utilisateurs dans l'application Angular. Il supporte :

- ✅ Inscription avec validation complète
- ✅ Connexion avec JWT
- ✅ Gestion des rôles (DOCTORANT, DIRECTEUR, ADMIN)
- ✅ Rafraîchissement automatique des tokens
- ✅ Profil utilisateur avec mise à jour
- ✅ Changement de mot de passe
- ✅ Réinitialisation de mot de passe (forgot password)
- ✅ Guards pour protéger les routes
- ✅ Intercepteur HTTP pour ajouter les tokens

---

## 🏗️ Architecture

### Structure des fichiers
```
frontend/src/app/
├── core/
│   ├── services/
│   │   └── auth.service.ts          # Service principal d'authentification
│   ├── guards/
│   │   ├── auth.guard.ts            # Protection des routes authentifiées
│   │   └── role.guard.ts            # Protection par rôle
│   ├── interceptors/
│   │   └── auth.interceptor.ts      # Ajout automatique des tokens
│   └── validators/
│       └── custom-validators.ts     # Validateurs personnalisés
├── features/
│   └── auth/
│       ├── login/                   # Composant de connexion
│       ├── register/                # Composant d'inscription
│       ├── profile/                 # Composant de profil
│       ├── forgot-password/         # Composant de réinitialisation
│       └── auth.routes.ts           # Routes d'authentification
└── environments/
    └── environment.ts               # Configuration API
```

---

## 🔧 Service d'authentification

### AuthService (`auth.service.ts`)

Le service central qui gère toutes les opérations d'authentification.

#### Interfaces principales

```typescript
// Requêtes
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
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

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

// Réponses
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}

export interface UserInfo {
  id: number;
  FirstName: string;
  LastName: string;
  email: string;
  phoneNumber: string;
  adresse: string;
  ville: string;
  pays: string;
  roles: string[];  // ['ROLE_DOCTORANT', 'ROLE_DIRECTEUR', 'ROLE_ADMIN']
  enabled?: boolean;
}
```

#### Méthodes principales

##### 📝 Inscription
```typescript
register(data: RegisterRequest): Observable<any>
```
- Enregistre un nouvel utilisateur
- Retourne une confirmation d'inscription
- L'utilisateur doit ensuite se connecter

##### 🔐 Connexion
```typescript
login(credentials: LoginRequest): Observable<UserInfo>
```
- Authentifie l'utilisateur
- Stocke les tokens (access + refresh)
- Charge automatiquement les informations utilisateur
- Retourne l'objet UserInfo complet

##### 🚪 Déconnexion
```typescript
logout(): void
```
- Supprime les tokens du localStorage
- Réinitialise l'état utilisateur
- Redirige vers la page de connexion

##### 🔄 Rafraîchissement du token
```typescript
refreshToken(): Observable<TokenResponse>
```
- Utilise le refresh token pour obtenir un nouveau access token
- Appelé automatiquement par l'intercepteur en cas de 401
- En cas d'échec, déconnecte l'utilisateur

##### 👤 Gestion du profil
```typescript
getCurrentUser(): Observable<UserResponse>
updateProfile(data: Partial<UserInfo>): Observable<UserResponse>
changePassword(data: ChangePasswordRequest): Observable<any>
```

##### 📧 Réinitialisation du mot de passe
```typescript
forgotPassword(data: ForgotPasswordRequest): Observable<any>
resetPassword(data: ResetPasswordRequest): Observable<any>
```

##### 🎭 Gestion des rôles
```typescript
hasRole(roleName: string): boolean
getUserRole(): string | null
isAdmin(): boolean
isDirecteur(): boolean
isDoctorant(): boolean
getDashboardRoute(): string  // Retourne la route selon le rôle
```

##### 🔍 Vérifications
```typescript
isAuthenticated(): boolean
isTokenExpired(): boolean
isTokenExpiringSoon(): boolean  // Expire dans moins de 5 minutes
```

#### Observable currentUser$

Le service expose un BehaviorSubject pour suivre l'état de l'utilisateur :

```typescript
public currentUser$: Observable<UserInfo | null>
```

Utilisation dans les composants :
```typescript
this.authService.currentUser$.subscribe(user => {
  if (user) {
    console.log('Utilisateur connecté:', user.FirstName);
  }
});
```

---

## 🎨 Composants

### 1. LoginComponent

**Fichier:** `features/auth/login/login.ts`

**Fonctionnalités:**
- Formulaire de connexion (email + password)
- Validation en temps réel
- Affichage/masquage du mot de passe
- Gestion des erreurs
- Redirection automatique selon le rôle après connexion

**Utilisation:**
```typescript
onLogin(): void {
  if (this.loginForm.valid) {
    this.authService.login(this.loginForm.value).subscribe({
      next: (user) => {
        const route = this.authService.getDashboardRoute();
        this.router.navigate([route]);
      },
      error: (error) => {
        this.errorMessage = this.getErrorMessage(error);
      }
    });
  }
}
```

### 2. RegisterComponent

**Fichier:** `features/auth/register/register.ts`

**Fonctionnalités:**
- Formulaire d'inscription complet
- Validation stricte du mot de passe (12+ caractères, majuscules, minuscules, chiffres, caractères spéciaux)
- Confirmation du mot de passe
- Validation du numéro de téléphone
- Affichage des critères de validation en temps réel

**Champs:**
- Email
- Mot de passe (avec confirmation)
- Prénom / Nom
- Téléphone
- Adresse, Ville, Pays

### 3. ProfileComponent

**Fichier:** `features/auth/profile/profile.component.ts`

**Fonctionnalités:**
- Affichage et modification du profil utilisateur
- Changement de mot de passe
- Deux formulaires séparés (profil + mot de passe)
- Validation complète
- Messages de succès/erreur

**Formulaires:**

1. **Formulaire de profil:**
   - FirstName, LastName
   - phoneNumber
   - adresse, ville, pays

2. **Formulaire de changement de mot de passe:**
   - oldPassword
   - newPassword
   - confirmPassword

### 4. ForgotPasswordComponent

**Fichier:** `features/auth/forgot-password/forgot-password.ts`

**Fonctionnalités:**
- Deux modes : demande de réinitialisation et réinitialisation avec token
- Mode "request" : saisie de l'email
- Mode "reset" : saisie du nouveau mot de passe avec token (depuis l'URL)
- Validation du mot de passe en temps réel
- Affichage des critères de sécurité

**Flux:**
1. Utilisateur saisit son email
2. Backend envoie un email avec un lien contenant un token
3. Utilisateur clique sur le lien (ex: `/forgot-password?token=xxx`)
4. Composant détecte le token et passe en mode "reset"
5. Utilisateur saisit son nouveau mot de passe
6. Redirection vers login après succès

---

## 🛡️ Guards et Intercepteurs

### AuthGuard

**Fichier:** `core/guards/auth.guard.ts`

Protège les routes nécessitant une authentification.

```typescript
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false;
};
```

**Utilisation dans les routes:**
```typescript
{
  path: 'dashboard',
  canActivate: [authGuard],
  component: DashboardComponent
}
```

### RoleGuard

**Fichier:** `core/guards/role.guard.ts`

Protège les routes selon le rôle de l'utilisateur.

```typescript
export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requiredRole = route.data['role'] as string;

  if (authService.hasRole(requiredRole)) {
    return true;
  }

  router.navigate(['/unauthorized']);
  return false;
};
```

**Utilisation:**
```typescript
{
  path: 'admin',
  canActivate: [authGuard, roleGuard],
  data: { role: 'ROLE_ADMIN' },
  component: AdminComponent
}
```

### AuthInterceptor

**Fichier:** `core/interceptors/auth.interceptor.ts`

Intercepte toutes les requêtes HTTP pour :
- Ajouter automatiquement le token JWT dans les headers
- Gérer le rafraîchissement automatique du token en cas de 401
- Gérer les erreurs d'authentification

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  // Ajouter le token si disponible
  if (token && !req.url.includes('/auth/')) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Token expiré, tenter le rafraîchissement
        return authService.refreshToken().pipe(
          switchMap(() => {
            // Réessayer la requête avec le nouveau token
            const newToken = authService.getToken();
            req = req.clone({
              setHeaders: {
                Authorization: `Bearer ${newToken}`
              }
            });
            return next(req);
          }),
          catchError(() => {
            // Échec du rafraîchissement, déconnecter
            authService.logout();
            return throwError(() => error);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
```

**Configuration dans `app.config.ts`:**
```typescript
export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(
      withInterceptors([authInterceptor])
    )
  ]
};
```

---

## 🔄 Flux d'authentification

### 1. Inscription

```
Utilisateur → RegisterComponent → AuthService.register()
                                        ↓
                                   POST /auth/register
                                        ↓
                                   Backend crée l'utilisateur
                                        ↓
                                   Confirmation
                                        ↓
                                   Redirection vers /login
```

### 2. Connexion

```
Utilisateur → LoginComponent → AuthService.login()
                                    ↓
                               POST /auth/login
                                    ↓
                               Backend retourne tokens
                                    ↓
                               Stockage dans localStorage
                                    ↓
                               GET /users/profile
                                    ↓
                               Chargement UserInfo
                                    ↓
                               currentUser$ émet l'utilisateur
                                    ↓
                               Redirection selon rôle
```

### 3. Requête authentifiée

```
Composant → HTTP Request
                ↓
           AuthInterceptor ajoute le token
                ↓
           Envoi au backend
                ↓
           ┌─ Succès (200) → Retour des données
           │
           └─ Erreur (401) → Rafraîchissement du token
                                    ↓
                               POST /auth/refresh
                                    ↓
                               ┌─ Succès → Nouveau token
                               │            ↓
                               │       Réessai de la requête
                               │
                               └─ Échec → Déconnexion
```

### 4. Réinitialisation du mot de passe

```
Utilisateur → ForgotPasswordComponent (mode request)
                    ↓
               Saisie email
                    ↓
               POST /users/forgot-password
                    ↓
               Backend envoie email avec token
                    ↓
               Utilisateur clique sur le lien
                    ↓
               ForgotPasswordComponent (mode reset)
                    ↓
               Saisie nouveau mot de passe
                    ↓
               POST /users/reset-password
                    ↓
               Succès → Redirection vers /login
```

---

## 🔑 Gestion des tokens

### Stockage

Les tokens sont stockés dans le `localStorage` :

```typescript
// Clés définies dans environment.ts
tokenKey: 'access_token'
refreshTokenKey: 'refresh_token'
```

### Access Token

- Durée de vie : 15 minutes (configurable dans le backend)
- Utilisé pour toutes les requêtes authentifiées
- Ajouté automatiquement par l'intercepteur

### Refresh Token

- Durée de vie : 7 jours (configurable dans le backend)
- Utilisé uniquement pour obtenir un nouveau access token
- Stocké séparément de l'access token

### Vérification d'expiration

```typescript
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
```

### Rafraîchissement automatique

L'intercepteur détecte les erreurs 401 et tente automatiquement de rafraîchir le token :

```typescript
if (error.status === 401) {
  return authService.refreshToken().pipe(
    switchMap(() => {
      // Réessayer avec le nouveau token
      return next(clonedRequest);
    }),
    catchError(() => {
      // Échec → déconnexion
      authService.logout();
      return throwError(() => error);
    })
  );
}
```

---

## ✅ Validation des formulaires

### CustomValidators

**Fichier:** `core/validators/custom-validators.ts`

Validateurs personnalisés pour les formulaires d'authentification.

#### Validateurs disponibles

##### 1. Mot de passe fort
```typescript
static strongPassword(control: AbstractControl): ValidationErrors | null
```
Critères :
- 12 à 64 caractères
- Au moins une minuscule
- Au moins une majuscule
- Au moins un chiffre
- Au moins un caractère spécial (@$!%*?&.)
- Pas d'espaces

##### 2. Nom
```typescript
static name(control: AbstractControl): ValidationErrors | null
```
- Lettres uniquement (avec accents)
- Espaces et tirets autorisés
- 2 à 50 caractères

##### 3. Numéro de téléphone
```typescript
static phoneNumber(control: AbstractControl): ValidationErrors | null
```
- Format international accepté
- Exemples : +33612345678, 0612345678

##### 4. Correspondance de champs
```typescript
static matchFields(field1: string, field2: string): ValidatorFn
```
Vérifie que deux champs ont la même valeur (ex: password et confirmPassword)

##### 5. Messages d'erreur
```typescript
static getErrorMessage(errors: ValidationErrors, fieldName: string): string
```
Retourne un message d'erreur lisible selon le type d'erreur.

#### Utilisation dans les formulaires

```typescript
this.registerForm = this.fb.group({
  email: ['', [Validators.required, Validators.email]],
  password: ['', [
    Validators.required,
    Validators.minLength(12),
    CustomValidators.strongPassword
  ]],
  confirmPassword: ['', [Validators.required]],
  firstName: ['', [
    Validators.required,
    Validators.minLength(2),
    CustomValidators.name
  ]],
  phoneNumber: ['', [
    Validators.required,
    CustomValidators.phoneNumber
  ]]
}, {
  validators: CustomValidators.matchFields('password', 'confirmPassword')
});
```

---

## ⚠️ Gestion des erreurs

### Codes d'erreur HTTP

Le service gère les erreurs HTTP courantes :

```typescript
private getErrorMessage(error: any): string {
  if (error.error?.message) {
    return error.error.message;
  }

  switch (error.status) {
    case 0:
      return 'Impossible de contacter le serveur. Vérifiez que le backend est démarré.';
    case 400:
      return 'Données invalides. Veuillez vérifier votre saisie.';
    case 401:
      return 'Email ou mot de passe incorrect.';
    case 403:
      return 'Accès refusé.';
    case 404:
      return 'Ressource non trouvée.';
    case 409:
      return 'Cet email est déjà utilisé.';
    case 422:
      return 'Données non valides. Veuillez corriger les erreurs.';
    case 500:
      return 'Erreur serveur. Veuillez réessayer plus tard.';
    default:
      return 'Une erreur inattendue s\'est produite.';
  }
}
```

### Affichage des erreurs dans les composants

```typescript
// Dans le template
<div *ngIf="errorMessage" class="error-message">
  {{ errorMessage }}
</div>

// Dans le composant
onLogin(): void {
  this.authService.login(credentials).subscribe({
    next: (user) => {
      // Succès
    },
    error: (error) => {
      this.errorMessage = this.getErrorMessage(error);
    }
  });
}
```

---

## 🧪 Tests

### Tests unitaires

Chaque composant dispose de son fichier de test :

- `login.spec.ts`
- `register.spec.ts`
- `profile.component.spec.ts`
- `forgot-password.spec.ts`

### Exemple de test pour AuthService

```typescript
describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should login successfully', () => {
    const mockCredentials: LoginRequest = {
      email: 'test@example.com',
      password: 'Password123!'
    };

    const mockTokenResponse: TokenResponse = {
      accessToken: 'mock-access-token',
      refreshToken: 'mock-refresh-token'
    };

    const mockUser: UserInfo = {
      id: 1,
      FirstName: 'John',
      LastName: 'Doe',
      email: 'test@example.com',
      roles: ['ROLE_DOCTORANT']
    };

    service.login(mockCredentials).subscribe(user => {
      expect(user).toEqual(mockUser);
    });

    const loginReq = httpMock.expectOne(`${service['API_URL']}/login`);
    expect(loginReq.request.method).toBe('POST');
    loginReq.flush(mockTokenResponse);

    const profileReq = httpMock.expectOne(`${service['USER_API_URL']}/profile`);
    profileReq.flush(mockUser);
  });
});
```

---

## 📝 Configuration

### Environment

**Fichier:** `environments/environment.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  tokenKey: 'access_token',
  refreshTokenKey: 'refresh_token'
};
```

### Routes

**Fichier:** `features/auth/auth.routes.ts`

```typescript
export const authRoutes: Routes = [
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'register',
    component: RegisterComponent
  },
  {
    path: 'profile',
    component: ProfileComponent,
    canActivate: [authGuard]
  },
  {
    path: 'forgot-password',
    component: ForgotPasswordComponent
  }
];
```

---

## 🚀 Utilisation

### 1. Importer le service

```typescript
import { AuthService } from './core/services/auth.service';

constructor(private authService: AuthService) {}
```

### 2. Vérifier l'authentification

```typescript
ngOnInit(): void {
  this.authService.currentUser$.subscribe(user => {
    if (user) {
      console.log('Utilisateur connecté:', user.FirstName);
    } else {
      console.log('Utilisateur non connecté');
    }
  });
}
```

### 3. Protéger une route

```typescript
{
  path: 'dashboard',
  canActivate: [authGuard],
  component: DashboardComponent
}
```

### 4. Vérifier un rôle

```typescript
ngOnInit(): void {
  if (this.authService.isAdmin()) {
    // Afficher les fonctionnalités admin
  }
}
```

---

## 🔒 Sécurité

### Bonnes pratiques implémentées

1. **Tokens JWT** : Authentification stateless
2. **Refresh tokens** : Renouvellement automatique sans redemander les credentials
3. **HttpOnly cookies** : (À implémenter côté backend pour plus de sécurité)
4. **Validation stricte** : Mots de passe forts, validation des emails
5. **Gestion des erreurs** : Messages génériques pour éviter la fuite d'informations
6. **Expiration des tokens** : Vérification automatique
7. **Déconnexion automatique** : En cas d'échec du rafraîchissement

### Recommandations supplémentaires

- Utiliser HTTPS en production
- Implémenter un rate limiting côté backend
- Ajouter un CAPTCHA sur les formulaires sensibles
- Implémenter une politique de verrouillage de compte après X tentatives
- Logger les tentatives de connexion suspectes

---

## 📚 Ressources

### Documentation Angular
- [Angular Forms](https://angular.io/guide/forms)
- [Angular HTTP Client](https://angular.io/guide/http)
- [Angular Router](https://angular.io/guide/router)

### JWT
- [JWT.io](https://jwt.io/)
- [RFC 7519](https://tools.ietf.org/html/rfc7519)

### Sécurité
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)

---

## 🎉 Conclusion

Ce module d'authentification fournit une base solide et sécurisée pour gérer l'authentification dans votre application Angular. Il est conçu pour être :

- ✅ **Complet** : Toutes les fonctionnalités d'authentification nécessaires
- ✅ **Sécurisé** : Bonnes pratiques de sécurité implémentées
- ✅ **Maintenable** : Code propre et bien structuré
- ✅ **Extensible** : Facile à adapter à vos besoins spécifiques
- ✅ **Testé** : Tests unitaires pour chaque composant

N'hésitez pas à adapter ce module selon vos besoins spécifiques !
