# End Dev / Bluetooth

라즈베리파이 등 외부 장치에서 Bluetooth 입력을 자판기 클라이언트로 전달할 때 사용합니다.

## bt_bridge.py

- 클라이언트의 `BluetoothInputServer` 포트로 명령 전달
- `config/end_dev.properties`의 `bluetooth.port` 설정과 연동

## 명령 (Java BluetoothInputServer)

| 명령 | 설명 |
|------|------|
| `HELP` | 사용 가능 명령 목록 |
| `LIST` | 음료 번호·가격·재고 |
| `INSERT:500` | 500원 투입 |
| `BUY:0` | 0번 음료 구매 |
| `RETURN` | 투입금 반환 |
| `STATUS` | 모드·투입액 조회 |

관리자 모드에서는 `STATUS`/`LIST`/`HELP`만 허용됩니다.

## 실행

1. `scripts\windows\clients\run_end_dev.bat` 로 End Dev 클라이언트 실행
2. PC 테스트: `echo LIST | python scripts\end_dev\bt_bridge.py --test`
3. 라즈베리파이: `python scripts\end_dev\bt_bridge.py` (PyBluez 필요)
