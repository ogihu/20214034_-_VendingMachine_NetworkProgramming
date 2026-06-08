package vending.ui.theme;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

// 앱아이콘기능
public final class AppIcon {

    private static final Color CAN_RED = new Color(220, 58, 52);
    private static final Color CAN_RED_DARK = new Color(170, 32, 36);
    private static final Color LID_SILVER = new Color(196, 202, 210);
    private static final Color LID_DARK = new Color(140, 148, 158);

    private AppIcon() {
    }

    public static Image create(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float cx = size / 2f;
        float canW = size * 0.52f;
        float canH = size * 0.72f;
        float left = cx - canW / 2f;
        float top = size * 0.16f;
        float bottom = top + canH;
        float rimH = Math.max(2f, size * 0.08f);

        // 캔 몸통
        g2.setPaint(new GradientPaint(left, top, CAN_RED, left + canW, bottom, CAN_RED_DARK));
        g2.fill(new RoundRectangle2D.Float(left, top + rimH * 0.35f, canW, canH - rimH * 0.7f,
                canW * 0.18f, canW * 0.18f));

        // 하이라이트 스트라이프
        float stripeW = Math.max(1.5f, size * 0.05f);
        g2.setColor(new Color(255, 255, 255, 90));
        g2.fill(new RoundRectangle2D.Float(left + canW * 0.22f, top + rimH,
                stripeW, canH - rimH * 1.4f, stripeW, stripeW));

        // 상단 림
        g2.setColor(LID_DARK);
        g2.fill(new Ellipse2D.Float(left, top, canW, rimH * 1.6f));
        g2.setColor(LID_SILVER);
        g2.fill(new Ellipse2D.Float(left + canW * 0.04f, top + rimH * 0.1f,
                canW * 0.92f, rimH * 1.2f));

        // 하단 베이스
        g2.setColor(LID_DARK);
        g2.fill(new Ellipse2D.Float(left, bottom - rimH * 1.1f, canW, rimH * 1.3f));
        g2.setColor(new Color(120, 128, 138));
        g2.fill(new Ellipse2D.Float(left + canW * 0.06f, bottom - rimH * 0.95f,
                canW * 0.88f, rimH * 0.9f));

        // 뚜껑 돌출부
        if (size >= 24) {
            float tabW = canW * 0.28f;
            float tabH = rimH * 0.55f;
            g2.setColor(new Color(175, 182, 192));
            g2.fill(new RoundRectangle2D.Float(cx - tabW / 2f, top - tabH * 0.35f, tabW, tabH, tabH, tabH));
        }

        g2.dispose();
        return img;
    }

    public static void applyToFrame(JFrame frame) {
        List<Image> icons = new ArrayList<>();
        for (int s : new int[]{16, 24, 32, 48, 64, 128}) {
            icons.add(create(s));
        }
        frame.setIconImages(icons);
    }
}
