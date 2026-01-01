# ✅ Vérification Complète du Module d'Authentification

## 🎯 Objectif

Ce document vous guide pour vérifier que **TOUT** fonctionne correctement dans le module d'authentification.

---

## 📋 Prérequis

### 1. Backend Démarré

```bash
# Vérifier que le user-service tourne
curl http://localhost:8081/api/actuator/health
```

**Résultat attendu** : `{"status":"UP"}`

### 2. Frontend Installé

```bash
cd frontend
npm install
```

### 3. Frontend Démarré

```bash
cd frontend
npm start
```

**Résultat attendu** : Application accessible sur `http://localhost:4200`

---

## 🔍 Vérification des Fichiers

### 1. Structure des Fichiers

Vérifier que tous les fichiers existent :

```
frontend/src/app/
├── core/
│   ├── services/
│   │   └── auth.service.ts ✅
│   ├── guards/
│   │   ├── auth.guard.ts ✅
│   │   └── role.guard.ts ✅
│   ├── interceptors/
│   │   └── auth.interceptor.ts ✅
│   └── validators/
│       └── custom-validators.ts ✅
├── features/
│   └── auth/
│       ├── login/ ✅
│       ├── register/ ✅
│       ├── profile/ ✅
│       ├── forgot-password/ ✅
│       └── auth.routes.ts ✅
└── environments/
    └── environment.ts ✅
```

### 2. Vérification des Imports

Ouvrir `frontend/src/app/app.config.ts` et vérifier :

```typescript
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(withInterceptors([authInterceptor])),
    // ...
  ],
};
```

### 3. Vérification des Routes

Ouvrir `frontend/src/app/app.routes.ts` et vérifier :

```typescript
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: 'login', loadChildren: () => import('./features/auth/auth.routes') },
  { path: 'register', loadChildren: () => import('./features/auth/auth.routes') },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadChildren: () => import('./features/auth/auth.routes'),
  },
  // ...
];
```

---

## 🧪 Tests Fonctionnels

### Test 1 : Inscription ✅

**Étapes** :

1. Aller sur `http://localhost:4200/register`
2. Remplir le formulaire :
   ```
   Prénom: Test
   Nom: User
   Email: test.user@example.com
   Téléphone: +212612345678
   Adresse: 123 Test Street
   Ville: Casablanca
   Pays: Maroc
   Mot de passe: Test@1234567890
   Confirmer: Test@1234567890
   ```
3. Cliquer sur "S'inscrire"

**Vérifications** :

- [ ] Aucune erreur de validation
- [ ] Message de succès affiché
- [ ] Redirection vers `/login` après 2 secondes
- [ ] Console : `✅ [AUTH SERVICE] Inscription réussie`

**En cas d'erreur** :

- 409 : Email déjà utilisé → Utiliser un autre email
- 400 : Données invalides → Vérifier le format des données
- 0 : Backend non accessible → Vérifier que le backend tourne

---

### Test 2 : Connexion ✅

**Étapes** :

1. Aller sur `http://localhost:4200/login`
2. Saisir :
   ```
   Email: test.user@example.com
   Mot de passe: Test@1234567890
   ```
3. Cliquer sur "Se connecter"

**Vérifications** :

- [ ] Aucune erreur
- [ ] Console : `✅ [AUTH SERVICE] Tokens reçus`
- [ ] Console : `✅ [AUTH SERVICE] Utilisateur chargé`
- [ ] Console : `🎭 Rôles: ["ROLE_DOCTORANT"]`
- [ ] Redirection vers `/dashboard/doctorant`
- [ ] LocalStorage contient `accessToken` et `refreshToken`

**Vérifier le LocalStorage** :

```javascript
// Dans la console du navigateur
console.log('Access Token:', localStorage.getItem('accessToken'));
console.log('Refresh Token:', localStorage.getItem('refreshToken'));
```

**En cas d'erreur** :

- 401 : Credentials invalides → Vérifier email/mot de passe
- 0 : Backend non accessible → Vérifier que le backend tourne

