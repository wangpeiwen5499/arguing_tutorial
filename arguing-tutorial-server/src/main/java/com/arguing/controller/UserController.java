package com.arguing.controller;

import com.arguing.common.GuestInterceptor;
import com.arguing.entity.Report;
import com.arguing.entity.Scene;
import com.arguing.entity.Session;
import com.arguing.entity.User;
import com.arguing.exception.ApiException;
import com.arguing.repository.ReportRepository;
import com.arguing.repository.SceneRepository;
import com.arguing.repository.SessionRepository;
import com.arguing.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器。
 * 提供用户信息、对练历史、统计数据接口。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final ReportRepository reportRepository;
    private final SceneRepository sceneRepository;

    public UserController(UserRepository userRepository,
                          SessionRepository sessionRepository,
                          ReportRepository reportRepository,
                          SceneRepository sceneRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.reportRepository = reportRepository;
        this.sceneRepository = sceneRepository;
    }

    /**
     * 获取用户信息。
     * GET /api/user/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(HttpServletRequest request) {
        Long userId = extractUserId(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));

        // 获取用户的统计信息
        List<Session> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Integer> scores = getUserScores(sessions);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("nickname", user.getNickname() != null ? user.getNickname() : "游客用户");
        profile.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
        profile.put("isGuest", user.getIsGuest());
        profile.put("totalSessions", sessions.size());
        profile.put("avgScore", calculateAvg(scores));
        profile.put("bestScore", calculateBest(scores));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", profile);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取对练历史（分页）。
     * GET /api/user/history
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(HttpServletRequest request) {
        Long userId = extractUserId(request);

        List<Session> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<Map<String, Object>> historyList = new ArrayList<>();
        for (Session session : sessions) {
            if (session.getStatus() != Session.SessionStatus.COMPLETED) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();

            // 获取场景名称
            String sceneName = sceneRepository.findById(session.getSceneId())
                    .map(Scene::getName)
                    .orElse("未知场景");
            item.put("sceneName", sceneName);

            // 获取分数
            int score = reportRepository.findBySessionId(session.getId())
                    .map(Report::getTotalScore)
                    .orElse(0);
            item.put("score", score);

            // 日期
            String date = session.getCreatedAt() != null
                    ? session.getCreatedAt().format(DATE_FORMATTER)
                    : "";
            item.put("date", date);

            historyList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", historyList);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取统计数据。
     * GET /api/user/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(HttpServletRequest request) {
        Long userId = extractUserId(request);

        List<Session> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Integer> scores = getUserScores(sessions);

        // 最近 4 次分数（按时间正序）
        List<Integer> recentScores = new ArrayList<>();
        int fromIndex = Math.max(0, scores.size() - 4);
        for (int i = scores.size() - 1; i >= fromIndex; i--) {
            recentScores.add(scores.get(i));
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSessions", sessions.size());
        stats.put("avgScore", calculateAvg(scores));
        stats.put("bestScore", calculateBest(scores));
        stats.put("recentScores", recentScores);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", stats);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户所有已完成会话的分数列表。
     */
    private List<Integer> getUserScores(List<Session> sessions) {
        List<Integer> scores = new ArrayList<>();
        for (Session session : sessions) {
            if (session.getStatus() == Session.SessionStatus.COMPLETED) {
                reportRepository.findBySessionId(session.getId())
                        .map(Report::getTotalScore)
                        .ifPresent(scores::add);
            }
        }
        return scores;
    }

    /**
     * 计算平均分。
     */
    private double calculateAvg(List<Integer> scores) {
        if (scores.isEmpty()) {
            return 0.0;
        }
        return scores.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    /**
     * 获取最高分。
     */
    private int calculateBest(List<Integer> scores) {
        return scores.stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
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
