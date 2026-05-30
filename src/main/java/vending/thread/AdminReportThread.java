package vending.thread;

import javax.swing.SwingUtilities;
import vending.kiosk.KioskService;
import vending.sales.model.SaleEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * 매출 보고서 파일 읽기를 별도 스레드에서 처리한다.
 */
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
            List<SaleEntry> daily = service.getSalesService().allEntries();
            daily = service.getSalesService().getSorter().sortByDateDesc(daily);
            List<SaleEntry> dailyFinal = daily;
            SwingUtilities.invokeLater(() -> dailyConsumer.accept(dailyFinal));

            String monthKey = LocalDate.now().toString().substring(0, 7);
            List<String[]> monthly = service.getSalesService().monthlyRows(monthKey);
            SwingUtilities.invokeLater(() -> monthlyConsumer.accept(monthly));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
