package vending.ui.boot;

import vending.ui.theme.AppIcon;
import vending.ui.theme.KioskColors;
import vending.ui.theme.KioskFonts;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

// 부팅화면기능
public class BootSplashFrame extends JFrame {

    public interface OnBootFinished {
        void run();
    }

    private static final String[] STEPS = {
            "하드웨어 점검 중...",
            "음료 재고 불러오는 중...",
            "결제 모듈 준비 중...",
            "서버 연결 확인 중...",
            "키오스크 시작"
    };

    private final JLabel stepLabel = new JLabel(STEPS[0], SwingConstants.CENTER);
    private final JProgressBar progressBar = new JProgressBar(0, STEPS.length);
    private int stepIndex;
    private Timer bootTimer;

    public BootSplashFrame(String clientId, OnBootFinished callback) {
        super("Starting...");
        AppIcon.applyToFrame(this);
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(520, 300));
        setLocationRelativeTo(null);

        JPanel shell = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(KioskColors.BORDER);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 14, 14));
                g2.dispose();
            }
        };
        shell.setOpaque(false);
        shell.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        JLabel logo = new JLabel("▰", SwingConstants.CENTER);
        logo.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 34));
        logo.setForeground(KioskColors.BLUE);

        JLabel title = new JLabel("20214034_정영웅_자판기프로그램", SwingConstants.CENTER);
        title.setFont(KioskFonts.title());
        title.setForeground(KioskColors.BLUE_DARK);

        JLabel sub = new JLabel("SMART VENDING · 단말 " + clientId, SwingConstants.CENTER);
        sub.setFont(KioskFonts.small());
        sub.setForeground(KioskColors.SUBTEXT);

        stepLabel.setFont(KioskFonts.body());
        stepLabel.setForeground(KioskColors.TEXT);

        progressBar.setValue(0);
        progressBar.setForeground(KioskColors.BLUE);
        progressBar.setBackground(KioskColors.BG);
        progressBar.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(logo, BorderLayout.NORTH);
        top.add(title, BorderLayout.CENTER);
        top.add(sub, BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(stepLabel, BorderLayout.NORTH);
        bottom.add(progressBar, BorderLayout.SOUTH);

        shell.add(top, BorderLayout.NORTH);
        shell.add(bottom, BorderLayout.SOUTH);
        setContentPane(shell);

        bootTimer = new Timer(520, e -> nextStep(callback));
        bootTimer.setInitialDelay(350);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                bootTimer.start();
            }
        });
    }

    private void nextStep(OnBootFinished callback) {
        stepIndex++;
        progressBar.setValue(stepIndex);

        if (stepIndex >= STEPS.length) {
            bootTimer.stop();
            dispose();
            callback.run();
            return;
        }
        stepLabel.setText(STEPS[stepIndex]);
    }
}
