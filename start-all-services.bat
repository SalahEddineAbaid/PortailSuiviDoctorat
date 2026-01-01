@echo off
REM ============================================
REM Script de démarrage de tous les services
REM Plateforme de Gestion des Thèses
REM ============================================

echo ========================================
echo Demarrage de la plateforme
echo ========================================
echo.

REM Vérifier Java
echo [1/8] Verification de Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERREUR: Java n'est pas installe ou n'est pas dans le PATH
    pause
    exit /b 1
)
echo Java OK
echo.

REM Vérifier Maven
echo [2/8] Verification de Maven...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ERREUR: Maven n'est pas installe ou n'est pas dans le PATH
    pause
    exit /b 1
)
echo Maven OK
echo.

REM Vérifier Node.js
echo [3/8] Verification de Node.js...
node -v >nul 2>&1
if errorlevel 1 (
    echo ERREUR: Node.js n'est pas installe ou n'est pas dans le PATH
    pause
    exit /b 1
)
echo Node.js OK
echo.

REM Créer les dossiers de logs
echo [4/8] Creation des dossiers de logs...
if not exist "logs" mkdir logs
if not exist "eureka-server\logs" mkdir eureka-server\logs
if not exist "gateway-service\logs" mkdir gateway-service\logs
if not exist "user-service\logs" mkdir user-service\logs
if not exist "inscription-service\logs" mkdir inscription-service\logs
if not exist "notification-service\logs" mkdir notification-service\logs
if not exist "defense-service\logs" mkdir defense-service\logs
if not exist "batch-service\logs" mkdir batch-service\logs
echo Dossiers crees
echo.

REM Démarrer Eureka Server
echo [5/8] Demarrage d'Eureka Server (port 8761)...
start "Eureka Server" cmd /k "cd eureka-server && mvn spring-boot:run"
echo Attente de 30 secondes pour le demarrage d'Eureka...
timeout /t 30 /nobreak
echo.

REM Démarrer Gateway Service
echo [6/8] Demarrage du Gateway Service (port 8080)...
start "Gateway Service" cmd /k "cd gateway-service && mvn spring-boot:run"
timeout /t 10 /nobreak
echo.

REM Démarrer les services métier
echo [7/8] Demarrage des services metier...

echo - User Service (port 8081)...
start "User Service" cmd /k "cd user-service && mvn spring-boot:run"
timeout /t 5 /nobreak

echo - Defense Service (port 8082)...
start "Defense Service" cmd /k "cd defense-service && mvn spring-boot:run"
timeout /t 5 /nobreak

echo - Inscription Service (port 8084)...
start "Inscription Service" cmd /k "cd inscription-service && mvn spring-boot:run"
timeout /t 5 /nobreak

echo - Batch Service (port 8085)...
start "Batch Service" cmd /k "cd batch-service && mvn spring-boot:run"
timeout /t 5 /nobreak

echo - Notification Service (port 8086)...
start "Notification Service" cmd /k "cd notification-service && mvn spring-boot:run"
timeout /t 5 /nobreak

echo.
echo Tous les services backend sont en cours de demarrage...
echo Attente de 60 secondes pour la stabilisation...
timeout /t 60 /nobreak
echo.

REM Démarrer le frontend
echo [8/8] Demarrage du frontend Angular (port 4200)...
start "Frontend Angular" cmd /k "cd frontend && npm start"
echo.

echo ========================================
echo Tous les services sont demarres !
echo ========================================
echo.
echo URLs importantes:
echo - Frontend:           http://localhost:4200
echo - Gateway API:        http://localhost:8080
echo - Eureka Dashboard:   http://localhost:8761
echo.
echo Services backend:
echo - User Service:       http://localhost:8081
echo - Defense Service:    http://localhost:8082
echo - Inscription Service: http://localhost:8084
echo - Batch Service:      http://localhost:8085
echo - Notification Service: http://localhost:8086
echo.
echo Appuyez sur une touche pour ouvrir le navigateur...
pause
start http://localhost:4200
start http://localhost:8761
echo.
echo Pour arreter tous les services, fermez toutes les fenetres de commande.
echo.
pause
