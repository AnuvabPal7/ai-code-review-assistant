package com.codereview.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ComplexityMetrics {
    private int numClasses;
    private int numMethods;
    private int linesOfCode;
    private int cyclomaticComplexity;
    private double maintainabilityIndex;
    private String estimatedTimeComplexity;
    private String timeComplexityExplanation;
}