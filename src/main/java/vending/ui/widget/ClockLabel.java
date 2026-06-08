package vending.ui.widget;

import vending.ui.theme.KioskColors;
import vending.ui.theme.KioskFonts;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class ClockLabel extends JLabel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final Timer timer;

    public ClockLabel() {
        setFont(KioskFonts.bodyBold());
        setForeground(KioskColors.TEXT);
        setHorizontalAlignment(SwingConstants.RIGHT);
        timer = new Timer(1000, e -> updateText());
        updateText();
        timer.start();
    }

    private void updateText() {
        LocalDateTime now = LocalDateTime.now();
        String day = now.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
        setText(DATE_FMT.format(now) + " (" + day + ")  " + TIME_FMT.format(now));
    }

    public void stop() {
        timer.stop();
    }
}
