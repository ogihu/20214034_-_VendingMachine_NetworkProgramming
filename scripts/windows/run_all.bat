@echo off
call "%~dp0env.bat"
echo 서버 3개 + 클라이언트 3개를 새 창에서 실행합니다...
start "Server1" cmd /k "%~dp0servers\run_server1.bat"
timeout /t 2 /nobreak >nul
start "Server2" cmd /k "%~dp0servers\run_server2.bat"
timeout /t 2 /nobreak >nul
start "Backup" cmd /k "%~dp0servers\run_backup.bat"
timeout /t 3 /nobreak >nul
start "Client1" cmd /k "%~dp0clients\run_client1.bat"
timeout /t 2 /nobreak >nul
start "Client2" cmd /k "%~dp0clients\run_client2.bat"
timeout /t 2 /nobreak >nul
start "Client3" cmd /k "%~dp0clients\run_client3.bat"
echo 완료. 각 창에서 프로그램이 시작됩니다.
