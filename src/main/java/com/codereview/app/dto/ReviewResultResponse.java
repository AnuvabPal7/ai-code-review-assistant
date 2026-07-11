package com.codereview.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewResultResponse {
    private Long reviewId;
    private int score;
    private String summary;
    private int findingsCount;
}