package com.codereview.app.service;

import com.codereview.app.entity.Review;
import com.codereview.app.entity.ReviewFinding;
import com.codereview.app.repository.ReviewFindingRepository;
import com.codereview.app.repository.ReviewRepository;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final ReviewRepository reviewRepository;
    private final ReviewFindingRepository reviewFindingRepository;

    @Value("${app.reports-dir}")
    private String reportsDir;

    public String generateReport(Long reviewId) throws IOException {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        List<ReviewFinding> findings = reviewFindingRepository.findByReviewId(reviewId);

        Path reportsPath = Paths.get(reportsDir);
        if (!Files.exists(reportsPath)) {
            Files.createDirectories(reportsPath);
        }

        String fileName = "review-" + reviewId + "-report.pdf";
        Path filePath = reportsPath.resolve(fileName);

        try (PdfWriter writer = new PdfWriter(filePath.toString());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.add(new Paragraph("Code Review Report").setBold().setFontSize(18));
            document.add(new Paragraph("Project: " + review.getProject().getProjectName()));
            document.add(new Paragraph("Review Score: " + review.getReviewScore() + " / 100"));
            document.add(new Paragraph("Summary: " + review.getSummary()));
            document.add(new Paragraph(" "));

            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 3, 3, 1}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell(new Cell().add(new Paragraph("Severity")));
            table.addHeaderCell(new Cell().add(new Paragraph("Source")));
            table.addHeaderCell(new Cell().add(new Paragraph("Issue")));
            table.addHeaderCell(new Cell().add(new Paragraph("Suggestion")));
            table.addHeaderCell(new Cell().add(new Paragraph("Line")));

            for (ReviewFinding f : findings) {
                table.addCell(new Cell().add(new Paragraph(nullSafe(f.getSeverity()))));
                table.addCell(new Cell().add(new Paragraph(nullSafe(f.getSource()))));
                table.addCell(new Cell().add(new Paragraph(nullSafe(f.getIssue()))));
                table.addCell(new Cell().add(new Paragraph(nullSafe(f.getSuggestion()))));
                table.addCell(new Cell().add(new Paragraph(f.getLineNumber() == null ? "-" : f.getLineNumber().toString())));
            }

            document.add(table);
        }

        return fileName;
    }

    private String nullSafe(String value) {
        return value == null ? "-" : value;
    }
}