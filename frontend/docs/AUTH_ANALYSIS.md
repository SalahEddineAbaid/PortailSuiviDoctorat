# Analyse du User-Service - Authentification Backend

## 📋 Vue d'ensemble

Le user-service expose une API REST complète pour l'authentification et la gestion des utilisateurs, avec sécurité JWT, gestion des rôles, et refresh tokens.

---

## 🔌 Endpoints API

### AuthController (`/api/auth`)

#### 1. POST `/api/auth/register` - Inscription

**Request Body:**

```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+212600000000",
  "adresse": "123 Rue Example",
  "ville": "Casablanca",
  "pays": "Maroc",
  "roles": ["ROLE_DOCTORANT"]
}
```

**Validations:**

- Email: format valide, obligatoire
- Password: 12-64 caractères, doit contenir majuscule, minuscule, chiffre et caractère spécial
- Tous les champs sont obligatoires sauf `roles`

**Response Success (201):**

```json
{
  "id": 1,
  "email": "user@example.com",
  "FirstName": "John",
  "LastName": "Doe",
  "phoneNumber": "+212600000000",
  "adresse": "123 Rue Example",
  "ville": "Casablanca",
  "pays": "Maroc",
  "roles": ["ROLE_DOCTORANT"]
}
```

**Response Error (409 - Conflict):**

```json
{
  "error": "Un utilisateur avec cet email existe déjà"
}
```

---

#### 2. POST `/api/auth/login` - Connexion

**Request Body:**

```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response Success (200):**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**JWT Access Token Claims:**

```json
{
  "userId": 1,
  "email": "user@example.com",
  "roles": ["ROLE_DOCTORANT"],
  "iat": 1234567890,
  "exp": 1234654290
}
```

**Response Error (401):**

```json
{
  "error": "Identifiants invalides"
}
```

---

#### 3. POST `/api/auth/refresh` - Rafraîchir le token

**Request Body:**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response Success (200):**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response Error (401):**

```json
{
  "error": "Refresh token invalide ou expiré"
}
```

---

### UserController (`/api/users`)

#### 4. GET `/api/users/profile` - Récupérer le profil

**Headers:** `Authorization: Bearer <accessToken>`

**Response Success (200):**

```json
{
  "id": 1,
  "email": "user@example.com",
  "FirstName": "John",
  "LastName": "Doe",
  "phoneNumber": "+212600000000",
  "adresse": "123 Rue Example",
  "ville": "Casablanca",
  "pays": "Maroc",
  "roles": ["ROLE_DOCTORANT"]
}
```

---

#### 5. PUT `/api/users/profile` - Mettre à jour le profil

**Headers:** `Authorization: Bearer <accessToken>`

**Request Body:**

```json
{
  "firstName": "John Updated",
  "lastName": "Doe",
  "phoneNumber": "+212611111111",
  "adresse": "456 New Address",
  "ville": "Rabat",
  "pays": "Maroc"
}
```

**Response Success (200):** UserResponse

---

#### 6. POST `/api/users/change-password` - Changer le mot de passe

**Headers:** `Authorization: Bearer <accessToken>`

**Request Body:**

```json
{
  "oldPassword": "OldSecurePass123!",
  "newPassword": "NewSecurePass456!"
}
```

**Validations:**

- oldPassword: obligatoire
- newPassword: minimum 6 caractères, obligatoire

**Response Success (200):**

```json
{
  "message": "Mot de passe modifié avec succès"
}
```

---

#### 7. POST `/api/users/forgot-password` - Mot de passe oublié

**Request Body:**

```json
{
  "email": "user@example.com"
}
```

**Response Success (200):**

```json
{
  "message": "Si l'email existe, un lien de réinitialisation a été envoyé"
}
```

---

#### 8. POST `/api/users/reset-password` - Réinitialiser le mot de passe

**Request Body:**

```json
{
  "token": "reset-token-from-email",
  "newPassword": "NewSecurePass789!"
}
```

**Response Success (200):**

```json
{
  "message": "Mot de passe réinitialisé avec succès"
}
```

---

#### 9. POST `/api/users/logout` - Déconnexion

**Headers:** `Authorization: Bearer <accessToken>`

**Response Success (200):**

```json
{
  "message": "Déconnexion réussie"
}
```

---

## 🔐 Sécurité JWT

### Configuration

- **Secret Key:** Configuré via `jwt.secret` (minimum 256 bits pour HS256)
- **Access Token Expiration:** `jwt.expiration` (par défaut 24h = 86400000ms)
- **Refresh Token Expiration:** `jwt.refresh-expiration` (par défaut 7 jours = 604800000ms)
- **Algorithm:** HS256 (HMAC with SHA-256)

### Structure du Token

```
Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload (Access Token):
{
  "userId": 1,
  "email": "user@example.com",
  "roles": ["ROLE_DOCTORANT"],
  "sub": "user@example.com",
  "iat": 1234567890,
  "exp": 1234654290
}

