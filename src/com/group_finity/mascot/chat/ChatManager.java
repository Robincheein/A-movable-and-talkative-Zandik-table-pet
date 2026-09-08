package com.group_finity.mascot.chat;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.SettingsManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 管理 AI 对话系统的单例。
 * 负责初始化 RAG 索引、AIClient、CharacterLoader 和 ConversationHistory，
 * 并创建 ChatDialog。
 */
public class ChatManager {
    private static final Logger log = Logger.getLogger(ChatManager.class.getName());

    private static ChatManager instance;

    private AIClient aiClient;
    private CharacterLoader characterLoader;
    private ConversationHistory history;
    private Properties config;

    // RAG 组件
    private TFIDFIndex ragIndex;
    private RAGPromptBuilder ragPromptBuilder;
    private boolean ragEnabled = true;

    private ChatDialog activeDialog;
    private final Object memoryUpdateLock = new Object();
    private LongTermMemoryStore memoryStore;

    // 配置值
    private int dialogWidth = 340;
    private int dialogHeight = 220;
    private int typingSpeedMs = 30;
    private int maxHistory = 20;
    private int ragTopK = 5;
    private double ragMinSimilarity = 0.05;
    private boolean memoryEnabled = true;
    private int memoryTopK = 8;

    /** 增量记忆提取：已处理到的最新消息下标（下一次从 summarizedUpTo 开始） */
    private int summarizedUpTo = 0;
    /** 增量记忆提取：是否已有任务待运行，避免每轮对话都新建后台线程 */
    private boolean summarizeScheduled = false;

    public static synchronized ChatManager getInstance() {
        if (instance == null) {
            instance = new ChatManager();
        }
        return instance;
    }

    /** 初始化聊天系统 */
    public void init(File baseDir) {
        loadConfig(baseDir);
        initCharacterLoader(baseDir);
        initMemory(baseDir);
        initRAG(baseDir);
        initAIClient();
    }

