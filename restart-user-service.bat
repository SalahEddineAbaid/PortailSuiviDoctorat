@echo off
echo ========================================
echo Redemarrage USER-SERVICE uniquement
echo ========================================
echo.
echo Fermez la fenetre USER-SERVICE actuelle si elle est ouverte
pause
echo.
echo Demarrage du User Service (port 8081)...
start "User Service" cmd /k "cd user-service && mvn spring-boot:run"
echo.
echo Attendez 30 secondes puis verifiez:
echo 1. Logs dans la fenetre User Service
echo 2. Eureka Dashboard: http://localhost:8761
echo 3. Health check: http://localhost:8081/actuator/health
echo.
pause
