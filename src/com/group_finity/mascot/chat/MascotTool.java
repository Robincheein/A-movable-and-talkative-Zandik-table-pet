package com.group_finity.mascot.chat;

import com.group_finity.mascot.Mascot;
import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;
import java.awt.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * 工具执行器——AI Agent 的全部能力。
 * 文件只读 + 系统信息 + 网络搜索 + 动作控制。
 */
public class MascotTool {
    private static final Logger log = Logger.getLogger(MascotTool.class.getName());

    /** 禁止读取的文件名关键词（仅屏蔽真正敏感的） */
    private static final String[] BLOCKED_KEYWORDS = {
        ".ssh", ".gnupg", "id_rsa", "known_hosts",
        "NTUSER.DAT", "SAM", "SYSTEM", "SOFTWARE", "SECURITY",
        "AppData", "Cookies", "Login Data", "Web Data", "Credential"
    };

    /** 允许读取的文本扩展名 */
    private static final Set<String> TEXT_EXTS = new HashSet<>(Arrays.asList(
        "txt","md","java","py","js","ts","html","css","xml","json",
        "yml","yaml","properties","cfg","ini","conf","log","csv",
        "c","cpp","h","hpp","rs","go","rb","php","sh","bat","ps1",
        "sql","r","lua","swift","kt","scala","clj","hs","ml",
        "pdf","docx","xlsx","pptx","rtf"
    ));

    // ============ 文件操作 ============

    private static String readFile(String argsJson) {
        try {
            String path = extractArg(argsJson, "path");
            if (path == null) return "缺少参数: path";
            File f = new File(path);
            String normalized = f.getCanonicalPath().replace('\\', '/').toLowerCase();
            String fileName = f.getName().toLowerCase();
            for (String kw : BLOCKED_KEYWORDS) {
                if (normalized.contains(kw.toLowerCase()))
                    return "安全限制：路径包含敏感关键词 '" + kw + "'";
            }
            if (!f.exists()) return "文件不存在";
            if (f.isDirectory()) return "是目录，不是文件";
            if (f.length() > 1_000_000) return "文件过大 (" + (f.length()/1024) + " KB)";
            String content = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
            if (content.length() > 8000) content = content.substring(0,8000) + "\n...(截断)";
            return content;
        } catch (Exception e) { return "读取失败: " + e.getMessage(); }
    }

    private static String listFiles(String argsJson) {
        String path = extractArg(argsJson, "path");
        if (path == null || path.isEmpty()) path = System.getProperty("user.home");
        try {
            File dir = new File(path);
            if (!dir.exists()) return "目录不存在";
            if (!dir.isDirectory()) return "不是目录";
            File[] files = dir.listFiles();
            if (files == null) return "无法读取";
            if (files.length > 200) files = Arrays.copyOf(files, 200);
            StringBuilder sb = new StringBuilder(path).append("\n");
            for (File f : files) {
                if (f.getName().startsWith(".")) continue;
                boolean blocked = false;
                for (String kw : BLOCKED_KEYWORDS)
                    if (f.getName().toLowerCase().contains(kw.toLowerCase())) { blocked=true; break; }
                if (blocked) continue;
                String t = f.isDirectory() ? "[DIR]" : "[FILE]";
                String s = f.isFile() ? " "+formatSize(f.length()) : "";
                sb.append("  ").append(t).append(" ").append(f.getName()).append(s).append("\n");
            }
            return sb.toString();
        } catch (Exception e) { return "错误: " + e.getMessage(); }
    }

    // ============ 系统信息 ============

