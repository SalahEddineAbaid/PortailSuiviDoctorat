# Requirements Document - Portail de Suivi du Doctorat Frontend

## Introduction

Le Portail de Suivi du Doctorat est une application web destinée à la gestion complète du parcours doctoral, depuis l'inscription jusqu'à la soutenance. Ce document définit les exigences pour le développement du frontend Angular qui consommera les APIs REST existantes du backend microservices Spring Boot.

## Glossary

- **Portail_Frontend**: L'application Angular qui fournit l'interface utilisateur
- **Backend_API**: Les microservices Spring Boot exposés via Spring Cloud Gateway
- **JWT_Token**: Token d'authentification JSON Web Token
- **Doctorant**: Étudiant en doctorat utilisant le système
- **Directeur_These**: Encadrant académique supervisant un ou plusieurs doctorants
- **Administrateur**: Utilisateur avec privilèges de gestion du système
- **Campagne_Inscription**: Période définie pour les inscriptions/réinscriptions
- **Dossier_Soutenance**: Ensemble des documents et informations pour une demande de soutenance
- **Dashboard**: Tableau de bord personnalisé selon le rôle utilisateur
- **Guard**: Mécanisme de protection des routes Angular
- **Interceptor**: Middleware pour traiter les requêtes HTTP

## Requirements

### Requirement 1 - Authentification et Sécurité

**User Story:** En tant qu'utilisateur du portail, je veux pouvoir me connecter de manière sécurisée avec mon rôle approprié, afin d'accéder aux fonctionnalités qui me sont autorisées.

#### Acceptance Criteria

1. WHEN un utilisateur saisit ses identifiants valides, THE Portail_Frontend SHALL authentifier l'utilisateur via Backend_API et stocker le JWT_Token
2. WHEN un JWT_Token expire, THE Portail_Frontend SHALL rediriger automatiquement vers la page de connexion
3. WHEN un utilisateur accède à une route protégée sans authentification, THE Portail_Frontend SHALL rediriger vers la page de connexion
4. WHERE un utilisateur possède le rôle Doctorant, THE Portail_Frontend SHALL autoriser l'accès aux routes /doctorant/*
5. WHERE un utilisateur possède le rôle Directeur_These, THE Portail_Frontend SHALL autoriser l'accès aux routes /directeur/*

### Requirement 2 - Inscription des Doctorants

**User Story:** En tant que doctorant, je veux pouvoir créer mon compte et soumettre ma demande d'inscription avec tous les documents requis, afin d'être enregistré dans le système.

#### Acceptance Criteria

1. WHEN un nouveau doctorant accède au formulaire d'inscription, THE Portail_Frontend SHALL afficher tous les champs obligatoires (informations personnelles, sujet de thèse, directeur, laboratoire)
2. WHEN un doctorant upload un document, THE Portail_Frontend SHALL valider le format (PDF/JPG) et la taille maximale
3. WHEN un doctorant soumet son dossier complet, THE Portail_Frontend SHALL envoyer les données vers Backend_API et confirmer la soumission
4. WHILE une Campagne_Inscription est fermée, THE Portail_Frontend SHALL désactiver le formulaire d'inscription
5. WHEN un doctorant déjà inscrit accède au processus de réinscription, THE Portail_Frontend SHALL pré-remplir les données de l'année précédente

### Requirement 3 - Dashboard Doctorant

**User Story:** En tant que doctorant, je veux visualiser l'état d'avancement de mon parcours doctoral sur un tableau de bord, afin de suivre mes démarches et échéances.

#### Acceptance Criteria

1. WHEN un Doctorant accède à son Dashboard, THE Portail_Frontend SHALL afficher le statut de son inscription actuelle
2. WHEN un Doctorant consulte sa timeline, THE Portail_Frontend SHALL présenter les étapes avec leurs états (soumis/en cours/validé/rejeté)
3. WHILE la durée du doctorat approche de la limite réglementaire, THE Portail_Frontend SHALL afficher une alerte visuelle
4. WHEN le statut d'un dossier change, THE Portail_Frontend SHALL mettre à jour l'affichage en temps réel
5. WHERE des prérequis de soutenance ne sont pas remplis, THE Portail_Frontend SHALL lister les éléments manquants

### Requirement 4 - Processus de Soutenance

**User Story:** En tant que doctorant, je veux pouvoir soumettre ma demande de soutenance avec tous les documents requis, afin d'obtenir l'autorisation de soutenir ma thèse.

