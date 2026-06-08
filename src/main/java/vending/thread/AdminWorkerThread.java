package vending.thread;

import vending.kiosk.KioskService;

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
