package vending.thread;

import vending.kiosk.KioskService;

/**
 * 관리자 모드 중 주기적으로 재고 파일을 저장하는 스레드.
 */
public class AdminWorkerThread extends Thread {

    private final KioskService service;

    public AdminWorkerThread(KioskService service) {
        super("admin-worker");
        this.service = service;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(5000);
                if (service.isAdminMode()) {
                    service.periodicAdminSave();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
