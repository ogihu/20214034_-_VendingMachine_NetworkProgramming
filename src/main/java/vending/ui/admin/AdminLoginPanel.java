package vending.ui.admin;



import vending.kiosk.KioskService;

import vending.ui.theme.KioskColors;

import vending.ui.theme.KioskFonts;

import vending.ui.theme.UiKit;

import vending.ui.widget.ClockLabel;

import vending.ui.widget.VendingDialog;



import javax.swing.BorderFactory;

import javax.swing.Box;

import javax.swing.BoxLayout;

import javax.swing.JButton;

import javax.swing.JLabel;

import javax.swing.JPanel;

import javax.swing.JPasswordField;

import javax.swing.SwingConstants;

import java.awt.BorderLayout;

import java.awt.Dimension;

import java.awt.FlowLayout;

import java.awt.GridBagConstraints;

import java.awt.GridBagLayout;

import java.awt.Insets;



public class AdminLoginPanel extends JPanel {



    private final KioskService service;

    private final Runnable onBack;

    private final JPasswordField passwordField = new JPasswordField(20);

    private final JLabel errorLabel = new JLabel(" ", SwingConstants.CENTER);

    private JButton loginBtn;



    public AdminLoginPanel(KioskService service, Runnable onBack) {

        this.service = service;

        this.onBack = onBack;



        setBackground(KioskColors.BG);

        setLayout(new BorderLayout());

        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        setPreferredSize(new Dimension(1040, 780));



        add(buildHeader(), BorderLayout.NORTH);

        add(buildCenter(), BorderLayout.CENTER);

    }



    private JPanel buildHeader() {

        JPanel header = new JPanel(new BorderLayout());

        header.setOpaque(false);



        JLabel title = new JLabel("SMART VENDING");

        title.setFont(KioskFonts.title());

        title.setForeground(KioskColors.NAVY);



        JLabel mode = new JLabel("관리자 인증");

        mode.setFont(KioskFonts.small());

        mode.setForeground(KioskColors.SUBTEXT);



        JPanel titleBox = new JPanel();

        titleBox.setOpaque(false);

        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        titleBox.add(title);

        titleBox.add(Box.createVerticalStrut(2));

        titleBox.add(mode);



        header.add(titleBox, BorderLayout.WEST);

        header.add(new ClockLabel(), BorderLayout.EAST);

        return header;

    }



    private JPanel buildCenter() {

        JPanel card = new JPanel();

        card.setBackground(KioskColors.CARD);

        card.setBorder(BorderFactory.createCompoundBorder(

                UiKit.cardBorder(),

                BorderFactory.createEmptyBorder(40, 52, 40, 52)));

        card.setPreferredSize(new Dimension(520, 420));

        card.setMaximumSize(new Dimension(520, 420));

        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));



        AdminPersonIcon icon = new AdminPersonIcon();
        icon.setAlignmentX(CENTER_ALIGNMENT);



        JLabel title = new JLabel("관리자 비밀번호 입력", SwingConstants.CENTER);

        title.setFont(KioskFonts.section());

        title.setForeground(KioskColors.TEXT);

        title.setAlignmentX(CENTER_ALIGNMENT);



        JLabel guide = new JLabel("관리자만 접근할 수 있는 설정 화면입니다.", SwingConstants.CENTER);

        guide.setFont(KioskFonts.small());

        guide.setForeground(KioskColors.SUBTEXT);

        guide.setAlignmentX(CENTER_ALIGNMENT);



        passwordField.setFont(KioskFonts.body());

        passwordField.setMaximumSize(new Dimension(400, 44));

        passwordField.setPreferredSize(new Dimension(400, 44));

        passwordField.setAlignmentX(CENTER_ALIGNMENT);

        passwordField.setBorder(BorderFactory.createCompoundBorder(

                BorderFactory.createLineBorder(KioskColors.BORDER),

                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        passwordField.addActionListener(e -> onLogin());



        errorLabel.setFont(KioskFonts.small());

        errorLabel.setForeground(KioskColors.RED);

        errorLabel.setAlignmentX(CENTER_ALIGNMENT);



        loginBtn = UiKit.primaryButton("접속");

        loginBtn.setPreferredSize(new Dimension(400, 48));

        loginBtn.setMaximumSize(new Dimension(400, 48));

        loginBtn.setAlignmentX(CENTER_ALIGNMENT);

        loginBtn.addActionListener(e -> onLogin());



        JButton backBtn = UiKit.outlineButton("← 고객 화면으로");

        backBtn.setPreferredSize(new Dimension(400, 44));

        backBtn.setMaximumSize(new Dimension(400, 44));

        backBtn.setAlignmentX(CENTER_ALIGNMENT);

        backBtn.addActionListener(e -> goBack());



        card.add(icon);

        card.add(Box.createVerticalStrut(12));

        card.add(title);

        card.add(Box.createVerticalStrut(8));

        card.add(guide);

        card.add(Box.createVerticalStrut(24));

        card.add(passwordField);

        card.add(Box.createVerticalStrut(8));

        card.add(errorLabel);

        card.add(Box.createVerticalStrut(20));

        card.add(loginBtn);

        card.add(Box.createVerticalStrut(10));

        card.add(backBtn);



        JPanel wrap = new JPanel(new GridBagLayout());

        wrap.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;

        gbc.gridy = 0;

        gbc.insets = new Insets(40, 0, 0, 0);

        wrap.add(card, gbc);

        return wrap;

    }



    private void onLogin() {

        String pw = new String(passwordField.getPassword());

        if (service.loginAdmin(pw)) {

            passwordField.setText("");

            errorLabel.setText(" ");

        } else {

            errorLabel.setText("비밀번호가 틀렸습니다.");

            VendingDialog.showResult(this, "인증 실패", "비밀번호가 틀렸습니다.", false);

        }

    }



    @Override

    public void addNotify() {

        super.addNotify();

        javax.swing.JRootPane root = javax.swing.SwingUtilities.getRootPane(this);

        if (root != null && loginBtn != null) {

            root.setDefaultButton(loginBtn);

        }

    }



    public void reset() {

        passwordField.setText("");

        errorLabel.setText(" ");

        requestFocusInWindow();

        passwordField.requestFocusInWindow();

    }



    private void goBack() {

        passwordField.setText("");

        errorLabel.setText(" ");

        onBack.run();

    }

}


