package com.arguing.service.prompt;

import com.arguing.entity.Round;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 策略提示 Prompt 构建器。
 * 基于当前对话上下文构建策略提示 Prompt，要求 AI 返回建议策略名称和一句话说明。
 * 格式: "策略名：说明"（纯文本，不是JSON）
 */
@Component
public class HintPromptBuilder {

    /**
     * 构建策略提示消息列表。
     *
     * @param historyRounds 历史轮次记录
     * @param sceneName     场景名称
     * @return 消息列表，传给 AiService.chat() 后返回 "策略名：说明" 格式的文本
     */
    public List<Map<String, String>> build(List<Round> historyRounds, String sceneName) {
        List<Map<String, String>> messages = new ArrayList<>();

        // System Prompt：指导 AI 以简短格式返回策略建议
        String systemPrompt = "你是一个辩论策略顾问。"
                + "根据当前辩论进展，给出一个具体的辩论策略建议。"
                + "只回复纯文本，格式为：策略名：一句话说明。"
                + "不要回复 JSON，不要加多余的解释。"
                + "示例：数据举证：用具体数字支撑你的论点";
        messages.add(message("system", systemPrompt));

        // 附加对话历史作为上下文
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("当前辩论场景：").append(sceneName).append("\n\n");
        contextBuilder.append("对话历史：\n");

        for (Round round : historyRounds) {
            if (round.getUserText() != null && !round.getUserText().isEmpty()) {
                contextBuilder.append("用户：").append(round.getUserText()).append("\n");
            }
            if (round.getAiText() != null && !round.getAiText().isEmpty()) {
                contextBuilder.append("对手：").append(round.getAiText()).append("\n");
            }
        }

        contextBuilder.append("\n请给出下一步的辩论策略建议：");
        messages.add(message("user", contextBuilder.toString()));

        return messages;
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }
}