Payload (Refresh Token):
{
  "userId": 1,
  "tokenType": "refresh",
  "sub": "user@example.com",
  "iat": 1234567890,
  "exp": 1235172090
}
```

---

## 👥 Rôles Disponibles

```java
enum RoleName {
    ROLE_DOCTORANT,        // Étudiant doctorant
    ROLE_DIRECTEUR,        // Directeur de thèse
    ROLE_ADMIN,            // Administrateur système
    ROLE_DOCTORANT_ACTIF   // Doctorant avec inscription active
}
```

---

## 🚨 Gestion des Erreurs

### Codes HTTP

- **200 OK:** Succès
- **201 Created:** Ressource créée (inscription)
- **400 Bad Request:** Validation échouée
- **401 Unauthorized:** Non authentifié ou token invalide
- **403 Forbidden:** Accès refusé (rôle insuffisant)
- **409 Conflict:** Ressource déjà existante (email dupliqué)
- **500 Internal Server Error:** Erreur serveur

### Format des erreurs

```json
{
  "error": "Message d'erreur descriptif"
}
```

ou

```json
{
  "message": "Message d'erreur",
  "status": 400,
  "timestamp": "2026-01-01T12:00:00Z"
}
```

---

## 🔄 Flux d'Authentification

### 1. Inscription

```
Client → POST /api/auth/register
       ← 201 Created + UserResponse
```

### 2. Connexion

```
Client → POST /api/auth/login
       ← 200 OK + { accessToken, refreshToken }

Client stocke les tokens (localStorage/sessionStorage)
```

### 3. Requêtes authentifiées

```
Client → GET /api/users/profile
         Header: Authorization: Bearer <accessToken>
       ← 200 OK + UserResponse
```

### 4. Refresh du token

```
Client → POST /api/auth/refresh
         Body: { refreshToken }
       ← 200 OK + { accessToken, refreshToken }

Client met à jour les tokens stockés
```

### 5. Déconnexion

```
Client → POST /api/users/logout
         Header: Authorization: Bearer <accessToken>
       ← 200 OK

Client supprime les tokens du storage
```

---

## 📝 Validations Côté Backend

### Email

- Format valide (regex email)
- Obligatoire
- Unique dans la base de données

### Password (Inscription)

- Longueur: 12-64 caractères
- Au moins une majuscule
- Au moins une minuscule
- Au moins un chiffre
- Au moins un caractère spécial (@$!%\*?&.)
- Pattern: `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.])[A-Za-z\d@$!%*?&.]{12,}$`

### Password (Changement)

- Minimum 6 caractères
- Ancien mot de passe correct

### Autres champs

- firstName, lastName, phoneNumber, adresse, ville, pays: obligatoires et non vides

---

## 🎯 Points Clés pour le Frontend

1. **Stockage des tokens:** Utiliser localStorage ou sessionStorage
2. **Interceptor HTTP:** Ajouter automatiquement le header `Authorization: Bearer <token>`
3. **Refresh automatique:** Intercepter les 401 et tenter un refresh avant de déconnecter
4. **Validation côté client:** Reproduire les validations backend pour UX
5. **Gestion des rôles:** Stocker les rôles avec le token pour le routing conditionnel
6. **Décodage JWT:** Utiliser une lib comme `jwt-decode` pour extraire les claims
7. **Guards:** Protéger les routes selon l'authentification et les rôles
8. **CORS:** Le backend accepte `http://localhost:4200` avec credentials

---

## 🔗 URLs de Base

- **Development:** `http://localhost:8081` (user-service direct)
- **Via Gateway:** `http://localhost:8080/api/users` (recommandé en production)

---

## 📊 Diagramme de Séquence - Login

```
┌──────┐          ┌─────────┐          ┌──────────┐          ┌──────────┐
│Client│          │Frontend │          │  Gateway │          │User-Svc  │
└──┬───┘          └────┬────┘          └────┬─────┘          └────┬─────┘
   │                   │                    │                     │
   │ Enter credentials │                    │                     │
   ├──────────────────>│                    │                     │
   │                   │                    │                     │
   │                   │ POST /api/auth/login                     │
   │                   ├───────────────────>│                     │
   │                   │                    │                     │
   │                   │                    │ Forward request     │
   │                   │                    ├────────────────────>│
   │                   │                    │                     │
   │                   │                    │  Validate credentials
   │                   │                    │  Generate JWT       │
   │                   │                    │<────────────────────┤
   │                   │                    │                     │
   │                   │ { accessToken, refreshToken }            │
   │                   │<───────────────────┤                     │
   │                   │                    │                     │
   │  Store tokens     │                    │                     │
   │  Redirect to dashboard                 │                     │
   │<──────────────────┤                    │                     │
   │                   │                    │                     │
```

---

## ✅ Checklist d'Implémentation Frontend

- [ ] Créer les modèles TypeScript (interfaces)
- [ ] Implémenter AuthService avec toutes les méthodes
- [ ] Créer l'HTTP Interceptor pour JWT
- [ ] Implémenter AuthGuard et RoleGuard
- [ ] Créer les composants Login, Register, Profile, ForgotPassword
- [ ] Implémenter la validation des formulaires
- [ ] Gérer le stockage des tokens
- [ ] Implémenter le refresh automatique des tokens
- [ ] Créer les messages d'erreur utilisateur-friendly
- [ ] Tester tous les flux d'authentification
