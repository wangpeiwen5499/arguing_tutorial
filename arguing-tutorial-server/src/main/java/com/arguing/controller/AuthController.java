package com.arguing.controller;

import com.arguing.entity.User;
import com.arguing.exception.ApiException;
import com.arguing.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 获取或刷新游客 token。
     * 如果请求头中有 X-Guest-Token 且有效，返回对应游客信息；
     * 否则创建新的游客用户。
     */
    @PostMapping("/guest")
    public ResponseEntity<Map<String, Object>> guestLogin(
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken) {
        User user = authService.ensureGuest(guestToken);
        return ResponseEntity.ok(buildUserResponse(user));
    }

    /**
     * 微信登录（云托管模式）。
     * 云托管自动注入 X-WX-OPENID 请求头，无需前端传 code。
     */
    @PostMapping("/wx-login")
    public ResponseEntity<Map<String, Object>> wxLogin(
            @RequestHeader(value = "X-WX-OPENID", required = false) String openid) {
        if (openid == null || openid.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "无法获取用户 OpenID");
        }
        User user = authService.loginByWx(openid);
        return ResponseEntity.ok(buildUserResponse(user));
    }

    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getId());
        result.put("nickname", user.getNickname());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("isGuest", user.getIsGuest());
        if (user.getIsGuest()) {
            result.put("guestToken", user.getGuestToken());
        }
        return result;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getStatus().value());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}
