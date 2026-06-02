package vending.bluetooth;

import vending.kiosk.KioskService;
import vending.util.AppLog;

/**
 * Bluetooth/원격에서 받은 명령을 키오스크 로직에 연결.
 *
 * 명령 형식:
 *   INSERT:500   - 500원 투입
 *   BUY:0        - 0번 음료 구매
 *   RETURN       - 화폐 반환
 */
public class BluetoothCommandHandler {

    private final KioskService service;

    public BluetoothCommandHandler(KioskService service) {
        this.service = service;
    }

    public String handle(String raw) {
        if (raw == null || raw.isBlank()) {
            return "EMPTY";
        }
        String cmd = raw.trim().toUpperCase();

        try {
            if (cmd.startsWith("INSERT:")) {
                int unit = Integer.parseInt(cmd.substring(7).trim());
                String err = service.insertMoney(unit);
                return err == null ? "OK:INSERT:" + unit : "ERR:" + err;
            }
            if (cmd.startsWith("BUY:")) {
                int index = Integer.parseInt(cmd.substring(4).trim());
                String result = service.buyDrink(index);
                return result.startsWith("판매 완료") ? "OK:" + result : "ERR:" + result;
            }
            if ("RETURN".equals(cmd)) {
                String result = service.returnInsertedMoney();
                return "OK:" + result;
            }
            if ("STATUS".equals(cmd)) {
                return "OK:INSERTED:" + service.insertedTotal();
            }
            return "ERR:UNKNOWN:" + cmd;
        } catch (NumberFormatException e) {
            AppLog.error("BT", "명령 파싱 실패: " + cmd, e);
            return "ERR:PARSE";
        } catch (Exception e) {
            AppLog.error("BT", "명령 처리 실패: " + cmd, e);
            return "ERR:" + e.getMessage();
        }
    }
}
