package com.arguing.service;

import com.arguing.dto.ChatResponse;
import com.arguing.entity.Round;
import com.arguing.entity.Scene;
import com.arguing.entity.Session;
import com.arguing.exception.ApiException;
import com.arguing.repository.RoundRepository;
import com.arguing.repository.SceneRepository;
import com.arguing.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    /** 每次会话最多可使用的提示次数 */
    private static final int MAX_HINT_COUNT = 3;

    /** Mock AI 回复列表 */
    private static final List<String> MOCK_AI_REPLIES = List.of(
            "你说得有道理，但是你有没有考虑过另一种情况？",
            "我理解你的观点，不过从实际数据来看，情况并非如此。",
            "这确实是个有趣的论点，但逻辑上还存在漏洞。",
            "假设你是对的，那怎么解释反面的证据呢？",
            "我们可以换个角度来看这个问题。",
            "你的论证缺乏有力的事实支撑。",
            "即使如此，这也不能推导出你的结论。",
            "不妨举一个具体的例子来说明你的观点。",
            "这个论据的说服力还不够强，试着用数据支撑。",
            "好的，我部分同意你的说法，但还有保留意见。"
    );

    /** Mock 提示列表 */
    private static final List<String> MOCK_HINTS = List.of(
            "数据举证：用具体数字支撑你的论点",
            "逻辑推理：尝试从对方的前提推导出矛盾",
            "类比论证：用一个恰当的类比来增强说服力"
    );

    /** 音频临时存储路径 */
    private static final String AUDIO_TMP_DIR = System.getProperty("java.io.tmpdir") + "/arguing-tutorial/audio";

    private final SessionRepository sessionRepository;
    private final RoundRepository roundRepository;
    private final SceneRepository sceneRepository;

    public SessionService(SessionRepository sessionRepository,
                          RoundRepository roundRepository,
                          SceneRepository sceneRepository) {
        this.sessionRepository = sessionRepository;
        this.roundRepository = roundRepository;
        this.sceneRepository = sceneRepository;
    }

    /**
     * 开始对练会话。
     * 1. 校验场景存在
     * 2. 创建 Session
     * 3. 创建 Round 0 作为 AI 开场白
     * 4. 返回 ChatResponse 包含开场白
     */
    @Transactional
    public ChatResponse startSession(Long userId, Long sceneId) {
        // 1. 校验场景存在
        Scene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "场景不存在"));

        // 2. 创建 Session
        Session session = new Session();
        session.setUserId(userId);
        session.setSceneId(sceneId);
        session.setStatus(Session.SessionStatus.ACTIVE);
        session.setTotalRounds(10);
        session.setCurrentRound(0);
        session.setHintUsedCount(0);
        session.setCreatedAt(LocalDateTime.now());
        session = sessionRepository.save(session);

        // 3. 创建 Round 0（AI 开场白）
        String openingLine = scene.getOpeningLine();
        if (openingLine == null || openingLine.isEmpty()) {
            openingLine = "你好，让我们开始今天的辩论练习吧。";
        }

        Round round0 = new Round();
        round0.setSessionId(session.getId());
        round0.setRoundNumber(0);
        round0.setAiText(openingLine);
        round0.setAiEmotion("neutral");
        round0.setCreatedAt(LocalDateTime.now());
        roundRepository.save(round0);

        log.info("用户 {} 开始会话 {}，场景 {}", userId, session.getId(), sceneId);

        // 4. 返回 ChatResponse
        ChatResponse response = new ChatResponse();
        response.setText(openingLine);
        response.setAudioUrl(null); // mock 阶段为空，后续 Task 6 接入
        response.setEmotion("neutral");
        response.setExpression(null); // mock 阶段为空
        response.setCurrentRound(0);
        response.setTotalRounds(session.getTotalRounds());
        return response;
    }

    /**
     * 发送语音对练。
     * 1. 校验 Session 存在且 ACTIVE
     * 2. 校验 currentRound < totalRounds
     * 3. 保存音频文件到临时路径
     * 4. Mock ASR 返回固定文字
     * 5. 创建 Round 记录
     * 6. Mock AI 回复
     * 7. currentRound++
     * 8. 若达到总轮次，自动结束
     * 9. 返回 ChatResponse
     */
    @Transactional
    public ChatResponse chat(Long userId, Long sessionId, MultipartFile audioFile) {
        // 1. 校验 Session 存在且 ACTIVE
        Session session = findActiveSession(sessionId, userId);

        // 2. 校验轮次
        if (session.getCurrentRound() >= session.getTotalRounds()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "对练已结束，不能再发送消息");
        }

        // 3. 保存音频文件
        String audioUrl = null;
        if (audioFile != null && !audioFile.isEmpty()) {
            audioUrl = saveAudioFile(sessionId, session.getCurrentRound() + 1, audioFile);
        }

        // 4. Mock ASR
        String userText = "[mock] 用户发言内容";

        // 5. 创建用户轮次 Round 记录
        int userRoundNumber = session.getCurrentRound() + 1;
        Round userRound = new Round();
        userRound.setSessionId(session.getId());
        userRound.setRoundNumber(userRoundNumber);
        userRound.setUserText(userText);
        userRound.setUserAudioUrl(audioUrl);
        userRound.setCreatedAt(LocalDateTime.now());
        roundRepository.save(userRound);

        // 6. Mock AI 回复
        String aiText = MOCK_AI_REPLIES.get(userRoundNumber % MOCK_AI_REPLIES.size());
        String aiEmotion = "neutral";

        Round aiRound = new Round();
        aiRound.setSessionId(session.getId());
        aiRound.setRoundNumber(userRoundNumber);
        aiRound.setAiText(aiText);
        aiRound.setAiEmotion(aiEmotion);
        aiRound.setCreatedAt(LocalDateTime.now());
        roundRepository.save(aiRound);

        // 7. currentRound++
        session.setCurrentRound(userRoundNumber);

        // 8. 若达到总轮次，自动结束
        if (session.getCurrentRound() >= session.getTotalRounds()) {
            session.setStatus(Session.SessionStatus.COMPLETED);
            session.setFinishedAt(LocalDateTime.now());
            log.info("会话 {} 已达到总轮次，自动结束", sessionId);
        }
        sessionRepository.save(session);

        log.info("会话 {} 第 {} 轮对练完成", sessionId, userRoundNumber);

        // 9. 返回 ChatResponse
        ChatResponse response = new ChatResponse();
        response.setText(aiText);
        response.setAudioUrl(null); // mock 阶段为空
        response.setEmotion(aiEmotion);
        response.setExpression(null); // mock 阶段为空
        response.setCurrentRound(session.getCurrentRound());
        response.setTotalRounds(session.getTotalRounds());
        return response;
    }

    /**
     * 请求策略提示。
     * 1. 校验 Session 存在且 ACTIVE
     * 2. 校验 hintUsedCount < 3
     * 3. hintUsedCount++
     * 4. 返回 mock 提示文字
     */
    @Transactional
    public String requestHint(Long userId, Long sessionId) {
        Session session = findActiveSession(sessionId, userId);

        if (session.getHintUsedCount() >= MAX_HINT_COUNT) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "提示次数已用完（最多 " + MAX_HINT_COUNT + " 次）");
        }

        session.setHintUsedCount(session.getHintUsedCount() + 1);
        sessionRepository.save(session);

        // 返回 mock 提示
        String hint = MOCK_HINTS.get(session.getHintUsedCount() - 1);
        log.info("会话 {} 请求提示（第 {} 次）", sessionId, session.getHintUsedCount());
        return hint;
    }

    /**
     * 结束对练会话。
     * 1. 校验 Session 存在且 ACTIVE
     * 2. 设置 status=COMPLETED, finishedAt=now
     * 3. 返回 sessionId
     */
    @Transactional
    public Long endSession(Long userId, Long sessionId) {
        Session session = findActiveSession(sessionId, userId);

        session.setStatus(Session.SessionStatus.COMPLETED);
        session.setFinishedAt(LocalDateTime.now());
        sessionRepository.save(session);

        log.info("用户 {} 手动结束会话 {}", userId, sessionId);
        return sessionId;
    }

    /**
     * 查找并校验 Session：存在、属于当前用户、状态为 ACTIVE。
     */
    private Session findActiveSession(Long sessionId, Long userId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "会话不存在"));

        if (!session.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权访问该会话");
        }

        if (session.getStatus() != Session.SessionStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "会话已结束，无法继续操作");
        }

        return session;
    }

    /**
     * 保存音频文件到本地临时路径。
     * 后续会替换为 OSS 存储。
     */
    private String saveAudioFile(Long sessionId, int roundNumber, MultipartFile audioFile) {
        try {
            Path dir = Paths.get(AUDIO_TMP_DIR, String.valueOf(sessionId));
            Files.createDirectories(dir);

            String originalFilename = audioFile.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = "round_" + roundNumber + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

            Path filePath = dir.resolve(filename);
            audioFile.transferTo(filePath.toFile());

            log.debug("音频文件已保存: {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            log.warn("保存音频文件失败，sessionId={}, round={}", sessionId, roundNumber, e);
            return null;
        }
    }
}
