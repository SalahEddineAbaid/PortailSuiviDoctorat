# 🌐 Tester Feign Client dans le navigateur (sans contrôleur)

## Méthode : Utiliser les endpoints existants

Le Feign Client est **automatiquement appelé** quand tu utilises les endpoints d'inscription qui envoient des notifications.

---

## 🚀 Test Simple (3 étapes)

### Étape 1 : Créer un utilisateur (Directeur)
Ouvre ton navigateur et va sur : **http://localhost:8081**

Ou utilise cette URL directement dans le navigateur :
```
http://localhost:8081/api/auth/register
```

Mais comme c'est un POST, utilise **Postman** ou **curl** :

```bash
curl -X POST http://localhost:8081/api/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"firstName\":\"Dr. Hassan\",\"lastName\":\"Alami\",\"email\":\"hassan@test.com\",\"password\":\"Test123!\",\"role\":\"DIRECTEUR\"}"
```

**Note l'ID** (exemple : `"id": 2`)

---

### Étape 2 : Créer un doctorant
```bash
curl -X POST http://localhost:8081/api/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"firstName\":\"Ahmed\",\"lastName\":\"Bennani\",\"email\":\"ahmed@test.com\",\"password\":\"Test123!\",\"role\":\"DOCTORANT\"}"
```

**Note l'ID** (exemple : `"id": 1`)

---

### Étape 3 : Créer une campagne
```bash
curl -X POST http://localhost:8082/api/campagnes ^
  -H "Content-Type: application/json" ^
  -d "{\"libelle\":\"Campagne 2025\",\"type\":\"INSCRIPTION\",\"dateDebut\":\"2025-09-01\",\"dateFin\":\"2025-10-31\",\"anneeUniversitaire\":2025}"
```

**Note l'ID** (exemple : `"id": 1`)

---

### Étape 4 : Créer une inscription
```bash
curl -X POST http://localhost:8082/api/inscriptions ^
  -H "Content-Type: application/json" ^
  -d "{\"doctorantId\":1,\"directeurTheseId\":2,\"campagneId\":1,\"sujetThese\":\"IA en médecine\",\"type\":\"PREMIERE_INSCRIPTION\",\"anneeInscription\":2025,\"cin\":\"AB123456\",\"cne\":\"R123456789\",\"telephone\":\"0612345678\",\"adresse\":\"123 Rue Test\",\"ville\":\"Casablanca\",\"pays\":\"Maroc\",\"dateNaissance\":\"1995-05-15\",\"lieuNaissance\":\"Rabat\",\"nationalite\":\"Marocaine\",\"titreThese\":\"IA et diagnostic\",\"discipline\":\"Informatique\",\"laboratoire\":\"LRIT\",\"etablissementAccueil\":\"Faculté\",\"cotutelle\":false,\"dateDebutPrevue\":\"2025-09-01\"}"
```

**Note l'ID** (exemple : `"id": 1`)

---

### Étape 5 : Soumettre l'inscription (🎯 ICI FEIGN EST APPELÉ !)

```bash
curl -X POST "http://localhost:8082/api/inscriptions/1/soumettre?doctorantId=1"
```

**À ce moment, Feign Client appelle User Service pour récupérer les infos du directeur !**

---

## 🔍 Vérifier que Feign a fonctionné

### Dans les logs d'Inscription Service
Cherche ces lignes :
```
Notification directeur 2 - nouvelle demande 1
Notification envoyée au directeur 2 via Kafka
```

Si tu vois ces logs, **Feign a fonctionné** ! Il a récupéré les infos du directeur (ID 2) depuis User Service.

---

## 🌐 Test dans le navigateur (GET uniquement)

Tu peux tester certains endpoints GET directement dans le navigateur :

### 1. Vérifier qu'Inscription Service est UP
```
http://localhost:8082/api/campagnes
```

### 2. Voir les inscriptions d'un doctorant
```
http://localhost:8082/api/inscriptions/doctorant/1
```

### 3. Voir une inscription spécifique
```
http://localhost:8082/api/inscriptions/1
```

---

## 📊 Vérifier dans les logs

### Logs Inscription Service (port 8082)
Quand tu soumets une inscription, tu verras :
```
INFO  m.e.i.s.NotificationService - Notification directeur 2 - nouvelle demande 1
DEBUG m.e.i.client.UserServiceClient - [UserServiceClient#getUserById] ---> GET http://USER-SERVICE/api/users/2
DEBUG m.e.i.client.UserServiceClient - [UserServiceClient#getUserById] <--- HTTP/1.1 200 (123ms)
INFO  m.e.i.s.NotificationService - Notification envoyée au directeur 2 via Kafka
```

### Logs User Service (port 8081)
Tu verras :
```
INFO  m.e.u.c.UserController - GET /api/users/2
```

---

## ✅ Preuve que Feign fonctionne

Si dans les logs d'Inscription Service tu vois :
- ✅ `Notification directeur X - nouvelle demande Y`
- ✅ `Notification envoyée au directeur X via Kafka`

**Alors Feign Client a bien appelé User Service !**

---

## 🎯 Résumé

**Tu n'as pas besoin de créer un contrôleur de test !**

Feign est déjà utilisé dans :
- `NotificationService.notifierDirecteurNouvelleDemande()` → Appelle `userServiceClient.getUserById()`
- `NotificationService.notifierDoctorantRejet()` → Appelle `userServiceClient.getUserById()`
- `NotificationService.notifierValidationDefinitive()` → Appelle `userServiceClient.getUserById()` deux fois

**Il suffit de soumettre une inscription pour déclencher Feign !**

---

## 🔧 Activer les logs Feign (pour voir les détails)

Ajoute dans `application.properties` :
```properties
logging.level.ma.emsi.inscriptionservice.client.UserServiceClient=DEBUG
logging.level.feign=DEBUG
```

Redémarre le service et tu verras tous les appels Feign dans les logs !

---

## 🎉 Test réussi si...

1. ✅ Inscription créée
2. ✅ Inscription soumise
3. ✅ Logs montrent "Notification directeur X"
4. ✅ Logs montrent "Notification envoyée"
5. ✅ Pas d'erreur "Connection refused" ou "Load balancer"

**Feign Client fonctionne ! 🚀**
