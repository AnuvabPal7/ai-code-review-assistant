package com.codereview.app.controller;

import com.codereview.app.dto.DocumentationResponse;
import com.codereview.app.service.DocumentationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/documentation")
@RequiredArgsConstructor
public class DocumentationController {

    private final DocumentationService documentationService;

    @PostMapping("/{projectId}")
    public ResponseEntity<DocumentationResponse> generate(@PathVariable Long projectId) throws IOException {
        return ResponseEntity.ok(documentationService.generateDocumentation(projectId));
    }
}