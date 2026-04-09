package com.arguing.dto;

/**
 * 分享卡片响应 DTO。
 * 返回分享卡片图片 URL 及基本信息。
 */
public class ShareCardResponse {

    /** 分享卡片图片 URL */
    private String imageUrl;

    /** 场景名称 */
    private String sceneName;

    /** 总分 */
    private int totalScore;

    public ShareCardResponse() {
    }

    public ShareCardResponse(String imageUrl, String sceneName, int totalScore) {
        this.imageUrl = imageUrl;
        this.sceneName = sceneName;
        this.totalScore = totalScore;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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
}
