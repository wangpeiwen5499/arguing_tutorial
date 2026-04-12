package com.arguing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云服务配置。
 * 从 application.yml 的 ali 前缀读取配置项。
 */
@Configuration
@ConfigurationProperties(prefix = "ali")
public class AliConfig {

    /** 阿里云 AccessKey ID */
    private String accessKeyId;

    /** 阿里云 AccessKey Secret */
    private String accessKeySecret;

    /** 智能语音交互项目 AppKey */
    private String appKey;

    // 方案B：阿里云 OSS 配置（暂未使用，后续可能切换）
    // private Oss oss = new Oss();
    //
    // public static class Oss {
    //     private String endpoint;
    //     private String bucket;
    //     private String urlPrefix;
    //     public String getEndpoint() { return endpoint; }
    //     public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    //     public String getBucket() { return bucket; }
    //     public void setBucket(String bucket) { this.bucket = bucket; }
    //     public String getUrlPrefix() { return urlPrefix; }
    //     public void setUrlPrefix(String urlPrefix) { this.urlPrefix = urlPrefix; }
    // }
    //
    // public Oss getOss() { return oss; }
    // public void setOss(Oss oss) { this.oss = oss; }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }
}
