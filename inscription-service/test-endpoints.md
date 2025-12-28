# Tests des endpoints - Inscription Service

## Prérequis
1. MariaDB avec la base `inscriptiondb` créée
2. Eureka Server en cours d'exécution (port 8761)
3. Inscription Service démarré (port 8082)

## Tests avec curl (Windows)

### 1. Créer une campagne

```bash
curl -X POST http://localhost:8082/api/campagnes ^
  -H "Content-Type: application/json" ^
  -d "{\"libelle\":\"Campagne Inscription 2024-2025\",\"type\":\"INSCRIPTION\",\"dateDebut\":\"2024-09-01T00:00:00\",\"dateFin\":\"2024-10-31T23:59:59\",\"description\":\"Campagne d'inscription pour l'année universitaire 2024-2025\"}"
```

### 2. Lister toutes les campagnes

```bash
curl -X GET http://localhost:8082/api/campagnes
```

### 3. Lister les campagnes actives

```bash
curl -X GET http://localhost:8082/api/campagnes/actives
```

### 4. Créer une inscription

```bash
curl -X POST http://localhost:8082/api/inscriptions ^
  -H "Content-Type: application/json" ^
  -d "{\"doctorantId\":1,\"directeurTheseId\":2,\"campagneId\":1,\"sujetThese\":\"Intelligence Artificielle et Machine Learning\",\"type\":\"PREMIERE_INSCRIPTION\",\"anneeInscription\":2024}"
```

### 5. Récupérer une inscription

```bash
curl -X GET http://localhost:8082/api/inscriptions/1
```

### 6. Lister les inscriptions d'un doctorant

```bash
curl -X GET http://localhost:8082/api/inscriptions/doctorant/1
```

### 7. Soumettre une inscription

```bash
curl -X POST "http://localhost:8082/api/inscriptions/1/soumettre?doctorantId=1"
```

### 8. Valider par le directeur

```bash
curl -X POST "http://localhost:8082/api/inscriptions/1/valider-directeur?directeurId=2" ^
  -H "Content-Type: application/json" ^
  -d "{\"inscriptionId\":1,\"approuve\":true,\"commentaire\":\"Dossier complet et conforme\"}"
```

### 9. Valider par l'administration

```bash
curl -X POST http://localhost:8082/api/inscriptions/1/valider-admin ^
  -H "Content-Type: application/json" ^
  -d "{\"inscriptionId\":1,\"approuve\":true,\"commentaire\":\"Inscription validée\"}"
```

## Tests avec Postman

Importez la collection suivante :

1. Créez une nouvelle collection "Inscription Service"
2. Ajoutez les requêtes ci-dessus
3. Configurez la variable `{{baseUrl}}` = `http://localhost:8082`

## Vérification

Après chaque test, vérifiez :
- Le code de statut HTTP (200, 201, etc.)
- La structure de la réponse JSON
- Les données dans la base de données via HeidiSQL
