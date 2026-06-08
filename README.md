# 자판기 관리 프로그램 (네트워크프로그래밍)

Java 11 + Swing GUI + TCP 소켓(JSON) 기반 자판기·서버 시스템입니다.

## 폴더 구조

```
src/main/java/vending/
  ClientMain.java          클라이언트 실행
  config/                  설정·데이터 경로
  drink/                   음료·재고(연결 리스트)
  coin/                    화폐·거스름(스택)
  payment/                 투입·검증·거스름 계산
  sales/                   일별·월별·초기 매출 파일
  admin/                   비밀번호·수금·재고 파일
  kiosk/                   판매·관리 핵심 로직
  ui/                      Swing 화면
  event/                   네트워크 전송 큐·보류 저장
  network/                 TCP 클라이언트·페일오버
  protocol/                메시지 타입·JSON
  server/                  Server1/2 집계·동기화
  thread/                  백그라운드 작업
  util/                    로그·예외·콘솔 인코딩
config/                    client1~3.properties
data/                      실행 시 생성 (매출·재고)
```

## 실행 (권장 6대: Server1, Server2, Backup, Client1~3)

**가장 쉬운 방법:** `scripts\windows\run_all.bat` 더블클릭

또는 터미널을 각각 열고:

```bat
scripts\windows\servers\run_server1.bat
scripts\windows\servers\run_server2.bat
scripts\windows\servers\run_backup.bat
scripts\windows\clients\run_client1.bat
scripts\windows\clients\run_client2.bat
scripts\windows\clients\run_client3.bat
```

Gradle 직접 실행: `gradle runServer1` 등 (`scripts\README.md` 참고)

## 관리자

- 기본 비밀번호: `Admin123!`
- 관리자 모드: 메인 화면 **SMART VENDING 5번 클릭**
- 기능: 재고 보충, 수금, 가격·이름 변경, 일별/월별 매출, 서버 집계, 원격 음료 변경

## 데이터 파일

| 경로 | 내용 |
|------|------|
| `data/clients/Client1/sales/` | 클라이언트별 일별·월별 매출 |
| `data/clients/Client1/inventory/` | 클라이언트별 재고·화폐 |
| `data/clients/Client1/network/pending_events.jsonl` | 전송 실패 이벤트 보류 |
| `data/admin/password.txt` | 관리자 비밀번호 (공통) |
| `data/server/*.log` | 서버 집계 로그 |
| `data/server/pending_sync_*.jsonl` | 서버 동기화 보류 |

## 소켓 포트

| 역할 | 포트 |
|------|------|
| Server1 | 9090 |
| Server2 | 9091 |
| Backup | 9092 |
| Cloud | 9093 |

클라이언트는 `config/clientN.properties`의 `server.host` / `server.port`로 접속합니다.

## 서버 동기화·페일오버

- **Backup 활성 서버 조회**: 클라이언트가 15초마다 Backup(9092)에 조회해 Server1/2 중 살아 있는 서버로 자동 전환
- **전송 재시도**: 메모리 12회 재시도 후 디스크 보류 → 서버 복구 시 자동 재전송
- **Server1↔Server2 동기화**: `PeerSyncManager` + `SyncRetryQueue`
- **Backup 동기화**: Server1/2가 Backup(9092)로 SYNC 메시지 전송 (`backup.sync.host/port`)
- **스냅샷**: 피어 복구 시 `SNAPSHOT_REQUEST` / `SNAPSHOT`으로 동기화
- **원격 음료 변경**: 관리자 → 서버 큐 → 대상 클라이언트가 15초 주기로 `QUERY_REMOTE` 수신 후 반영

## 선택 실행

```bat
gradle runCloud      # Cloud 집계 (9093)
gradle runClient4    # 추가 클라이언트
```
