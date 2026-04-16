package com.arguing.service;

import com.arguing.config.StorageConfig;
import com.arguing.config.WxCloudCosCredentialsProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 对象存储服务。
 * 使用微信云托管内置存储（腾讯云 COS），通过临时密钥访问。
 */
@Service
public class OssService {

    private static final Logger log = LoggerFactory.getLogger(OssService.class);
    private static final String META_ENCODE_URL = "http://api.weixin.qq.com/_/cos/metaid/encode";

    private final StorageConfig storageConfig;
    private final COSClient cosClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OssService(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
        ClientConfig clientConfig = new ClientConfig(new Region(storageConfig.getRegion()));
        this.cosClient = new COSClient(new WxCloudCosCredentialsProvider(), clientConfig);
    }

    @PreDestroy
    public void destroy() {
        if (cosClient != null) {
            cosClient.shutdown();
        }
    }

    /**
     * 上传字节数组到 COS。
     *
     * @param key  对象键（如 "tts/tts_123.wav"）
     * @param data 文件内容
     * @return COS 对象键（key）
     */
    public String upload(String key, byte[] data) {
        return upload(key, new ByteArrayInputStream(data), data.length);
    }

    /**
     * 上传 InputStream 到 COS。
     * 会自动写入 x-cos-meta-fileid 元数据，确保小程序端可访问。
     *
     * @param key    对象键
     * @param stream 输入流
     * @param size   数据大小
     * @return COS 对象键（key）
     */
    public String upload(String key, InputStream stream, long size) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(size);

            // 获取文件元数据（微信云托管要求，否则小程序端无法访问）
            String metaFieldValue = getFileMetaId(key);
            if (metaFieldValue != null) {
                metadata.addUserMetadata("fileid", metaFieldValue);
            }

            PutObjectRequest request = new PutObjectRequest(
                    storageConfig.getBucket(), key, stream, metadata);
            cosClient.putObject(request);

            log.info("文件已上传到 COS: key={}", key);
            return key;
        } catch (Exception e) {
            log.error("上传 COS 失败: {}", e.getMessage(), e);
            throw new RuntimeException("上传文件失败", e);
        }
    }

    /**
     * 从 COS 下载文件，返回字节数组。
     *
     * @param key 对象键（如 "audio/1/round_1_xxx.mp3"）
     * @return 文件内容字节数组
     */
    public byte[] download(String key) {
        GetObjectRequest getRequest = new GetObjectRequest(storageConfig.getBucket(), key);
        COSObject cosObject = null;
        try {
            cosObject = cosClient.getObject(getRequest);
            try (InputStream is = cosObject.getObjectContent();
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) != -1) {
                    bos.write(buf, 0, len);
                }
                log.debug("从 COS 下载文件成功: key={}, size={}", key, bos.size());
                return bos.toByteArray();
            }
        } catch (Exception e) {
            log.error("从 COS 下载文件失败: key={}, error={}", key, e.getMessage(), e);
            throw new RuntimeException("下载文件失败", e);
        } finally {
            if (cosObject != null) {
                try { cosObject.close(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 通过 HTTP URL 下载文件（用于前端通过 wx.cloud.getTempFileURL 获取的临时链接）。
     *
     * @param url 可访问的 HTTP/HTTPS URL
     * @return 文件内容字节数组
     */
    public byte[] downloadFromUrl(String url) {
        log.info("通过 HTTP 下载文件: {}", url.length() > 120 ? url.substring(0, 120) + "..." : url);
        long start = System.currentTimeMillis();
        try (InputStream is = URI.create(url).toURL().openStream();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) {
                bos.write(buf, 0, len);
            }
            log.info("HTTP 下载完成, size={}, 耗时={}ms", bos.size(), System.currentTimeMillis() - start);
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("HTTP 下载失败: {}", e.getMessage(), e);
            throw new RuntimeException("HTTP下载文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成带签名的 COS 文件访问 URL（有效期1小时）。
     * COS 桶默认私有，无签名的链接会被拒绝。
     *
     * @param key 对象键
     * @return 带签名的可访问 URL
     */
    public String getUrl(String key) {
        Date expiration = new Date(System.currentTimeMillis() + 3600 * 1000);
        return cosClient.generatePresignedUrl(storageConfig.getBucket(), key, expiration).toString();
    }

    /**
     * 获取文件元数据 x-cos-meta-fileid。
     * 微信云托管要求服务端上传时写入此元数据，否则小程序端无法访问文件。
     */
    private String getFileMetaId(String key) {
        try {
            String path = key.startsWith("/") ? key : "/" + key;
            String body = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                put("openid", "");
                put("bucket", storageConfig.getBucket());
                put("paths", java.util.Collections.singletonList(path));
            }});

            HttpURLConnection conn = (HttpURLConnection) URI.create(META_ENCODE_URL).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            try (InputStream is = conn.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);
                JsonNode metaStrs = root.at("/respdata/x_cos_meta_field_strs");
                if (metaStrs.isArray() && metaStrs.size() > 0) {
                    return metaStrs.get(0).asText();
                }
            }
            log.warn("获取文件元数据返回空: key={}", key);
            return null;
        } catch (Exception e) {
            log.warn("获取文件元数据失败（非致命）: key={}, error={}", key, e.getMessage());
            return null;
        }
    }
}
