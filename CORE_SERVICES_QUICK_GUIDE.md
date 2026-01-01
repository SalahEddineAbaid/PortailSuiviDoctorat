# 🚀 Guide Rapide - Core Services

## 📖 Guide d'utilisation des services core

Ce guide fournit des exemples pratiques d'utilisation des services core de l'application.

---

## 🔐 Authentication

### Connexion

```typescript
import { AuthService } from '@core/services';

constructor(private authService: AuthService) {}

login() {
  this.authService.login({
    email: 'user@example.com',
    password: 'password123'
  }).subscribe({
    next: (user) => {
      console.log('Connecté:', user);
      // Redirection automatique vers le dashboard approprié
      const route = this.authService.getDashboardRoute();
      this.router.navigate([route]);
    },
    error: (error) => {
      console.error('Erreur de connexion:', error);
    }
  });
}
```

### Vérification des rôles

```typescript
// Dans un composant
ngOnInit() {
  if (this.authService.isAdmin()) {
    // Afficher les fonctionnalités admin
  }

  if (this.authService.hasRole('ROLE_DIRECTEUR')) {
    // Afficher les fonctionnalités directeur
  }
}
```

### Protection des routes

```typescript
// Dans app.routes.ts
import { authGuard, roleGuard } from "@core/guards";

export const routes: Routes = [
  {
    path: "admin",
    canActivate: [authGuard, roleGuard],
    data: { role: "ROLE_ADMIN" },
    loadChildren: () => import("./features/admin/admin.routes"),
  },
];
```

---

## 🌐 API Integration

### Requêtes HTTP simples

```typescript
import { ApiIntegrationService } from '@core/services';

constructor(private apiService: ApiIntegrationService) {}

// GET
getData() {
  this.apiService.get<MyData>('/api/data')
    .subscribe(data => console.log(data));
}

// POST
createData(data: MyData) {
  this.apiService.post<MyData>('/api/data', data)
    .subscribe(result => console.log('Créé:', result));
}

// PUT
updateData(id: number, data: MyData) {
  this.apiService.put<MyData>(`/api/data/${id}`, data)
    .subscribe(result => console.log('Mis à jour:', result));
}

// DELETE
deleteData(id: number) {
  this.apiService.delete(`/api/data/${id}`)
    .subscribe(() => console.log('Supprimé'));
}
```

### Upload de fichiers

```typescript
uploadDocument(file: File) {
  this.apiService.uploadFile('/api/documents/upload', file)
    .subscribe({
      next: (response) => {
        console.log('Fichier uploadé:', response.filename);
      },
      error: (error) => {
        console.error('Erreur upload:', error);
      }
    });
}
```

### Download de fichiers

```typescript
downloadDocument(documentId: number) {
  this.apiService.downloadFile(`/api/documents/${documentId}/download`)
    .subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'document.pdf';
      a.click();
      window.URL.revokeObjectURL(url);
    });
}
```

---

## 🔒 Security

### Validation et sanitization

```typescript
import { SecurityService } from '@core/services';

constructor(private securityService: SecurityService) {}

validateInput(userInput: string) {
  // Sanitize input
  const clean = this.securityService.sanitizeInput(userInput);

  // Detect XSS
  if (this.securityService.detectXSS(userInput)) {
    console.warn('Tentative XSS détectée!');
    return;
  }

  // Validate email
  if (!this.securityService.isValidEmail(userInput)) {
    console.error('Email invalide');
    return;
  }

  // Use clean input
  this.processInput(clean);
}
```

### Validation de mot de passe

```typescript
checkPasswordStrength(password: string) {
  const result = this.securityService.validatePasswordStrength(password);

  if (!result.isValid) {
    console.log('Mot de passe faible:', result.feedback);
    // Afficher les recommandations à l'utilisateur
    result.feedback.forEach(msg => console.log('- ' + msg));
  } else {
    console.log('Mot de passe fort! Score:', result.score);
  }
}
```

---

## 💾 Cache

### Utilisation du cache