    private void loadConfig(File baseDir) {
        config = new Properties();
        File configFile = new File(baseDir, "conf/chat_config.properties");
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                config.load(fis);
                log.info("聊天配置加载完成: " + configFile.getAbsolutePath());
            } catch (IOException e) {
                log.log(Level.WARNING, "无法加载聊天配置，使用默认值", e);
            }
        } else {
            log.info("聊天配置文件不存在，使用默认值: " + configFile.getAbsolutePath());
        }

        dialogWidth = SettingsManager.getInstance().getDialogWidth();
        dialogHeight = SettingsManager.getInstance().getDialogHeight();
        typingSpeedMs = getIntConfig("typing.speed.ms", 30);
        maxHistory = getIntConfig("max.history", 20);
        ragEnabled = getBoolConfig("rag.enabled", true);
        ragTopK = getIntConfig("rag.top_k", 5);
        ragMinSimilarity = getDoubleConfig("rag.min_similarity", 0.05);
        memoryEnabled = getBoolConfig("memory.enabled", true);
        memoryTopK = getIntConfig("memory.retrieve_top_k", 8);
    }

    private void initCharacterLoader(File baseDir) {
        File characterDir = new File(baseDir, "character");
        characterLoader = new CharacterLoader(characterDir);
        characterLoader.loadSystemPrompt();
    }

    private void initMemory(File baseDir) {
        if (!memoryEnabled) {
            log.info("SQLite 长期记忆已禁用");
            return;
        }
        try {
            File memoryDir = characterLoader.getMemoryDir();
            memoryStore = new LongTermMemoryStore(new File(memoryDir, "dottore_memory.db"));
            memoryStore.initialize(memoryDir);
        } catch (Exception e) {
            log.log(Level.WARNING, "SQLite 长期记忆初始化失败，将继续使用基础对话", e);
            memoryStore = null;
        }
    }

    /**
     * 初始化 RAG 向量检索系统。
     */
    private void initRAG(File baseDir) {
        if (!ragEnabled) {
            log.info("RAG 已禁用");
            return;
        }

        try {
            File characterDir = new File(baseDir, "character");
            File indexFile = new File(baseDir, "character/rag_index.json");

            ragIndex = new TFIDFIndex(indexFile);

            // 检查是否需要重建索引
            String currentHash = TFIDFIndex.computeFileHash(characterDir);
            boolean needRebuild = true;

            if (indexFile.exists()) {
                if (ragIndex.loadFromFile()) {
                    if (currentHash.equals(ragIndex.getLastFileHash())) {
                        log.info("RAG 索引未过期，直接加载");
                        needRebuild = false;
                    } else {
                        log.info("检测到角色文件变更，重建 RAG 索引");
                    }
                }
            }

            if (needRebuild) {
                // 分块
                ChunkSplitter splitter = new ChunkSplitter();
                List<ChunkSplitter.Chunk> chunks = splitter.splitAll(characterDir);
                log.info("RAG 文本分块完成: " + chunks.size() + " 个 chunk");

                // 构建索引
                ragIndex.buildIndex(chunks, currentHash);

                // 持久化
                try {
                    ragIndex.saveToFile();
                } catch (IOException e) {
                    log.warning("RAG 索引保存失败: " + e.getMessage());
                }
            }

            // 构建 Prompt Builder
            String corePersonality = buildCorePersonality();
            String rules = loadRules();
            ragPromptBuilder = new RAGPromptBuilder(ragIndex, corePersonality, rules,
                    ragTopK, ragMinSimilarity);

            log.info("RAG 系统初始化完成");
        } catch (Exception e) {
            log.log(Level.WARNING, "RAG 初始化失败（不影响基础对话功能）", e);
            ragEnabled = false;
        }
    }

    /** 获取 CharacterLoader（供外部保存记忆） */
    public CharacterLoader getCharacterLoader() { return characterLoader; }

    /** 保存完整会话到 SQLite；JSON 文件继续作为可读备份保留。 */
    public String saveSessionToMemory(String sessionId,
                                      List<ConversationHistory.Message> messages,
                                      File sourceFile) {
        if (memoryStore == null || !memoryStore.isAvailable()) return sessionId;
        try {
            return memoryStore.saveSession(sessionId, messages, sourceFile);
        } catch (Exception e) {
            log.warning("会话写入 SQLite 失败: " + e.getMessage());
            return sessionId;
        }
    }

    /** 增量写入会话：不存在则创建，存在则按最新消息替换并更新哈希。 */
    public String updateSessionInMemory(String sessionId,
                                        List<ConversationHistory.Message> messages,
                                        File sourceFile) {
        if (memoryStore == null || !memoryStore.isAvailable()) return sessionId;
        try {
            return memoryStore.updateSession(sessionId, messages, sourceFile);
        } catch (Exception e) {
            log.warning("会话增量写入 SQLite 失败: " + e.getMessage());
            return sessionId;
        }
    }

    /**
     * 每轮对话结束后增量提取长期记忆：只处理上次提取位置之后的新消息，
     * 以 5 条为一组发给 AI 抽取结构化记忆，写入 SQLite 并刷新用户画像。
     * 提取在后台线程执行；若上一个任务还在运行，新调用会被合并（运行中的任务会一直处理到最新消息）。
     */
    public void summarizeLastTurns() {
        if (history == null || !history.hasUserMessage()) return;
        if (memoryStore == null || !memoryStore.isAvailable()) {
            log.warning("SQLite 未连接，跳过长期记忆提取");
            return;
        }
        synchronized (memoryUpdateLock) {
            if (summarizeScheduled) return;
            summarizeScheduled = true;
        }
        Thread worker = new Thread(() -> {
            synchronized (memoryUpdateLock) {
                try {
                    boolean anyStored = false;
                    while (true) {
                        final List<ConversationHistory.Message> conversation = history.getMessages();
                        final int total = conversation.size();
                        if (total < 2 || summarizedUpTo >= total - 1) break;
                        int chunkEnd = Math.min(summarizedUpTo + 4, total - 1);
                        StringBuilder conv = new StringBuilder();
                        for (int i = summarizedUpTo; i <= chunkEnd; i++) {
                            ConversationHistory.Message msg = conversation.get(i);
                            conv.append("[").append(msg.role).append("] ")
                                    .append(msg.content).append("\n");
                        }
                        String prompt = "你正在从对话中提取可跨会话使用的长期记忆。只输出合法 JSON，不要使用 Markdown。\n"
                                + "输出格式：{\"memories\":[{\"type\":\"preference\",\"subject\":\"简短稳定主题\","
                                + "\"content\":\"第三人称事实描述\",\"importance\":3,\"confidence\":0.9,"
                                + "\"evidence\":\"支持该事实的用户原话\"}]}\n"
                                + "type 只能是 profile、preference、event、project、relationship、instruction。\n"
                                + "只记录用户明确表达的稳定身份、偏好、重要经历、长期项目、关系变化和长期要求；"
                                + "不要记录问候、临时情绪、模型回复、模型推测或敏感凭据；没有值得记录的信息就输出 {\"memories\":[]}。"
                                + "同一主题使用一致且简短的 subject，最多输出 8 条。\n\n本次对话：\n" + conv;

                        AIClient.AIResponse resp = aiClient.sendStructuredMemoryDirect(prompt);
                        if (resp == null) {
                            log.warning("长期记忆提取没有返回结果");
                            return;
                        }
                        int stored = memoryStore.storeCandidatesFromJson(resp.text, null);
                        if (stored > 0) {
                            anyStored = true;
                            log.info("SQLite 长期记忆已更新: " + stored + " 条");
                        }
                        summarizedUpTo = chunkEnd + 1;
                    }
                    if (anyStored) {
                        String snapshot = memoryStore.buildProfileSnapshot();
                        if (!snapshot.isEmpty()) characterLoader.saveUserProfile(snapshot);
                        // 重建 RAG Prompt Builder，让新记忆无需重启即可进入后续对话
                        refreshLongTermMemoryPrompt();
                    }
                } catch (Exception e) {
                    log.warning("SQLite 长期记忆更新失败: " + e.getMessage());
                } finally {
                    summarizeScheduled = false;
                }
            }
        }, "dottore-memory-summarizer");
        worker.setDaemon(true);
        worker.start();
    }

    public LongTermMemoryStore getMemoryStore() { return memoryStore; }
    private boolean isValidMemoryProfile(String text) {
        if (text == null) return false;
        String cleaned = cleanMemoryProfile(text);
        String lower = cleaned.toLowerCase();
        return cleaned.length() >= 40
                && cleaned.contains("#")
                && cleaned.contains("旅行者")
                && !lower.contains("the user is asking me")
                && !lower.contains("conversation log is in mojibake")
                && !lower.contains("as an ai");
    }

    private String cleanMemoryProfile(String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:markdown|md|text)?\\s*", "")
                    .replaceFirst("\\s*```$", "").trim();
        }
        return cleaned;
    }

    /**
     * 从 analysis/ 和 personality.txt 构建核心性格 prompt。
     */
    private String buildCorePersonality() {
        StringBuilder sb = new StringBuilder();

        sb.append(characterLoader.loadCorePrompt()).append("\n\n");

        // SQLite 不可用时才回退到旧版整份文本画像。
        String profile = characterLoader.loadUserProfile();
        if ((memoryStore == null || !memoryStore.isAvailable())
                && profile != null && !profile.isEmpty() && !profile.contains("尚未记录")) {
            sb.append("【关于正在与你对话的旅行者——这是你之前观察到并记录的信息】\n");
            sb.append(profile).append("\n\n");
        }

        if (sb.length() == 0) {
            sb.append("你是多托雷（Il Dottore），愚人众执行官第二席「博士」。");
            sb.append("一位傲慢的天才科学家，将一切视为实验对象。");
        }

        return sb.toString().trim();
    }

    private String loadRules() {
        String rules = characterLoader.loadSpecificFile("rules.txt");
        return rules != null ? rules : "";
    }

    /** 画像更新后重建 Prompt Builder，使记忆无需重启即可生效。 */
    private synchronized void refreshLongTermMemoryPrompt() {
        if (!ragEnabled || ragIndex == null) return;
        ragPromptBuilder = new RAGPromptBuilder(ragIndex, buildCorePersonality(), loadRules(),
                ragTopK, ragMinSimilarity);
        if (aiClient != null) aiClient.setRAGPromptBuilder(ragPromptBuilder);
    }

    private void initAIClient() {
        String provider = config.getProperty("ai.provider", "api").trim().toLowerCase();
        boolean useOllama = "ollama".equals(provider);
        String apiKey;
        String endpoint;
        String model;
        if (useOllama) {
            apiKey = "ollama";
            endpoint = config.getProperty("ollama.endpoint",
                    "http://127.0.0.1:11434/v1/chat/completions").trim();
            model = config.getProperty("ollama.model", "dottore:7b").trim();
            log.info("AI 后端: Ollama 本地模型 " + model);
        } else {
            apiKey = config.getProperty("api.key", "");
            endpoint = config.getProperty("api.endpoint",
                    "https://api.deepseek.com/v1/chat/completions");
            model = config.getProperty("api.model", "deepseek-chat");
            log.info("AI 后端: 远程 API " + model);
        }

        aiClient = new AIClient(apiKey, endpoint, model);
        if (useOllama) {
            int readTimeoutSeconds = getIntConfig("ollama.read_timeout_seconds", 180);
            aiClient.setTimeouts(5000, readTimeoutSeconds * 1000);
            aiClient.setGenerationSettings(
                    getDoubleConfig("ollama.temperature", 0.45),
                    getDoubleConfig("ollama.top_p", 0.80),
                    getIntConfig("ollama.max_tokens", 360));
        } else {
            aiClient.setGenerationSettings(
                    getDoubleConfig("api.temperature", 0.65),
                    getDoubleConfig("api.top_p", 0.9),
                    getIntConfig("api.max_tokens", 1000));
        }
        aiClient.setCharacterLoader(characterLoader);
        aiClient.setRAGPromptBuilder(ragPromptBuilder);
        aiClient.setRAGEnabled(ragEnabled);
        aiClient.setLongTermMemoryStore(memoryStore, memoryTopK);
        aiClient.setToolsEnabled(!useOllama);
        aiClient.setRoleStyleGuard(useOllama);
    }
    /**
     * 为指定 mascot 打开对话窗口。
     */
    public void openChatDialog(Mascot mascot) {
        // 同一程序进程内复用同一个窗口，保留界面内容和正在进行的回答。
        if (activeDialog != null) {
            if (activeDialog.isVisible()) {
                activeDialog.toFront();
                activeDialog.requestFocus();
            } else {
                activeDialog.reopen(mascot);
            }
            return;
        }

        // history 只在本次程序进程中保留；程序重启后单例会重新创建。
        boolean firstConversation = history == null || history.size() == 0;
        if (history == null) history = new ConversationHistory(maxHistory);
        mascot.setAnimatingPublic(true);

        ChatDialog dialog = new ChatDialog(mascot, aiClient, history,
                dialogWidth, dialogHeight, typingSpeedMs);
        activeDialog = dialog;
        if (firstConversation) dialog.generateAIGreeting();
        dialog.setVisible(true);
    }

    /** 设置窗口保存后立即更新当前对话框。 */
    public void applyUiSettings() {
        dialogWidth = SettingsManager.getInstance().getDialogWidth();
        dialogHeight = SettingsManager.getInstance().getDialogHeight();
        if (activeDialog != null) {
            activeDialog.applyUiSettings(dialogWidth, dialogHeight);
        }
    }

    /** 获取当前对话历史 */
    public ConversationHistory getCurrentHistory() {
        return history;
    }

    /** 新建对话 */
    public void newConversation() {
        if (activeDialog != null) activeDialog.dispose();
        activeDialog = null;
        history = new ConversationHistory(maxHistory);
        synchronized (memoryUpdateLock) {
            summarizedUpTo = 0;
        }
    }

    /** 手动重建 RAG 索引 */
    public void rebuildRAGIndex(File baseDir) {
        if (!ragEnabled) return;
        try {
            File characterDir = new File(baseDir, "character");
            File indexFile = new File(baseDir, "character/rag_index.json");

            ChunkSplitter splitter = new ChunkSplitter();
            List<ChunkSplitter.Chunk> chunks = splitter.splitAll(characterDir);
            String currentHash = TFIDFIndex.computeFileHash(characterDir);
            ragIndex.buildIndex(chunks, currentHash);
            ragIndex.saveToFile();

            // 重建 Prompt Builder
            String corePersonality = buildCorePersonality();
            String rules = loadRules();
            ragPromptBuilder = new RAGPromptBuilder(ragIndex, corePersonality, rules,
                    ragTopK, ragMinSimilarity);

            log.info("RAG 索引手动重建完成");
            if (aiClient != null) aiClient.setRAGPromptBuilder(ragPromptBuilder);
        } catch (Exception e) {
            log.log(Level.WARNING, "RAG 索引重建失败", e);
        }
    }

    private int getIntConfig(String key, int defaultValue) {
        String val = config.getProperty(key);
        if (val != null) {
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException e) {
                log.warning("配置项 " + key + " 的值无效: " + val);
            }
        }
        return defaultValue;
    }

    private double getDoubleConfig(String key, double defaultValue) {
        String val = config.getProperty(key);
        if (val != null) {
            try {
                return Double.parseDouble(val.trim());
            } catch (NumberFormatException e) {
                log.warning("配置项 " + key + " 的值无效: " + val);
            }
        }
        return defaultValue;
    }

    private boolean getBoolConfig(String key, boolean defaultValue) {
        String val = config.getProperty(key);
        if (val != null) {
            return Boolean.parseBoolean(val.trim());
        }
        return defaultValue;
    }
}
