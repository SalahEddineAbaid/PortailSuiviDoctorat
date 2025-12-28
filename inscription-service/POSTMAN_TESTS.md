# Guide de test Postman - Inscription Service

## 🚀 Prérequis
- Inscription Service démarré sur **http://localhost:8082**
- Base de données `inscriptiondb` créée
- Eureka Server en cours d'exécution (optionnel)

---

## 📋 CAMPAGNES - CampagneController

### 1. Créer une campagne d'inscription
```
POST http://localhost:8082/api/campagnes
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "libelle": "Campagne Inscription 2025-2026",
  "type": "INSCRIPTION",
  "dateDebut": "2025-09-01",
  "dateFin": "2025-10-31",
  "anneeUniversitaire": 2025
}
```

### 2. Créer une campagne de réinscription
```
POST http://localhost:8082/api/campagnes
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "libelle": "Campagne Réinscription 2025-2026",
  "type": "REINSCRIPTION",
  "dateDebut": "2025-11-01",
  "dateFin": "2025-11-30",
  "anneeUniversitaire": 2025
}
```

### 3. Récupérer toutes les campagnes
```
GET http://localhost:8082/api/campagnes
```

### 4. Récupérer les campagnes actives
```
GET http://localhost:8082/api/campagnes/actives
```

### 5. Récupérer une campagne par ID
```
GET http://localhost:8082/api/campagnes/1
```

### 6. Modifier une campagne
```
PUT http://localhost:8082/api/campagnes/1
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "libelle": "Campagne Inscription 2025-2026 (Modifiée)",
  "type": "INSCRIPTION",
  "dateDebut": "2025-09-01",
  "dateFin": "2025-11-15",
  "anneeUniversitaire": 2025
}
```

### 7. Fermer une campagne
```
PUT http://localhost:8082/api/campagnes/1/fermer
```

---

## 📝 INSCRIPTIONS - InscriptionController

### 1. Créer une première inscription
```
POST http://localhost:8082/api/inscriptions
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "doctorantId": 1,
  "directeurTheseId": 2,
  "campagneId": 1,
  "sujetThese": "Intelligence Artificielle appliquée à la médecine",
  "type": "PREMIERE_INSCRIPTION",
  "anneeInscription": 2024,
  "cin": "AB123456",
  "cne": "R123456789",
  "telephone": "0612345678",
  "adresse": "123 Rue Mohammed V",
  "ville": "Casablanca",
  "pays": "Maroc",
  "dateNaissance": "1995-05-15",
  "lieuNaissance": "Rabat",
  "nationalite": "Marocaine",
  "titreThese": "IA et diagnostic médical automatisé",
  "discipline": "Informatique",
  "laboratoire": "LRIT",
  "etablissementAccueil": "Faculté des Sciences",
  "cotutelle": false,
  "dateDebutPrevue": "2025-09-01"
}
```

### 2. Créer une réinscription
```
POST http://localhost:8082/api/inscriptions
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "doctorantId": 1,
  "directeurTheseId": 2,
  "campagneId": 2,
  "sujetThese": "Intelligence Artificielle appliquée à la médecine",
  "type": "REINSCRIPTION",
  "anneeInscription": 2025,
  "cin": "AB123456",
  "cne": "R123456789",
  "telephone": "0612345678",
  "adresse": "123 Rue Mohammed V",
  "ville": "Casablanca",
  "pays": "Maroc",
  "dateNaissance": "1995-05-15",
  "lieuNaissance": "Rabat",
  "nationalite": "Marocaine",
  "titreThese": "IA et diagnostic médical automatisé",
  "discipline": "Informatique",
  "laboratoire": "LRIT",
  "etablissementAccueil": "Faculté des Sciences",
  "cotutelle": false,
  "dateDebutPrevue": "2025-09-01"
}
```

### 3. Créer une inscription en cotutelle
```
POST http://localhost:8082/api/inscriptions
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "doctorantId": 3,
  "directeurTheseId": 4,
  "campagneId": 1,
  "sujetThese": "Blockchain et sécurité des données",
  "type": "PREMIERE_INSCRIPTION",
  "anneeInscription": 2024,
  "cin": "CD789012",
  "cne": "R987654321",
  "telephone": "0698765432",
  "adresse": "456 Avenue Hassan II",
  "ville": "Rabat",
  "pays": "Maroc",
  "dateNaissance": "1994-08-20",
  "lieuNaissance": "Casablanca",
  "nationalite": "Marocaine",
  "titreThese": "Blockchain pour la sécurité des transactions",
  "discipline": "Informatique",
  "laboratoire": "LISI",
  "etablissementAccueil": "ENSIAS",
  "cotutelle": true,
  "universitePartenaire": "Université Paris-Saclay",
  "paysPartenaire": "France",
  "dateDebutPrevue": "2025-09-01"
}
```

### 4. Récupérer une inscription par ID
```
GET http://localhost:8082/api/inscriptions/1
```

### 5. Récupérer les inscriptions d'un doctorant
```
GET http://localhost:8082/api/inscriptions/doctorant/1
```

### 6. Soumettre une inscription pour validation
```
POST http://localhost:8082/api/inscriptions/1/soumettre?doctorantId=1
```

### 7. Récupérer les inscriptions en attente pour un directeur
```
GET http://localhost:8082/api/inscriptions/directeur/2/en-attente
```

