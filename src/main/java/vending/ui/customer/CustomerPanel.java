package vending.ui.customer;

import vending.drink.DrinkCatalog;
import vending.drink.DrinkInfo;
import vending.kiosk.KioskService;
import vending.ui.theme.KioskColors;
import vending.ui.theme.KioskFonts;
import vending.ui.theme.UiKit;
import vending.ui.widget.ClockLabel;
import vending.ui.widget.VendingDialog;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.util.Locale;

public class CustomerPanel extends JPanel {

    private static final Color[] THUMB_COLORS = {
            new Color(196, 160, 110),
            new Color(150, 110, 80),
            new Color(120, 180, 220),
            new Color(130, 95, 70),
            new Color(100, 170, 130),
            new Color(90, 70, 55),
            new Color(210, 120, 90),
            new Color(170, 130, 200)
    };

    private final KioskService service;
    private final NumberFormat won = NumberFormat.getNumberInstance(Locale.KOREA);
    private final Runnable openAdmin;

    private final JLabel insertedLabel = new JLabel("0원", SwingConstants.LEFT);
    private final JLabel insertedDetailLabel = new JLabel("투입 내역 없음", SwingConstants.LEFT);
    private final JLabel selectedProductLabel = new JLabel("상품을 선택해주세요", SwingConstants.CENTER);
    private final JLabel selectedCountLabel = new JLabel("0개", SwingConstants.LEFT);
    private final JLabel selectedPriceLabel = new JLabel("0원", SwingConstants.LEFT);
    private final JLabel expectedChangeLabel = new JLabel("0원", SwingConstants.LEFT);
    private final JLabel hintLabel = new JLabel(" ", SwingConstants.LEFT);
    private final JButton resetBtn = UiKit.darkButton("전체 금액 취소");
    private final JButton returnBtn = UiKit.outlineButton("투입금 반환");
    private final JButton payBtn = UiKit.primaryButton("결제하기");
    private final JButton homeBtn = UiKit.outlineButton("처음으로");

    private final ProductCardPanel[] cards;
    private final BannerCarouselPanel bannerCarousel = new BannerCarouselPanel();
    private int selectedIndex = -1;
    private int adminTapCount;
    private long lastAdminTapMs;

