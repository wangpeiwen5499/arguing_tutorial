package com.arguing.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对练聊天响应 DTO。
 * 包含 AI 回复文字、语音 URL、情绪、表情指令以及轮次进度。
 */
public class ChatResponse {

    /** AI 回复文字 */
    private String text;

    /** AI 语音 URL（mock 阶段为空） */
    private String audioUrl;

    /** 情绪标签：angry/sarcastic/hesitant/compromising/confident/neutral */
    private String emotion;

    /** 表情指令（mock 阶段为空） */
    private Object expression;

    /** 当前轮次 */
    private Integer currentRound;

    /** 总轮次 */
    private Integer totalRounds;

    public ChatResponse() {
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public Object getExpression() {
        return expression;
    }

    public void setExpression(Object expression) {
        this.expression = expression;
    }

    public Integer getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(Integer currentRound) {
        this.currentRound = currentRound;
    }

    public Integer getTotalRounds() {
        return totalRounds;
    }

    public void setTotalRounds(Integer totalRounds) {
        this.totalRounds = totalRounds;
    }

    /**
     * 将响应转为 Map，方便统一返回格式。
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("text", text);
        map.put("audioUrl", audioUrl);
        map.put("emotion", emotion);
        map.put("expression", expression);
        map.put("currentRound", currentRound);
        map.put("totalRounds", totalRounds);
        return map;
    }
}
