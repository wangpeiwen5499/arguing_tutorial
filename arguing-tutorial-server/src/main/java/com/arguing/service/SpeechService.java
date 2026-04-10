package com.arguing.service;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.arguing.config.AliConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 语音服务。
 * 接入阿里云智能语音交互（NLS），提供 ASR 和 TTS 功能。
 */
@Service
public class SpeechService {

    private static final Logger log = LoggerFactory.getLogger(SpeechService.class);

    private static final String NLS_GATEWAY = "https://nls-gateway-cn-shanghai.aliyuncs.com";
    private static final String TTS_DIR = System.getProperty("java.io.tmpdir") + "/arguing-tutorial/tts";

    private final AliConfig aliConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** 缓存的 NLS Token */
    private String nlsToken;
    /** Token 过期时间（秒级时间戳） */
    private long tokenExpireTime;

    public SpeechService(AliConfig aliConfig, ObjectMapper objectMapper) {
        this.aliConfig = aliConfig;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    // ==================== Token 管理 ====================

    /**
     * 获取 NLS Token，过期前自动刷新。
     */
    private synchronized String getToken() {
        if (nlsToken != null && System.currentTimeMillis() / 1000 < tokenExpireTime - 60) {
            return nlsToken;
        }

        try {
            DefaultProfile profile = DefaultProfile.getProfile(
                    "cn-shanghai", aliConfig.getAccessKeyId(), aliConfig.getAccessKeySecret());
            IAcsClient client = new DefaultAcsClient(profile);

            CommonRequest request = new CommonRequest();
            request.setDomain("nls-meta.cn-shanghai.aliyuncs.com");
            request.setVersion("2019-02-28");
            request.setAction("CreateToken");
            request.setMethod(MethodType.POST);

            CommonResponse response = client.getCommonResponse(request);
            JsonNode root = objectMapper.readTree(response.getData());
            JsonNode tokenNode = root.get("Token");
            nlsToken = tokenNode.get("Id").asText();
            tokenExpireTime = tokenNode.get("ExpireTime").asLong();

            log.info("NLS Token 获取成功，过期时间戳: {}", tokenExpireTime);
            return nlsToken;
        } catch (Exception e) {
            log.error("获取 NLS Token 失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取语音服务 Token 失败", e);
        }
    }

    // ==================== ASR 语音识别 ====================

    /**
     * ASR 语音识别：将音频文件转为文字。
     * 使用阿里云一句话识别 REST API。
     *
     * @param audioFile 音频文件（支持 wav/pcm/mp3）
     * @return 识别出的文字
     */
    public String recognize(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            log.debug("音频文件为空，跳过 ASR");
            return null;
        }

        try {
            String token = getToken();

            // 根据文件扩展名判断音频格式
            String format = detectAudioFormat(audioFile.getOriginalFilename());

            String url = NLS_GATEWAY + "/stream/v1/asr"
                    + "?appkey=" + aliConfig.getAppKey()
                    + "&format=" + format
                    + "&sample_rate=16000"
                    + "&enable_punctuation_prediction=true"
                    + "&enable_inverse_text_normalization=true";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("X-NLS-Token", token);

            HttpEntity<byte[]> entity = new HttpEntity<>(audioFile.getBytes(), headers);
            String responseBody = restTemplate.postForObject(url, entity, String.class);

            log.debug("ASR 原始响应: {}", responseBody);

            // 解析响应: {"task_id":"...", "result":"识别文本", "status":20000000, "message":"SUCCESS"}
            JsonNode root = objectMapper.readTree(responseBody);
            int statusCode = root.path("status").asInt();

            if (statusCode == 20000000) {
                String result = root.path("result").asText("").trim();
                log.info("ASR 识别成功，文本: {}", result);
                return result.isEmpty() ? null : result;
            } else {
                String message = root.path("message").asText("未知错误");
                log.warn("ASR 识别失败, status={}, message={}", statusCode, message);
                return null;
            }
        } catch (Exception e) {
            log.error("ASR 识别异常: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== TTS 语音合成 ====================

    /**
     * TTS 语音合成：将文字转为语音文件。
     * 使用阿里云语音合成 REST API，返回音频文件路径。
     *
     * @param text 要合成的文字
     * @return 语音文件路径，失败返回 null
     */
    public String synthesize(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        try {
            String token = getToken();
            String url = NLS_GATEWAY + "/stream/v1/tts";

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("appkey", aliConfig.getAppKey());
            body.put("token", token);
            body.put("text", text);
            body.put("format", "wav");
            body.put("sample_rate", 16000);
            body.put("voice", "zhimiao_emo");
            body.put("volume", 50);
            body.put("speech_rate", 0);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().length == 0) {
                log.warn("TTS 合成返回空响应");
                return null;
            }

            // 判断返回的是音频还是 JSON 错误信息
            MediaType contentType = response.getHeaders().getContentType();
            if (contentType != null && contentType.includes(MediaType.APPLICATION_JSON)) {
                String errorJson = new String(response.getBody(), StandardCharsets.UTF_8);
                log.warn("TTS 合成失败: {}", errorJson);
                return null;
            }

            // 保存音频文件
            Path dir = Paths.get(TTS_DIR);
            Files.createDirectories(dir);
            String filename = "tts_" + System.currentTimeMillis() + ".wav";
            Path filePath = dir.resolve(filename);
            Files.write(filePath, response.getBody());

            log.info("TTS 合成成功: {} ({} 字节)", filePath, response.getBody().length);
            return filePath.toString();
        } catch (Exception e) {
            log.error("TTS 合成异常: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== 音素时间戳 ====================

    /**
     * 获取音素时间戳（用于口型同步）。
     * 暂不实现，返回空列表。
     */
    public List<Object> getPhonemeTimestamps(String text) {
        return Collections.emptyList();
    }

    // ==================== 工具方法 ====================

    /**
     * 根据文件名判断音频格式。
     */
    private String detectAudioFormat(String filename) {
        if (filename == null) {
            return "wav";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".mp3")) {
            return "mp3";
        } else if (lower.endsWith(".pcm")) {
            return "pcm";
        } else if (lower.endsWith(".ogg") || lower.endsWith(".opus")) {
            return "ogg_opus";
        }
        return "wav";
    }
}
