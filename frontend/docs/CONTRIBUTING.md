# Guide de Contribution - Portail de Suivi du Doctorat Frontend

Merci de votre intérêt pour contribuer au Portail de Suivi du Doctorat ! Ce guide vous aidera à comprendre notre processus de développement et nos standards de qualité.

## 📋 Table des Matières

- [Prérequis](#prérequis)
- [Configuration de l'environnement](#configuration-de-lenvironnement)
- [Standards de code](#standards-de-code)
- [Workflow de développement](#workflow-de-développement)
- [Tests](#tests)
- [Documentation](#documentation)
- [Pull Requests](#pull-requests)
- [Signalement de bugs](#signalement-de-bugs)

## 🛠️ Prérequis

### Outils requis

- **Node.js** : Version 18.x ou supérieure
- **npm** : Version 9.x ou supérieure
- **Git** : Version 2.x ou supérieure
- **Angular CLI** : Version 20.x
- **IDE recommandé** : Visual Studio Code avec les extensions Angular

### Extensions VS Code recommandées

```json
{
  "recommendations": [
    "angular.ng-template",
    "ms-vscode.vscode-typescript-next",
    "esbenp.prettier-vscode",
    "ms-vscode.vscode-eslint",
    "bradlc.vscode-tailwindcss",
    "angular.ng-template"
  ]
}
```

## ⚙️ Configuration de l'environnement

### 1. Fork et clone du projet

```bash
# Fork le projet sur GitHub, puis clonez votre fork
git clone https://github.com/VOTRE-USERNAME/portail-doctorat-frontend.git
cd portail-doctorat-frontend/frontend

# Ajoutez le repository original comme remote
git remote add upstream https://github.com/ORIGINAL-OWNER/portail-doctorat-frontend.git
```

### 2. Installation des dépendances

```bash
npm install
```

### 3. Configuration des hooks Git

```bash
# Installation des hooks pre-commit
npm run prepare
```

### 4. Vérification de l'installation

```bash
# Lancer les tests
npm test

# Vérifier le linting
npm run lint

# Démarrer l'application
npm start
```

## 📝 Standards de Code

### TypeScript

#### Configuration stricte

```typescript
// tsconfig.json - Configuration stricte activée
{
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "noImplicitReturns": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true
  }
}
```

#### Conventions de nommage

```typescript
// ✅ Bon
export class UserService { }
export interface UserData { }
export enum UserStatus { }
export const API_ENDPOINTS = { };

// ❌ Mauvais
export class userService { }
export interface userData { }
export enum userStatus { }
export const api_endpoints = { };
```

#### Types et interfaces

```typescript
// ✅ Préférer les interfaces pour les objets
interface User {
  id: number;
  name: string;
  email: string;
}

// ✅ Utiliser des types pour les unions et primitives
type Status = 'active' | 'inactive' | 'pending';
type ID = string | number;

// ✅ Typage strict des méthodes
getUserById(id: number): Observable<User | null> {
  return this.http.get<User>(`/users/${id}`);
}
```

### Angular

#### Structure des composants

```typescript
// ✅ Structure recommandée
@Component({
  selector: 'app-user-profile',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-profile.component.html',
  styleUrl: './user-profile.component.scss'
})
export class UserProfileComponent implements OnInit, OnDestroy {
  // 1. Propriétés publiques
  user: User | null = null;
  isLoading = false;
  
  // 2. Propriétés privées
  private destroy$ = new Subject<void>();
  
  // 3. Constructor avec injection de dépendances
  constructor(
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}
  
  // 4. Lifecycle hooks
  ngOnInit(): void {
    this.loadUser();
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  // 5. Méthodes publiques
  onSave(): void {
    // Implementation
  }
  
  // 6. Méthodes privées
  private loadUser(): void {
    // Implementation
  }
}
```

#### Gestion des souscriptions

```typescript
// ✅ Utiliser takeUntil pour éviter les fuites mémoire
export class MyComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  ngOnInit(): void {
    this.dataService.getData()
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => this.handleData(data));
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}

// ✅ Ou utiliser async pipe dans le template
@Component({
  template: `
    <div *ngFor="let item of items$ | async">
      {{ item.name }}
    </div>
  `
})
export class MyComponent {
  items$ = this.dataService.getItems();
}
```

### SCSS/CSS

#### Architecture SCSS

```scss
// Variables globales
:root {
  --primary-color: #1976d2;
  --secondary-color: #424242;
  --success-color: #4caf50;
  --error-color: #f44336;
  
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
}

// Mixins utilitaires
@mixin flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

@mixin responsive($breakpoint) {
  @if $breakpoint == mobile {
    @media (max-width: 767px) { @content; }
  }
  @if $breakpoint == tablet {
    @media (min-width: 768px) and (max-width: 1023px) { @content; }
  }
  @if $breakpoint == desktop {
    @media (min-width: 1024px) { @content; }
  }
}
```

#### Conventions CSS

```scss
// ✅ Utiliser BEM pour les classes
.user-profile {
  &__header {
    padding: var(--spacing-md);
    
    &--highlighted {
      background-color: var(--primary-color);
    }
  }
  
  &__content {
    margin-top: var(--spacing-lg);
  }
}

// ✅ Éviter les sélecteurs trop spécifiques
.button {
  padding: var(--spacing-sm) var(--spacing-md);
  
  &--primary {
    background-color: var(--primary-color);
  }
}
```

### Gestion des erreurs

```typescript
// ✅ Gestion d'erreur avec types spécifiques
interface ApiError {
  message: string;
  code: string;
  details?: any;
}

// ✅ Service avec gestion d'erreur
@Injectable()
export class UserService {
  getUser(id: number): Observable<User> {
    return this.http.get<User>(`/users/${id}`).pipe(
      catchError((error: HttpErrorResponse) => {
        const apiError: ApiError = {
          message: error.error?.message || 'Erreur inconnue',
          code: error.status.toString(),
          details: error.error
        };
        
        this.logError(apiError);
        return throwError(() => apiError);
      })
    );
  }
  
  private logError(error: ApiError): void {
    console.error('API Error:', error);
    // Envoyer vers service de monitoring
  }
}
```

## 🔄 Workflow de Développement

### 1. Création d'une branche

```bash
# Synchroniser avec upstream
git fetch upstream
git checkout main
git merge upstream/main

# Créer une nouvelle branche
git checkout -b feature/nom-de-la-feature
# ou
git checkout -b fix/nom-du-bug
# ou
git checkout -b docs/nom-de-la-doc
```

### 2. Développement

```bash
# Faire vos modifications
# Tester régulièrement
npm test

# Vérifier le linting
npm run lint

# Corriger automatiquement si possible
npm run lint:fix
```

### 3. Commits

Nous utilisons [Conventional Commits](https://www.conventionalcommits.org/) :

```bash
# Types de commits
feat: nouvelle fonctionnalité
fix: correction de bug
docs: documentation
style: formatage, point-virgules manquants, etc.
refactor: refactoring du code
test: ajout ou modification de tests
chore: maintenance, configuration

# Exemples
git commit -m "feat(auth): add JWT token refresh mechanism"
git commit -m "fix(inscription): resolve form validation issue"
git commit -m "docs(api): update service documentation"
```

### 4. Push et Pull Request

```bash
# Push vers votre fork
git push origin feature/nom-de-la-feature

# Créer une Pull Request sur GitHub
```

## 🧪 Tests

### Standards de tests

#### Tests unitaires

```typescript
describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;
  
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [UserService]
    });
    
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });
  
  afterEach(() => {
    httpMock.verify();
  });
  
  describe('getUser', () => {
    it('should return user data when API call succeeds', () => {
      // Arrange
      const mockUser: User = { id: 1, name: 'John Doe', email: 'john@example.com' };
      
      // Act
      service.getUser(1).subscribe(user => {
        // Assert
        expect(user).toEqual(mockUser);
      });
      
      // Assert HTTP call
      const req = httpMock.expectOne('/api/users/1');
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);
    });
    
    it('should handle API errors gracefully', () => {
      // Act
      service.getUser(1).subscribe({
        next: () => fail('Should have failed'),
        error: (error: ApiError) => {
          // Assert
          expect(error.message).toBe('User not found');
          expect(error.code).toBe('404');
        }
      });
      
      // Simulate error
      const req = httpMock.expectOne('/api/users/1');
      req.flush({ message: 'User not found' }, { status: 404, statusText: 'Not Found' });
    });
  });
});
```

#### Tests de composants

```typescript
describe('UserProfileComponent', () => {
  let component: UserProfileComponent;
  let fixture: ComponentFixture<UserProfileComponent>;
  let userService: jasmine.SpyObj<UserService>;
  
  beforeEach(async () => {
    const userServiceSpy = jasmine.createSpyObj('UserService', ['getUser', 'updateUser']);
    
    await TestBed.configureTestingModule({
      imports: [UserProfileComponent],
      providers: [
        { provide: UserService, useValue: userServiceSpy }
      ]
    }).compileComponents();
    
    fixture = TestBed.createComponent(UserProfileComponent);
    component = fixture.componentInstance;
    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
  });
  
  it('should create', () => {
    expect(component).toBeTruthy();
  });
  
  it('should load user data on init', () => {
    // Arrange
    const mockUser: User = { id: 1, name: 'John Doe', email: 'john@example.com' };
    userService.getUser.and.returnValue(of(mockUser));
    
    // Act
    component.ngOnInit();
    
    // Assert
    expect(userService.getUser).toHaveBeenCalledWith(1);
    expect(component.user).toEqual(mockUser);
  });
  
  it('should display user name in template', () => {
    // Arrange
    const mockUser: User = { id: 1, name: 'John Doe', email: 'john@example.com' };
    component.user = mockUser;
    
    // Act
    fixture.detectChanges();
    
    // Assert
    const nameElement = fixture.debugElement.query(By.css('.user-name'));
    expect(nameElement.nativeElement.textContent).toContain('John Doe');
  });
});
```

### Couverture de code

Objectifs de couverture :
- **Statements** : 80%
- **Branches** : 75%
- **Functions** : 80%
- **Lines** : 80%

```bash
# Générer le rapport de couverture
npm run test:coverage

# Voir le rapport dans le navigateur
open coverage/index.html
```

## 📚 Documentation

### Documentation du code

```typescript
/**
 * Service pour la gestion des utilisateurs
 * 
 * @example
 * ```typescript
 * constructor(private userService: UserService) {}
 * 
 * ngOnInit() {
 *   this.userService.getUser(1).subscribe(user => {
 *     console.log(user);
 *   });
 * }
 * ```
 */
@Injectable({
  providedIn: 'root'
})
export class UserService {
  
  /**
   * Récupère un utilisateur par son ID
   * 
   * @param id - L'identifiant unique de l'utilisateur
   * @returns Observable contenant les données utilisateur ou null si non trouvé
   * 
   * @throws {ApiError} Quand l'utilisateur n'existe pas ou en cas d'erreur serveur
   */
  getUser(id: number): Observable<User | null> {
    return this.http.get<User>(`/api/users/${id}`);
  }
}
```

### README des composants

Chaque composant complexe doit avoir sa documentation :

```markdown
# UserProfileComponent

## Description
Composant pour afficher et éditer le profil utilisateur.

## Inputs
- `userId: number` - ID de l'utilisateur à afficher
- `readonly: boolean` - Mode lecture seule (défaut: false)

## Outputs
- `userUpdated: EventEmitter<User>` - Émis quand l'utilisateur est mis à jour

## Exemple d'utilisation
```html
<app-user-profile 
  [userId]="currentUserId"
  [readonly]="!canEdit"
  (userUpdated)="onUserUpdated($event)">
</app-user-profile>
```
```

## 🔍 Pull Requests

### Checklist avant soumission

- [ ] Le code respecte les standards de style
- [ ] Tous les tests passent
- [ ] La couverture de code est maintenue
- [ ] La documentation est à jour
- [ ] Les commits suivent la convention
- [ ] Pas de conflits avec la branche main
- [ ] Les changements sont testés manuellement

### Template de Pull Request

```markdown
## Description
Brève description des changements apportés.

## Type de changement
- [ ] Bug fix (changement qui corrige un problème)
- [ ] Nouvelle fonctionnalité (changement qui ajoute une fonctionnalité)
- [ ] Breaking change (changement qui casse la compatibilité)
- [ ] Documentation (changement de documentation uniquement)

## Tests
- [ ] Tests unitaires ajoutés/mis à jour
- [ ] Tests d'intégration ajoutés/mis à jour
- [ ] Tests manuels effectués

## Captures d'écran (si applicable)
Ajoutez des captures d'écran pour illustrer les changements visuels.

## Checklist
- [ ] Mon code suit les standards du projet
- [ ] J'ai effectué une auto-review de mon code
- [ ] J'ai commenté les parties complexes
- [ ] J'ai mis à jour la documentation
- [ ] Mes changements ne génèrent pas de nouveaux warnings
- [ ] Tous les tests passent
```

### Processus de review

1. **Review automatique** : GitHub Actions vérifie le build et les tests
2. **Review par les pairs** : Au moins 1 approbation requise
3. **Review du mainteneur** : Vérification finale avant merge

## 🐛 Signalement de Bugs

### Template d'issue

```markdown
## Description du bug
Description claire et concise du problème.

## Étapes pour reproduire
1. Aller à '...'
2. Cliquer sur '...'
3. Faire défiler jusqu'à '...'
4. Voir l'erreur

## Comportement attendu
Description de ce qui devrait se passer.

## Captures d'écran
Si applicable, ajoutez des captures d'écran.

## Environnement
- OS: [ex: Windows 10]
- Navigateur: [ex: Chrome 91]
- Version Node.js: [ex: 18.15.0]
- Version Angular: [ex: 20.0.0]

## Informations supplémentaires
Tout autre contexte utile pour le problème.
```

## 🎯 Bonnes Pratiques

### Performance

```typescript
// ✅ Utiliser OnPush quand possible
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush
})

// ✅ Utiliser trackBy pour les listes
trackByUserId(index: number, user: User): number {
  return user.id;
}

// ✅ Lazy loading des modules
const routes: Routes = [
  {
    path: 'users',
    loadChildren: () => import('./users/users.module').then(m => m.UsersModule)
  }
];
```

### Sécurité

```typescript
// ✅ Sanitiser les entrées utilisateur
@Component({
  template: `<div [innerHTML]="sanitizedContent"></div>`
})
export class SafeComponent {
  get sanitizedContent(): SafeHtml {
    return this.sanitizer.sanitize(SecurityContext.HTML, this.rawContent) || '';
  }
}

// ✅ Valider les données côté client ET serveur
const userForm = this.fb.group({
  email: ['', [Validators.required, Validators.email]],
  password: ['', [Validators.required, Validators.minLength(8)]]
});
```

### Accessibilité

```html
<!-- ✅ Utiliser les attributs ARIA -->
<button 
  [attr.aria-expanded]="isExpanded"
  [attr.aria-controls]="menuId"
  (click)="toggleMenu()">
  Menu
</button>

<!-- ✅ Labels pour les formulaires -->
<label for="email">Email</label>
<input id="email" type="email" formControlName="email">

<!-- ✅ Texte alternatif pour les images -->
<img [src]="user.avatar" [alt]="user.name + ' avatar'">
```

## 🤝 Communauté

### Communication

- **Issues GitHub** : Pour les bugs et demandes de fonctionnalités
- **Discussions GitHub** : Pour les questions générales
- **Pull Requests** : Pour les contributions de code

### Code de conduite

Nous nous engageons à maintenir un environnement accueillant et inclusif. Consultez notre [Code de Conduite](CODE_OF_CONDUCT.md) pour plus de détails.

---

Merci de contribuer au Portail de Suivi du Doctorat ! 🎓