package vending.ui.theme;

import java.awt.Font;
import java.awt.GraphicsEnvironment;

// 폰트테마
public final class KioskFonts {

    private static final String FAMILY = pickFamily();

    private KioskFonts() {
    }

    private static String pickFamily() {
        String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String prefer : new String[]{"맑은 고딕", "Malgun Gothic", "NanumGothic", "SansSerif"}) {
            for (String n : names) {
                if (n.equalsIgnoreCase(prefer)) {
                    return n;
                }
            }
        }
        return "SansSerif";
    }

    public static Font title() {
        return new Font(FAMILY, Font.BOLD, 20);
    }

    public static Font section() {
        return new Font(FAMILY, Font.BOLD, 15);
    }

    public static Font body() {
        return new Font(FAMILY, Font.PLAIN, 13);
    }

    public static Font bodyBold() {
        return new Font(FAMILY, Font.BOLD, 13);
    }

    public static Font amount() {
        return new Font(FAMILY, Font.BOLD, 28);
    }

    public static Font small() {
        return new Font(FAMILY, Font.PLAIN, 12);
    }

    public static Font button() {
        return new Font(FAMILY, Font.BOLD, 14);
    }
}
