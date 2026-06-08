package vending.ui.admin;

import vending.coin.CoinInventory;
import vending.coin.CoinSlot;
import vending.config.DataPaths;
import vending.drink.DrinkCatalog;
import vending.drink.DrinkInfo;
import vending.kiosk.KioskService;
import vending.ui.customer.CustomerPanel;
import vending.ui.customer.ProductImageLoader;
import vending.ui.theme.KioskColors;
import vending.ui.theme.KioskFonts;
import vending.ui.theme.UiKit;
import vending.ui.widget.ClockLabel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

public class AdminPanel extends JPanel {

    private enum AdminView {
        DASHBOARD("재고 · 화폐 · 매출 · 이미지 관리"),
        DRINK_SETTING("음료 설정 · 이름, 가격, 재고 관리"),
        DRINK_RESTOCK("선택한 음료 재고를 보충합니다"),
        COIN_REFILL("화폐 단위별 보충 수량을 입력합니다"),
        COLLECT("수금 가능 금액을 확인하고 수금합니다"),
        PASSWORD("관리자 비밀번호를 변경합니다"),
        SERVER("서버 매출 집계 정보를 확인합니다"),
        IMAGE_CHANGE("선택한 음료 이미지를 변경합니다"),
        SALES("일별/월별 매출을 조회합니다"),
        REMOTE("다른 클라이언트 음료 정보를 원격 변경합니다");

        private final String subtitle;

        AdminView(String subtitle) {
            this.subtitle = subtitle;
        }
    }

    private final KioskService service;
    private final CustomerPanel customerPanel;
    private final Runnable onBack;
    private final NumberFormat won = NumberFormat.getNumberInstance(Locale.KOREA);

    private final AdminStatCard todayCard = new AdminStatCard("오늘 매출", KioskColors.BLUE);
    private final AdminStatCard totalCard = new AdminStatCard("누적 매출", KioskColors.GREEN);
    private final AdminStatCard sessionCard = new AdminStatCard("세션 매출", new Color(230, 120, 30));
    private final AdminStatCard balanceCard = new AdminStatCard("현재 잔고", new Color(120, 80, 180));
    private final JLabel pageSubtitle = new JLabel(AdminView.DASHBOARD.subtitle);
    private final JLabel alertLabel = new JLabel("알림 없음");
    private final JLabel serverBadgeDot = new JLabel("●");
    private final JLabel serverBadgeText = new JLabel("서버 확인 중");
    private final JLabel serverSummaryTotal = new JLabel("-");
    private final JLabel serverSummaryToday = new JLabel("-");
    private final JLabel serverSummaryClients = new JLabel("-");

    private DefaultTableModel drinkModel;
    private DefaultTableModel settingDrinkModel;
    private DefaultTableModel coinModel;
    private JTable drinkTable;
    private JTable settingDrinkTable;

    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentHost = new JPanel(contentLayout);
    private final Map<AdminView, JButton> menuButtons = new HashMap<>();
    private AdminView currentView = AdminView.DASHBOARD;

    private final JLabel settingPreview = new JLabel("", SwingConstants.CENTER);
    private final JTextField settingNameField = new JTextField(16);
    private final JTextField settingPriceField = new JTextField(8);
    private final JTextField settingStockField = new JTextField(8);
    private final JLabel settingStatusLabel = new JLabel(" ");

    private final JComboBox<String> restockDrinkBox = new JComboBox<>();
    private final JTextField restockCountField = new JTextField("10", 8);
    private final JLabel restockStatusLabel = new JLabel(" ");
    private final JTextArea restockSideNote = AdminFormKit.noteArea("");

    private final JComboBox<Integer> coinUnitBox = new JComboBox<>(new Integer[]{1000, 500, 100, 50, 10});
    private final JTextField coinCountField = new JTextField("10", 8);
    private final JLabel coinStatusLabel = new JLabel(" ");
    private final JTextArea coinSideNote = AdminFormKit.noteArea("");

    private final JTextField collectAmountField = new JTextField(12);
    private final JLabel collectMaxValue = new JLabel("0원");
    private final JLabel collectStatusLabel = new JLabel(" ");
    private final JTextArea collectSideNote = AdminFormKit.noteArea("");

    private final JPasswordField newPasswordField = new JPasswordField(16);
    private final JPasswordField confirmPasswordField = new JPasswordField(16);
    private final JLabel passwordStatusLabel = new JLabel(" ");

    private final JTextArea serverSummaryArea = new JTextArea(12, 40);
    private final JLabel serverStatusDetail = new JLabel(" ");

    private final JComboBox<String> imageDrinkBox = new JComboBox<>();
    private final JLabel imagePreview = new JLabel("", SwingConstants.CENTER);
    private final JLabel imageStatusLabel = new JLabel(" ");

    private final JComboBox<String> remoteDrinkBox = new JComboBox<>();
    private final JTextField remoteClientField = new JTextField("Client2", 10);
    private final JTextField remoteNameField = new JTextField(12);
    private final JTextField remotePriceField = new JTextField(8);
    private final JLabel remoteStatusLabel = new JLabel(" ");

    private boolean refreshing;

