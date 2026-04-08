package com.arguing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "report")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true)
    private Long sessionId;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Column(name = "logic_score", nullable = false)
    private Integer logicScore;

    @Column(name = "emotion_score", nullable = false)
    private Integer emotionScore;

    @Column(name = "persuasion_score", nullable = false)
    private Integer persuasionScore;

    @Column(name = "strategy_score", nullable = false)
    private Integer strategyScore;

    @Column(name = "clarity_score", nullable = false)
    private Integer clarityScore;

    @Column(columnDefinition = "JSON")
    private String strengths;

    @Column(columnDefinition = "JSON")
    private String improvements;

    @Column(name = "round_reviews", columnDefinition = "JSON")
    private String roundReviews;

    @Column(name = "share_card_url", length = 500)
    private String shareCardUrl;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
