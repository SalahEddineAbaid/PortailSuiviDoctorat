# 🛠️ Commandes Utiles - Dashboards

Guide de référence rapide pour le développement et le débogage des dashboards.

---

## 🚀 Démarrage

### Démarrer le Frontend

```bash
# Développement
cd frontend
npm start

# Avec port spécifique
ng serve --port 4200

# Avec ouverture automatique du navigateur
ng serve --open

# Mode production
ng serve --configuration production
```

### Démarrer les Microservices

```bash
# Depuis la racine du projet
./start-all-services.bat

# Ou individuellement
cd user-service && mvn spring-boot:run
cd inscription-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
```

---

## 🔨 Build & Compilation

### Build de Développement

```bash
# Build simple
ng build

# Build avec watch
ng build --watch

# Build avec configuration spécifique
ng build --configuration development
```

### Build de Production

```bash
# Build optimisé
ng build --configuration production

# Build avec analyse de bundle
ng build --configuration production --stats-json
npm install -g webpack-bundle-analyzer
webpack-bundle-analyzer dist/frontend/browser/stats.json
```

### Vérification TypeScript

```bash
# Compiler sans générer de fichiers
tsc --noEmit

# Vérifier les types
ng build --watch
```

---

## 🧪 Tests

### Tests Unitaires

```bash
# Lancer tous les tests
ng test

# Tests avec coverage
ng test --code-coverage

# Tests en mode headless
ng test --browsers=ChromeHeadless --watch=false

# Tests d'un fichier spécifique
ng test --include='**/dashboard.service.spec.ts'
```

### Tests E2E

```bash
# Avec Cypress (si installé)
npm run e2e

# Avec Playwright (si installé)
npx playwright test
```

---

## 🔍 Débogage

### Logs et Console

```bash
# Activer les logs détaillés
ng serve --verbose

# Voir les erreurs de compilation
ng build --watch

# Analyser les performances
ng serve --source-map
```

### Vérifier les Endpoints API

```bash
# Test de connexion
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"doctorant@test.com","password":"password123"}'

# Test dashboard doctorant (avec token)
curl -X GET http://localhost:8081/api/inscriptions/doctorant/1/dashboard \
  -H "Authorization: Bearer YOUR_TOKEN"

# Test dashboard directeur
curl -X GET http://localhost:8081/api/inscriptions/directeur/1/en-attente \
  -H "Authorization: Bearer YOUR_TOKEN"

# Test dashboard admin
curl -X GET http://localhost:8081/api/admin/statistics/users \
  -H "Authorization: Bearer YOUR_TOKEN"

# Test notifications
curl -X GET http://localhost:8081/api/notifications/user/1/unread \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 📦 Gestion des Dépendances

### Installation

```bash
# Installer toutes les dépendances
npm install

# Installer une dépendance spécifique
npm install package-name

# Installer en dev
npm install --save-dev package-name
```

### Mise à Jour

```bash
# Vérifier les packages obsolètes
npm outdated

# Mettre à jour Angular
ng update @angular/core @angular/cli

# Mettre à jour toutes les dépendances
npm update

# Mettre à jour une dépendance spécifique
npm update package-name
```

### Nettoyage

```bash
# Supprimer node_modules et réinstaller
rm -rf node_modules package-lock.json
npm install

# Nettoyer le cache npm
npm cache clean --force

# Nettoyer le cache Angular
ng cache clean
```

---

## 🔧 Génération de Code

### Générer des Composants

```bash
# Composant standalone
ng generate component features/dashboard/new-widget --standalone

# Service
ng generate service features/dashboard/services/new-service

# Guard
ng generate guard core/guards/new-guard

# Resolver
ng generate resolver features/dashboard/resolvers/new-resolver

# Interface
ng generate interface features/dashboard/models/new-model
```

---

## 📊 Analyse et Optimisation

### Analyse de Bundle

```bash
# Générer les stats
ng build --configuration production --stats-json

# Analyser avec webpack-bundle-analyzer
npx webpack-bundle-analyzer dist/frontend/browser/stats.json
```

### Performance

```bash
# Build avec source maps
ng build --source-map

# Analyser les performances
ng serve --source-map

# Lighthouse audit (dans Chrome DevTools)
# Ouvrir DevTools > Lighthouse > Generate report
```

### Linting

```bash
# Si ESLint est configuré
npm run lint

# Fixer automatiquement
npm run lint -- --fix
```

---

## 🐛 Troubleshooting

### Erreur : "Cannot find module"

```bash
# Solution 1 : Réinstaller les dépendances
rm -rf node_modules package-lock.json
npm install

# Solution 2 : Vérifier les imports
# Assurez-vous que les chemins sont corrects
```

### Erreur : "Port 4200 already in use"

```bash
# Solution 1 : Utiliser un autre port
ng serve --port 4201

# Solution 2 : Tuer le processus
# Windows
netstat -ano | findstr :4200
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:4200 | xargs kill -9
```

### Erreur : "Module not found: Error: Can't resolve"

```bash
# Vérifier que le module est installé
npm list package-name

