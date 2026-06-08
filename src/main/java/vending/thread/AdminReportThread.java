package vending.thread;

import javax.swing.SwingUtilities;
import vending.kiosk.KioskService;
import vending.util.AppLog;
import vending.sales.model.SaleEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

public class AdminReportThread extends Thread {

    private final KioskService service;
    private final Consumer<List<SaleEntry>> dailyConsumer;
    private final Consumer<List<String[]>> monthlyConsumer;

    public AdminReportThread(KioskService service,
                             Consumer<List<SaleEntry>> dailyConsumer,
                             Consumer<List<String[]>> monthlyConsumer) {
        super("admin-report");
        this.service = service;
        this.dailyConsumer = dailyConsumer;
        this.monthlyConsumer = monthlyConsumer;
    }

    @Override
    public void run() {
        try {
            List<SaleEntry> daily = service.getSalesService().allEntriesSortedByDate();
            List<SaleEntry> dailyFinal = daily;
            SwingUtilities.invokeLater(() -> dailyConsumer.accept(dailyFinal));

            String monthKey = LocalDate.now().toString().substring(0, 7);
            List<String[]> monthly = service.getSalesService().monthlyRows(monthKey);
            SwingUtilities.invokeLater(() -> monthlyConsumer.accept(monthly));
        } catch (Exception e) {
            AppLog.error("ADMIN", "매출 보고서 로드 실패", e);
        }
    }
}
