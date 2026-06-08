package vending.bluetooth;

import vending.kiosk.KioskService;
import vending.util.AppLog;

// 블루투스명령기능
public class BluetoothCommandHandler {

    private static final String HELP_TEXT =
            "OK:INSERT:금액|BUY:번호|RETURN|STATUS|LIST|HELP";

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
            if ("HELP".equals(cmd)) {
                return HELP_TEXT;
            }
            if ("LIST".equals(cmd)) {
                return service.listDrinksForBluetooth();
            }
            if ("STATUS".equals(cmd)) {
                return buildStatus();
            }
            if (service.isAdminMode()) {
                return "ERR:관리자 모드에서는 판매할 수 없습니다.";
            }
            if (cmd.startsWith("INSERT:")) {
                int unit = Integer.parseInt(cmd.substring(7).trim());
                String err = service.insertMoney(unit);
                return err == null ? "OK:INSERT:" + unit + ",TOTAL:" + service.insertedTotal()
                        : "ERR:" + err;
            }
            if (cmd.startsWith("BUY:")) {
                int index = Integer.parseInt(cmd.substring(4).trim());
                String result = service.buyDrink(index);
                return result.startsWith("판매 완료") ? "OK:" + result : "ERR:" + result;
            }
            if ("RETURN".equals(cmd)) {
                String result = service.returnInsertedMoney();
                return result.startsWith("반환 완료") ? "OK:" + result : "ERR:" + result;
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

    private String buildStatus() {
        String mode = service.isAdminMode() ? "ADMIN" : "CUSTOMER";
        return "OK:MODE:" + mode + ",INSERTED:" + service.insertedTotal();
    }
}