    public CustomerPanel(KioskService service, Runnable openAdmin) {
        this.service = service;
        this.openAdmin = openAdmin;
        setBackground(KioskColors.BG);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));
        setPreferredSize(new Dimension(1040, 780));

        DrinkCatalog catalog = service.getCatalog();
        int n = catalog.drinkCount();
        cards = new ProductCardPanel[n];

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainContent(catalog, n), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("SMART VENDING");
        title.setFont(KioskFonts.title());
        title.setForeground(KioskColors.NAVY);
        title.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        title.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                long now = System.currentTimeMillis();
                if (now - lastAdminTapMs > 2500) {
                    adminTapCount = 0;
                }
                lastAdminTapMs = now;
                adminTapCount++;
                if (adminTapCount >= 5) {
                    adminTapCount = 0;
                    openAdmin.run();
                }
            }
        });

        JLabel mode = new JLabel("현금/상품 선택");
        mode.setFont(KioskFonts.small());
        mode.setForeground(KioskColors.SUBTEXT);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(mode);

        header.add(titleBox, BorderLayout.WEST);
        header.add(new ClockLabel(), BorderLayout.EAST);
        return header;
    }

    private JPanel buildMainContent(DrinkCatalog catalog, int n) {
        JPanel main = new JPanel(new BorderLayout(16, 0));
        main.setOpaque(false);
        main.add(buildProductArea(catalog, n), BorderLayout.CENTER);
        main.add(buildSidePanel(), BorderLayout.EAST);
        return main;
    }

    private JPanel buildProductArea(DrinkCatalog catalog, int n) {
        JPanel wrap = new JPanel(new BorderLayout(0, 12));
        wrap.setBackground(KioskColors.CARD);
        wrap.setBorder(BorderFactory.createCompoundBorder(
                UiKit.cardBorder(),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)));

        wrap.add(bannerCarousel, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 4, 12, 14));
        grid.setOpaque(false);
        for (int i = 0; i < n; i++) {
            DrinkInfo d = catalog.getDrink(i);
            cards[i] = new ProductCardPanel(i, d.getName(), d.getPrice(),
                    THUMB_COLORS[i % THUMB_COLORS.length], this::selectProduct);
            grid.add(cards[i]);
        }

        homeBtn.setPreferredSize(new Dimension(110, 36));
        homeBtn.addActionListener(e -> resetSelection());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(homeBtn);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(grid, BorderLayout.CENTER);
        center.add(bottom, BorderLayout.SOUTH);

        wrap.add(center, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildSidePanel() {
        JPanel side = new JPanel(new BorderLayout(0, 14));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(280, 560));
        side.add(buildAmountPanel(), BorderLayout.NORTH);
        side.add(buildSelectedPanel(), BorderLayout.CENTER);
        side.add(buildMoneyPanel(), BorderLayout.SOUTH);
        return side;
    }

    private JPanel buildAmountPanel() {
        JPanel box = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(KioskColors.NAVY);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.dispose();
            }
        };
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        box.setPreferredSize(new Dimension(10, 120));

        insertedLabel.setFont(KioskFonts.amount());
        insertedLabel.setForeground(Color.WHITE);
        insertedDetailLabel.setFont(KioskFonts.small());
        insertedDetailLabel.setForeground(new Color(220, 230, 245));

        JLabel title = new JLabel("투입 금액");
        title.setFont(KioskFonts.small());
        title.setForeground(new Color(200, 215, 235));

        JPanel center = new JPanel(new BorderLayout(0, 4));
        center.setOpaque(false);
        center.add(insertedLabel, BorderLayout.NORTH);
        center.add(insertedDetailLabel, BorderLayout.CENTER);

        box.add(title, BorderLayout.NORTH);
        box.add(center, BorderLayout.CENTER);
        return box;
    }

    private JPanel buildSelectedPanel() {
        JPanel box = new JPanel(new BorderLayout(0, 10));
        box.setBackground(KioskColors.CARD);
        box.setBorder(BorderFactory.createCompoundBorder(
                UiKit.cardBorder(),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));

        selectedProductLabel.setFont(KioskFonts.bodyBold());
        selectedProductLabel.setForeground(KioskColors.TEXT);

        JPanel info = new JPanel(new GridLayout(3, 2, 8, 4));
        info.setOpaque(false);
        info.add(labelOf("선택 수량"));
        info.add(selectedCountLabel);
        info.add(labelOf("상품 금액"));
        info.add(selectedPriceLabel);
        info.add(labelOf("예상 거스름돈"));
        info.add(expectedChangeLabel);
        selectedCountLabel.setFont(KioskFonts.bodyBold());
        selectedPriceLabel.setFont(KioskFonts.bodyBold());
        expectedChangeLabel.setFont(KioskFonts.bodyBold());

        box.add(selectedProductLabel, BorderLayout.NORTH);
        box.add(info, BorderLayout.CENTER);
        return box;
    }

    private JPanel buildMoneyPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(KioskColors.CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                UiKit.cardBorder(),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));

        JLabel moneyTitle = new JLabel("화폐 투입");
        moneyTitle.setFont(KioskFonts.section());

        JLabel limit = new JLabel("<html>10/50/100/500/1000원 · 최대 7,000원</html>");
        limit.setFont(KioskFonts.small());
        limit.setForeground(KioskColors.SUBTEXT);

        JPanel moneyGrid = new JPanel(new GridLayout(3, 2, 8, 8));
        moneyGrid.setOpaque(false);
        moneyGrid.add(coinBtn(10));
        moneyGrid.add(coinBtn(50));
        moneyGrid.add(coinBtn(100));
        moneyGrid.add(coinBtn(500));
        moneyGrid.add(billBtn(1000));

        resetBtn.setPreferredSize(new Dimension(240, 42));
        resetBtn.addActionListener(e -> onResetMoney());

        returnBtn.setPreferredSize(new Dimension(115, 48));
        returnBtn.addActionListener(e -> onReturn());

        payBtn.setPreferredSize(new Dimension(115, 48));
        payBtn.addActionListener(e -> onPay());

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setOpaque(false);
        actions.add(returnBtn);
        actions.add(payBtn);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(moneyTitle, BorderLayout.NORTH);
        top.add(limit, BorderLayout.CENTER);
        top.add(moneyGrid, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);
        panel.add(resetBtn, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(KioskColors.FOOTER);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
        };
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        footer.setPreferredSize(new Dimension(10, 48));

        hintLabel.setFont(KioskFonts.small());
        hintLabel.setForeground(new Color(220, 225, 235));

        JLabel guide = new JLabel("상품을 선택하고 금액을 투입한 후 결제 버튼을 눌러주세요.");
        guide.setFont(KioskFonts.small());
        guide.setForeground(new Color(190, 198, 210));

        JLabel contact = new JLabel("사용 문의 010-2571-0884");
        contact.setFont(KioskFonts.small());
        contact.setForeground(new Color(190, 198, 210));
        contact.setHorizontalAlignment(SwingConstants.RIGHT);

        footer.add(guide, BorderLayout.WEST);
        footer.add(hintLabel, BorderLayout.CENTER);
        footer.add(contact, BorderLayout.EAST);
        return footer;
    }

    private JLabel labelOf(String text) {
        JLabel l = new JLabel(text);
        l.setFont(KioskFonts.small());
        l.setForeground(KioskColors.SUBTEXT);
        return l;
    }

    private JButton coinBtn(int unit) {
        JButton btn = UiKit.accentMoneyButton("+" + won.format(unit) + "원");
        btn.addActionListener(e -> insert(unit));
        return btn;
    }

    private JButton billBtn(int unit) {
        JButton btn = UiKit.accentMoneyButton("+" + won.format(unit) + "원");
        btn.setPreferredSize(new Dimension(210, 44));
        btn.addActionListener(e -> insert(unit));
        return btn;
    }

    private void insert(int unit) {
        String err = service.insertMoney(unit);
        if (err != null) {
            showHint(err);
        } else {
            showHint(" ");
            refresh();
        }
    }

    private void selectProduct(int index) {
        selectedIndex = index;
        showHint(" ");
        refresh();
    }

    private void resetSelection() {
        selectedIndex = -1;
        showHint(" ");
        refresh();
    }

    private void onResetMoney() {
        if (service.insertedTotal() <= 0) {
            showHint("초기화할 투입 금액이 없습니다.");
            return;
        }
        String msg = service.returnInsertedMoney();
        showHint(msg.contains("완료") ? "투입 금액이 초기화되었습니다." : msg);
        if (msg.contains("완료")) {
            VendingDialog.showResult(this, "투입금 반환 완료", msg, true);
        }
        refresh();
    }

    private void onReturn() {
        if (service.insertedTotal() <= 0) {
            showHint("반환할 금액이 없습니다.");
            refresh();
            return;
        }
        String msg = service.returnInsertedMoney();
        showHint(msg);
        if (msg.contains("완료")) {
            VendingDialog.showResult(this, "화폐 반환 완료", msg, true);
        } else {
            VendingDialog.showResult(this, "화폐 반환 실패", msg, false);
        }
        refresh();
    }

    private void onPay() {
        if (selectedIndex < 0) {
            showHint("먼저 상품을 선택해주세요.");
            return;
        }
        if (service.isSoldOut(selectedIndex)) {
            showHint("품절입니다.");
            selectedIndex = -1;
            refresh();
            return;
        }
        if (!service.canBuy(selectedIndex)) {
            if (!service.canAfford(selectedIndex)) {
                showHint("금액이 부족합니다.");
            } else {
                showHint("거스름돈이 부족합니다.");
            }
            refresh();
            return;
        }
        String msg = service.buyDrink(selectedIndex);
        if (msg.startsWith("판매 완료")) {
            showHint(" ");
            selectedIndex = -1;
            VendingDialog.showResult(this, "결제 완료", msg, true);
        } else {
            showHint(msg);
            VendingDialog.showResult(this, "결제 실패", msg, false);
        }
        refresh();
    }

    public void refresh() {
        DrinkCatalog catalog = service.getCatalog();
        insertedLabel.setText(won.format(service.insertedTotal()) + "원");
        insertedDetailLabel.setText(service.insertedSummary());

        if (selectedIndex >= 0 && !service.isSoldOut(selectedIndex)) {
            DrinkInfo d = catalog.getDrink(selectedIndex);
            selectedProductLabel.setText(d.getName());
            selectedCountLabel.setText("1개");
            selectedPriceLabel.setText(won.format(d.getPrice()) + "원");
            expectedChangeLabel.setText(won.format(service.changeAmountFor(selectedIndex)) + "원");
        } else {
            selectedProductLabel.setText("상품을 선택해주세요");
            selectedCountLabel.setText("0개");
            selectedPriceLabel.setText("0원");
            expectedChangeLabel.setText("0원");
            if (selectedIndex >= 0 && service.isSoldOut(selectedIndex)) {
                selectedIndex = -1;
            }
        }

        for (int i = 0; i < cards.length; i++) {
            DrinkInfo d = catalog.getDrink(i);
            cards[i].update(
                    d.getName(),
                    d.getPrice(),
                    catalog.getStock(i).size(),
                    service.isSoldOut(i),
                    i == selectedIndex,
                    service.isAffordable(i)
            );
        }

        updateButtons();
    }

    private void updateButtons() {
        boolean hasMoney = service.insertedTotal() > 0;
        boolean hasSelection = selectedIndex >= 0 && !service.isSoldOut(selectedIndex);
        boolean canAfford = hasSelection && service.canAfford(selectedIndex);
        boolean canPay = hasSelection && service.canBuy(selectedIndex);

        resetBtn.setEnabled(hasMoney);
        returnBtn.setEnabled(hasMoney);
        payBtn.setEnabled(canPay);
        if (!hasSelection) {
            payBtn.setText("상품 선택");
        } else if (!canAfford) {
            payBtn.setText("금액 부족");
        } else if (!canPay) {
            payBtn.setText("거스름돈 부족");
        } else {
            payBtn.setText("결제하기");
        }

        if (hasSelection && canAfford && !canPay) {
            showHint("거스름돈이 부족합니다. 화폐를 보충하거나 반환을 눌러주세요.");
        }
    }

    public void showHint(String text) {
        hintLabel.setText(text == null || text.isBlank() ? " " : text);
    }
}
