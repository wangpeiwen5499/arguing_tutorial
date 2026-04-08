package com.arguing.service;

import com.arguing.config.AiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * AI 大模型调用服务。
 * 通过 HTTP 调用 OpenAI 兼容 API，发送对话消息并获取回复。
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final AiConfig aiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AiService(AiConfig aiConfig, ObjectMapper objectMapper) {
        this.aiConfig = aiConfig;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 调用大模型 API，返回回复文本。
     *
     * @param messages 消息列表，每条消息为 Map，包含 role 和 content 字段
     * @return AI 回复的文本内容
     */
    public String chat(List<Map<String, String>> messages) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = Map.of(
                    "model", aiConfig.getModel(),
                    "messages", messages,
                    "temperature", 0.8
            );

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiConfig.getApiKey());

            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(requestBody), headers
            );

            log.debug("调用 AI API, model={}, messagesCount={}", aiConfig.getModel(), messages.size());

            // 发送请求
            String responseBody = restTemplate.postForObject(
                    aiConfig.getApiUrl(), entity, String.class
            );

            if (responseBody == null) {
                log.warn("AI API 返回空响应");
                return "（AI 暂时无法回复，请稍后再试）";
            }

            // 解析响应: response.choices[0].message.content
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    String content = message.get("content").asText();
                    log.debug("AI 回复长度: {} 字符", content.length());
                    return content;
                }
            }

            log.warn("AI API 响应格式异常: {}", responseBody);
            return "（AI 回复解析失败，请重试）";

        } catch (Exception e) {
            log.error("调用 AI API 失败: {}", e.getMessage(), e);
            return "（AI 服务异常，请稍后再试）";
        }
    }
}
