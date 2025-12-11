import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;

/**
 * 테트리스 게임 보드 로직 및 렌더링을 담당하는 클래스
 */
public class TetrisBoard extends JPanel implements ActionListener {

    /** 부모 프레임 객체 */
    private TetrisMain parent;
    /** 보드 가로 칸 수 */
    private final int BOARD_WIDTH = 10;
    /** 보드 세로 칸 수 */
    private final int BOARD_HEIGHT = 20;
    /** 블록 하나의 크기 (픽셀) */
    private final int BLOCK_SIZE = 30;

    /** 게임 루프 타이머 */
    private Timer timer;
    /** 일시정지 상태 플래그 */
    private boolean isPaused = false;
    /** 게임 종료 상태 플래그 */
    private boolean isGameOver = false;
    /** 홀드 기능 사용 가능 여부 */
    private boolean canHold = true;

    /** 게임 보드 데이터 배열 */
    private Tetrominoes[][] board;
    /** 현재 조작 중인 블록 */
    private Shape curPiece;
    /** 다음에 나올 블록 */
    private Shape nextPiece;
    /** 홀드된 블록 */
    private Shape holdPiece;

    /** 현재 블록의 X 좌표 */
    private int curX = 0;
    /** 현재 블록의 Y 좌표 */
    private int curY = 0;
    /** 현재 점수 */
    private int score = 0;
    /** 현재 콤보 수 */
    private int combo = 0;
    /** 이전 턴에 블록 삭제가 있었는지 여부 (콤보 계산용) */
    private boolean lastMoveCleared = false;

    /** 화면 흔들림 X축 오프셋 */
    private int shakeX = 0;
    /** 화면 흔들림 Y축 오프셋 */
    private int shakeY = 0;
    /** 이펙트 타이머 */
    private Timer effectTimer;
    /** 이펙트 지속 시간 */
    private int effectDuration = 0;

    /** 테트로미노 형태 열거형 */
    enum Tetrominoes { NoShape, ZShape, SShape, LineShape, TShape, SquareShape, LShape, MirroredLShape }

    /** 블록 모양별 좌표 테이블 */
    private int[][][] coordsTable = {
            {{0,0}, {0,0}, {0,0}, {0,0}}, {{0,-1}, {0,0}, {-1,0}, {-1,1}}, {{0,-1}, {0,0}, {1,0}, {1,1}},
            {{0,-1}, {0,0}, {0,1}, {0,2}}, {{-1,0}, {0,0}, {1,0}, {0,1}}, {{0,0}, {1,0}, {0,1}, {1,1}},
            {{-1,-1}, {0,-1}, {0,0}, {0,1}}, {{1,-1}, {0,-1}, {0,0}, {0,1}}
    };

    /** 블록 색상 배열 */
    private Color[] colors = {
            new Color(0,0,0), new Color(204,102,102), new Color(102,204,102), new Color(102,102,204),
            new Color(204,204,102), new Color(204,102,204), new Color(102,204,204), new Color(218,170,0)
    };

    /**
     * 보드 초기화 및 설정
     * @param parent 메인 프레임 인스턴스
     */
    public TetrisBoard(TetrisMain parent) {
        this.parent = parent;
        setFocusable(true);
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(BLOCK_SIZE * (BOARD_WIDTH + 6), BLOCK_SIZE * BOARD_HEIGHT));
        setFocusTraversalKeysEnabled(false);

        board = new Tetrominoes[BOARD_HEIGHT][BOARD_WIDTH];
        addKeyListener(new TAdapter());

