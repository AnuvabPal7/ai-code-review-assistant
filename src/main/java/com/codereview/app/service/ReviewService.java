package com.codereview.app.service;

import com.codereview.app.dto.ComplexityMetrics;
import com.codereview.app.dto.ReviewFindingDto;
import com.codereview.app.dto.ReviewHistoryDto;
import com.codereview.app.dto.ReviewResultResponse;
import com.codereview.app.dto.StaticFinding;
import com.codereview.app.entity.Project;
import com.codereview.app.entity.Review;
import com.codereview.app.entity.ReviewFinding;
import com.codereview.app.entity.User;
import com.codereview.app.repository.ProjectRepository;
import com.codereview.app.repository.ReviewFindingRepository;
import com.codereview.app.repository.ReviewRepository;
import com.codereview.app.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ProjectRepository projectRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewFindingRepository reviewFindingRepository;
    private final UserRepository userRepository;
    private final CheckstyleService checkstyleService;
    private final PmdService pmdService;
    private final GroqService groqService;
    private final ComplexityAnalysisService complexityAnalysisService;
    private final LanguageDetectionService languageDetectionService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    private final ObjectMapper mapper = new ObjectMapper();

    public ReviewResultResponse runReview(Long projectId, String userEmail) throws IOException {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!languageDetectionService.isSupported(project.getProjectName())) {
            String detected = languageDetectionService.detectLanguage(project.getProjectName());
            throw new IllegalArgumentException(
                    "Detected file type: " + detected + ". This is not programmed yet - only Java files are currently supported."
            );
        }

        Path filePath = Paths.get(uploadDir).resolve(project.getStoredFileName());
        File javaFile = filePath.toFile();

        ComplexityMetrics complexity = complexityAnalysisService.analyze(javaFile);

        Review review = Review.builder()
                .project(project)
                .numClasses(complexity.getNumClasses())
                .numMethods(complexity.getNumMethods())
                .linesOfCode(complexity.getLinesOfCode())
                .cyclomaticComplexity(complexity.getCyclomaticComplexity())
                .maintainabilityIndex(complexity.getMaintainabilityIndex())
                .estimatedTimeComplexity("O(1)")
                .timeComplexityExplanation("Not yet analyzed by AI")
                .build();
        review = reviewRepository.save(review);

        List<ReviewFinding> allFindings = new ArrayList<>();

        for (StaticFinding f : checkstyleService.analyze(javaFile)) {
            allFindings.add(toEntity(review, f, "STATIC_ANALYSIS"));
        }
        for (StaticFinding f : pmdService.analyze(javaFile)) {
            allFindings.add(toEntity(review, f, "STATIC_ANALYSIS"));
        }

        String code = Files.readString(filePath);
        String timeComplexityEstimate = "O(1)";
        String timeComplexityExplanation = "Not analyzed";

        try {
            String aiResponse = groqService.reviewCode(code);
            String cleaned = aiResponse.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```json", "").replaceAll("```", "").trim();
            }

            JsonNode root = mapper.readTree(cleaned);

            JsonNode findingsNode = root.path("findings");
            allFindings.addAll(parseAiFindings(review, findingsNode));

            JsonNode tcNode = root.path("timeComplexity");
            timeComplexityEstimate = tcNode.path("estimate").asText("O(1)");
            timeComplexityExplanation = tcNode.path("explanation").asText("No explanation provided");

        } catch (Exception e) {
            allFindings.add(ReviewFinding.builder()
                    .review(review)
                    .severity("LOW")
                    .issue("AI review unavailable")
                    .explanation(e.getMessage())
                    .suggestion("Retry later")
                    .fileName(project.getProjectName())
                    .lineNumber(0)
                    .findingType("SYSTEM")
                    .source("SYSTEM")
                    .build());
        }

        reviewFindingRepository.saveAll(allFindings);

        int score = computeScore(allFindings);
        review.setReviewScore(score);
        review.setSummary("Found " + allFindings.size() + " findings across static analysis and AI review.");
        review.setEstimatedTimeComplexity(timeComplexityEstimate);
        review.setTimeComplexityExplanation(timeComplexityExplanation);
        reviewRepository.save(review);

        ComplexityMetrics finalComplexity = new ComplexityMetrics(
                complexity.getNumClasses(),
                complexity.getNumMethods(),
                complexity.getLinesOfCode(),
                complexity.getCyclomaticComplexity(),
                complexity.getMaintainabilityIndex(),
                timeComplexityEstimate,
                timeComplexityExplanation
        );

        return new ReviewResultResponse(review.getId(), score, review.getSummary(), allFindings.size(), finalComplexity);
    }

    public List<ReviewFindingDto> getFindings(Long reviewId) {
        return reviewFindingRepository.findByReviewId(reviewId).stream()
                .map(f -> new ReviewFindingDto(f.getId(), f.getSeverity(), f.getIssue(), f.getExplanation(),
                        f.getSuggestion(), f.getFileName(), f.getLineNumber(), f.getFindingType(), f.getSource()))
                .collect(Collectors.toList());
    }

    public List<ReviewHistoryDto> getUserReviewHistory(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return reviewRepository.findByProject_User_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(r -> new ReviewHistoryDto(
                        r.getId(),
                        r.getProject().getProjectName(),
                        r.getReviewScore(),
                        r.getSummary(),
                        r.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public ComplexityMetrics getComplexity(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        return new ComplexityMetrics(
                review.getNumClasses() == null ? 0 : review.getNumClasses(),
                review.getNumMethods() == null ? 0 : review.getNumMethods(),
                review.getLinesOfCode() == null ? 0 : review.getLinesOfCode(),
                review.getCyclomaticComplexity() == null ? 0 : review.getCyclomaticComplexity(),
                review.getMaintainabilityIndex() == null ? 0 : review.getMaintainabilityIndex(),
                review.getEstimatedTimeComplexity() == null ? "O(1)" : review.getEstimatedTimeComplexity(),
                review.getTimeComplexityExplanation() == null ? "" : review.getTimeComplexityExplanation()
        );
    }

    public void deleteReview(Long reviewId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (!review.getProject().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Not authorized to delete this review");
        }

        reviewFindingRepository.deleteAll(reviewFindingRepository.findByReviewId(reviewId));
        reviewRepository.delete(review);
    }

    private ReviewFinding toEntity(Review review, StaticFinding f, String findingType) {
        return ReviewFinding.builder()
                .review(review)
                .severity(normalizeSeverity(f.getSeverity(), f.getMessage()))
                .issue(f.getMessage())
                .explanation(friendlyExplanation(f.getMessage()))
                .suggestion(friendlySuggestion(f.getMessage()))
                .fileName(review.getProject().getProjectName())
                .lineNumber(f.getLineNumber())
                .findingType(findingType)
                .source(f.getSource())
                .build();
    }

    private String normalizeSeverity(String raw, String message) {
        String m = message == null ? "" : message.toLowerCase();
        if (m.contains("system.out") || m.contains("system.err")) {
            return "LOW";
        }
        if (raw == null) return "LOW";
        String s = raw.toUpperCase();
        if (s.contains("HIGH")) return "HIGH";
        if (s.contains("MEDIUM") || s.contains("MODERATE")) return "MEDIUM";
        return "LOW";
    }

    private String friendlyExplanation(String message) {
        String m = message.toLowerCase();
        if (m.contains("closed after use") || m.contains("close resource")) {
            return "Your code opens something (like keyboard input or a file) but never tells the program you're done with it. This is called a 'resource leak' - not dangerous for small programs, but a bad habit for bigger ones.";
        }
        if (m.contains("system.out") || m.contains("system.err")) {
            return "println() works fine for learning and testing, but real-world projects use proper logging tools instead, so output can be controlled, searched, and turned on/off later.";
        }
        if (m.contains("varargs")) {
            return "This is a minor style tip about how a method accepts array-type input. Not a bug - just a more flexible way to write the method signature.";
        }
        if (m.contains("unused")) {
            return "This variable or import is declared but never actually used anywhere in the code, which just adds clutter.";
        }
        if (m.contains("empty catch")) {
            return "This code catches an error but does nothing about it, silently, which can hide real bugs from you later.";
        }
        if (m.contains("magic number")) {
            return "A number appears directly in the code without explanation. Giving it a named constant makes the code easier to understand later.";
        }
        if (m.contains("override")) {
            return "This method overrides a method from a parent class/interface, but doesn't have the @Override annotation, which helps catch typos and mismatches.";
        }
        return "A static analysis tool flagged this pattern as worth reviewing.";
    }

    private String friendlySuggestion(String message) {
        String m = message.toLowerCase();
        if (m.contains("closed after use") || m.contains("close resource")) {
            return "Add a call like scanner.close(); once you're done using it, or use a try-with-resources block.";
        }
        if (m.contains("system.out") || m.contains("system.err")) {
            return "For a learning project this is fine to leave as-is. In production code, consider a logging library like SLF4J instead.";
        }
        if (m.contains("varargs")) {
            return "Optional: you could rewrite the array parameter using varargs (e.g. int... values) for more flexible calling.";
        }
        if (m.contains("unused")) {
            return "Remove the unused variable or import to keep the code clean.";
        }
        if (m.contains("empty catch")) {
            return "Add at least a comment or logging statement inside the catch block so errors aren't silently ignored.";
        }
        if (m.contains("magic number")) {
            return "Define a named constant (e.g. private static final int MAX_SIZE = 500;) instead of using the raw number directly.";
        }
        if (m.contains("override")) {
            return "Add the @Override annotation directly above the method signature.";
        }
        return "Review this line and consider whether a change improves clarity or safety.";
    }

    private List<ReviewFinding> parseAiFindings(Review review, JsonNode findingsArray) {
        List<ReviewFinding> results = new ArrayList<>();

        for (JsonNode node : findingsArray) {
            String category = node.path("category").asText("STYLE");
            String findingType = "BUG".equalsIgnoreCase(category) ? "AI_LOGICAL_BUG" : "AI_SUGGESTION";

            results.add(ReviewFinding.builder()
                    .review(review)
                    .severity(normalizeSeverity(node.path("severity").asText("LOW"), node.path("message").asText("")))
                    .issue(node.path("message").asText(""))
                    .explanation(category)
                    .suggestion(node.path("suggestion").asText(""))
                    .fileName(review.getProject().getProjectName())
                    .lineNumber(node.path("lineNumber").asInt(0))
                    .findingType(findingType)
                    .source("AI")
                    .build());
        }
        return results;
    }

    private int computeScore(List<ReviewFinding> findings) {
        int score = 100;
        for (ReviewFinding f : findings) {
            String sev = f.getSeverity() == null ? "" : f.getSeverity().toUpperCase();
            switch (sev) {
                case "HIGH" -> score -= 10;
                case "MEDIUM" -> score -= 5;
                case "LOW" -> score -= 2;
                default -> score -= 1;
            }
        }
        return Math.max(score, 0);
    }
}