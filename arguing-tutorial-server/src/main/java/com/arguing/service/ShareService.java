package com.arguing.service;

import com.arguing.dto.ShareCardResponse;
import com.arguing.entity.Report;
import com.arguing.entity.Scene;
import com.arguing.entity.Session;
import com.arguing.exception.ApiException;
import com.arguing.repository.ReportRepository;
import com.arguing.repository.SceneRepository;
import com.arguing.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * 分享卡片生成服务。
 * 使用 Java 2D Graphics 渲染分享卡片图片。
 */
@Service
public class ShareService {

    private static final Logger log = LoggerFactory.getLogger(ShareService.class);

    /** 卡片宽度 */
    private static final int CARD_WIDTH = 750;

    /** 卡片高度 */
    private static final int CARD_HEIGHT = 600;

    /** 渐变起始色 */
    private static final Color GRADIENT_START = new Color(0x66, 0x7E, 0xEA);

    /** 渐变结束色 */
    private static final Color GRADIENT_END = new Color(0x76, 0x4B, 0xA2);

    /** 五维度名称 */
    private static final String[] DIMENSION_NAMES = {"逻辑性", "情绪控制", "说服力", "策略运用", "表达清晰"};

    private final ReportRepository reportRepository;
    private final SessionRepository sessionRepository;
    private final SceneRepository sceneRepository;
    private final OssService ossService;

    public ShareService(ReportRepository reportRepository,
                        SessionRepository sessionRepository,
                        SceneRepository sceneRepository,
                        OssService ossService) {
        this.reportRepository = reportRepository;
        this.sessionRepository = sessionRepository;
        this.sceneRepository = sceneRepository;
        this.ossService = ossService;
    }

    /**
     * 生成分享卡片。
     *
     * @param sessionId 会话 ID
     * @return 分享卡片响应
     */
    @Transactional
    public ShareCardResponse generateShareCard(Long sessionId) {
        // 1. 获取 Report 和 Scene 信息
        Report report = reportRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "复盘报告不存在"));

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "会话不存在"));

        Scene scene = sceneRepository.findById(session.getSceneId())
                .orElse(null);
        String sceneName = scene != null ? scene.getName() : "未知场景";

        // 2. 渲染卡片图片（返回 COS key）
        String imageKey = renderCardImage(report, sceneName);
        String imageUrl = imageKey != null ? ossService.getUrl(imageKey) : null;

        // 3. 更新 Report.shareCardUrl
        report.setShareCardUrl(imageUrl);
        reportRepository.save(report);

        log.info("分享卡片已生成, sessionId={}, imageUrl={}", sessionId, imageUrl);

        // 4. 返回 ShareCardResponse
        return new ShareCardResponse(imageUrl, sceneName, report.getTotalScore());
    }

    /**
     * 使用 Java 2D Graphics 渲染卡片图片。
     */
    private String renderCardImage(Report report, String sceneName) {
        BufferedImage image = new BufferedImage(CARD_WIDTH, CARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        try {
            // 开启抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 绘制渐变背景
            GradientPaint gradient = new GradientPaint(
                    0, 0, GRADIENT_START,
                    CARD_WIDTH, CARD_HEIGHT, GRADIENT_END
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, CARD_WIDTH, CARD_HEIGHT);

            // 绘制标题 "吵架修炼场"
            g2d.setColor(Color.WHITE);
            Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, 36);
            g2d.setFont(titleFont);
            drawCenteredText(g2d, "吵架修炼场", CARD_WIDTH / 2, 80);

            // 绘制场景名称
            Font sceneFont = new Font(Font.SANS_SERIF, Font.PLAIN, 22);
            g2d.setFont(sceneFont);
            g2d.setColor(new Color(0xDD, 0xDD, 0xFF));
            drawCenteredText(g2d, "场景：" + sceneName, CARD_WIDTH / 2, 130);

            // 绘制分隔线
            g2d.setColor(new Color(0xFF, 0xFF, 0xFF, 80));
            g2d.drawLine(100, 160, CARD_WIDTH - 100, 160);

            // 绘制大字总分
            Font scoreFont = new Font(Font.SANS_SERIF, Font.BOLD, 96);
            g2d.setFont(scoreFont);
            g2d.setColor(Color.WHITE);
            drawCenteredText(g2d, String.valueOf(report.getTotalScore()), CARD_WIDTH / 2, 280);

            // 总分标签
            Font labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 20);
            g2d.setFont(labelFont);
            g2d.setColor(new Color(0xCC, 0xCC, 0xFF));
            drawCenteredText(g2d, "综合得分", CARD_WIDTH / 2, 315);

            // 绘制分隔线
            g2d.setColor(new Color(0xFF, 0xFF, 0xFF, 80));
            g2d.drawLine(100, 345, CARD_WIDTH - 100, 345);

            // 绘制五维度星级
            int[] scores = {
                    report.getLogicScore(),
                    report.getEmotionScore(),
                    report.getPersuasionScore(),
                    report.getStrategyScore(),
                    report.getClarityScore()
            };

            Font dimLabelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
            Font dimValueFont = new Font(Font.SANS_SERIF, Font.BOLD, 18);
            int startY = 390;
            int lineSpacing = 40;

            for (int i = 0; i < DIMENSION_NAMES.length; i++) {
                int y = startY + i * lineSpacing;

                // 维度名称（左侧）
                g2d.setFont(dimLabelFont);
                g2d.setColor(new Color(0xCC, 0xCC, 0xFF));
                g2d.drawString(DIMENSION_NAMES[i], 120, y);

                // 星级（中间）
                String stars = scoreToStars(scores[i]);
                g2d.setFont(dimLabelFont);
                g2d.setColor(new Color(0xFF, 0xD7, 0x00)); // 金色星星
                g2d.drawString(stars, 300, y);

                // 分数（右侧）
                g2d.setFont(dimValueFont);
                g2d.setColor(Color.WHITE);
                String scoreText = scores[i] + "分";
                g2d.drawString(scoreText, 580, y);
            }

            // 上传图片到 OSS
            String fileName = "share_card_" + report.getSessionId() + "_" + System.currentTimeMillis() + ".jpg";

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();

            String ossKey = "share-cards/" + fileName;
            ossService.upload(ossKey, imageBytes);

            log.debug("分享卡片图片已上传: key={}", ossKey);

            return ossKey;

        } catch (Exception e) {
            log.error("生成分享卡片图片失败", e);
            return null;
        } finally {
            g2d.dispose();
        }
    }

    /**
     * 绘制居中文本。
     */
    private void drawCenteredText(Graphics2D g2d, String text, int centerX, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g2d.drawString(text, centerX - textWidth / 2, y);
    }

    /**
     * 将分数转换为星级字符串（5 星制，每 20 分一颗星）。
     */
    private String scoreToStars(int score) {
        int starCount = Math.max(0, Math.min(5, score / 20));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(i < starCount ? "\u2605" : "\u2606"); // ★ / ☆
        }
        return sb.toString();
    }
}
