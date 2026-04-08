package com.arguing.repository;

import com.arguing.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundRepository extends JpaRepository<Round, Long> {

    List<Round> findBySessionIdOrderByRoundNumber(Long sessionId);
}
