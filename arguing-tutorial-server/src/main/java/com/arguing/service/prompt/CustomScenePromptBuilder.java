package com.arguing.service.prompt;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义场景生成 Prompt 构建器。
 * 根据用户提供的场景名称、描述和对手描述，
 * 构建 Prompt 让 LLM 生成 personality、opening_line、difficulty。
 */
@Component
public class CustomScenePromptBuilder {

    /**
     * 构建自定义场景生成消息列表。
     *
     * @param name               场景名称
     * @param description        场景描述
     * @param opponentDescription 对手描述
     * @return 消息列表，传给 AiService.chat() 后返回 JSON 格式结果
     */
    public List<Map<String, String>> build(String name,
                                           String description,
                                           String opponentDescription) {
        List<Map<String, String>> messages = new ArrayList<>();

        String systemPrompt = buildSystemPrompt();
        messages.add(message("system", systemPrompt));

        String userPrompt = buildUserPrompt(name, description, opponentDescription);
        messages.add(message("user", userPrompt));

        return messages;
    }

    private String buildSystemPrompt() {
        return "你是一个辩论对练场景设计专家。"
                + "根据用户提供的场景信息，生成对手的角色设定。"
                + "你必须只返回纯 JSON，不要包含任何其他文字、解释或 markdown 代码块标记。"
                + "\n\n返回格式：\n"
                + "{\"personality\": \"角色性格描述（50-100字，描述对手的性格、说话风格、常用策略）\","
                + "\"opening_line\": \"开场白（对手的第一句话，要有代入感，20-50字）\","
                + "\"difficulty\": 难度等级（1-5的整数，1最简单，5最难）}";
    }

    private String buildUserPrompt(String name, String description, String opponentDescription) {
        return "场景名称：" + name + "\n"
                + "场景描述：" + description + "\n"
                + "对手描述：" + opponentDescription + "\n\n"
                + "请为这个场景设计对手角色，直接返回 JSON：";
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }
}
