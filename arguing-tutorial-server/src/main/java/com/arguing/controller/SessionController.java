package com.arguing.controller;

import com.arguing.common.GuestInterceptor;
import com.arguing.dto.ChatResponse;
import com.arguing.exception.ApiException;
import com.arguing.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * 开始对练会话。
     * POST /api/sessions
     * Body: { "sceneId": Long }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> startSession(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = extractUserId(request);

        Object sceneIdObj = body.get("sceneId");
        if (sceneIdObj == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "缺少 sceneId 参数");
        }
        Long sceneId;
        try {
            sceneId = Long.valueOf(sceneIdObj.toString());
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "sceneId 格式不正确");
        }

        SessionService.SessionStartResult startResult = sessionService.startSession(userId, sceneId);

        Map<String, Object> data = startResult.getResponse().toMap();
        data.put("sessionId", startResult.getSessionId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", data);
        return ResponseEntity.ok(result);
    }

    /**
     * 发送语音，返回 AI 回复。
     * POST /api/sessions/{id}/chat
     * Multipart: audio file
     */
    @PostMapping("/{id}/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @PathVariable("id") Long sessionId,
            @RequestParam(value = "audio", required = false) MultipartFile audio,
            HttpServletRequest request) {
        Long userId = extractUserId(request);

        ChatResponse response = sessionService.chat(userId, sessionId, audio);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", response.toMap());
        return ResponseEntity.ok(result);
    }

    /**
     * 请求策略提示。
     * POST /api/sessions/{id}/hint
     */
    @PostMapping("/{id}/hint")
    public ResponseEntity<Map<String, Object>> requestHint(
            @PathVariable("id") Long sessionId,
            HttpServletRequest request) {
        Long userId = extractUserId(request);

        String hint = sessionService.requestHint(userId, sessionId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hint", hint);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", data);
        return ResponseEntity.ok(result);
    }

    /**
     * 结束对练。
     * POST /api/sessions/{id}/end
     */
    @PostMapping("/{id}/end")
    public ResponseEntity<Map<String, Object>> endSession(
            @PathVariable("id") Long sessionId,
            HttpServletRequest request) {
        Long userId = extractUserId(request);

        Long endedSessionId = sessionService.endSession(userId, sessionId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", endedSessionId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", data);
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
