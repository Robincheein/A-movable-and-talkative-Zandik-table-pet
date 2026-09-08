package com.group_finity.mascot.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 从 character/ 文件夹加载多托雷角色人设。
 * 只固定加载核心卡和示例；原始资料由 RAG 按需检索。
 */
public class CharacterLoader {
    private static final Logger log = Logger.getLogger(CharacterLoader.class.getName());

    /** character 文件夹路径 */
    private final File characterDir;

    /** 缓存的 system prompt */
    private String cachedPrompt;

    /** 上次加载时间 */
    private long lastLoadTime = 0;

    /** 获取 memory 目录 */
    public File getMemoryDir() {
        return new File(characterDir, "memory");
    }

    /** 加载用户画像 */
    public String loadUserProfile() {
        File profile = new File(getMemoryDir(), "user_profile.txt");
        if (!profile.exists()) return "";
        try {
            return readTextFileAuto(profile);
        } catch (IOException e) {
            return "";
        }
    }

    /** 保存用户画像（统一使用 UTF-8，避免 Windows 默认编码导致乱码） */
    public void saveUserProfile(String content) {
        File profile = new File(getMemoryDir(), "user_profile.txt");
        getMemoryDir().mkdirs();
        if (profile.exists()) {
            try {
                Files.copy(profile.toPath(), new File(getMemoryDir(), "user_profile.previous.txt").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.warning("备份旧用户画像失败: " + e.getMessage());
            }
        }
        writeUtf8(profile, content);
    }

    /** 保存对话日志 */
    public void saveConversationLog(String content) {
        File logsDir = new File(getMemoryDir(), "conversation_logs");
        logsDir.mkdirs();
        String filename = "chat_" + new java.text.SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new java.util.Date()) + ".txt";
        writeUtf8(new File(logsDir, filename), content);
    }

    public CharacterLoader(File characterDir) {
        this.characterDir = characterDir;
    }

    /**
     * 加载角色人设，返回完整 system prompt。
     * 固定核心卡和规则，不将外部资料当作系统指令拼接。
     */
    public String loadSystemPrompt() {
        // Raw sources are evidence, never an unconditional system instruction.
        String core = loadCorePrompt();
        String rules = loadSpecificFile("rules.txt");
        cachedPrompt = core + "\n\n" + (rules == null ? "" : rules);
        lastLoadTime = System.currentTimeMillis();
        return cachedPrompt;
    }

    /** Small, explicit always-on character card; analyses remain searchable evidence. */
    public String loadCorePrompt() {
        StringBuilder prompt = new StringBuilder();
        for (String path : new String[] {"personality.txt", "background.txt",
                "core_profile.txt", "dialogue_examples.txt"}) {
            String content = loadSpecificFile(path);
            if (content != null && !content.isEmpty()) {
                prompt.append(content).append("\n\n");
            }
        }
        return prompt.length() == 0 ? getFallbackPrompt() : prompt.toString().trim();
    }


    /** 读取文件全部内容（UTF-8） */
    private String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /** 自动识别 UTF-8；兼容项目中旧的 GBK/GB18030 记忆文件。 */
    private String readTextFileAuto(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, Charset.forName("GB18030"));
        }
    }

    private void writeUtf8(File file, String content) {
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content == null ? "" : content);
        } catch (IOException e) {
            log.warning("保存文件失败: " + file.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    /** 获取缓存的 prompt，如果文件有更新则重新加载 */
    public String getSystemPrompt() {
        if (cachedPrompt == null || hasFilesChanged()) {
            return loadSystemPrompt();
        }
        return cachedPrompt;
    }

    /** 检查 character 文件夹中的文件是否有更新 */
    private boolean hasFilesChanged() {
        if (!characterDir.exists()) return false;
        return findNewestFileTime(characterDir) > lastLoadTime;
    }

    private long findNewestFileTime(File dir) {
        long newest = 0;
        File[] files = dir.listFiles();
        if (files == null) return newest;
        for (File file : files) {
            if (file.isDirectory()) {
                if ("memory".equalsIgnoreCase(file.getName())) continue;
                newest = Math.max(newest, findNewestFileTime(file));
            } else if (file.getName().toLowerCase().endsWith(".txt")) {
                newest = Math.max(newest, file.lastModified());
            }
        }
        return newest;
    }

    /**
     * 加载 character 文件夹下的特定文件。
     * @param relativePath 相对于 character/ 的路径，如 "personality.txt" 或 "analysis/01_core_identity.txt"
     * @return 文件内容，不存在时返回 null
     */
    public String loadSpecificFile(String relativePath) {
        File file = new File(characterDir, relativePath);
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        try {
            return readFile(file).trim();
        } catch (IOException e) {
            log.log(Level.WARNING, "读取文件失败: " + file.getAbsolutePath(), e);
            return null;
        }
    }

    /** 如果 character 文件夹不存在，使用备用 prompt */
    private String getFallbackPrompt() {
        return "你是多托雷（Dottore），愚人众执行官第二席「博士」。"
                + "你是一位傲慢的天才科学家。请以冷静、理性的态度对话。";
    }
}
