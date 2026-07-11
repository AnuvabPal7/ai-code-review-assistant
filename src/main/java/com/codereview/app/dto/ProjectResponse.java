package com.codereview.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String projectName;
    private String uploadType;
    private LocalDateTime createdAt;
}