package com.codereview.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReviewHistoryDto {
    private Long reviewId;
    private String projectName;
    private Integer score;
    private String summary;
    private LocalDateTime createdAt;
}