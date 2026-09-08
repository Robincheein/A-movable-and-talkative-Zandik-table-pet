package com.group_finity.mascot.chat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.SwingWorker;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DeepSeek API 客户端。
 * 使用 OpenAI 兼容格式调用 DeepSeek Chat API。
 */
public class AIClient {
    private static final Logger log = Logger.getLogger(AIClient.class.getName());

    private final String apiKey;
    private final String endpoint;
    private final String model;
    private final Gson gson = new Gson();
    private int connectTimeoutMs = 15000;
    private int readTimeoutMs = 30000;
    private double temperature = 0.8;
    private double topP = 0.9;
    private int maxTokens = 1000;
    private boolean roleStyleGuard = false;

    /** 角色人设加载器（回退方案） */
    private CharacterLoader characterLoader;

    /** RAG Prompt 构建器 */
    private RAGPromptBuilder ragPromptBuilder;

    /** SQLite 长期记忆（每轮按当前话题动态检索） */
    private LongTermMemoryStore memoryStore;
    private int memoryTopK = 8;

    /** 是否启用工具调用 */
    private boolean toolsEnabled = true;

    /** AI 回复结果 */
    public static class AIResponse {
        public String text;
        public java.util.List<String> toolCalls = new java.util.ArrayList<>();
        public java.util.List<String> toolInputs = new java.util.ArrayList<>(); // JSON args
    }

    /** RAG 是否启用 */
    private boolean ragEnabled = true;