# Réinstaller si nécessaire
npm install package-name

# Vérifier tsconfig.json paths
```

### Erreur de Compilation TypeScript

```bash
# Vérifier la version de TypeScript
tsc --version

# Compiler avec détails
ng build --verbose

# Vérifier tsconfig.json
cat tsconfig.json
```

### Problème de CORS

```bash
# Utiliser le proxy Angular
# Créer proxy.conf.json
{
  "/api": {
    "target": "http://localhost:8081",
    "secure": false,
    "changeOrigin": true
  }
}

# Démarrer avec proxy
ng serve --proxy-config proxy.conf.json
```

---

## 🔐 Sécurité

### Vérifier les Vulnérabilités

```bash
# Audit npm
npm audit

# Fixer automatiquement
npm audit fix

# Fixer avec breaking changes
npm audit fix --force
```

### Mettre à Jour les Packages de Sécurité

```bash
# Mettre à jour les packages avec vulnérabilités
npm update --depth 9999

# Vérifier à nouveau
npm audit
```

---

## 📝 Git

### Commandes Utiles

```bash
# Status
git status

# Ajouter les fichiers dashboard
git add frontend/src/app/features/dashboard/

# Commit
git commit -m "feat: implement complete dashboards for all roles"

# Push
git push origin main

# Créer une branche
git checkout -b feature/dashboards

# Voir les différences
git diff
```

---

## 🌐 Environnements

### Développement

```bash
# Utiliser environment.ts
ng serve

# Ou explicitement
ng serve --configuration development
```

### Production

```bash
# Utiliser environment.prod.ts
ng build --configuration production

# Servir localement
npm install -g http-server
http-server dist/frontend/browser -p 8080
```

### Staging (si configuré)

```bash
# Créer environment.staging.ts
ng build --configuration staging
```

---

## 📊 Monitoring

### Logs en Temps Réel

```bash
# Logs du frontend
ng serve --verbose

# Logs des microservices
tail -f user-service/logs/application.log
tail -f inscription-service/logs/inscription-service.log
tail -f notification-service/logs/notification-service.log
```

### Vérifier l'État des Services

```bash
# Health check gateway
curl http://localhost:8081/actuator/health

# Health check user-service
curl http://localhost:8083/actuator/health

# Health check inscription-service
curl http://localhost:8084/actuator/health

# Health check notification-service
curl http://localhost:8086/actuator/health
```

---

## 🎨 Styles

### Compiler SCSS

```bash
# Angular compile automatiquement
# Mais pour tester manuellement :
sass frontend/src/app/features/dashboard/doctorant-dashboard/doctorant-dashboard.scss output.css
```

### Vérifier les Styles

```bash
# Ouvrir DevTools (F12)
# Onglet Elements > Styles
# Vérifier les classes appliquées
```

---

## 🔄 Rechargement

### Hot Reload

```bash
# Activé par défaut avec ng serve
ng serve

# Désactiver si nécessaire
ng serve --live-reload=false
```

### Rechargement Complet

```bash
# Ctrl+C pour arrêter
# Puis redémarrer
npm start
```

---

## 📱 Tests Responsive

### Avec Chrome DevTools

```bash
# 1. Ouvrir DevTools (F12)
# 2. Toggle device toolbar (Ctrl+Shift+M)
# 3. Sélectionner un appareil ou dimension personnalisée
```

### Avec BrowserStack (si disponible)

```bash
# Tester sur vrais appareils
# https://www.browserstack.com/
```

---

## 🚀 Déploiement

### Build Production

```bash
# Build optimisé
ng build --configuration production

# Vérifier la taille
du -sh dist/frontend/browser/

# Compresser pour déploiement
cd dist/frontend/browser
tar -czf frontend.tar.gz *
```

### Déployer sur Serveur

```bash
# Copier les fichiers
scp -r dist/frontend/browser/* user@server:/var/www/html/

# Ou avec rsync
rsync -avz dist/frontend/browser/ user@server:/var/www/html/
```

---

## 💾 Backup

### Sauvegarder le Code

```bash
# Créer une archive
tar -czf dashboard-backup-$(date +%Y%m%d).tar.gz \
  frontend/src/app/features/dashboard/

# Sauvegarder sur Git
git add .
git commit -m "backup: dashboard implementation"
git push
```

---

## 📚 Documentation

### Générer la Documentation

```bash
# Avec Compodoc (si installé)
npm install -g @compodoc/compodoc
compodoc -p tsconfig.json -s

# Ouvrir dans le navigateur
# http://localhost:8080
```

---

## ✅ Checklist Avant Commit

```bash
# 1. Vérifier la compilation
ng build

# 2. Lancer les tests
ng test --watch=false

# 3. Vérifier le linting
npm run lint

# 4. Vérifier les vulnérabilités
npm audit

# 5. Commit
git add .
git commit -m "feat: your message"
git push
```

---

**Référence rapide pour le développement quotidien ! 🚀**
