package com.arguing.controller;

import com.arguing.common.GuestInterceptor;
import com.arguing.dto.ReportView;
import com.arguing.exception.ApiException;
import com.arguing.service.AnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 复盘报告控制器。
 * 提供复盘报告查询和分享卡片接口。
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final AnalysisService analysisService;

    public ReportController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * 获取复盘报告。
     * GET /api/reports/{sessionId}
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getReport(
            @PathVariable("sessionId") Long sessionId,
            HttpServletRequest request) {
        Long userId = extractUserId(request);

        ReportView reportView = analysisService.getReport(sessionId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", reportView.toMap());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取分享卡片（暂返回 mock 数据）。
     * GET /api/reports/{sessionId}/share-card
     */
    @GetMapping("/{sessionId}/share-card")
    public ResponseEntity<Map<String, Object>> getShareCard(
            @PathVariable("sessionId") Long sessionId,
            HttpServletRequest request) {
        extractUserId(request);

        // TODO: 后续实现真实的分享卡片生成
        Map<String, Object> cardData = new LinkedHashMap<>();
        cardData.put("shareCardUrl", "https://example.com/share-card/" + sessionId + ".png");
        cardData.put("title", "我在辩了个论中获得了一份辩论成绩单，来看看我的表现吧！");
        cardData.put("description", "快来挑战你的辩论能力");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", cardData);
        return ResponseEntity.ok(result);
    }

    /**
     * 从 request attribute 中提取 userId（由 GuestInterceptor 注入）。
     */
    private Long extractUserId(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute(GuestInterceptor.USER_ID_ATTR);
        if (userIdAttr == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return (Long) userIdAttr;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getStatus().value());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}
