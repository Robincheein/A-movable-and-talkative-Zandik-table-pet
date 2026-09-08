package com.group_finity.mascot.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话历史管理。
 * 存储用户和 AI 的消息记录，支持导出为 API 需要的格式。
 */
public class ConversationHistory {

    /** 发送给模型的最大上下文条数；UI 会保留本次程序运行期间的全部消息。 */
    private final int maxHistory;

    /** 消息列表 */
    private final List<Message> messages = new ArrayList<>();

    public ConversationHistory(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    /** 添加用户消息 */
    public void addUser(String text) {
        messages.add(new Message("user", text));
        trim();
    }

    /** 添加 AI 回复 */
    public void addAssistant(String text) {
        messages.add(new Message("assistant", text));
        trim();
    }

    /** 添加系统消息 */
    public void addSystem(String text) {
        messages.add(new Message("system", text));
        trim();
    }

    /** UI 历史不裁剪；模型请求在 toApiFormat 中只取最近的消息。 */
    private void trim() {
    }

    /** 获取消息列表 */
    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    /** 转成 API 兼容格式: [{role, content}, ...] */
    public List<Map<String, String>> toApiFormat() {
        List<Map<String, String>> result = new ArrayList<>();
        int start = Math.max(0, messages.size() - maxHistory);
        for (int i = start; i < messages.size(); i++) {
            Message msg = messages.get(i);
            Map<String, String> map = new HashMap<>();
            map.put("role", msg.role);
            map.put("content", msg.content);
            result.add(map);
        }
        return result;
    }

    /** 移除最后一条 user 消息（用于清理自主发言的提示词） */
    public void removeLastUser() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).role)) {
                messages.remove(i);
                return;
            }
        }
    }

    /** 当前历史是否已经以这条用户消息结尾，用于避免 API 请求重复追加当前输入。 */
    public boolean endsWithUserMessage(String text) {
        if (messages.isEmpty()) return false;
        Message last = messages.get(messages.size() - 1);
        return "user".equals(last.role) && last.content.equals(text);
    }

    /** 是否包含用户实际输入。 */
    public boolean hasUserMessage() {
        for (Message message : messages) {
            if ("user".equals(message.role)) return true;
        }
        return false;
    }

    /** 清空历史 */
    public void clear() {
        messages.clear();
    }

    /** 获取当前消息数量 */
    public int size() {
        return messages.size();
    }

    /** 对话消息 */
    public static class Message {
        public final String role;
        public final String content;

        Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
