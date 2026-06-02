package vending.thread;

import vending.kiosk.KioskService;

/**
 * 서버 알림·집계를 주기적으로 조회하는 스레드.
 */
public class AlertPollThread extends Thread {

    private final KioskService service;

    public AlertPollThread(KioskService service) {
        super("alert-poll");
        this.service = service;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                service.pollServerInfo();
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
