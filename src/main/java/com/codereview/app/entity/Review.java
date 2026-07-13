package com.codereview.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "review_score")
    private Integer reviewScore;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "num_classes")
    private Integer numClasses;

    @Column(name = "num_methods")
    private Integer numMethods;

    @Column(name = "lines_of_code")
    private Integer linesOfCode;

    @Column(name = "cyclomatic_complexity")
    private Integer cyclomaticComplexity;

    @Column(name = "maintainability_index")
    private Double maintainabilityIndex;

    @Column(name = "estimated_time_complexity")
    private String estimatedTimeComplexity;

    @Column(name = "time_complexity_explanation", columnDefinition = "TEXT")
    private String timeComplexityExplanation;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}