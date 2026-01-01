# 🚀 Quick Start - Module d'Authentification

## 📋 Prérequis

1. **Backend démarré** : Le user-service doit tourner sur `http://localhost:8081`
2. **Frontend installé** : `npm install` dans le dossier `frontend/`

---

## 🎯 Démarrage Rapide

### 1. Démarrer le Frontend

```bash
cd frontend
npm start
```

L'application sera accessible sur `http://localhost:4200`

### 2. Tester l'Inscription

**URL** : `http://localhost:4200/register`

**Données de test** :

```
Prénom: Jean
Nom: Dupont
Email: jean.dupont@test.com
Téléphone: +212612345678
Adresse: 123 Rue de Test
Ville: Casablanca
Pays: Maroc
Mot de passe: Test@1234567890
Confirmer: Test@1234567890
```

**Résultat attendu** :

- ✅ Message de succès
- ✅ Redirection automatique vers `/login` après 2 secondes

### 3. Tester la Connexion

**URL** : `http://localhost:4200/login`

**Credentials** :

```
Email: jean.dupont@test.com
Mot de passe: Test@1234567890
```

**Résultat attendu** :

- ✅ Connexion réussie
- ✅ Tokens stockés dans localStorage
- ✅ Profil utilisateur chargé
- ✅ Redirection vers `/dashboard/doctorant` (ou selon le rôle)

### 4. Tester le Profil

**URL** : `http://localhost:4200/profile`

**Actions** :

1. Modifier le prénom : `Jean-Pierre`
2. Cliquer sur "Mettre à jour"
3. Vérifier le message de succès

**Changement de mot de passe** :

```
Ancien mot de passe: Test@1234567890
Nouveau mot de passe: NewTest@1234567890
Confirmer: NewTest@1234567890
```

**Résultat attendu** :

- ✅ Profil mis à jour
- ✅ Mot de passe changé avec succès

### 5. Tester le Mot de Passe Oublié

**URL** : `http://localhost:4200/forgot-password`

**Étape 1 - Demande** :

```
Email: jean.dupont@test.com
```

**Résultat attendu** :

- ✅ Message de confirmation
- ✅ Email envoyé (vérifier les logs du backend)

**Étape 2 - Réinitialisation** :

```
URL: http://localhost:4200/forgot-password?token=<TOKEN_FROM_EMAIL>
Nouveau mot de passe: Reset@1234567890
Confirmer: Reset@1234567890
```

**Résultat attendu** :

- ✅ Mot de passe réinitialisé
- ✅ Redirection vers `/login` après 3 secondes

---

## 🔍 Vérifications dans la Console

### Console du Navigateur

Lors de la connexion, vous devriez voir :

```
📤 [AUTH SERVICE] Tentative de connexion pour: jean.dupont@test.com
✅ [AUTH SERVICE] Tokens reçus
🔑 Access Token: eyJhbGciOiJIUzI1NiIs...
🔄 Refresh Token: eyJhbGciOiJIUzI1NiIs...
💾 [AUTH SERVICE] Tokens stockés dans localStorage
👤 [AUTH SERVICE] Chargement des infos utilisateur...
✅ [AUTH SERVICE] Utilisateur chargé: {id: 1, FirstName: "Jean", ...}
👤 Nom: Jean Dupont
📧 Email: jean.dupont@test.com
🎭 Rôles: ["ROLE_DOCTORANT"]
🎯 [AUTH SERVICE] Détermination de la route du dashboard...
🎭 [AUTH SERVICE] Rôle détecté: ROLE_DOCTORANT
✅ [AUTH SERVICE] Route: /dashboard/doctorant
```

### LocalStorage

Vérifier dans DevTools > Application > Local Storage :

```
accessToken: eyJhbGciOiJIUzI1NiIs...
refreshToken: eyJhbGciOiJIUzI1NiIs...
```

---

## 🧪 Tests des Erreurs

### 1. Email Déjà Existant (409)

**Action** : S'inscrire avec le même email deux fois

**Résultat attendu** :

```
❌ Cet email est déjà utilisé
```

### 2. Credentials Invalides (401)

**Action** : Se connecter avec un mauvais mot de passe

**Résultat attendu** :

```
❌ Email ou mot de passe incorrect
```

### 3. Backend Non Démarré (0)

**Action** : Arrêter le backend et essayer de se connecter

**Résultat attendu** :

```
❌ Impossible de contacter le serveur. Vérifiez que le backend est démarré sur le port 8081.
```

### 4. Token Expiré (401)

**Action** :

1. Se connecter
2. Attendre l'expiration du token (ou modifier manuellement dans localStorage)
3. Faire une requête authentifiée

**Résultat attendu** :

```
🔄 Token expiré, tentative de rafraîchissement...
✅ Token rafraîchi, nouvelle tentative de requête
```

### 5. Accès Non Autorisé (403)

**Action** : Essayer d'accéder à une route admin sans être admin

**Résultat attendu** :

