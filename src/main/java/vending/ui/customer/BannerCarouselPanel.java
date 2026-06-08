package vending.ui.customer;

import vending.config.DataPaths;
import vending.ui.theme.KioskFonts;
import vending.util.AppLog;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class BannerCarouselPanel extends JPanel {

    private static final BannerSlide[] SLIDES = {
            new BannerSlide("배너1", "목마를 땐 시원한 한 병!"),
            new BannerSlide("배너2", "피곤함을 깨우는 선택"),
            new BannerSlide("배너3", "상큼함이 터지는 순간"),
            new BannerSlide("배너4", "기분 전환이 필요할 때")
    };

    private int index;
    private final Image[] images = new Image[SLIDES.length];
    private final Timer timer;

    public BannerCarouselPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(10, 128));
        loadImages();
        timer = new Timer(7000, e -> {
            index = (index + 1) % SLIDES.length;
            repaint();
        });
        timer.start();
    }

    private void loadImages() {
        for (int i = 0; i < SLIDES.length; i++) {
            images[i] = loadImage(SLIDES[i].fileName);
        }
    }

    private Image loadImage(String name) {
        for (String ext : new String[]{".PNG", ".png"}) {
            Path path = DataPaths.PROJECT_IMAGES.resolve(name + ext);
            if (Files.exists(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    return ImageIO.read(in);
                } catch (Exception e) {
                    AppLog.warn("IMAGE", "배너 이미지 로드 실패: " + path + " / " + e.getMessage());
                }
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        BannerSlide slide = SLIDES[index];
        Image img = images[index];

        g2.setClip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
        if (img != null) {
            drawCover(g2, img, 0, 0, getWidth(), getHeight());
        } else {
            g2.setColor(new Color(225, 240, 255));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.setClip(null);

        drawShadowText(g2, slide.message, 22, 72, KioskFonts.title().deriveFont(Font.BOLD, 22f));

        g2.dispose();
    }

    private void drawShadowText(Graphics2D g2, String text, int x, int y, Font font) {
        g2.setFont(font);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawString(text, x + 1, y + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(text, x, y);
    }

    private void drawCover(Graphics2D g2, Image img, int x, int y, int w, int h) {
        int imgW = img.getWidth(null);
        int imgH = img.getHeight(null);
        if (imgW <= 0 || imgH <= 0) {
            return;
        }
        double scale = Math.max((double) w / imgW, (double) h / imgH);
        int drawW = (int) (imgW * scale);
        int drawH = (int) (imgH * scale);
        int dx = x + (w - drawW) / 2;
        int dy = y + (h - drawH) / 2;
        g2.drawImage(img, dx, dy, drawW, drawH, null);
    }

    public void stop() {
        timer.stop();
    }

    private static class BannerSlide {
        private final String fileName;
        private final String message;

        BannerSlide(String fileName, String message) {
            this.fileName = fileName;
            this.message = message;
        }
    }
}
