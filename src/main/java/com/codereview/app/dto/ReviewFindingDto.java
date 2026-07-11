package com.codereview.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewFindingDto {
    private Long id;
    private String severity;
    private String issue;
    private String explanation;
    private String suggestion;
    private String fileName;
    private Integer lineNumber;
    private String findingType;
    private String source;
}