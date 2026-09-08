package com.group_finity.mascot.chat;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.SettingsManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 像素风格 AI 对话对话框。
 */
public class ChatDialog extends JDialog {
    private static final Logger log = Logger.getLogger(ChatDialog.class.getName());

    private static final Color BG_COLOR = new Color(22, 22, 40);
    private static final Color BORDER_COLOR = new Color(180, 180, 200);
    private static final Color TEXT_COLOR = new Color(220, 230, 255);
    private static final Color INPUT_BG = new Color(30, 30, 55);
    private static final Color TYPING_COLOR = new Color(120, 200, 120);

    private Mascot mascot;
    private final AIClient aiClient;
    private final ConversationHistory history;
    private int dialogWidth;
    private int dialogHeight;
    private final int typingSpeedMs;

    /** 会话标识：本次对话在数据库中的唯一 ID；会话创建后保持不变（内容去重时可能复用已有会话） */
    private String sessionId;

    private JTextPane messageArea;
    private JTextArea inputField;
    private JScrollPane scrollPane;
    private JLabel typingIndicator;
    private Font pixelFont;
    private Font inputFont;
    private boolean isWaitingForAI = false;

    // 拖拽对话框用
    private Point dragStart;

    public ChatDialog(Mascot mascot, AIClient aiClient, ConversationHistory history,
                      int width, int height, int typingSpeedMs) {
        super((Frame) null);
        this.mascot = mascot;
        this.aiClient = aiClient;
        this.history = history;
        this.dialogWidth = width;
        this.dialogHeight = height;
        this.typingSpeedMs = typingSpeedMs;

        initFonts();
        initUI();
        positionDialog();
        displayHistory();

        // 对话框（重新）打开即为一次会话：固定会话 ID，
        // 之后每轮对话增量写入 SQLite，避免依赖“关闭窗口”这一保存时机。
        sessionId = new java.text.SimpleDateFormat("yyyy-MM-dd_HHmmss")
                .format(new java.util.Date());

        startPositionTracking();
        startIdleTimer();
    }

    // ==== 自主发言计时器 ====
    private javax.swing.Timer idleTimer;
    private static final int IDLE_SECONDS = 180; // 3分钟

    private void startIdleTimer() {
        if (idleTimer != null) idleTimer.stop();
        idleTimer = new javax.swing.Timer(IDLE_SECONDS * 1000, e -> triggerIdleSpeech());
        idleTimer.setRepeats(false);
        idleTimer.start();
    }

    private void resetIdleTimer() {
        if (idleTimer != null) {
            idleTimer.restart();
        }
    }

    private void triggerIdleSpeech() {
        if (!isVisible() || isWaitingForAI) return;
        String[] idlePrompts = {
            "(旅行者沉默了很久，你注意到对方一言不发。你作为多托雷，主动打破沉默。不要说你为什么沉默这种话，而是自然地评论、自言自语你的实验、或者嘲讽对方的沉默。)",
            "(实验对象似乎心不在焉。你感到有趣。催促他继续对话，但要用你一贯的傲慢科学家的口吻。)",
            "(旅行者许久未说话。哼一声，然后开始自言自语你的某个实验——就像你经常做的那样。)",
            "(你等得有些不耐烦了，但不会直接表现。用你的方式提醒旅行者：时间宝贵，实验不等人。)"
        };
        String prompt = idlePrompts[(int)(Math.random() * idlePrompts.length)];
        isWaitingForAI = true;
        showTypingIndicator();
        aiClient.sendMessageAsync(prompt, history, new AIClient.ChatCallback() {
            @Override
            public void onSuccess(AIClient.AIResponse resp) {
                SwingUtilities.invokeLater(() -> {
                    hideTypingIndicator();
                    String txt = resp.text != null ? resp.text : "";
                    history.addAssistant(txt);
                    typewriterEffect("Dottore", txt);
                    executeToolCalls(resp);
                    checkAutoClose(txt);
                    isWaitingForAI = false;
                    saveConversationMemory();
                    ChatManager.getInstance().summarizeLastTurns();
                    startIdleTimer();
                });
            }
            @Override
            public void onError(Throwable err) {
                SwingUtilities.invokeLater(() -> {
                    hideTypingIndicator();
                    isWaitingForAI = false;
                    startIdleTimer();
                });
            }
        });
    }

