package vending.thread;

import vending.kiosk.KioskService;
import vending.network.ActiveServerResolver;

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
                ActiveServerResolver.refresh();
                service.pollServerInfo();
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
