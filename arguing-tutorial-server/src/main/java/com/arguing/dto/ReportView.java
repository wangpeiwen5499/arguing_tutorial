package com.arguing.dto;

import java.util.List;
import java.util.Map;

/**
 * 复盘报告视图 DTO。
 * 返回给前端的完整复盘报告数据。
 */
public class ReportView {

    /** 会话 ID */
    private Long sessionId;

    /** 场景名称 */
    private String sceneName;

    /** 总分 */
    private int totalScore;

    /** 逻辑性分数（权重 25%） */
    private int logicScore;

    /** 情绪控制分数（权重 20%） */
    private int emotionScore;

    /** 说服力分数（权重 25%） */
    private int persuasionScore;

    /** 策略运用分数（权重 15%） */
    private int strategyScore;

    /** 表达清晰度分数（权重 15%） */
    private int clarityScore;

    /** 优势列表 */
    private List<String> strengths;

    /** 改进建议列表 */
    private List<String> improvements;

    /** 各轮次评语 */
    private List<RoundReview> roundReviews;

    /** 与上次对比的分数差异，null 表示无历史对比 */
    private Integer scoreDiff;

    /** 分享卡片 URL */
    private String shareCardUrl;

    public ReportView() {
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getSceneName() {
        return sceneName;
    }

    public void setSceneName(String sceneName) {
        this.sceneName = sceneName;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getLogicScore() {
        return logicScore;
    }

    public void setLogicScore(int logicScore) {
        this.logicScore = logicScore;
    }

    public int getEmotionScore() {
        return emotionScore;
    }

    public void setEmotionScore(int emotionScore) {
        this.emotionScore = emotionScore;
    }

    public int getPersuasionScore() {
        return persuasionScore;
    }

    public void setPersuasionScore(int persuasionScore) {
        this.persuasionScore = persuasionScore;
    }

    public int getStrategyScore() {
        return strategyScore;
    }

    public void setStrategyScore(int strategyScore) {
        this.strategyScore = strategyScore;
    }

    public int getClarityScore() {
        return clarityScore;
    }

    public void setClarityScore(int clarityScore) {
        this.clarityScore = clarityScore;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getImprovements() {
        return improvements;
    }

    public void setImprovements(List<String> improvements) {
        this.improvements = improvements;
    }

    public List<RoundReview> getRoundReviews() {
        return roundReviews;
    }

    public void setRoundReviews(List<RoundReview> roundReviews) {
        this.roundReviews = roundReviews;
    }

    public Integer getScoreDiff() {
        return scoreDiff;
    }

    public void setScoreDiff(Integer scoreDiff) {
        this.scoreDiff = scoreDiff;
    }

    public String getShareCardUrl() {
        return shareCardUrl;
    }

    public void setShareCardUrl(String shareCardUrl) {
        this.shareCardUrl = shareCardUrl;
    }

    /**
     * 将视图转为 Map，方便统一返回格式。
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("sessionId", sessionId);
        map.put("sceneName", sceneName);
        map.put("totalScore", totalScore);
        map.put("logicScore", logicScore);
        map.put("emotionScore", emotionScore);
        map.put("persuasionScore", persuasionScore);
        map.put("strategyScore", strategyScore);
        map.put("clarityScore", clarityScore);
        map.put("strengths", strengths);
        map.put("improvements", improvements);
        map.put("roundReviews", roundReviews != null
                ? roundReviews.stream().map(RoundReview::toMap).toList()
                : List.of());
        map.put("scoreDiff", scoreDiff);
        map.put("shareCardUrl", shareCardUrl);
        return map;
    }

    /**
     * 轮次评语内嵌 DTO。
     */
    public static class RoundReview {

        /** 轮次编号 */
        private int round;

        /** 该轮次评语 */
        private String comment;

        /** 该轮次分数 */
        private int score;

        public RoundReview() {
        }

        public RoundReview(int round, String comment, int score) {
            this.round = round;
            this.comment = comment;
            this.score = score;
        }

        public int getRound() {
            return round;
        }

        public void setRound(int round) {
            this.round = round;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("round", round);
            map.put("comment", comment);
            map.put("score", score);
            return map;
        }
    }
}
