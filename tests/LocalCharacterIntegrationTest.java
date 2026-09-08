package com.group_finity.mascot.chat;

import java.io.File;
import java.lang.reflect.Field;

/** Checks the published configuration, character RAG, and SQLite memory path together. */
public final class LocalCharacterIntegrationTest {
    private LocalCharacterIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        File baseDir = new File(args.length > 0 ? args[0] : "dist");
        ChatManager manager = ChatManager.getInstance();
        manager.init(baseDir);

        Field field = ChatManager.class.getDeclaredField("aiClient");
        field.setAccessible(true);
        AIClient client = (AIClient) field.get(manager);
        AIClient.AIResponse response = client.sendMessageDirect(
                "用一句简体中文介绍你自己，不要提及AI、模型或提示词。",
                new ConversationHistory(6));
        if (response == null || response.text == null || response.text.trim().isEmpty()) {
            throw new IllegalStateException("Local character conversation returned no text");
        }
        System.out.println("LOCAL_CHARACTER_OK=" + response.text.trim());

        LongTermMemoryStore store = manager.getMemoryStore();
        if (store == null || !store.isAvailable()) {
            throw new IllegalStateException("SQLite memory is not available");
        }
        System.out.println("LOCAL_MEMORY_OK=" + store.getJdbcUrl());
        store.close();
    }
}
