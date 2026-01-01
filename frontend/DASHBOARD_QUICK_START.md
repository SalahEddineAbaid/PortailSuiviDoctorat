# 🚀 Guide de Démarrage Rapide - Dashboards

**Date** : 2026-01-01  
**Version** : 1.0.0

---

## 📋 Prérequis

Avant de tester les dashboards, assurez-vous que :

✅ Tous les microservices sont démarrés
✅ Le frontend Angular est compilé sans erreurs
✅ Vous avez des comptes de test pour chaque rôle

---

## 🎯 Démarrage Rapide

### 1. Démarrer les Services Backend

```bash
# Depuis la racine du projet
./start-all-services.bat
```

**Services requis** :

- ✅ Eureka Server (8761, 8762)
- ✅ Gateway Service (8081)
- ✅ User Service (8083)
- ✅ Inscription Service (8084)
- ✅ Notification Service (8086)

### 2. Démarrer le Frontend

```bash
cd frontend
npm install  # Si première fois
npm start
```

L'application sera accessible sur : **http://localhost:4200**

---

## 👤 Comptes de Test

### Doctorant

```
Email: doctorant@test.com
Password: password123
Route: /dashboard/doctorant
```

### Directeur

```
Email: directeur@test.com
Password: password123
Route: /dashboard/directeur
```

### Admin

```
Email: admin@test.com
Password: password123
Route: /dashboard/admin
```

---

## 🧪 Scénarios de Test

### Test 1 : Connexion et Redirection Automatique

1. Ouvrir http://localhost:4200
2. Se connecter avec un compte doctorant
3. ✅ Vérifier la redirection automatique vers `/dashboard/doctorant`
4. ✅ Vérifier l'affichage des données

### Test 2 : Navigation entre Dashboards

1. Se connecter en tant qu'admin
2. Naviguer vers `/dashboard/admin`
3. ✅ Vérifier l'affichage du dashboard admin
4. Se déconnecter
5. Se connecter en tant que directeur
6. ✅ Vérifier la redirection vers `/dashboard/directeur`

### Test 3 : Chargement des Données

**Dashboard Doctorant** :

- ✅ Statistiques personnelles affichées
- ✅ Progression de la thèse visible
- ✅ Liste des inscriptions chargée
- ✅ Notifications récentes affichées

**Dashboard Directeur** :

- ✅ Statistiques de supervision affichées
- ✅ Demandes en attente listées
- ✅ Doctorants supervisés visibles
- ✅ Actions disponibles (Valider, Rejeter)

**Dashboard Admin** :

- ✅ Statistiques globales affichées
- ✅ Campagnes listées
- ✅ Utilisateurs actifs dans le tableau
- ✅ Logs d'audit visibles
- ✅ Alertes système affichées

### Test 4 : Rafraîchissement Manuel

1. Sur n'importe quel dashboard
2. Cliquer sur le bouton "Actualiser"
3. ✅ Vérifier l'icône de chargement
4. ✅ Vérifier le rechargement des données

### Test 5 : Responsive Design

1. Ouvrir les DevTools (F12)
2. Activer le mode responsive
3. Tester les résolutions :
   - 📱 Mobile (375px)
   - 📱 Tablette (768px)
   - 💻 Desktop (1920px)
4. ✅ Vérifier l'adaptation du layout

### Test 6 : Gestion des Erreurs

1. Arrêter un microservice (ex: inscription-service)
2. Rafraîchir le dashboard
3. ✅ Vérifier l'affichage du message d'erreur
4. Redémarrer le service
5. Rafraîchir à nouveau
6. ✅ Vérifier le retour à la normale

---

## 🔍 Points de Vérification

### Architecture

- [ ] Les resolvers préchargent les données
- [ ] Les guards protègent les routes
- [ ] L'intercepteur ajoute le JWT automatiquement
- [ ] Les services gèrent les erreurs correctement

### UI/UX

- [ ] Les cartes statistiques s'affichent correctement
- [ ] Les widgets sont bien alignés
- [ ] Les couleurs sont cohérentes
- [ ] Les animations sont fluides
- [ ] Les icônes s'affichent (Font Awesome)

### Fonctionnalités

- [ ] Le bouton de rafraîchissement fonctionne
- [ ] Les liens de navigation fonctionnent
- [ ] Les actions sur les demandes sont cliquables
- [ ] Les notifications sont affichées
- [ ] Les empty states s'affichent quand pas de données

---

## 🐛 Dépannage

### Problème : Erreur de compilation TypeScript

**Solution** :

```bash
cd frontend
npm install
ng build
```

### Problème : Données non chargées

**Vérifications** :

1. Les microservices sont-ils démarrés ?
2. Le gateway est-il accessible sur le port 8081 ?
3. Le JWT est-il valide ?
4. Vérifier la console du navigateur (F12)

**Commande de test** :

