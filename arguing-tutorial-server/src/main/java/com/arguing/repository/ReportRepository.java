package com.arguing.repository;

import com.arguing.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findBySessionId(Long sessionId);
}
