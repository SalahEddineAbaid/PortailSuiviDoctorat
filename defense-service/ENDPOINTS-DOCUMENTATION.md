# 📚 Documentation Complète des Endpoints - Defense Service

## 🎯 Vue d'Ensemble

Le Defense Service gère tout le processus de soutenance de thèse, de la demande initiale jusqu'à l'évaluation finale. Il est composé de **9 controllers** avec **27 endpoints**.

---

## 📋 Table des Matières

1. [TestController](#1-testcontroller) - Tests de connectivité
2. [PrerequisitesController](#2-prerequisitescontroller) - Gestion des prérequis
3. [DefenseRequestController](#3-defenserequestcontroller) - Demandes de défense
4. [JuryController](#4-jurycontroller) - Gestion des jurys
5. [JuryMemberController](#5-jurymembercontroller) - Membres du jury
6. [DefenseController](#6-defensecontroller) - Planification des soutenances
7. [DocumentController](#7-documentcontroller) - Gestion des documents
8. [RapportController](#8-rapportcontroller) - Rapports d'évaluation

---

## 1. TestController

**Base URL:** `/api/defense-service/test`

**Objectif:** Vérifier la santé du service et la communication avec user-service

### 1.1 Health Check

```
GET /api/defense-service/test/health
```

**Objectif:** Vérifier que le defense-service est opérationnel

**Réponse:**

```json
{
  "status": "UP",
  "service": "defense-service"
}
```

**Utilisation:** Monitoring, tests de déploiement

---

### 1.2 Test Communication User Service

```
GET /api/defense-service/test/user/{id}
```

**Objectif:** Tester la communication Feign avec user-service via Eureka

**Paramètres:**

- `id` (path) - ID de l'utilisateur à récupérer

**Réponse Succès:**

```json
{
  "success": true,
  "message": "Communication avec user-service réussie",
  "user": {
    "id": 1,
    "email": "ahmed@emsi.ma",
    "firstName": "Ahmed",
    "lastName": "Bennani",
    "roles": ["ROLE_DOCTORANT"]
  }
}
```

**Réponse Erreur:**

```json
{
  "success": false,
  "message": "Erreur de communication avec user-service",
  "error": "...",
  "errorType": "..."
}
```

**Utilisation:** Debugging, vérification de la découverte de service

---

## 2. PrerequisitesController

**Base URL:** `/api/defense-service/prerequisites`

**Objectif:** Gérer les prérequis nécessaires avant de soumettre une demande de défense

### 2.1 Créer des Prérequis

```
POST /api/defense-service/prerequisites
```

**Objectif:** Enregistrer les prérequis d'un doctorant (publications, participations, approbations)

**Body:**

```json
{
  "publicationsCount": 3,
  "conferenceParticipations": 2,
  "hasDirectorApproval": true,
  "hasEthicsCommitteeApproval": true,
  "minimumDurationMet": true
}
```

**Réponse (201 Created):**

```json
{
  "id": 1,
  "publicationsCount": 3,
  "conferenceParticipations": 2,
  "hasDirectorApproval": true,
  "hasEthicsCommitteeApproval": true,
  "minimumDurationMet": true,
  "validated": false,
  "validationDate": null
}
```

**Utilisation:** Le doctorant ou le directeur enregistre les prérequis avant de faire une demande

---

### 2.2 Récupérer des Prérequis

```
GET /api/defense-service/prerequisites/{id}
```

**Objectif:** Consulter les prérequis d'un doctorant

**Paramètres:**

- `id` (path) - ID des prérequis

**Utilisation:** Vérification par le directeur ou l'administration

---

### 2.3 Valider/Rejeter des Prérequis

```
PATCH /api/defense-service/prerequisites/{id}/validate?valid=true
```

**Objectif:** Valider ou rejeter les prérequis par le directeur ou l'administration

**Paramètres:**

- `id` (path) - ID des prérequis
- `valid` (query) - `true` pour valider, `false` pour rejeter

**Réponse:**

```json
{
  "id": 1,
  "publicationsCount": 3,
  "conferenceParticipations": 2,
  "hasDirectorApproval": true,
  "hasEthicsCommitteeApproval": true,
  "minimumDurationMet": true,
  "validated": true,
  "validationDate": "2025-11-25T10:30:00"
}
```

**Utilisation:** Le directeur valide que le doctorant remplit les conditions

---

## 3. DefenseRequestController

**Base URL:** `/api/defense-service/defense-requests`

**Objectif:** Gérer les demandes de soutenance de thèse

### 3.1 Créer une Demande de Défense

```
POST /api/defense-service/defense-requests
```

**Objectif:** Soumettre une demande de soutenance

**Body:**

```json
{
  "doctorantId": 1,
  "prerequisitesId": 1
}
```

**Validations Automatiques:**

- ✅ Vérifie que le doctorant existe dans user-service
- ✅ Vérifie que l'utilisateur a le rôle ROLE_DOCTORANT
- ✅ Vérifie que les prérequis existent (si fournis)
- ✅ Définit automatiquement la date de soumission
- ✅ Définit le statut à PENDING

**Réponse (201 Created):**

```json
{
  "id": 1,
  "doctorantId": 1,
  "doctorantFirstName": "Ahmed",
  "doctorantLastName": "Bennani",
  "doctorantEmail": "ahmed@emsi.ma",
  "submissionDate": "2025-11-25T10:46:31",
  "status": "PENDING",
  "prerequisitesId": 1,
  "documentsCount": 0,
  "rapportsCount": 0
}
```

**Utilisation:** Le doctorant soumet sa demande de soutenance

---

### 3.2 Récupérer une Demande

```
GET /api/defense-service/defense-requests/{id}
```

**Objectif:** Consulter les détails d'une demande spécifique

**Enrichissement Automatique:**

- Récupère les infos du doctorant depuis user-service
- Compte les documents associés
- Compte les rapports d'évaluation

**Utilisation:** Consultation par le doctorant, directeur, ou administration

---

### 3.3 Lister Toutes les Demandes

```
GET /api/defense-service/defense-requests
```

**Objectif:** Obtenir la liste de toutes les demandes de défense

**Réponse:**

```json
[
  {
    "id": 1,
    "doctorantId": 1,
    "doctorantFirstName": "Ahmed",
    "doctorantLastName": "Bennani",
    "status": "PENDING",
    ...
  },
  {
    "id": 2,
    "doctorantId": 2,
    "doctorantFirstName": "Salah",
    "status": "APPROVED",
    ...
  }
]
```

**Utilisation:** Dashboard administratif, liste des demandes en attente

---

### 3.4 Mettre à Jour le Statut

```
PATCH /api/defense-service/defense-requests/{id}/status?status=APPROVED
```

**Objectif:** Changer le statut d'une demande

**Paramètres:**

- `id` (path) - ID de la demande
- `status` (query) - Nouveau statut

**Statuts Possibles:**

- `PENDING` - En attente de traitement
- `APPROVED` - Approuvée par le directeur/administration
- `REJECTED` - Rejetée
- `SCHEDULED` - Soutenance planifiée
- `COMPLETED` - Soutenance terminée

**Utilisation:** Le directeur approuve ou rejette une demande

---

### 3.5 Supprimer une Demande

```
DELETE /api/defense-service/defense-requests/{id}
```

**Objectif:** Supprimer une demande (annulation)

**Réponse:** 204 No Content

**Utilisation:** Le doctorant annule sa demande avant traitement

---

## 4. JuryController

**Base URL:** `/api/defense-service/juries`

**Objectif:** Gérer la composition et le statut des jurys de soutenance

### 4.1 Créer un Jury

```
POST /api/defense-service/juries
```

**Objectif:** Proposer un jury pour une demande de défense

**Body:**

```json
{
  "defenseRequestId": 1,
  "proposedDate": "2025-06-15T10:00:00",
  "status": "PROPOSED"
}
```

**Réponse (201 Created):**

```json
{
  "id": 1,
  "defenseRequestId": 1,
  "proposedDate": "2025-06-15T10:00:00",
  "status": "PROPOSED",
  "members": []
}
```

**Utilisation:** Le directeur propose un jury après approbation de la demande

---

### 4.2 Récupérer le Jury d'une Demande

```
GET /api/defense-service/juries/defense-request/{defenseRequestId}
```

**Objectif:** Consulter le jury associé à une demande (avec ses membres)

**Réponse:**

```json
{
  "id": 1,
  "defenseRequestId": 1,
  "proposedDate": "2025-06-15T10:00:00",
  "status": "CONFIRMED",
  "members": [
    {
      "id": 1,
      "professorId": 10,
      "role": "PRESIDENT",
      "status": "ACCEPTED"
    },
    {
      "id": 2,
      "professorId": 11,
      "role": "RAPPORTEUR",
      "status": "ACCEPTED"
    }
  ]
}
```

**Utilisation:** Consultation de la composition du jury

---

### 4.3 Mettre à Jour le Statut du Jury

```
PATCH /api/defense-service/juries/{id}/status?status=CONFIRMED
```

**Objectif:** Changer le statut du jury

**Statuts Possibles:**

- `PROPOSED` - Jury proposé, en attente de confirmation
- `CONFIRMED` - Tous les membres ont accepté
- `REJECTED` - Jury rejeté, besoin de recomposition
- `COMPLETED` - Soutenance terminée

**Utilisation:** Mise à jour automatique ou manuelle du statut

---

## 5. JuryMemberController

**Base URL:** `/api/defense-service/jury-members`

**Objectif:** Gérer les membres individuels d'un jury

### 5.1 Ajouter un Membre au Jury

```
POST /api/defense-service/jury-members
```

**Objectif:** Inviter un professeur à faire partie du jury

**Body:**

```json
{
  "juryId": 1,
  "professorId": 10,
  "role": "PRESIDENT",
  "status": "INVITED"
}
```

**Rôles Possibles:**

- `PRESIDENT` - Président du jury
- `RAPPORTEUR` - Rapporteur (évalue la thèse en détail)
- `EXAMINATEUR` - Examinateur

**Réponse (201 Created):**

```json
{
  "id": 1,
  "juryId": 1,
  "professorId": 10,
  "role": "PRESIDENT",
  "status": "INVITED",
  "invitationDate": "2025-11-25T10:45:00",
  "responseDate": null
}
```

**Utilisation:** Le directeur compose le jury en invitant des professeurs

---

### 5.2 Récupérer les Membres d'un Jury

```
GET /api/defense-service/jury-members/jury/{juryId}
```

**Objectif:** Lister tous les membres d'un jury spécifique

**Réponse:**

```json
[
  {
    "id": 1,
    "juryId": 1,
    "professorId": 10,
    "role": "PRESIDENT",
    "status": "ACCEPTED"
  },
  {
    "id": 2,
    "juryId": 1,
    "professorId": 11,
    "role": "RAPPORTEUR",
    "status": "INVITED"
  }
]
```

**Utilisation:** Voir la composition complète du jury

---

### 5.3 Mettre à Jour le Statut d'un Membre

```
PATCH /api/defense-service/jury-members/{id}/status?status=ACCEPTED
```

**Objectif:** Enregistrer la réponse d'un professeur à l'invitation

**Statuts Possibles:**

- `INVITED` - Invitation envoyée, en attente de réponse
- `ACCEPTED` - Professeur a accepté
- `DECLINED` - Professeur a refusé

**Utilisation:** Le professeur accepte ou refuse l'invitation

---

## 6. DefenseController

**Base URL:** `/api/defense-service/defenses`

**Objectif:** Planifier et gérer les soutenances

### 6.1 Planifier une Soutenance

```
POST /api/defense-service/defenses
```

**Objectif:** Fixer la date, l'heure et le lieu de la soutenance

**Body:**

```json
{
  "defenseRequestId": 1,
  "scheduledDate": "2025-06-15T10:00:00",
  "location": "Amphithéâtre A, Bâtiment Sciences",
  "status": "SCHEDULED"
}
```

**Réponse (201 Created):**

```json
{
  "id": 1,
  "defenseRequestId": 1,
  "scheduledDate": "2025-06-15T10:00:00",
  "location": "Amphithéâtre A, Bâtiment Sciences",
  "status": "SCHEDULED",
  "actualStartTime": null,
  "actualEndTime": null
}
```

**Utilisation:** L'administration planifie la soutenance après confirmation du jury

---

### 6.2 Récupérer la Soutenance d'une Demande

```
GET /api/defense-service/defenses/defense-request/{requestId}
```

**Objectif:** Consulter les détails de planification d'une soutenance

**Utilisation:** Le doctorant consulte la date et le lieu de sa soutenance

---

### 6.3 Mettre à Jour le Statut de la Soutenance

```
PATCH /api/defense-service/defenses/{id}/status?status=IN_PROGRESS
```

**Objectif:** Suivre l'avancement de la soutenance

**Statuts Possibles:**

- `SCHEDULED` - Planifiée, en attente
- `IN_PROGRESS` - Soutenance en cours
- `COMPLETED` - Soutenance terminée
- `CANCELLED` - Annulée

**Utilisation:** Mise à jour en temps réel du statut

---

## 7. DocumentController

**Base URL:** `/api/defense-service/documents`

**Objectif:** Gérer les documents liés à une demande de défense

### 7.1 Uploader un Document

```
POST /api/defense-service/documents
```

**Objectif:** Enregistrer un document (thèse, articles, présentation)

**Body:**

```json
{
  "defenseRequestId": 1,
  "documentType": "THESIS",
  "fileName": "these_ahmed_bennani.pdf",
  "fileUrl": "https://storage.example.com/documents/these.pdf",
  "fileSize": 5242880
}
```

**Types de Documents:**

- `THESIS` - Manuscrit de thèse
- `ARTICLE` - Article de publication
- `PRESENTATION` - Présentation PowerPoint
- `RAPPORT` - Rapport d'avancement
- `OTHER` - Autre document

**Réponse (201 Created):**

```json
{
  "id": 1,
  "defenseRequestId": 1,
  "documentType": "THESIS",
  "fileName": "these_ahmed_bennani.pdf",
  "fileUrl": "https://storage.example.com/documents/these.pdf",
  "fileSize": 5242880,
  "uploadDate": "2025-11-25T11:00:00"
}
```

**Utilisation:** Le doctorant upload sa thèse et documents annexes

---

### 7.2 Récupérer les Documents d'une Demande

```
GET /api/defense-service/documents/defense-request/{requestId}
```

**Objectif:** Lister tous les documents associés à une demande

**Réponse:**

```json
[
  {
    "id": 1,
    "documentType": "THESIS",
    "fileName": "these_ahmed_bennani.pdf",
    "fileSize": 5242880,
    "uploadDate": "2025-11-25T11:00:00"
  },
  {
    "id": 2,
    "documentType": "ARTICLE",
    "fileName": "article_1.pdf",
    "fileSize": 1048576,
    "uploadDate": "2025-11-25T11:05:00"
  }
]
```

**Utilisation:** Le jury consulte les documents avant la soutenance

---

### 7.3 Supprimer un Document

```
DELETE /api/defense-service/documents/{id}
```

**Objectif:** Supprimer un document (correction, remplacement)

**Réponse:** 204 No Content

**Utilisation:** Le doctorant remplace un document incorrect

---

## 8. RapportController

**Base URL:** `/api/defense-service/rapports`

**Objectif:** Gérer les rapports d'évaluation des membres du jury

### 8.1 Soumettre un Rapport

```
POST /api/defense-service/rapports
```

**Objectif:** Un membre du jury soumet son évaluation

**Body:**

```json
{
  "defenseRequestId": 1,
  "evaluatorId": 10,
  "evaluatorRole": "PRESIDENT",
  "technicalScore": 18.5,
  "presentationScore": 17.0,
  "comments": "Excellent travail de recherche. Présentation claire.",
  "recommendation": "ACCEPTED"
}
```

**Recommendations Possibles:**

- `ACCEPTED` - Thèse acceptée sans réserve
- `ACCEPTED_WITH_MINOR_REVISIONS` - Acceptée avec corrections mineures
- `ACCEPTED_WITH_MAJOR_REVISIONS` - Acceptée avec corrections majeures
- `REJECTED` - Thèse rejetée

**Réponse (201 Created):**

```json
{
  "id": 1,
  "defenseRequestId": 1,
  "evaluatorId": 10,
  "evaluatorRole": "PRESIDENT",
  "technicalScore": 18.5,
  "presentationScore": 17.0,
  "comments": "Excellent travail de recherche.",
  "recommendation": "ACCEPTED",
  "submissionDate": "2025-11-25T11:10:00"
}
```

**Utilisation:** Chaque membre du jury soumet son rapport après la soutenance

---

### 8.2 Récupérer les Rapports d'une Demande

```
GET /api/defense-service/rapports/defense-request/{defenseRequestId}
```

**Objectif:** Consulter tous les rapports d'évaluation

**Réponse:**

```json
[
  {
    "id": 1,
    "evaluatorId": 10,
    "evaluatorRole": "PRESIDENT",
    "technicalScore": 18.5,
    "presentationScore": 17.0,
    "recommendation": "ACCEPTED",
    "submissionDate": "2025-11-25T11:10:00"
  },
  {
    "id": 2,
    "evaluatorId": 11,
    "evaluatorRole": "RAPPORTEUR",
    "technicalScore": 17.0,
    "presentationScore": 16.5,
    "recommendation": "ACCEPTED_WITH_MINOR_REVISIONS",
    "submissionDate": "2025-11-25T11:15:00"
  }
]
```

**Utilisation:** L'administration compile les évaluations pour la décision finale

---

## 🔄 Workflow Complet

Voici le processus complet d'une soutenance :

```
1. PRÉREQUIS
   POST /prerequisites
   PATCH /prerequisites/{id}/validate

2. DEMANDE
   POST /defense-requests
   PATCH /defense-requests/{id}/status → APPROVED

3. JURY
   POST /juries
   POST /jury-members (×3: Président, Rapporteur, Examinateur)
   PATCH /jury-members/{id}/status → ACCEPTED
   PATCH /juries/{id}/status → CONFIRMED

4. DOCUMENTS
   POST /documents (Thèse)
   POST /documents (Articles)
   POST /documents (Présentation)

5. PLANIFICATION
   POST /defenses
   PATCH /defenses/{id}/status → SCHEDULED

6. SOUTENANCE
   PATCH /defenses/{id}/status → IN_PROGRESS
   PATCH /defenses/{id}/status → COMPLETED

7. ÉVALUATION
   POST /rapports (Président)
   POST /rapports (Rapporteur)
   POST /rapports (Examinateur)

8. FINALISATION
   PATCH /defense-requests/{id}/status → COMPLETED
```

---

## 📊 Résumé des Endpoints

| Controller               | Endpoints | Objectif Principal            |
| ------------------------ | --------- | ----------------------------- |
| TestController           | 2         | Tests et monitoring           |
| PrerequisitesController  | 3         | Validation des prérequis      |
| DefenseRequestController | 5         | Gestion des demandes          |
| JuryController           | 3         | Composition des jurys         |
| JuryMemberController     | 3         | Gestion des membres           |
| DefenseController        | 3         | Planification des soutenances |
| DocumentController       | 3         | Gestion documentaire          |
| RapportController        | 2         | Évaluations                   |
| **TOTAL**                | **27**    | **Processus complet**         |

---

## 🔐 Sécurité

**Note:** Actuellement, les endpoints ne sont pas sécurisés. En production, vous devriez :

1. Ajouter Spring Security
2. Implémenter l'authentification JWT
3. Définir les autorisations par rôle :
   - `ROLE_DOCTORANT` : Créer demandes, uploader documents
   - `ROLE_DIRECTEUR` : Valider prérequis, composer jurys, approuver demandes
   - `ROLE_ADMIN` : Tous les droits
   - Membres du jury : Soumettre rapports

---

## 🎯 Bonnes Pratiques

1. **Ordre des opérations** : Respectez le workflow (prérequis → demande → jury → soutenance)
2. **Validation** : Tous les DTOs sont validés avec `@Valid`
3. **Enrichissement** : Les réponses sont enrichies avec les données du user-service
4. **Statuts** : Utilisez les enums pour les statuts (pas de strings libres)
5. **Cascade** : La suppression d'une demande supprime les entités liées

---

Voilà ! Vous avez maintenant une documentation complète de tous les endpoints du Defense Service ! 🚀
