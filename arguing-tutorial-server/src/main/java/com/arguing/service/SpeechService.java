package com.arguing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

/**
 * 语音服务。
 * 提供 ASR（语音转文字）和 TTS（文字转语音）功能。
 * 当前为 mock 实现，后续接入真实 ASR/TTS 服务。
 */
@Service
public class SpeechService {

    private static final Logger log = LoggerFactory.getLogger(SpeechService.class);

    /**
     * ASR 语音识别：将音频文件转为文字。
     * Mock 实现：返回固定文本。
     *
     * @param audioFile 音频文件
     * @return 识别出的文字
     */
    public String recognize(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            log.debug("音频文件为空，跳过 ASR");
            return null;
        }

        // Mock 实现：返回固定文本
        log.debug("Mock ASR: 音频文件大小={} bytes", audioFile.getSize());
        return "（语音输入）";
    }

    /**
     * TTS 语音合成：将文字转为语音文件。
     * Mock 实现：返回 null。
     *
     * @param text 要合成的文字
     * @return 语音文件 URL，mock 阶段返回 null
     */
    public String synthesize(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // Mock 实现：返回空 URL
        log.debug("Mock TTS: 文本长度={} 字符", text.length());
        return null;
    }

    /**
     * 获取音素时间戳（用于口型同步）。
     * Mock 实现：返回空列表。
     *
     * @param text 文本内容
     * @return 音素时间戳列表
     */
    public List<Object> getPhonemeTimestamps(String text) {
        // Mock 实现
        log.debug("Mock PhonemeTimestamps: text={}", text);
        return Collections.emptyList();
    }
}
