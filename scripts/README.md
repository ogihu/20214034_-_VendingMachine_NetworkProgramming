# 실행 스크립트

Windows용 실행 스크립트 모음입니다. 프로젝트 루트에서 Gradle을 실행합니다.

## 폴더 구조

```
scripts/
  run_guide.bat          ← 전체 안내 (이 파일 실행)
  README.md              ← 이 문서
  windows/
    env.bat              ← 공통 환경 설정 (JAVA_HOME, UTF-8, 경로)
    run_all.bat          ← 서버+클라이언트 6개 새 창 일괄 실행
    servers/
      run_server1.bat    ← Server1 (9090)
      run_server2.bat    ← Server2 (9091)
      run_backup.bat     ← Backup (9092)
      run_cloud.bat      ← Cloud (9093)
    clients/
      run_client1.bat
      run_client2.bat
      run_client3.bat
      run_client4.bat
      run_end_dev.bat
  end_dev/
    bt_bridge.py         ← 라즈베리파이 Bluetooth 브릿지
```

## 빠른 시작

1. `scripts\run_guide.bat` 실행 — 안내 확인
2. 또는 `scripts\windows\run_all.bat` — 권장 6대 자동 실행

## 개별 실행

| 스크립트 | Gradle 태스크 |
|----------|---------------|
| `windows\servers\run_server1.bat` | `gradle runServer1` |
| `windows\servers\run_server2.bat` | `gradle runServer2` |
| `windows\servers\run_backup.bat` | `gradle runBackup` |
| `windows\servers\run_cloud.bat` | `gradle runCloud` |
| `windows\clients\run_client1.bat` | `gradle runClient1` |
| `windows\clients\run_client2.bat` | `gradle runClient2` |
| `windows\clients\run_client3.bat` | `gradle runClient3` |
| `windows\clients\run_client4.bat` | `gradle runClient4` |
| `windows\clients\run_end_dev.bat` | `gradle runEndDev` |

## 관리자

- 비밀번호: `Admin123!`
- 진입: 메인 화면 **SMART VENDING 5번 클릭**