    private static String getTime(String argsJson) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm:ss EEEE"));
    }

    private static String getSystemInfo(String argsJson) {
        Runtime rt = Runtime.getRuntime();
        long totalMem = rt.totalMemory();
        long freeMem = rt.freeMemory();
        long usedMem = totalMem - freeMem;
        StringBuilder sb = new StringBuilder();
        sb.append("操作系统: ").append(System.getProperty("os.name")).append(" ")
          .append(System.getProperty("os.version")).append("\n");
        sb.append("Java版本: ").append(System.getProperty("java.version")).append("\n");
        sb.append("CPU核心: ").append(rt.availableProcessors()).append("\n");
        sb.append("内存: 已用").append(usedMem/1024/1024).append("MB / 共")
          .append(totalMem/1024/1024).append("MB\n");
        // 磁盘
        for (File root : File.listRoots()) {
            try {
                long total = root.getTotalSpace();
                long free = root.getFreeSpace();
                sb.append("磁盘 ").append(root.getPath()).append(": ")
                  .append("共").append(total/1024/1024/1024).append("GB, ")
                  .append("可用").append(free/1024/1024/1024).append("GB\n");
            } catch (Exception ignored) {}
        }
        sb.append("用户名: ").append(System.getProperty("user.name")).append("\n");
        sb.append("工作目录: ").append(System.getProperty("user.dir")).append("\n");
        return sb.toString();
    }

    private static String getScreenSize(String argsJson) {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        return "屏幕: " + d.width + "x" + d.height;
    }

    // ============ 网络 ============

    private static String webSearch(String argsJson) {
        String query = extractArg(argsJson, "query");
        if (query == null || query.isEmpty()) return "需要搜索关键词";
        try {
            String enc = URLEncoder.encode(query, "UTF-8");
            // 用 DuckDuckGo Lite 版（免 API key，返回 HTML）
            URL url = new URL("https://lite.duckduckgo.com/lite/?q=" + enc);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            String html = readStream(conn.getInputStream());
            // 简单提取标题+摘要
            StringBuilder sb = new StringBuilder("搜索: " + query + "\n");
            Pattern p = Pattern.compile("<a[^>]*class=\"result-link\"[^>]*>([^<]+)</a>.*?class=\"result-snippet\"[^>]*>([^<]+)</td>",
                    Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(html);
            int count = 0;
            while (m.find() && count < 8) {
                String title = m.group(1).trim();
                String snippet = m.group(2).replaceAll("<[^>]+>","").trim();
                sb.append(count+1).append(". ").append(title).append("\n   ").append(snippet).append("\n");
                count++;
            }
            if (count == 0) {
                sb.append("未找到结果。HTML 长度: ").append(html.length());
                sb.append("\n预览: ").append(html.substring(0, Math.min(500, html.length())));
            }
            return sb.toString();
        } catch (Exception e) {
            return "搜索失败: " + e.getMessage() + " (可尝试更简单的关键词)";
        }
    }

    private static String webFetch(String argsJson) {
        String urlStr = extractArg(argsJson, "url");
        if (urlStr == null || urlStr.isEmpty()) return "缺少参数: url";
        try {
            if (!urlStr.startsWith("http")) urlStr = "https://" + urlStr;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            String html = readStream(conn.getInputStream());
            // 简单清洗 HTML
            String text = html.replaceAll("<script[^>]*>.*?</script>", "")
                    .replaceAll("<style[^>]*>.*?</style>", "")
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (text.length() > 5000) text = text.substring(0,5000) + "\n...(截断)";
            return text;
        } catch (Exception e) {
            return "获取失败: " + e.getMessage();
        }
    }

    // ============ 桌面感知 ============

    /** 获取当前活动窗口标题 */
    private static String getActiveWindow(String argsJson) {
        try {
            User32 user32 = User32.INSTANCE;
            WinDef.HWND hwnd = user32.GetForegroundWindow();
            char[] title = new char[512];
            user32.GetWindowText(hwnd, title, 512);
            String t = Native.toString(title).trim();
            return "当前窗口: " + (t.isEmpty() ? "(无标题)" : t);
        } catch (Exception e) { return "窗口检测失败"; }
    }

    /** 列出所有可见窗口 */
    private static String getRunningApps(String argsJson) {
        try {
            User32 user32 = User32.INSTANCE;
            java.util.List<String> titles = new ArrayList<>();
            user32.EnumWindows((hwnd, ptr) -> {
                if (user32.IsWindowVisible(hwnd)) {
                    char[] t = new char[512];
                    user32.GetWindowText(hwnd, t, 512);
                    String s = Native.toString(t).trim();
                    if (!s.isEmpty() && s.length() > 1 && !s.equals("Program Manager"))
                        titles.add(s);
                }
                return true;
            }, null);
            StringBuilder sb = new StringBuilder("可见窗口:\n");
            for (int i = 0; i < Math.min(titles.size(), 25); i++)
                sb.append("  ").append(titles.get(i)).append("\n");
            if (titles.size() > 25) sb.append("  ...(共").append(titles.size()).append("个)");
            return sb.toString();
        } catch (Exception e) { return "枚举失败"; }
    }

    // ============ 工具执行 ============

    public static String execute(Mascot mascot, String toolName, String argsJson) {
        try {
            switch (toolName) {
                case "turn_around":
                    if (mascot != null) mascot.setLookRight(!mascot.isLookRight());
                    return "ok";
                case "read_file": return readFile(argsJson);
                case "list_files": return listFiles(argsJson);
                case "get_time": return getTime(argsJson);
                case "get_system_info": return getSystemInfo(argsJson);
                case "get_screen_size": return getScreenSize(argsJson);
                case "web_search": return webSearch(argsJson);
                case "web_fetch": return webFetch(argsJson);
                case "get_active_window": return getActiveWindow(argsJson);
                case "get_running_apps": return getRunningApps(argsJson);
                default: return "未知工具: " + toolName;
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "工具失败: " + toolName, e);
            return "失败: " + e.getMessage();
        }
    }

    // ============ 辅助 ============

    private static String extractArg(String argsJson, String key) {
        if (argsJson == null || argsJson.isEmpty()) return null;
        String sk = "\"" + key + "\"";
        int ki = argsJson.indexOf(sk);
        if (ki < 0) return null;
        int c = argsJson.indexOf(":", ki);
        if (c < 0) return null;
        int sq = argsJson.indexOf("\"", c);
        if (sq < 0) return null;
        int eq = argsJson.indexOf("\"", sq + 1);
        if (eq < 0) return null;
        return argsJson.substring(sq + 1, eq);
    }

    private static String formatSize(long b) {
        if (b < 1024) return b+"B";
        if (b < 1024*1024) return (b/1024)+"KB";
        return (b/(1024*1024))+"MB";
    }

    private static String readStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096]; int n;
        while ((n = is.read(buf)) > 0) baos.write(buf,0,n);
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    public static String getToolsJson() {
        return "[" +
            "{\"name\":\"turn_around\",\"description\":\"转身\",\"input_schema\":{\"type\":\"object\",\"properties\":{},\"required\":[]}}," +
            "{\"name\":\"read_file\",\"description\":\"读取一个文件的内容。传入完整路径。\",\"input_schema\":{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\",\"description\":\"文件完整路径\"}},\"required\":[\"path\"]}}," +
            "{\"name\":\"list_files\",\"description\":\"列出目录内容。\",\"input_schema\":{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\",\"description\":\"目录路径\"}},\"required\":[]}}," +
            "{\"name\":\"get_time\",\"description\":\"获取当前日期和时间\",\"input_schema\":{\"type\":\"object\",\"properties\":{},\"required\":[]}}," +
            "{\"name\":\"get_system_info\",\"description\":\"获取电脑系统信息：操作系统、内存、磁盘空间、CPU等\",\"input_schema\":{\"type\":\"object\",\"properties\":{},\"required\":[]}}," +
            "{\"name\":\"get_screen_size\",\"description\":\"获取屏幕分辨率\",\"input_schema\":{\"type\":\"object\",\"properties\":{},\"required\":[]}}," +
            "{\"name\":\"web_search\",\"description\":\"搜索网页。当旅行者问需要查资料的问题时使用。\",\"input_schema\":{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\",\"description\":\"搜索关键词\"}},\"required\":[\"query\"]}}," +
            "{\"name\":\"web_fetch\",\"description\":\"获取一个网页的内容。传入完整URL。\",\"input_schema\":{\"type\":\"object\",\"properties\":{\"url\":{\"type\":\"string\"}},\"required\":[\"url\"]}}," +
            "{\"name\":\"get_active_window\",\"description\":\"获取当前活动窗口的标题，看看实验对象在做什么。\",\"input_schema\":{\"type\":\"object\",\"properties\":{},\"required\":[]}}," +
            "{\"name\":\"get_running_apps\",\"description\":\"列出所有可见窗口。当你想了解实验对象的后台运行状况时使用。\",\"input_schema\":{\"type\":\"object\",\"properties\":{},\"required\":[]}}" +
            "]";
    }
}