```bash
# Tester le gateway
curl http://localhost:8081/api/users/profile -H "Authorization: Bearer YOUR_TOKEN"
```

### Problème : Redirection incorrecte

**Vérifications** :

1. Le rôle de l'utilisateur est-il correct ?
2. Le guard `roleGuard` fonctionne-t-il ?
3. Vérifier les logs dans la console

**Debug** :

```typescript
// Dans auth.service.ts
console.log('Rôle détecté:', this.getUserRole());
console.log('Route dashboard:', this.getDashboardRoute());
```

### Problème : Styles non appliqués

**Solution** :

```bash
# Vérifier que les fichiers SCSS sont bien importés
# Redémarrer le serveur de développement
npm start
```

### Problème : Icônes Font Awesome manquantes

**Vérification** :

```html
<!-- Dans index.html -->
<link
  rel="stylesheet"
  href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css"
/>
```

---

## 📊 Endpoints API à Tester

### Test Manuel avec cURL

**1. Dashboard Doctorant**

```bash
curl -X GET "http://localhost:8081/api/inscriptions/doctorant/1/dashboard" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**2. Dashboard Directeur**

```bash
curl -X GET "http://localhost:8081/api/inscriptions/directeur/1/en-attente" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**3. Dashboard Admin**

```bash
curl -X GET "http://localhost:8081/api/admin/statistics/users" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**4. Notifications**

```bash
curl -X GET "http://localhost:8081/api/notifications/user/1/unread" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 🎨 Personnalisation

### Modifier les Couleurs

Fichier : `doctorant-dashboard.scss` (et autres)

```scss
// Couleurs principales
$primary-color: #3498db;
$success-color: #27ae60;
$warning-color: #f39c12;
$danger-color: #e74c3c;
```

### Ajouter un Widget

1. Créer le composant widget
2. Ajouter dans le template du dashboard
3. Mettre à jour le service pour charger les données
4. Ajouter les styles

---

## 📈 Métriques de Performance

### Temps de Chargement Attendus

- **Connexion** : < 1s
- **Chargement dashboard** : < 2s
- **Rafraîchissement** : < 1s
- **Navigation** : < 500ms

### Optimisations Possibles

1. **Lazy Loading** : ✅ Déjà implémenté
2. **OnPush Change Detection** : ✅ Déjà implémenté
3. **Resolvers** : ✅ Déjà implémenté
4. **Caching** : ⏳ À implémenter si nécessaire

---

## 🔐 Sécurité

### Points de Sécurité Implémentés

- ✅ JWT pour l'authentification
- ✅ Guards pour la protection des routes
- ✅ Vérification des rôles côté frontend
- ✅ Intercepteur pour ajouter le token automatiquement

### Rappels Importants

⚠️ **La sécurité côté frontend n'est pas suffisante !**

- Toujours valider côté backend
- Ne jamais exposer de données sensibles
- Vérifier les permissions sur chaque endpoint

---

## 📞 Support

### En cas de problème

1. **Vérifier les logs** :

   - Console navigateur (F12)
   - Logs des microservices
   - Logs du gateway

2. **Vérifier la documentation** :

   - `DASHBOARD_IMPLEMENTATION_PLAN.md`
   - `DASHBOARD_PROGRESS.md`
   - `README_AUTHENTICATION.md`

3. **Commandes utiles** :

```bash
# Nettoyer et réinstaller
rm -rf node_modules package-lock.json
npm install

# Rebuild complet
ng build --configuration production

# Vérifier les erreurs TypeScript
ng build --watch
```

---

## ✅ Checklist Finale

Avant de considérer les dashboards comme terminés :

### Fonctionnel

- [ ] Tous les dashboards s'affichent correctement
- [ ] Les données sont chargées depuis les APIs
- [ ] La navigation fonctionne
- [ ] Les actions sont fonctionnelles
- [ ] Le rafraîchissement fonctionne

### Technique

- [ ] Aucune erreur de compilation
- [ ] Aucune erreur dans la console
- [ ] Les types TypeScript sont corrects
- [ ] Les imports sont tous résolus

### UX/UI

- [ ] Le design est cohérent
- [ ] Le responsive fonctionne
- [ ] Les animations sont fluides
- [ ] Les loading states sont visibles
- [ ] Les erreurs sont gérées

### Performance

- [ ] Temps de chargement < 2s
- [ ] Pas de memory leaks
- [ ] Change detection optimisée

---

## 🎉 Prochaines Étapes

Une fois les tests validés :

1. **Optimisations** :

   - Ajouter des graphiques (Chart.js)
   - Implémenter le WebSocket pour les notifications temps réel
   - Ajouter des filtres avancés

2. **Fonctionnalités** :

   - Export PDF des statistiques
   - Personnalisation des widgets
   - Mode sombre

3. **Tests** :
   - Tests unitaires (Jasmine/Karma)
   - Tests E2E (Cypress/Playwright)
   - Tests de performance

---

**Bon test ! 🚀**
