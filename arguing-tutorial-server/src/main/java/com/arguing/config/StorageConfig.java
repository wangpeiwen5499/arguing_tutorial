package com.arguing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 对象存储配置（微信云托管内置存储 - 腾讯云 COS）。
 * 临时密钥由 WxCloudCosCredentialsProvider 自动从微信云托管 API 获取，
 * 此处仅需配置 bucket 和 region。
 */
@Configuration
@ConfigurationProperties(prefix = "storage.cos")
public class StorageConfig {

    /** COS 地域（如 ap-shanghai） */
    private String region;

    /** COS 存储桶名称 */
    private String bucket;

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
}
