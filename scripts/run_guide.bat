@echo off
chcp 65001 >nul
echo ============================================
echo  SMART VENDING 실행 가이드
echo ============================================
echo.
echo [자동 실행] scripts\windows\run_all.bat
echo   - Server1, Server2, Backup, Client1~3 새 창 실행
echo.
echo [서버 - scripts\windows\servers\]
echo   run_server1.bat   (9090)
echo   run_server2.bat   (9091)
echo   run_backup.bat    (9092, 페일오버)
echo   run_cloud.bat     (9093, 선택)
echo.
echo [클라이언트 - scripts\windows\clients\]
echo   run_client1.bat
echo   run_client2.bat
echo   run_client3.bat
echo   run_client4.bat   (선택)
echo   run_end_dev.bat   (선택)
echo.
echo [Gradle 직접 실행]
echo   gradle runServer1 / runServer2 / runBackup
echo   gradle runClient1 / runClient2 / runClient3
echo.
echo 관리자 비밀번호: Admin123!
echo 관리자 진입: SMART VENDING 5번 클릭
echo.
echo 데이터 경로: data\clients\Client1\ ...
echo 상세: scripts\README.md
echo.
pause