---

### Test 3 : Profil Utilisateur ✅

**Étapes** :

1. Se connecter (si pas déjà connecté)
2. Aller sur `http://localhost:4200/profile`
3. Modifier le prénom : `Test-Modified`
4. Cliquer sur "Mettre à jour"

**Vérifications** :

- [ ] Message de succès affiché
- [ ] Console : `✅ [AUTH SERVICE] Profil mis à jour`
- [ ] Prénom mis à jour dans l'interface
- [ ] Aucune erreur

**En cas d'erreur** :

- 401 : Non authentifié → Se reconnecter
- 400 : Données invalides → Vérifier le format

---

### Test 4 : Changement de Mot de Passe ✅

**Étapes** :

1. Sur la page profil, aller à la section "Changer le mot de passe"
2. Remplir :
   ```
   Ancien mot de passe: Test@1234567890
   Nouveau mot de passe: NewTest@1234567890
   Confirmer: NewTest@1234567890
   ```
3. Cliquer sur "Changer le mot de passe"

**Vérifications** :

- [ ] Message de succès affiché
- [ ] Console : `✅ [AUTH SERVICE] Mot de passe changé avec succès`
- [ ] Formulaire réinitialisé
- [ ] Aucune erreur

**Test de connexion avec le nouveau mot de passe** :

1. Se déconnecter
2. Se reconnecter avec `NewTest@1234567890`
3. Vérifier que la connexion fonctionne

**En cas d'erreur** :

- 401 : Ancien mot de passe incorrect
- 400 : Nouveau mot de passe ne respecte pas les critères

---

### Test 5 : Mot de Passe Oublié ✅

**Étapes** :

1. Se déconnecter
2. Aller sur `http://localhost:4200/forgot-password`
3. Saisir : `test.user@example.com`
4. Cliquer sur "Envoyer"

**Vérifications** :

- [ ] Message de confirmation affiché
- [ ] Console : `✅ [AUTH SERVICE] Email de réinitialisation envoyé`
- [ ] Vérifier les logs du backend pour le token

**Récupérer le token** :

Dans les logs du backend, chercher :

```
Reset token for test.user@example.com: <TOKEN>
```

**Réinitialisation avec le token** :

1. Aller sur `http://localhost:4200/forgot-password?token=<TOKEN>`
2. Saisir :
   ```
   Nouveau mot de passe: Reset@1234567890
   Confirmer: Reset@1234567890
   ```
3. Cliquer sur "Réinitialiser"

**Vérifications** :

- [ ] Message de succès affiché
- [ ] Console : `✅ [AUTH SERVICE] Mot de passe réinitialisé`
- [ ] Redirection vers `/login` après 3 secondes
- [ ] Connexion possible avec le nouveau mot de passe

**En cas d'erreur** :

- 400 : Token invalide ou expiré → Refaire une demande
- 0 : Backend non accessible

---

### Test 6 : Déconnexion ✅

**Étapes** :

1. Être connecté
2. Cliquer sur "Déconnexion" (dans le menu ou header)

**Vérifications** :

- [ ] Console : `🚪 [AUTH SERVICE] Déconnexion de l'utilisateur`
- [ ] Redirection vers `/login`
- [ ] LocalStorage vidé (pas de tokens)
- [ ] Impossible d'accéder aux routes protégées

**Vérifier le LocalStorage** :

```javascript
// Dans la console du navigateur
console.log('Access Token:', localStorage.getItem('accessToken')); // null
console.log('Refresh Token:', localStorage.getItem('refreshToken')); // null
```

---

### Test 7 : AuthGuard ✅

**Étapes** :

1. Se déconnecter (si connecté)
2. Essayer d'accéder à `http://localhost:4200/profile`

**Vérifications** :

- [ ] Redirection automatique vers `/login`
- [ ] URL contient `?returnUrl=/profile`
- [ ] Console : `⚠️ AuthGuard : Utilisateur non authentifié`

**Après connexion** :

- [ ] Redirection automatique vers `/profile` (returnUrl)

---

