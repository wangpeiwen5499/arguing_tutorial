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
@Table(name = "round")
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "user_audio_url", length = 2048)
    private String userAudioUrl;

    @Column(name = "user_text", columnDefinition = "TEXT")
    private String userText;

    @Column(name = "ai_text", columnDefinition = "TEXT")
    private String aiText;

    @Column(name = "ai_audio_url", length = 2048)
    private String aiAudioUrl;

    @Column(name = "ai_emotion", length = 20)
    private String aiEmotion;

    @Column(name = "ai_expression", columnDefinition = "JSON")
    private String aiExpression;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
