package com.codereview.app.service;

import com.codereview.app.dto.DocumentationResponse;
import com.codereview.app.entity.Project;
import com.codereview.app.repository.ProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DocumentationService {

    private final ProjectRepository projectRepository;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.model:llama-3.3-70b-versatile}")
    private String model;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String DOC_SYSTEM_PROMPT = """
            You are generating documentation for a Java file.
            Return ONLY a single JSON object (no markdown, no preamble) with exactly two fields:

            "classSummary": a 1-3 sentence plain-English summary of what this class does overall.

            "methods": a JSON array, one entry per method found in the file, each with:
            - "methodName": the method's name
            - "description": a 1-2 sentence plain-English description of what the method does
            - "parameters": an array of strings describing each parameter (e.g. "int size - the number of elements"), empty array if none
            - "returns": a short description of the return value (e.g. "the calculated sum as an int"), or "void - does not return a value" if applicable

            Return ONLY the JSON object described above, nothing else.
            """;

    public DocumentationResponse generateDocumentation(Long projectId) throws IOException {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        Path filePath = Paths.get(uploadDir).resolve(project.getStoredFileName());
        String code = Files.readString(filePath);

        var requestBody = mapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("temperature", 0.2);

        var messages = mapper.createArrayNode();
        var systemMsg = mapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", DOC_SYSTEM_PROMPT);
        messages.add(systemMsg);

        var userMsg = mapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", code);
        messages.add(userMsg);

        requestBody.set("messages", messages);

        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + groqApiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Groq API call failed: " + response.code());
            }
            JsonNode json = mapper.readTree(response.body().string());
            String content = json.get("choices").get(0).get("message").get("content").asText();

            String cleaned = content.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```json", "").replaceAll("```", "").trim();
            }

            JsonNode root = mapper.readTree(cleaned);
            String classSummary = root.path("classSummary").asText("No summary available.");

            List<DocumentationResponse.MethodDoc> methods = new ArrayList<>();
            for (JsonNode m : root.path("methods")) {
                List<String> params = new ArrayList<>();
                for (JsonNode p : m.path("parameters")) {
                    params.add(p.asText());
                }
                methods.add(new DocumentationResponse.MethodDoc(
                        m.path("methodName").asText("unknown"),
                        m.path("description").asText(""),
                        params,
                        m.path("returns").asText("")
                ));
            }

            return new DocumentationResponse(classSummary, methods);
        }
    }
}