        timer = new Timer(400, this);
        effectTimer = new Timer(30, e -> updateEffect());
    }

    /**
     * 게임 시작 및 변수 초기화
     */
    public void start() {
        isPaused = false;
        isGameOver = false;
        score = 0;
        combo = 0;
        lastMoveCleared = false;
        holdPiece = null;
        clearBoard();
        nextPiece = new Shape(); nextPiece.setRandomShape();
        newPiece();
        timer.start();
    }

    /**
     * 보드 배열 비우기
     */
    private void clearBoard() {
        for (int i = 0; i < BOARD_HEIGHT; i++)
            for (int j = 0; j < BOARD_WIDTH; j++) board[i][j] = Tetrominoes.NoShape;
    }

    /**
     * 게임 일시정지 및 메뉴 다이얼로그 표시
     */
    private void pauseGame() {
        if (isGameOver) return;

        isPaused = true;
        timer.stop();
        repaint();

        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "일시정지", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 2),
                BorderFactory.createEmptyBorder(20, 40, 20, 40)
        ));

        JLabel titleLabel = new JLabel("일시정지");
        titleLabel.setFont(new Font("Monospaced", Font.BOLD, 30));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel scoreLabel = new JLabel("점수: " + score);
        scoreLabel.setFont(new Font("Monospaced", Font.PLAIN, 16));
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnResume = createStyledButton("계속하기");
        btnResume.addActionListener(e -> {
            dialog.dispose();
            isPaused = false;
            timer.start();
        });

        String controlText = TetrisMain.useWasd ? "방향키로 변경" : "WASD로 변경";
        JButton btnSwitch = createStyledButton(controlText);
        btnSwitch.addActionListener(e -> {
            TetrisMain.useWasd = !TetrisMain.useWasd;
            dialog.dispose();
            isPaused = false;
            timer.start();
        });

        JButton btnRestart = createStyledButton("재시작");
        btnRestart.addActionListener(e -> {
            dialog.dispose();
            start();
        });

        JButton btnExit = createStyledButton("메인 메뉴로");
        btnExit.addActionListener(e -> {
            dialog.dispose();
            parent.showMenu();
        });

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(scoreLabel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(btnResume);
        panel.add(Box.createVerticalStrut(10));
        panel.add(btnSwitch);
        panel.add(Box.createVerticalStrut(10));
        panel.add(btnRestart);
        panel.add(Box.createVerticalStrut(10));
        panel.add(btnExit);

        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(parentWindow);
        dialog.setVisible(true);
    }

    /**
     * 스타일이 적용된 버튼 생성
     * @param text 버튼 텍스트
     * @return 생성된 JButton 객체
     */
    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Monospaced", Font.PLAIN, 14));
        return btn;
    }

    /**
     * 타이머 이벤트 처리 (게임 루프)
     * @param e 이벤트 객체
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (isGameOver || isPaused) return;
        if (!tryMove(curPiece, curX, curY + 1)) {
            pieceDropped();
        }
    }

    /**
     * 컴포넌트 그리기
     * @param g 그래픽 컨텍스트
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(shakeX, shakeY);

        for (int i = 0; i < BOARD_HEIGHT; i++) {
            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (board[i][j] != Tetrominoes.NoShape)
                    drawSquare(g2d, j * BLOCK_SIZE, i * BLOCK_SIZE, board[i][j]);
            }
        }

        if (curPiece != null && !isGameOver && !isPaused) {
            int ghostY = curY;
            while (ghostY < BOARD_HEIGHT && tryMoveSimulate(curPiece, curX, ghostY + 1)) ghostY++;
            drawGhost(g2d, curX * BLOCK_SIZE, ghostY * BLOCK_SIZE, curPiece);
            for (int i = 0; i < 4; i++)
                drawSquare(g2d, (curX + curPiece.x(i)) * BLOCK_SIZE, (curY + curPiece.y(i)) * BLOCK_SIZE, curPiece.shape);
        }

        drawUI(g2d);
        g2d.translate(-shakeX, -shakeY);
    }

    /**
     * 게임 UI(점수, 다음 블록 등) 그리기
     * @param g 그래픽 컨텍스트
     */
    private void drawUI(Graphics2D g) {
        int uiX = BOARD_WIDTH * BLOCK_SIZE + 25;
        int nextBlockY = 60;
        int holdBlockY = 240;
        int scoreY = 420;

        g.setColor(Color.LIGHT_GRAY);
        g.drawLine(BOARD_WIDTH * BLOCK_SIZE, 0, BOARD_WIDTH * BLOCK_SIZE, getHeight());

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("다음", uiX, nextBlockY - 25);
        if (nextPiece != null) drawMiniPiece(g, nextPiece, uiX, nextBlockY);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("블록 킵핑", uiX, holdBlockY - 45);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("C키: 스왑", uiX, holdBlockY - 25);
        if (holdPiece != null) drawMiniPiece(g, holdPiece, uiX, holdBlockY);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("점수", uiX, scoreY);
        g.setColor(Color.CYAN);
        g.drawString(String.valueOf(score), uiX, scoreY + 30);

        if (combo > 0) {
            g.setColor(Color.MAGENTA);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString(combo + " 콤보!", uiX, scoreY + 55);
        }

        if (isPaused) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("일시정지됨", uiX, scoreY + 80);
        }

        g.setColor(Color.DARK_GRAY);
        g.drawString("Tetris", uiX, getHeight() - 30);
        g.drawString("Java Edition", uiX, getHeight() - 10);
    }

    /**
     * 화면 흔들림 효과 시작
     */
    private void startShake() { effectDuration = 6; effectTimer.start(); }

    /**
     * 흔들림 효과 업데이트
     */
    private void updateEffect() {
        if (effectDuration-- > 0) {
            Random r = new Random();
            shakeX = r.nextInt(7) - 3; shakeY = r.nextInt(7) - 3;
            repaint();
        } else {
            shakeX = 0; shakeY = 0; effectTimer.stop(); repaint();
        }
    }

    /**
     * 블록 회전 및 벽 킥 처리 (SRS 유사 로직 및 T-Spin 보정 포함)
     */
    private void tryKickRotate() {
        if (curPiece.shape == Tetrominoes.NoShape) return;
        Shape rotated = curPiece.rotateLeft();

        if (tryMove(rotated, curX, curY)) return;
        if (tryMove(rotated, curX + 1, curY)) return;
        if (tryMove(rotated, curX - 1, curY)) return;
        if (tryMove(rotated, curX, curY - 1)) return;
        if (tryMove(rotated, curX + 2, curY)) return;
        if (tryMove(rotated, curX - 2, curY)) return;

        if (tryMove(rotated, curX, curY + 1)) return;
        if (tryMove(rotated, curX - 1, curY + 1)) return;
        if (tryMove(rotated, curX + 1, curY + 1)) return;
        if (tryMove(rotated, curX - 1, curY - 1)) return;
        if (tryMove(rotated, curX + 1, curY - 1)) return;
    }

    /**
     * 현재 블록 홀드 처리
     */
    private void holdBlock() {
        if (!canHold) return;
        if (holdPiece == null) {
            holdPiece = curPiece;
            newPiece();
        } else {
            Shape temp = curPiece;
            curPiece = holdPiece;
            holdPiece = temp;
            curX = BOARD_WIDTH / 2;
            curY = 0;
        }
        canHold = false;
        repaint();
    }

    /**
     * 블록을 바닥까지 즉시 내림 (Hard Drop)
     */
    private void dropDown() {
        int newY = curY;
        while (newY < BOARD_HEIGHT && tryMoveSimulate(curPiece, curX, newY + 1)) newY++;
        if(tryMove(curPiece, curX, newY)) { startShake(); pieceDropped(); }
    }

    /**
     * 새로운 블록 생성 및 게임 오버 체크
     */
    private void newPiece() {
        curPiece = nextPiece;
        nextPiece = new Shape(); nextPiece.setRandomShape();
        curX = BOARD_WIDTH / 2; curY = 0;
        canHold = true;

        if (!tryMove(curPiece, curX, curY)) {
            curPiece.shape = Tetrominoes.NoShape;
            timer.stop(); isGameOver = true;
            parent.showGameOver(score);
        }
    }

    /**
     * 블록 고정 후 줄 삭제 및 콤보/점수 계산
     */
    private void pieceDropped() {
        for (int i = 0; i < 4; i++) {
            int x = curX + curPiece.x(i);
            int y = curY + curPiece.y(i);
            if(y >= 0) board[y][x] = curPiece.shape;
        }

        int linesCleared = removeFullLines();

        if (linesCleared > 0) {
            // [콤보 로직 수정]
            if (lastMoveCleared) {
                // 턴 연속 삭제 시: 이전 값 + 현재 삭제된 행 + 1
                combo = combo + linesCleared + 1;
            } else {
                // 연속이 아닐 때 (시작)
                if (linesCleared >= 2) {
                    // 삭제된 행이 2개 이상일 때: 삭제된 행의 갯수
                    combo = linesCleared;
                } else {
                    // 1줄만 삭제하면 콤보는 0 (규칙상 '2개 이상' 명시)
                    combo = 0;
                }
            }
            lastMoveCleared = true; // 이번 턴에 삭제했음을 기록

            // 점수 계산 (콤보 보너스 적용)
            int bonus = (combo > 0) ? combo : 1;
            score += linesCleared * 100 * bonus;

        } else {
            // 줄을 삭제하지 못하면 콤보 초기화
            combo = 0;
            lastMoveCleared = false;
        }
        repaint();

        if (!isGameOver) newPiece();
    }

    /**
     * 꽉 찬 줄 삭제 및 정렬
     * @return 삭제된 줄의 개수
     */
    private int removeFullLines() {
        int numFullLines = 0;
        for (int i = BOARD_HEIGHT - 1; i >= 0; i--) {
            boolean lineIsFull = true;
            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (board[i][j] == Tetrominoes.NoShape) { lineIsFull = false; break; }
            }
            if (lineIsFull) {
                numFullLines++;
                for (int k = i; k > 0; k--)
                    for (int j = 0; j < BOARD_WIDTH; j++) board[k][j] = board[k-1][j];
                i++;
            }
        }
        return numFullLines;
    }

    /**
     * 블록 이동 가능 여부 확인 및 이동
     * @param newPiece 이동할 블록 형태
     * @param newX 이동할 X 좌표
     * @param newY 이동할 Y 좌표
     * @return 이동 성공 여부
     */
    private boolean tryMove(Shape newPiece, int newX, int newY) {
        for (int i = 0; i < 4; i++) {
            int x = newX + newPiece.x(i);
            int y = newY + newPiece.y(i);
            if (x < 0 || x >= BOARD_WIDTH || y >= BOARD_HEIGHT) return false;
            if (y >= 0 && board[y][x] != Tetrominoes.NoShape) return false;
        }
        curPiece = newPiece; curX = newX; curY = newY; repaint(); return true;
    }

    /**
     * 이동 시뮬레이션 (실제 이동 없음)
     * @param newPiece 시뮬레이션할 블록 형태
     * @param newX 시뮬레이션할 X 좌표
     * @param newY 시뮬레이션할 Y 좌표
     * @return 이동 가능 여부
     */
    private boolean tryMoveSimulate(Shape newPiece, int newX, int newY) {
        for (int i = 0; i < 4; i++) {
            int x = newX + newPiece.x(i);
            int y = newY + newPiece.y(i);
            if (x < 0 || x >= BOARD_WIDTH || y >= BOARD_HEIGHT) return false;
            if (y >= 0 && board[y][x] != Tetrominoes.NoShape) return false;
        }
        return true;
    }

    /**
     * 단일 블록 사각형 그리기
     * @param g 그래픽 컨텍스트
     * @param x X 좌표
     * @param y Y 좌표
     * @param shape 블록 모양
     */
    private void drawSquare(Graphics2D g, int x, int y, Tetrominoes shape) {
        Color color = colors[shape.ordinal()];
        g.setColor(color); g.fillRect(x + 1, y + 1, BLOCK_SIZE - 2, BLOCK_SIZE - 2);
        g.setColor(color.brighter()); g.drawLine(x, y + BLOCK_SIZE - 1, x, y); g.drawLine(x, y, x + BLOCK_SIZE - 1, y);
        g.setColor(color.darker()); g.drawLine(x + 1, y + BLOCK_SIZE - 1, x + BLOCK_SIZE - 1, y + BLOCK_SIZE - 1); g.drawLine(x + BLOCK_SIZE - 1, y + BLOCK_SIZE - 1, x + BLOCK_SIZE - 1, y + 1);
    }

    /**
     * 미리보기용 미니 블록 그리기
     * @param g 그래픽 컨텍스트
     * @param piece 그릴 블록
     * @param x 기준 X 좌표
     * @param y 기준 Y 좌표
     */
    private void drawMiniPiece(Graphics2D g, Shape piece, int x, int y) {
        for (int i = 0; i < 4; i++) drawSquare(g, x + (piece.x(i) + 1) * BLOCK_SIZE, y + (piece.y(i) + 1) * BLOCK_SIZE, piece.shape);
    }

    /**
     * 고스트 블록 그리기
     * @param g 그래픽 컨텍스트
     * @param x X 좌표
     * @param y Y 좌표
     * @param piece 그릴 블록
     */
    private void drawGhost(Graphics2D g, int x, int y, Shape piece) {
        g.setColor(Color.GRAY);
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{2.0f}, 0.0f));
        for (int i = 0; i < 4; i++) g.drawRect(x + piece.x(i) * BLOCK_SIZE + 1, y + piece.y(i) * BLOCK_SIZE + 1, BLOCK_SIZE - 2, BLOCK_SIZE - 2);
        g.setStroke(old);
    }

    /**
     * 키보드 입력을 처리하는 어댑터 클래스
     */
    class TAdapter extends KeyAdapter {
        /**
         * 키 누름 이벤트 처리
         * @param e 이벤트 객체
         */
        @Override
        public void keyPressed(KeyEvent e) {
            int keycode = e.getKeyCode();
            if (isGameOver) return;
            if (keycode == KeyEvent.VK_ESCAPE) { pauseGame(); return; }
            if (isPaused || curPiece.shape == Tetrominoes.NoShape) return;

            if (keycode == KeyEvent.VK_SPACE) { dropDown(); return; }
            if (keycode == KeyEvent.VK_Z) { tryKickRotate(); return; }
            if (keycode == KeyEvent.VK_C) { holdBlock(); return; }

            if (TetrisMain.useWasd) {
                switch (keycode) {
                    case KeyEvent.VK_A: tryMove(curPiece, curX - 1, curY); break;
                    case KeyEvent.VK_D: tryMove(curPiece, curX + 1, curY); break;
                    case KeyEvent.VK_S: tryMove(curPiece, curX, curY + 1); break;
                    case KeyEvent.VK_W: tryKickRotate(); break;
                }
            } else {
                switch (keycode) {
                    case KeyEvent.VK_LEFT: tryMove(curPiece, curX - 1, curY); break;
                    case KeyEvent.VK_RIGHT: tryMove(curPiece, curX + 1, curY); break;
                    case KeyEvent.VK_DOWN: tryMove(curPiece, curX, curY + 1); break;
                    case KeyEvent.VK_UP: tryKickRotate(); break;
                }
            }
        }
    }

    /**
     * 테트로미노 좌표 및 회전 관리 클래스
     */
    class Shape {
        /** 블록 모양 */
        Tetrominoes shape;
        /** 블록 좌표 배열 */
        int[][] coords;

        /**
         * Shape 생성자
         */
        public Shape() { coords = new int[4][2]; shape = Tetrominoes.NoShape; }

        /**
         * 랜덤 모양 설정 (가중치 적용)
         */
        public void setRandomShape() {
            int totalWeight = 0;
            for(int w : TetrisMain.blockWeights) totalWeight += w;

            if (totalWeight == 0) {
                setShape(Tetrominoes.values()[Math.abs(new Random().nextInt()) % 7 + 1]);
                return;
            }

            int r = new Random().nextInt(totalWeight);
            int currentSum = 0;
            for(int i=0; i<7; i++) {
                currentSum += TetrisMain.blockWeights[i];
                if (r < currentSum) {
                    setShape(Tetrominoes.values()[i+1]);
                    return;
                }
            }
        }

        /**
         * 특정 모양으로 설정
         * @param shape 설정할 블록 모양
         */
        public void setShape(Tetrominoes shape) {
            for(int i=0; i<4; i++) System.arraycopy(coordsTable[shape.ordinal()][i], 0, coords[i], 0, 2);
            this.shape = shape;
        }

        /**
         * 내부 좌표 X 설정
         * @param index 좌표 인덱스
         * @param x X 값
         */
        private void setX(int index, int x) { coords[index][0] = x; }

        /**
         * 내부 좌표 Y 설정
         * @param index 좌표 인덱스
         * @param y Y 값
         */
        private void setY(int index, int y) { coords[index][1] = y; }

        /**
         * X 좌표 반환
         * @param index 좌표 인덱스
         * @return X 값
         */
        public int x(int index) { return coords[index][0]; }

        /**
         * Y 좌표 반환
         * @param index 좌표 인덱스
         * @return Y 값
         */
        public int y(int index) { return coords[index][1]; }

        /**
         * 왼쪽으로 회전된 새로운 Shape 반환
         * @return 회전된 Shape 객체
         */
        public Shape rotateLeft() {
            if (shape == Tetrominoes.SquareShape) return this;
            Shape result = new Shape(); result.shape = shape;
            for (int i = 0; i < 4; i++) { result.setX(i, y(i)); result.setY(i, -x(i)); }
            return result;
        }
    }
}