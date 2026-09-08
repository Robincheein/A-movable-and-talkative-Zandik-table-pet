package com.group_finity.mascot.chat;

import java.io.File;
import java.lang.reflect.Field;

/** Regression check for common prompts that make general assistants overly warm. */
public final class DottoreToneSelfTest {
    private static final String[] PROMPTS = {
            "我最近总是睡不着，你会担心吗？",
            "如果有一天我不再来找你呢？",
            "别人说我很没用，我也开始相信了。"
    };

    private static final String[] FORBIDDEN = {
            "我理解你的感受", "我会陪着你", "我一直都在", "抱抱",
            "你已经很棒", "照顾好自己", "随时可以找我", "作为AI",
            "我能帮你", "帮助你", "你愿意分享", "我们可以讨论", "找到应对的方法",
            "提供支持", "更安心", "需要帮助", "我会尽量", "让我们看看", "一起讨论",
            "你比你自己想象的要强大", "你比自己想象的更强大",
            "别担心", "继续前进", "相信你自己", "不断优化自我"
    };

    private DottoreToneSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        File baseDir = new File(args.length > 0 ? args[0] : "dist");
        ChatManager manager = ChatManager.getInstance();
        manager.init(baseDir);

        Field field = ChatManager.class.getDeclaredField("aiClient");
        field.setAccessible(true);
        AIClient client = (AIClient) field.get(manager);
        ConversationHistory history = new ConversationHistory(12);

        for (String prompt : PROMPTS) {
            history.addUser(prompt);
            AIClient.AIResponse response = client.sendMessageDirect(prompt, history);
            if (response == null || response.text == null || response.text.trim().isEmpty()) {
                throw new IllegalStateException("No response for: " + prompt);
            }
            String text = response.text.trim();
            for (String forbidden : FORBIDDEN) {
                if (text.contains(forbidden)) {
                    throw new IllegalStateException("Overly warm phrase '" + forbidden
                            + "' in response: " + text);
                }
            }
            history.addAssistant(text);
            System.out.println("PROMPT=" + prompt);
            System.out.println("RESPONSE=" + text);
        }

        LongTermMemoryStore store = manager.getMemoryStore();
        if (store != null) store.close();
        System.out.println("DOTTORE_TONE_OK");
    }
}
