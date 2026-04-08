package com.arguing.service;

import com.arguing.entity.User;
import com.arguing.exception.ApiException;
import com.arguing.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 确保游客用户存在：若 token 无对应用户则创建新的游客用户。
     * 返回用户实体。
     */
    @Transactional
    public User ensureGuest(String guestToken) {
        if (guestToken != null && !guestToken.isEmpty()) {
            return userRepository.findByGuestToken(guestToken)
                    .orElseGet(this::createGuestUser);
        }
        return createGuestUser();
    }

    /**
     * 创建新的游客用户，生成 UUID token。
     */
    @Transactional
    public User createGuestUser() {
        User user = new User();
        user.setGuestToken(UUID.randomUUID().toString());
        user.setIsGuest(true);
        user.setNickname("游客" + UUID.randomUUID().toString().substring(0, 6));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * 微信登录：使用 code 换取 openid，创建或查找正式用户。
     * 当前为 stub 实现，后续对接微信 API。
     */
    @Transactional
    public User loginByWx(String code) {
        // TODO: 调用微信 API 换取 openid
        // 当前 stub 实现：使用 code 作为 mock openid
        String mockOpenid = "wx_mock_" + code;

        return userRepository.findByWxOpenid(mockOpenid)
                .orElseGet(() -> {
                    User user = new User();
                    user.setWxOpenid(mockOpenid);
                    user.setIsGuest(false);
                    user.setNickname("微信用户");
                    user.setCreatedAt(LocalDateTime.now());
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user);
                });
    }

    /**
     * 游客转正式用户：将游客账号升级为微信注册用户。
     * 迁移会话（当前 stub，后续实现会话迁移逻辑）。
     */
    @Transactional
    public User upgradeGuest(Long guestUserId, String wxCode) {
        User guest = userRepository.findById(guestUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (!guest.getIsGuest()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "该用户已经是正式用户");
        }

        // TODO: 调用微信 API 换取 openid
        String mockOpenid = "wx_mock_" + wxCode;

        // 检查该 openid 是否已有正式用户
        userRepository.findByWxOpenid(mockOpenid).ifPresent(existing -> {
            throw new ApiException(HttpStatus.CONFLICT, "该微信已绑定其他账号");
        });

        guest.setWxOpenid(mockOpenid);
        guest.setIsGuest(false);
        guest.setGuestToken(null);
        guest.setNickname("微信用户");
        guest.setUpdatedAt(LocalDateTime.now());

        // TODO: 迁移游客会话到正式账号

        log.info("游客用户 {} 已升级为正式用户", guestUserId);
        return userRepository.save(guest);
    }

    /**
     * 根据 guestToken 查找用户。
     */
    public User findByGuestToken(String guestToken) {
        return userRepository.findByGuestToken(guestToken).orElse(null);
    }
}
