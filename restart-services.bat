@echo off
echo ========================================
echo Redemarrage des services corriges
echo ========================================
echo.

REM Demarrer Eureka Server
echo [1/7] Demarrage d'Eureka Server (port 8761)...
start "Eureka Server" cmd /k "cd eureka-server && mvn spring-boot:run"
timeout /t 30 /nobreak
echo.

REM Demarrer Gateway Service
echo [2/7] Demarrage du Gateway Service (port 8080)...
start "Gateway Service" cmd /k "cd gateway-service && mvn spring-boot:run"
timeout /t 15 /nobreak
echo.

REM Demarrer User Service
echo [3/7] Demarrage du User Service (port 8081)...
start "User Service" cmd /k "cd user-service && mvn spring-boot:run"
timeout /t 10 /nobreak
echo.

REM Demarrer Defense Service
echo [4/7] Demarrage du Defense Service (port 8082)...
start "Defense Service" cmd /k "cd defense-service && mvn spring-boot:run"
timeout /t 10 /nobreak
echo.

REM Demarrer Inscription Service
echo [5/7] Demarrage du Inscription Service (port 8084)...
start "Inscription Service" cmd /k "cd inscription-service && mvn spring-boot:run"
timeout /t 10 /nobreak
echo.

REM Demarrer Batch Service
echo [6/7] Demarrage du Batch Service (port 8085)...
start "Batch Service" cmd /k "cd batch-service && mvn spring-boot:run"
timeout /t 10 /nobreak
echo.

REM Demarrer Notification Service
echo [7/7] Demarrage du Notification Service (port 8086)...
start "Notification Service" cmd /k "cd notification-service && mvn spring-boot:run"
echo.

echo ========================================
echo Tous les services sont demarres !
echo ========================================
echo.
echo Attendez 2 minutes puis verifiez Eureka:
echo http://localhost:8761
echo.
pause