### 8. Valider une inscription par le directeur (APPROUVÉE)
```
POST http://localhost:8082/api/inscriptions/1/valider-directeur?directeurId=2
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "inscriptionId": 1,
  "approuve": true,
  "commentaire": "Dossier complet et conforme. Le sujet de thèse est pertinent et bien défini."
}
```

### 9. Valider une inscription par le directeur (REJETÉE)
```
POST http://localhost:8082/api/inscriptions/1/valider-directeur?directeurId=2
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "inscriptionId": 1,
  "approuve": false,
  "commentaire": "Le sujet de thèse nécessite plus de précisions. Veuillez revoir la problématique."
}
```

### 10. Récupérer les inscriptions en attente pour l'administration
```
GET http://localhost:8082/api/inscriptions/admin/en-attente
```

### 11. Valider une inscription par l'administration (APPROUVÉE)
```
POST http://localhost:8082/api/inscriptions/1/valider-admin
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "inscriptionId": 1,
  "approuve": true,
  "commentaire": "Inscription validée. Tous les documents sont conformes."
}
```

### 12. Valider une inscription par l'administration (REJETÉE)
```
POST http://localhost:8082/api/inscriptions/1/valider-admin
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "inscriptionId": 1,
  "approuve": false,
  "commentaire": "Documents manquants : diplôme de master non fourni."
}
```

---

## 📎 DOCUMENTS - DocumentController

### 1. Upload un document (Diplôme Master)
```
POST http://localhost:8082/api/documents/1/upload
Content-Type: multipart/form-data
```

**Form Data:**
- `file`: [Sélectionner un fichier PDF]
- `typeDocument`: `DIPLOME_MASTER`

### 2. Upload un document (CV)
```
POST http://localhost:8082/api/documents/1/upload
Content-Type: multipart/form-data
```

**Form Data:**
- `file`: [Sélectionner un fichier PDF]
- `typeDocument`: `CV`

### 3. Upload un document (Lettre de motivation)
```
POST http://localhost:8082/api/documents/1/upload
Content-Type: multipart/form-data
```

**Form Data:**
- `file`: [Sélectionner un fichier PDF]
- `typeDocument`: `LETTRE_MOTIVATION`

### 4. Upload un document (Projet de recherche)
```
POST http://localhost:8082/api/documents/1/upload
Content-Type: multipart/form-data
```

**Form Data:**
- `file`: [Sélectionner un fichier PDF]
- `typeDocument`: `PROJET_RECHERCHE`

### 5. Récupérer tous les documents d'une inscription
```
GET http://localhost:8082/api/documents/1
```

### 6. Télécharger un document
```
GET http://localhost:8082/api/documents/download/1
```

### 7. Supprimer un document
```
DELETE http://localhost:8082/api/documents/1
```

---

## 🔄 Scénario de test complet

### Étape 1 : Créer une campagne
1. Créer une campagne d'inscription (endpoint 1 des campagnes)
2. Noter l'ID retourné (ex: `campagneId = 1`)

### Étape 2 : Créer une inscription
1. Créer une inscription (endpoint 1 des inscriptions)
2. Noter l'ID retourné (ex: `inscriptionId = 1`)

### Étape 3 : Uploader des documents
1. Upload diplôme master
2. Upload CV
3. Upload lettre de motivation
4. Upload projet de recherche

### Étape 4 : Soumettre l'inscription
1. Soumettre l'inscription (endpoint 6 des inscriptions)

### Étape 5 : Validation directeur
1. Récupérer les inscriptions en attente directeur
2. Valider l'inscription (approuvée)

### Étape 6 : Validation administration
1. Récupérer les inscriptions en attente admin
2. Valider l'inscription (approuvée)

### Étape 7 : Vérifications
1. Récupérer l'inscription par ID
2. Vérifier que le statut est `VALIDEE`

---

## 📊 Types de données

### Types d'inscription
- `PREMIERE_INSCRIPTION`
- `REINSCRIPTION`

### Types de campagne
- `INSCRIPTION`
- `REINSCRIPTION`

### Types de document
- `DIPLOME_MASTER`
- `CV`
- `LETTRE_MOTIVATION`
- `PROJET_RECHERCHE`
- `ATTESTATION_INSCRIPTION`
- `AUTRE`

### Statuts d'inscription
- `BROUILLON` - Inscription en cours de création
- `SOUMIS` - Soumise pour validation
- `EN_ATTENTE_DIRECTEUR` - En attente validation directeur
- `VALIDEE_DIRECTEUR` - Validée par le directeur
- `REJETEE_DIRECTEUR` - Rejetée par le directeur
- `EN_ATTENTE_ADMIN` - En attente validation admin
- `VALIDEE` - Validée définitivement
- `REJETEE` - Rejetée définitivement

---

## 💡 Conseils Postman

1. **Créer une collection** : Organisez vos requêtes par contrôleur
2. **Variables d'environnement** :
   - `baseUrl` = `http://localhost:8082`
   - `campagneId` = ID de la campagne créée
   - `inscriptionId` = ID de l'inscription créée
3. **Tests automatiques** : Ajoutez des scripts pour extraire les IDs
4. **Sauvegarde** : Exportez votre collection pour la réutiliser

---

## ⚠️ Notes importantes

- Les dates doivent être au format ISO 8601 : `YYYY-MM-DDTHH:mm:ss`
- Le téléphone doit contenir exactement 10 chiffres
- La date de naissance doit être dans le passé
- La date de début prévue doit être dans le futur
- Pour les uploads, utilisez des fichiers PDF de moins de 10MB
