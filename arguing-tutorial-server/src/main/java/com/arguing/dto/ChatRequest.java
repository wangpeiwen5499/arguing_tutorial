package com.arguing.dto;

import org.springframework.web.multipart.MultipartFile;

/**
 * 对练聊天请求 DTO。
 * sessionId 来自 URL 路径参数，audio 为用户录音文件。
 */
public class ChatRequest {

    private MultipartFile audio;

    public ChatRequest() {
    }

    public MultipartFile getAudio() {
        return audio;
    }

    public void setAudio(MultipartFile audio) {
        this.audio = audio;
    }
}