    public AdminPanel(KioskService service, CustomerPanel customerPanel, Runnable onBack) {
        this.service = service;
        this.customerPanel = customerPanel;
        this.onBack = onBack;

        setPreferredSize(new Dimension(1040, 780));
        setBackground(KioskColors.BG);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        add(buildTopHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        showView(AdminView.DASHBOARD);
    }

    private JPanel buildTopHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("관리자 설정");
        title.setFont(KioskFonts.title());
        title.setForeground(KioskColors.NAVY);

        pageSubtitle.setFont(KioskFonts.small());
        pageSubtitle.setForeground(KioskColors.SUBTEXT);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(pageSubtitle);

        JButton backBtn = UiKit.outlineButton("← 고객 화면으로");
        backBtn.addActionListener(e -> onBack.run());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(new ClockLabel());
        right.add(backBtn);

        header.add(titleBox, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);
        body.add(buildSidebar(), BorderLayout.WEST);
        body.add(buildContentHost(), BorderLayout.CENTER);
        return body;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(KioskColors.NAVY);
        side.setPreferredSize(new Dimension(210, 10));
        side.setBorder(BorderFactory.createEmptyBorder(18, 14, 18, 14));

        JLabel brand = new JLabel("<html><b>SMART VENDING</b><br><span style='font-size:11px'>관리자 시스템</span></html>");
        brand.setFont(KioskFonts.bodyBold());
        brand.setForeground(Color.WHITE);

        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new GridLayout(0, 1, 0, 4));

        addMenuSection(menu, "재고 관리",
                entry("재고 현황", AdminView.DASHBOARD),
                entry("음료 설정", AdminView.DRINK_SETTING),
                entry("음료 보충", AdminView.DRINK_RESTOCK));
        menu.add(gap());
        addMenuSection(menu, "화폐 관리",
                entry("화폐 보충", AdminView.COIN_REFILL),
                entry("수금", AdminView.COLLECT));
        menu.add(gap());
        addMenuSection(menu, "시스템 관리",
                entry("비밀번호 변경", AdminView.PASSWORD),
                entry("서버 집계", AdminView.SERVER),
                entry("이미지 변경", AdminView.IMAGE_CHANGE));
        menu.add(gap());
        addMenuSection(menu, "매출 관리",
                entry("일별/월별 매출", AdminView.SALES),
                entry("원격 음료 변경", AdminView.REMOTE));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(buildServerStatusBadge(), BorderLayout.SOUTH);