### Test 8 : RoleGuard ✅

**Étapes** :

1. Se connecter avec un compte DOCTORANT
2. Essayer d'accéder à `http://localhost:4200/admin`

**Vérifications** :

- [ ] Redirection vers `/unauthorized` ou `/dashboard/doctorant`
- [ ] Console : `⚠️ RoleGuard : Utilisateur n'a pas le rôle ROLE_ADMIN`
- [ ] Message d'erreur affiché

---

### Test 9 : Rafraîchissement du Token ✅

**Simulation d'expiration** :

```javascript
// Dans la console du navigateur
// 1. Sauvegarder le refresh token
const refreshToken = localStorage.getItem('refreshToken');

// 2. Invalider l'access token
localStorage.setItem('accessToken', 'invalid_token');

// 3. Faire une requête authentifiée (ex: aller sur /profile)
// L'intercepteur devrait automatiquement rafraîchir le token
```

**Vérifications** :

- [ ] Console : `🔄 [AUTH SERVICE] Tentative de rafraîchissement du token...`
- [ ] Console : `✅ [AUTH SERVICE] Token rafraîchi avec succès`
- [ ] Nouveau token dans localStorage
- [ ] Requête réussie après rafraîchissement

**En cas d'échec** :

- [ ] Console : `❌ [AUTH SERVICE] Erreur rafraîchissement token`
- [ ] Déconnexion automatique
- [ ] Redirection vers `/login`

---

### Test 10 : Validation des Formulaires ✅

**Test du mot de passe faible** :

1. Aller sur `/register`
2. Saisir un mot de passe : `test123`

**Vérifications** :

- [ ] Erreur affichée : "Le mot de passe doit contenir au moins 12 caractères"
- [ ] Erreur : "Au moins une lettre majuscule"
- [ ] Erreur : "Au moins un caractère spécial"
- [ ] Bouton "S'inscrire" désactivé

**Test de l'email invalide** :

1. Saisir : `test@invalid`

**Vérifications** :

- [ ] Erreur affichée : "Email invalide"
- [ ] Bouton désactivé

**Test du téléphone invalide** :

1. Saisir : `123`

**Vérifications** :

- [ ] Erreur affichée : "Numéro de téléphone invalide"
- [ ] Bouton désactivé

**Test des mots de passe non correspondants** :

1. Mot de passe : `Test@1234567890`
2. Confirmer : `Test@1234567891`

**Vérifications** :

- [ ] Erreur affichée : "Les mots de passe ne correspondent pas"
- [ ] Bouton désactivé

---

## 🔐 Tests de Sécurité

### Test 1 : Accès Non Autorisé

**Sans token** :

```bash
curl http://localhost:8081/api/users/profile
```

**Résultat attendu** : `401 Unauthorized`

**Avec token invalide** :

```bash
curl -H "Authorization: Bearer invalid_token" http://localhost:8081/api/users/profile
```

**Résultat attendu** : `401 Unauthorized`

### Test 2 : CORS

**Depuis un autre domaine** :

```javascript
// Dans la console d'un autre site
fetch('http://localhost:8081/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email: 'test@test.com', password: 'test' }),
});
```

**Résultat attendu** : Erreur CORS (bloqué par le navigateur)

### Test 3 : Injection SQL

**Tentative d'injection** :

```
Email: test@test.com' OR '1'='1
Mot de passe: anything
```

