package com.codereview.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class GroqService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.model:llama-3.3-70b-versatile}")
    private String model;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            You are a Senior Java Software Engineer performing a code review.
            Review the given Java code and return ONLY a JSON array (no markdown, no preamble) of findings.
            Each finding must have these fields:
            - "category": one of "BUG", "SECURITY", "PERFORMANCE", "STYLE", "BEST_PRACTICE", "REFACTOR"
            - "severity": one of "HIGH", "MEDIUM", "LOW"
            - "lineNumber": integer (best guess, 0 if unknown)
            - "message": short description
            - "suggestion": actionable suggestion text

            IMPORTANT RULE:
            - If category is "BUG" (a logical/functional bug), do NOT reveal the fix directly.
              Instead, phrase "suggestion" as a targeted guiding question that helps the developer
              discover the bug themselves (e.g. "What happens here if the list is empty?").
            - For all other categories, give direct, actionable suggestions.

            Return ONLY the JSON array, nothing else.
            """;

    public String reviewCode(String code) throws IOException {
        var requestBody = mapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("temperature", 0.3);

        var messages = mapper.createArrayNode();
        var systemMsg = mapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);
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
            return json.get("choices").get(0).get("message").get("content").asText();
        }
    }
}