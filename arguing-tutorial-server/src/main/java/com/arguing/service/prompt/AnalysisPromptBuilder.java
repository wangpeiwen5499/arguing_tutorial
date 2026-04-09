package com.arguing.service.prompt;

import com.arguing.entity.Round;
import com.arguing.entity.Scene;
import com.arguing.entity.Session;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 复盘分析 Prompt 构建器。
 * 将完整对话历史 + 场景信息 + 评分标准喂给 LLM，要求返回结构化 JSON 分析结果。
 */
@Component
public class AnalysisPromptBuilder {

    /**
     * 构建复盘分析消息列表。
     *
     * @param session 完成的会话
     * @param scene   场景信息
     * @param rounds  所有轮次记录
     * @return 消息列表，传给 AiService.chat() 后返回结构化 JSON
     */
    public List<Map<String, String>> build(Session session, Scene scene, List<Round> rounds) {
        List<Map<String, String>> messages = new ArrayList<>();

        // System Prompt
        String systemPrompt = buildSystemPrompt();
        messages.add(message("system", systemPrompt));

        // 用户消息：包含场景信息、评分标准和完整对话历史
        String userPrompt = buildUserPrompt(session, scene, rounds);
        messages.add(message("user", userPrompt));

        return messages;
    }

    /**
     * 构建 System Prompt：定义评分维度和输出格式要求。
     */
    private String buildSystemPrompt() {
        return "你是一个专业的辩论分析教练。你的任务是对用户的辩论表现进行全面复盘分析。"
                + "你需要严格按照以下五个维度进行评分（0-100分），并给出具体的评语：\n\n"
                + "1. logic_score（逻辑性，权重25%）：论点是否有逻辑支撑，是否存在逻辑漏洞\n"
                + "2. emotion_score（情绪控制，权重20%）：是否保持冷静，有无情绪化表达\n"
                + "3. persuasion_score（说服力，权重25%）：论据是否充分，能否打动对方\n"
                + "4. strategy_score（策略运用，权重15%）：是否灵活运用谈判策略\n"
                + "5. clarity_score（表达清晰度，权重15%）：表达是否简洁清晰\n\n"
                + "请严格按照以下 JSON 格式返回，不要包含任何其他文字说明：\n"
                + "{\n"
                + "  \"logic_score\": 75,\n"
                + "  \"emotion_score\": 60,\n"
                + "  \"persuasion_score\": 80,\n"
                + "  \"strategy_score\": 65,\n"
                + "  \"clarity_score\": 85,\n"
                + "  \"strengths\": [\"优势1\", \"优势2\"],\n"
                + "  \"improvements\": [\"改进建议1\", \"改进建议2\"],\n"
                + "  \"round_reviews\": [\n"
                + "    {\"round\": 1, \"comment\": \"该轮次评语\", \"score\": 70},\n"
                + "    {\"round\": 2, \"comment\": \"该轮次评语\", \"score\": 75}\n"
                + "  ]\n"
                + "}\n\n"
                + "要求：\n"
                + "- strengths 列出 2-4 个用户表现好的方面\n"
                + "- improvements 列出 2-4 个可以改进的方面\n"
                + "- round_reviews 对每个有用户发言的轮次给出评语和分数（0-100）\n"
                + "- 所有评语要具体、有针对性，不要泛泛而谈\n"
                + "- 只返回纯 JSON，不要用 markdown 代码块包裹";
    }

    /**
     * 构建用户消息：包含场景信息、对话历史和提示使用情况。
     */
    private String buildUserPrompt(Session session, Scene scene, List<Round> rounds) {
        StringBuilder sb = new StringBuilder();

        // 场景信息
        sb.append("【辩论场景】\n");
        sb.append("场景名称：").append(scene.getName()).append("\n");
        sb.append("场景描述：").append(scene.getDescription() != null ? scene.getDescription() : "无").append("\n");
        if (scene.getPersonality() != null) {
            sb.append("对手性格：").append(scene.getPersonality()).append("\n");
        }
        sb.append("总轮次：").append(session.getTotalRounds()).append("\n");
        sb.append("使用提示次数：").append(session.getHintUsedCount()).append("\n\n");

        // 对话历史
        sb.append("【完整对话历史】\n");
        for (Round round : rounds) {
            int roundNum = round.getRoundNumber();
            if (round.getUserText() != null && !round.getUserText().isEmpty()) {
                sb.append("第").append(roundNum).append("轮 - 用户：").append(round.getUserText()).append("\n");
            }
            if (round.getAiText() != null && !round.getAiText().isEmpty()) {
                sb.append("第").append(roundNum).append("轮 - 对手：").append(round.getAiText()).append("\n");
            }
        }

        sb.append("\n请对用户的表现进行全面复盘分析，严格按照 JSON 格式返回评分和评语。");
        return sb.toString();
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }
}
