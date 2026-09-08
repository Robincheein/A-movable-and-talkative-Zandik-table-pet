package com.group_finity.mascot.chat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Minimal end-to-end check for the local Ollama backend. */
public final class LocalOllamaSelfTest {
    private LocalOllamaSelfTest() {
    }

    public static void main(String[] args) {
        String endpoint = args.length > 0
                ? args[0]
                : "http://127.0.0.1:11434/v1/chat/completions";
        String model = args.length > 1 ? args[1] : "qwen2.5:7b";

        AIClient client = new AIClient("ollama", endpoint, model);
        client.setTimeouts(5000, 180000);
        client.setRAGEnabled(false);
        client.setToolsEnabled(false);

        AIClient.AIResponse chat = client.sendMessageDirect(
                "只回复这六个字，不要解释：本地连接成功", new ConversationHistory(6));
        require(chat != null && chat.text != null && !chat.text.trim().isEmpty(),
                "Ollama did not return a chat response");
        System.out.println("OLLAMA_CHAT_OK=" + chat.text.trim());

        String prompt = "请从这段对话中抽取长期记忆：用户说‘我只接受简体中文回复。’。"
                + "严格返回这个结构：{\"memories\":[{\"category\":\"preference\","
                + "\"memory_key\":\"reply_language\",\"memory_value\":\"简体中文\","
                + "\"importance\":4,\"confidence\":1.0}]}";
        AIClient.AIResponse memory = client.sendStructuredMemoryDirect(prompt);
        require(memory != null && memory.text != null, "No structured memory response");
        JsonObject json = JsonParser.parseString(memory.text).getAsJsonObject();
        require(json.has("memories") && json.getAsJsonArray("memories").size() > 0,
                "Structured memory JSON has no memories: " + memory.text);
        System.out.println("OLLAMA_MEMORY_JSON_OK=" + memory.text.trim());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
