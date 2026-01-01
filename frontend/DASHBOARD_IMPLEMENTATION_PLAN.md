# 📊 Plan d'Implémentation des Dashboards - Frontend Angular

## 🎯 Vue d'ensemble

Ce document détaille l'implémentation complète des dashboards pour les trois rôles utilisateurs : Doctorant, Directeur et Admin.

---

## 📋 Table des matières

1. [Analyse des Microservices](#analyse-des-microservices)
2. [Architecture Frontend](#architecture-frontend)
3. [Endpoints par Dashboard](#endpoints-par-dashboard)
4. [Structure des Composants](#structure-des-composants)
5. [Services Angular](#services-angular)
6. [Modèles de Données](#modèles-de-données)
7. [Routing et Guards](#routing-et-guards)
8. [Plan d'Implémentation](#plan-dimplémentation)

---

## 🔍 Analyse des Microservices

### 1. **User Service** (Port 8083)

**Base URL**: `http://localhost:8081/api/users`

**Endpoints disponibles**:

- `GET /api/users/profile` - Profil utilisateur connecté
- `GET /api/users/{id}` - Utilisateur par ID
- `GET /api/users` - Liste tous les utilisateurs (ADMIN)
- `GET /api/users/{id}/profile-complete` - Profil détaillé
- `POST /api/users/change-password` - Changer mot de passe
- `POST /api/users/logout` - Déconnexion

**Endpoints Admin**:

- `GET /api/admin/statistics/users` - Statistiques utilisateurs
- `GET /api/admin/statistics/connections` - Statistiques connexions
- `GET /api/admin/users/disabled` - Utilisateurs désactivés
- `POST /api/admin/users/{userId}/disable` - Désactiver utilisateur
- `POST /api/admin/users/{userId}/enable` - Activer utilisateur

**Endpoints Audit**:

- `GET /api/admin/audit/users/{userId}` - Historique audit utilisateur
- `GET /api/admin/audit/recent` - Audits récents

---

### 2. **Inscription Service** (Port 8084)

**Base URL**: `http://localhost:8081/api/inscriptions`

**Endpoints Doctorant**:

- `GET /api/inscriptions/doctorant/{id}/dashboard` - **Dashboard complet doctorant** ⭐
- `GET /api/inscriptions/doctorant/{doctorantId}` - Inscriptions du doctorant
- `GET /api/inscriptions/{id}` - Détail inscription
- `POST /api/inscriptions` - Créer inscription
- `POST /api/inscriptions/{id}/soumettre` - Soumettre inscription
- `GET /api/inscriptions/{id}/attestation` - Télécharger attestation

**Endpoints Directeur**:

- `GET /api/inscriptions/directeur/{directeurId}/en-attente` - Inscriptions en attente
- `POST /api/inscriptions/{id}/valider-directeur` - Valider inscription
- `POST /api/inscriptions/{id}/derogation/valider-directeur` - Valider dérogation

**Endpoints Admin**:

- `GET /api/inscriptions/admin/en-attente` - Inscriptions en attente admin
- `POST /api/inscriptions/{id}/valider-admin` - Valider inscription
- `POST /api/inscriptions/{id}/derogation/valider-ped` - Valider dérogation PED
- `GET /api/inscriptions/verifier-alertes` - Vérifier alertes système

**Endpoints Campagnes**:

- `GET /api/campagnes` - Liste campagnes
- `GET /api/campagnes/active` - Campagne active
- `POST /api/campagnes` - Créer campagne (ADMIN)
- `PUT /api/campagnes/{id}` - Modifier campagne (ADMIN)

**Endpoints Documents**:

- `GET /api/documents/{inscriptionId}` - Documents d'une inscription
- `POST /api/documents/{inscriptionId}/upload` - Upload document
- `GET /api/documents/download/{documentId}` - Télécharger document

---

### 3. **Notification Service** (Port 8086)

**Base URL**: `http://localhost:8081/api/notifications`

**Endpoints disponibles**:

- `GET /api/notifications/user/{userId}` - Notifications utilisateur
- `GET /api/notifications/user/{userId}/unread` - Notifications non lues
- `GET /api/notifications/user/{userId}/unread/count` - Nombre non lues
- `POST /api/notifications/{id}/mark-read` - Marquer comme lue
- `POST /api/notifications/user/{userId}/mark-all-read` - Tout marquer comme lu
- `DELETE /api/notifications/{id}` - Supprimer notification
- `GET /api/notifications/user/{userId}/preferences` - Préférences notifications
- `PUT /api/notifications/user/{userId}/preferences` - Modifier préférences

---

### 4. **Batch Service** (Port 8087)

**Base URL**: `http://localhost:8081/api/batch`

**Endpoints disponibles**:

- `GET /api/batch/jobs` - Liste des jobs
- `POST /api/batch/jobs/{jobName}/run` - Lancer un job
- `GET /api/batch/jobs/{jobName}/status` - Statut d'un job
- `GET /api/batch/jobs/{jobName}/history` - Historique d'un job

---

## 🏗️ Architecture Frontend

### Structure des Dossiers

```
frontend/src/app/features/dashboard/
├── dashboard.routes.ts                    # Routes principales
├── models/
│   ├── dashboard.model.ts                 # Modèles TypeScript
│   ├── statistics.model.ts
│   └── widget.model.ts
├── services/
│   ├── dashboard.service.ts               # Service principal
│   ├── statistics.service.ts              # Service statistiques
│   └── widget.service.ts                  # Service widgets
├── resolvers/
│   ├── doctorant-dashboard.resolver.ts    # Préchargement données doctorant
│   ├── directeur-dashboard.resolver.ts    # Préchargement données directeur
│   └── admin-dashboard.resolver.ts        # Préchargement données admin
├── dashboard-container/
│   ├── dashboard-container.component.ts   # Container principal
│   ├── dashboard-container.component.html
│   └── dashboard-container.component.scss
├── doctorant-dashboard/
│   ├── doctorant-dashboard.component.ts
│   ├── doctorant-dashboard.component.html
│   ├── doctorant-dashboard.component.scss
│   └── widgets/
│       ├── progression-widget/
│       ├── inscriptions-widget/
│       ├── notifications-widget/
│       └── quick-actions-widget/
├── directeur-dashboard/
│   ├── directeur-dashboard.component.ts
│   ├── directeur-dashboard.component.html
│   ├── directeur-dashboard.component.scss
│   └── widgets/
│       ├── doctorants-widget/
│       ├── pending-requests-widget/
│       ├── statistics-widget/
│       └── notifications-widget/
└── admin-dashboard/
    ├── admin-dashboard.component.ts
    ├── admin-dashboard.component.html
    ├── admin-dashboard.component.scss
    └── widgets/
        ├── system-overview-widget/
        ├── users-statistics-widget/
        ├── campagnes-widget/
        ├── active-users-widget/
        └── audit-logs-widget/
```

---

## 📊 Endpoints par Dashboard

### 🎓 Dashboard Doctorant

**Données à charger**:

1. **Statistiques personnelles** → `GET /api/inscriptions/doctorant/{id}/dashboard`
2. **Inscriptions en cours** → `GET /api/inscriptions/doctorant/{doctorantId}`
3. **Notifications récentes** → `GET /api/notifications/user/{userId}/unread`
4. **Profil utilisateur** → `GET /api/users/profile`

**Widgets**:

- **Progression** : Avancement thèse, étapes validées
- **Inscriptions** : Liste inscriptions actives/en attente
- **Notifications** : 5 dernières notifications
- **Actions rapides** : Nouvelle inscription, Upload document, Voir attestations

---

### 👨‍🏫 Dashboard Directeur

**Données à charger**:

1. **Doctorants supervisés** → `GET /api/inscriptions/directeur/{directeurId}/en-attente`
2. **Demandes en attente** → `GET /api/inscriptions/directeur/{directeurId}/en-attente`
3. **Statistiques supervision** → Calculées côté frontend
4. **Notifications** → `GET /api/notifications/user/{userId}/unread`

**Widgets**:

- **Doctorants** : Liste doctorants avec statuts
- **Demandes en attente** : Inscriptions/dérogations à valider
- **Statistiques** : Nombre doctorants, taux validation, etc.
- **Notifications** : Alertes importantes

---

### 🛠️ Dashboard Admin

**Données à charger**:

1. **Statistiques utilisateurs** → `GET /api/admin/statistics/users`
2. **Statistiques connexions** → `GET /api/admin/statistics/connections`
3. **Inscriptions en attente** → `GET /api/inscriptions/admin/en-attente`
4. **Alertes système** → `GET /api/inscriptions/verifier-alertes`
5. **Campagnes actives** → `GET /api/campagnes`
6. **Utilisateurs actifs** → `GET /api/users`
7. **Logs récents** → `GET /api/admin/audit/recent`

**Widgets**:

- **Vue d'ensemble** : Métriques clés (users, thèses, campagnes)
- **Statistiques globales** : Graphiques et tendances
- **Gestion campagnes** : Liste et actions rapides
- **Utilisateurs actifs** : Tableau avec actions
- **Logs système** : Dernières activités

---

## 🧩 Structure des Composants

### Dashboard Container (Routeur dynamique)

```typescript
// Responsabilités :
// - Détection du rôle utilisateur
// - Routing vers le bon dashboard
// - Layout commun (header, sidebar)
// - Gestion des erreurs de chargement
```

### Composants Dashboard

Chaque dashboard suit la même structure :

1. **Component principal** : Orchestration et layout
2. **Widgets** : Composants réutilisables et autonomes
3. **Services** : Logique métier et appels API
4. **Resolvers** : Préchargement des données

---

## 🔧 Services Angular

### 1. DashboardService

```typescript
// Méthodes :
// - getDoctorantDashboard(userId: number): Observable<DoctorantDashboard>
// - getDirecteurDashboard(userId: number): Observable<DirecteurDashboard>
// - getAdminDashboard(): Observable<AdminDashboard>
// - refreshDashboard(): void
```

### 2. StatisticsService

```typescript
// Méthodes :
// - getUserStatistics(): Observable<UserStatistics>
// - getConnectionStatistics(): Observable<ConnectionStatistics>
// - getInscriptionStatistics(): Observable<InscriptionStatistics>
```

### 3. WidgetService

```typescript
// Méthodes :
// - getWidgetData(widgetType: string): Observable<any>
// - refreshWidget(widgetId: string): void
// - saveWidgetPreferences(preferences: WidgetPreferences): Observable<void>
```

---

## 📦 Modèles de Données

### DoctorantDashboard

```typescript
export interface DoctorantDashboard {
  user: UserInfo;
  statistics: {
    totalInscriptions: number;
    inscriptionsEnCours: number;
    inscriptionsValidees: number;
    documentsManquants: number;
    progressionThese: number;
  };
  inscriptions: InscriptionSummary[];
  notifications: Notification[];
  quickActions: QuickAction[];
}
```

### DirecteurDashboard

```typescript
export interface DirecteurDashboard {
  user: UserInfo;
  statistics: {
    totalDoctorants: number;
    demandesEnAttente: number;
    validationsEnCours: number;
    tauxValidation: number;
  };
  doctorants: DoctorantSummary[];
  demandesEnAttente: DemandeSummary[];
  notifications: Notification[];
}
```

### AdminDashboard

```typescript
export interface AdminDashboard {
  statistics: {
    totalUsers: number;
    activeUsers: number;
    totalInscriptions: number;
    activeCampagnes: number;
    pendingValidations: number;
  };
  userStatistics: UserStatistics;
  connectionStatistics: ConnectionStatistics;
  campagnes: Campagne[];
  recentAudits: AuditRecord[];
  systemAlerts: SystemAlert[];
}
```

---

## 🛡️ Routing et Guards

### Routes Configuration

```typescript
export const dashboardRoutes: Routes = [
  {
    path: '',
    component: DashboardContainerComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'doctorant',
        component: DoctorantDashboardComponent,
        canActivate: [roleGuard],
        data: { role: 'ROLE_DOCTORANT' },
        resolve: { dashboard: DoctorantDashboardResolver },
      },
      {
        path: 'directeur',
        component: DirecteurDashboardComponent,
        canActivate: [roleGuard],
        data: { role: 'ROLE_DIRECTEUR' },
        resolve: { dashboard: DirecteurDashboardResolver },
      },
      {
        path: 'admin',
        component: AdminDashboardComponent,
        canActivate: [roleGuard],
        data: { role: 'ROLE_ADMIN' },
        resolve: { dashboard: AdminDashboardResolver },
      },
    ],
  },
];
```

---

## 📅 Plan d'Implémentation

### Phase 1 : Fondations (Modèles & Services)

1. ✅ Créer les modèles TypeScript
2. ✅ Implémenter DashboardService
3. ✅ Implémenter StatisticsService
4. ✅ Créer les Resolvers

### Phase 2 : Dashboard Doctorant

1. ✅ Composant principal
2. ✅ Widget Progression
3. ✅ Widget Inscriptions
4. ✅ Widget Notifications
5. ✅ Widget Actions rapides

### Phase 3 : Dashboard Directeur

1. ✅ Composant principal
2. ✅ Widget Doctorants
3. ✅ Widget Demandes en attente
4. ✅ Widget Statistiques
5. ✅ Widget Notifications

### Phase 4 : Dashboard Admin

1. ✅ Composant principal
2. ✅ Widget Vue d'ensemble
3. ✅ Widget Statistiques utilisateurs
4. ✅ Widget Campagnes
5. ✅ Widget Utilisateurs actifs
6. ✅ Widget Logs système

### Phase 5 : Container & Routing

1. ✅ Dashboard Container
2. ✅ Configuration routing
3. ✅ Tests de navigation

### Phase 6 : Polish & UX

1. ✅ Loading states
2. ✅ Error handling
3. ✅ Responsive design
4. ✅ Animations

---

## 🎨 Bonnes Pratiques

### 1. **Composants Modulaires**

- Chaque widget est autonome
- Communication via @Input/@Output
- Pas de dépendances directes entre widgets

### 2. **Gestion des Erreurs**

- Try/catch dans les services
- Messages d'erreur utilisateur-friendly
- Fallback UI pour les erreurs

### 3. **Performance**

- Lazy loading des dashboards
- Resolvers pour préchargement
- OnPush change detection
- TrackBy dans les \*ngFor

### 4. **UX**

- Loading spinners
- Skeleton screens
- Refresh manuel
- Auto-refresh optionnel

---

## 🚀 Prochaines Étapes

1. Créer les modèles TypeScript
2. Implémenter les services
3. Créer les resolvers
4. Développer les composants dashboard
5. Implémenter les widgets
6. Tests et validation

---

**Date de création** : 2026-01-01
**Dernière mise à jour** : 2026-01-01
**Statut** : 🟢 Prêt pour implémentation