```typescript
import { CacheService } from '@core/services';

constructor(private cacheService: CacheService) {}

// Mettre en cache
saveToCache() {
  const data = { name: 'John', age: 30 };
  this.cacheService.set('user-data', data, 5 * 60 * 1000); // 5 minutes
}

// Récupérer du cache
getFromCache() {
  const data = this.cacheService.get<UserData>('user-data');
  if (data) {
    console.log('Données en cache:', data);
  } else {
    console.log('Cache expiré ou vide');
  }
}

// Cache d'Observable
loadData() {
  return this.cacheService.cacheObservable(
    'api-data',
    this.http.get('/api/data'),
    10 * 60 * 1000 // 10 minutes
  );
}
```

---

## 📊 Performance

### Mesure des performances

```typescript
import { PerformanceService } from '@core/services';

constructor(private perfService: PerformanceService) {}

// Mesurer une fonction synchrone
processData() {
  const result = this.perfService.measureExecution('data-processing', () => {
    // Code à mesurer
    return this.heavyComputation();
  });

  console.log('Résultat:', result);
}

// Mesurer une fonction asynchrone
async loadData() {
  const result = await this.perfService.measureAsyncExecution(
    'data-loading',
    async () => {
      return await this.fetchData();
    }
  );

  console.log('Données chargées:', result);
}

// Obtenir les métriques
getMetrics() {
  const metrics = this.perfService.getMetrics();
  metrics.forEach((value, key) => {
    console.log(`${key}: ${value.toFixed(2)}ms`);
  });
}
```

---

## ♿ Accessibility

### Annonces pour lecteurs d'écran

```typescript
import { AccessibilityService } from '@core/services';

constructor(private a11yService: AccessibilityService) {}

// Annonce polie (ne coupe pas la lecture en cours)
announceSuccess() {
  this.a11yService.announce('Inscription enregistrée avec succès', 'polite');
}

// Annonce assertive (interrompt la lecture en cours)
announceError() {
  this.a11yService.announce('Erreur: veuillez corriger les champs', 'assertive');
}

// Gérer le focus
focusOnElement() {
  this.a11yService.focusElement('#main-content');
}

// Skip to content
skipToMain() {
  this.a11yService.skipToContent('main-content');
}
```

### Préférences d'accessibilité

```typescript
ngOnInit() {
  // Observer les préférences
  this.a11yService.preferences$.subscribe(prefs => {
    console.log('Préférences:', prefs);

    if (prefs.reduceMotion) {
      // Désactiver les animations
    }

    if (prefs.highContrast) {
      // Appliquer le thème à fort contraste
    }
  });
}

// Modifier une préférence
toggleHighContrast() {
  const current = this.a11yService.getPreferences();
  this.a11yService.updatePreference('highContrast', !current.highContrast);
}
```

---

## 🔌 WebSocket

### Connexion WebSocket

```typescript
import { WebSocketService, WebSocketState } from '@core/services';

constructor(private wsService: WebSocketService) {}

ngOnInit() {
  // Se connecter avec authentification
  const token = this.authService.getToken();
  this.wsService.connectWithAuth('ws://localhost:8081/ws', token!)
    .subscribe(state => {
      console.log('État WebSocket:', state);
    });

  // Écouter les messages
  this.wsService.messages$.subscribe(message => {
    console.log('Message reçu:', message);

    switch (message.type) {
      case 'NOTIFICATION':
        this.handleNotification(message.data);
        break;
      case 'STATUS_UPDATE':
        this.handleStatusUpdate(message.data);
        break;
    }
  });

  // Écouter les changements d'état
  this.wsService.state$.subscribe(state => {
    if (state === WebSocketState.CONNECTED) {
      console.log('WebSocket connecté');
    } else if (state === WebSocketState.DISCONNECTED) {
      console.log('WebSocket déconnecté');
    }
  });
}

// Envoyer un message
sendMessage() {
  this.wsService.send({
    type: 'CHAT_MESSAGE',
    data: { text: 'Hello!' }
  });
}

ngOnDestroy() {
  this.wsService.disconnect();
}
```

---

## 📊 Dashboard

### Récupérer les données du dashboard

