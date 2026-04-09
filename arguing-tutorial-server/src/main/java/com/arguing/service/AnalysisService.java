package com.arguing.service;

import com.arguing.dto.ReportView;
import com.arguing.entity.Report;
import com.arguing.entity.Round;
import com.arguing.entity.Scene;
import com.arguing.entity.Session;
import com.arguing.exception.ApiException;
import com.arguing.repository.ReportRepository;
import com.arguing.repository.RoundRepository;
import com.arguing.repository.SceneRepository;
import com.arguing.repository.SessionRepository;
import com.arguing.service.prompt.AnalysisPromptBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 复盘分析服务。
 * 负责对完成的对练会话进行 AI 分析，生成多维度评分和评语报告。
 */
@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    /** 提示扣分：每次使用提示扣 5 分 */
    private static final int HINT_PENALTY_PER_USE = 5;

    /** 评分权重 */
    private static final double WEIGHT_LOGIC = 0.25;
    private static final double WEIGHT_EMOTION = 0.20;
    private static final double WEIGHT_PERSUASION = 0.25;
    private static final double WEIGHT_STRATEGY = 0.15;
    private static final double WEIGHT_CLARITY = 0.15;

    private final AiService aiService;
    private final ReportRepository reportRepository;
    private final RoundRepository roundRepository;
    private final SessionRepository sessionRepository;
    private final SceneRepository sceneRepository;
    private final AnalysisPromptBuilder analysisPromptBuilder;
    private final ObjectMapper objectMapper;

    public AnalysisService(AiService aiService,
                           ReportRepository reportRepository,
                           RoundRepository roundRepository,
                           SessionRepository sessionRepository,
                           SceneRepository sceneRepository,
                           AnalysisPromptBuilder analysisPromptBuilder,
                           ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.reportRepository = reportRepository;
        this.roundRepository = roundRepository;
        this.sessionRepository = sessionRepository;
        this.sceneRepository = sceneRepository;
        this.analysisPromptBuilder = analysisPromptBuilder;
        this.objectMapper = objectMapper;
    }

    /**
     * 对已完成的会话进行复盘分析。
     * 1. 获取所有 rounds
     * 2. 构建 analysis prompt
     * 3. 调用 aiService.chat() 获取分析结果
     * 4. 解析 JSON 获取各维度分数
     * 5. 计算总分: logic*0.25 + emotion*0.20 + persuasion*0.25 + strategy*0.15 + clarity*0.15
     * 6. 减去提示扣分: hintUsedCount * 5
     * 7. 最低 0 分，最高 100 分
     * 8. 保存 Report
     *
     * @param session 已完成的会话
     * @return 生成的复盘报告
     */
    @Transactional
    public Report analyze(Session session) {
        // 1. 获取场景信息和所有轮次
        Scene scene = sceneRepository.findById(session.getSceneId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "场景不存在"));

        List<Round> rounds = roundRepository.findBySessionIdOrderByRoundNumberAsc(session.getId());

        if (rounds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "会话没有对话记录，无法分析");
        }

        // 2. 构建 analysis prompt
        List<Map<String, String>> messages = analysisPromptBuilder.build(session, scene, rounds);

        // 3. 调用 AI 获取分析结果
        String aiResponse = aiService.chat(messages);
        log.debug("AI 分析结果原始响应: {}", aiResponse);

        // 4. 解析 JSON 获取各维度分数和评语
        AnalysisResult result = parseAnalysisResult(aiResponse);

        // 5. 计算加权总分
        double rawScore = result.logicScore * WEIGHT_LOGIC
                + result.emotionScore * WEIGHT_EMOTION
                + result.persuasionScore * WEIGHT_PERSUASION
                + result.strategyScore * WEIGHT_STRATEGY
                + result.clarityScore * WEIGHT_CLARITY;

        // 6. 减去提示扣分
        int hintPenalty = session.getHintUsedCount() * HINT_PENALTY_PER_USE;
        int totalScore = (int) Math.round(rawScore) - hintPenalty;

        // 7. 限制在 0-100 范围内
        totalScore = Math.max(0, Math.min(100, totalScore));

        log.info("会话 {} 分析完成: 总分={}, 加权分={}, 提示扣分={}",
                session.getId(), totalScore, Math.round(rawScore), hintPenalty);

        // 8. 保存 Report
        Report report = reportRepository.findBySessionId(session.getId())
                .orElseGet(Report::new);

        report.setSessionId(session.getId());
        report.setTotalScore(totalScore);
        report.setLogicScore(result.logicScore);
        report.setEmotionScore(result.emotionScore);
        report.setPersuasionScore(result.persuasionScore);
        report.setStrategyScore(result.strategyScore);
        report.setClarityScore(result.clarityScore);

        try {
            report.setStrengths(objectMapper.writeValueAsString(result.strengths));
            report.setImprovements(objectMapper.writeValueAsString(result.improvements));
            report.setRoundReviews(objectMapper.writeValueAsString(result.roundReviews));
        } catch (Exception e) {
            log.warn("序列化评语失败", e);
            report.setStrengths("[]");
            report.setImprovements("[]");
            report.setRoundReviews("[]");
        }

        report.setCreatedAt(LocalDateTime.now());
        report = reportRepository.save(report);

        log.info("复盘报告已保存, reportId={}, sessionId={}", report.getId(), session.getId());
        return report;
    }

    /**
     * 获取复盘报告视图。
     * 如果会话已完成但还没有报告，会自动触发生成。
     *
     * @param sessionId 会话 ID
     * @return 复盘报告视图
     */
    @Transactional
    public ReportView getReport(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "会话不存在"));

        // 会话必须是已完成状态
        if (session.getStatus() == Session.SessionStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "会话尚未结束，无法查看报告");
        }

        // 查找现有报告，如果不存在则自动生成
        Report report = reportRepository.findBySessionId(sessionId)
                .orElseGet(() -> analyze(session));

        // 获取场景名称
        Scene scene = sceneRepository.findById(session.getSceneId())
                .orElse(null);
        String sceneName = scene != null ? scene.getName() : "未知场景";

        // 计算与上次对比的分数差异
        Integer scoreDiff = calculateScoreDiff(session, report.getTotalScore());

        // 构建 ReportView
        ReportView view = new ReportView();
        view.setSessionId(sessionId);
        view.setSceneName(sceneName);
        view.setTotalScore(report.getTotalScore());
        view.setLogicScore(report.getLogicScore());
        view.setEmotionScore(report.getEmotionScore());
        view.setPersuasionScore(report.getPersuasionScore());
        view.setStrategyScore(report.getStrategyScore());
        view.setClarityScore(report.getClarityScore());
        view.setScoreDiff(scoreDiff);
        view.setShareCardUrl(report.getShareCardUrl());

        // 解析 JSON 字段
        try {
            JsonNode strengthsNode = objectMapper.readTree(report.getStrengths());
            List<String> strengths = new ArrayList<>();
            if (strengthsNode.isArray()) {
                for (JsonNode item : strengthsNode) {
                    strengths.add(item.asText());
                }
            }
            view.setStrengths(strengths);

            JsonNode improvementsNode = objectMapper.readTree(report.getImprovements());
            List<String> improvements = new ArrayList<>();
            if (improvementsNode.isArray()) {
                for (JsonNode item : improvementsNode) {
                    improvements.add(item.asText());
                }
            }
            view.setImprovements(improvements);

            JsonNode roundReviewsNode = objectMapper.readTree(report.getRoundReviews());
            List<ReportView.RoundReview> roundReviews = new ArrayList<>();
            if (roundReviewsNode.isArray()) {
                for (JsonNode item : roundReviewsNode) {
                    int round = item.has("round") ? item.get("round").asInt() : 0;
                    String comment = item.has("comment") ? item.get("comment").asText() : "";
                    int score = item.has("score") ? item.get("score").asInt() : 0;
                    roundReviews.add(new ReportView.RoundReview(round, comment, score));
                }
            }
            view.setRoundReviews(roundReviews);
        } catch (Exception e) {
            log.warn("解析报告 JSON 字段失败", e);
            view.setStrengths(List.of());
            view.setImprovements(List.of());
            view.setRoundReviews(List.of());
        }

        return view;
    }

    /**
     * 解析 AI 返回的分析结果 JSON。
     */
    private AnalysisResult parseAnalysisResult(String aiResponse) {
        AnalysisResult result = new AnalysisResult();

        try {
            // 尝试从响应中提取 JSON（可能被 markdown 代码块包裹）
            String jsonStr = extractJson(aiResponse);
            JsonNode root = objectMapper.readTree(jsonStr);

            result.logicScore = clampScore(root.path("logic_score").asInt(50));
            result.emotionScore = clampScore(root.path("emotion_score").asInt(50));
            result.persuasionScore = clampScore(root.path("persuasion_score").asInt(50));
            result.strategyScore = clampScore(root.path("strategy_score").asInt(50));
            result.clarityScore = clampScore(root.path("clarity_score").asInt(50));

            // 解析 strengths
            JsonNode strengthsNode = root.path("strengths");
            if (strengthsNode.isArray()) {
                for (JsonNode item : strengthsNode) {
                    result.strengths.add(item.asText());
                }
            }

            // 解析 improvements
            JsonNode improvementsNode = root.path("improvements");
            if (improvementsNode.isArray()) {
                for (JsonNode item : improvementsNode) {
                    result.improvements.add(item.asText());
                }
            }

            // 解析 round_reviews
            JsonNode roundReviewsNode = root.path("round_reviews");
            if (roundReviewsNode.isArray()) {
                for (JsonNode item : roundReviewsNode) {
                    result.roundReviews.add(Map.of(
                            "round", item.path("round").asInt(0),
                            "comment", item.path("comment").asText(""),
                            "score", item.path("score").asInt(50)
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("解析 AI 分析结果失败，使用默认分数", e);
            // 解析失败时使用默认分数
            result.logicScore = 50;
            result.emotionScore = 50;
            result.persuasionScore = 50;
            result.strategyScore = 50;
            result.clarityScore = 50;
            result.strengths = List.of("分析暂不可用");
            result.improvements = List.of("分析暂不可用");
            result.roundReviews = List.of();
        }

        return result;
    }

    /**
     * 从 AI 响应中提取 JSON 字符串。
     * 处理可能被 markdown 代码块包裹的情况。
     */
    private String extractJson(String response) {
        String trimmed = response.trim();
        // 去掉 markdown 代码块包裹
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    /**
     * 将分数限制在 0-100 范围内。
     */
    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    /**
     * 计算与该用户上次报告的分数差异。
     */
    private Integer calculateScoreDiff(Session currentSession, int currentScore) {
        if (currentSession.getUserId() == null) {
            return null;
        }

        List<Session> userSessions = sessionRepository
                .findByUserIdOrderByCreatedAtDesc(currentSession.getUserId());

        Session previousSession = null;
        for (Session s : userSessions) {
            if (s.getId().equals(currentSession.getId())) {
                continue;
            }
            if (s.getStatus() == Session.SessionStatus.COMPLETED) {
                previousSession = s;
                break;
            }
        }

        if (previousSession == null) {
            return null;
        }

        return reportRepository.findBySessionId(previousSession.getId())
                .map(Report::getTotalScore)
                .map(prevScore -> currentScore - prevScore)
                .orElse(null);
    }

    /**
     * 内部类：AI 分析结果。
     */
    private static class AnalysisResult {
        int logicScore = 50;
        int emotionScore = 50;
        int persuasionScore = 50;
        int strategyScore = 50;
        int clarityScore = 50;
        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        List<Map<String, Object>> roundReviews = new ArrayList<>();
    }
}
