package com.arguing.common;

import com.arguing.entity.User;
import com.arguing.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class GuestInterceptor implements HandlerInterceptor {

    private static final String GUEST_TOKEN_HEADER = "X-Guest-Token";
    public static final String USER_ID_ATTR = "userId";

    private final AuthService authService;

    public GuestInterceptor(AuthService authService) {
        this.authService = authService;
    }

    private static final String WX_OPENID_HEADER = "X-WX-OPENID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 优先使用云托管注入的 OpenID
        String openid = request.getHeader(WX_OPENID_HEADER);
        if (openid != null && !openid.isEmpty()) {
            User user = authService.loginByWx(openid);
            request.setAttribute(USER_ID_ATTR, user.getId());
            return true;
        }

        // 降级到游客模式
        String guestToken = request.getHeader(GUEST_TOKEN_HEADER);
        User user = authService.ensureGuest(guestToken);
        request.setAttribute(USER_ID_ATTR, user.getId());
        return true;
    }
}
