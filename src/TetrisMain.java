import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

/**
 * 게임의 메인 진입점 및 화면 전환 관리 클래스
 */
public class TetrisMain extends JFrame {

    /** 화면 전환용 카드 레이아웃 */
    private final CardLayout cardLayout;
    /** 메인 패널 컨테이너 */
    private final JPanel mainPanel;
    /** 게임 보드 패널 */
    private final TetrisBoard gameBoard;
    /** 메인 메뉴 패널 */
    private final MainMenuPanel menuPanel;
    /** 게임 오버 패널 */
    private final GameOverPanel gameOverPanel;
    /** 치트/설정 패널 */
    private final CheatPanel cheatPanel;
    /** 크레딧 정보 패널 */
    private final CreditPanel creditPanel;

    /** WASD 키 사용 여부 플래그 */
    public static boolean useWasd = false;

    /** 블록 생성 가중치 배열 */
    public static int[] blockWeights = {10, 10, 10, 10, 10, 10, 10};

    /**
     * 메인 윈도우 생성 및 초기화
     */
    public TetrisMain() {
        setTitle("Tetris Java Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        menuPanel = new MainMenuPanel(this);
        gameBoard = new TetrisBoard(this);
        gameOverPanel = new GameOverPanel(this);
        cheatPanel = new CheatPanel(this);
        creditPanel = new CreditPanel(this);

        mainPanel.add(menuPanel, "MENU");
        mainPanel.add(gameBoard, "GAME");
        mainPanel.add(gameOverPanel, "GAMEOVER");
        mainPanel.add(cheatPanel, "CHEAT");
        mainPanel.add(creditPanel, "CREDIT");

        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        menuPanel.requestFocusInWindow();
    }

    /**
     * 게임 화면으로 전환 및 시작
     */
    public void showGame() {
        gameBoard.start();
        cardLayout.show(mainPanel, "GAME");
        gameBoard.requestFocusInWindow();
    }

    /**
     * 메인 메뉴 화면으로 전환
     */
    public void showMenu() {
        cardLayout.show(mainPanel, "MENU");
        menuPanel.requestFocusInWindow();
    }

    /**
     * 게임 오버 화면으로 전환
     * @param score 최종 점수
     */
    public void showGameOver(int score) {
        gameOverPanel.setScore(score);
        cardLayout.show(mainPanel, "GAMEOVER");
    }

    /**
     * 설정/치트 화면으로 전환
     */
    public void showCheat() {
        cardLayout.show(mainPanel, "CHEAT");
        cheatPanel.refreshSliders();
        cheatPanel.requestFocusInWindow();
    }

    /**
     * 크레딧 화면으로 전환
     */
    public void showCredit() {
        cardLayout.show(mainPanel, "CREDIT");
        creditPanel.requestFocusInWindow();
    }

    /**
     * 프로그램 시작점
     * @param args 실행 인자
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TetrisMain());
    }
}

/**
 * 메인 메뉴 화면 패널
 */
class MainMenuPanel extends JPanel {
    /** 부모 프레임 객체 */
    private final TetrisMain parent;
    /** 조작키 토글 버튼 */
    private final JButton toggleKeyBtn;
    /** 디버그 버튼 */
    private final JButton debugBtn;

    /** 디버그 모드 해금 여부 */
    private boolean isDebugUnlocked = false;

    /** 치트 코드 키 시퀀스 */
    private final int[] CHEAT_CODE = {
            KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_UP, KeyEvent.VK_DOWN,
            KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
            KeyEvent.VK_A, KeyEvent.VK_B, KeyEvent.VK_ENTER
    };
    /** 입력 키 버퍼 */
    private final ArrayList<Integer> inputBuffer = new ArrayList<>();

    /**
     * 메뉴 패널 생성자
     * @param parent 메인 프레임
     */
    public MainMenuPanel(TetrisMain parent) {
        this.parent = parent;
        setLayout(new GridBagLayout());
        setBackground(Color.DARK_GRAY);
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                inputBuffer.add(e.getKeyCode());
                checkCheatCode();
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("T E T R I S");
        title.setFont(new Font("Arial", Font.BOLD, 40));
        title.setForeground(Color.CYAN);
        add(title, gbc);

        gbc.gridy++;
        JButton startBtn = createButton("게임 시작");
        startBtn.addActionListener(e -> parent.showGame());
        add(startBtn, gbc);

        gbc.gridy++;
        toggleKeyBtn = createButton("조작: 방향키");
        toggleKeyBtn.addActionListener(e -> {
            TetrisMain.useWasd = !TetrisMain.useWasd;
            toggleKeyBtn.setText(TetrisMain.useWasd ? "조작: WASD" : "조작: 방향키");
            MainMenuPanel.this.requestFocusInWindow();
        });
        add(toggleKeyBtn, gbc);

        gbc.gridy++;
        JButton exitBtn = createButton("종료");
        exitBtn.addActionListener(e -> System.exit(0));
        add(exitBtn, gbc);

        gbc.gridy++;
        JButton creditBtn = createButton("크레딧");
        creditBtn.setBackground(Color.DARK_GRAY);
        creditBtn.setForeground(Color.DARK_GRAY);
        creditBtn.addActionListener(e -> parent.showCredit());
        add(creditBtn, gbc);

        gbc.gridy++;
        debugBtn = createButton("디버그 설정");
        debugBtn.setForeground(Color.ORANGE);
        debugBtn.addActionListener(e -> parent.showCheat());
        debugBtn.setVisible(false);
        add(debugBtn, gbc);
    }

    /**
     * 기본 스타일 버튼 생성
     * @param text 버튼 텍스트
     * @return 생성된 JButton 객체
     */
    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(200, 50));
        return btn;
    }

