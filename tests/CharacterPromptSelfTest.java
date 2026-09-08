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
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/anthropic/v1/messages", exchange -> {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buf = new byte[4096]; int n;
            while ((n = exchange.getRequestBody().read(buf)) != -1) bytes.write(buf, 0, n);
            requestBody.set(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            byte[] response = "{\"content\":[{\"type\":\"text\",\"text\":\"test reply\"}]}".getBytes(StandardCharsets.UTF_8);
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
            String ordinary = builder.buildSystemPrompt("今天早餐不错");
            check(ordinary.contains("不能被普通出资人替代"), "core absent on unrelated topic");
            check(!RAGPromptBuilder.retrievalQuery("那他呢？", history.toApiFormat()).contains("invented"), "assistant inventions used in retrieval");
            check(RAGPromptBuilder.retrievalQuery("我们换个话题聊食物", history.toApiFormat()).indexOf("潘塔罗涅") < 0, "old topic contaminates explicit switch");
        } finally { server.stop(0); }
        System.out.println("CHARACTER_PROMPT_OK");
    }
}
