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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    /** 每次会话最多可使用的提示次数 */
    private static final int MAX_HINT_COUNT = 3;

    private final SessionRepository sessionRepository;
    private final RoundRepository roundRepository;
    private final SceneRepository sceneRepository;
    private final AiService aiService;
    private final SpeechService speechService;
    private final ContentSafetyService contentSafetyService;
    private final RolePlayPromptBuilder rolePlayPromptBuilder;
    private final HintPromptBuilder hintPromptBuilder;
    private final ObjectMapper objectMapper;
    private final OssService ossService;

    public SessionService(SessionRepository sessionRepository,
                          RoundRepository roundRepository,
                          SceneRepository sceneRepository,
                          AiService aiService,
                          SpeechService speechService,
                          ContentSafetyService contentSafetyService,
                          RolePlayPromptBuilder rolePlayPromptBuilder,
                          HintPromptBuilder hintPromptBuilder,
                          ObjectMapper objectMapper,
                          OssService ossService) {
        this.sessionRepository = sessionRepository;
        this.roundRepository = roundRepository;
        this.sceneRepository = sceneRepository;
        this.aiService = aiService;
        this.speechService = speechService;
        this.contentSafetyService = contentSafetyService;
        this.rolePlayPromptBuilder = rolePlayPromptBuilder;
        this.hintPromptBuilder = hintPromptBuilder;
        this.objectMapper = objectMapper;
        this.ossService = ossService;
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
        log.info("[startSession] 开始创建会话, userId={}, sceneId={}", userId, sceneId);

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

        log.info("[startSession] 会话创建完成, sessionId={}, 场景={}, 开场白={}",
                session.getId(), scene.getName(), truncate(openingLine, 80));

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
     * 3. 从 COS 下载音频 → ASR 语音转文字
     * 4. 敏感词过滤
     * 5. 构建角色扮演 Prompt
     * 6. 调用 AI 获取回复
     * 7. 解析 AI 回复 JSON（reply + emotion）
     * 8. 审核 AI 输出安全性
     * 9. TTS 生成语音
     * 10. 保存 Round，返回 ChatResponse
     */
    @Transactional
    public ChatResponse chat(Long userId, Long sessionId, String audioUrl, String audioKey) {
        log.info("[chat] 开始处理 会话={}, 用户={}, audioKey={}", sessionId, userId, audioKey);

        // 1. 校验 Session 存在且 ACTIVE
        Session session = findActiveSession(sessionId, userId);
        log.info("[chat] 步骤1-校验Session通过, 当前轮次={}/{}", session.getCurrentRound(), session.getTotalRounds());

        // 2. 校验轮次
        if (session.getCurrentRound() >= session.getTotalRounds()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "对练已结束，不能再发送消息");
        }

        // 3. 下载音频 → ASR 语音转文字
        // audioUrl: 临时链接，仅用于本次下载
        // audioKey: COS key 永久标识，存入数据库
        String userText = null;
        if (audioUrl != null && !audioUrl.isEmpty()) {
            long t3 = System.currentTimeMillis();
            try {
                byte[] audioData;
                if (audioUrl.startsWith("http://") || audioUrl.startsWith("https://")) {
                    audioData = ossService.downloadFromUrl(audioUrl);
                } else {
                    audioData = ossService.download(audioUrl);
                }
                log.info("[chat] 步骤3a-下载音频完成, size={}, 耗时={}ms",
                        audioData.length, System.currentTimeMillis() - t3);

                long t3b = System.currentTimeMillis();
                userText = speechService.recognize(audioData, audioUrl);
                log.info("[chat] 步骤3b-ASR识别完成, 结果={}, 耗时={}ms",
                        userText != null ? truncate(userText, 100) : null, System.currentTimeMillis() - t3b);
            } catch (Exception e) {
                log.warn("[chat] 步骤3-音频处理失败, 跳过ASR: {}", e.getMessage());
            }
        }
        if (userText == null || userText.isEmpty()) {
            userText = "（语音输入）";
        }

        // 4. 敏感词过滤
        userText = contentSafetyService.filter(userText);
        log.info("[chat] 步骤4-敏感词过滤后: {}", truncate(userText, 100));

        // 获取场景信息用于构建 Prompt
        Scene scene = sceneRepository.findById(session.getSceneId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "场景不存在"));
        String personality = scene.getPersonality() != null ? scene.getPersonality() : "普通";

        // 获取历史轮次
        List<Round> historyRounds = roundRepository.findBySessionIdOrderByRoundNumberAsc(session.getId());

        int userRoundNumber = session.getCurrentRound() + 1;
        log.info("[chat] 步骤5-构建Prompt, 场景={}, 性格={}, 历史轮次数={}, 目标轮次={}",
                scene.getName(), personality, historyRounds.size(), userRoundNumber);

        // 5. 构建角色扮演 Prompt
        List<Map<String, String>> messages = rolePlayPromptBuilder.build(
                personality,
                session.getTotalRounds(),
                userRoundNumber,
                historyRounds,
                userText
        );

        // 6. 调用 AI 获取回复
        long t6 = System.currentTimeMillis();
        String aiRawResponse = aiService.chat(messages);
        log.info("[chat] 步骤6-AI回复完成, 耗时={}ms, 原始回复={}",
                System.currentTimeMillis() - t6, truncate(aiRawResponse, 200));

        // 7. 解析 AI 回复 JSON（reply + emotion）
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
            log.debug("[chat] 步骤7-AI回复非JSON格式，直接使用原文");
            aiText = aiRawResponse;
        }
        log.info("[chat] 步骤7-解析AI回复: text={}, emotion={}", truncate(aiText, 100), aiEmotion);

        // 8. 审核 AI 输出安全性
        if (!contentSafetyService.audit(aiText)) {
            log.warn("[chat] 步骤8-AI输出未通过安全审核，已过滤");
            aiText = "（内容已被系统过滤）";
            aiEmotion = "neutral";
        }

        // 9. TTS 生成语音（返回 COS key）
        long t9 = System.currentTimeMillis();
        String aiAudioKey = speechService.synthesize(aiText);
        log.info("[chat] 步骤9-TTS合成完成, audioKey={}, 耗时={}ms",
                aiAudioKey, System.currentTimeMillis() - t9);

        // 保存用户轮次 Round 记录
        Round userRound = new Round();
        userRound.setSessionId(session.getId());
        userRound.setRoundNumber(userRoundNumber);
        userRound.setUserText(userText);
        userRound.setUserAudioUrl(audioKey);
        userRound.setCreatedAt(LocalDateTime.now());
        roundRepository.save(userRound);

        // 保存 AI 轮次 Round 记录
        Round aiRound = new Round();
        aiRound.setSessionId(session.getId());
        aiRound.setRoundNumber(userRoundNumber);
        aiRound.setAiText(aiText);
        aiRound.setAiEmotion(aiEmotion);
        aiRound.setAiAudioUrl(aiAudioKey != null ? ossService.getUrl(aiAudioKey) : null);
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
        response.setAudioUrl(aiAudioKey != null ? ossService.getUrl(aiAudioKey) : null);
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
        log.info("[requestHint] 开始, userId={}, sessionId={}", userId, sessionId);
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
        long t = System.currentTimeMillis();
        List<Map<String, String>> messages = hintPromptBuilder.build(historyRounds, scene.getName());
        String hint = aiService.chat(messages);

        session.setHintUsedCount(session.getHintUsedCount() + 1);
        sessionRepository.save(session);

        log.info("[requestHint] 完成, 第{}次提示, 耗时={}ms, 结果={}",
                session.getHintUsedCount(), System.currentTimeMillis() - t, truncate(hint, 100));
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
        log.info("[endSession] 开始, userId={}, sessionId={}", userId, sessionId);
        Session session = findActiveSession(sessionId, userId);

        session.setStatus(Session.SessionStatus.COMPLETED);
        session.setFinishedAt(LocalDateTime.now());
        sessionRepository.save(session);

        log.info("[endSession] 会话已结束, sessionId={}, 总轮次={}, 使用提示={}次",
                sessionId, session.getCurrentRound(), session.getHintUsedCount());
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
     * 截断字符串，避免日志过长。
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "null";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "...";
    }

}
