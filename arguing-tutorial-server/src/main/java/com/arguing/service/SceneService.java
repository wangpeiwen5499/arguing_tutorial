package com.arguing.service;

import com.arguing.entity.Scene;
import com.arguing.exception.ApiException;
import com.arguing.repository.SceneRepository;
import com.arguing.service.prompt.CustomScenePromptBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SceneService {

    private static final Logger log = LoggerFactory.getLogger(SceneService.class);

    /** 对手描述关键词 -> 预制 avatar_config 标签映射 */
    private static final Map<String, String> AVATAR_KEYWORD_MAP = new LinkedHashMap<>();

    static {
        AVATAR_KEYWORD_MAP.put("强势", "boss_01");
        AVATAR_KEYWORD_MAP.put("年轻", "pm_01");
        AVATAR_KEYWORD_MAP.put("技术", "dev_01");
        AVATAR_KEYWORD_MAP.put("老练", "manager_01");
        AVATAR_KEYWORD_MAP.put("固执", "dev_02");
    }

    private static final String DEFAULT_AVATAR = "dev_01";

    private final SceneRepository sceneRepository;
    private final AiService aiService;
    private final CustomScenePromptBuilder customScenePromptBuilder;
    private final ObjectMapper objectMapper;

    public SceneService(SceneRepository sceneRepository,
                        AiService aiService,
                        CustomScenePromptBuilder customScenePromptBuilder,
                        ObjectMapper objectMapper) {
        this.sceneRepository = sceneRepository;
        this.aiService = aiService;
        this.customScenePromptBuilder = customScenePromptBuilder;
        this.objectMapper = objectMapper;
    }

    public List<Scene> listScenes(String category) {
        if (category != null && !category.isEmpty()) {
            return sceneRepository.findByCategory(category);
        }
        return sceneRepository.findAll();
    }

    public Scene getScene(Long id) {
        return sceneRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "场景不存在"));
    }

    /**
     * 创建自定义场景。
     * 1. 调用 LLM 生成 personality、opening_line、difficulty
     * 2. 根据 opponentDescription 关键词匹配预制 avatar_config
     * 3. 设置默认 evaluation_criteria 和 background_config
     * 4. 保存并返回 Scene
     */
    public Scene createCustomScene(String name, String description, String opponentDescription) {
        // 1. 调用 LLM 生成场景设定
        SceneAiResult aiResult = callAiForSceneGeneration(name, description, opponentDescription);

        // 2. 关键词匹配 avatar_config
        String avatarConfig = matchAvatarConfig(opponentDescription);

        // 3. 构建 Scene 实体
        Scene scene = new Scene();
        scene.setName(name);
        scene.setDescription(description);
        scene.setCategory("custom");
        scene.setDifficulty(aiResult.difficulty);
        scene.setPersonality(aiResult.personality);
        scene.setOpeningLine(aiResult.openingLine);
        scene.setEvaluationCriteria(null); // 使用默认权重
        scene.setBackgroundConfig("{\"type\":\"custom\",\"bgColor\":\"#1a1a2e\"}");
        scene.setAvatarConfig(avatarConfig);
        scene.setCreatedAt(LocalDateTime.now());
        scene.setUpdatedAt(LocalDateTime.now());

        // 4. 保存并返回
        return sceneRepository.save(scene);
    }

    /**
     * 调用 AI 生成场景的 personality、opening_line、difficulty。
     */
    private SceneAiResult callAiForSceneGeneration(String name, String description, String opponentDescription) {
        List<Map<String, String>> messages = customScenePromptBuilder.build(name, description, opponentDescription);
        String aiResponse = aiService.chat(messages);

        try {
            // 清理可能的 markdown 代码块标记
            String json = aiResponse.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }

            JsonNode root = objectMapper.readTree(json);
            String personality = root.has("personality") ? root.get("personality").asText() : "性格不明";
            String openingLine = root.has("opening_line") ? root.get("opening_line").asText() : "你好，让我们开始吧。";
            int difficulty = root.has("difficulty") ? Math.max(1, Math.min(5, root.get("difficulty").asInt())) : 3;

            return new SceneAiResult(personality, openingLine, difficulty);
        } catch (Exception e) {
            log.warn("解析 AI 场景生成结果失败，使用默认值: {}", e.getMessage());
            return new SceneAiResult(
                    "性格强硬但讲道理，善于用数据反驳",
                    "你好，听说你有不同意见？说来听听。",
                    3
            );
        }
    }

    /**
     * 根据 opponentDescription 中的关键词匹配预制 avatar_config 标签。
     */
    private String matchAvatarConfig(String opponentDescription) {
        if (opponentDescription == null || opponentDescription.isEmpty()) {
            return DEFAULT_AVATAR;
        }
        for (Map.Entry<String, String> entry : AVATAR_KEYWORD_MAP.entrySet()) {
            if (opponentDescription.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return DEFAULT_AVATAR;
    }

    /**
     * AI 场景生成结果的内部记录。
     */
    private record SceneAiResult(String personality, String openingLine, int difficulty) {
    }
}
