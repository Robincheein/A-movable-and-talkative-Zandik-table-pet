package com.group_finity.mascot.chat;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;

/** Explicit bounded live preview; no tools, user history, or memory writes. */
public final class CharacterLivePreview {
    public static void main(String[] args) throws Exception {
        File root = new File(".");
        Properties config = new Properties();
        try (InputStream in = new FileInputStream("conf/chat_config.properties")) { config.load(in); }
        CharacterLoader loader = new CharacterLoader(new File(root, "character"));
        TFIDFIndex index = new TFIDFIndex(new File(root, "character/rag_index.json"));
        if (!index.loadFromFile()) throw new IOException("Rebuild the index first");
        AIClient client = new AIClient(config.getProperty("api.key"), config.getProperty("api.endpoint"), config.getProperty("api.model"));
        client.setGenerationSettings(0.65, 0.9, 1000);
        client.setTimeouts(15000, 60000);
        client.setToolsEnabled(false);
        client.setRAGPromptBuilder(new RAGPromptBuilder(index, loader.loadCorePrompt(), loader.loadSpecificFile("rules.txt"), 4, 0.05));
        String[] prompts = {
            "我想给你讲个没有科学道理的小故事，你会不会觉得无聊？",
            "潘塔罗涅对你来说，只是一个给实验出钱的人吗？",
            "如果潘塔罗涅隐瞒身体不舒服，你会怎么办？",
            "有人解析说你的所有计划都是为了逃离命运，这就是官方已经确定的答案吗？"
        };
        StringBuilder report = new StringBuilder("# 角色修改后实测样例\n\n模型配置：" + config.getProperty("api.model") + "\n\n");
        for (String prompt : prompts) {
            AIClient.AIResponse reply = client.sendMessageDirect(prompt, new ConversationHistory(8));
            if (reply == null || reply.text == null || reply.text.trim().isEmpty())
                throw new IOException("Live preview failed; see log, no memory was modified");
            report.append("旅行者：").append(prompt).append("\n\n多托雷：").append(reply.text).append("\n\n");
            Files.write(Paths.get("docs/character_live_preview.md"), report.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("PREVIEW_COMPLETED");
        }
    }
}