    public AIClient(String apiKey, String endpoint, String model) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = model;
    }

    public void setCharacterLoader(CharacterLoader loader) {
        this.characterLoader = loader;
    }

    public void setRAGPromptBuilder(RAGPromptBuilder builder) {
        this.ragPromptBuilder = builder;
    }

    public void setRAGEnabled(boolean enabled) {
        this.ragEnabled = enabled;
    }

    public void setLongTermMemoryStore(LongTermMemoryStore store, int topK) {
        this.memoryStore = store;
        this.memoryTopK = Math.max(1, Math.min(20, topK));
    }

    public void setTimeouts(int connectTimeoutMs, int readTimeoutMs) {
        this.connectTimeoutMs = Math.max(1000, connectTimeoutMs);
        this.readTimeoutMs = Math.max(5000, readTimeoutMs);
    }

    public void setGenerationSettings(double temperature, double topP, int maxTokens) {
        this.temperature = Math.max(0.0, Math.min(2.0, temperature));
        this.topP = Math.max(0.1, Math.min(1.0, topP));
        this.maxTokens = Math.max(64, Math.min(4096, maxTokens));
    }

    /**
     * 异步发送消息，通过回调返回 AI 回复。
     *
     * @param userMessage  用户输入
     * @param history      对话历史（不含 system prompt）
     * @param callback     回调：onSuccess(aiReply) 或 onError(exception)
     */
    public void setToolsEnabled(boolean enabled) { this.toolsEnabled = enabled; }
    public void setRoleStyleGuard(boolean enabled) { this.roleStyleGuard = enabled; }

    /** 同步发送消息（供后台线程调用，不通过 SwingWorker） */
    public AIResponse sendMessageDirect(String userMessage, ConversationHistory history) {
        try {
            return sendWithToolLoop(userMessage, history);
        } catch (Exception e) {
            log.log(Level.WARNING, "同步 AI 调用失败", e);
            return null;
        }
    }

    /** 独立的结构化记忆提取请求，不注入角色人设、RAG、长期记忆或工具。 */
    public AIResponse sendStructuredMemoryDirect(String prompt) {
        try {
            String system = "你是严格的信息抽取器。只根据用户提供的对话输出符合要求的合法 JSON，"
                    + "不得角色扮演，不得补充未明确表达的事实。";
            return sendMessage(prompt, new ConversationHistory(1), system, false);
        } catch (Exception e) {
            log.log(Level.WARNING, "结构化记忆提取调用失败", e);
            return null;
        }
    }

    public void sendMessageAsync(String userMessage, ConversationHistory history,
                                  ChatCallback callback) {
        new SwingWorker<AIResponse, Void>() {
            @Override
            protected AIResponse doInBackground() throws Exception {
                return sendWithToolLoop(userMessage, history);
            }

            @Override
            protected void done() {
                try {
                    AIResponse result = get();
                    callback.onSuccess(result);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause == null) cause = e;
                    java.io.StringWriter sw = new java.io.StringWriter();
                    cause.printStackTrace(new java.io.PrintWriter(sw));
                    String stack = sw.toString();
                    log.log(Level.SEVERE, "AI API 调用失败: " + stack);
                    callback.onError(new RuntimeException(cause.toString() + "\n"
                            + stack.substring(0, Math.min(300, stack.length()))));
                } catch (Exception e) {
                    log.log(Level.SEVERE, "AI API 调用异常", e);
                    callback.onError(e);
                }
            }
        }.execute();
    }

    /**
     * 同步发送消息到 DeepSeek API。
     */
    /**
     * 带工具循环的消息发送：
     * AI 调用工具 → 执行 → 结果回传 → AI 再次回复（最多 3 轮）
     */
    private AIResponse sendWithToolLoop(String userMessage, ConversationHistory history) throws Exception {
        AIResponse resp = sendMessage(userMessage, history);
        int loop = 0;
        int maxLoops = 3;

        while (!resp.toolCalls.isEmpty() && loop < maxLoops) {
            loop++;
            log.info("工具循环第 " + loop + " 轮: " + resp.toolCalls);

            // 执行所有工具调用，收集结果
            StringBuilder toolResults = new StringBuilder();
            for (int i = 0; i < resp.toolCalls.size(); i++) {
                String toolName = resp.toolCalls.get(i);
                String argsJson = i < resp.toolInputs.size() ? resp.toolInputs.get(i) : "{}";
                String result = MascotTool.execute(null, toolName, argsJson);
                toolResults.append("[工具: ").append(toolName).append("]\n").append(result).append("\n");
            }

            // 将工具结果作为用户消息追加到对话中
            String followUpPrompt = "以下是工具执行的结果：\n" + toolResults.toString()
                    + "\n请基于这些结果继续回答旅行者的问题。不要重复工具结果的内容，直接用你的话回答。";
            history.addUser(followUpPrompt);

            // 再次调用 API；工具结果只服务于本轮，不写入长期对话历史。
            try {
                resp = sendMessage(followUpPrompt, history);
            } finally {
                history.removeLastUser();
            }
        }

        return resp;
    }

    /** 从 AIResponse 的原始 JSON 中提取工具参数（简易实现） */
    private String extractToolArgs(AIResponse resp, String toolName) {
        // toolCalls 列表只存了工具名，参数需要从原始响应中获取
        // 目前 read_file 和 list_files 的参数从工具名推断不了，返回 null
        // 让 execute 方法自己从默认值处理
        return null;
    }

    private AIResponse sendMessage(String userMessage, ConversationHistory history) throws Exception {
        return sendMessage(userMessage, history, null, toolsEnabled);
    }

    private AIResponse sendMessage(String userMessage, ConversationHistory history,
                                   String systemPromptOverride, boolean allowTools) throws Exception {
        // 构建消息列表
        List<Map<String, String>> messages = new ArrayList<>();

        // System prompt（优先使用显式覆盖，其次使用 RAG，最后回退到角色人设）
        String systemPrompt;
        if (systemPromptOverride != null) {
            systemPrompt = systemPromptOverride;
        } else if (ragEnabled && ragPromptBuilder != null) {
            systemPrompt = ragPromptBuilder.buildSystemPrompt(userMessage,
                    history.toApiFormat());
            log.fine("使用 RAG 增强 prompt");
        } else if (characterLoader != null) {
            systemPrompt = characterLoader.getSystemPrompt();
            log.fine("使用基础角色 prompt");
        } else {
            systemPrompt = "你是多托雷，一位傲慢的天才科学家。";
        }

        if (systemPromptOverride == null && memoryStore != null && memoryStore.isAvailable()) {
            String memoryPrompt = memoryStore.buildMemoryPrompt(userMessage, memoryTopK);
            if (memoryPrompt != null && !memoryPrompt.isEmpty()) {
                systemPrompt += "\n\n" + memoryPrompt;
            }
        }

        // 对话历史（不含 system）
        boolean isAnthropic = endpoint.contains("anthropic");
        if (!isAnthropic) {
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);
        }
        messages.addAll(history.toApiFormat());

        // 调用方通常已把当前输入放入 history；仅在问候、闲置发言、总结等
        // 临时请求中补充它，避免同一句用户消息被发送两遍。
        if (!history.endsWithUserMessage(userMessage)) {
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);
        }

        // 构建请求体（兼容 OpenAI 和 Anthropic 格式）
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        if (isAnthropic) {
            requestBody.addProperty("system", systemPrompt);
        }
        requestBody.add("messages", gson.toJsonTree(messages));
        requestBody.addProperty("temperature", temperature);
        requestBody.addProperty("max_tokens", maxTokens);
        if (!isAnthropic) {
            requestBody.addProperty("top_p", topP);
        }
        // 工具调用
        if (allowTools && isAnthropic) {
            requestBody.add("tools", JsonParser.parseString(MascotTool.getToolsJson()).getAsJsonArray());
        }

        String jsonBody = gson.toJson(requestBody);
        log.fine("API 请求: " + jsonBody);

        // HTTP 连接
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        // Anthropic API 用 x-api-key，OpenAI API 用 Authorization: Bearer
        if (endpoint.contains("anthropic")) {
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("anthropic-version", "2023-06-01");
        } else {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        conn.setDoOutput(true);
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);

        // 发送请求
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        // 读取响应
        int responseCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        java.io.InputStream bodyStream;
        if (responseCode >= 200 && responseCode < 300) {
            bodyStream = conn.getInputStream();
        } else {
            bodyStream = conn.getErrorStream();
        }
        // 如果 errorStream 也是 null，直接报错
        if (bodyStream == null) {
            throw new RuntimeException("API 返回错误码 " + responseCode + "，且无错误详情。请检查 endpoint 和 API Key 是否正确。");
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(bodyStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        if (responseCode != 200) {
            throw new RuntimeException("API 错误 " + responseCode + ": " + response.toString());
        }

        // 解析响应 —— 兼容 OpenAI 和 Anthropic 两种格式
        JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
        AIResponse result = new AIResponse();

        // 尝试 OpenAI 格式: choices[0].message.content
        JsonArray choices = jsonResponse.getAsJsonArray("choices");
        if (choices != null && choices.size() > 0) {
            JsonObject msg = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (msg != null) {
                result.text = msg.get("content") != null ? msg.get("content").getAsString() : null;
            }
        }

        // Anthropic 格式: content[] 遍历
        if (result.text == null) {
            JsonArray content = jsonResponse.getAsJsonArray("content");
            if (content != null && content.size() > 0) {
                for (int i = 0; i < content.size(); i++) {
                    JsonObject block = content.get(i).getAsJsonObject();
                    String type = block.has("type") ? block.get("type").getAsString() : "";
                    if ("text".equals(type)) {
                        result.text = block.has("text") ? block.get("text").getAsString() : null;
                    } else if ("thinking".equals(type)) {
                        // thinking 块不提取——跳过
                    } else if ("tool_use".equals(type)) {
                        String toolName = block.has("name") ? block.get("name").getAsString() : "unknown";
                        String toolInput = block.has("input") ? block.get("input").toString() : "{}";
                        result.toolCalls.add(toolName);
                        result.toolInputs.add(toolInput);
                        log.info("AI 工具调用: " + toolName + " args=" + toolInput);
                    }
                }
            }
        }

        if (result.text == null) {
            // 没有 text 块时，检查是否只有 thinking（DeepSeek 偶尔不返回 text）
            JsonArray content = jsonResponse.getAsJsonArray("content");
            if (content != null) {
                for (int i = 0; i < content.size(); i++) {
                    JsonObject block = content.get(i).getAsJsonObject();
                    if ("thinking".equals(block.has("type") ? block.get("type").getAsString() : "")) {
                        String th = block.has("thinking") ? block.get("thinking").getAsString() : "";
                        // 只取前500字符，避免乱码
                        result.text = th.length() > 500 ? th.substring(0, 500) + "..." : th;
                        break;
                    }
                }
            }
        }
        if (result.text == null && result.toolCalls.isEmpty()) {
            throw new RuntimeException("API 响应无内容");
        }

        log.fine("AI 回复: " + result.text + " tools: " + result.toolCalls);
        if (result.text != null) {
            result.text = result.text.replaceFirst("(?s)^\\s*<think>.*?</think>\\s*", "").trim();
            if (result.text.isEmpty() && result.toolCalls.isEmpty()) {
                throw new RuntimeException("模型只返回了思考过程，没有最终回答");
            }
        }
        if (roleStyleGuard && systemPromptOverride == null
                && result.text != null
                && looksLikeGenericAssistant(result.text)) {
            log.info("检测到通用助手式语气，执行一次角色纠偏重写");
            String correction = systemPrompt
                    + "\n\n【输出校准】保留候选回复中的有效信息，按核心人格自然重写。"
                    + "去掉通用客服套话，保留具体的兴趣、判断、实际关照和对象差异。"
                    + "不要为了显得冷酷而强制训斥、反问、否认情感或称人样本。"
                    + "特别保留潘塔罗涅的私人分量与对意外事物的兴趣。只输出回复。";
            String rewritePrompt = "旅行者原话：" + userMessage
                    + "\n不合格候选回复：" + result.text
                    + "\n请只输出纠偏后的角色回复。";
            return sendMessage(rewritePrompt, new ConversationHistory(1), correction, false);
        }
        return result;
    }

    private boolean isEmotionallyLoaded(String text) {
        String[] markers = {
                "难过", "伤心", "害怕", "担心", "生病", "睡不着", "没用",
                "失败", "放弃", "孤独", "想你", "离开", "安慰", "关心", "喜欢", "爱"
        };
        for (String marker : markers) {
            if (text.contains(marker)) return true;
        }
        return false;
    }

    private boolean looksLikeGenericAssistant(String text) {
        String[] markers = {
                "我理解你的感受", "我会陪着你", "我一直都在", "你已经很棒",
                "照顾好自己", "随时可以找我", "你并不孤单", "很抱歉",
                "我很遗憾", "我能帮你", "帮助你", "你愿意分享",
                "请告诉我", "我们可以讨论", "找到应对的方法", "相信自己",
                "提供支持", "更安心", "需要帮助", "我会尽量",
                "让我们看看", "告诉我具体需求", "共同改进", "一起讨论",
                "你比你自己想象的要强大", "你比自己想象的更强大",
                "别担心", "继续前进", "相信你自己", "不断优化自我"
        };
        for (String marker : markers) {
            if (text.contains(marker)) return true;
        }
        return false;
    }

    /**
     * AI 调用回调接口。
     */
    public interface ChatCallback {
        void onSuccess(AIResponse response);
        void onError(Throwable error);
    }
}
