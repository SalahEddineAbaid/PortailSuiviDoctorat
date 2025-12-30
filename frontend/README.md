# Portail de Suivi du Doctorat - Frontend

Une application Angular moderne pour la gestion complète du parcours doctoral, depuis l'inscription jusqu'à la soutenance.

## 🚀 Aperçu

Le Portail de Suivi du Doctorat Frontend est une application Angular 20 qui fournit une interface utilisateur intuitive et sécurisée pour tous les acteurs du processus doctoral :

- **Doctorants** : Gestion des inscriptions, suivi du parcours, demandes de soutenance
- **Directeurs de thèse** : Supervision des doctorants, validation des dossiers
- **Administrateurs** : Gestion des campagnes, validation administrative, paramétrage

## 📋 Prérequis

- **Node.js** : Version 18.x ou supérieure
- **npm** : Version 9.x ou supérieure
- **Angular CLI** : Version 20.x
- **Backend API** : Le backend Spring Boot doit être démarré sur le port 8081

## 🛠️ Installation

### 1. Cloner le projet

```bash
git clone <repository-url>
cd frontend
```

### 2. Installer les dépendances

```bash
npm install
```

### 3. Configuration de l'environnement

Vérifiez le fichier `src/environments/environment.ts` :

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081',
  wsUrl: 'ws://localhost:8081/ws',
  tokenKey: 'accessToken',
  refreshTokenKey: 'refreshToken'
};
```

### 4. Démarrer l'application

```bash
npm start
# ou
ng serve
```

L'application sera accessible sur `http://localhost:4200`

## 🏗️ Architecture

### Structure des dossiers

```
src/app/
├── core/                          # Services globaux et configuration
│   ├── guards/                    # Protection des routes
│   ├── interceptors/              # Intercepteurs HTTP
│   ├── models/                    # Interfaces TypeScript
│   ├── services/                  # Services métier
│   ├── config/                    # Configuration (CSP, etc.)
│   └── handlers/                  # Gestionnaires d'erreurs
├── shared/                        # Composants réutilisables
│   ├── components/                # Composants UI
│   ├── pipes/                     # Pipes personnalisés
│   └── directives/                # Directives personnalisées
├── features/                      # Modules fonctionnels
│   ├── auth/                      # Authentification
│   ├── dashboard/                 # Tableaux de bord
│   ├── inscription/               # Gestion des inscriptions
│   ├── soutenance/                # Processus de soutenance
│   ├── admin/                     # Administration
│   └── notifications/             # Système de notifications
└── assets/                        # Ressources statiques
```

### Modules principaux

#### Core Module
- **Services** : AuthService, CacheService, SecurityService, PerformanceService
- **Guards** : AuthGuard, RoleGuard
- **Interceptors** : AuthInterceptor, ErrorInterceptor, SecurityInterceptor

#### Feature Modules
- **Inscription** : Gestion des inscriptions et réinscriptions
- **Soutenance** : Processus de demande de soutenance
- **Admin** : Outils d'administration
- **Notifications** : Système de notifications temps réel

## 🔐 Sécurité

### Mesures de sécurité implémentées

1. **Content Security Policy (CSP)**
   - Configuration stricte pour prévenir les attaques XSS
   - Différentiation développement/production

2. **Protection CSRF**
   - Tokens CSRF pour les requêtes modifiantes
   - Validation côté serveur

3. **Authentification JWT**
   - Tokens sécurisés avec expiration
   - Refresh automatique des tokens
   - Validation de l'intégrité

4. **Validation des entrées**
   - Sanitisation des données utilisateur
   - Détection des tentatives XSS
   - Validation des fichiers uploadés

5. **Rate Limiting**
   - Limitation du nombre de requêtes par minute
   - Protection contre les attaques par déni de service

### Configuration CSP

```typescript
// Développement
'script-src': ["'self'", "'unsafe-inline'", "'unsafe-eval'"]

// Production
'script-src': ["'self'", "https://cdn.jsdelivr.net"]
```

## ⚡ Optimisations des performances

### Lazy Loading
Tous les modules features sont chargés à la demande :

```typescript
{
  path: 'inscription',
  loadChildren: () => import('./features/inscription/inscription.routes')
}
```

### Change Detection
Utilisation d'OnPush sur les composants critiques :

```typescript
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush
})
```

### Cache Service
Mise en cache des réponses API fréquentes :

```typescript
// Cache pendant 5 minutes
this.cacheService.cacheObservable('key', observable, 5 * 60 * 1000)
```

### Bundle Optimization
- Tree shaking automatique
- Code splitting par route
- Préchargement des modules critiques

## 🧪 Tests

### Lancer les tests

```bash
# Tests unitaires
npm test

# Tests avec couverture
npm run test:coverage

# Tests en mode watch
npm run test:watch

# Tests e2e
npm run e2e
```

### Structure des tests

```
src/app/
├── core/services/*.spec.ts        # Tests des services
├── shared/components/*.spec.ts    # Tests des composants partagés
└── features/**/*.spec.ts          # Tests des modules features
```

### Couverture de code

Objectif de couverture : 70% minimum
- Branches : 70%
- Fonctions : 70%
- Lignes : 70%
- Statements : 70%

## 🚀 Déploiement

### Build de production

```bash
npm run build:prod
```

### Variables d'environnement

Créer `src/environments/environment.prod.ts` :

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://your-api-domain.com',
  wsUrl: 'wss://your-api-domain.com/ws',
  tokenKey: 'accessToken',
  refreshTokenKey: 'refreshToken'
};
```

### Configuration serveur web

#### Nginx

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /path/to/dist;
    index index.html;

    # Gestion des routes Angular
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Headers de sécurité
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
}
```