```typescript
import { DashboardService } from '@core/services';

constructor(private dashboardService: DashboardService) {}

// Dashboard doctorant
loadDoctorantDashboard() {
  this.dashboardService.getDoctorantDashboardData()
    .subscribe(data => {
      this.inscriptionActuelle = data.inscriptionActuelle;
      this.prochaineSoutenance = data.prochaineSoutenance;
      this.notifications = data.notifications;
      this.alertes = data.alertes;
    });
}

// Widgets de statut
loadStatusWidgets() {
  this.dashboardService.getDoctorantStatusWidgets()
    .subscribe(widgets => {
      this.widgets = widgets;
    });
}

// Dashboard directeur
loadDirecteurDashboard() {
  this.dashboardService.getDirecteurDashboardData()
    .subscribe(data => {
      this.doctorants = data.doctorants;
      this.dossiersEnAttente = data.dossiersEnAttente;
      this.statistiques = data.statistiques;
    });
}

// Dashboard admin
loadAdminDashboard() {
  this.dashboardService.getAdminDashboardData()
    .subscribe(data => {
      this.statistiques = data.statistiques;
      this.campagneActive = data.campagneActive;
      this.dossiersEnAttente = data.dossiersEnAttente;
    });
}
```

---

## 🧪 Backend Testing

### Tester la connectivité backend

```typescript
import { BackendTestService } from '@core/services';

constructor(private testService: BackendTestService) {}

// Tester tous les endpoints
runTests() {
  this.testService.testAllEndpoints()
    .subscribe(results => {
      console.log('Tests terminés:', results);
      console.log(`Réussis: ${results.summary.passed}/${results.summary.total}`);
      console.log(`Temps moyen: ${results.summary.averageResponseTime}ms`);

      // Afficher les tests échoués
      results.tests
        .filter(t => !t.success)
        .forEach(t => {
          console.error(`❌ ${t.name}: ${t.error}`);
        });
    });
}

// Tester le token JWT
testToken() {
  this.testService.testJWTToken()
    .subscribe(result => {
      if (result.valid) {
        console.log('✅ Token valide');
        console.log('Expiration:', result.expiration);
      } else {
        console.error('❌ Token invalide:', result.details);
      }
    });
}

// Générer un rapport
generateReport() {
  this.testService.testAllEndpoints()
    .subscribe(results => {
      const report = this.testService.generateTestReport(results);
      console.log(report);
      // Ou télécharger le rapport
      this.downloadReport(report);
    });
}
```

---

## 🎯 Bonnes Pratiques

### 1. Toujours se désabonner

```typescript
import { Subject, takeUntil } from "rxjs";

export class MyComponent implements OnDestroy {
  private destroy$ = new Subject<void>();

  ngOnInit() {
    this.myService
      .getData()
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        // Traiter les données
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

### 2. Gérer les erreurs

```typescript
loadData() {
  this.apiService.get('/api/data')
    .pipe(
      catchError(error => {
        console.error('Erreur:', error);
        this.showError('Impossible de charger les données');
        return of([]); // Retourner une valeur par défaut
      })
    )
    .subscribe(data => {
      this.data = data;
    });
}
```

### 3. Utiliser le cache pour les données statiques

```typescript
getCountries() {
  return this.cacheService.cacheObservable(
    'countries',
    this.apiService.get('/api/countries'),
    24 * 60 * 60 * 1000 // 24 heures
  );
}
```

### 4. Mesurer les performances critiques

```typescript
loadCriticalData() {
  return this.perfService.measureAsyncExecution(
    'critical-data-load',
    async () => {
      const data = await this.fetchData();
      return this.processData(data);
    }
  );
}
```

### 5. Annoncer les changements importants

```typescript
saveData() {
  this.apiService.post('/api/data', this.formData)
    .subscribe({
      next: () => {
        this.a11yService.announce('Données enregistrées avec succès');
        this.showSuccess('Enregistré!');
      },
      error: () => {
        this.a11yService.announce('Erreur lors de l\'enregistrement', 'assertive');
        this.showError('Erreur!');
      }
    });
}
```

---

## 📚 Ressources

- [Documentation complète](./CORE_SERVICES_STATUS.md)
- [Architecture](./ARCHITECTURE.md)
- [API Integration](./API_INTEGRATION.md)

---

**Dernière mise à jour:** 2026-01-01
