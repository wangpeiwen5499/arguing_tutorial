package com.arguing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务配置。
 * 从 application.yml 的 ai 前缀读取配置项，
 * 支持通过环境变量覆盖。
 */
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    /** 大模型 API 地址 */
    private String apiUrl = "https://api.openai.com/v1/chat/completions";

    /** API Key */
    private String apiKey = "sk-placeholder";

    /** 模型名称 */
    private String model = "gpt-4o-mini";

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
