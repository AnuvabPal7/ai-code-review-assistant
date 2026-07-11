package com.codereview.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review_findings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewFinding {
    private String findingType; // STATIC_ANALYSIS, AI_LOGICAL_BUG, AI_SUGGESTION, SYSTEM

    private String source; // CHECKSTYLE, PMD, AI, SYSTEM

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    private String severity; // e.g. "HIGH", "MEDIUM", "LOW"

    @Column(columnDefinition = "TEXT")
    private String issue;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "line_number")
    private Integer lineNumber;
}