#### Apache

```apache
<VirtualHost *:80>
    ServerName your-domain.com
    DocumentRoot /path/to/dist
    
    # Gestion des routes Angular
    <Directory "/path/to/dist">
        RewriteEngine On
        RewriteBase /
        RewriteRule ^index\.html$ - [L]
        RewriteCond %{REQUEST_FILENAME} !-f
        RewriteCond %{REQUEST_FILENAME} !-d
        RewriteRule . /index.html [L]
    </Directory>
    
    # Headers de sécurité
    Header always set X-Frame-Options DENY
    Header always set X-Content-Type-Options nosniff
    Header always set X-XSS-Protection "1; mode=block"
</VirtualHost>
```

## 🔧 Configuration

### Environnements

#### Développement (`environment.ts`)
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081',
  wsUrl: 'ws://localhost:8081/ws',
  // ... autres configurations
};
```

#### Production (`environment.prod.ts`)
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.your-domain.com',
  wsUrl: 'wss://api.your-domain.com/ws',
  // ... autres configurations
};
```

### Proxy de développement

Créer `proxy.conf.json` pour le développement :

```json
{
  "/api/*": {
    "target": "http://localhost:8081",
    "secure": true,
    "changeOrigin": true,
    "logLevel": "debug"
  },
  "/ws": {
    "target": "ws://localhost:8081",
    "ws": true
  }
}
```

Utiliser avec :
```bash
ng serve --proxy-config proxy.conf.json
```

## 📊 Monitoring et Logging

### Performance Monitoring

Le service `PerformanceService` collecte automatiquement :
- Temps de réponse des API
- Métriques Core Web Vitals (LCP, FID, CLS)
- Utilisation mémoire
- Temps de chargement des composants

### Security Logging

Le service `SecurityService` enregistre :
- Tentatives d'attaques XSS
- Violations CSP
- Accès non autorisés
- Dépassements de rate limit

### Intégration avec des services externes

```typescript
// Exemple d'intégration avec un service de monitoring
export function sendToMonitoringService(data: any) {
  // Envoyer vers Sentry, DataDog, etc.
}
```

## 🤝 Contribution

### Standards de code

1. **TypeScript strict** : Tous les fichiers doivent respecter le mode strict
2. **ESLint** : Utilisation des règles Angular recommandées
3. **Prettier** : Formatage automatique du code
4. **Conventional Commits** : Format des messages de commit

### Workflow de développement

1. Créer une branche feature : `git checkout -b feature/nom-feature`
2. Développer et tester localement
3. Lancer les tests : `npm test`
4. Vérifier le linting : `npm run lint`
5. Créer une Pull Request

### Commandes utiles

```bash
# Linting
npm run lint
npm run lint:fix

# Formatage
npm run format

# Analyse des bundles
npm run analyze

# Tests de sécurité
npm audit
npm audit fix
```

## 📚 Documentation API

### Services principaux

#### AuthService
```typescript
// Connexion
login(credentials: LoginRequest): Observable<TokenResponse>

// Déconnexion
logout(): void

// Utilisateur actuel
getCurrentUser(): Observable<UserResponse>
```

#### InscriptionService
```typescript
// Mes inscriptions
getMyInscriptions(): Observable<InscriptionResponse[]>

// Créer inscription
createInscription(data: InscriptionRequest): Observable<InscriptionResponse>
```

#### SoutenanceService
```typescript
// Mes soutenances
getMySoutenances(): Observable<SoutenanceResponse[]>

// Vérifier prérequis
checkPrerequis(doctorantId: number): Observable<PrerequisStatus>
```

### Modèles de données

#### User
```typescript
interface User {
  id: number;
  FirstName: string;
  LastName: string;
  email: string;
  roles: Role[];
}
```

#### Inscription
```typescript
interface Inscription {
  id: number;
  doctorant: User;
  directeur: User;
  sujetThese: string;
  statut: InscriptionStatus;
}
```

## 🐛 Dépannage

### Problèmes courants

#### Erreur CORS
```
Access to XMLHttpRequest at 'http://localhost:8081' from origin 'http://localhost:4200' has been blocked by CORS policy
```

**Solution** : Vérifier la configuration CORS du backend Spring Boot

#### Token expiré
```
401 Unauthorized - Token expired
```

**Solution** : Le refresh automatique est implémenté, vérifier la configuration des tokens

#### WebSocket connection failed
```
WebSocket connection to 'ws://localhost:8081/ws' failed
```

**Solution** : Vérifier que le backend WebSocket est démarré

### Logs de débogage

Activer les logs détaillés :

```typescript
// Dans environment.ts
export const environment = {
  production: false,
  debug: true,
  // ...
};
```

## 📞 Support

Pour toute question ou problème :

1. Consulter cette documentation
2. Vérifier les issues GitHub existantes
3. Créer une nouvelle issue avec :
   - Description du problème
   - Étapes pour reproduire
   - Environnement (OS, Node.js, navigateur)
   - Logs d'erreur

## 📄 Licence

Ce projet est sous licence [MIT](LICENSE).

## 🔄 Changelog

### Version 1.0.0
- ✅ Authentification JWT complète
- ✅ Gestion des inscriptions
- ✅ Processus de soutenance
- ✅ Interface d'administration
- ✅ Système de notifications
- ✅ Optimisations de performance
- ✅ Sécurité renforcée
- ✅ Tests unitaires

---

**Développé avec ❤️ par l'équipe de développement**