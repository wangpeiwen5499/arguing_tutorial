package com.arguing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 对象存储配置（方案A：微信云托管内置存储 - 腾讯云 COS）。
 * 从 application.yml 的 storage.cos 前缀读取配置项。
 */
@Configuration
@ConfigurationProperties(prefix = "storage.cos")
public class StorageConfig {

    /** 腾讯云 SecretId */
    private String secretId;

    /** 腾讯云 SecretKey */
    private String secretKey;

    /** COS 地域（如 ap-shanghai） */
    private String region;

    /** COS 存储桶名称 */
    private String bucket;

    public String getSecretId() { return secretId; }
    public void setSecretId(String secretId) { this.secretId = secretId; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
}
