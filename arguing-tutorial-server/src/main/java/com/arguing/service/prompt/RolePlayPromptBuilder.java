package com.arguing.service.prompt;

import com.arguing.entity.Round;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色扮演对话 Prompt 构建器。
 * 构建 System Prompt 和对话历史消息，用于调用大模型进行角色扮演对练。
 */
@Component
public class RolePlayPromptBuilder {

    /**
     * 构建完整的角色扮演对话消息列表。
     *
     * @param personality      角色性格描述
     * @param totalRounds      总轮次
     * @param currentRound     当前轮次
     * @param historyRounds    历史轮次记录
     * @param currentUserText  当前用户输入文本
     * @return 消息列表，可直接传给 AiService.chat()
     */
    public List<Map<String, String>> build(String personality,
                                           int totalRounds,
                                           int currentRound,
                                           List<Round> historyRounds,
                                           String currentUserText) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 1. System Prompt
        int remaining = totalRounds - currentRound;
        String systemPrompt = buildSystemPrompt(personality, remaining);
        messages.add(message("system", systemPrompt));

        // 2. 将历史轮次转为 messages 数组（user_text -> user, ai_text -> assistant）
        for (Round round : historyRounds) {
            if (round.getUserText() != null && !round.getUserText().isEmpty()) {
                messages.add(message("user", round.getUserText()));
            }
            if (round.getAiText() != null && !round.getAiText().isEmpty()) {
                messages.add(message("assistant", round.getAiText()));
            }
        }

        // 3. 附加当前轮次的用户输入
        if (currentUserText != null && !currentUserText.isEmpty()) {
            messages.add(message("user", currentUserText));
        }

        return messages;
    }

    /**
     * 构建 System Prompt。
     */
    private String buildSystemPrompt(String personality, int remaining) {
        return "你是一个角色扮演AI，正在扮演" + personality + "的对手。"
                + "规则："
                + "1.不生成有害内容 "
                + "2.在第" + remaining + "轮后推动结论 "
                + "3.回复JSON格式: {\"reply\":\"...\",\"emotion\":\"angry|sarcastic|hesitant|compromising|confident|neutral\"}";
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }
}
