package com.group_finity.mascot.chat;

import java.io.File;
import java.util.List;

/** Minimal Java 8 integration check for the SQLite long-term-memory store. */
public final class MemoryStoreSelfTest {
    public static void main(String[] args) throws Exception {
        File memoryDir = new File(args[0]);
        memoryDir.mkdirs();
        LongTermMemoryStore store = new LongTermMemoryStore(
                new File(memoryDir, "memory_test.db"));
        store.initialize(memoryDir);

        ConversationHistory history = new ConversationHistory(20);
        history.addUser("我正在完善视频转写工具，只需要简体中文输出。");
        history.addAssistant("我记住了。");
        List<ConversationHistory.Message> messages = history.getMessages();
        String first = store.saveSession("test_session", messages, null);
        String second = store.saveSession("test_session_copy", messages, null);
        if (!first.equals(second)) throw new AssertionError("session deduplication failed");

        String json = "{\"memories\":["
                + "{\"type\":\"project\",\"subject\":\"视频转写工具\","
                + "\"content\":\"用户正在完善视频转写工具。\",\"importance\":4,"
                + "\"confidence\":0.95,\"evidence\":\"我正在完善视频转写工具\"},"
                + "{\"type\":\"preference\",\"subject\":\"中文输出\","
                + "\"content\":\"用户只需要简体中文输出。\",\"importance\":5,"
                + "\"confidence\":1.0,\"evidence\":\"只需要简体中文输出\"}]}";
        int stored = store.storeCandidatesFromJson(json, first);
        if (stored != 2) throw new AssertionError("candidate storage failed: " + stored);
        String prompt = store.buildMemoryPrompt("视频工具的中文输出", 8);
        if (!prompt.contains("简体中文")) throw new AssertionError("memory retrieval failed");
        if (!store.buildProfileSnapshot().contains("视频转写工具"))
            throw new AssertionError("profile snapshot failed");

        System.out.println("MEMORY_STORE_OK");
        System.out.println("JDBC_URL=" + store.getJdbcUrl());
        store.close();
    }
}
