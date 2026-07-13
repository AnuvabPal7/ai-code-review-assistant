package com.codereview.app.controller;

import com.codereview.app.dto.ComplexityMetrics;
import com.codereview.app.dto.ReviewFindingDto;
import com.codereview.app.dto.ReviewHistoryDto;
import com.codereview.app.dto.ReviewResultResponse;
import com.codereview.app.service.PdfReportService;
import com.codereview.app.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final PdfReportService pdfReportService;

    @Value("${app.reports-dir}")
    private String reportsDir;

    @PostMapping("/{projectId}")
    public ResponseEntity<ReviewResultResponse> runReview(
            @PathVariable Long projectId,
            Authentication authentication
    ) throws IOException {
        return ResponseEntity.ok(reviewService.runReview(projectId, authentication.getName()));
    }

    @GetMapping("/{reviewId}/findings")
    public ResponseEntity<List<ReviewFindingDto>> getFindings(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.getFindings(reviewId));
    }

    @GetMapping("/{reviewId}/complexity")
    public ResponseEntity<ComplexityMetrics> getComplexity(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.getComplexity(reviewId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ReviewHistoryDto>> getHistory(Authentication authentication) {
        return ResponseEntity.ok(reviewService.getUserReviewHistory(authentication.getName()));
    }

    @GetMapping("/{reviewId}/export/pdf")
    public ResponseEntity<Resource> exportPdf(@PathVariable Long reviewId) throws IOException {
        String fileName = pdfReportService.generateReport(reviewId);
        Path filePath = Paths.get(reportsDir).resolve(fileName);
        Resource resource = new FileSystemResource(filePath);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
