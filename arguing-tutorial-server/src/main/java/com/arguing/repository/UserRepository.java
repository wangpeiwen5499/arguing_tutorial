package com.arguing.repository;

import com.arguing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGuestToken(String guestToken);

    Optional<User> findByWxOpenid(String wxOpenid);
}
