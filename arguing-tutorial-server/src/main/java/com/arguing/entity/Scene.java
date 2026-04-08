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
@Table(name = "scene")
public class Scene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false)
    private Integer difficulty;

    @Column(columnDefinition = "JSON")
    private String backgroundConfig;

    @Column(columnDefinition = "JSON")
    private String avatarConfig;

    @Column(length = 500)
    private String personality;

    @Column(length = 500)
    private String openingLine;

    @Column(columnDefinition = "JSON")
    private String evaluationCriteria;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
