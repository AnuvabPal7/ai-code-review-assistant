package com.codereview.app.service;

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

    @Value("${app.upload-dir}")
    private String uploadDir;

    private final ObjectMapper mapper = new ObjectMapper();

    public ReviewResultResponse runReview(Long projectId, String userEmail) throws IOException {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        Path filePath = Paths.get(uploadDir).resolve(project.getStoredFileName());
        File javaFile = filePath.toFile();

        Review review = reviewRepository.save(Review.builder().project(project).build());

        List<ReviewFinding> allFindings = new ArrayList<>();

        for (StaticFinding f : checkstyleService.analyze(javaFile)) {
            allFindings.add(toEntity(review, f, "STATIC_ANALYSIS"));
        }
        for (StaticFinding f : pmdService.analyze(javaFile)) {
            allFindings.add(toEntity(review, f, "STATIC_ANALYSIS"));
        }

        String code = Files.readString(filePath);
        try {
            String aiJson = groqService.reviewCode(code);
            allFindings.addAll(parseAiFindings(review, aiJson));
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
        reviewRepository.save(review);

        return new ReviewResultResponse(review.getId(), score, review.getSummary(), allFindings.size());
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

    private ReviewFinding toEntity(Review review, StaticFinding f, String findingType) {
        return ReviewFinding.builder()
                .review(review)
                .severity(normalizeSeverity(f.getSeverity()))
                .issue(f.getMessage())
                .explanation(friendlyExplanation(f.getMessage()))
                .suggestion(friendlySuggestion(f.getMessage()))
                .fileName(review.getProject().getProjectName())
                .lineNumber(f.getLineNumber())
                .findingType(findingType)
                .source(f.getSource())
                .build();
    }

    private String normalizeSeverity(String raw) {
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
        return "Review this line and consider whether a change improves clarity or safety.";
    }

    private List<ReviewFinding> parseAiFindings(Review review, String aiJson) throws IOException {
        List<ReviewFinding> results = new ArrayList<>();
        String cleaned = aiJson.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("```json", "").replaceAll("```", "").trim();
        }

        JsonNode array = mapper.readTree(cleaned);
        for (JsonNode node : array) {
            String category = node.path("category").asText("STYLE");
            String findingType = "BUG".equalsIgnoreCase(category) ? "AI_LOGICAL_BUG" : "AI_SUGGESTION";

            results.add(ReviewFinding.builder()
                    .review(review)
                    .severity(normalizeSeverity(node.path("severity").asText("LOW")))
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