```
⚠️ RoleGuard : Utilisateur n'a pas le rôle ROLE_ADMIN
→ Redirection vers /unauthorized
```

---

## 🔐 Validation du Mot de Passe

### Critères Requis

Le mot de passe doit contenir :

- ✅ Entre 12 et 64 caractères
- ✅ Au moins une lettre minuscule
- ✅ Au moins une lettre majuscule
- ✅ Au moins un chiffre
- ✅ Au moins un caractère spécial (@$!%\*?&.)
- ✅ Pas d'espaces ni de caractères non autorisés

### Exemples Valides

```
✅ Test@1234567890
✅ MyP@ssw0rd2024!
✅ Secure$Pass123
✅ Admin@2024Test!
```

### Exemples Invalides

```
❌ test123 (trop court, pas de majuscule, pas de spécial)
❌ Test123456 (pas de caractère spécial)
❌ test@123456 (pas de majuscule)
❌ TEST@123456 (pas de minuscule)
❌ Test@abcdef (pas de chiffre)
❌ Test 123456@ (contient un espace)
```

---

## 📊 Endpoints Backend Utilisés

| Endpoint                     | Méthode | Description               | Status Codes  |
| ---------------------------- | ------- | ------------------------- | ------------- |
| `/api/auth/register`         | POST    | Inscription               | 201, 409, 400 |
| `/api/auth/login`            | POST    | Connexion                 | 200, 401      |
| `/api/auth/refresh`          | POST    | Rafraîchir token          | 200, 401      |
| `/api/users/profile`         | GET     | Récupérer profil          | 200, 401      |
| `/api/users/profile`         | PUT     | Mettre à jour profil      | 200, 400, 401 |
| `/api/users/change-password` | POST    | Changer mot de passe      | 200, 401      |
| `/api/users/forgot-password` | POST    | Demander réinitialisation | 200           |
| `/api/users/reset-password`  | POST    | Réinitialiser avec token  | 200, 400      |

---

## 🛠️ Dépannage

### Problème : "Cannot GET /"

**Solution** : Aller sur `http://localhost:4200/login` directement

### Problème : "Cannot contact server"

**Solution** :

1. Vérifier que le backend tourne sur le port 8081
2. Vérifier les logs du backend
3. Vérifier la configuration dans `environment.ts`

### Problème : "Token expired"

**Solution** :

1. Se déconnecter
2. Se reconnecter
3. Le token sera automatiquement rafraîchi

### Problème : "CORS error"

**Solution** :

1. Vérifier que le backend a `@CrossOrigin(origins = "http://localhost:4200")`
2. Redémarrer le backend

### Problème : "Invalid password"

**Solution** :

1. Vérifier que le mot de passe respecte tous les critères
2. Utiliser un des exemples valides ci-dessus

---

## 📝 Commandes Utiles

### Nettoyer le LocalStorage

```javascript
// Dans la console du navigateur
localStorage.clear();
```

### Voir les Tokens

```javascript
// Dans la console du navigateur
console.log('Access Token:', localStorage.getItem('accessToken'));
console.log('Refresh Token:', localStorage.getItem('refreshToken'));
```

### Décoder le JWT

```javascript
// Dans la console du navigateur
const token = localStorage.getItem('accessToken');
const payload = JSON.parse(atob(token.split('.')[1]));
console.log('Token Payload:', payload);
console.log('Expiration:', new Date(payload.exp * 1000));
```

### Forcer l'Expiration du Token

```javascript
// Dans la console du navigateur
localStorage.setItem('accessToken', 'invalid_token');
// Puis faire une requête authentifiée
```

---

## ✅ Checklist de Test

### Inscription

- [ ] Inscription avec données valides
- [ ] Inscription avec email existant (409)
- [ ] Inscription avec mot de passe faible
- [ ] Inscription avec mots de passe non correspondants
- [ ] Redirection vers login après succès

### Connexion

- [ ] Connexion avec credentials valides
- [ ] Connexion avec email invalide (401)
- [ ] Connexion avec mot de passe invalide (401)
- [ ] Redirection selon le rôle
- [ ] Tokens stockés dans localStorage

### Profil

- [ ] Accès au profil (authentifié)
- [ ] Modification du profil
- [ ] Changement de mot de passe
- [ ] Validation des champs

### Mot de Passe Oublié

- [ ] Demande de réinitialisation
- [ ] Réinitialisation avec token
- [ ] Validation du nouveau mot de passe
- [ ] Redirection vers login

### Sécurité

- [ ] Rafraîchissement automatique du token
- [ ] Déconnexion en cas d'erreur
- [ ] Protection des routes par AuthGuard
- [ ] Protection des routes par RoleGuard

---

## 🎉 Félicitations !

Si tous les tests passent, votre module d'authentification est **100% fonctionnel** ! 🚀

**Prochaines étapes** :

1. Implémenter les dashboards par rôle
2. Ajouter les fonctionnalités métier (thèses, défenses, etc.)
3. Améliorer l'UX avec des animations
4. Ajouter les tests unitaires et e2e
