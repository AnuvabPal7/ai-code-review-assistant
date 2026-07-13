package com.codereview.app.service;

import com.codereview.app.dto.ComplexityMetrics;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ComplexityAnalysisService {

    private static final Pattern CLASS_PATTERN = Pattern.compile("\\b(class|interface|enum)\\s+\\w+");
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(public|private|protected)\\s+[\\w<>\\[\\],\\s]+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{");
    private static final Pattern BRANCH_PATTERN = Pattern.compile(
            "\\b(if|for|while|case|catch)\\b|&&|\\|\\||\\?");

    public ComplexityMetrics analyze(File javaFile) throws IOException {
        String code = Files.readString(javaFile.toPath());

        int linesOfCode = (int) code.lines().filter(line -> !line.trim().isEmpty()).count();

        int numClasses = countMatches(CLASS_PATTERN, code);
        int numMethods = countMatches(METHOD_PATTERN, code);
        int branchCount = countMatches(BRANCH_PATTERN, code);

        int cyclomaticComplexity = 1 + branchCount;

        double maintainabilityIndex = Math.max(0,
                100 - (cyclomaticComplexity * 2) - (linesOfCode / 10.0));

        return new ComplexityMetrics(
                numClasses,
                Math.max(numMethods, 1),
                linesOfCode,
                cyclomaticComplexity,
                Math.round(maintainabilityIndex * 100.0) / 100.0,
                "O(1)",
                "Not yet analyzed by AI"
        );
    }

    private int countMatches(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}