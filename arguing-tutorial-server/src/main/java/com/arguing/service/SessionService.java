package com.arguing.service;

import com.arguing.dto.ChatResponse;
import com.arguing.entity.Round;
import com.arguing.entity.Scene;
import com.arguing.entity.Session;
import com.arguing.exception.ApiException;
import com.arguing.repository.RoundRepository;
import com.arguing.repository.SceneRepository;
import com.arguing.repository.SessionRepository;
import com.arguing.service.prompt.HintPromptBuilder;
import com.arguing.service.prompt.RolePlayPromptBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    /** 每次会话最多可使用的提示次数 */
    private static final int MAX_HINT_COUNT = 3;

    /** 音频临时存储路径 */
    private static final String AUDIO_TMP_DIR = System.getProperty("java.io.tmpdir") + "/arguing-tutorial/audio";

    private final SessionRepository sessionRepository;
    private final RoundRepository roundRepository;
    private final SceneRepository sceneRepository;
    private final AiService aiService;
    private final SpeechService speechService;
    private final ContentSafetyService contentSafetyService;
    private final RolePlayPromptBuilder rolePlayPromptBuilder;
    private final HintPromptBuilder hintPromptBuilder;
    private final ObjectMapper objectMapper;

    public SessionService(SessionRepository sessionRepository,
                          RoundRepository roundRepository,
                          SceneRepository sceneRepository,
                          AiService aiService,
                          SpeechService speechService,
                          ContentSafetyService contentSafetyService,
                          RolePlayPromptBuilder rolePlayPromptBuilder,
                          HintPromptBuilder hintPromptBuilder,
                          ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.roundRepository = roundRepository;
        this.sceneRepository = sceneRepository;
        this.aiService = aiService;
        this.speechService = speechService;
        this.contentSafetyService = contentSafetyService;
        this.rolePlayPromptBuilder = rolePlayPromptBuilder;
        this.hintPromptBuilder = hintPromptBuilder;
        this.objectMapper = objectMapper;
    }

    /**
     * 开始会话的返回结果，包含 sessionId 和 ChatResponse。
     */
    public static class SessionStartResult {
        private final Long sessionId;
        private final ChatResponse response;

        public SessionStartResult(Long sessionId, ChatResponse response) {
            this.sessionId = sessionId;
            this.response = response;
        }

        public Long getSessionId() { return sessionId; }
        public ChatResponse getResponse() { return response; }
    }

    /**
     * 开始对练会话。
     * 1. 校验场景存在
     * 2. 创建 Session
     * 3. 创建 Round 0 作为 AI 开场白
     * 4. 返回 SessionStartResult 包含 sessionId 和开场白
     */
    @Transactional
    public SessionStartResult startSession(Long userId, Long sceneId) {
        // 1. 校验场景存在
        Scene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "场景不存在"));

        // 2. 创建 Session
        Session session = new Session();
        session.setUserId(userId);
        session.setSceneId(sceneId);
        session.setStatus(Session.SessionStatus.ACTIVE);
        session.setTotalRounds(10);
        session.setCurrentRound(0);
        session.setHintUsedCount(0);
        session.setCreatedAt(LocalDateTime.now());
        session = sessionRepository.save(session);

        // 3. 创建 Round 0（AI 开场白）
        String openingLine = scene.getOpeningLine();
        if (openingLine == null || openingLine.isEmpty()) {
            openingLine = "你好，让我们开始今天的辩论练习吧。";
        }

        Round round0 = new Round();
        round0.setSessionId(session.getId());
        round0.setRoundNumber(0);
        round0.setAiText(openingLine);
        round0.setAiEmotion("neutral");
        round0.setCreatedAt(LocalDateTime.now());
        roundRepository.save(round0);

        log.info("用户 {} 开始会话 {}，场景 {}", userId, session.getId(), sceneId);

        // 4. 返回 SessionStartResult
        ChatResponse response = new ChatResponse();
        response.setText(openingLine);
        response.setAudioUrl(null);
        response.setEmotion("neutral");
        response.setExpression(null);
        response.setCurrentRound(0);
        response.setTotalRounds(session.getTotalRounds());
        return new SessionStartResult(session.getId(), response);
    }

    /**
     * 发送语音对练。
     * 1. 校验 Session 存在且 ACTIVE
     * 2. 校验 currentRound < totalRounds
     * 3. 保存音频文件到临时路径
     * 4. ASR 语音转文字
     * 5. 敏感词过滤
     * 6. 构建角色扮演 Prompt
     * 7. 调用 AI 获取回复
     * 8. 解析 AI 回复 JSON（reply + emotion）
     * 9. 审核 AI 输出安全性
     * 10. TTS 生成语音
     * 11. 保存 Round，返回 ChatResponse
     */
    @Transactional
    public ChatResponse chat(Long userId, Long sessionId, MultipartFile audioFile) {
        // 1. 校验 Session 存在且 ACTIVE
        Session session = findActiveSession(sessionId, userId);

        // 2. 校验轮次
        if (session.getCurrentRound() >= session.getTotalRounds()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "对练已结束，不能再发送消息");
        }

        // 3. ASR 语音转文字（必须在 transferTo 之前，否则临时文件被删除）
        String userText = speechService.recognize(audioFile);

        // 4. 保存音频文件
        String audioUrl = null;
        if (audioFile != null && !audioFile.isEmpty()) {
            audioUrl = saveAudioFile(sessionId, session.getCurrentRound() + 1, audioFile);
        }
        if (userText == null || userText.isEmpty()) {
            userText = "（语音输入）";
        }

        // 5. 敏感词过滤
        userText = contentSafetyService.filter(userText);

        // 获取场景信息用于构建 Prompt
        Scene scene = sceneRepository.findById(session.getSceneId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "场景不存在"));
        String personality = scene.getPersonality() != null ? scene.getPersonality() : "普通";

        // 获取历史轮次
        List<Round> historyRounds = roundRepository.findBySessionIdOrderByRoundNumberAsc(session.getId());

        int userRoundNumber = session.getCurrentRound() + 1;

        // 6. 构建角色扮演 Prompt
        List<Map<String, String>> messages = rolePlayPromptBuilder.build(
                personality,
                session.getTotalRounds(),
                userRoundNumber,
                historyRounds,
                userText
        );

        // 7. 调用 AI 获取回复
        String aiRawResponse = aiService.chat(messages);

        // 8. 解析 AI 回复 JSON（reply + emotion）
        String aiText;
        String aiEmotion = "neutral";
        try {
            JsonNode jsonNode = objectMapper.readTree(aiRawResponse);
            if (jsonNode.has("reply")) {
                aiText = jsonNode.get("reply").asText();
            } else {
                aiText = aiRawResponse;
            }
            if (jsonNode.has("emotion")) {
                String parsedEmotion = jsonNode.get("emotion").asText();
                if (isValidEmotion(parsedEmotion)) {
                    aiEmotion = parsedEmotion;
                }
            }
        } catch (Exception e) {
            // JSON 解析失败，直接使用原始回复
            log.debug("AI 回复非 JSON 格式，直接使用原文");
            aiText = aiRawResponse;
        }

        // 9. 审核 AI 输出安全性
        if (!contentSafetyService.audit(aiText)) {
            aiText = "（内容已被系统过滤）";
            aiEmotion = "neutral";
        }

        // 10. TTS 生成语音
        String aiAudioUrl = speechService.synthesize(aiText);

        // 保存用户轮次 Round 记录
        Round userRound = new Round();
        userRound.setSessionId(session.getId());
        userRound.setRoundNumber(userRoundNumber);
        userRound.setUserText(userText);
        userRound.setUserAudioUrl(audioUrl);
        userRound.setCreatedAt(LocalDateTime.now());
        roundRepository.save(userRound);

        // 保存 AI 轮次 Round 记录
        Round aiRound = new Round();
        aiRound.setSessionId(session.getId());
        aiRound.setRoundNumber(userRoundNumber);
        aiRound.setAiText(aiText);
        aiRound.setAiEmotion(aiEmotion);
        aiRound.setAiAudioUrl(aiAudioUrl);
        aiRound.setCreatedAt(LocalDateTime.now());
        roundRepository.save(aiRound);

        // currentRound++
        session.setCurrentRound(userRoundNumber);

        // 若达到总轮次，自动结束
        if (session.getCurrentRound() >= session.getTotalRounds()) {
            session.setStatus(Session.SessionStatus.COMPLETED);
            session.setFinishedAt(LocalDateTime.now());
            log.info("会话 {} 已达到总轮次，自动结束", sessionId);
        }
        sessionRepository.save(session);

        log.info("会话 {} 第 {} 轮对练完成", sessionId, userRoundNumber);

        // 返回 ChatResponse
        ChatResponse response = new ChatResponse();
        response.setText(aiText);
        response.setAudioUrl(aiAudioUrl);
        response.setEmotion(aiEmotion);
        response.setExpression(null);
        response.setCurrentRound(session.getCurrentRound());
        response.setTotalRounds(session.getTotalRounds());
        return response;
    }

    /**
     * 请求策略提示。
     * 1. 校验 Session 存在且 ACTIVE
     * 2. 校验 hintUsedCount < 3
     * 3. 构建策略提示 Prompt
     * 4. 调用 AI 获取策略建议
     * 5. hintUsedCount++
     * 6. 返回策略提示文字
     */
    @Transactional
    public String requestHint(Long userId, Long sessionId) {
        Session session = findActiveSession(sessionId, userId);

        if (session.getHintUsedCount() >= MAX_HINT_COUNT) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "提示次数已用完（最多 " + MAX_HINT_COUNT + " 次）");
        }

        // 获取场景信息和历史轮次
        Scene scene = sceneRepository.findById(session.getSceneId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "场景不存在"));
        List<Round> historyRounds = roundRepository.findBySessionIdOrderByRoundNumberAsc(session.getId());

        // 构建策略提示 Prompt 并调用 AI
        List<Map<String, String>> messages = hintPromptBuilder.build(historyRounds, scene.getName());
        String hint = aiService.chat(messages);

        session.setHintUsedCount(session.getHintUsedCount() + 1);
        sessionRepository.save(session);

        log.info("会话 {} 请求提示（第 {} 次）", sessionId, session.getHintUsedCount());
        return hint;
    }

    /**
     * 结束对练会话。
     * 1. 校验 Session 存在且 ACTIVE
     * 2. 设置 status=COMPLETED, finishedAt=now
     * 3. 返回 sessionId
     */
    @Transactional
    public Long endSession(Long userId, Long sessionId) {
        Session session = findActiveSession(sessionId, userId);

        session.setStatus(Session.SessionStatus.COMPLETED);
        session.setFinishedAt(LocalDateTime.now());
        sessionRepository.save(session);

        log.info("用户 {} 手动结束会话 {}", userId, sessionId);
        return sessionId;
    }

    /**
     * 查找并校验 Session：存在、属于当前用户、状态为 ACTIVE。
     */
    private Session findActiveSession(Long sessionId, Long userId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "会话不存在"));

        if (!session.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权访问该会话");
        }

        if (session.getStatus() != Session.SessionStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "会话已结束，无法继续操作");
        }

        return session;
    }

    /**
     * 校验情绪标签是否合法。
     */
    private boolean isValidEmotion(String emotion) {
        if (emotion == null) return false;
        return List.of("angry", "sarcastic", "hesitant", "compromising", "confident", "neutral")
                .contains(emotion.toLowerCase());
    }

    /**
     * 保存音频文件到本地临时路径。
     * 后续会替换为 OSS 存储。
     */
    private String saveAudioFile(Long sessionId, int roundNumber, MultipartFile audioFile) {
        try {
            Path dir = Paths.get(AUDIO_TMP_DIR, String.valueOf(sessionId));
            Files.createDirectories(dir);

            String originalFilename = audioFile.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = "round_" + roundNumber + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

            Path filePath = dir.resolve(filename);
            audioFile.transferTo(filePath.toFile());

            log.debug("音频文件已保存: {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            log.warn("保存音频文件失败，sessionId={}, round={}", sessionId, roundNumber, e);
            return null;
        }
    }
}
