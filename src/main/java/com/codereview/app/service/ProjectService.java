package com.codereview.app.service;

import com.codereview.app.dto.ProjectResponse;
import com.codereview.app.dto.SubmitCodeRequest;
import com.codereview.app.entity.Project;
import com.codereview.app.entity.Review;
import com.codereview.app.entity.User;
import com.codereview.app.repository.ProjectRepository;
import com.codereview.app.repository.ReviewFindingRepository;
import com.codereview.app.repository.ReviewRepository;
import com.codereview.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewFindingRepository reviewFindingRepository;
    private final FileStorageService fileStorageService;
    private final LanguageDetectionService languageDetectionService;

    public ProjectResponse uploadProject(String userEmail, MultipartFile file) throws IOException {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String originalFilename = file.getOriginalFilename();
        String detectedLanguage = languageDetectionService.detectLanguage(originalFilename);
        boolean supported = languageDetectionService.isSupported(originalFilename);

        String storedFilename = fileStorageService.storeFile(file);

        Project project = Project.builder()
                .user(user)
                .projectName(originalFilename)
                .uploadType(originalFilename != null && originalFilename.endsWith(".zip") ? "ZIP" : "FILE")
                .storedFileName(storedFilename)
                .detectedLanguage(detectedLanguage)
                .build();

        project = projectRepository.save(project);

        return new ProjectResponse(project.getId(), project.getProjectName(), project.getUploadType(),
                project.getCreatedAt(), detectedLanguage, supported);
    }

    public ProjectResponse submitCodeSnippet(String userEmail, SubmitCodeRequest request) throws IOException {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String fileName = request.getFileName();
        if (fileName == null || fileName.isBlank()) {
            fileName = "Snippet.java";
        }

        String detectedLanguage = languageDetectionService.detectLanguage(fileName);
        boolean supported = languageDetectionService.isSupported(fileName);

        String storedFilename = fileStorageService.storeTextAsFile(request.getCode(), fileName);

        Project project = Project.builder()
                .user(user)
                .projectName(fileName)
                .uploadType("SNIPPET")
                .storedFileName(storedFilename)
                .detectedLanguage(detectedLanguage)
                .build();

        project = projectRepository.save(project);

        return new ProjectResponse(project.getId(), project.getProjectName(), project.getUploadType(),
                project.getCreatedAt(), detectedLanguage, supported);
    }

    public List<ProjectResponse> getUserProjects(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return projectRepository.findByUserId(user.getId()).stream()
                .map(p -> new ProjectResponse(p.getId(), p.getProjectName(), p.getUploadType(),
                        p.getCreatedAt(), p.getDetectedLanguage(),
                        languageDetectionService.isSupported(p.getProjectName())))
                .collect(Collectors.toList());
    }

    public void deleteProject(Long projectId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Not authorized to delete this project");
        }

        List<Review> reviews = reviewRepository.findByProjectId(projectId);
        for (Review review : reviews) {
            reviewFindingRepository.deleteAll(reviewFindingRepository.findByReviewId(review.getId()));
        }
        reviewRepository.deleteAll(reviews);

        projectRepository.delete(project);
    }
}