#### Acceptance Criteria

1. WHEN un Doctorant accède au formulaire de demande de soutenance, THE Portail_Frontend SHALL vérifier les prérequis (publications, heures de formation)
2. IF les prérequis ne sont pas respectés, THEN THE Portail_Frontend SHALL bloquer la soumission et afficher les éléments manquants
3. WHEN un Doctorant upload les documents obligatoires, THE Portail_Frontend SHALL valider leur conformité
4. WHEN un Dossier_Soutenance est soumis, THE Portail_Frontend SHALL envoyer les données vers Backend_API et confirmer la réception
5. WHILE un Dossier_Soutenance est en cours de traitement, THE Portail_Frontend SHALL afficher le statut de progression

### Requirement 5 - Espace Directeur de Thèse

**User Story:** En tant que directeur de thèse, je veux pouvoir consulter et gérer les dossiers de mes doctorants, afin de donner mes avis et validations nécessaires.

#### Acceptance Criteria

1. WHEN un Directeur_These accède à son espace, THE Portail_Frontend SHALL lister tous ses doctorants encadrés
2. WHEN un Directeur_These consulte un dossier, THE Portail_Frontend SHALL afficher toutes les informations et documents associés
3. WHEN un Directeur_These donne un avis, THE Portail_Frontend SHALL enregistrer le commentaire et le statut via Backend_API
4. WHERE un dossier nécessite une validation, THE Portail_Frontend SHALL proposer les actions (valider/refuser) avec zone de commentaire
5. WHEN un Directeur_These propose un jury, THE Portail_Frontend SHALL permettre la saisie des membres et leurs rôles

### Requirement 6 - Administration du Système

**User Story:** En tant qu'administrateur, je veux pouvoir gérer les campagnes d'inscription et valider les dossiers administratifs, afin d'assurer le bon fonctionnement du processus doctoral.

#### Acceptance Criteria

1. WHEN un Administrateur accède à la gestion des campagnes, THE Portail_Frontend SHALL permettre la création/modification des Campagne_Inscription
2. WHEN un Administrateur définit les dates d'ouverture/fermeture, THE Portail_Frontend SHALL valider la cohérence temporelle
3. WHEN un Administrateur consulte les dossiers en attente, THE Portail_Frontend SHALL lister tous les dossiers nécessitant une validation administrative
4. WHEN un Administrateur valide ou rejette un dossier, THE Portail_Frontend SHALL enregistrer la décision avec commentaire via Backend_API
5. WHERE des règles de soutenance sont modifiées, THE Portail_Frontend SHALL permettre la mise à jour des seuils et critères

### Requirement 7 - Notifications et Documents

**User Story:** En tant qu'utilisateur du portail, je veux recevoir des notifications pertinentes et pouvoir télécharger les documents officiels, afin de rester informé et d'obtenir les attestations nécessaires.

#### Acceptance Criteria

1. WHEN une notification est émise par Backend_API, THE Portail_Frontend SHALL afficher l'alerte dans l'interface utilisateur
2. WHEN un utilisateur clique sur une notification, THE Portail_Frontend SHALL marquer la notification comme lue
3. WHEN un document officiel est disponible, THE Portail_Frontend SHALL proposer le téléchargement au format PDF
4. WHERE un utilisateur demande une attestation d'inscription, THE Portail_Frontend SHALL générer le document via Backend_API
5. WHILE des documents sont en cours de génération, THE Portail_Frontend SHALL afficher un indicateur de progression

### Requirement 8 - Interface Utilisateur et Navigation

**User Story:** En tant qu'utilisateur du portail, je veux naviguer facilement dans l'application avec une interface intuitive, afin d'accomplir mes tâches efficacement.

#### Acceptance Criteria

1. WHEN un utilisateur se connecte, THE Portail_Frontend SHALL afficher une navigation adaptée à son rôle
2. WHILE un utilisateur navigue, THE Portail_Frontend SHALL maintenir une sidebar contextuelle avec les options disponibles
3. WHEN une erreur API survient, THE Portail_Frontend SHALL afficher un message d'erreur compréhensible
4. WHERE des données sont en cours de chargement, THE Portail_Frontend SHALL afficher des indicateurs de progression
5. WHEN un utilisateur accède à une fonctionnalité, THE Portail_Frontend SHALL fournir des feedbacks visuels clairs sur les actions possibles