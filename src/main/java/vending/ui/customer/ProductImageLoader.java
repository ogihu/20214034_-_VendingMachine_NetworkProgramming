package vending.ui.customer;

import vending.config.DataPaths;
import vending.util.AppLog;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
// 이미지로드기능
public final class ProductImageLoader {

    private static final String[] IMAGE_NAMES = {
            "믹스커피", "프리미엄커피", "생수", "캔커피",
            "이온음료", "고급캔커피", "탄산음료", "특화음료"
    };

    private ProductImageLoader() {
    }

    public static Image load(int index) {
        Image fromData = loadFile(DataPaths.PRODUCT_IMAGES.resolve(index + ".png"));
        if (fromData != null) {
            return fromData;
        }
        fromData = loadFile(DataPaths.PRODUCT_IMAGES.resolve(index + ".PNG"));
        if (fromData != null) {
            return fromData;
        }

        String name = imageName(index);
        Path projectImageDir = DataPaths.PROJECT_IMAGES;
        fromData = loadFile(projectImageDir.resolve(name + ".PNG"));
        if (fromData != null) {
            return fromData;
        }
        fromData = loadFile(projectImageDir.resolve(name + ".png"));
        if (fromData != null) {
            return fromData;
        }

        URL url = ProductImageLoader.class.getResource("/images/products/" + index + ".png");
        if (url != null) {
            ImageIcon icon = new ImageIcon(url);
            if (icon.getIconWidth() > 0) {
                return icon.getImage();
            }
        }
        return null;
    }

    public static Image fit(Image image, int maxW, int maxH) {
        if (image == null || maxW <= 0 || maxH <= 0) {
            return image;
        }
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        if (w <= 0 || h <= 0) {
            ImageIcon icon = new ImageIcon(image);
            w = icon.getIconWidth();
            h = icon.getIconHeight();
        }
        if (w <= 0 || h <= 0) {
            return image;
        }
        double scale = Math.min((double) maxW / w, (double) maxH / h);
        int drawW = Math.max(1, (int) Math.round(w * scale));
        int drawH = Math.max(1, (int) Math.round(h * scale));
        return image.getScaledInstance(drawW, drawH, Image.SCALE_SMOOTH);
    }

    private static Image loadFile(Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(path)) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                return null;
            }
            return trimTransparent(img);
        } catch (Exception e) {
            AppLog.warn("IMAGE", "이미지 로드 실패: " + path + " / " + e.getMessage());
            return null;
        }
    }

    private static BufferedImage trimTransparent(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int minX = w;
        int minY = h;
        int maxX = 0;
        int maxY = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xff;
                if (alpha > 16) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return img;
        }
        int cropW = maxX - minX + 1;
        int cropH = maxY - minY + 1;
        if (cropW == w && cropH == h) {
            return img;
        }
        return img.getSubimage(minX, minY, cropW, cropH);
    }

    private static String imageName(int index) {
        if (index >= 0 && index < IMAGE_NAMES.length) {
            return IMAGE_NAMES[index];
        }
        return String.valueOf(index);
    }
}
