package com.arguing.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.auth.COSCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * 微信云托管 COS 临时密钥提供者。
 * 通过 http://api.weixin.qq.com/_/cos/getauth 获取临时密钥，
 * 在接近过期时自动刷新（提前60秒）。
 */
public class WxCloudCosCredentialsProvider implements COSCredentialsProvider {

    private static final Logger log = LoggerFactory.getLogger(WxCloudCosCredentialsProvider.class);
    private static final String AUTH_URL = "http://api.weixin.qq.com/_/cos/getauth";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile COSCredentials credentials;
    private volatile long expireTime;

    @Override
    public COSCredentials getCredentials() {
        if (credentials == null || epochSeconds() >= expireTime - 60) {
            doRefresh();
        }
        return credentials;
    }

    @Override
    public void refresh() {
        doRefresh();
    }

    private synchronized void doRefresh() {
        if (credentials != null && epochSeconds() < expireTime - 60) {
            return;
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(AUTH_URL).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            try (InputStream is = conn.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);
                this.credentials = new BasicSessionCredentials(
                        root.get("TmpSecretId").asText(),
                        root.get("TmpSecretKey").asText(),
                        root.get("Token").asText()
                );
                this.expireTime = root.get("ExpiredTime").asLong();
                log.info("COS 临时密钥已刷新, 过期时间戳={}", expireTime);
            }
        } catch (Exception e) {
            log.error("获取 COS 临时密钥失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取COS临时密钥失败", e);
        }
    }

    private static long epochSeconds() {
        return System.currentTimeMillis() / 1000;
    }
}
