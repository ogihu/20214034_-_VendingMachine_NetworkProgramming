package vending.ui.customer;

import vending.drink.DrinkCatalog;
import vending.drink.DrinkInfo;
import vending.kiosk.KioskService;
import vending.ui.theme.KioskColors;
import vending.ui.theme.KioskFonts;
import vending.ui.theme.UiKit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 이미지 레퍼런스와 같은 흐름의 고객 결제 화면.
 * 상품 선택 → 화폐 투입 → 결제하기
 */
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

    private final JLabel insertedLabel = new JLabel("0원", SwingConstants.LEFT);
    private final JLabel selectedCountLabel = new JLabel("0개", SwingConstants.LEFT);
    private final JLabel selectedPriceLabel = new JLabel("0원", SwingConstants.LEFT);
    private final JLabel serverStatusLabel = new JLabel("서버 연결: 확인 중");
    private final JLabel changeStatusLabel = new JLabel("거스름돈: 확인 중");
    private final JLabel hintLabel = new JLabel(" ", SwingConstants.CENTER);

    private final ProductCardPanel[] cards;
    private int selectedIndex = -1;

    public CustomerPanel(KioskService service) {
        this.service = service;
        setBackground(KioskColors.BG);
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        DrinkCatalog catalog = service.getCatalog();
        int n = catalog.drinkCount();
        cards = new ProductCardPanel[n];

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSummary(), BorderLayout.PAGE_START);
        add(buildProductArea(catalog, n), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("SMART VENDING MACHINE");
        title.setFont(KioskFonts.title());
        title.setForeground(KioskColors.TEXT);

        JPanel status = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        status.setOpaque(false);
        styleStatus(serverStatusLabel, KioskColors.SUBTEXT);
        styleStatus(changeStatusLabel, KioskColors.SUBTEXT);
        status.add(serverStatusLabel);
        status.add(changeStatusLabel);

        header.add(title, BorderLayout.WEST);
        header.add(status, BorderLayout.EAST);
        return header;
    }

    private void styleStatus(JLabel label, Color color) {
        label.setFont(KioskFonts.small());
        label.setForeground(color);
        label.setIcon(new DotIcon(KioskColors.GREEN));
        label.setIconTextGap(6);
    }

    private JPanel buildSummary() {
        JPanel box = new JPanel(new BorderLayout(16, 0));
        box.setBackground(KioskColors.CARD);
        box.setBorder(UiKit.cardBorder());

        insertedLabel.setFont(KioskFonts.amount());
        insertedLabel.setForeground(KioskColors.BLUE);

        selectedCountLabel.setFont(KioskFonts.bodyBold());
        selectedPriceLabel.setFont(KioskFonts.bodyBold());

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 8));
        left.add(labeled("투입 금액", insertedLabel), BorderLayout.CENTER);

        JPanel mid = new JPanel(new GridLayout(2, 2, 10, 2));
        mid.setOpaque(false);
        mid.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
        mid.add(labelOf("선택한 상품"));
        mid.add(selectedCountLabel);
        mid.add(labelOf("상품 금액"));
        mid.add(selectedPriceLabel);

        JButton resetBtn = UiKit.outlineButton("금액 초기화");
        resetBtn.setPreferredSize(new Dimension(108, 34));
        resetBtn.addActionListener(e -> onResetMoney());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 24));
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 14));
        right.add(resetBtn);

        box.add(left, BorderLayout.CENTER);
        box.add(mid, BorderLayout.EAST);
        box.add(right, BorderLayout.LINE_END);
        return box;
    }

    private JPanel labeled(String title, JLabel value) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        p.add(labelOf(title), BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        return p;
    }

    private JLabel labelOf(String text) {
        JLabel l = new JLabel(text);
        l.setFont(KioskFonts.small());
        l.setForeground(KioskColors.SUBTEXT);
        return l;
    }

    private JPanel buildProductArea(DrinkCatalog catalog, int n) {
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);

        JLabel section = new JLabel("상품 선택");
        section.setFont(KioskFonts.section());
        section.setForeground(KioskColors.TEXT);

        JPanel grid = new JPanel(new GridLayout(2, 4, 12, 12));
        grid.setOpaque(false);

        for (int i = 0; i < n; i++) {
            DrinkInfo d = catalog.getDrink(i);
            int idx = i;
            cards[i] = new ProductCardPanel(i, d.getName(), d.getPrice(), THUMB_COLORS[i % THUMB_COLORS.length],
                    index -> selectProduct(index));
            grid.add(cards[i]);
        }

        wrap.add(section, BorderLayout.NORTH);
        wrap.add(grid, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildBottom() {
        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setOpaque(false);

        JLabel moneyTitle = new JLabel("화폐 투입");
        moneyTitle.setFont(KioskFonts.section());

        JLabel limit = new JLabel("최대 투입 가능 금액: 7,000원");
        limit.setFont(KioskFonts.small());
        limit.setForeground(KioskColors.SUBTEXT);

        JPanel moneyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        moneyRow.setOpaque(false);
        moneyRow.add(coinBtn(10));
        moneyRow.add(coinBtn(50));
        moneyRow.add(coinBtn(100));
        moneyRow.add(coinBtn(500));
        moneyRow.add(billBtn(1000));

        JPanel moneyBox = new JPanel(new BorderLayout(0, 8));
        moneyBox.setOpaque(false);
        moneyBox.add(moneyTitle, BorderLayout.NORTH);
        moneyBox.add(limit, BorderLayout.CENTER);
        moneyBox.add(moneyRow, BorderLayout.SOUTH);

        JButton returnBtn = UiKit.outlineButton("화폐 반환");
        returnBtn.setPreferredSize(new Dimension(140, 46));
        returnBtn.addActionListener(e -> onReturn());

        JButton payBtn = UiKit.primaryButton("결제하기");
        payBtn.setPreferredSize(new Dimension(160, 46));
        payBtn.addActionListener(e -> onPay());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(returnBtn);
        actions.add(payBtn);

        hintLabel.setFont(KioskFonts.small());
        hintLabel.setForeground(KioskColors.RED);

        JLabel guide = new JLabel("상품을 선택하고 금액을 투입한 후 결제 버튼을 눌러주세요.");
        guide.setFont(KioskFonts.small());
        guide.setForeground(KioskColors.SUBTEXT);
        guide.setHorizontalAlignment(SwingConstants.CENTER);

        bottom.add(moneyBox, BorderLayout.NORTH);
        bottom.add(actions, BorderLayout.CENTER);
        bottom.add(hintLabel, BorderLayout.SOUTH);
        bottom.add(guide, BorderLayout.PAGE_END);
        return bottom;
    }

    private JButton coinBtn(int unit) {
        JButton btn = UiKit.moneyButton(won.format(unit), false);
        btn.addActionListener(e -> insert(unit));
        return btn;
    }

    private JButton billBtn(int unit) {
        JButton btn = UiKit.moneyButton(won.format(unit), true);
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

    private void onResetMoney() {
        if (service.insertedTotal() <= 0) {
            showHint("초기화할 투입 금액이 없습니다.");
            return;
        }
        String msg = service.returnInsertedMoney();
        showHint(msg.contains("완료") ? "투입 금액이 초기화되었습니다." : msg);
        refresh();
    }

    private void onReturn() {
        String msg = service.returnInsertedMoney();
        showHint(msg);
        refresh();
    }

    private void onPay() {
        if (selectedIndex < 0) {
            showHint("먼저 상품을 선택해주세요.");
            return;
        }
        String msg = service.buyDrink(selectedIndex);
        if (msg.startsWith("판매 완료")) {
            showHint(" ");
            selectedIndex = -1;
            JOptionPane.showMessageDialog(this, msg, "결제 완료", JOptionPane.INFORMATION_MESSAGE);
        } else {
            showHint(msg);
        }
        refresh();
    }

    public void refresh() {
        DrinkCatalog catalog = service.getCatalog();
        insertedLabel.setText(won.format(service.insertedTotal()) + "원");

        if (selectedIndex >= 0 && !service.isSoldOut(selectedIndex)) {
            DrinkInfo d = catalog.getDrink(selectedIndex);
            selectedCountLabel.setText("1개");
            selectedPriceLabel.setText(won.format(d.getPrice()) + "원");
        } else {
            selectedCountLabel.setText("0개");
            selectedPriceLabel.setText("0원");
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

        updateStatusLabels();
    }

    private void updateStatusLabels() {
        serverStatusLabel.setText("서버 연결: " + service.getServerStatusText());
        changeStatusLabel.setText("거스름돈: " + service.getChangeStatusText());

        Color dot = service.isChangeOk() ? KioskColors.GREEN : KioskColors.RED;
        serverStatusLabel.setIcon(new DotIcon(service.isServerOk() ? KioskColors.GREEN : KioskColors.RED));
        changeStatusLabel.setIcon(new DotIcon(dot));
    }

    public void showHint(String text) {
        hintLabel.setText(text == null || text.isBlank() ? " " : text);
    }

    /** 상태 표시용 작은 원 */
    private static class DotIcon implements javax.swing.Icon {
        private final Color color;

        DotIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillOval(x, y + 2, 8, 8);
        }

        @Override
        public int getIconWidth() {
            return 10;
        }

        @Override
        public int getIconHeight() {
            return 12;
        }
    }
}