    /**
     * 컴포넌트 렌더링 (디버그 텍스트 표시)
     * @param g 그래픽 컨텍스트
     */
    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (isDebugUnlocked) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            String text = "디버그 모드";
            FontMetrics fm = g.getFontMetrics();
            int x = getWidth() - fm.stringWidth(text) - 10;
            int y = getHeight() - 10;
            g.drawString(text, x, y);
        }
    }

    /**
     * 치트 코드 입력 확인
     */
    private void checkCheatCode() {
        if (inputBuffer.size() > CHEAT_CODE.length) inputBuffer.remove(0);
        if (inputBuffer.size() == CHEAT_CODE.length) {
            boolean match = true;
            for (int i = 0; i < CHEAT_CODE.length; i++) {
                if (inputBuffer.get(i) != CHEAT_CODE[i]) { match = false; break; }
            }
            if (match) {
                inputBuffer.clear();
                if (!isDebugUnlocked) {
                    isDebugUnlocked = true;
                    debugBtn.setVisible(true);
                    parent.setTitle("Tetris Java Edition (debug mode XP)");
                    revalidate();
                    repaint();
                }
            }
        }
    }
}

/**
 * 개발자 정보 표시 패널
 */
class CreditPanel extends JPanel {
    /** 부모 프레임 객체 */
    private final TetrisMain parent;

    /**
     * 크레딧 패널 생성자
     * @param parent 메인 프레임
     */
    public CreditPanel(TetrisMain parent) {
        this.parent = parent;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.BLACK);

        add(Box.createVerticalGlue());

        JLabel title = new JLabel("제작진");
        title.setFont(new Font("Arial", Font.BOLD, 30));
        title.setForeground(Color.CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(title);

        add(Box.createVerticalStrut(20));

        JLabel name = new JLabel("개발/기획: 박여웅 (@Z3r0c0k3)");
        name.setFont(new Font("Arial", Font.PLAIN, 18));
        name.setForeground(Color.WHITE);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(name);

        add(Box.createVerticalStrut(10));

        JLabel license = new JLabel("MIT 라이선스가 적용됩니다.");
        license.setFont(new Font("Arial", Font.ITALIC, 14));
        license.setForeground(Color.LIGHT_GRAY);
        license.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(license);

        add(Box.createVerticalStrut(40));

        JButton backBtn = new JButton("메뉴로 돌아가기");
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backBtn.addActionListener(e -> parent.showMenu());
        add(backBtn);

        add(Box.createVerticalGlue());
    }
}

/**
 * 블록 확률 조작 패널
 */