    private void initFonts() {
        SettingsManager settings = SettingsManager.getInstance();
        pixelFont = new Font(settings.getFontFamily(), Font.PLAIN, settings.getFontSize());
        inputFont = new Font(settings.getFontFamily(), Font.PLAIN, settings.getFontSize() + 2);
    }

    private void initUI() {
        setUndecorated(true);
        setAlwaysOnTop(false); // 小人图层在对话框上面
        setSize(dialogWidth, dialogHeight);
        setFocusableWindowState(true);
        setFocusable(true);

        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2.setColor(BG_COLOR);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER_COLOR);
                g2.fillRect(0, 0, getWidth(), 2);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
                g2.fillRect(0, 0, 2, getHeight());
                g2.fillRect(getWidth() - 2, 0, 2, getHeight());
            }
        };
        mainPanel.setBorder(new EmptyBorder(6, 8, 6, 8));

        // === 标题栏（可拖拽移动 + 大小按钮 + 关闭） ===
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);

        JLabel titleLabel = new JLabel("◆ DOTTORE");
        titleLabel.setFont(pixelFont.deriveFont(Font.BOLD, 11f));
        titleLabel.setForeground(TYPING_COLOR);
        titleBar.add(titleLabel, BorderLayout.WEST);

        // 大小调整按钮
        JPanel sizeBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        sizeBtns.setOpaque(false);

        JButton smallerBtn = new JButton("-");
        styleTitleBtn(smallerBtn);
        smallerBtn.addActionListener(e -> resizeDialog(-60, -40));

        JButton biggerBtn = new JButton("+");
        styleTitleBtn(biggerBtn);
        biggerBtn.addActionListener(e -> resizeDialog(60, 40));

        sizeBtns.add(smallerBtn);
        sizeBtns.add(biggerBtn);
        titleBar.add(sizeBtns, BorderLayout.CENTER);

        JButton closeBtn = new JButton("X");
        styleTitleBtn(closeBtn);
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { closeBtn.setForeground(Color.RED); }
            @Override public void mouseExited(MouseEvent e) { closeBtn.setForeground(TEXT_COLOR); }
        });
        closeBtn.addActionListener(e -> closeDialog());
        titleBar.add(closeBtn, BorderLayout.EAST);

        // 标题栏拖拽
        titleBar.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { dragStart = e.getPoint(); }
        });
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    Point p = getLocation();
                    setLocation(p.x + e.getX() - dragStart.x, p.y + e.getY() - dragStart.y);
                }
            }
        });

        // === 消息区 ===
        messageArea = new JTextPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        messageArea.setEditable(false);
        messageArea.setFont(pixelFont);
        messageArea.setForeground(TEXT_COLOR);
        messageArea.setBackground(BG_COLOR);
        messageArea.setCaretColor(TEXT_COLOR);
        messageArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

        scrollPane = new JScrollPane(messageArea);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));

        // === 打字指示器 ===
        typingIndicator = new JLabel("");
        typingIndicator.setFont(pixelFont);
        typingIndicator.setForeground(TYPING_COLOR);
        typingIndicator.setOpaque(false);

        // === 输入区 ===
        JPanel inputPanel = new JPanel(new BorderLayout(4, 0));
        inputPanel.setOpaque(false);

        inputField = new JTextArea(2, 20);
        inputField.setLineWrap(true);
        inputField.setWrapStyleWord(true);
        inputField.setFont(inputFont);
        inputField.setForeground(TEXT_COLOR);
        inputField.setBackground(INPUT_BG);
        inputField.setCaretColor(TEXT_COLOR);
        inputField.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        // Enter 发送，Shift+Enter 插入换行。
        inputField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send-message");
        inputField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK),
                javax.swing.text.DefaultEditorKit.insertBreakAction);
        inputField.getActionMap().put("send-message", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // 输入框 ESC → 关闭对话框
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    closeDialog();
                }
            }
        });

        JButton sendBtn = new JButton(">");
        sendBtn.setFont(pixelFont.deriveFont(Font.BOLD, 14f));
        sendBtn.setForeground(TEXT_COLOR);
        sendBtn.setBackground(INPUT_BG);
        sendBtn.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        sendBtn.setFocusPainted(false);
        sendBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendBtn.addActionListener(e -> sendMessage());
        JScrollPane inputScrollPane = new JScrollPane(inputField);
        inputScrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        inputScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        inputScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.add(typingIndicator, BorderLayout.NORTH);
        bottomPanel.add(inputPanel, BorderLayout.SOUTH);

        mainPanel.add(titleBar, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);

        // 点击外部区域关闭
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getY() >= 24) return;
                if (e.getX() < 8 || e.getX() > getWidth() - 8
                        || e.getY() < 8 || e.getY() > getHeight() - 8) {
                    closeDialog();
                }
            }
        });

        // ESC 关闭（对话框级别的 KeyListener）
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    closeDialog();
                }
            }
        });
    }

    private void styleTitleBtn(JButton btn) {
        btn.setFont(pixelFont.deriveFont(Font.BOLD, 11f));
        btn.setForeground(TEXT_COLOR);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /** 调整对话框大小 */
    private void resizeDialog(int dw, int dh) {
        dialogWidth = Math.max(280, Math.min(1000, dialogWidth + dw));
        dialogHeight = Math.max(180, Math.min(800, dialogHeight + dh));
        setSize(dialogWidth, dialogHeight);
        positionDialog();
        revalidate();
        repaint();
        SettingsManager.getInstance().updateDialogSize(dialogWidth, dialogHeight);
    }

    /** 恢复当前程序运行期间的对话；程序重启后 history 会重新创建。 */
    private void displayHistory() {
        if (history == null) return;
        for (ConversationHistory.Message message : history.getMessages()) {
            String speaker = "user".equals(message.role) ? "旅行者" :
                    ("assistant".equals(message.role) ? "Dottore" : "System");
            appendMessage(speaker, message.content);
        }
    }

    void applyUiSettings(int width, int height) {
        dialogWidth = width;
        dialogHeight = height;
        initFonts();
        messageArea.setFont(pixelFont);
        inputField.setFont(inputFont);
        typingIndicator.setFont(pixelFont);
        setSize(dialogWidth, dialogHeight);
        positionDialog();
        revalidate();
        repaint();
    }

    private void startPositionTracking() {
        new Timer(100, e -> {
            if (isVisible() && mascot != null) {
                positionDialog();
            }
        }).start();
    }

    private void positionDialog() {
        Point anchor = mascot.getAnchor();
        if (anchor == null) return;
        JWindow mascotWindow = mascot.getWindow().asJWindow();
        Point windowLoc = mascotWindow.getLocationOnScreen();
        Dimension windowSize = mascotWindow.getSize();
        int mascotScreenX = windowLoc.x + (windowSize.width / 2);
        int mascotScreenY = windowLoc.y;
        int dialogX = mascotScreenX - dialogWidth / 2;
        int dialogY = mascotScreenY - dialogHeight - 20;

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        GraphicsConfiguration gc = mascotWindow.getGraphicsConfiguration();
        if (gc != null) {
            Rectangle bounds = gc.getBounds();
            dialogX = Math.max(bounds.x, Math.min(dialogX, bounds.x + bounds.width - dialogWidth));
            dialogY = Math.max(bounds.y, Math.min(dialogY, bounds.y + bounds.height - dialogHeight));
        }
        setLocation(dialogX, dialogY);
    }

    private void sendMessage() {
        if (isWaitingForAI) return;
        String userInput = inputField.getText().trim();
        if (userInput.isEmpty()) return;

        appendMessage("旅行者", userInput);
        history.addUser(userInput);
        inputField.setText("");
        inputField.setEnabled(false);
        showTypingIndicator();
        isWaitingForAI = true;

        aiClient.sendMessageAsync(userInput, history, new AIClient.ChatCallback() {
            @Override
            public void onSuccess(AIClient.AIResponse resp) {
                SwingUtilities.invokeLater(() -> {
                    hideTypingIndicator();
                    String txt = resp.text != null ? resp.text : "";
                    history.addAssistant(txt);
                    typewriterEffect("Dottore", txt);
                    executeToolCalls(resp);
                    checkAutoClose(txt);
                    isWaitingForAI = false;
                    inputField.setEnabled(true);
                    inputField.requestFocus();
                    saveConversationMemory();
                    ChatManager.getInstance().summarizeLastTurns();
                    resetIdleTimer();
                });
            }
            @Override
            public void onError(Throwable error) {
                SwingUtilities.invokeLater(() -> {
                    hideTypingIndicator();
                    String errMsg = error.getMessage();
                    if (errMsg == null || errMsg.isEmpty()) errMsg = error.toString();
                    if (errMsg.length() > 500) errMsg = errMsg.substring(0, 500) + "...";
                    appendMessage("System", "错误: " + errMsg);
                    isWaitingForAI = false;
                    inputField.setEnabled(true);
                    inputField.requestFocus();
                    // 本轮没有 AI 回复，暂不写数据库；之后每轮保存时按最新历史整体替换
                });
            }
        });
    }

    private void typewriterEffect(String speaker, String fullText) {
        appendRawText("[" + speaker + "] ", TEXT_COLOR);
        final int[] idx = {0};
        final int startPos = messageArea.getDocument().getLength();
        javax.swing.Timer timer = new javax.swing.Timer(typingSpeedMs, null);
        timer.addActionListener(e -> {
            if (idx[0] < fullText.length()) {
                try {
                    char c = fullText.charAt(idx[0]);
                    messageArea.getDocument().insertString(
                            messageArea.getDocument().getLength(),
                            String.valueOf(c), null);
                    idx[0]++;
                    messageArea.setCaretPosition(messageArea.getDocument().getLength());
                } catch (Exception ex) {
                    log.log(Level.WARNING, "打字机异常", ex);
                }
            } else {
                appendRawText("\n", TEXT_COLOR);
                // 打字完成后染色：动作描述用灰色
                recolorActionDescriptions(startPos);
                ((javax.swing.Timer) e.getSource()).stop();
            }
        });
        timer.start();
    }

    /** 将文字中的动作描述（*...*、（...）、(...)）染成淡灰色 */
    private void recolorActionDescriptions(int startPos) {
        try {
            javax.swing.text.StyledDocument doc = messageArea.getStyledDocument();
            String fullText = doc.getText(startPos, doc.getLength() - startPos);
            // 匹配 *动作*、（动作）、(动作)
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "\\*[^*]+\\*|（[^）]+）|\\([^)]+\\)");
            java.util.regex.Matcher m = p.matcher(fullText);
            javax.swing.text.Style style = messageArea.addStyle("action", null);
            javax.swing.text.StyleConstants.setForeground(style, new Color(150, 150, 170));
            while (m.find()) {
                doc.setCharacterAttributes(startPos + m.start(),
                    m.end() - m.start(), style, false);
            }
        } catch (Exception e) {
            // 染色失败不影响使用
        }
    }

    private void appendMessage(String speaker, String text) {
        appendRawText("[" + speaker + "] ", TEXT_COLOR);
        appendRawText(text + "\n", TEXT_COLOR);
    }

    private void appendRawText(String text) {
        appendRawText(text, TEXT_COLOR);
    }

    private void appendRawText(String text, Color color) {
        try {
            javax.swing.text.StyledDocument doc = messageArea.getStyledDocument();
            javax.swing.text.Style style = messageArea.addStyle("c", null);
            javax.swing.text.StyleConstants.setForeground(style, color);
            doc.insertString(doc.getLength(), text, style);
            messageArea.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            log.log(Level.WARNING, "追加消息失败", e);
        }
    }

    private javax.swing.Timer typingAnimTimer;

    private void showTypingIndicator() {
        typingIndicator.setText("Dottore 正在思考...");
        typingAnimTimer = new javax.swing.Timer(400, null);
        final String[] frames = {".", "..", "..."};
        final int[] fi = {0};
        typingAnimTimer.addActionListener(e -> {
            typingIndicator.setText("Dottore 正在思考" + frames[fi[0]]);
            fi[0] = (fi[0] + 1) % frames.length;
        });
        typingAnimTimer.start();
    }

    private void hideTypingIndicator() {
        if (typingAnimTimer != null) typingAnimTimer.stop();
        typingIndicator.setText("");
    }

    public void showGreeting(String greeting) {
        appendMessage("Dottore", greeting);
        history.addAssistant(greeting);
    }

    /** 让 AI 自动生成开场白 */
    public void generateAIGreeting() {
        showTypingIndicator();
        isWaitingForAI = true;
        aiClient.sendMessageAsync(
            "(旅行者刚刚点击了你。这是本次对话的第一句话。你作为多托雷，按照你的性格说一句开场白。不要说你为什么在这里之类的话——你一直就在这里。自然地开始对话。)",
            history, new AIClient.ChatCallback() {
                @Override
                public void onSuccess(AIClient.AIResponse resp) {
                    SwingUtilities.invokeLater(() -> {
                        hideTypingIndicator();
                        String txt = resp.text != null ? resp.text : "哼，你终于注意到我了。";
                        history.addAssistant(txt);
                        typewriterEffect("Dottore", txt);
                        executeToolCalls(resp);
                        isWaitingForAI = false;
                        inputField.setEnabled(true);
                        inputField.requestFocus();
                        // 问候也是对话的一部分，同样实时入库
                        saveConversationMemory();
                        startIdleTimer();
                    });
                }
                @Override
                public void onError(Throwable err) {
                    SwingUtilities.invokeLater(() -> {
                        hideTypingIndicator();
                        String fallback = "哦？你终于注意到我了。有什么有趣的实验要和我一起做吗？";
                        history.addAssistant(fallback);
                        typewriterEffect("Dottore", fallback);
                        isWaitingForAI = false;
                        inputField.setEnabled(true);
                        inputField.requestFocus();
                        saveConversationMemory();
                        startIdleTimer();
                    });
                }
            });
    }

    /** 执行 AI 返回的工具调用（仅正面动作类，文件操作已在 AIClient 中处理） */
    private void executeToolCalls(AIClient.AIResponse resp) {
        if (resp.toolCalls.isEmpty()) return;
        for (int i = 0; i < resp.toolCalls.size(); i++) {
            String toolName = resp.toolCalls.get(i);
            String argsJson = i < resp.toolInputs.size() ? resp.toolInputs.get(i) : "{}";
            // 文件操作已在 AIClient 的工具循环中执行，此处只处理需要 mascot 的动作
            if ("turn_around".equals(toolName)) {
                try {
                    MascotTool.execute(mascot, toolName, argsJson);
                } catch (Exception ex) {
                    log.log(Level.WARNING, "工具异常: " + toolName, ex);
                }
            }
        }
    }

    /** 检测 AI 回复中是否有告别/终止对话的信号，有则自动关闭 */
    private void checkAutoClose(String aiText) {
        if (aiText == null || aiText.isEmpty()) return;
        String[] farewells = {
            "再见", "再会", "告辞", "今天就到这里", "到此为止",
            "你可以走了", "退下吧", "下次再说", "今天就到这",
            "实验结束", "今天就聊到这里", "不送了"
        };
        for (String f : farewells) {
            if (aiText.contains(f)) {
                // 给用户 3 秒阅读时间再关
                new javax.swing.Timer(3000, e -> closeDialog()).start();
                return;
            }
        }
    }

    void closeDialog() {
        if (idleTimer != null) idleTimer.stop();
        // 每轮对话都已实时入库，这里只做最终刷新并写可读 JSON 备份
        saveConversationMemory();
        saveSessionBackupJson();
        if (mascot != null) mascot.setAnimatingPublic(true);
        setVisible(false);
    }

    /**
     * 把当前会话实时写入数据库。
     * 每轮对话结束后调用；updateSession 以会话起点以来的全部消息整体替换旧快照，
     * 因此即使某轮保存失败，下一轮成功保存也会自动补齐（失败的轮次不含 AI 回复）。
     */
    private void saveConversationMemory() {
        if (history == null || history.size() < 2) return;
        String storedSessionId = ChatManager.getInstance().updateSessionInMemory(
                sessionId, history.getMessages(), null);
        if (storedSessionId != null && !storedSessionId.isEmpty()) {
            sessionId = storedSessionId; // 内容去重可能复用已有会话
        }
    }

    /** 关闭时写一份可读 JSON 备份到 character/memory/sessions/。 */
    private void saveSessionBackupJson() {
        if (history == null || history.size() < 2) return;
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (ConversationHistory.Message msg : history.getMessages()) {
                java.util.Map<String,String> item = new java.util.HashMap<>();
                item.put("role", msg.role);
                item.put("content", msg.content);
                list.add(item);
            }
            CharacterLoader loader = ChatManager.getInstance().getCharacterLoader();
            java.io.File sessionsDir = new java.io.File(loader.getMemoryDir(), "sessions");
            sessionsDir.mkdirs();
            java.io.File sessionFile = new java.io.File(sessionsDir, sessionId + ".json");
            try (java.io.Writer writer = new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(sessionFile), java.nio.charset.StandardCharsets.UTF_8)) {
                writer.write(gson.toJson(list));
            }
        } catch (Exception e) {
            log.warning("会话 JSON 备份失败: " + e.getMessage());
        }
    }

    void reopen(Mascot mascot) {
        this.mascot = mascot;
        if (idleTimer != null) idleTimer.restart();
        setVisible(true);
        toFront();
        inputField.requestFocusInWindow();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            positionDialog();
            SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());
        }
        super.setVisible(visible);
    }

    private boolean dialogHasFocus() {
        return isFocusOwner() || inputField.isFocusOwner();
    }
}
