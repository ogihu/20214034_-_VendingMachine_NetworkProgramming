@echo off
REM 로컬 통합 테스트 - 터미널 5개에서 각각 실행

echo [1] Server1  : gradlew runServer1
echo [2] Server2  : gradlew runServer2
echo [3] Backup   : gradlew runBackup
echo [4] Cloud    : gradlew runCloud
echo [5] Client1  : gradlew runClient1
echo [6] End_Dev  : gradlew runEndDev
echo.
echo Railway Cloud 배포:
echo   gradlew fatJar
echo   railway up  (Dockerfile, SERVER_ROLE=CLOUD)