class CheatPanel extends JPanel {
    /** 부모 프레임 객체 */
    private final TetrisMain parent;
    /** 확률 조절 슬라이더 배열 */
    private final JSlider[] sliders = new JSlider[7];
    /** 블록 이름 배열 */
    private final String[] names = {"Z 블록", "S 블록", "I 블록 (일자)", "T 블록", "O 블록 (네모)", "L 블록", "J 블록"};

    /**
     * 치트 패널 생성자
     * @param parent 메인 프레임
     */
    public CheatPanel(TetrisMain parent) {
        this.parent = parent;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.DARK_GRAY);

        add(Box.createRigidArea(new Dimension(0, 20)));
        JLabel title = new JLabel("블록 생성 확률 설정 (가중치 0~100)");
        title.setForeground(Color.ORANGE);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(title);
        add(Box.createRigidArea(new Dimension(0, 20)));

        JPanel sliderPanel = new JPanel(new GridLayout(7, 2, 10, 10));
        sliderPanel.setBackground(Color.DARK_GRAY);
        sliderPanel.setMaximumSize(new Dimension(400, 300));

        for (int i = 0; i < 7; i++) {
            JLabel label = new JLabel(names[i], SwingConstants.RIGHT);
            label.setForeground(Color.WHITE);
            sliderPanel.add(label);

            sliders[i] = new JSlider(0, 100, 10);
            sliders[i].setMajorTickSpacing(20);
            sliders[i].setMinorTickSpacing(5);
            sliders[i].setPaintTicks(true);
            sliders[i].setPaintLabels(true);
            sliders[i].setBackground(Color.DARK_GRAY);
            sliders[i].setForeground(Color.WHITE);
            sliderPanel.add(sliders[i]);
        }
        add(sliderPanel);

        add(Box.createRigidArea(new Dimension(0, 20)));

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Color.DARK_GRAY);
        btnPanel.setLayout(new FlowLayout());

        JButton resetBtn = new JButton("기본값 초기화");
        resetBtn.addActionListener(e -> {
            for(int i=0; i<7; i++) TetrisMain.blockWeights[i] = 10;
            refreshSliders();
            JOptionPane.showMessageDialog(this, "초기화 완료!");
        });
        btnPanel.add(resetBtn);

        JButton applyBtn = new JButton("적용 및 돌아가기");
        applyBtn.addActionListener(e -> {
            for(int i=0; i<7; i++) {
                TetrisMain.blockWeights[i] = sliders[i].getValue();
            }
            parent.showMenu();
        });
        btnPanel.add(applyBtn);

        btnPanel.setMaximumSize(new Dimension(400, 50));
        add(btnPanel);
    }

    /**
     * 슬라이더 값을 현재 설정으로 갱신
     */
    public void refreshSliders() {
        for(int i=0; i<7; i++) {
            sliders[i].setValue(TetrisMain.blockWeights[i]);
        }
    }
}

/**
 * 게임 오버 화면 패널
 */
class GameOverPanel extends JPanel {
    /** 부모 프레임 객체 */
    private final TetrisMain parent;
    /** 점수 표시 라벨 */
    private final JLabel scoreLabel;

    /**
     * 게임 오버 패널 생성자
     * @param parent 메인 프레임
     */
    public GameOverPanel(TetrisMain parent) {
        this.parent = parent;
        setLayout(new GridBagLayout());
        setBackground(Color.BLACK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 0, 15, 0);
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("게임 종료");
        title.setFont(new Font("Arial", Font.BOLD, 40));
        title.setForeground(Color.RED);
        add(title, gbc);

        gbc.gridy++;
        scoreLabel = new JLabel("점수: 0");
        scoreLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        scoreLabel.setForeground(Color.WHITE);
        add(scoreLabel, gbc);

        gbc.gridy++;
        JButton menuBtn = new JButton("메인 메뉴로 이동");
        menuBtn.setPreferredSize(new Dimension(200, 50));
        menuBtn.addActionListener(e -> parent.showMenu());
        add(menuBtn, gbc);
    }

    /**
     * 최종 점수 설정 및 표시
     * @param score 최종 점수
     */
    public void setScore(int score) {
        scoreLabel.setText("최종 점수: " + score);
    }
}