package com.codereview.app.controller;

import com.codereview.app.dto.ProjectResponse;
import com.codereview.app.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/upload")
    public ResponseEntity<ProjectResponse> uploadProject(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) throws IOException {
        String userEmail = authentication.getName();
        ProjectResponse response = projectService.uploadProject(userEmail, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getUserProjects(Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(projectService.getUserProjects(userEmail));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id, Authentication authentication) {
        projectService.deleteProject(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