**Résultat attendu** : `401 Unauthorized` (pas d'injection possible)

---

## 📊 Vérification des Logs

### Logs du Frontend (Console)

**Connexion réussie** :

```
📤 [AUTH SERVICE] Tentative de connexion pour: test.user@example.com
✅ [AUTH SERVICE] Tokens reçus
🔑 Access Token: eyJhbGciOiJIUzI1NiIs...
🔄 Refresh Token: eyJhbGciOiJIUzI1NiIs...
💾 [AUTH SERVICE] Tokens stockés dans localStorage
👤 [AUTH SERVICE] Chargement des infos utilisateur...
✅ [AUTH SERVICE] Utilisateur chargé: {id: 1, FirstName: "Test", ...}
👤 Nom: Test User
📧 Email: test.user@example.com
🎭 Rôles: ["ROLE_DOCTORANT"]
🎯 [AUTH SERVICE] Détermination de la route du dashboard...
🎭 [AUTH SERVICE] Rôle détecté: ROLE_DOCTORANT
✅ [AUTH SERVICE] Route: /dashboard/doctorant
```

### Logs du Backend

**Connexion réussie** :

```
INFO  - Login attempt for: test.user@example.com
INFO  - User authenticated successfully: test.user@example.com
INFO  - Access token generated for user: 1
INFO  - Refresh token generated for user: 1
```

**Inscription réussie** :

```
INFO  - Registration attempt for: test.user@example.com
INFO  - User registered successfully: test.user@example.com
INFO  - Verification email sent to: test.user@example.com
```

---

## ✅ Checklist Finale

### Fonctionnalités

- [ ] Inscription avec validation complète
- [ ] Connexion avec JWT
- [ ] Déconnexion
- [ ] Profil utilisateur (lecture)
- [ ] Mise à jour du profil
- [ ] Changement de mot de passe
- [ ] Mot de passe oublié (demande)
- [ ] Réinitialisation avec token
- [ ] Rafraîchissement automatique du token

### Sécurité

- [ ] AuthGuard protège les routes
- [ ] RoleGuard protège par rôle
- [ ] AuthInterceptor ajoute les tokens
- [ ] Tokens stockés dans localStorage
- [ ] Validation stricte des mots de passe
- [ ] Gestion des erreurs HTTP
- [ ] Protection CORS
- [ ] Pas d'injection SQL possible

### UX

- [ ] Messages d'erreur clairs
- [ ] Messages de succès
- [ ] Validation en temps réel
- [ ] Affichage/masquage des mots de passe
- [ ] Redirections automatiques
- [ ] Loading states
- [ ] Formulaires réactifs

### Code

- [ ] Aucune erreur TypeScript
- [ ] Aucune erreur de compilation
- [ ] Aucun warning dans la console
- [ ] Code bien structuré
- [ ] Commentaires clairs
- [ ] Logs détaillés

---

## 🎉 Résultat

Si **TOUS** les tests passent, votre module d'authentification est **100% fonctionnel et sécurisé** ! 🚀

### Score

- **Fonctionnalités** : \_\_\_/9
- **Sécurité** : \_\_\_/8
- **UX** : \_\_\_/8
- **Code** : \_\_\_/4

**Total** : \_\_\_/29

### Niveau

- **29/29** : 🏆 Parfait !
- **25-28** : ✅ Excellent
- **20-24** : 👍 Bon
- **15-19** : ⚠️ À améliorer
- **< 15** : ❌ Problèmes à résoudre

---

## 🐛 Dépannage

### Problème : Erreurs TypeScript

**Solution** :

```bash
cd frontend
npm install
```

### Problème : Backend non accessible

**Solution** :

```bash
# Vérifier que le backend tourne
curl http://localhost:8081/api/actuator/health

# Démarrer le backend si nécessaire
cd user-service
mvn spring-boot:run
```

### Problème : CORS errors

**Solution** :

1. Vérifier `@CrossOrigin` dans les controllers
2. Redémarrer le backend
3. Vider le cache du navigateur

### Problème : Tokens non stockés

**Solution** :

1. Vérifier la console pour les erreurs
2. Vérifier que `environment.ts` contient les bonnes clés
3. Vider le localStorage et réessayer

### Problème : Redirection infinie

**Solution** :

1. Vérifier les guards
2. Vérifier les routes
3. Vider le localStorage
4. Redémarrer l'application

---

## 📞 Support

Si vous rencontrez des problèmes :

1. Vérifier les logs (frontend + backend)
2. Vérifier la documentation
3. Vérifier les issues GitHub
4. Contacter l'équipe de développement

---

**Dernière mise à jour** : 2024
**Version** : 1.0.0
