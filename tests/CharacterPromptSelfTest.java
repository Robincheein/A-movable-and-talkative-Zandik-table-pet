package com.group_finity.mascot.chat;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises the actual outbound API payload without contacting a model or SQLite. */
public final class CharacterPromptSelfTest {
    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        File base = new File(args.length == 0 ? "." : args[0]);
        CharacterLoader loader = new CharacterLoader(new File(base, "character"));
        String core = loader.loadCorePrompt();
        check(core.contains("潘塔罗涅") && core.contains("不是唯一话题"), "missing core dimensions");
        check(core.contains("原创语气校准"), "examples not always on");
        check(!core.contains("你是 35 岁的切片"), "legacy identity leaked");
        String fallback = loader.loadSystemPrompt();
        check(!fallback.contains("本地模型角色锁"), "legacy lock leaked");
        TFIDFIndex index = new TFIDFIndex(new File(base, "character/rag_index.json"));
        index.buildIndex(Arrays.asList(
                new ChunkSplitter.Chunk("1", "sources/lore.txt", "潘塔罗涅 富人 费奥潘 交涉 合作 剧情证据标记"),
                new ChunkSplitter.Chunk("2", "analysis/interpretation.txt", "潘塔罗涅 富人 在意 关系解析标记"),
                new ChunkSplitter.Chunk("3", "sources/source_fandom_dottore_2026-08-10.txt", "潘塔罗涅 旧未核实标记"),
                new ChunkSplitter.Chunk("4", "memory/secret.txt", "潘塔罗涅 不应检索标记")), "fixture");
        RAGPromptBuilder builder = new RAGPromptBuilder(index, core, loader.loadSpecificFile("rules.txt"), 4, 0.01);
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> mockResponse = new AtomicReference<>("{\"content\":[{\"type\":\"text\",\"text\":\"测试回复。\"}]}");
        java.util.concurrent.atomic.AtomicInteger requestCount = new java.util.concurrent.atomic.AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/anthropic/v1/messages", exchange -> {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buf = new byte[4096]; int n;
            while ((n = exchange.getRequestBody().read(buf)) != -1) bytes.write(buf, 0, n);
            requestBody.set(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            requestCount.incrementAndGet();
            String data = requestBody.get().contains("不合格候选回复")
                    ? "{\"content\":[{\"type\":\"text\",\"text\":\"这件事倒出乎我的意料。\"}]}" : mockResponse.get();
            byte[] response = data.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            AIClient client = new AIClient("test-key", "http://127.0.0.1:" + server.getAddress().getPort() + "/anthropic/v1/messages", "fixture");
            client.setToolsEnabled(false);
            client.setRAGPromptBuilder(builder);
            ConversationHistory history = new ConversationHistory(8);
            history.addUser("说说潘塔罗涅");
            history.addAssistant("previous invented detail");
            history.addUser("那他呢？");
            AIClient.AIResponse reply = client.sendMessageDirect("那他呢？", history);
            check(reply != null, "no mock response");
            JsonObject payload = JsonParser.parseString(requestBody.get()).getAsJsonObject();
            String prompt = payload.get("system").getAsString();
            check(prompt.contains("剧情证据标记"), "follow-up lost lore retrieval");
            check(prompt.contains("关系解析标记"), "analysis channel missing");
            check(!prompt.contains("旧未核实标记") && !prompt.contains("不应检索标记"), "excluded source leaked");
            check(prompt.contains("推论，不是官方定论"), "evidence type missing");
            check(payload.getAsJsonArray("messages").size() == 3, "duplicated user message");
            check(prompt.contains("【最终回复语言】") && prompt.contains("英文参考资料必须消化后用中文表达"), "language rule missing");
            String ordinary = builder.buildSystemPrompt("今天早餐不错");
            check(ordinary.contains("不能被普通出资人替代"), "core absent on unrelated topic");
            check(!RAGPromptBuilder.retrievalQuery("那他呢？", history.toApiFormat()).contains("invented"), "assistant inventions used in retrieval");
            check(RAGPromptBuilder.retrievalQuery("我们换个话题聊食物", history.toApiFormat()).indexOf("潘塔罗涅") < 0, "old topic contaminates explicit switch");
            check(!AIClient.needsChineseRewrite("聊聊代码", "使用 `hello world example`，CUDA 和 API。https://example.com"), "technical text falsely flagged");
            check(!AIClient.needsChineseRewrite("用英文回答", "This is an example."), "explicit language request ignored");
            check(AIClient.needsChineseRewrite("你好", "That is quite unexpected. 出乎意料。"), "English prose missed");
            mockResponse.set("{\"content\":[{\"type\":\"thinking\",\"thinking\":\"SECRET ENGLISH REASONING\"}]}");
            check(client.sendMessageDirect("你好", new ConversationHistory(1)) == null, "thinking leaked as final reply");
            mockResponse.set("{\"content\":[{\"type\":\"thinking\",\"thinking\":\"secret\"},{\"type\":\"text\",\"text\":\"第一段。\"},{\"type\":\"text\",\"text\":\"第二段。\"}]}");
            check("第一段。\n第二段。".equals(client.sendMessageDirect("你好", new ConversationHistory(1)).text), "text blocks dropped");
            mockResponse.set("{\"choices\":[{\"message\":{\"content\":null,\"reasoning_content\":\"secret\"}}]}");
            check(client.sendMessageDirect("你好", new ConversationHistory(1)) == null, "OpenAI null content mishandled");
            mockResponse.set("{\"content\":[{\"type\":\"text\",\"text\":\"That is quite unexpected.\"},{\"type\":\"tool_use\",\"name\":\"walk\",\"input\":{}}]}");
            int before = requestCount.get();
            // Exercise the raw response boundary, not the real tool executor.
            java.lang.reflect.Method send = AIClient.class.getDeclaredMethod("sendMessage", String.class,
                    ConversationHistory.class, String.class, boolean.class);
            send.setAccessible(true);
            AIClient.AIResponse corrected = (AIClient.AIResponse)send.invoke(client, "你好", new ConversationHistory(1), null, true);
            check("这件事倒出乎我的意料。".equals(corrected.text), "language correction missing");
            check(requestCount.get() == before + 2, "correction not bounded to one retry");
            check(corrected.toolCalls.size() == 1 && corrected.toolCalls.get(0).equals("walk"), "correction lost action");
        } finally { server.stop(0); }
        System.out.println("CHARACTER_PROMPT_OK");
    }
}
