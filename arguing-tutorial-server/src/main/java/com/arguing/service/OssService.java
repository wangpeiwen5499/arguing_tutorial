package com.arguing.service;

// 方案B：阿里云 OSS（暂未使用，后续可能切换）
// import com.aliyun.oss.OSS;
// import com.aliyun.oss.OSSClientBuilder;
// import com.arguing.config.AliConfig;

import com.arguing.config.StorageConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 对象存储服务。
 * 当前使用方案A：微信云托管内置存储（腾讯云 COS）。
 * 如需切换到方案B（阿里云 OSS），取消注释相关代码并注释 COS 代码即可。
 */
@Service
public class OssService {

    private static final Logger log = LoggerFactory.getLogger(OssService.class);

    private final StorageConfig storageConfig;

    public OssService(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
    }

    /**
     * 上传字节数组到 COS。
     *
     * @param key  对象键（如 "tts/tts_123.wav"）
     * @param data 文件内容
     * @return 可访问的 HTTP URL
     */
    public String upload(String key, byte[] data) {
        return upload(key, new ByteArrayInputStream(data), data.length);
    }

    /**
     * 上传 InputStream 到 COS。
     *
     * @param key    对象键
     * @param stream 输入流
     * @param size   数据大小
     * @return 可访问的 HTTP URL
     */
    public String upload(String key, InputStream stream, long size) {
        COSClient cosClient = null;
        try {
            COSCredentials cred = new BasicCOSCredentials(
                    storageConfig.getSecretId(),
                    storageConfig.getSecretKey());
            ClientConfig clientConfig = new ClientConfig(new Region(storageConfig.getRegion()));
            cosClient = new COSClient(cred, clientConfig);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(size);

            PutObjectRequest request = new PutObjectRequest(
                    storageConfig.getBucket(), key, stream, metadata);
            cosClient.putObject(request);

            String url = String.format("https://%s.cos.%s.myqcloud.com/%s",
                    storageConfig.getBucket(), storageConfig.getRegion(), key);
            log.info("文件已上传到 COS: {}", url);
            return url;
        } catch (Exception e) {
            log.error("上传 COS 失败: {}", e.getMessage(), e);
            throw new RuntimeException("上传文件失败", e);
        } finally {
            if (cosClient != null) {
                cosClient.shutdown();
            }
        }
    }

    // ==================== 方案B：阿里云 OSS（暂未使用） ====================
    //
    // private final AliConfig aliConfig;
    //
    // public OssService(AliConfig aliConfig) {
    //     this.aliConfig = aliConfig;
    // }
    //
    // public String upload(String key, byte[] data) {
    //     return upload(key, new ByteArrayInputStream(data), data.length);
    // }
    //
    // public String upload(String key, InputStream stream, long size) {
    //     OSS ossClient = null;
    //     try {
    //         AliConfig.Oss ossConfig = aliConfig.getOss();
    //         ossClient = new OSSClientBuilder().build(
    //                 ossConfig.getEndpoint(),
    //                 aliConfig.getAccessKeyId(),
    //                 aliConfig.getAccessKeySecret());
    //
    //         ossClient.putObject(ossConfig.getBucket(), key, stream);
    //
    //         String url = ossConfig.getUrlPrefix() + key;
    //         log.info("文件已上传到 OSS: {}", url);
    //         return url;
    //     } catch (Exception e) {
    //         log.error("上传 OSS 失败: {}", e.getMessage(), e);
    //         throw new RuntimeException("上传文件失败", e);
    //     } finally {
    //         if (ossClient != null) {
    //             ossClient.shutdown();
    //         }
    //     }
    // }
}