        side.add(brand, BorderLayout.NORTH);
        side.add(menu, BorderLayout.CENTER);
        side.add(bottom, BorderLayout.SOUTH);
        return side;
    }

    private JPanel buildServerStatusBadge() {
        serverBadgeDot.setFont(KioskFonts.small());
        serverBadgeText.setFont(KioskFonts.small());
        serverBadgeText.setForeground(new Color(190, 205, 225));

        JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badge.setOpaque(false);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 35)),
                BorderFactory.createEmptyBorder(12, 6, 0, 6)));
        badge.add(serverBadgeDot);
        badge.add(serverBadgeText);
        return badge;
    }

    private void updateServerBadge() {
        boolean ok = "정상".equals(service.getServerStatusText());
        serverBadgeDot.setForeground(ok ? KioskColors.GREEN : new Color(230, 120, 30));
        serverBadgeText.setText("서버 " + service.getServerStatusText());
    }

    private void addMenuSection(JPanel menu, String title, Map.Entry<String, AdminView>... items) {
        menu.add(sectionLabel(title));
        for (Map.Entry<String, AdminView> item : items) {
            menu.add(createMenuButton(item.getKey(), item.getValue()));
        }
    }

    private Map.Entry<String, AdminView> entry(String label, AdminView view) {
        return new LinkedHashMap.SimpleEntry<>(label, view);
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(KioskFonts.small());
        l.setForeground(new Color(170, 190, 220));
        l.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 0));
        return l;
    }

    private JPanel gap() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(1, 6));
        return p;
    }

    private JButton createMenuButton(String text, AdminView view) {
        JButton btn = new JButton(text);
        btn.setFont(KioskFonts.small());
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        btn.setForeground(new Color(210, 220, 235));
        btn.addActionListener(e -> showView(view));
        menuButtons.put(view, btn);
        return btn;
    }

    private JPanel buildContentHost() {
        contentHost.setOpaque(false);
        contentHost.setBackground(KioskColors.BG);
        contentHost.setMinimumSize(new Dimension(760, 620));
        contentHost.setPreferredSize(new Dimension(760, 620));
        contentHost.add(buildDashboardView(), AdminView.DASHBOARD.name());
        contentHost.add(buildDrinkSettingView(), AdminView.DRINK_SETTING.name());
        contentHost.add(buildDrinkRestockView(), AdminView.DRINK_RESTOCK.name());
        contentHost.add(buildCoinRefillView(), AdminView.COIN_REFILL.name());
        contentHost.add(buildCollectView(), AdminView.COLLECT.name());
        contentHost.add(buildPasswordView(), AdminView.PASSWORD.name());
        contentHost.add(buildServerView(), AdminView.SERVER.name());
        contentHost.add(buildImageChangeView(), AdminView.IMAGE_CHANGE.name());
        contentHost.add(new AdminSalesPanel(service), AdminView.SALES.name());
        contentHost.add(buildRemoteView(), AdminView.REMOTE.name());
        return contentHost;
    }

    private JPanel buildDashboardView() {
        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setOpaque(false);

        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setOpaque(false);
        stats.add(todayCard);
        stats.add(totalCard);
        stats.add(sessionCard);
        stats.add(balanceCard);

        drinkModel = createDrinkModel();
        drinkTable = createDrinkTable(drinkModel, 58);
        drinkTable.getColumnModel().getColumn(0).setPreferredWidth(68);
        drinkTable.getColumnModel().getColumn(0).setMaxWidth(72);
        drinkTable.getColumnModel().getColumn(0).setMinWidth(64);
        drinkTable.getColumnModel().getColumn(1).setPreferredWidth(120);

        coinModel = createCoinModel();
        JTable coinTable = createCoinTable(coinModel);

        JPanel tables = new JPanel(new GridLayout(1, 2, 14, 0));
        tables.setOpaque(false);
        tables.add(AdminTableKit.panel("재고 현황", drinkTable));
        tables.add(AdminTableKit.panel("화폐 보유 현황", coinTable));

        JPanel alert = alertPanel();
        JPanel quick = buildQuickActions();

        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        bottom.add(alert, BorderLayout.NORTH);
        bottom.add(quick, BorderLayout.SOUTH);

        main.add(stats, BorderLayout.NORTH);
        main.add(tables, BorderLayout.CENTER);
        main.add(bottom, BorderLayout.SOUTH);
        return main;
    }

    private JPanel buildDrinkSettingView() {
        settingDrinkModel = createDrinkModel();
        settingDrinkTable = createDrinkTable(settingDrinkModel, 72);
        settingDrinkTable.getColumnModel().getColumn(0).setPreferredWidth(76);
        settingDrinkTable.getColumnModel().getColumn(0).setMaxWidth(80);
        settingDrinkTable.getColumnModel().getColumn(0).setMinWidth(72);
        settingDrinkTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        settingDrinkTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSettingSelection(settingDrinkTable.getSelectedRow());
            }
        });

        settingPreview.setPreferredSize(new Dimension(180, 200));
        settingPreview.setMinimumSize(new Dimension(180, 200));
        settingPreview.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KioskColors.BORDER),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        AdminFormKit.styleField(settingNameField);
        AdminFormKit.styleField(settingPriceField);
        AdminFormKit.styleField(settingStockField);
        JPanel form = new JPanel(new GridLayout(4, 2, 8, 10));
        form.setOpaque(false);
        form.add(label("음료명"));
        form.add(settingNameField);
        form.add(label("가격"));
        form.add(settingPriceField);
        form.add(label("재고"));
        form.add(settingStockField);
        form.add(new JLabel());
        JButton saveBtn = UiKit.primaryButton("저장");
        saveBtn.addActionListener(e -> saveDrinkSetting());
        form.add(saveBtn);

        settingStatusLabel.setFont(KioskFonts.small());
        settingStatusLabel.setForeground(KioskColors.SUBTEXT);

        JPanel right = new JPanel(new BorderLayout(0, 12));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(220, 10));
        right.add(settingPreview, BorderLayout.NORTH);
        right.add(form, BorderLayout.CENTER);
        right.add(settingStatusLabel, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);
        body.add(AdminTableKit.wrap(settingDrinkTable), BorderLayout.CENTER);
        body.add(right, BorderLayout.EAST);
        JPanel inner = new JPanel(new BorderLayout(0, 10));
        inner.setOpaque(false);
        inner.add(body, BorderLayout.CENTER);
        return AdminFormKit.page("🥤", "음료 설정",
                "목록에서 음료를 선택하고 이름, 가격, 재고를 수정한 뒤 저장하세요.", inner);
    }

    private JPanel buildDrinkRestockView() {
        JButton runBtn = UiKit.primaryButton("보충 실행");
        runBtn.addActionListener(e -> runRestock());
        restockStatusLabel.setForeground(KioskColors.SUBTEXT);
        JPanel form = AdminFormKit.formBody(
                AdminFormKit.fieldRow("음료 선택", restockDrinkBox, "보충할 음료를 선택하세요."),
                AdminFormKit.fieldRow("보충 수량", restockCountField, "1개 이상의 숫자를 입력하세요."),
                AdminFormKit.actionRow(runBtn, restockStatusLabel)
        );
        JPanel body = AdminFormKit.splitBody(form,
                AdminFormKit.sidePanel("현재 재고 현황", restockSideNote));
        return AdminFormKit.page("📦", "음료 보충",
                "선택한 음료의 재고를 입력한 수량만큼 추가합니다.", body);
    }

    private JPanel buildCoinRefillView() {
        JButton runBtn = UiKit.primaryButton("보충 실행");
        runBtn.addActionListener(e -> runCoinRefill());
        coinStatusLabel.setForeground(KioskColors.SUBTEXT);
        JPanel form = AdminFormKit.formBody(
                AdminFormKit.fieldRow("화폐 단위", coinUnitBox, "보충할 화폐 종류를 선택하세요."),
                AdminFormKit.fieldRow("보충 수량", coinCountField, "추가할 매수를 입력하세요."),
                AdminFormKit.actionRow(runBtn, coinStatusLabel)
        );
        JPanel body = AdminFormKit.splitBody(form,
                AdminFormKit.sidePanel("화폐 보유 현황", coinSideNote));
        return AdminFormKit.page("💴", "화폐 보충",
                "자판기 내부 화폐 재고를 보충합니다.", body);
    }

    private JPanel buildCollectView() {
        JButton runBtn = UiKit.primaryButton("수금 실행");
        runBtn.addActionListener(e -> runCollect());
        collectStatusLabel.setForeground(KioskColors.SUBTEXT);
        JPanel form = AdminFormKit.formBody(
                AdminFormKit.infoBox("최대 수금 가능", collectMaxValue),
                AdminFormKit.fieldRow("수금액", collectAmountField, "수금할 금액을 입력하세요."),
                AdminFormKit.actionRow(runBtn, collectStatusLabel)
        );
        JPanel body = AdminFormKit.splitBody(form,
                AdminFormKit.sidePanel("수금 안내", collectSideNote));
        return AdminFormKit.page("💰", "수금",
                "자판기에 모인 현금 중 일부를 수금합니다.", body);
    }

    private JPanel buildPasswordView() {
        JButton runBtn = UiKit.primaryButton("비밀번호 변경");
        runBtn.addActionListener(e -> runPasswordChange());
        passwordStatusLabel.setForeground(KioskColors.SUBTEXT);
        JPanel form = AdminFormKit.formBody(
                AdminFormKit.fieldRow("새 비밀번호", newPasswordField, "8자 이상, 숫자와 특수문자 포함"),
                AdminFormKit.fieldRow("비밀번호 확인", confirmPasswordField, "같은 비밀번호를 다시 입력하세요."),
                AdminFormKit.actionRow(runBtn, passwordStatusLabel)
        );
        JPanel body = AdminFormKit.splitBody(form, AdminFormKit.sidePanel("비밀번호 규칙",
                AdminFormKit.noteArea(
                        "· 8자 이상 입력\n"
                                + "· 숫자 1개 이상 포함\n"
                                + "· 특수문자 1개 이상 포함\n"
                                + "· 변경 후 다음 로그인부터 적용\n"
                                + "· 분실 시 data/admin/password.txt 확인")));
        return AdminFormKit.page("🔐", "비밀번호 변경",
                "관리자 로그인에 사용할 새 비밀번호를 설정합니다.", body);
    }

    private JPanel buildServerView() {
        JButton refreshBtn = UiKit.primaryButton("서버 정보 새로고침");
        refreshBtn.addActionListener(e -> refreshServerSummary());

        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
        stats.setOpaque(false);
        stats.add(serverMiniCard("전체 누적", serverSummaryTotal, KioskColors.BLUE));
        stats.add(serverMiniCard("오늘 매출", serverSummaryToday, KioskColors.GREEN));
        stats.add(serverMiniCard("연결 클라이언트", serverSummaryClients, new Color(120, 80, 180)));

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(stats, BorderLayout.NORTH);
        body.add(AdminFormKit.summaryArea(serverSummaryArea), BorderLayout.CENTER);
        body.add(AdminFormKit.actionRow(refreshBtn, serverStatusDetail), BorderLayout.SOUTH);

        return AdminFormKit.page("📊", "서버 집계",
                "서버에서 조회한 매출·재고 집계 정보입니다. 연결이 안 되면 로컬 정보를 함께 표시합니다.", body);
    }

    private JPanel serverMiniCard(String title, JLabel value, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(KioskColors.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KioskColors.BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel t = new JLabel(title);
        t.setFont(KioskFonts.small());
        t.setForeground(KioskColors.SUBTEXT);
        value.setFont(KioskFonts.section());
        value.setForeground(color);
        card.add(t, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildImageChangeView() {
        imageDrinkBox.addActionListener(e -> updateImagePreview());
        JButton pickBtn = UiKit.outlineButton("이미지 파일 선택");
        pickBtn.addActionListener(e -> pickImageFile());
        JButton saveBtn = UiKit.primaryButton("이미지 적용");
        saveBtn.addActionListener(e -> applyImageChange());
        imageStatusLabel.setForeground(KioskColors.SUBTEXT);

        JPanel left = AdminFormKit.formBody(
                AdminFormKit.fieldRow("음료 선택", imageDrinkBox, "이미지를 바꿀 음료를 선택하세요.")
        );
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(AdminFormKit.previewPanel(imagePreview, 180, 200));
        right.add(Box.createVerticalStrut(12));
        JPanel btns = new JPanel(new GridLayout(1, 2, 8, 0));
        btns.setOpaque(false);
        btns.setMaximumSize(new Dimension(420, 44));
        btns.add(pickBtn);
        btns.add(saveBtn);
        right.add(btns);
        right.add(Box.createVerticalStrut(8));
        right.add(imageStatusLabel);

        JPanel split = new JPanel(new GridLayout(1, 2, 24, 0));
        split.setOpaque(false);
        split.add(left);
        split.add(right);

        return AdminFormKit.page("🖼", "이미지 변경",
                "선택한 음료의 상품 이미지를 새 파일로 교체합니다.", split);
    }

    private JPanel buildRemoteView() {
        remoteDrinkBox.addActionListener(e -> loadRemoteDefaults());
        JButton runBtn = UiKit.primaryButton("원격 변경 요청");
        runBtn.addActionListener(e -> runRemoteChange());
        remoteStatusLabel.setForeground(KioskColors.SUBTEXT);
        JPanel form = AdminFormKit.formBody(
                AdminFormKit.fieldRow("대상 클라이언트", remoteClientField, "예: Client2, Client3"),
                AdminFormKit.fieldRow("음료 선택", remoteDrinkBox, "변경할 음료 슬롯을 선택하세요."),
                AdminFormKit.fieldRow("새 음료명", remoteNameField, "변경 후 표시될 이름"),
                AdminFormKit.fieldRow("새 가격", remotePriceField, "10원 단위 가격"),
                AdminFormKit.actionRow(runBtn, remoteStatusLabel)
        );
        JPanel body = AdminFormKit.splitBody(form, AdminFormKit.sidePanel("원격 변경 안내",
                AdminFormKit.noteArea(
                        "· 서버를 통해 다른 클라이언트에 변경 명령 전송\n"
                                + "· 대상 클라이언트가 온라인이어야 반영\n"
                                + "· 가격은 10원 단위로 입력\n"
                                + "· 변경 내역은 서버 집계에 기록")));
        return AdminFormKit.page("🌐", "원격 음료 변경",
                "다른 클라이언트의 음료 정보 변경을 서버에 요청합니다.", body);
    }

    private JPanel buildQuickActions() {
        JPanel quick = new JPanel(new GridLayout(2, 4, 10, 10));
        quick.setOpaque(false);
        quick.add(quickBtn("음료 설정", AdminView.DRINK_SETTING));
        quick.add(quickBtn("음료 보충", AdminView.DRINK_RESTOCK));
        quick.add(quickBtn("화폐 보충", AdminView.COIN_REFILL));
        quick.add(quickBtn("수금", AdminView.COLLECT));
        quick.add(quickBtn("비밀번호", AdminView.PASSWORD));
        quick.add(quickBtn("서버 집계", AdminView.SERVER));
        quick.add(quickBtn("매출 조회", AdminView.SALES));
        quick.add(quickBtn("이미지 변경", AdminView.IMAGE_CHANGE));
        return quick;
    }

    private JButton quickBtn(String text, AdminView view) {
        JButton btn = UiKit.outlineButton(text);
        btn.setPreferredSize(new Dimension(120, 42));
        btn.addActionListener(e -> showView(view));
        return btn;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(KioskFonts.bodyBold());
        return l;
    }

    private DefaultTableModel createDrinkModel() {
        return new DefaultTableModel(new String[]{"", "음료명", "재고", "가격", "상태"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private DefaultTableModel createCoinModel() {
        return new DefaultTableModel(new String[]{"화폐 단위", "수량", "금액"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JTable createDrinkTable(DefaultTableModel model, int rowHeight) {
        JTable table = new JTable(model);
        table.setRowHeight(rowHeight);
        AdminTableKit.style(table);
        table.getColumnModel().getColumn(0).setCellRenderer(new DrinkImageRenderer());
        table.setDefaultRenderer(Object.class, new StatusTableRenderer());
        return table;
    }

    private JTable createCoinTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        AdminTableKit.style(table);
        return table;
    }

    private JPanel alertPanel() {
        JPanel alert = new JPanel(new BorderLayout());
        alert.setBackground(new Color(255, 245, 245));
        alert.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 200, 200)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        JLabel alertTitle = new JLabel("서버 재고 알림");
        alertTitle.setFont(KioskFonts.bodyBold());
        alertTitle.setForeground(KioskColors.RED);
        alertLabel.setFont(KioskFonts.small());
        alertLabel.setForeground(KioskColors.TEXT);
        alert.add(alertTitle, BorderLayout.NORTH);
        alert.add(alertLabel, BorderLayout.CENTER);
        return alert;
    }

    private void showView(AdminView view) {
        currentView = view;
        pageSubtitle.setText(view.subtitle);
        contentLayout.show(contentHost, view.name());
        contentHost.revalidate();
        contentHost.repaint();
        highlightMenu(view);
        if (view == AdminView.SERVER) {
            refreshServerSummary();
        }
        if (view == AdminView.COLLECT) {
            updateCollectInfo();
        }
        if (view == AdminView.IMAGE_CHANGE) {
            updateImagePreview();
        }
        if (view == AdminView.DRINK_SETTING && settingDrinkTable != null && settingDrinkTable.getRowCount() > 0) {
            settingDrinkTable.setRowSelectionInterval(0, 0);
            loadSettingSelection(0);
        }
    }

    private void highlightMenu(AdminView active) {
        for (Map.Entry<AdminView, JButton> entry : menuButtons.entrySet()) {
            JButton btn = entry.getValue();
            if (entry.getKey() == active) {
                btn.setBackground(new Color(59, 91, 219));
                btn.setForeground(Color.WHITE);
                btn.setOpaque(true);
            } else {
                btn.setBackground(KioskColors.NAVY);
                btn.setOpaque(false);
                btn.setForeground(new Color(210, 220, 235));
            }
        }
    }

    private void loadSettingSelection(int row) {
        if (row < 0) {
            return;
        }
        DrinkInfo drink = service.getCatalog().getDrink(row);
        settingNameField.setText(drink.getName());
        settingPriceField.setText(String.valueOf(drink.getPrice()));
        settingStockField.setText(String.valueOf(service.getCatalog().getStock(row).size()));
        updatePreview(settingPreview, row);
        settingStatusLabel.setText(drink.getName() + " 선택됨");
    }

    private void saveDrinkSetting() {
        int row = settingDrinkTable.getSelectedRow();
        if (row < 0) {
            settingStatusLabel.setForeground(KioskColors.RED);
            settingStatusLabel.setText("음료를 선택하세요.");
            return;
        }
        try {
            String name = settingNameField.getText().trim();
            int price = Integer.parseInt(settingPriceField.getText().trim());
            int stock = Integer.parseInt(settingStockField.getText().trim());
            if (name.isBlank() || price <= 0 || price % 10 != 0 || stock < 0) {
                settingStatusLabel.setForeground(KioskColors.RED);
                settingStatusLabel.setText("입력값을 확인하세요.");
                return;
            }
            service.updateDrinkName(row, name);
            service.updateDrinkPrice(row, price);
            service.updateDrinkStock(row, stock);
            customerPanel.refresh();
            refresh();
            settingStatusLabel.setForeground(KioskColors.GREEN);
            settingStatusLabel.setText("저장되었습니다.");
        } catch (NumberFormatException ex) {
            settingStatusLabel.setForeground(KioskColors.RED);
            settingStatusLabel.setText("가격과 재고는 숫자로 입력하세요.");
        }
    }

    private void runRestock() {
        int row = restockDrinkBox.getSelectedIndex();
        if (row < 0) {
            restockStatusLabel.setForeground(KioskColors.RED);
            restockStatusLabel.setText("음료를 선택하세요.");
            return;
        }
        try {
            int count = Integer.parseInt(restockCountField.getText().trim());
            if (count <= 0) {
                restockStatusLabel.setForeground(KioskColors.RED);
                restockStatusLabel.setText("1개 이상 입력하세요.");
                return;
            }
            service.restockDrink(row, count);
            customerPanel.refresh();
            refresh();
            restockStatusLabel.setForeground(KioskColors.GREEN);
            restockStatusLabel.setText(count + "개 보충 완료");
        } catch (NumberFormatException ex) {
            restockStatusLabel.setForeground(KioskColors.RED);
            restockStatusLabel.setText("숫자를 입력하세요.");
        }
    }

    private void runCoinRefill() {
        try {
            int unit = (Integer) coinUnitBox.getSelectedItem();
            int count = Integer.parseInt(coinCountField.getText().trim());
            if (count <= 0) {
                coinStatusLabel.setForeground(KioskColors.RED);
                coinStatusLabel.setText("1개 이상 입력하세요.");
                return;
            }
            service.addCoinStock(unit, count);
            refresh();
            coinStatusLabel.setForeground(KioskColors.GREEN);
            coinStatusLabel.setText(unit + "원 " + count + "개 보충 완료");
        } catch (NumberFormatException ex) {
            coinStatusLabel.setForeground(KioskColors.RED);
            coinStatusLabel.setText("숫자를 입력하세요.");
        }
    }

    private void runCollect() {
        try {
            int amount = Integer.parseInt(collectAmountField.getText().trim());
            String err = service.collectMoney(amount);
            if (err != null) {
                collectStatusLabel.setForeground(KioskColors.RED);
                collectStatusLabel.setText(err);
            } else {
                refresh();
                updateCollectInfo();
                collectStatusLabel.setForeground(KioskColors.GREEN);
                collectStatusLabel.setText(amount + "원 수금 완료");
            }
        } catch (NumberFormatException ex) {
            collectStatusLabel.setForeground(KioskColors.RED);
            collectStatusLabel.setText("숫자를 입력하세요.");
        }
    }

    private void runPasswordChange() {
        String pw = new String(newPasswordField.getPassword());
        String confirm = new String(confirmPasswordField.getPassword());
        if (!pw.equals(confirm)) {
            passwordStatusLabel.setForeground(KioskColors.RED);
            passwordStatusLabel.setText("비밀번호가 일치하지 않습니다.");
            return;
        }
        String err = service.changePassword(pw);
        if (err != null) {
            passwordStatusLabel.setForeground(KioskColors.RED);
            passwordStatusLabel.setText(err);
        } else {
            newPasswordField.setText("");
            confirmPasswordField.setText("");
            passwordStatusLabel.setForeground(KioskColors.GREEN);
            passwordStatusLabel.setText("비밀번호가 변경되었습니다.");
        }
    }

    private void updateCollectInfo() {
        collectMaxValue.setText(won.format(service.maxCollectableAmount()) + "원");
        collectAmountField.setText(String.valueOf(service.maxCollectableAmount()));
    }

    private void updateSideNotes() {
        DrinkCatalog catalog = service.getCatalog();
        StringBuilder stock = new StringBuilder();
        for (int i = 0; i < catalog.drinkCount(); i++) {
            int count = catalog.getStock(i).size();
            String mark = count <= 2 ? " ⚠" : "";
            stock.append("· ").append(catalog.getDrink(i).getName())
                    .append(": ").append(count).append("개").append(mark).append("\n");
        }
        restockSideNote.setText(stock.toString());

        CoinInventory coins = service.getCoinInventory();
        StringBuilder coin = new StringBuilder();
        for (CoinSlot slot : coins.getSlots()) {
            coin.append("· ").append(slot.getValue()).append("원: ")
                    .append(slot.getCount()).append("개\n");
        }
        coin.append("합계 ").append(won.format(coins.totalBalance())).append("원");
        coinSideNote.setText(coin.toString());

        collectSideNote.setText(
                "· 거스름돈 상태: " + service.getChangeStatusText() + "\n"
                        + "· 현재 잔고: " + won.format(coins.totalBalance()) + "원\n"
                        + "· 최대 수금 가능: " + won.format(service.maxCollectableAmount()) + "원\n"
                        + "· 수금 후에도 거스름돈 지급이 가능한 금액만 수금됩니다.");
    }

    private void refreshServerSummary() {
        service.pollServerInfo();
        updateServerSummaryDisplay();
    }

    private void updateServerSummaryDisplay() {
        String summary = service.getServerSalesSummary();
        if (summary == null || summary.isBlank()) {
            summary = buildFallbackServerSummary();
        } else {
            summary = summary + "\n\n" + buildLocalServerAppendix();
        }
        serverSummaryArea.setText(summary);
        serverSummaryArea.setCaretPosition(0);
        updateServerSummaryCards(summary);
        updateServerBadge();
        serverStatusDetail.setFont(KioskFonts.small());
        serverStatusDetail.setForeground(KioskColors.SUBTEXT);
        serverStatusDetail.setText("마지막 조회: " + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                + " · 서버 " + service.getServerStatusText());
    }

    private String buildFallbackServerSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("서버 집계 데이터를 불러오지 못했습니다.\n");
        sb.append("서버(Server1/Server2)가 실행 중인지 확인한 뒤 새로고침을 눌러주세요.\n\n");
        sb.append(buildLocalServerAppendix());
        return sb.toString();
    }

    private String buildLocalServerAppendix() {
        StringBuilder sb = new StringBuilder();
        sb.append("[이 단말 로컬 정보]\n");
        sb.append("· 오늘 매출: ").append(won.format(service.getTodaySalesAmount())).append("원\n");
        sb.append("· 누적 매출: ").append(won.format(service.getTotalSalesAmount())).append("원\n");
        sb.append("· 세션 매출: ").append(won.format(service.getSessionSales())).append("원\n");
        sb.append("· 현재 잔고: ").append(won.format(service.getCoinInventory().totalBalance())).append("원\n");
        sb.append("· 거스름돈 상태: ").append(service.getChangeStatusText()).append("\n");
        sb.append("· 서버 연결: ").append(service.getServerStatusText());
        return sb.toString();
    }

    private void updateServerSummaryCards(String summary) {
        String total = extractLineValue(summary, "[전체 누적 매출]");
        if (total == null) {
            total = won.format(service.getTotalSalesAmount()) + "원";
        }
        serverSummaryTotal.setText(total);

        String today = won.format(service.getTodaySalesAmount()) + "원";
        serverSummaryToday.setText(today);

        int clientCount = countClientsInSummary(summary);
        serverSummaryClients.setText(clientCount > 0 ? clientCount + "대" : "-");
    }

    private String extractLineValue(String text, String key) {
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().equals(key) || lines[i].contains(key)) {
                if (i + 1 < lines.length) {
                    String next = lines[i + 1].trim();
                    if (!next.isBlank()) {
                        return next;
                    }
                }
            }
        }
        return null;
    }

    private int countClientsInSummary(String summary) {
        int count = 0;
        boolean inSection = false;
        for (String line : summary.split("\n")) {
            if (line.contains("[클라이언트별 누적 매출]")) {
                inSection = true;
                continue;
            }
            if (inSection) {
                if (line.startsWith("[") || line.isBlank()) {
                    break;
                }
                if (line.contains("Client") && line.contains(":")) {
                    count++;
                }
            }
        }
        return count;
    }

    private Path pendingImagePath;

    private void pickImageFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PNG 이미지", "png"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pendingImagePath = chooser.getSelectedFile().toPath();
            ImageIcon icon = new ImageIcon(pendingImagePath.toString());
            imagePreview.setIcon(new ImageIcon(ProductImageLoader.fit(icon.getImage(), 164, 184)));
            imageStatusLabel.setForeground(KioskColors.SUBTEXT);
            imageStatusLabel.setText("파일 선택됨: " + pendingImagePath.getFileName());
        }
    }

    private void applyImageChange() {
        int row = imageDrinkBox.getSelectedIndex();
        if (row < 0 || pendingImagePath == null) {
            imageStatusLabel.setForeground(KioskColors.RED);
            imageStatusLabel.setText("음료와 이미지 파일을 선택하세요.");
            return;
        }
        try {
            Files.createDirectories(DataPaths.PRODUCT_IMAGES);
            Path target = DataPaths.PRODUCT_IMAGES.resolve(row + ".png");
            Files.copy(pendingImagePath, target, StandardCopyOption.REPLACE_EXISTING);
            customerPanel.refresh();
            refresh();
            updateImagePreview();
            imageStatusLabel.setForeground(KioskColors.GREEN);
            imageStatusLabel.setText("이미지가 적용되었습니다.");
        } catch (Exception ex) {
            imageStatusLabel.setForeground(KioskColors.RED);
            imageStatusLabel.setText("이미지 저장 실패: " + ex.getMessage());
        }
    }

    private void updateImagePreview() {
        int row = imageDrinkBox.getSelectedIndex();
        if (row < 0) {
            return;
        }
        updatePreview(imagePreview, row);
        pendingImagePath = null;
        imageStatusLabel.setText(service.getCatalog().getDrink(row).getName() + " 미리보기");
    }

    private void loadRemoteDefaults() {
        int row = remoteDrinkBox.getSelectedIndex();
        if (row < 0) {
            return;
        }
        DrinkInfo drink = service.getCatalog().getDrink(row);
        remoteNameField.setText(drink.getName());
        remotePriceField.setText(String.valueOf(drink.getPrice()));
    }

    private void runRemoteChange() {
        int row = remoteDrinkBox.getSelectedIndex();
        if (row < 0) {
            remoteStatusLabel.setForeground(KioskColors.RED);
            remoteStatusLabel.setText("음료를 선택하세요.");
            return;
        }
        try {
            int price = Integer.parseInt(remotePriceField.getText().trim());
            String result = service.requestRemoteDrinkChange(
                    remoteClientField.getText().trim(),
                    row,
                    remoteNameField.getText().trim(),
                    price
            );
            remoteStatusLabel.setForeground(KioskColors.GREEN);
            remoteStatusLabel.setText(result);
        } catch (NumberFormatException ex) {
            remoteStatusLabel.setForeground(KioskColors.RED);
            remoteStatusLabel.setText("가격은 숫자여야 합니다.");
        }
    }

    private void updatePreview(JLabel target, int index) {
        Image image = ProductImageLoader.load(index);
        if (image != null) {
            int maxW = Math.max(120, target.getWidth() > 0 ? target.getWidth() - 16 : 164);
            int maxH = Math.max(120, target.getHeight() > 0 ? target.getHeight() - 16 : 184);
            target.setIcon(new ImageIcon(ProductImageLoader.fit(image, maxW, maxH)));
            target.setText("");
        } else {
            target.setIcon(null);
            target.setText("이미지 없음");
        }
    }

    private String stockStatus(int stock) {
        if (stock <= 0) {
            return "품절";
        }
        if (stock <= 2) {
            return "재고 부족";
        }
        return "정상";
    }

    private void fillDrinkTable(DefaultTableModel model) {
        DrinkCatalog catalog = service.getCatalog();
        model.setRowCount(0);
        for (int i = 0; i < catalog.drinkCount(); i++) {
            DrinkInfo d = catalog.getDrink(i);
            int stock = catalog.getStock(i).size();
            Vector<Object> row = new Vector<>();
            row.add("");
            row.add(d.getName());
            row.add(stock + "개");
            row.add(won.format(d.getPrice()) + "원");
            row.add(stockStatus(stock));
            model.addRow(row);
        }
    }

    private static class StatusTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String colName = table.getColumnName(column);
            setHorizontalAlignment("음료명".equals(colName) ? SwingConstants.LEFT : SwingConstants.CENTER);
            if (!isSelected && "상태".equals(colName) && value != null) {
                String status = value.toString();
                if ("품절".equals(status)) {
                    setForeground(KioskColors.RED);
                    setFont(KioskFonts.bodyBold());
                } else if ("재고 부족".equals(status)) {
                    setForeground(new Color(230, 120, 30));
                    setFont(KioskFonts.bodyBold());
                } else {
                    setForeground(KioskColors.GREEN);
                    setFont(KioskFonts.body());
                }
            } else if (!isSelected && "재고".equals(colName) && value != null && value.toString().startsWith("0")) {
                setForeground(KioskColors.RED);
            } else if (!isSelected) {
                setForeground(KioskColors.TEXT);
                setFont(KioskFonts.body());
            }
            return c;
        }
    }

    public void refresh() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        try {
            DrinkCatalog catalog = service.getCatalog();
            int lowCount = 0;
            for (int i = 0; i < catalog.drinkCount(); i++) {
                if (catalog.getStock(i).size() <= 2) {
                    lowCount++;
                }
            }

            if (drinkModel != null) {
                fillDrinkTable(drinkModel);
            }
            if (settingDrinkModel != null) {
                int selected = settingDrinkTable != null ? settingDrinkTable.getSelectedRow() : -1;
                fillDrinkTable(settingDrinkModel);
                if (selected >= 0 && selected < settingDrinkTable.getRowCount()) {
                    settingDrinkTable.setRowSelectionInterval(selected, selected);
                    loadSettingSelection(selected);
                }
            }

            CoinInventory coins = service.getCoinInventory();
            coinModel.setRowCount(0);
            int totalUnits = 0;
            for (CoinSlot slot : coins.getSlots()) {
                int amount = slot.getValue() * slot.getCount();
                totalUnits += slot.getCount();
                Vector<Object> coinRow = new Vector<>();
                coinRow.add(slot.getValue() + "원");
                coinRow.add(slot.getCount() + "개");
                coinRow.add(won.format(amount) + "원");
                coinModel.addRow(coinRow);
            }
            Vector<Object> sumRow = new Vector<>();
            sumRow.add("합계");
            sumRow.add(totalUnits + "개");
            sumRow.add(won.format(coins.totalBalance()) + "원");
            coinModel.addRow(sumRow);

            todayCard.setValue(won.format(service.getTodaySalesAmount()) + "원");
            totalCard.setValue(won.format(service.getTotalSalesAmount()) + "원");
            sessionCard.setValue(won.format(service.getSessionSales()) + "원");
            balanceCard.setValue(won.format(coins.totalBalance()) + "원");

            restockDrinkBox.removeAllItems();
            imageDrinkBox.removeAllItems();
            remoteDrinkBox.removeAllItems();
            for (int i = 0; i < catalog.drinkCount(); i++) {
                String name = catalog.getDrink(i).getName();
                restockDrinkBox.addItem(name);
                imageDrinkBox.addItem(name);
                remoteDrinkBox.addItem(name);
            }

            String alerts = service.getServerAlerts();
            if (alerts == null || alerts.isBlank()) {
                alertLabel.setText(lowCount > 0
                        ? "재고 부족 음료가 " + lowCount + "개 있습니다. 보충이 필요합니다."
                        : "알림 없음");
            } else {
                alertLabel.setText(alerts);
            }

            updateServerBadge();
            updateSideNotes();
            if (currentView == AdminView.COLLECT) {
                updateCollectInfo();
            }
            if (currentView == AdminView.SERVER) {
                updateServerSummaryDisplay();
            }
        } finally {
            refreshing = false;
        }
    }
}
